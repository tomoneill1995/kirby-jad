package com.kirbyjad.character;

import static com.kirbyjad.character.Curve.keys;
import static com.kirbyjad.character.Curve.sin;
import com.kirbyjad.mesh.Ellipsoid;
import com.kirbyjad.mesh.Mesh;
import com.kirbyjad.mesh.MeshBuilder;
import com.kirbyjad.mesh.Pose;
import com.kirbyjad.mesh.Shapes;
import com.kirbyjad.mesh.Shapes.Decal;
import java.util.LinkedHashMap;
import java.util.Map;

// yt-mejkot. one big eye, whips a beam at you, hops when it heals
public final class WaddleDoo extends Critter
{
	private static final float R = 140;

	public WaddleDoo()
	{
		super(build(), R, 0, durations());
	}

	private static Map<Anim, Float> durations()
	{
		Map<Anim, Float> d = new LinkedHashMap<>();
		d.put(Anim.ATTACK, 0.76f);
		d.put(Anim.HEAL, 0.76f);
		d.put(Anim.DEFEND, 1.22f);
		d.put(Anim.DEATH, 1.6f);
		return d;
	}

	private static Mesh build()
	{
		MeshBuilder mb = new MeshBuilder();
		Ellipsoid body = body(mb, R, 0xec7b3c, 0);

		// the eye: white, blue, pupil, shine
		Decal white = new Decal(0, 0.1f, 1, 0.28f, 0.33f, 2.0f).res(4, 24);
		int[] w = Shapes.decal(mb, body, white, BODY, (f, a) -> 0xffffff, 0);
		Shapes.decalMorph(mb, "blink", body, w, white, white.size(0.3f, 0.02f).shift(0, -0.06f));
		Decal iris = white.size(0.17f, 0.2f).shift(0, -0.02f).res(3, 20);
		iris.offset = 3.0f;
		int[] ir = Shapes.decal(mb, body, iris, BODY, (f, a) -> f < 0.6f ? 0x2a5ad0 : 0x3a7ae0, 0);
		Shapes.decalMorph(mb, "blink", body, ir, iris, iris.size(0.17f, 0.01f).shift(0, -0.06f));
		Decal pupil = white.size(0.08f, 0.1f).shift(0, -0.02f).res(2, 14);
		pupil.offset = 4.0f;
		int[] pp = Shapes.decal(mb, body, pupil, BODY, (f, a) -> 0x0e0e18, 0);
		Shapes.decalMorph(mb, "blink", body, pp, pupil, pupil.size(0.08f, 0.005f).shift(0, -0.06f));
		Decal shine = white.size(0.05f, 0.06f).shift(0.06f, 0.1f).res(2, 10);
		shine.offset = 5.0f;
		int[] sh = Shapes.decal(mb, body, shine, BODY, (f, a) -> 0xffffff, 0);
		Shapes.decalMorph(mb, "blink", body, sh, shine, shine.size(0.04f, 0.005f).shift(-0.05f, -0.13f));

		arms(mb, R, 0xec7b3c);
		feet(mb, R, 0xf5b81e);

		// eye beam: a string of sparkly balls, scaled to nothing unless attacking
		for (int i = 0; i < 6; i++)
		{
			float z = (1.1f + i * 0.28f) * R;
			float size = (0.12f - i * 0.012f) * R;
			Shapes.ellipsoid(mb, new Ellipsoid(0, 1.06f * R, z, size, size, size), 5, 8, EXTRA,
				Shapes.solid(i % 2 == 0 ? 0xfff27a : 0xffffff), Mesh.FLAG_EMISSIVE);
		}
		return mb.build();
	}

	@Override
	void special(Anim anim, float t, float clock, Pose p)
	{
		p.scale(EXTRA, 0);
		if (anim == Anim.ATTACK)
		{
			// beam whips across
			float on = keys(t, 0, 0, 0.15f, 1, 0.6f, 1, 0.76f, 0);
			p.scale(EXTRA, on);
			p.yaw[EXTRA] = keys(t, 0.1f, -0.7f, 0.6f, 0.7f);
			p.glow = 1.1f + 0.15f * sin(t * 60);
			p.morph[mBlink] = 0;
			p.pitch[BODY] = 0.1f * on;
		}
		else if (anim == Anim.HEAL)
		{
			float k = keys(t, 0, 0, 0.2f, 1, 0.55f, 1, 0.76f, 0);
			p.ty[ROOT] = 0.35f * R * k;
			armUp(p, ARM_A, 1.4f * k);
			armUp(p, ARM_B, 1.4f * k);
		}
	}
}
