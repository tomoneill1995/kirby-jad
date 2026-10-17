package com.kirbyjad.mesh;

// bones have to be listed parent first
public final class Rig
{
	public final int[] parent;
	public final float[] px, py, pz;

	public float[] outX, outY, outZ;
	// -1 = face is hidden this frame
	public int[] outRgb;
	public float[] outI1, outI2, outI3;

	private float[][] world;
	private float[] nx, ny, nz;
	private float[] mx, my, mz;

	private static final int STONE_RGB = 0x8c8a90;

	public Rig(int[] parent, float[] px, float[] py, float[] pz)
	{
		this.parent = parent;
		this.px = px;
		this.py = py;
		this.pz = pz;
	}

	public int boneCount()
	{
		return parent.length;
	}

	public void apply(Mesh mesh, Pose pose, float lx, float ly, float lz)
	{
		int vc = mesh.vertexCount, fc = mesh.faceCount, bc = parent.length;
		if (outX == null || outX.length != vc)
		{
			outX = new float[vc];
			outY = new float[vc];
			outZ = new float[vc];
			nx = new float[vc];
			ny = new float[vc];
			nz = new float[vc];
			mx = new float[vc];
			my = new float[vc];
			mz = new float[vc];
			outRgb = new int[fc];
			outI1 = new float[fc];
			outI2 = new float[fc];
			outI3 = new float[fc];
			world = new float[bc][12];
		}

		float[] rot = new float[9];
		float[] local = new float[12];
		for (int b = 0; b < bc; b++)
		{
			Mat.rotation(pose.pitch[b], pose.yaw[b], pose.roll[b], rot);
			float cx = px[b], cy = py[b], cz = pz[b];
			float sx = pose.sx[b], sy = pose.sy[b], sz = pose.sz[b];
			for (int i = 0; i < 3; i++)
			{
				local[i * 4] = rot[i * 3] * sx;
				local[i * 4 + 1] = rot[i * 3 + 1] * sy;
				local[i * 4 + 2] = rot[i * 3 + 2] * sz;
			}
			// rotate/scale around the pivot
			local[3] = cx + pose.tx[b] - (local[0] * cx + local[1] * cy + local[2] * cz);
			local[7] = cy + pose.ty[b] - (local[4] * cx + local[5] * cy + local[6] * cz);
			local[11] = cz + pose.tz[b] - (local[8] * cx + local[9] * cy + local[10] * cz);
			if (parent[b] < 0)
			{
				System.arraycopy(local, 0, world[b], 0, 12);
			}
			else
			{
				Mat.mul(world[parent[b]], local, world[b]);
			}
		}

		System.arraycopy(mesh.x, 0, mx, 0, vc);
		System.arraycopy(mesh.y, 0, my, 0, vc);
		System.arraycopy(mesh.z, 0, mz, 0, vc);
		for (int m = 0; m < mesh.morphNames.length; m++)
		{
			float w = pose.morph[m];
			if (w == 0)
			{
				continue;
			}
			int[] idx = mesh.morphIndices[m];
			float[] dx = mesh.morphDx[m], dy = mesh.morphDy[m], dz = mesh.morphDz[m];
			for (int i = 0; i < idx.length; i++)
			{
				mx[idx[i]] += dx[i] * w;
				my[idx[i]] += dy[i] * w;
				mz[idx[i]] += dz[i] * w;
			}
		}

		for (int v = 0; v < vc; v++)
		{
			float[] w = world[mesh.bone[v]];
			float x = mx[v], y = my[v], z = mz[v];
			outX[v] = w[0] * x + w[1] * y + w[2] * z + w[3];
			outY[v] = w[4] * x + w[5] * y + w[6] * z + w[7];
			outZ[v] = w[8] * x + w[9] * y + w[10] * z + w[11];
			nx[v] = 0;
			ny[v] = 0;
			nz[v] = 0;
		}

		for (int f = 0; f < fc; f++)
		{
			int a = mesh.a[f], b = mesh.b[f], c = mesh.c[f];
			float ux = outX[b] - outX[a], uy = outY[b] - outY[a], uz = outZ[b] - outZ[a];
			float wx = outX[c] - outX[a], wy = outY[c] - outY[a], wz = outZ[c] - outZ[a];
			float fx = uy * wz - uz * wy, fy = uz * wx - ux * wz, fz = ux * wy - uy * wx;
			nx[a] += fx;
			ny[a] += fy;
			nz[a] += fz;
			nx[b] += fx;
			ny[b] += fy;
			nz[b] += fz;
			nx[c] += fx;
			ny[c] += fy;
			nz[c] += fz;
		}

		float stone = pose.stone;
		for (int f = 0; f < fc; f++)
		{
			int a = mesh.a[f], b = mesh.b[f], c = mesh.c[f];
			float ux = outX[b] - outX[a], uy = outY[b] - outY[a], uz = outZ[b] - outZ[a];
			float wx = outX[c] - outX[a], wy = outY[c] - outY[a], wz = outZ[c] - outZ[a];
			float fx = uy * wz - uz * wy, fy = uz * wx - ux * wz, fz = ux * wy - uy * wx;
			float fl = (float) Math.sqrt(fx * fx + fy * fy + fz * fz);
			if (fl < 0.05f)
			{
				outRgb[f] = -1;
				continue;
			}
			int flags = mesh.flags[f];
			int rgb = mesh.rgb[f];
			if ((flags & Mesh.FLAG_EMISSIVE) != 0)
			{
				outRgb[f] = rgb;
				outI1[f] = outI2[f] = outI3[f] = pose.glow;
				continue;
			}
			float faceLight = light(fx / fl, fy / fl, fz / fl, lx, ly, lz);
			float ia = vertexLight(a, lx, ly, lz), ib = vertexLight(b, lx, ly, lz), ic = vertexLight(c, lx, ly, lz);
			if ((flags & Mesh.FLAG_STONE) != 0 && stone > 0)
			{
				rgb = mix(rgb, STONE_RGB, Math.min(1, stone * 1.15f));
				ia += (faceLight - ia) * stone;
				ib += (faceLight - ib) * stone;
				ic += (faceLight - ic) * stone;
			}
			outRgb[f] = rgb;
			outI1[f] = ia;
			outI2[f] = ib;
			outI3[f] = ic;
		}
	}

	private float vertexLight(int v, float lx, float ly, float lz)
	{
		float x = nx[v], y = ny[v], z = nz[v];
		float l = (float) Math.sqrt(x * x + y * y + z * z);
		if (l == 0)
		{
			return 1;
		}
		return light(x / l, y / l, z / l, lx, ly, lz);
	}

	static float light(float x, float y, float z, float lx, float ly, float lz)
	{
		float d = x * lx + y * ly + z * lz;
		d = Math.max(0, (d + 0.3f) / 1.3f);
		return 0.5f + 0.62f * d + 0.1f * Math.max(0, y);
	}

	static int mix(int a, int b, float t)
	{
		int ar = a >> 16 & 255, ag = a >> 8 & 255, ab = a & 255;
		int br = b >> 16 & 255, bg = b >> 8 & 255, bb = b & 255;
		return (int) (ar + (br - ar) * t) << 16 | (int) (ag + (bg - ag) * t) << 8 | (int) (ab + (bb - ab) * t);
	}
}
