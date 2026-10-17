package com.kirbyjad.character;

import com.kirbyjad.mesh.Ellipsoid;
import com.kirbyjad.mesh.Mesh;
import com.kirbyjad.mesh.MeshBuilder;
import com.kirbyjad.mesh.Pose;
import com.kirbyjad.mesh.Rig;
import com.kirbyjad.mesh.Shapes;
import java.util.Random;

// static dreamland scenery. one bone, never animated (stars just get spun by orientation)
public final class Prop
{
	public final Mesh mesh;
	public final Rig rig;

	private Prop(Mesh mesh)
	{
		this.mesh = mesh;
		this.rig = new Rig(new int[]{-1}, new float[]{0}, new float[]{0}, new float[]{0});
	}

	public Pose newPose()
	{
		return new Pose(1, mesh.morphNames.length);
	}


	// a clump of round grassy domes, about `radius` units across from the middle
	public static Prop hill(float radius, long seed)
	{
		Random rng = new Random(seed);
		MeshBuilder mb = new MeshBuilder();
		// a few shades of green so neighbouring clumps don't all match
		int[] greens = {0x7fdc4f, 0x72d148, 0x8de35c};
		int base = greens[rng.nextInt(greens.length)];
		int light = mix(base, 0xc8f57a, 0.35f);

		Ellipsoid main = new Ellipsoid(0, 0.05f * radius, 0, radius, 0.85f * radius, radius);
		Shapes.ellipsoid(mb, main, 11, 18, 0, Shapes.solid(base), 0);
		flowers(mb, main, rng, 4 + rng.nextInt(5));

		int bumps = 2 + rng.nextInt(3);
		for (int i = 0; i < bumps; i++)
		{
			double a = rng.nextDouble() * Math.PI * 2;
			float d = radius * (0.55f + rng.nextFloat() * 0.35f);
			float r = radius * (0.45f + rng.nextFloat() * 0.25f);
			Ellipsoid bump = new Ellipsoid((float) Math.cos(a) * d, 0, (float) Math.sin(a) * d, r, r * 0.9f, r);
			Shapes.ellipsoid(mb, bump, 8, 14, 0, Shapes.solid(i % 2 == 0 ? light : base), 0);
			flowers(mb, bump, rng, 1 + rng.nextInt(3));
		}
		return new Prop(mb.build());
	}

	// little flowers dotted over the top of a clump
	private static void flowers(MeshBuilder mb, Ellipsoid on, Random rng, int count)
	{
		int[] colors = {0xffffff, 0xff9fd0, 0xffe25a};
		float[] p = new float[3];
		for (int i = 0; i < count; i++)
		{
			double a = rng.nextDouble() * Math.PI * 2;
			double up = 0.35 + rng.nextDouble() * 0.9;
			float dx = (float) (Math.cos(a) * Math.cos(up));
			float dy = (float) Math.sin(up);
			float dz = (float) (Math.sin(a) * Math.cos(up));
			on.surface(dx, dy, dz, 2, p);
			float size = Math.max(7, on.rx * 0.075f);
			int color = colors[rng.nextInt(colors.length)];
			Shapes.ellipsoid(mb, new Ellipsoid(p[0], p[1], p[2], size, size * 0.6f, size), 3, 6, 0, Shapes.solid(color), 0);
			Shapes.ellipsoid(mb, new Ellipsoid(p[0], p[1] + size * 0.4f, p[2], size * 0.4f, size * 0.3f, size * 0.4f), 2, 5, 0,
				Shapes.solid(color == 0xffe25a ? 0xff8a3a : 0xffd53a), 0);
		}
	}

	private static int mix(int a, int b, float t)
	{
		int ar = a >> 16 & 255;
		int ag = a >> 8 & 255;
		int ab = a & 255;
		return (int) (ar + ((b >> 16 & 255) - ar) * t) << 16
			| (int) (ag + ((b >> 8 & 255) - ag) * t) << 8
			| (int) (ab + ((b & 255) - ab) * t);
	}

	// lollipop tree: round canopy on a stubby trunk
	// one tile's worth of puffy hedge. a row of these side by side merges into one rounded hedge
	public static Prop hedge(long seed)
	{
		Random rng = new Random(seed);
		MeshBuilder mb = new MeshBuilder();
		int base = rng.nextBoolean() ? 0x6fd24c : 0x66c946;
		for (int i = -1; i <= 1; i++)
		{
			// stays within about a tile so it doesn't spill onto the path
			float r = 44 + rng.nextFloat() * 10;
			Ellipsoid puff = new Ellipsoid(i * 30, 58 + rng.nextFloat() * 12, (rng.nextFloat() - 0.5f) * 14, r, r * 1.2f, r);
			Shapes.ellipsoid(mb, puff, 8, 12, 0, Shapes.solid(i == 0 ? mix(base, 0xc8f57a, 0.2f) : base), 0);
			if (rng.nextInt(3) == 0)
			{
				flowers(mb, puff, rng, 1);
			}
		}
		return new Prop(mb.build());
	}

