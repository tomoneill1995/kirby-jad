package com.kirbyjad.character;

import static com.kirbyjad.character.Curve.TAU;
import static com.kirbyjad.character.Curve.blink;
import static com.kirbyjad.character.Curve.clamp01;
import static com.kirbyjad.character.Curve.cos;
import static com.kirbyjad.character.Curve.easeIn;
import static com.kirbyjad.character.Curve.keys;
import static com.kirbyjad.character.Curve.sin;
import static com.kirbyjad.character.Curve.smooth;
import com.kirbyjad.mesh.Ellipsoid;
import com.kirbyjad.mesh.Mesh;
import com.kirbyjad.mesh.MeshBuilder;
import com.kirbyjad.mesh.Pose;
import com.kirbyjad.mesh.Rig;
import com.kirbyjad.mesh.Shapes;
import com.kirbyjad.mesh.Shapes.Decal;

public final class WaddleDee extends Toon
{
	static final float R = 60;

	static final int ROOT = 0, BODY = 1, ARM_A = 2, ARM_B = 3, FOOT_A = 4, FOOT_B = 5, SPEAR = 6;

	static final int SKIN = 0xEC7B3C;
	static final int FACE = 0xFAD7AE;
	static final int FEET = 0xF5B81E;
	static final int EYE = 0x16141E;
	static final int BANDANA = 0x2346C4;
	static final int BANDANA_DARK = 0x1A3698;
	static final int DOT = 0xF4F4F8;
	static final int SHAFT = 0x8E5B2C;
	static final int TIP = 0xCDD2DE;

	private final int mBlink;

	public WaddleDee()
	{
		super(build(), rig());
		mBlink = mesh.morphIndex("blink");
	}

	private static Rig rig()
	{
		return new Rig(
			new int[]{-1, ROOT, BODY, BODY, ROOT, ROOT, ARM_B},
			new float[]{0, 0, -0.78f * R, 0.78f * R, -0.42f * R, 0.42f * R, 1.0f * R},
			new float[]{0, 0, 0.85f * R, 0.85f * R, 0.1f * R, 0.1f * R, 0.82f * R},
			new float[]{0, 0, 0, 0, 0.08f * R, 0.08f * R, 0.25f * R});
	}

