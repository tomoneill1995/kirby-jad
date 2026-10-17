package com.kirbyjad.character;

import static com.kirbyjad.character.Curve.TAU;
import static com.kirbyjad.character.Curve.blink;
import static com.kirbyjad.character.Curve.clamp01;
import static com.kirbyjad.character.Curve.cos;
import static com.kirbyjad.character.Curve.easeIn;
import static com.kirbyjad.character.Curve.easeOutBack;
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

public final class Kirby extends Toon
{
	// jad is ~545 tall, this makes kirby about the same
	static final float R = 240;

	static final int ROOT = 0, BODY = 1, ARM_A = 2, ARM_B = 3, FOOT_A = 4, FOOT_B = 5, FLAME = 6;

	static final int PINK = 0xF7A3C0;
	static final int CHEEK = 0xF25E8E;
	static final int FOOT = 0xD81C48;
	static final int EYE_BLACK = 0x0E1226;
	static final int EYE_BLUE = 0x1E4ADC;
	static final int EYE_WHITE = 0xFFFFFF;
	static final int MOUTH_DARK = 0x5A0616;
	static final int MOUTH_RED = 0xA8102C;
	static final int TONGUE = 0xF2587A;
	static final int FIRE_CORE = 0xFFE45A;
	static final int FIRE_MID = 0xFFA024;
	static final int FIRE_EDGE = 0xF0461C;

	private final int mBlink, mMouth, mSmile;

	public Kirby()
	{
		super(build(), rig());
		mBlink = mesh.morphIndex("blink");
		mMouth = mesh.morphIndex("mouth");
		mSmile = mesh.morphIndex("smile");
	}

	private static Rig rig()
	{
		return new Rig(
			new int[]{-1, ROOT, BODY, BODY, ROOT, ROOT, BODY},
			new float[]{0, 0, -0.8f * R, 0.8f * R, -0.46f * R, 0.46f * R, 0},
			new float[]{0, 0, 1.0f * R, 1.0f * R, 0.12f * R, 0.12f * R, 0.86f * R},
			new float[]{0, 0, 0, 0, 0.05f * R, 0.05f * R, 1.0f * R});
	}

