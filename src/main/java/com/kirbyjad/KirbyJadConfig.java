package com.kirbyjad;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;
import net.runelite.client.config.Units;

@ConfigGroup(KirbyJadConfig.GROUP)
public interface KirbyJadConfig extends Config
{
	String GROUP = "kirbyjad";

	@ConfigItem(
		keyName = "replaceJad",
		name = "Kirby for Jad",
		description = "Draw TzTok-Jad as Kirby in the Fight Caves",
		position = 0
	)
	default boolean replaceJad()
	{
		return true;
	}

	@ConfigItem(
		keyName = "replaceHealers",
		name = "Waddle Dees for healers",
		description = "Draw Jad's healers as Waddle Dees in the Fight Caves",
		position = 1
	)
	default boolean replaceHealers()
	{
		return true;
	}

	@ConfigItem(
		keyName = "replaceWaves",
		name = "Kirby enemies for waves",
		description = "Draw the wave monsters as Bronto Burts, Scarfies, Gordos, Waddle Doos and Meta Knights",
		position = 2
	)
	default boolean replaceWaves()
	{
		return true;
	}

	@ConfigItem(
		keyName = "dreamland",
		name = "Dreamland caves",
		description = "Turn the Fight Caves into a Kirby-style meadow with hills, trees and water",
		position = 3
	)
	default boolean dreamland()
	{
		return true;
	}

	@Range(min = 40, max = 120)
	@Units(Units.PERCENT)
	@ConfigItem(
		keyName = "brightness",
		name = "Colour brightness",
		description = "Dims or brightens everything this plugin colours. Lower it if it looks too bright on a normal screen or in recordings",
		position = 4
	)
	default int brightness()
	{
		return 100;
	}
}