	private static Mesh build()
	{
		MeshBuilder mb = new MeshBuilder();

		Ellipsoid body = new Ellipsoid(0, 0.96f * R, 0, 1.06f * R, 0.96f * R, R);
		Shapes.ellipsoid(mb, body, 18, 30, BODY, Shapes.solid(SKIN), 0);

		// face
		Decal face = new Decal(0, -0.08f, 1, 0.68f, 0.56f, 0.8f).res(5, 30);
		Shapes.decal(mb, body, face, BODY, (f, a) -> FACE, 0);

		// eyes
		for (int s = -1; s <= 1; s += 2)
		{
			Decal eye = new Decal(s * 0.2f, 0.04f, 1, 0.075f, 0.17f, 2.0f).res(3, 18);
			int[] e = Shapes.decal(mb, body, eye, BODY, (f, a) -> EYE, 0);
			Shapes.decalMorph(mb, "blink", body, e, eye, eye.size(0.085f, 0.018f).shift(0, -0.05f));
			Decal glint = eye.size(0.034f, 0.055f).shift(0, 0.08f).res(2, 12);
			glint.offset = 3.0f;
			int[] g = Shapes.decal(mb, body, glint, BODY, (f, a) -> 0xFFFFFF, 0);
			Shapes.decalMorph(mb, "blink", body, g, glint, glint.size(0.03f, 0.003f).shift(0, -0.05f));
		}

		// bandana + knot at the back
		Ellipsoid cap = new Ellipsoid(0, 0.98f * R, -0.02f * R, 1.1f * R, 1.02f * R, 1.07f * R, -0.32f, 0, 0);
		Shapes.band(mb, cap, 0, 1.05f, 10, 44, BODY, (x, y, z) -> dotted(x, y, z) ? DOT : BANDANA, 0);
		Ellipsoid rim = new Ellipsoid(0, 0.98f * R, -0.02f * R, 1.115f * R, 1.035f * R, 1.085f * R, -0.32f, 0, 0);
		Shapes.band(mb, rim, 1.0f, 1.14f, 1, 44, BODY, Shapes.solid(BANDANA_DARK), 0);
		for (int s = -1; s <= 1; s += 2)
		{
			Shapes.ellipsoid(mb, new Ellipsoid(s * 0.2f * R, 1.42f * R, -1.0f * R, 0.2f * R, 0.13f * R, 0.1f * R, 0, 0, s * 0.4f),
				6, 10, BODY, Shapes.solid(BANDANA), 0);
			Shapes.ellipsoid(mb, new Ellipsoid(s * 0.16f * R, 1.12f * R, -1.04f * R, 0.09f * R, 0.26f * R, 0.06f * R, 0.3f, 0, -s * 0.35f),
				6, 10, BODY, Shapes.solid(BANDANA_DARK), 0);
		}
		Shapes.ellipsoid(mb, new Ellipsoid(0, 1.4f * R, -1.04f * R, 0.1f * R, 0.1f * R, 0.08f * R), 5, 8, BODY, Shapes.solid(BANDANA_DARK), 0);

		// arms
		for (int s = -1; s <= 1; s += 2)
		{
			Ellipsoid arm = new Ellipsoid(s * 0.98f * R, 0.8f * R, 0.12f * R, 0.27f * R, 0.22f * R, 0.25f * R, 0, 0, -s * 0.5f);
			Shapes.ellipsoid(mb, arm, 8, 14, s < 0 ? ARM_A : ARM_B, Shapes.solid(SKIN), 0);
		}

		// feet
		for (int s = -1; s <= 1; s += 2)
		{
			Ellipsoid foot = new Ellipsoid(s * 0.43f * R, 0.15f * R, 0.14f * R, 0.37f * R, 0.18f * R, 0.52f * R, -0.08f, s * 0.22f, 0);
			Shapes.ellipsoid(mb, foot, 8, 16, s < 0 ? FOOT_A : FOOT_B, (x, y, z) -> y < -0.7f ? 0xD8981A : FEET, 0);
		}

		// spear
		float sx = 1.06f * R, sz = 0.25f * R;
		Shapes.tube(mb, sx, 0.1f * R, sz, sx, 2.25f * R, sz, 0.065f * R, 0.065f * R, 7, SPEAR, SHAFT, 0);
		Shapes.tube(mb, sx, 2.2f * R, sz, sx, 2.36f * R, sz, 0.11f * R, 0.11f * R, 7, SPEAR, BANDANA, 0);
		Shapes.tube(mb, sx, 2.36f * R, sz, sx, 2.85f * R, sz, 0.15f * R, 0, 7, SPEAR, TIP, 0);
		return mb.build();
	}

	private static boolean dotted(float x, float y, float z)
	{
		double lat = Math.acos(Math.max(-1, Math.min(1, y)));
		double lon = Math.atan2(z, x);
		if (lat < 0.35)
		{
			return false;
		}
		double row = lat / 0.32, col = (lon + Math.PI) / (2 * Math.PI) * 9 + (((int) Math.floor(row)) % 2) * 0.5;
		double fr = row - Math.floor(row) - 0.5, fc = col - Math.floor(col) - 0.5;
		return fr * fr + fc * fc < 0.06;
	}

	@Override
	public float duration(Anim anim)
	{
		switch (anim)
		{
			case HEAL:
				return 0.9f;
			case ATTACK:
				return 0.8f;
			case DEFEND:
				return 1.0f;
			case DEATH:
				return 1.9f;
			default:
				return 0;
		}
	}

	@Override
	public Anim[] showcase()
	{
		return new Anim[]{Anim.IDLE, Anim.WALK, Anim.HEAL, Anim.ATTACK, Anim.DEFEND, Anim.DEATH};
	}

	private static void armUp(Pose p, int arm, float amount)
	{
		p.roll[arm] += arm == ARM_A ? -amount : amount;
	}