	private static Mesh build()
	{
		MeshBuilder mb = new MeshBuilder();
		final int stone = Mesh.FLAG_STONE;

		Ellipsoid body = new Ellipsoid(0, 0.97f * R, 0, 1.04f * R, 0.97f * R, R);
		Shapes.ellipsoid(mb, body, 24, 40, BODY, Shapes.solid(PINK), stone);

		// arms
		for (int s = -1; s <= 1; s += 2)
		{
			Ellipsoid arm = new Ellipsoid(s * 1.0f * R, 1.04f * R, 0.02f * R, 0.36f * R, 0.27f * R, 0.3f * R, 0, 0, -s * 0.45f);
			Shapes.ellipsoid(mb, arm, 10, 18, s < 0 ? ARM_A : ARM_B, Shapes.solid(PINK), stone);
		}

		// feet
		for (int s = -1; s <= 1; s += 2)
		{
			Ellipsoid foot = new Ellipsoid(s * 0.47f * R, 0.17f * R, 0.1f * R, 0.42f * R, 0.21f * R, 0.62f * R, -0.08f, s * 0.28f, 0);
			Shapes.ellipsoid(mb, foot, 10, 20, s < 0 ? FOOT_A : FOOT_B,
				(x, y, z) -> y < -0.75f ? 0xA8143A : FOOT, stone);
		}

		// eyes - black outline, blue bottom, white shine on top
		for (int s = -1; s <= 1; s += 2)
		{
			Decal outer = new Decal(s * 0.165f, 0.31f, 0.94f, 0.074f, 0.178f, 1.6f).res(4, 20);
			int[] o = Shapes.decal(mb, body, outer, BODY, (f, a) -> EYE_BLACK, 0);
			Shapes.decalMorph(mb, "blink", body, o, outer, outer.size(0.082f, 0.016f).shift(0, -0.04f));

			Decal blue = outer.size(0.05f, 0.072f).shift(0, -0.078f).res(3, 16);
			blue.offset = 2.8f;
			int[] b = Shapes.decal(mb, body, blue, BODY, (f, a) -> f < 0.5f ? 0x3A6CF4 : EYE_BLUE, 0);
			Shapes.decalMorph(mb, "blink", body, b, blue, blue.size(0.045f, 0.004f).shift(0, -0.04f));

			Decal glint = outer.size(0.044f, 0.075f).shift(s * 0.004f, 0.066f).res(3, 16);
			glint.offset = 3.6f;
			int[] g = Shapes.decal(mb, body, glint, BODY, (f, a) -> EYE_WHITE, 0);
			Shapes.decalMorph(mb, "blink", body, g, glint, glint.size(0.04f, 0.004f).shift(0, -0.036f));
		}

		// blush
		for (int s = -1; s <= 1; s += 2)
		{
			Decal cheek = new Decal(s * 0.43f, 0.05f, 0.9f, 0.11f, 0.06f, 1.4f).res(2, 18);
			Shapes.decal(mb, body, cheek, BODY, (f, a) -> f < 0.5f ? CHEEK : 0xF47FA6, 0);
		}

		// mouth + tongue, the "mouth" morph opens it right up
		Decal mouth = new Decal(0, -0.02f, 1, 0.055f, 0.04f, 2.0f).shift(0, -0.02f).res(4, 28);
		int[] m = Shapes.decal(mb, body, mouth, BODY, (f, a) -> f < 0.6f ? MOUTH_DARK : f < 0.85f ? 0x7E0A20 : MOUTH_RED, 0);
		Shapes.decalMorph(mb, "mouth", body, m, mouth, mouth.size(0.25f, 0.31f).shift(0, -0.11f));
		Shapes.decalMorph(mb, "smile", body, m, mouth, mouth.size(0.09f, 0.03f).shift(0, -0.03f));
		Decal tongue = new Decal(0, -0.02f, 1, 0.004f, 0.003f, 3.0f).shift(0, -0.02f).res(2, 18);
		int[] t = Shapes.decal(mb, body, tongue, BODY, (f, a) -> f < 0.5f ? 0xF7708E : TONGUE, 0);
		Shapes.decalMorph(mb, "mouth", body, t, tongue, tongue.size(0.15f, 0.085f).shift(0, -0.33f));

		// fire breath, scaled to 0 unless he's doing the mage attack
		final int fire = Mesh.FLAG_EMISSIVE;
		Shapes.DirColor flame = (x, y, z) -> z > 0.55f ? FIRE_CORE : z > -0.1f ? FIRE_MID : FIRE_EDGE;
		Shapes.ellipsoid(mb, new Ellipsoid(0, 0.84f * R, 1.32f * R, 0.3f * R, 0.26f * R, 0.3f * R), 7, 12, FLAME, flame, fire);
		Shapes.ellipsoid(mb, new Ellipsoid(0.16f * R, 0.98f * R, 1.62f * R, 0.2f * R, 0.17f * R, 0.2f * R), 6, 10, FLAME, flame, fire);
		Shapes.ellipsoid(mb, new Ellipsoid(-0.14f * R, 0.74f * R, 1.72f * R, 0.17f * R, 0.15f * R, 0.17f * R), 6, 10, FLAME, flame, fire);
		Shapes.ellipsoid(mb, new Ellipsoid(0.02f * R, 0.88f * R, 1.98f * R, 0.13f * R, 0.12f * R, 0.13f * R), 5, 8, FLAME, flame, fire);
		float[][] embers = {{0.32f, 1.2f, 1.5f}, {-0.34f, 0.98f, 1.42f}, {0.26f, 0.56f, 1.86f}, {-0.2f, 1.18f, 2.05f},
			{0.05f, 0.5f, 2.2f}, {0.38f, 0.86f, 2.18f}};
		for (float[] e : embers)
		{
			Shapes.ellipsoid(mb, new Ellipsoid(e[0] * R, e[1] * R, e[2] * R, 0.05f * R, 0.05f * R, 0.05f * R, 0.6f, 0.6f, 0),
				2, 4, FLAME, Shapes.solid(FIRE_MID), fire);
		}
		return mb.build();
	}

	@Override
	public float duration(Anim anim)
	{
		switch (anim)
		{
			case MAGIC:
				return 3.1f;
			case RANGED:
				return 1.1f;
			case MELEE:
				return 1.44f;
			case DEFEND:
				return 1.2f;
			case DEATH:
				return 2.8f;
			default:
				return 0;
		}
	}

	@Override
	public Anim[] showcase()
	{
		return new Anim[]{Anim.IDLE, Anim.WALK, Anim.MAGIC, Anim.RANGED, Anim.MELEE, Anim.DEFEND, Anim.DANCE, Anim.DEATH};
	}

	private static void armUp(Pose p, int arm, float amount)
	{
		p.roll[arm] += arm == ARM_A ? -amount : amount;
	}

