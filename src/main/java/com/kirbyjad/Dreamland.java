package com.kirbyjad;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import lombok.AllArgsConstructor;
import net.runelite.api.Constants;
import net.runelite.api.DecorativeObject;
import net.runelite.api.GameObject;
import net.runelite.api.JagexColor;
import net.runelite.api.Model;
import net.runelite.api.Point;
import net.runelite.api.Renderable;
import net.runelite.api.Scene;
import net.runelite.api.SceneTileModel;
import net.runelite.api.SceneTilePaint;
import net.runelite.api.Tile;
import net.runelite.api.WallObject;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.gameval.ObjectID;

// strips the fight caves down to its terrain when the scene loads and paints it like dreamland.
// the removed rocks are handed to DreamlandScenery as spots to put hills and trees on.
// runs on the maploader thread, so it only touches the scene
class Dreamland
{
	static final int FIGHT_CAVES_REGION = 9551;
	static final int SKY = 0x9ad8ff;

	private static final Set<Integer> KEEP = Set.of(ObjectID.TZHAAR_FIGHTCAVE_WALL_ENTRANCE, ObjectID.TZHAAR_FIGHTCAVE_WALL_EXIT);

	private static final int SIZE = Constants.SCENE_SIZE;
	private static final int HIDDEN = 12345678;
	private static final int GRASS_DARK = 0x71cb4b;
	private static final int GRASS_LIGHT = 0x8bdd59;
	private static final int SAND = 0xeed27c;
	private static final int SHALLOW = 0x8fe4ff;
	private static final int DEEP = 0x45b2f2;

	// smooth colours over the tile corners, one for dry ground and one for water.
	// every tile vertex samples whichever one it belongs to so there are no hard tile edges
	private int[][] landField;
	private int[][] waterField;
	private boolean[][] wet;
	private boolean[][] solid;

	enum SpotType
	{
		ROCK,
		// the cave's outer wall, used to close the ring of hills
		EDGE,
		GROUND,
	}

	@AllArgsConstructor
	static class Spot
	{
		final SpotType type;
		final int x;
		final int y;
		final int size;
	}

	// walls and decorations can't be removed, only blanked out. their models are cached and can be
	// reused outside the caves (tzhaar city), so keep the original colours to put back
	private final Map<Model, int[]> hidden = new WeakHashMap<>();

	static boolean isFightCaves(Scene scene)
	{
		if (scene.isInstance())
		{
			for (int[][] plane : scene.getInstanceTemplateChunks())
			{
				for (int[] column : plane)
				{
					for (int chunk : column)
					{
						if (chunk == -1)
						{
							continue;
						}

						int x = (chunk >> 14 & 0x3ff) * 8;
						int y = (chunk >> 3 & 0x7ff) * 8;
						if (((x >> 6) << 8 | (y >> 6)) == FIGHT_CAVES_REGION)
						{
							return true;
						}
					}
				}
			}
			return false;
		}

		int x = scene.getBaseX() + 52;
		int y = scene.getBaseY() + 52;
		return ((x >> 6) << 8 | (y >> 6)) == FIGHT_CAVES_REGION;
	}

	List<Spot> paint(Scene scene)
	{
		buildFields(scene.getTiles()[0]);
		List<Spot> spots = new ArrayList<>();
		for (Tile[][] plane : scene.getTiles())
		{
			for (Tile[] column : plane)
			{
				for (Tile tile : column)
				{
					if (tile != null)
					{
						paintTile(scene, tile, spots);
					}
				}
			}
		}
		borders(scene.getTiles()[0], spots);
		return spots;
	}

	// tiles at the edge of the land, next to the void or the end of the loaded area
	private void borders(Tile[][] tiles, List<Spot> spots)
	{
		solid = new boolean[SIZE][SIZE];
		for (int x = 0; x < SIZE; x++)
		{
			for (int y = 0; y < SIZE; y++)
			{
				Tile t = tiles[x][y];
				solid[x][y] = t != null && (t.getSceneTilePaint() != null || t.getSceneTileModel() != null);
			}
		}

		for (int x = 0; x < SIZE; x++)
		{
			for (int y = 0; y < SIZE; y++)
			{
				if (!solid[x][y] || wet[x][y])
				{
					continue;
				}

				boolean edge = false;
				for (int d = 0; d < 4 && !edge; d++)
				{
					int nx = x + (d == 0 ? 1 : d == 1 ? -1 : 0);
					int ny = y + (d == 2 ? 1 : d == 3 ? -1 : 0);
					edge = nx < 0 || ny < 0 || nx >= SIZE || ny >= SIZE || !solid[nx][ny];
				}
				if (edge)
				{
					spots.add(new Spot(SpotType.EDGE, x * 128 + 64, y * 128 + 64, 1));
				}
			}
		}
	}

