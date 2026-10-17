package com.kirbyjad.mesh;

public final class Shapes
{
	private Shapes()
	{
	}

	public interface DirColor
	{
		int rgb(float dx, float dy, float dz);
	}

	// f goes 0 at the middle to 1 at the edge
	public interface RingColor
	{
		int rgb(float f, float angle);
	}

	public static DirColor solid(int rgb)
	{
		return (x, y, z) -> rgb;
	}

	public static void ellipsoid(MeshBuilder mb, Ellipsoid e, int rings, int segs, int bone, DirColor col, int flags)
	{
		band(mb, e, 0, (float) Math.PI, rings, segs, bone, col, flags);
	}

	// slice between theta0 and theta1, 0 = top. used for the bandana
	public static void band(MeshBuilder mb, Ellipsoid e, float theta0, float theta1, int rings, int segs, int bone,
		DirColor col, int flags)
	{
		final float eps = 1e-4f;
		int[][] rows = new int[rings + 1][];
		float[][][] dirs = new float[rings + 1][][];
		float[] p = new float[3];
		for (int i = 0; i <= rings; i++)
		{
			float th = theta0 + (theta1 - theta0) * i / rings;
			boolean pole = th < eps || th > Math.PI - eps;
			int n = pole ? 1 : segs;
			rows[i] = new int[n];
			dirs[i] = new float[n][];
			for (int j = 0; j < n; j++)
			{
				float ph = (float) (2 * Math.PI * j / segs);
				float dx = (float) (Math.sin(th) * Math.cos(ph));
				float dy = (float) Math.cos(th);
				float dz = (float) (Math.sin(th) * Math.sin(ph));
				if (pole)
				{
					dx = 0;
					dz = 0;
					dy = th < eps ? 1 : -1;
				}
				e.surface(dx, dy, dz, 0, p);
				rows[i][j] = mb.vertex(p[0], p[1], p[2], bone);
				dirs[i][j] = new float[]{dx, dy, dz};
			}
		}
		for (int i = 0; i < rings; i++)
		{
			int[] r0 = rows[i], r1 = rows[i + 1];
			float[][] d0 = dirs[i], d1 = dirs[i + 1];
			for (int j = 0; j < segs; j++)
			{
				int j1 = (j + 1) % segs;
				if (r0.length == 1 && r1.length == 1)
				{
					continue;
				}
				if (r0.length == 1)
				{
					tri(mb, e, r0[0], r1[j], r1[j1], d0[0], d1[j], d1[j1], bone, col, flags);
				}
				else if (r1.length == 1)
				{
					tri(mb, e, r0[j], r1[0], r0[j1], d0[j], d1[0], d0[j1], bone, col, flags);
				}
				else
				{
					tri(mb, e, r0[j], r1[j], r1[j1], d0[j], d1[j], d1[j1], bone, col, flags);
					tri(mb, e, r0[j], r1[j1], r0[j1], d0[j], d1[j1], d0[j1], bone, col, flags);
				}
			}
		}
	}

	private static void tri(MeshBuilder mb, Ellipsoid e, int a, int b, int c, float[] da, float[] db, float[] dc,
		int bone, DirColor col, int flags)
	{
		float dx = da[0] + db[0] + dc[0], dy = da[1] + db[1] + dc[1], dz = da[2] + db[2] + dc[2];
		float l = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
		dx /= l;
		dy /= l;
		dz /= l;
		float[] n = new float[3];
		e.normal(dx, dy, dz, n);
		mb.face(a, b, c, col.rgb(dx, dy, dz), flags, n[0], n[1], n[2]);
	}

	// a flat shape wrapped onto an ellipsoid - eyes, mouth etc
	public static final class Decal
	{
		public float dx, dy, dz;
		public float ru, rv;
		public float angle;
		public float du, dv;
		public float offset;
		public int rings = 3;
		public int segs = 16;

		public Decal(float dx, float dy, float dz, float ru, float rv, float offset)
		{
			float l = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
			this.dx = dx / l;
			this.dy = dy / l;
			this.dz = dz / l;
			this.ru = ru;
			this.rv = rv;
			this.offset = offset;
		}

