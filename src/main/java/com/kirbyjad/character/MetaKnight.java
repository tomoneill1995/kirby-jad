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

// ket-zek. mage: cape opens into wings and the sword glows overhead. melee: sword slash
public final class MetaKnight extends Critter
{
	private static final float R = 190;

	public MetaKnight()
	{
		super(build(), R, 0, durations());
	}

	private static Map<Anim, Float> durations()
	{
		Map<Anim, Float> d = new LinkedHashMap<>();
		d.put(Anim.MAGIC, 1.3f);
		d.put(Anim.MELEE, 1.2f);
		d.put(Anim.DEFEND, 1.16f);
		d.put(Anim.DEATH, 1.6f);
		return d;
	}

	private static Mesh build()
	{
		MeshBuilder mb = new MeshBuilder();
		Ellipsoid body = body(mb, R, 0x283a8c, 0);

		// mask, the dark visor slit and the yellow eyes
		Shapes.decal(mb, body, new Decal(0, 0.04f, 1, 0.6f, 0.62f, 1.0f).res(5, 30), BODY, (f, a) -> 0xc4ccdc, 0);
		Decal slit = new Decal(0, 0.1f, 1, 0.42f, 0.17f, 2.4f).res(3, 24);
		Shapes.decal(mb, body, slit, BODY, (f, a) -> 0x101020, 0);
		for (int s = -1; s <= 1; s += 2)
		{
			Decal eye = new Decal(s * 0.17f, 0.1f, 1, 0.06f, 0.09f, 3.6f).res(2, 14);
			int[] e = Shapes.decal(mb, body, eye, BODY, (f, a) -> 0xffe14a, Mesh.FLAG_EMISSIVE);
			Shapes.decalMorph(mb, "blink", body, e, eye, eye.size(0.07f, 0.01f));
			// shoulder pads
			Shapes.ellipsoid(mb, new Ellipsoid(s * 0.8f * R, 1.35f * R, 0, 0.28f * R, 0.16f * R, 0.3f * R, 0, 0, -s * 0.35f), 7, 12, BODY,
				Shapes.solid(0xc4ccdc), 0);
		}

		arms(mb, R, 0x9aa4c0);
		feet(mb, R, 0x8a3cb0);

		// cape behind, opens into bat wings for the mage attack
		Shapes.ellipsoid(mb, new Ellipsoid(0, 1.0f * R, -0.8f * R, 1.2f * R, 1.0f * R, 0.16f * R), 10, 20, EXTRA,
			(x, y, z) -> z > 0 ? 0x8a1c3a : 0x1c2a6a, 0);

		// galaxia, held point up in hand b
		float hx = 1.05f * R;
		float hz = 0.25f * R;
		Shapes.tube(mb, hx, 0.55f * R, hz, hx, 0.85f * R, hz, 0.06f * R, 0.06f * R, 7, HELD, 0x6a4ac0, 0);
		Shapes.ellipsoid(mb, new Ellipsoid(hx, 0.88f * R, hz, 0.24f * R, 0.06f * R, 0.1f * R), 5, 10, HELD, Shapes.solid(0xf2c84a), 0);
		Shapes.tube(mb, hx, 0.9f * R, hz, hx, 2.4f * R, hz, 0.1f * R, 0, 8, HELD, 0xffe27a, Mesh.FLAG_EMISSIVE);
		return mb.build();
	}

	@Override
	void special(Anim anim, float t, float clock, Pose p)
	{
		p.glow = 0.95f;
		if (anim == Anim.MAGIC)
		{
			// rises with the cape spread like wings and the sword held up glowing
			float k = keys(t, 0, 0, 0.3f, 1, 1.0f, 1, 1.3f, 0);
			p.ty[ROOT] = 0.5f * R * k;
			p.sx[EXTRA] = 1 + 1.0f * k;
			p.sy[EXTRA] = 1 + 0.3f * k;
			p.ty[EXTRA] = 0.2f * R * k;
			armUp(p, ARM_B, 1.6f * k);
			p.roll[HELD] = -1.4f * k;
			p.glow = 1 + 0.25f * k + 0.1f * k * sin(t * 40);
		}
		else if (anim == Anim.MELEE)
		{
			// wind up then a big slash across
			float wind = keys(t, 0, 0, 0.35f, 1, 0.45f, 0);
			float slash = keys(t, 0.35f, 0, 0.55f, 1, 0.9f, 1, 1.2f, 0);
			p.yaw[BODY] = -0.5f * wind + 0.5f * slash;
			p.tz[ROOT] = 0.35f * R * slash;
			armUp(p, ARM_B, 1.2f * wind + 0.3f * slash);
			p.pitch[ARM_B] = -0.8f * wind + 1.5f * slash;
		}
	}
}