	private void paintTile(Scene scene, Tile tile, List<Spot> spots)
	{
		Point p = tile.getSceneLocation();
		int x = p.getX();
		int y = p.getY();
		boolean water = false;

		SceneTilePaint paint = tile.getSceneTilePaint();
		// paints marked like this aren't drawn (there are layers of them under the pools), keep them that way
		if (paint != null && paint.getNeColor() == HIDDEN)
		{
			paint = null;
		}
		if (paint != null)
		{
			water = paint.getTexture() != -1;
			int[][] field = water ? waterField : landField;
			paint.setTexture(-1);
			// one colour per tile. blending per corner made striped artifacts round the old lava pits
			int rgb = sample(field, x * 128 + 64, y * 128 + 64);
			paint.setSwColor(shade(rgb, paint.getSwColor()));
			paint.setSeColor(shade(rgb, paint.getSeColor()));
			paint.setNwColor(shade(rgb, paint.getNwColor()));
			paint.setNeColor(shade(rgb, paint.getNeColor()));
		}

		SceneTileModel model = tile.getSceneTileModel();
		if (model != null)
		{
			int[] a = model.getTriangleColorA();
			int[] b = model.getTriangleColorB();
			int[] c = model.getTriangleColorC();
			int[] fa = model.getFaceX();
			int[] fb = model.getFaceY();
			int[] fc = model.getFaceZ();
			int[] vx = model.getVertexX();
			int[] vz = model.getVertexZ();
			int[] textures = model.getTriangleTextureId();
			for (int i = 0; i < a.length; i++)
			{
				// marks a triangle that isn't drawn, leave it that way
				if (a[i] == HIDDEN)
				{
					continue;
				}

				boolean wet = textures != null && textures[i] != -1;
				int[][] field = wet ? waterField : landField;
				if (wet)
				{
					textures[i] = -1;
				}
				int rgb = sample(field, x * 128 + 64, y * 128 + 64);
				a[i] = shade(rgb, a[i]);
				b[i] = shade(rgb, b[i]);
				c[i] = shade(rgb, c[i]);
			}
		}

		// the glowing cracks
		tile.setGroundObject(null);

		for (GameObject obj : tile.getGameObjects())
		{
			if (obj == null || KEEP.contains(obj.getId()))
			{
				continue;
			}

			// objects cover several tiles, only record them once
			if (p.equals(obj.getSceneMinLocation()))
			{
				LocalPoint lp = obj.getLocalLocation();
				spots.add(new Spot(SpotType.ROCK, lp.getX(), lp.getY(), Math.max(obj.sizeX(), obj.sizeY())));
			}
			scene.removeGameObject(obj);
		}

		WallObject wall = tile.getWallObject();
		if (wall != null && !KEEP.contains(wall.getId()))
		{
			LocalPoint lp = tile.getLocalLocation();
			spots.add(new Spot(SpotType.EDGE, lp.getX(), lp.getY(), 1));
			hide(wall.getRenderable1());
			hide(wall.getRenderable2());
		}

		DecorativeObject deco = tile.getDecorativeObject();
		if (deco != null)
		{
			hide(deco.getRenderable());
			hide(deco.getRenderable2());
		}

		if (paint != null && !water)
		{
			LocalPoint lp = tile.getLocalLocation();
			spots.add(new Spot(SpotType.GROUND, lp.getX(), lp.getY(), 1));
		}
	}

	private void buildFields(Tile[][] tiles)
	{
		wet = new boolean[SIZE][SIZE];
		boolean[][] dry = new boolean[SIZE][SIZE];
		for (int x = 0; x < SIZE; x++)
		{
			for (int y = 0; y < SIZE; y++)
			{
				Tile tile = tiles[x][y];
				if (tile == null)
				{
					continue;
				}

				SceneTilePaint paint = tile.getSceneTilePaint();
				if (paint != null && paint.getNeColor() != HIDDEN)
				{
					wet[x][y] = paint.getTexture() != -1;
					dry[x][y] = paint.getTexture() == -1;
				}

				SceneTileModel model = tile.getSceneTileModel();
				int[] textures = model == null ? null : model.getTriangleTextureId();
				if (model != null)
				{
					int[] colors = model.getTriangleColorA();
					for (int i = 0; i < colors.length; i++)
					{
						if (colors[i] == HIDDEN)
						{
							continue;
						}
						if (textures != null && textures[i] != -1)
						{
							wet[x][y] = true;
						}
						else
						{
							dry[x][y] = true;
						}
					}
				}
			}
		}

		landField = new int[SIZE + 1][SIZE + 1];
		waterField = new int[SIZE + 1][SIZE + 1];
		for (int x = 0; x <= SIZE; x++)
		{
			for (int y = 0; y <= SIZE; y++)
			{
				// the four tiles that share this corner
				boolean nearWater = false;
				boolean nearLand = false;
				for (int dx = -1; dx <= 0; dx++)
				{
					for (int dy = -1; dy <= 0; dy++)
					{
						int tx = x + dx;
						int ty = y + dy;
						if (tx >= 0 && ty >= 0 && tx < SIZE && ty < SIZE)
						{
							nearWater |= wet[tx][ty];
							nearLand |= dry[tx][ty];
						}
					}
				}

				float n = 0.65f * noise(x / 7f, y / 7f) + 0.35f * noise(x / 2.5f + 31, y / 2.5f + 17);
				landField[x][y] = nearWater ? SAND : mix(GRASS_DARK, GRASS_LIGHT, n);
				waterField[x][y] = nearLand ? SHALLOW : mix(DEEP, SHALLOW, 0.25f * n);
			}
		}
	}