	// kirby style mesa: tan cliff with diagonal stripes and a rounded grassy cap that overhangs a little
	public static Prop mesa(float radius, float height, long seed)
	{
		Random rng = new Random(seed);
		MeshBuilder mb = new MeshBuilder();
		int segs = 26;
		int levels = 6;
		float top = radius * 0.86f;
		int[][] ring = new int[levels + 1][segs];
		for (int l = 0; l <= levels; l++)
		{
			float k = (float) l / levels;
			float r = radius + (top - radius) * k;
			for (int i = 0; i < segs; i++)
			{
				double a = Math.PI * 2 * i / segs;
				ring[l][i] = mb.vertex((float) Math.cos(a) * r, k * height, (float) Math.sin(a) * r, 0);
			}
		}
		for (int l = 0; l < levels; l++)
		{
			for (int i = 0; i < segs; i++)
			{
				int j = (i + 1) % segs;
				// stripes that wind round the cliff
				int rgb = ((i + l * 2) / 3 & 1) == 0 ? 0xeab872 : 0xc98d4c;
				double a = Math.PI * 2 * (i + 0.5) / segs;
				float hx = (float) Math.cos(a);
				float hz = (float) Math.sin(a);
				mb.face(ring[l][i], ring[l + 1][i], ring[l + 1][j], rgb, 0, hx, 0, hz);
				mb.face(ring[l][i], ring[l + 1][j], ring[l][j], rgb, 0, hx, 0, hz);
			}
		}

		// grassy cap, a bit wider than the top of the cliff
		Ellipsoid cap = new Ellipsoid(0, height, 0, top * 1.12f, radius * 0.4f, top * 1.12f);
		Shapes.ellipsoid(mb, cap, 9, 22, 0, Shapes.solid(0x72d14a), 0);
		flowers(mb, cap, rng, 3 + rng.nextInt(4));
		return new Prop(mb.build());
	}

	public static Prop tree(float size, long seed)
	{
		Random rng = new Random(seed);
		MeshBuilder mb = new MeshBuilder();
		Shapes.tube(mb, 0, 0, 0, 0, 1.25f * size, 0, 0.16f * size, 0.11f * size, 10, 0, 0xa8703c, 0);
		Shapes.DirColor leaves = Shapes.solid(0x6fd64a);
		Shapes.ellipsoid(mb, new Ellipsoid(0, 1.6f * size, 0, 0.72f * size, 0.66f * size, 0.72f * size), 12, 20, 0, leaves, 0);
		for (int i = 0; i < 2; i++)
		{
			double a = rng.nextDouble() * Math.PI * 2;
			Shapes.ellipsoid(mb, new Ellipsoid((float) Math.cos(a) * 0.55f * size, 1.35f * size, (float) Math.sin(a) * 0.55f * size,
				0.38f * size, 0.36f * size, 0.38f * size), 9, 14, 0, leaves, 0);
		}
		// a few apples
		for (int i = 0; i < 3; i++)
		{
			double a = rng.nextDouble() * Math.PI * 2;
			double up = 0.2 + rng.nextDouble() * 0.6;
			float x = (float) (Math.cos(a) * Math.cos(up)) * 0.74f * size;
			float y = 1.6f * size + (float) Math.sin(up) * 0.66f * size;
			float z = (float) (Math.sin(a) * Math.cos(up)) * 0.74f * size;
			Shapes.ellipsoid(mb, new Ellipsoid(x, y, z, 0.07f * size, 0.07f * size, 0.07f * size), 5, 8, 0, Shapes.solid(0xf0384a), 0);
		}
		return new Prop(mb.build());
	}

