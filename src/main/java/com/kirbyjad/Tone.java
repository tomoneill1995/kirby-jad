package com.kirbyjad;

// the colour brightness setting, applied to everything the plugin colours
final class Tone
{
	// 1 = as designed. set from the config
	static volatile float brightness = 1;

	private Tone()
	{
	}

	static int lum(int lum)
	{
		return Math.max(2, Math.min(126, Math.round(lum * brightness)));
	}

	// dimmed colours look muddy at full saturation, ease it off a bit with them
	static int sat(int sat)
	{
		if (brightness >= 1)
		{
			return sat;
		}
		return Math.max(0, Math.min(7, Math.round(sat * (0.5f + 0.5f * brightness))));
	}
}