	private static int corner(int[][] field, int x, int y)
	{
		return field[Math.max(0, Math.min(SIZE, x))][Math.max(0, Math.min(SIZE, y))];
	}

	// bilinear lookup at a scene-local position
	private static int sample(int[][] field, int lx, int lz)
	{
		float fx = Math.max(0, Math.min(SIZE - 0.001f, lx / 128f));
		float fz = Math.max(0, Math.min(SIZE - 0.001f, lz / 128f));
		int x = (int) fx;
		int z = (int) fz;
		float u = fx - x;
		float v = fz - z;
		int bottom = mix(field[x][z], field[x + 1][z], u);
		int top = mix(field[x][z + 1], field[x + 1][z + 1], u);
		return mix(bottom, top, v);
	}

	// smooth value noise, 0..1
	private static float noise(float x, float y)
	{
		int x0 = (int) Math.floor(x);
		int y0 = (int) Math.floor(y);
		float u = x - x0;
		float v = y - y0;
		u = u * u * (3 - 2 * u);
		v = v * v * (3 - 2 * v);
		float a = hash(x0, y0) + (hash(x0 + 1, y0) - hash(x0, y0)) * u;
		float b = hash(x0, y0 + 1) + (hash(x0 + 1, y0 + 1) - hash(x0, y0 + 1)) * u;
		return a + (b - a) * v;
	}

	private static float hash(int x, int y)
	{
		int h = x * 374761393 + y * 668265263;
		h = (h ^ (h >>> 13)) * 1274126177;
		return ((h ^ (h >>> 16)) & 0xffff) / 65535f;
	}

	private static int mix(int a, int b, float t)
	{
		int ar = a >> 16 & 255;
		int ag = a >> 8 & 255;
		int ab = a & 255;
		return (int) (ar + ((b >> 16 & 255) - ar) * t) << 16
			| (int) (ag + ((b >> 8 & 255) - ag) * t) << 8
			| (int) (ab + ((b & 255) - ab) * t);
	}

	// swap the colour but keep a bit of the terrain shading so slopes still read
	private static int shade(int rgb, int lit)
	{
		short base = JagexColor.rgbToHSL(rgb, 1.0);
		int baseLum = base & 127;
		int l = lit < 0 ? 0 : lit & 127;
		int lum = baseLum - 6 + Math.min(14, l / 3);
		// & 0xfc00: blue hues set the short's sign bit
		return (base & 0xfc00) | Tone.sat(base >> 7 & 7) << 7 | Tone.lum(lum);
	}

	private void hide(Renderable renderable)
	{
		if (!(renderable instanceof Model))
		{
			return;
		}

		Model model = (Model) renderable;
		if (hidden.containsKey(model))
		{
			return;
		}

		int[] c3 = model.getFaceColors3();
		hidden.put(model, c3.clone());
		Arrays.fill(c3, -2);
	}

	// call when a scene that isn't the caves loads, before it gets uploaded
	void restore()
	{
		for (Map.Entry<Model, int[]> e : hidden.entrySet())
		{
			int[] c3 = e.getKey().getFaceColors3();
			System.arraycopy(e.getValue(), 0, c3, 0, c3.length);
		}
		hidden.clear();
	}


	// is there dry ground at this scene-local position
	boolean isLand(int lx, int ly)
	{
		int x = lx >> 7;
		int y = ly >> 7;
		return solid != null && x >= 0 && y >= 0 && x < SIZE && y < SIZE && solid[x][y] && !wet[x][y];
	}

	// is there water at this scene-local position
	boolean isWater(int lx, int ly)
	{
		int x = lx >> 7;
		int y = ly >> 7;
		return wet != null && x >= 0 && y >= 0 && x < SIZE && y < SIZE && wet[x][y];
	}
}
