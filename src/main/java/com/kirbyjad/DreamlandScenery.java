package com.kirbyjad;

import com.kirbyjad.character.Butterfly;
import com.kirbyjad.character.Prop;
import com.kirbyjad.character.Toon;
import com.kirbyjad.character.WhispyWoods;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.BiPredicate;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Constants;
import net.runelite.api.Model;
import net.runelite.api.Perspective;
import net.runelite.api.RuneLiteObjectController;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;

// the hills, trees, flowers, stars and clouds put into the caves. client thread only
@Slf4j
class DreamlandScenery
{
	private static final int HOST_BASE_MODEL = 9319;
	private static final int CELL_SHIFT = 9;
	private static final int MAX_FLOWERS = 60;
	private static final int MAX_WHISPYS = 3;
	private static final int MAX_MESAS = 5;
	private static final int[] HILL_SIZES = {260, 300, 220};
	private static final int[] MESA_SIZES = {240, 290, 210};
	private static final int[] PETALS = {0xff8fc8, 0xffffff, 0xff6b6b, 0xb98cff};

	private final Client client;
	private final List<PropObject> objects = new ArrayList<>();
	private final List<Living> living = new ArrayList<>();
	private float spawnedAt;
	private boolean reRegistered;

	// every placed prop gets its own model, sharing one between objects stops them drawing
	private Prop[] hills;
	private Prop[] mesas;
	private Prop[] hedges;
	private Prop[] bushes;
	private Prop[] trees;
	private Prop[] flowers;
	private Prop[] clouds;
	private Prop star;

	private static class PropObject extends RuneLiteObjectController
	{
		final Model model;
		// jau per second, for the stars
		int spin;

		PropObject(Model model)
		{
			this.model = model;
		}

		@Override
		public Model getModel()
		{
			return model;
		}
	}

	DreamlandScenery(Client client)
	{
		this.client = client;
	}

	private boolean loadModels()
	{
		if (star != null)
		{
			return true;
		}

		hills = new Prop[]{Prop.hill(HILL_SIZES[0], 1), Prop.hill(HILL_SIZES[1], 2), Prop.hill(HILL_SIZES[2], 3)};
		hedges = new Prop[]{Prop.hedge(31), Prop.hedge(32), Prop.hedge(33)};
		mesas = new Prop[]{Prop.mesa(MESA_SIZES[0], 340, 11), Prop.mesa(MESA_SIZES[1], 400, 12), Prop.mesa(MESA_SIZES[2], 300, 13)};
		bushes = new Prop[]{Prop.hill(110, 4), Prop.hill(90, 5)};
		trees = new Prop[]{Prop.tree(150, 6), Prop.tree(130, 7)};
		flowers = new Prop[PETALS.length];
		for (int i = 0; i < PETALS.length; i++)
		{
			flowers[i] = Prop.flower(PETALS[i]);
		}
		clouds = new Prop[]{Prop.cloud(8), Prop.cloud(9), Prop.cloud(10)};
		star = Prop.star(70);
		return true;
	}

	private Model build(Prop prop)
	{
		Model model = ModelWriter.createHost(client, HOST_BASE_MODEL, prop.mesh.vertexCount, prop.mesh.faceCount);
		if (model == null)
		{
			log.warn("Unable to create host model for dreamland prop");
			return null;
		}
		new ModelWriter().write(model, prop.mesh, prop.rig, prop.newPose(), 0);
		return model;
	}