	@Override
	public void pose(Anim anim, float t, float clock, Pose p)
	{
		p.reset();
		p.scale(FLAME, 0);
		float breathe = sin(TAU * clock / 1.8f);
		p.morph[mBlink] = blink(clock, 3.7f, 0);

		switch (anim)
		{
			case IDLE:
			default:
			{
				p.sy[BODY] = 1 + 0.03f * breathe;
				p.sx[BODY] = p.sz[BODY] = 1 - 0.015f * breathe;
				armUp(p, ARM_A, 0.12f + 0.08f * sin(TAU * clock / 1.8f + 0.7f));
				armUp(p, ARM_B, 0.12f + 0.08f * sin(TAU * clock / 1.8f + 0.7f));
				break;
			}
			case WALK:
			{
				float ph = TAU * clock / 0.8f;
				float s = sin(ph), c = cos(ph);
				p.tz[FOOT_A] = 0.24f * R * s;
				p.ty[FOOT_A] = 0.12f * R * Math.max(0, c);
				p.pitch[FOOT_A] = -0.25f * Math.max(0, c);
				p.tz[FOOT_B] = -0.24f * R * s;
				p.ty[FOOT_B] = 0.12f * R * Math.max(0, -c);
				p.pitch[FOOT_B] = -0.25f * Math.max(0, -c);
				p.ty[BODY] = 0.06f * R * Math.abs(c);
				p.roll[BODY] = 0.08f * s;
				p.yaw[BODY] = 0.06f * s;
				p.pitch[BODY] = 0.06f;
				p.pitch[ARM_A] = 0.55f * s;
				p.pitch[ARM_B] = -0.55f * s;
				armUp(p, ARM_A, 0.25f);
				armUp(p, ARM_B, 0.25f);
				break;
			}
			case MAGIC:
			{
				// mouth needs to be open fast so you can still pray off it
				float puff = keys(t, 0, 0, 0.3f, 1, 0.95f, 1, 1.3f, 0.25f, 2.4f, 0);
				float lean = keys(t, 0, 0, 0.3f, -0.2f, 0.85f, -0.24f, 1.05f, 0.22f, 1.7f, 0.18f, 2.5f, 0);
				p.scale(BODY, 1 + 0.1f * puff);
				p.sy[BODY] += 0.04f * puff;
				p.pitch[BODY] = lean;
				p.morph[mMouth] = keys(t, 0, 0, 0.25f, 1, 1.9f, 1, 2.4f, 0);
				armUp(p, ARM_A, 1.0f * puff + 0.35f);
				armUp(p, ARM_B, 1.0f * puff + 0.35f);
				p.pitch[ARM_A] = p.pitch[ARM_B] = -0.4f * puff;
				float fireT = (t - 0.95f) / 0.95f;
				if (fireT > 0 && fireT < 1)
				{
					float grow = easeOutBack(fireT / 0.22f);
					float fade = 1 - smooth((fireT - 0.6f) / 0.4f);
					p.scale(FLAME, 1.15f * grow * fade);
					p.tz[FLAME] = 0.95f * R * easeIn(fireT);
					p.ty[FLAME] = -0.05f * R * fireT;
					p.roll[FLAME] = 0.18f * sin(t * 37);
					p.yaw[FLAME] = 0.12f * sin(t * 29);
					p.glow = 1.05f + 0.15f * sin(t * 53);
				}
				p.morph[mBlink] = Math.max(p.morph[mBlink], keys(t, 0.9f, 0, 1.05f, 0.55f, 1.8f, 0.55f, 2.1f, 0));
				break;
			}
			case RANGED:
			{
				// stone drop
				float crouch = keys(t, 0, 0, 0.14f, 1, 0.24f, 0);
				float land = keys(t, 0.6f, 0, 0.66f, 1, 0.8f, 0);
				p.sy[BODY] = 1 - 0.2f * crouch + 0.1f * keys(t, 0.2f, 0, 0.32f, 1, 0.45f, 0) - 0.22f * land;
				p.sx[BODY] = p.sz[BODY] = 1 + 0.12f * crouch + 0.14f * land;
				float h;
				if (t < 0.2f)
				{
					h = 0;
				}
				else if (t < 0.46f)
				{
					h = 1 - (1 - (t - 0.2f) / 0.26f) * (1 - (t - 0.2f) / 0.26f);
				}
				else if (t < 0.52f)
				{
					h = 1;
				}
				else
				{
					h = 1 - easeIn((t - 0.52f) / 0.1f);
				}
				p.ty[ROOT] = 1.25f * R * h;
				p.stone = keys(t, 0.26f, 0, 0.4f, 1, 0.85f, 1, 1.1f, 0);
				float tuck = p.stone;
				p.scale(ARM_A, 1 - 0.55f * tuck);
				p.scale(ARM_B, 1 - 0.55f * tuck);
				p.tx[ARM_A] = 0.12f * R * tuck;
				p.tx[ARM_B] = -0.12f * R * tuck;
				armUp(p, ARM_A, 0.5f * crouch - 0.3f * tuck);
				armUp(p, ARM_B, 0.5f * crouch - 0.3f * tuck);
				p.ty[FOOT_A] = p.ty[FOOT_B] = 0.08f * R * tuck * h;
				if (t > 0.62f && t < 0.86f)
				{
					float k = 1 - (t - 0.62f) / 0.24f;
					p.tx[ROOT] = 0.03f * R * sin(t * 90) * k;
				}
				p.morph[mBlink] = Math.max(p.morph[mBlink], 0.35f * tuck);
				break;
			}
			case MELEE:
			{
				float wind = keys(t, 0, 0, 0.42f, 1, 0.5f, 0);
				float hit = keys(t, 0.42f, 0, 0.58f, 1, 0.8f, 1, 1.35f, 0);
				p.yaw[BODY] = -0.45f * wind + 0.4f * hit;
				p.pitch[BODY] = 0.12f * hit - 0.08f * wind;
				p.tz[ROOT] = 0.32f * R * keys(t, 0.42f, 0, 0.56f, 1, 0.9f, 1, 1.4f, 0);
				armUp(p, ARM_B, 1.4f * wind + 0.2f * hit);
				p.pitch[ARM_B] = -0.7f * wind + 1.6f * hit;
				p.scale(ARM_B, 1 + 0.4f * hit);
				armUp(p, ARM_A, 0.3f * wind);
				p.morph[mMouth] = 0.35f * keys(t, 0.35f, 0, 0.5f, 1, 0.9f, 1, 1.2f, 0);
				break;
			}
			case DEFEND:
			{
				float k = keys(t, 0, 0, 0.1f, 1, 0.5f, 1, 1.0f, 0);
				p.sy[BODY] = 1 - 0.09f * k;
				p.sx[BODY] = p.sz[BODY] = 1 + 0.04f * k;
				p.pitch[BODY] = -0.14f * k;
				armUp(p, ARM_A, 0.7f * k);
				armUp(p, ARM_B, 0.7f * k);
				p.pitch[ARM_A] = p.pitch[ARM_B] = 0.5f * k;
				p.morph[mBlink] = Math.max(p.morph[mBlink], k);
				break;
			}
			case DEATH:
			{
				// spins off into the sky
				float startle = keys(t, 0, 0, 0.15f, 1, 0.45f, 1, 0.6f, 0);
				p.morph[mMouth] = 0.55f * startle;
				p.sy[BODY] = 1 + 0.12f * startle;
				armUp(p, ARM_A, 1.2f * startle + 0.8f * smooth((t - 0.5f) / 0.4f));
				armUp(p, ARM_B, 1.2f * startle + 0.8f * smooth((t - 0.5f) / 0.4f));
				float u = clamp01((t - 0.45f) / 2.2f);
				p.yaw[ROOT] = 3 * TAU * easeIn(u);
				p.ty[ROOT] = 2.2f * R * easeIn(u);
				p.scale(ROOT, 1 - smooth(u));
				p.morph[mBlink] = t > 0.5f ? 1 : p.morph[mBlink];
				p.morph[mSmile] = t > 0.5f ? 1 : 0;
				break;
			}
			case DANCE:
			{
				// the victory dance: hop side to side waving, then a spin
				float ph = TAU * clock / 1.2f;
				float hop = Math.abs(sin(ph));
				p.ty[ROOT] = 0.25f * R * hop;
				p.roll[ROOT] = 0.18f * cos(ph);
				p.tx[ROOT] = 0.25f * R * cos(ph);
				float spin = clock % 4.8f;
				p.yaw[ROOT] = spin > 3.6f ? TAU * smooth((spin - 3.6f) / 1.2f) : 0;
				armUp(p, ARM_A, 0.9f + 0.6f * sin(ph * 2));
				armUp(p, ARM_B, 0.9f - 0.6f * sin(ph * 2));
				p.sy[BODY] = 1 + 0.08f * (hop - 0.5f);
				p.morph[mSmile] = 1;
				p.morph[mBlink] = 1;
				break;
			}
		}
	}
}
