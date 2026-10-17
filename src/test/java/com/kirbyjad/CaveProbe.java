package com.kirbyjad;

import java.io.File;
import java.util.Map;
import java.util.TreeMap;
import net.runelite.cache.OverlayManager;
import net.runelite.cache.TextureManager;
import net.runelite.cache.UnderlayManager;
import net.runelite.cache.definitions.OverlayDefinition;
import net.runelite.cache.definitions.TextureDefinition;
import net.runelite.cache.definitions.UnderlayDefinition;
import net.runelite.cache.fs.Store;
import net.runelite.cache.region.Region;
import net.runelite.cache.region.RegionLoader;

// dumps the floor of the fight caves (region 9551) and every textured overlay, to find lava/water textures
public class CaveProbe
{
	private static final int FIGHT_CAVES = 9551;

	public static void main(String[] args) throws Exception
	{
		File dir = new File(System.getProperty("user.home"), ".runelite/jagexcache/oldschool/LIVE");
		try (Store store = new Store(dir))
		{
			store.load();
			OverlayManager overlays = new OverlayManager(store);
			overlays.load();
			UnderlayManager underlays = new UnderlayManager(store);
			underlays.load();
			TextureManager textures = new TextureManager(store);
			textures.load();

			RegionLoader loader = new RegionLoader(store, regionId -> null);
			Region region = loader.loadRegion(FIGHT_CAVES, loader.loadMapDef(FIGHT_CAVES), null);

			Map<Integer, Integer> overlayCounts = new TreeMap<>();
			Map<Integer, Integer> underlayCounts = new TreeMap<>();
			for (int x = 0; x < 64; x++)
			{
				for (int y = 0; y < 64; y++)
				{
					overlayCounts.merge(region.getOverlayId(0, x, y), 1, Integer::sum);
					underlayCounts.merge(region.getUnderlayId(0, x, y), 1, Integer::sum);
				}
			}

			System.out.println("overlays in caves (raw id: tiles):");
			for (Map.Entry<Integer, Integer> e : overlayCounts.entrySet())
			{
				OverlayDefinition def = e.getKey() > 0 ? overlays.provide(e.getKey() - 1) : null;
				System.out.printf("  %d: %d tiles %s%n", e.getKey(), e.getValue(), describe(def));
			}

			System.out.println("underlays in caves:");
			for (Map.Entry<Integer, Integer> e : underlayCounts.entrySet())
			{
				UnderlayDefinition def = e.getKey() > 0 ? underlays.provide(e.getKey() - 1) : null;
				System.out.printf("  %d: %d tiles rgb=%s%n", e.getKey(), e.getValue(),
					def == null ? "-" : String.format("%06x", def.getColor()));
			}

			System.out.println("all textured overlays:");
			for (OverlayDefinition def : overlays.getOverlays())
			{
				if (def.getTexture() >= 0)
				{
					System.out.printf("  overlay %d %s%n", def.getId(), describe(def));
				}
			}

			System.out.println("textures (id: average colour as hsl):");
			for (TextureDefinition t : textures.getTextures())
			{
				int hsl = t.missingColor;
				System.out.printf("  %d: hsl=%d (h=%d s=%d l=%d) anim=%d/%d%n", t.getId(), hsl, hsl >> 10 & 63, hsl >> 7 & 7,
					hsl & 127, t.animationDirection, t.animationSpeed);
			}
		}
	}

	private static String describe(OverlayDefinition def)
	{
		if (def == null)
		{
			return "-";
		}
		return String.format("rgb=%06x texture=%d secondary=%06x", def.getRgbColor(), def.getTexture(),
			def.getSecondaryRgbColor());
	}
}
