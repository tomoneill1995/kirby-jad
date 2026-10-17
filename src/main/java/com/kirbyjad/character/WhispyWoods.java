package com.kirbyjad.character;

import static com.kirbyjad.character.Curve.TAU;
import static com.kirbyjad.character.Curve.blink;
import static com.kirbyjad.character.Curve.keys;
import static com.kirbyjad.character.Curve.sin;
import com.kirbyjad.mesh.Ellipsoid;
import com.kirbyjad.mesh.Mesh;
import com.kirbyjad.mesh.MeshBuilder;
import com.kirbyjad.mesh.Pose;
import com.kirbyjad.mesh.Rig;
import com.kirbyjad.mesh.Shapes;
import com.kirbyjad.mesh.Shapes.Decal;
import java.util.Random;

// giant whispy woods on the horizon. just sways, blinks and puffs now and then
public final class WhispyWoods extends Toon
{
	private static final int ROOT = 0, TRUNK = 1, CANOPY = 2;

	private final int mBlink;
	private final int mMouth;

	public WhispyWoods(float size)
	{
		super(build(size), new Rig(new int[]{-1, ROOT, TRUNK}, new float[]{0, 0, 0}, new float[]{0, 0, 2.2f * size},
			new float[]{0, 0, 0}));
		mBlink = mesh.morphIndex("blink");
		mMouth = mesh.morphIndex("mouth");
	}

	// S is the size, the tree is about 4 S tall
	private static Mesh build(float S)
	{
		MeshBuilder mb = new MeshBuilder();
		Random rng = new Random(7);
		Ellipsoid trunk = new Ellipsoid(0, 1.15f * S, 0, 0.55f * S, 1.2f * S, 0.52f * S);
		Shapes.ellipsoid(mb, trunk, 14, 24, TRUNK, Shapes.solid(0xa8703c), 0);

		// face on the trunk
		for (int s = -1; s <= 1; s += 2)
		{
			Decal eye = new Decal(s * 0.26f, 0.22f, 1, 0.1f, 0.22f, 3.0f).res(3, 16);
			int[] e = Shapes.decal(mb, trunk, eye, TRUNK, (f, a) -> 0x2a160a, 0);
			Shapes.decalMorph(mb, "blink", trunk, e, eye, eye.size(0.11f, 0.015f).shift(0, -0.05f));
		}
		Shapes.ellipsoid(mb, new Ellipsoid(0, 1.15f * S, 0.55f * S, 0.11f * S, 0.15f * S, 0.13f * S), 6, 10, TRUNK,
			Shapes.solid(0x9a6232), 0);
		Decal mouth = new Decal(0, -0.2f, 1, 0.12f, 0.04f, 3.0f).res(3, 18);
		int[] m = Shapes.decal(mb, trunk, mouth, TRUNK, (f, a) -> 0x3a1a0a, 0);
		Shapes.decalMorph(mb, "mouth", trunk, m, mouth, mouth.size(0.18f, 0.2f));

		// big round canopy
		Shapes.ellipsoid(mb, new Ellipsoid(0, 2.95f * S, 0, 1.35f * S, 0.85f * S, 1.25f * S), 12, 22, CANOPY,
			Shapes.solid(0x5fcb48), 0);
		for (int i = 0; i < 6; i++)
		{
			double a = TAU * i / 6 + rng.nextFloat() * 0.4;
			float r = (0.55f + rng.nextFloat() * 0.2f) * S;
			Shapes.ellipsoid(mb, new Ellipsoid((float) Math.cos(a) * 1.1f * S, 2.75f * S + rng.nextFloat() * 0.4f * S,
				(float) Math.sin(a) * 1.0f * S, r, r * 0.9f, r), 8, 14, CANOPY, Shapes.solid(i % 2 == 0 ? 0x74d955 : 0x5fcb48), 0);
		}
		// apples
		for (int i = 0; i < 8; i++)
		{
			double a = rng.nextDouble() * TAU;
			Shapes.ellipsoid(mb, new Ellipsoid((float) Math.cos(a) * 1.25f * S, 2.6f * S + rng.nextFloat() * 0.6f * S,
				(float) Math.sin(a) * 1.15f * S, 0.1f * S, 0.1f * S, 0.1f * S), 5, 8, CANOPY, Shapes.solid(0xf0384a), 0);
		}
		// roots
		for (int i = 0; i < 4; i++)
		{
			double a = TAU * i / 4 + 0.4;
			Shapes.ellipsoid(mb, new Ellipsoid((float) Math.cos(a) * 0.5f * S, 0.08f * S, (float) Math.sin(a) * 0.5f * S,
				0.35f * S, 0.12f * S, 0.18f * S, 0, (float) -a, 0), 6, 10, ROOT, Shapes.solid(0x96612f), 0);
		}
		return mb.build();
	}

	@Override
	public float duration(Anim anim)
	{
		return 0;
	}

	@Override
	public Anim[] showcase()
	{
		return new Anim[]{Anim.IDLE};
	}

	@Override
	public void pose(Anim anim, float t, float clock, Pose p)
	{
		p.reset();
		p.roll[TRUNK] = 0.015f * sin(TAU * clock / 6);
		p.roll[CANOPY] = 0.04f * sin(TAU * clock / 6 + 0.8f);
		p.pitch[CANOPY] = 0.02f * sin(TAU * clock / 4.3f);
		p.morph[mBlink] = blink(clock, 4.5f, 0);
		// a big puff every 12 seconds
		float puff = clock % 12;
		p.morph[mMouth] = keys(puff, 9, 0, 9.4f, 1, 10.6f, 1, 11.2f, 0);
	}
}