	void spawn(List<Dreamland.Spot> spots, BiPredicate<Integer, Integer> water, BiPredicate<Integer, Integer> land, float now)
	{
		clear();
		spawnedAt = now;
		reRegistered = false;
		if (!loadModels())
		{
			return;
		}

		WorldView wv = client.getTopLevelWorldView();
		Random rng = new Random(9551);
		List<Dreamland.Spot> ground = new ArrayList<>();
		List<Dreamland.Spot> edges = new ArrayList<>();
		int minX = Integer.MAX_VALUE;
		int maxX = Integer.MIN_VALUE;
		int minY = Integer.MAX_VALUE;
		int maxY = Integer.MIN_VALUE;

		// the caves are made of thousands of little rocks. one prop per rock is far more than the gpu
		// plugin will draw, so group them into 4x4 tile cells and put one thing in each
		Map<Long, int[]> cells = new HashMap<>();
		for (Dreamland.Spot spot : spots)
		{
			if (spot.type == Dreamland.SpotType.GROUND)
			{
				ground.add(spot);
				minX = Math.min(minX, spot.x);
				maxX = Math.max(maxX, spot.x);
				minY = Math.min(minY, spot.y);
				maxY = Math.max(maxY, spot.y);
				continue;
			}

			if (spot.type == Dreamland.SpotType.EDGE)
			{
				edges.add(spot);
				continue;
			}

			long key = (long) (spot.x >> CELL_SHIFT) << 32 | (spot.y >> CELL_SHIFT);
			int[] cell = cells.computeIfAbsent(key, k -> new int[3]);
			int weight = spot.size * spot.size;
			cell[0] += spot.x * weight;
			cell[1] += spot.y * weight;
			cell[2] += weight;
		}

		// biggest clumps of rock first so the big props get the space
		List<int[]> sorted = new ArrayList<>(cells.values());
		sorted.sort((a, b) -> b[2] - a[2]);
		List<int[]> taken = new ArrayList<>();
		Walkable walk = Walkable.compute(client);
		int midX = (minX + maxX) / 2;
		int midY = (minY + maxY) / 2;
		Fitter fit = new Fitter(wv, rng, taken, walk, water, land, midX, midY, (long) ((maxX - minX) / 2 * 0.75f));

		// a hedge all the way round the walkable area, on the first tiles you can't walk on
		for (int[] t : walk.boundary())
		{
			if (land.test(t[0], t[1]))
			{
				taken.add(new int[]{t[0], t[1], 60});
				add(hedges[rng.nextInt(hedges.length)], wv, t[0], t[1], 0, rng.nextInt(4) * 512);
			}
		}

		for (int[] cell : sorted)
		{
			fit.place(cell[0] / cell[2], cell[1] / cell[2], cell[2] >= 6);
		}

		// close up the ring: along the old walls and rocks, walk outward off the rim rivers until something fits,
		// then a row of bigger hills further back so the view ends in hills, not open grass
		edges.addAll(rocks(spots));
		for (Dreamland.Spot spot : edges)
		{
			double out = Math.atan2(spot.y - midY, spot.x - midX);
			for (int step = 0; step <= 5; step++)
			{
				int x = spot.x + (int) (Math.cos(out) * step * 180);
				int y = spot.y + (int) (Math.sin(out) * step * 180);
				if (fit.place(x, y, true))
				{
					break;
				}
			}
			fit.place(spot.x + (int) (Math.cos(out) * 900), spot.y + (int) (Math.sin(out) * 900), true);
		}

		Collections.shuffle(ground, rng);
		for (int i = 0; i < Math.min(MAX_FLOWERS, ground.size()); i++)
		{
			Dreamland.Spot spot = ground.get(i);
			int fx = spot.x + rng.nextInt(80) - 40;
			int fy = spot.y + rng.nextInt(80) - 40;
			if (!overlaps(taken, fx, fy, 30))
			{
				add(flowers[rng.nextInt(flowers.length)], wv, fx, fy, 0, rng.nextInt(2048));
			}
		}

		// a few warp stars floating over the floor
		for (int i = 0; i < 5 && !ground.isEmpty(); i++)
		{
			Dreamland.Spot spot = ground.get(rng.nextInt(ground.size()));
			PropObject s = add(star, wv, spot.x, spot.y, -420, rng.nextInt(2048));
			if (s != null)
			{
				s.spin = 600 + rng.nextInt(400);
			}
		}

		// clouds in a ring around the arena, up high
		if (!ground.isEmpty())
		{
			int cx = (minX + maxX) / 2;
			int cy = (minY + maxY) / 2;
			int radius = Math.max(maxX - minX, maxY - minY) / 2 + 600;
			// objects outside the scene don't get drawn
			int edge = Constants.SCENE_SIZE * Perspective.LOCAL_TILE_SIZE - 256;
			for (int i = 0; i < 12; i++)
			{
				double a = Math.PI * 2 * i / 12 + rng.nextDouble() * 0.3;
				int r = radius + rng.nextInt(900);
				int x = Math.max(256, Math.min(edge, cx + (int) (Math.cos(a) * r)));
				int y = Math.max(256, Math.min(edge, cy + (int) (Math.sin(a) * r)));
				// face the clouds side on to the middle
				int orientation = (int) Math.round(-a * 1024 / Math.PI) & 2047;
				add(clouds[rng.nextInt(clouds.length)], wv, x, y, -1400 - rng.nextInt(800), orientation);
			}

			// rainbow over the north side and whispy woods watching from the north east
			// one big rainbow arching right over the arena from side to side
			int span = (maxX - minX) / 2 + 300;
			add(Prop.rainbow(span, 120), wv, cx, cy, 0, 0);

			// butterflies drifting round over the grass
			int[] wings = {0xff9fd0, 0xffe25a, 0x9fd8ff};
			for (int i = 0; i < 6; i++)
			{
				Dreamland.Spot spot = ground.get(rng.nextInt(ground.size()));
				living(new Butterfly(wings[i % wings.length]), wv, spot.x, spot.y, -70 - rng.nextInt(60),
					200 + rng.nextInt(250), 0.4f + rng.nextFloat() * 0.4f, rng.nextFloat() * 6, 1);
			}
		}
	}