	@Override
	public void pose(Anim anim, float t, float clock, Pose p)
	{
		p.reset();
		p.morph[mBlink] = blink(clock, 3.1f, 0);
		switch (anim)
		{
			case IDLE:
			default:
			{
				float b = sin(TAU * clock / 1.3f);
				p.sy[BODY] = 1 + 0.035f * b;
				p.sx[BODY] = p.sz[BODY] = 1 - 0.018f * b;
				p.roll[BODY] = 0.03f * sin(TAU * clock / 2.6f);
				armUp(p, ARM_A, 0.1f + 0.08f * sin(TAU * clock / 1.3f + 0.5f));
				armUp(p, ARM_B, 0.05f);
				p.roll[SPEAR] = -0.05f + 0.04f * sin(TAU * clock / 2.6f);
				break;
			}
			case WALK:
			{
				float ph = TAU * clock / 0.55f;
				float s = sin(ph), c = cos(ph);
				p.tz[FOOT_A] = 0.3f * R * s;
				p.ty[FOOT_A] = 0.16f * R * Math.max(0, c);
				p.tz[FOOT_B] = -0.3f * R * s;
				p.ty[FOOT_B] = 0.16f * R * Math.max(0, -c);
				p.ty[BODY] = 0.08f * R * Math.abs(c);
				p.roll[BODY] = 0.1f * s;
				p.pitch[BODY] = 0.1f;
				p.pitch[ARM_A] = 0.7f * s;
				p.pitch[ARM_B] = -0.35f * s;
				armUp(p, ARM_A, 0.2f);
				p.pitch[SPEAR] = 0.25f;
				break;
			}
			case HEAL:
			{
				float k = keys(t, 0, 0, 0.18f, 1, 0.7f, 1, 0.9f, 0);
				p.ty[ROOT] = 0.3f * R * Math.max(0, sin(TAU * clamp01(t / 0.9f) * 0.5f));
				armUp(p, ARM_B, 1.7f * k);
				p.roll[SPEAR] = -1.7f * k + 0.35f * k * sin(t * 24);
				armUp(p, ARM_A, 1.2f * k + 0.3f * k * sin(t * 24 + 1));
				p.sy[BODY] = 1 + 0.06f * k;
				break;
			}
			case ATTACK:
			{
				float wind = keys(t, 0, 0, 0.25f, 1, 0.35f, 0);
				float jab = keys(t, 0.25f, 0, 0.38f, 1, 0.55f, 1, 0.8f, 0);
				p.pitch[ARM_B] = -0.7f * wind + 1.45f * jab;
				p.pitch[SPEAR] = 0.2f * jab;
				p.tz[ROOT] = 0.35f * R * jab - 0.1f * R * wind;
				p.pitch[BODY] = -0.12f * wind + 0.18f * jab;
				p.yaw[BODY] = -0.3f * wind + 0.25f * jab;
				break;
			}
			case DEFEND:
			{
				float k = keys(t, 0, 0, 0.08f, 1, 0.4f, 1, 0.9f, 0);
				p.sy[BODY] = 1 - 0.12f * k;
				p.sx[BODY] = p.sz[BODY] = 1 + 0.06f * k;
				p.pitch[ROOT] = -0.12f * k;
				p.tz[ROOT] = -0.08f * R * k;
				p.morph[mBlink] = Math.max(p.morph[mBlink], k);
				break;
			}
			case DEATH:
			{
				float fall = easeIn(t / 0.55f);
				float bounce = keys(t, 0.55f, 0, 0.68f, 1, 0.82f, 0);
				p.pitch[ROOT] = -1.45f * fall;
				p.tz[ROOT] = -0.3f * R * fall;
				p.ty[ROOT] = 0.18f * R * bounce;
				armUp(p, ARM_A, 1.3f * smooth(t / 0.4f));
				armUp(p, ARM_B, 1.3f * smooth(t / 0.4f));
				p.morph[mBlink] = t > 0.3f ? 1 : p.morph[mBlink];
				p.scale(ROOT, 1 - smooth((t - 1.3f) / 0.6f));
				break;
			}
		}
	}
}
