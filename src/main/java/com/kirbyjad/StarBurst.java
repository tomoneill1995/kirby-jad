package com.kirbyjad;

import com.kirbyjad.character.Prop;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import net.runelite.api.Client;
import net.runelite.api.Model;
import net.runelite.api.Perspective;
import net.runelite.api.RuneLiteObjectController;
import net.runelite.api.coords.LocalPoint;

// ring of little stars that fly out when an enemy dies, like the kirby games
class StarBurst
{
	private static final int HOST_BASE_MODEL = 9319;
	private static final int STARS = 8;
	private static final float LIFE = 0.8f;

	private final Client client;
	private final Prop prop = Prop.star(24);
	// star models are reused once a burst is over
	private final Deque<Model> spare = new ArrayDeque<>();
	private final List<Star> live = new ArrayList<>();

	private static class Star extends RuneLiteObjectController
	{
		final Model model;
		LocalPoint origin;
		float dx;
		float dy;
		float born;

		Star(Model model)
		{
			this.model = model;
		}

		@Override
		public Model getModel()
		{
			return model;
		}
	}

	StarBurst(Client client)
	{
		this.client = client;
	}

	void spawn(LocalPoint at, float now)
	{
		for (int i = 0; i < STARS; i++)
		{
			Model model = spare.isEmpty() ? build() : spare.pop();
			if (model == null)
			{
				return;
			}

			double a = Math.PI * 2 * i / STARS;
			Star s = new Star(model);
			s.origin = at;
			s.dx = (float) Math.cos(a) * 420;
			s.dy = (float) Math.sin(a) * 420;
			s.born = now;
			s.setOrientation((int) (a * 1024 / Math.PI) & 2047);
			move(s, 0);
			client.registerRuneLiteObject(s);
			live.add(s);
		}
	}

	private Model build()
	{
		Model model = ModelWriter.createHost(client, HOST_BASE_MODEL, prop.mesh.vertexCount, prop.mesh.faceCount);
		if (model != null)
		{
			new ModelWriter().write(model, prop.mesh, prop.rig, prop.newPose(), 0);
		}
		return model;
	}

	private void move(Star s, float t)
	{
		float k = t / LIFE;
		// fast at first then slowing, with a little hop
		float out = 1 - (1 - k) * (1 - k);
		LocalPoint lp = new LocalPoint(s.origin.getX() + (int) (s.dx * out), s.origin.getY() + (int) (s.dy * out),
			s.origin.getWorldView());
		s.setLocation(lp, client.getTopLevelWorldView().getPlane());
		int ground = lp.isInScene() ? Perspective.getTileHeight(client, lp, client.getTopLevelWorldView().getPlane()) : 0;
		s.setZ(ground - 120 - (int) (160 * Math.sin(k * Math.PI)));
	}

	void tick(float now)
	{
		Iterator<Star> it = live.iterator();
		while (it.hasNext())
		{
			Star s = it.next();
			float t = now - s.born;
			if (t > LIFE)
			{
				client.removeRuneLiteObject(s);
				spare.push(s.model);
				it.remove();
				continue;
			}
			move(s, t);
			s.setOrientation((s.getOrientation() + 60) & 2047);
		}
	}

	void clear()
	{
		for (Star s : live)
		{
			client.removeRuneLiteObject(s);
			spare.push(s.model);
		}
		live.clear();
	}
}