	// puts the biggest prop that fits at a spot: never on water, never past the edge of the land,
	// never overlapping another prop or spreading over a tile the player can walk on
	private class Fitter
	{
		final WorldView wv;
		final Random rng;
		final List<int[]> taken;
		final Walkable walk;
		final BiPredicate<Integer, Integer> water;
		final BiPredicate<Integer, Integer> land;
		final int midX;
		final int midY;
		final long rim;
		int whispys;
		int mesaCount;

		Fitter(WorldView wv, Random rng, List<int[]> taken, Walkable walk, BiPredicate<Integer, Integer> water,
			BiPredicate<Integer, Integer> land, int midX, int midY, long rim)
		{
			this.wv = wv;
			this.rng = rng;
			this.taken = taken;
			this.walk = walk;
			this.water = water;
			this.land = land;
			this.midX = midX;
			this.midY = midY;
			this.rim = rim;
		}

		boolean place(int x, int y, boolean big)
		{
			if (!land.test(x, y))
			{
				return false;
			}

			int room = walk.room(x, y);
			if (big)
			{
				long ex = x - midX;
				long ey = y - midY;
				if (mesaCount < MAX_MESAS && ex * ex + ey * ey > rim * rim && rng.nextInt(2) == 0)
				{
					int i = rng.nextInt(mesas.length);
					if (put(mesas[i], x, y, (int) (MESA_SIZES[i] * 1.05f), room))
					{
						mesaCount++;
						return true;
					}
				}
				int i = rng.nextInt(hills.length);
				if (put(hills[i], x, y, (int) (HILL_SIZES[i] * 1.55f), room))
				{
					return true;
				}
			}

			if (whispys < MAX_WHISPYS && rng.nextInt(6) == 0 && fits(x, y, 280, room))
			{
				taken.add(new int[]{x, y, 280});
				Living tree = living(new WhispyWoods(150), wv, x, y, 0, 0, 0, rng.nextFloat() * 6, 0);
				if (tree != null)
				{
					// face the middle of the arena
					tree.facing = (int) Math.round(Math.atan2(x - midX, y - midY) * 1024 / Math.PI) & 2047;
				}
				whispys++;
				return true;
			}
			if (rng.nextInt(3) == 0 && put(trees[rng.nextInt(trees.length)], x, y, 140, room))
			{
				return true;
			}
			return put(bushes[rng.nextInt(bushes.length)], x, y, 170, room);
		}

		private boolean fits(int x, int y, int extent, int room)
		{
			return extent <= room && !overlaps(taken, x, y, extent) && !wet(water, x, y, extent);
		}

		private boolean put(Prop prop, int x, int y, int extent, int room)
		{
			if (!fits(x, y, extent, room))
			{
				return false;
			}
			taken.add(new int[]{x, y, extent});
			add(prop, wv, x, y, 0, rng.nextInt(2048));
			return true;
		}
	}

	private static List<Dreamland.Spot> rocks(List<Dreamland.Spot> spots)
	{
		List<Dreamland.Spot> out = new ArrayList<>();
		for (Dreamland.Spot s : spots)
		{
			if (s.type == Dreamland.SpotType.ROCK)
			{
				out.add(s);
			}
		}
		return out;
	}

	// checks the middle and a ring round the footprint
	private static boolean wet(BiPredicate<Integer, Integer> water, int x, int y, int r)
	{
		if (water.test(x, y))
		{
			return true;
		}
		int reach = r * 6 / 10;
		for (int i = 0; i < 8; i++)
		{
			double a = Math.PI * 2 * i / 8;
			if (water.test(x + (int) (Math.cos(a) * reach), y + (int) (Math.sin(a) * reach)))
			{
				return true;
			}
		}
		return false;
	}

