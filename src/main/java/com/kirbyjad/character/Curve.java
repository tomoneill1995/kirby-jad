package com.kirbyjad.character;

final class Curve
{
	private Curve()
	{
	}

	static final float PI = (float) Math.PI;
	static final float TAU = (float) (Math.PI * 2);

	static float clamp01(float x)
	{
		return x < 0 ? 0 : x > 1 ? 1 : x;
	}

	static float smooth(float x)
	{
		x = clamp01(x);
		return x * x * (3 - 2 * x);
	}

	static float easeIn(float x)
	{
		x = clamp01(x);
		return x * x;
	}

	static float easeOut(float x)
	{
		x = clamp01(x);
		return 1 - (1 - x) * (1 - x);
	}

	static float easeOutBack(float x)
	{
		x = clamp01(x);
		float c = 1.9f;
		float u = x - 1;
		return 1 + (c + 1) * u * u * u + c * u * u;
	}

	// keys(t, t0, v0, t1, v1, ...) - smoothstep between each pair, clamped at the ends
	static float keys(float t, float... tv)
	{
		if (t <= tv[0])
		{
			return tv[1];
		}
		for (int i = 0; i + 2 < tv.length; i += 2)
		{
			float t0 = tv[i], t1 = tv[i + 2];
			if (t <= t1)
			{
				float u = t1 > t0 ? (t - t0) / (t1 - t0) : 1;
				float s = smooth(u);
				return tv[i + 1] + (tv[i + 3] - tv[i + 1]) * s;
			}
		}
		return tv[tv.length - 1];
	}

	static float sin(float x)
	{
		return (float) Math.sin(x);
	}

	static float cos(float x)
	{
		return (float) Math.cos(x);
	}

	static float blink(float clock, float period, float phase)
	{
		float p = (clock + phase) % period;
		if (p < 0)
		{
			p += period;
		}
		return keys(p, 0, 0, 0.07f, 1, 0.15f, 1, 0.24f, 0);
	}
}