	public static Prop flower(int petals)
	{
		MeshBuilder mb = new MeshBuilder();
		float h = 34;
		Shapes.tube(mb, 0, 0, 0, 0, h, 0, 2.2f, 2.2f, 5, 0, 0x4fb33d, 0);
		Shapes.ellipsoid(mb, new Ellipsoid(4, 10, 0, 7, 2, 4, 0, 0, -0.5f), 4, 8, 0, Shapes.solid(0x5cc845), 0);
		for (int i = 0; i < 5; i++)
		{
			double a = Math.PI * 2 * i / 5;
			float x = (float) Math.cos(a) * 7;
			float z = (float) Math.sin(a) * 7;
			Shapes.ellipsoid(mb, new Ellipsoid(x, h, z, 6, 2.5f, 6), 5, 10, 0, Shapes.solid(petals), 0);
		}
		Shapes.ellipsoid(mb, new Ellipsoid(0, h + 1.5f, 0, 4.5f, 3.5f, 4.5f), 5, 10, 0, Shapes.solid(0xffd53a), 0);
		return new Prop(mb.build());
	}

	public static Prop cloud(long seed)
	{
		return cloud(seed, 0xffffff, 1);
	}

	// tinted and scaled up for the big sunset clouds round the horizon
	public static Prop cloud(long seed, int color, float scale)
	{
		Random rng = new Random(seed);
		MeshBuilder mb = new MeshBuilder();
		Shapes.DirColor tint = Shapes.solid(color);
		int puffs = 4 + rng.nextInt(3);
		for (int i = 0; i < puffs; i++)
		{
			float x = ((i - (puffs - 1) / 2f) * 150 + rng.nextFloat() * 40) * scale;
			float r = (140 + rng.nextFloat() * 90 - Math.abs(i - (puffs - 1) / 2f) * 25) * scale;
			Shapes.ellipsoid(mb, new Ellipsoid(x, r * 0.3f, (rng.nextFloat() * 60 - 30) * scale, r, r * 0.8f, r * 0.9f), 10, 16, 0,
				tint, 0);
		}
		return new Prop(mb.build());
	}

	// half ring of coloured bands standing upright, faces both ways
	public static Prop rainbow(float radius, float band)
	{
		int[] colors = {0xff5a5a, 0xffa04a, 0xffe45a, 0x7de36a, 0x5ac8ff, 0x6a7aff, 0xb46aff};
		MeshBuilder mb = new MeshBuilder();
		int segs = 40;
		for (int k = 0; k < colors.length; k++)
		{
			float outer = radius - k * band;
			float inner = outer - band;
			int[] o = new int[segs + 1];
			int[] in = new int[segs + 1];
			for (int i = 0; i <= segs; i++)
			{
				double a = Math.PI * i / segs;
				float c = (float) Math.cos(a);
				float sn = (float) Math.sin(a);
				o[i] = mb.vertex(c * outer, sn * outer, 0, 0);
				in[i] = mb.vertex(c * inner, sn * inner, 0, 0);
			}
			for (int i = 0; i < segs; i++)
			{
				for (int side = -1; side <= 1; side += 2)
				{
					mb.face(o[i], o[i + 1], in[i + 1], colors[k], Mesh.FLAG_EMISSIVE, 0, 0, side);
					mb.face(o[i], in[i + 1], in[i], colors[k], Mesh.FLAG_EMISSIVE, 0, 0, side);
				}
			}
		}
		return new Prop(mb.build());
	}

	// puffy 5 point warp star, faces +z
	public static Prop star(float size)
	{
		MeshBuilder mb = new MeshBuilder();
		int points = 5;
		float outer = size;
		float inner = size * 0.48f;
		float depth = size * 0.22f;
		int[] front = new int[points * 2];
		int[] back = new int[points * 2];
		for (int i = 0; i < points * 2; i++)
		{
			double a = Math.PI / 2 + Math.PI * i / points;
			float r = i % 2 == 0 ? outer : inner;
			float x = (float) Math.cos(a) * r;
			float y = (float) Math.sin(a) * r;
			front[i] = mb.vertex(x, y, depth * 0.35f, 0);
			back[i] = mb.vertex(x, y, -depth * 0.35f, 0);
		}
		int fc = mb.vertex(0, 0, depth, 0);
		int bc = mb.vertex(0, 0, -depth, 0);
		int yellow = 0xffde3a;
		int edge = 0xf5b81e;
		for (int i = 0; i < points * 2; i++)
		{
			int j = (i + 1) % (points * 2);
			mb.face(fc, front[i], front[j], yellow, 0, 0, 0, 1);
			mb.face(bc, back[j], back[i], yellow, 0, 0, 0, -1);
			float nx = mb.vx(front[i]) + mb.vx(front[j]);
			float ny = mb.vy(front[i]) + mb.vy(front[j]);
			mb.face(front[i], back[i], back[j], edge, 0, nx, ny, 0);
			mb.face(front[i], back[j], front[j], edge, 0, nx, ny, 0);
		}
		return new Prop(mb.build());
	}
}