	private static boolean overlaps(List<int[]> taken, int x, int y, int r)
	{
		for (int[] t : taken)
		{
			long dx = t[0] - x;
			long dy = t[1] - y;
			long min = (long) ((t[2] + r) * 0.8f);
			if (dx * dx + dy * dy < min * min)
			{
				return true;
			}
		}
		return false;
	}

	private Living living(Toon toon, WorldView wv, int x, int y, int height, int radius, float speed, float phase, int moves)
	{
		Model model = ModelWriter.createHost(client, HOST_BASE_MODEL, toon.mesh.vertexCount, toon.mesh.faceCount);
		if (model == null)
		{
			return null;
		}

		Living l = new Living(new ToonObject(toon, model, phase), wv, x, y, height, radius, speed, phase);
		l.moves = moves != 0;
		l.place(client, 0);
		client.registerRuneLiteObject(l.object);
		living.add(l);
		return l;
	}

	// animated scenery, butterflies circle around a spot
	private static class Living
	{
		final ToonObject object;
		final WorldView wv;
		final int cx;
		final int cy;
		final int height;
		final int radius;
		final float speed;
		final float phase;
		boolean moves;
		// which way it faces when it doesn't move
		int facing;

		Living(ToonObject object, WorldView wv, int cx, int cy, int height, int radius, float speed, float phase)
		{
			this.object = object;
			this.wv = wv;
			this.cx = cx;
			this.cy = cy;
			this.height = height;
			this.radius = radius;
			this.speed = speed;
			this.phase = phase;
		}

		void place(Client client, float now)
		{
			int x = cx;
			int y = cy;
			int orientation = facing;
			if (moves)
			{
				// wobbly circle
				double a = phase + now * speed;
				double r = radius * (1 + 0.25 * Math.sin(a * 2.3));
				x += (int) (Math.cos(a) * r);
				y += (int) (Math.sin(a) * r);
				// face along the path
				orientation = (int) Math.round((Math.PI - a) * 1024 / Math.PI) & 2047;
			}
			LocalPoint lp = new LocalPoint(x, y, wv);
			object.setLocation(lp, wv.getPlane());
			int ground = lp.isInScene() ? Perspective.getTileHeight(client, lp, wv.getPlane()) : 0;
			object.setZ(ground + height);
			object.setOrientation(orientation);
		}
	}

	private PropObject add(Prop prop, WorldView wv, int x, int y, int height, int orientation)
	{
		Model model = build(prop);
		if (model == null)
		{
			return null;
		}

		PropObject o = new PropObject(model);
		LocalPoint lp = new LocalPoint(x, y, wv);
		o.setLocation(lp, wv.getPlane());
		// out past the scene getTileHeight can't help, the floor there is flat anyway
		int ground = lp.isInScene() ? Perspective.getTileHeight(client, lp, wv.getPlane()) : 0;
		o.setZ(ground + height);
		o.setOrientation(orientation);
		client.registerRuneLiteObject(o);
		objects.add(o);
		return o;
	}

	void tick(float now, float delta)
	{
		// sometimes they still don't draw if added too soon after the load, registering again fixes it
		if (!reRegistered && !objects.isEmpty() && now - spawnedAt > 1.5f)
		{
			reRegistered = true;
			for (PropObject o : objects)
			{
				client.removeRuneLiteObject(o);
				client.registerRuneLiteObject(o);
			}
			for (Living l : living)
			{
				client.removeRuneLiteObject(l.object);
				client.registerRuneLiteObject(l.object);
			}
		}

		for (Living l : living)
		{
			if (!client.isRuneLiteObjectRegistered(l.object))
			{
				client.registerRuneLiteObject(l.object);
			}
			l.place(client, now);
			l.object.update(now);
		}

		for (PropObject o : objects)
		{
			// the client drops these when it swaps in a new scene, which can happen after they were added
			if (!client.isRuneLiteObjectRegistered(o))
			{
				client.registerRuneLiteObject(o);
			}
			if (o.spin != 0)
			{
				o.setOrientation((o.getOrientation() + (int) (o.spin * delta)) & 2047);
			}
		}
	}

	void clear()
	{
		for (PropObject o : objects)
		{
			client.removeRuneLiteObject(o);
		}
		objects.clear();
		for (Living l : living)
		{
			client.removeRuneLiteObject(l.object);
		}
		living.clear();
	}
}