		public Decal copy()
		{
			Decal d = new Decal(dx, dy, dz, ru, rv, offset);
			d.angle = angle;
			d.du = du;
			d.dv = dv;
			d.rings = rings;
			d.segs = segs;
			return d;
		}

		public Decal size(float ru, float rv)
		{
			Decal d = copy();
			d.ru = ru;
			d.rv = rv;
			return d;
		}

		public Decal shift(float du, float dv)
		{
			Decal d = copy();
			d.du = du;
			d.dv = dv;
			return d;
		}

		public Decal res(int rings, int segs)
		{
			this.rings = rings;
			this.segs = segs;
			return this;
		}

		public Decal rotate(float angle)
		{
			this.angle = angle;
			return this;
		}

		int pointCount()
		{
			return 1 + rings * segs;
		}
	}

	public static float[] decalPoints(Ellipsoid e, Decal d)
	{
		float nx = d.dx, ny = d.dy, nz = d.dz;
		float ux = 0, uy = 1, uz = 0;
		if (Math.abs(ny) > 0.95f)
		{
			uy = 0;
			uz = 1;
		}
		float tx = uy * nz - uz * ny, ty = uz * nx - ux * nz, tz = ux * ny - uy * nx;
		float tl = (float) Math.sqrt(tx * tx + ty * ty + tz * tz);
		tx /= tl;
		ty /= tl;
		tz /= tl;
		float sx = ny * tz - nz * ty, sy = nz * tx - nx * tz, sz = nx * ty - ny * tx;

		float[] out = new float[d.pointCount() * 3];
		float[] p = new float[3];
		float ca = (float) Math.cos(d.angle), sa = (float) Math.sin(d.angle);
		int k = 0;
		for (int r = 0; r <= d.rings; r++)
		{
			int n = r == 0 ? 1 : d.segs;
			float f = (float) r / d.rings;
			for (int j = 0; j < n; j++)
			{
				float th = (float) (2 * Math.PI * j / d.segs);
				float u0 = d.ru * f * (float) Math.cos(th), v0 = d.rv * f * (float) Math.sin(th);
				float u = u0 * ca - v0 * sa + d.du;
				float v = u0 * sa + v0 * ca + d.dv;
				float qx = nx + u * tx + v * sx, qy = ny + u * ty + v * sy, qz = nz + u * tz + v * sz;
				float ql = (float) Math.sqrt(qx * qx + qy * qy + qz * qz);
				e.surface(qx / ql, qy / ql, qz / ql, d.offset, p);
				out[k++] = p[0];
				out[k++] = p[1];
				out[k++] = p[2];
			}
		}
		return out;
	}

	public static int[] decal(MeshBuilder mb, Ellipsoid e, Decal d, int bone, RingColor col, int flags)
	{
		float[] pts = decalPoints(e, d);
		int[] idx = new int[d.pointCount()];
		for (int i = 0; i < idx.length; i++)
		{
			idx[i] = mb.vertex(pts[i * 3], pts[i * 3 + 1], pts[i * 3 + 2], bone);
		}
		float[] n = new float[3];
		e.normal(d.dx, d.dy, d.dz, n);
		for (int r = 0; r < d.rings; r++)
		{
			float fMid = (r + 0.5f) / d.rings;
			for (int j = 0; j < d.segs; j++)
			{
				int j1 = (j + 1) % d.segs;
				float ang = (float) (2 * Math.PI * (j + 0.5f) / d.segs);
				int rgb = col.rgb(fMid, ang);
				if (r == 0)
				{
					mb.face(idx[0], idx[1 + j], idx[1 + j1], rgb, flags, n[0], n[1], n[2]);
				}
				else
				{
					int a0 = 1 + (r - 1) * d.segs + j, a1 = 1 + (r - 1) * d.segs + j1;
					int b0 = 1 + r * d.segs + j, b1 = 1 + r * d.segs + j1;
					mb.face(idx[a0], idx[b0], idx[b1], rgb, flags, n[0], n[1], n[2]);
					mb.face(idx[a0], idx[b1], idx[a1], rgb, flags, n[0], n[1], n[2]);
				}
			}
		}
		return idx;
	}

