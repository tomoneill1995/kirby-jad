package com.kirbyjad;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import net.runelite.api.Client;
import net.runelite.api.CollisionData;
import net.runelite.api.CollisionDataFlag;
import net.runelite.api.Constants;
import net.runelite.api.Player;
import net.runelite.api.coords.LocalPoint;

// every tile the player can walk to from where they're standing, so scenery can stay off them.
// flood fills the collision map, walls block in their direction like they do in game
class Walkable
{
	private static final int SIZE = Constants.SCENE_SIZE;
	private static final int BLOCKED = CollisionDataFlag.BLOCK_MOVEMENT_FULL;

	private final boolean[][] reachable = new boolean[SIZE][SIZE];

	static Walkable compute(Client client)
	{
		Walkable w = new Walkable();
		Player player = client.getLocalPlayer();
		CollisionData[] maps = client.getTopLevelWorldView().getCollisionMaps();
		if (player == null || maps == null)
		{
			return w;
		}

		LocalPoint lp = player.getLocalLocation();
		int[][] flags = maps[client.getTopLevelWorldView().getPlane()].getFlags();
		int sx = lp.getSceneX();
		int sy = lp.getSceneY();
		if (sx < 0 || sy < 0 || sx >= SIZE || sy >= SIZE)
		{
			return w;
		}

		Deque<int[]> queue = new ArrayDeque<>();
		w.reachable[sx][sy] = true;
		queue.add(new int[]{sx, sy});
		while (!queue.isEmpty())
		{
			int[] t = queue.poll();
			int x = t[0];
			int y = t[1];
			w.step(queue, flags, x, y, x + 1, y, CollisionDataFlag.BLOCK_MOVEMENT_EAST, CollisionDataFlag.BLOCK_MOVEMENT_WEST);
			w.step(queue, flags, x, y, x - 1, y, CollisionDataFlag.BLOCK_MOVEMENT_WEST, CollisionDataFlag.BLOCK_MOVEMENT_EAST);
			w.step(queue, flags, x, y, x, y + 1, CollisionDataFlag.BLOCK_MOVEMENT_NORTH, CollisionDataFlag.BLOCK_MOVEMENT_SOUTH);
			w.step(queue, flags, x, y, x, y - 1, CollisionDataFlag.BLOCK_MOVEMENT_SOUTH, CollisionDataFlag.BLOCK_MOVEMENT_NORTH);
		}
		return w;
	}

	private void step(Deque<int[]> queue, int[][] flags, int x, int y, int nx, int ny, int out, int in)
	{
		if (nx < 0 || ny < 0 || nx >= SIZE || ny >= SIZE || reachable[nx][ny])
		{
			return;
		}
		if ((flags[x][y] & out) != 0 || (flags[nx][ny] & (in | BLOCKED)) != 0)
		{
			return;
		}
		reachable[nx][ny] = true;
		queue.add(new int[]{nx, ny});
	}

	// how far a prop centred here can spread before it covers a walkable tile, in local units
	int room(int lx, int ly)
	{
		int tx = lx >> 7;
		int ty = ly >> 7;
		long best = Long.MAX_VALUE;
		for (int x = Math.max(0, tx - 8); x <= Math.min(SIZE - 1, tx + 8); x++)
		{
			for (int y = Math.max(0, ty - 8); y <= Math.min(SIZE - 1, ty + 8); y++)
			{
				if (!reachable[x][y])
				{
					continue;
				}
				// distance to the nearest point of that tile, not its middle
				long dx = Math.max(0, Math.abs(lx - (x * 128 + 64)) - 64);
				long dy = Math.max(0, Math.abs(ly - (y * 128 + 64)) - 64);
				best = Math.min(best, dx * dx + dy * dy);
			}
		}
		return best == Long.MAX_VALUE ? 2000 : (int) Math.sqrt(best);
	}

	// tiles you can't walk on that touch one you can, the line the hedge runs along
	List<int[]> boundary()
	{
		List<int[]> out = new ArrayList<>();
		for (int x = 0; x < SIZE; x++)
		{
			for (int y = 0; y < SIZE; y++)
			{
				if (reachable[x][y])
				{
					continue;
				}
				boolean touches = false;
				for (int dx = -1; dx <= 1 && !touches; dx++)
				{
					for (int dy = -1; dy <= 1 && !touches; dy++)
					{
						int nx = x + dx;
						int ny = y + dy;
						touches = nx >= 0 && ny >= 0 && nx < SIZE && ny < SIZE && reachable[nx][ny];
					}
				}
				if (touches)
				{
					out.add(new int[]{x * 128 + 64, y * 128 + 64});
				}
			}
		}
		return out;
	}
}
