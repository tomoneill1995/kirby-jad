package com.kirbyjad.mesh;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

public final class MeshBuilder
{
	private float[] vx = new float[512];
	private float[] vy = new float[512];
	private float[] vz = new float[512];
	private int[] vbone = new int[512];
	private int vc;

	private int[] fa = new int[1024];
	private int[] fb = new int[1024];
	private int[] fc = new int[1024];
	private int[] frgb = new int[1024];
	private int[] fflags = new int[1024];
	private int fcount;

	private final Map<String, MorphBuilder> morphs = new LinkedHashMap<>();

	private static final class MorphBuilder
	{
		int[] idx = new int[64];
		float[] dx = new float[64];
		float[] dy = new float[64];
		float[] dz = new float[64];
		int n;

		void add(int i, float x, float y, float z)
		{
			if (n == idx.length)
			{
				idx = Arrays.copyOf(idx, n * 2);
				dx = Arrays.copyOf(dx, n * 2);
				dy = Arrays.copyOf(dy, n * 2);
				dz = Arrays.copyOf(dz, n * 2);
			}
			idx[n] = i;
			dx[n] = x;
			dy[n] = y;
			dz[n] = z;
			n++;
		}
	}

	public int vertexCount()
	{
		return vc;
	}

	public float vx(int i)
	{
		return vx[i];
	}

	public float vy(int i)
	{
		return vy[i];
	}

	public float vz(int i)
	{
		return vz[i];
	}

	public int vertex(float x, float y, float z, int bone)
	{
		if (vc == vx.length)
		{
			int n = vc * 2;
			vx = Arrays.copyOf(vx, n);
			vy = Arrays.copyOf(vy, n);
			vz = Arrays.copyOf(vz, n);
			vbone = Arrays.copyOf(vbone, n);
		}
		vx[vc] = x;
		vy[vc] = y;
		vz[vc] = z;
		vbone[vc] = bone;
		return vc++;
	}

	// flips the winding if needed so the face points along (hx, hy, hz)
	public void face(int a, int b, int c, int rgb, int flags, float hx, float hy, float hz)
	{
		float ux = vx[b] - vx[a], uy = vy[b] - vy[a], uz = vz[b] - vz[a];
		float wx = vx[c] - vx[a], wy = vy[c] - vy[a], wz = vz[c] - vz[a];
		float nx = uy * wz - uz * wy, ny = uz * wx - ux * wz, nz = ux * wy - uy * wx;
		if (nx * hx + ny * hy + nz * hz < 0)
		{
			int t = b;
			b = c;
			c = t;
		}
		if (fcount == fa.length)
		{
			int n = fcount * 2;
			fa = Arrays.copyOf(fa, n);
			fb = Arrays.copyOf(fb, n);
			fc = Arrays.copyOf(fc, n);
			frgb = Arrays.copyOf(frgb, n);
			fflags = Arrays.copyOf(fflags, n);
		}
		fa[fcount] = a;
		fb[fcount] = b;
		fc[fcount] = c;
		frgb[fcount] = rgb;
		fflags[fcount] = flags;
		fcount++;
	}

	public void morph(String name, int vertex, float dx, float dy, float dz)
	{
		morphs.computeIfAbsent(name, k -> new MorphBuilder()).add(vertex, dx, dy, dz);
	}

	public Mesh build()
	{
		int m = morphs.size();
		String[] names = new String[m];
		int[][] idx = new int[m][];
		float[][] dx = new float[m][], dy = new float[m][], dz = new float[m][];
		int i = 0;
		for (Map.Entry<String, MorphBuilder> e : morphs.entrySet())
		{
			MorphBuilder mb = e.getValue();
			names[i] = e.getKey();
			idx[i] = Arrays.copyOf(mb.idx, mb.n);
			dx[i] = Arrays.copyOf(mb.dx, mb.n);
			dy[i] = Arrays.copyOf(mb.dy, mb.n);
			dz[i] = Arrays.copyOf(mb.dz, mb.n);
			i++;
		}
		return new Mesh(vc, Arrays.copyOf(vx, vc), Arrays.copyOf(vy, vc), Arrays.copyOf(vz, vc), Arrays.copyOf(vbone, vc),
			fcount, Arrays.copyOf(fa, fcount), Arrays.copyOf(fb, fcount), Arrays.copyOf(fc, fcount),
			Arrays.copyOf(frgb, fcount), Arrays.copyOf(fflags, fcount),
			names, idx, dx, dy, dz);
	}
}