	// both decals need the same rings/segs
	public static void decalMorph(MeshBuilder mb, String morph, Ellipsoid e, int[] idx, Decal from, Decal to)
	{
		float[] a = decalPoints(e, from), b = decalPoints(e, to);
		for (int i = 0; i < idx.length; i++)
		{
			mb.morph(morph, idx[i], b[i * 3] - a[i * 3], b[i * 3 + 1] - a[i * 3 + 1], b[i * 3 + 2] - a[i * 3 + 2]);
		}
	}

	// radius 0 makes a cone
	public static void tube(MeshBuilder mb, float x0, float y0, float z0, float x1, float y1, float z1,
		float r0, float r1, int segs, int bone, int rgb, int flags)
	{
		float ax = x1 - x0, ay = y1 - y0, az = z1 - z0;
		float al = (float) Math.sqrt(ax * ax + ay * ay + az * az);
		ax /= al;
		ay /= al;
		az /= al;
		float px, py, pz;
		if (Math.abs(ay) < 0.9f)
		{
			px = -az;
			py = 0;
			pz = ax;
		}
		else
		{
			px = 0;
			py = az;
			pz = -ay;
		}
		float pl = (float) Math.sqrt(px * px + py * py + pz * pz);
		px /= pl;
		py /= pl;
		pz /= pl;
		float qx = ay * pz - az * py, qy = az * px - ax * pz, qz = ax * py - ay * px;

		int[] ring0 = ring(mb, x0, y0, z0, r0, segs, px, py, pz, qx, qy, qz, bone);
		int[] ring1 = ring(mb, x1, y1, z1, r1, segs, px, py, pz, qx, qy, qz, bone);
		for (int j = 0; j < segs; j++)
		{
			int j1 = (j + 1) % segs;
			float th = (float) (2 * Math.PI * (j + 0.5) / segs);
			float hx = (float) (px * Math.cos(th) + qx * Math.sin(th));
			float hy = (float) (py * Math.cos(th) + qy * Math.sin(th));
			float hz = (float) (pz * Math.cos(th) + qz * Math.sin(th));
			int a0 = ring0[ring0.length == 1 ? 0 : j], a1 = ring0[ring0.length == 1 ? 0 : j1];
			int b0 = ring1[ring1.length == 1 ? 0 : j], b1 = ring1[ring1.length == 1 ? 0 : j1];
			if (ring0.length > 1)
			{
				mb.face(a0, b0, a1, rgb, flags, hx, hy, hz);
			}
			if (ring1.length > 1)
			{
				mb.face(a1, b0, b1, rgb, flags, hx, hy, hz);
			}
		}
		if (ring0.length > 1)
		{
			int c = mb.vertex(x0, y0, z0, bone);
			for (int j = 0; j < segs; j++)
			{
				mb.face(c, ring0[j], ring0[(j + 1) % segs], rgb, flags, -ax, -ay, -az);
			}
		}
		if (ring1.length > 1)
		{
			int c = mb.vertex(x1, y1, z1, bone);
			for (int j = 0; j < segs; j++)
			{
				mb.face(c, ring1[j], ring1[(j + 1) % segs], rgb, flags, ax, ay, az);
			}
		}
	}

	private static int[] ring(MeshBuilder mb, float cx, float cy, float cz, float r, int segs,
		float px, float py, float pz, float qx, float qy, float qz, int bone)
	{
		if (r <= 0)
		{
			return new int[]{mb.vertex(cx, cy, cz, bone)};
		}
		int[] out = new int[segs];
		for (int j = 0; j < segs; j++)
		{
			double th = 2 * Math.PI * j / segs;
			float c = (float) Math.cos(th) * r, s = (float) Math.sin(th) * r;
			out[j] = mb.vertex(cx + px * c + qx * s, cy + py * c + qy * s, cz + pz * c + qz * s, bone);
		}
		return out;
	}
}
