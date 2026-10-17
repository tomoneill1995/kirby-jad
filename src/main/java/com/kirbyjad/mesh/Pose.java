package com.kirbyjad.mesh;

import java.util.Arrays;

public final class Pose
{
	public final int bones;
	public final float[] tx, ty, tz;
	public final float[] pitch, yaw, roll;
	public final float[] sx, sy, sz;
	public final float[] morph;
	public float stone;
	// brightness of the fire
	public float glow = 1;

	public Pose(int bones, int morphs)
	{
		this.bones = bones;
		tx = new float[bones];
		ty = new float[bones];
		tz = new float[bones];
		pitch = new float[bones];
		yaw = new float[bones];
		roll = new float[bones];
		sx = new float[bones];
		sy = new float[bones];
		sz = new float[bones];
		morph = new float[morphs];
		reset();
	}

	public void reset()
	{
		Arrays.fill(tx, 0);
		Arrays.fill(ty, 0);
		Arrays.fill(tz, 0);
		Arrays.fill(pitch, 0);
		Arrays.fill(yaw, 0);
		Arrays.fill(roll, 0);
		Arrays.fill(sx, 1);
		Arrays.fill(sy, 1);
		Arrays.fill(sz, 1);
		Arrays.fill(morph, 0);
		stone = 0;
		glow = 1;
	}

	public void scale(int bone, float s)
	{
		sx[bone] = s;
		sy[bone] = s;
		sz[bone] = s;
	}

	public void copyFrom(Pose o)
	{
		System.arraycopy(o.tx, 0, tx, 0, bones);
		System.arraycopy(o.ty, 0, ty, 0, bones);
		System.arraycopy(o.tz, 0, tz, 0, bones);
		System.arraycopy(o.pitch, 0, pitch, 0, bones);
		System.arraycopy(o.yaw, 0, yaw, 0, bones);
		System.arraycopy(o.roll, 0, roll, 0, bones);
		System.arraycopy(o.sx, 0, sx, 0, bones);
		System.arraycopy(o.sy, 0, sy, 0, bones);
		System.arraycopy(o.sz, 0, sz, 0, bones);
		System.arraycopy(o.morph, 0, morph, 0, morph.length);
		stone = o.stone;
		glow = o.glow;
	}

	public void blend(Pose a, Pose b, float t)
	{
		for (int i = 0; i < bones; i++)
		{
			tx[i] = lerp(a.tx[i], b.tx[i], t);
			ty[i] = lerp(a.ty[i], b.ty[i], t);
			tz[i] = lerp(a.tz[i], b.tz[i], t);
			pitch[i] = lerp(a.pitch[i], b.pitch[i], t);
			yaw[i] = lerp(a.yaw[i], b.yaw[i], t);
			roll[i] = lerp(a.roll[i], b.roll[i], t);
			sx[i] = lerp(a.sx[i], b.sx[i], t);
			sy[i] = lerp(a.sy[i], b.sy[i], t);
			sz[i] = lerp(a.sz[i], b.sz[i], t);
		}
		for (int i = 0; i < morph.length; i++)
		{
			morph[i] = lerp(a.morph[i], b.morph[i], t);
		}
		stone = lerp(a.stone, b.stone, t);
		glow = lerp(a.glow, b.glow, t);
	}

	private static float lerp(float a, float b, float t)
	{
		return a + (b - a) * t;
	}
}
