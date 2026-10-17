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

// tz-kih
public final class BrontoBurt extends Critter
{
	private static final float R = 48;

	public BrontoBurt()
	{
		super(build(), R, 70, durations());
	}

	private static Map<Anim, Float> durations()
	{
		Map<Anim, Float> d = new LinkedHashMap<>();
		d.put(Anim.ATTACK, 0.6f);
		d.put(Anim.DEFEND, 0.6f);
		d.put(Anim.DEATH, 1.6f);
		return d;
	}

	private static Mesh build()
	{
		MeshBuilder mb = new MeshBuilder();
		Ellipsoid body = body(mb, R, 0xf58ac0, 0);
		eyes(mb, body, 0.22f, 0.12f, 0.085f, 0.19f, 0x1a1020);
		for (int s = -1; s <= 1; s += 2)
		{
			Shapes.decal(mb, body, new Decal(s * 0.45f, -0.05f, 0.9f, 0.1f, 0.055f, 1.2f).res(2, 14), BODY, (f, a) -> 0xf0508a, 0);
			// wings, on the arm bones so they can flap
			Ellipsoid wing = new Ellipsoid(s * 1.1f * R, 1.3f * R, -0.25f * R, 0.6f * R, 0.12f * R, 0.4f * R, 0, 0, -s * 0.45f);
			Shapes.ellipsoid(mb, wing, 6, 14, s < 0 ? ARM_A : ARM_B, Shapes.solid(0xffffff), 0);
		}
		feet(mb, R, 0xff7a3a);
		return mb.build();
	}

	@Override
	void special(Anim anim, float t, float clock, Pose p)
	{
		float flap = 0.6f * sin(TAU * clock / 0.25f);
		armUp(p, ARM_A, flap);
		armUp(p, ARM_B, flap);
		p.ty[FOOT_A] = p.ty[FOOT_B] = 0.05f * R;

		if (anim == Anim.ATTACK)
		{
			// dive bomb
			float k = keys(t, 0, 0, 0.25f, 1, 0.6f, 0);
			p.tz[ROOT] = 0.9f * R * k;
			p.ty[ROOT] -= 0.5f * R * k;
			p.pitch[ROOT] = 0.6f * k;
		}
	}
}
