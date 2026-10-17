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
import java.util.Map;

// round dreamland enemy: shared bones, eyes and the boring animations (idle, walk, flinch, death).
// subclasses build the body and do their own attacks
abstract class Critter extends Toon
{
	static final int ROOT = 0, BODY = 1, ARM_A = 2, ARM_B = 3, FOOT_A = 4, FOOT_B = 5, EXTRA = 6, HELD = 7;

	final float r;
	// floaters (bronto burt, scarfy) hover instead of walking
	final float hover;
	private final Map<Anim, Float> durations;
	final int mBlink;

	Critter(Mesh mesh, float r, float hover, Map<Anim, Float> durations)
	{
		super(mesh, rig(r));
		this.r = r;
		this.hover = hover;
		this.durations = durations;
		mBlink = mesh.morphIndex("blink");
	}

	private static Rig rig(float r)
	{
		return new Rig(
			new int[]{-1, ROOT, BODY, BODY, ROOT, ROOT, BODY, ARM_B},
			new float[]{0, 0, -0.8f * r, 0.8f * r, -0.4f * r, 0.4f * r, 0, 1.0f * r},
			new float[]{0, 0, 0.85f * r, 0.85f * r, 0.1f * r, 0.1f * r, 1.0f * r, 0.8f * r},
			new float[]{0, 0, 0, 0, 0.05f * r, 0.05f * r, 0, 0.2f * r});
	}

	// body sphere sitting on the ground, radius r
	static Ellipsoid body(MeshBuilder mb, float r, int color, int flags)
	{
		Ellipsoid e = new Ellipsoid(0, 0.96f * r, 0, 1.04f * r, 0.96f * r, r);
		Shapes.ellipsoid(mb, e, 16, 28, BODY, Shapes.solid(color), flags);
		return e;
	}

	// black oval eyes with a shine, with a blink morph
	static void eyes(MeshBuilder mb, Ellipsoid body, float spread, float up, float w, float h, int color)
	{
		for (int s = -1; s <= 1; s += 2)
		{
			Decal eye = new Decal(s * spread, up, 1, w, h, 2.0f).res(3, 18);
			int[] e = Shapes.decal(mb, body, eye, BODY, (f, a) -> color, 0);
			Shapes.decalMorph(mb, "blink", body, e, eye, eye.size(w * 1.1f, h * 0.1f).shift(0, -h * 0.3f));
			Decal glint = eye.size(w * 0.45f, h * 0.35f).shift(0, h * 0.45f).res(2, 12);
			glint.offset = 3.0f;
			int[] g = Shapes.decal(mb, body, glint, BODY, (f, a) -> 0xffffff, 0);
			Shapes.decalMorph(mb, "blink", body, g, glint, glint.size(w * 0.4f, h * 0.02f).shift(0, -h * 0.3f));
		}
	}

	static void feet(MeshBuilder mb, float r, int color)
	{
		for (int s = -1; s <= 1; s += 2)
		{
			Ellipsoid foot = new Ellipsoid(s * 0.42f * r, 0.15f * r, 0.12f * r, 0.36f * r, 0.18f * r, 0.5f * r, -0.08f, s * 0.22f, 0);
			Shapes.ellipsoid(mb, foot, 8, 14, s < 0 ? FOOT_A : FOOT_B, Shapes.solid(color), 0);
		}
	}

	static void arms(MeshBuilder mb, float r, int color)
	{
		for (int s = -1; s <= 1; s += 2)
		{
			Ellipsoid arm = new Ellipsoid(s * 0.98f * r, 0.8f * r, 0.12f * r, 0.27f * r, 0.22f * r, 0.25f * r, 0, 0, -s * 0.5f);
			Shapes.ellipsoid(mb, arm, 7, 12, s < 0 ? ARM_A : ARM_B, Shapes.solid(color), 0);
		}
	}

	static void armUp(Pose p, int arm, float amount)
	{
		p.roll[arm] += arm == ARM_A ? -amount : amount;
	}

	@Override
	public float duration(Anim anim)
	{
		return durations.getOrDefault(anim, 0f);
	}

	@Override
	public void pose(Anim anim, float t, float clock, Pose p)
	{
		p.reset();
		p.morph[mBlink] = blink(clock, 3.3f, 0);
		if (hover > 0)
		{
			p.ty[ROOT] = hover + 0.12f * r * sin(TAU * clock / 1.4f);
		}

		switch (anim)
		{
			case WALK:
				walk(clock, p);
				break;
			case DEFEND:
			{
				float k = keys(t, 0, 0, 0.08f, 1, 0.35f, 1, duration(anim), 0);
				p.sy[BODY] = 1 - 0.14f * k;
				p.sx[BODY] = p.sz[BODY] = 1 + 0.07f * k;
				p.pitch[ROOT] = -0.15f * k;
				p.tz[ROOT] = -0.1f * r * k;
				p.morph[mBlink] = Math.max(p.morph[mBlink], k);
				break;
			}
			case DEATH:
			{
				// little hop, spin, then poof
				float u = clamp01(t / 1.6f);
				p.ty[ROOT] += 0.6f * r * sin(u * 3.14f);
				p.yaw[ROOT] = 2 * TAU * easeIn(u);
				p.scale(ROOT, 1 - smooth((u - 0.5f) / 0.5f));
				p.morph[mBlink] = 1;
				break;
			}
			case IDLE:
			{
				float b = sin(TAU * clock / 1.3f);
				p.sy[BODY] = 1 + 0.035f * b;
				p.sx[BODY] = p.sz[BODY] = 1 - 0.018f * b;
				armUp(p, ARM_A, 0.1f + 0.08f * sin(TAU * clock / 1.3f + 0.5f));
				armUp(p, ARM_B, 0.1f + 0.08f * sin(TAU * clock / 1.3f + 0.5f));
				break;
			}
			default:
				break;
		}

		special(anim, t, clock, p);
	}

	void walk(float clock, Pose p)
	{
		float ph = TAU * clock / 0.6f;
		float s = sin(ph);
		float c = cos(ph);
		if (hover > 0)
		{
			p.pitch[BODY] = 0.2f;
			return;
		}
		p.tz[FOOT_A] = 0.3f * r * s;
		p.ty[FOOT_A] = 0.16f * r * Math.max(0, c);
		p.tz[FOOT_B] = -0.3f * r * s;
		p.ty[FOOT_B] = 0.16f * r * Math.max(0, -c);
		p.ty[BODY] = 0.08f * r * Math.abs(c);
		p.roll[BODY] = 0.1f * s;
		p.pitch[BODY] = 0.1f;
		p.pitch[ARM_A] = 0.6f * s;
		p.pitch[ARM_B] = -0.6f * s;
	}

	// attacks and anything else the subclass wants to add on top
	abstract void special(Anim anim, float t, float clock, Pose p);

	@Override
	public Anim[] showcase()
	{
		Anim[] out = new Anim[durations.size() + 2];
		out[0] = Anim.IDLE;
		out[1] = Anim.WALK;
		int i = 2;
		for (Anim a : durations.keySet())
		{
			out[i++] = a;
		}
		return out;
	}
}
