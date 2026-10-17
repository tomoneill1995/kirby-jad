package com.kirbyjad;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class KirbyJadPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(KirbyJadPlugin.class);
		RuneLite.main(args);
	}
}
