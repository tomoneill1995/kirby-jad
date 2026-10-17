package com.kirbyjad.character;

import static com.kirbyjad.character.Curve.TAU;
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

// tz-kek. the big one and the two little ones it splits into use different sizes
public final class Scarfy extends Critter
{
	private final int mMouth;

	public Scarfy(float radius)
	{
		super(build(radius), radius, radius * 0.5f, durations());
		mMouth = mesh.morphIndex("mouth");
	}

	private static Map<Anim, Float> durations()
	{
		Map<Anim, Float> d = new LinkedHashMap<>();
		d.put(Anim.ATTACK, 1.16f);
		d.put(Anim.DEFEND, 0.74f);
		d.put(Anim.DEATH, 1.6f);
		return d;
	}

	private static Mesh build(float r)
	{
		MeshBuilder mb = new MeshBuilder();
		int fur = 0xf6b46a;
		Ellipsoid body = body(mb, r, fur, 0);
		eyes(mb, body, 0.25f, 0.12f, 0.13f, 0.23f, 0x2a1a40);
		for (int s = -1; s <= 1; s += 2)
		{
			Shapes.decal(mb, body, new Decal(s * 0.45f, -0.06f, 0.9f, 0.11f, 0.06f, 1.2f).res(2, 14), BODY, (f, a) -> 0xf27a7a, 0);
			// big floppy ears that it flies with
			Ellipsoid ear = new Ellipsoid(s * 0.95f * r, 1.45f * r, -0.1f * r, 0.55f * r, 0.2f * r, 0.36f * r, 0, 0, -s * 0.75f);
			Shapes.ellipsoid(mb, ear, 6, 14, s < 0 ? ARM_A : ARM_B, Shapes.solid(fur), 0);
		}
		// tiny smile that opens into a big angry mouth
		Decal mouth = new Decal(0, -0.12f, 1, 0.06f, 0.03f, 2.0f).res(3, 20);
		int[] m = Shapes.decal(mb, body, mouth, BODY, (f, a) -> f < 0.6f ? 0x5a0616 : 0x9a1028, 0);
		Shapes.decalMorph(mb, "mouth", body, m, mouth, mouth.size(0.3f, 0.22f).shift(0, -0.08f));
		return mb.build();
	}

	@Override
	void special(Anim anim, float t, float clock, Pose p)
	{
		float flap = 0.35f * sin(TAU * clock / 0.5f);
		armUp(p, ARM_A, flap);
		armUp(p, ARM_B, flap);

		if (anim == Anim.ATTACK)
		{
			// goes angry and chomps
			float k = keys(t, 0, 0, 0.3f, 1, 0.8f, 1, 1.16f, 0);
			float bite = keys(t, 0.3f, 0, 0.5f, 1, 0.8f, 0);
			p.morph[mMouth] = k;
			p.scale(BODY, 1 + 0.15f * k);
			p.tz[ROOT] = 0.5f * r * bite;
			p.morph[mBlink] = Math.max(p.morph[mBlink], 0.5f * k);
		}
	}
}
