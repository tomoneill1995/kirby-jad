package com.kirbyjad.character;

import static com.kirbyjad.character.Curve.TAU;
import static com.kirbyjad.character.Curve.keys;
import com.kirbyjad.mesh.Ellipsoid;
import com.kirbyjad.mesh.Mesh;
import com.kirbyjad.mesh.MeshBuilder;
import com.kirbyjad.mesh.Pose;
import com.kirbyjad.mesh.Shapes;
import com.kirbyjad.mesh.Shapes.Decal;
import java.util.LinkedHashMap;
import java.util.Map;

// tok-xil. spikes fire out for range, rolls into you for melee
public final class Gordo extends Critter
{
	private static final float R = 150;

	public Gordo()
	{
		super(build(), R, 40, durations());
	}

	private static Map<Anim, Float> durations()
	{
		Map<Anim, Float> d = new LinkedHashMap<>();
		d.put(Anim.RANGED, 1.58f);
		d.put(Anim.MELEE, 0.74f);
		d.put(Anim.DEFEND, 1.3f);
		d.put(Anim.DEATH, 1.6f);
		return d;
	}

	private static Mesh build()
	{
		MeshBuilder mb = new MeshBuilder();
		Ellipsoid body = new Ellipsoid(0, R, 0, R, R, R);
		Shapes.ellipsoid(mb, body, 16, 28, BODY, Shapes.solid(0x34344a), 0);

		// spikes spread evenly over the ball, on their own bone so they can shoot out
		int n = 26;
		double golden = Math.PI * (3 - Math.sqrt(5));
		for (int i = 0; i < n; i++)
		{
			float y = 1 - 2 * (i + 0.5f) / n;
			float rad = (float) Math.sqrt(1 - y * y);
			float x = (float) Math.cos(golden * i) * rad;
			float z = (float) Math.sin(golden * i) * rad;
			// leave the face clear
			if (z > 0.75f && Math.abs(y) < 0.45f)
			{
				continue;
			}
			Shapes.tube(mb, x * 0.85f * R, R + y * 0.85f * R, z * 0.85f * R, x * 1.55f * R, R + y * 1.55f * R, z * 1.55f * R,
				0.17f * R, 0, 8, EXTRA, 0xe9e9f2, 0);
		}

		// big white eyes with black pupils
		for (int s = -1; s <= 1; s += 2)
		{
			Decal white = new Decal(s * 0.22f, 0.12f, 1, 0.17f, 0.27f, 2.0f).res(3, 18);
			int[] w = Shapes.decal(mb, body, white, BODY, (f, a) -> 0xffffff, 0);
			Shapes.decalMorph(mb, "blink", body, w, white, white.size(0.18f, 0.02f).shift(0, -0.08f));
			Decal pupil = white.size(0.08f, 0.14f).shift(s * 0.01f, -0.03f).res(2, 12);
			pupil.offset = 3.0f;
			int[] pp = Shapes.decal(mb, body, pupil, BODY, (f, a) -> 0x101018, 0);
			Shapes.decalMorph(mb, "blink", body, pp, pupil, pupil.size(0.08f, 0.01f).shift(0, -0.08f));
		}
		return mb.build();
	}

	@Override
	void walk(float clock, Pose p)
	{
		// just floats along, rolling a bit
		p.pitch[BODY] = 0.15f;
	}

	@Override
	void special(Anim anim, float t, float clock, Pose p)
	{
		// spikes and body turn together
		p.pitch[EXTRA] = p.pitch[BODY];
		if (anim == Anim.RANGED)
		{
			// pull the spikes in, then fire them out
			float in = keys(t, 0, 0, 0.45f, 1, 0.6f, 0);
			float out = keys(t, 0.55f, 0, 0.7f, 1, 1.2f, 1, 1.58f, 0);
			p.scale(EXTRA, 1 - 0.4f * in + 0.5f * out);
			p.scale(BODY, 1 - 0.08f * in + 0.06f * out);
			p.morph[mBlink] = Math.max(p.morph[mBlink], in);
		}
		else if (anim == Anim.MELEE)
		{
			// roll forward into the player and back
			float k = keys(t, 0, 0, 0.3f, 1, 0.74f, 0);
			float spin = TAU * 0.5f * k;
			p.pitch[BODY] = p.pitch[EXTRA] = spin;
			// body and spikes pivot at the ground, move them so the ball turns around its middle
			float dy = R - R * (float) Math.cos(spin);
			float dz = -R * (float) Math.sin(spin);
			p.ty[BODY] = p.ty[EXTRA] = dy;
			p.tz[BODY] = p.tz[EXTRA] = dz;
			p.tz[ROOT] = 0.6f * R * k;
		}
	}
}
