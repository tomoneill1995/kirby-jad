package com.kirbyjad;

import com.google.common.base.Strings;
import com.google.inject.Provides;
import com.kirbyjad.character.Anim;
import com.kirbyjad.character.BrontoBurt;
import com.kirbyjad.character.Gordo;
import com.kirbyjad.character.Kirby;
import com.kirbyjad.character.MetaKnight;
import com.kirbyjad.character.Scarfy;
import com.kirbyjad.character.Toon;
import com.kirbyjad.character.WaddleDee;
import com.kirbyjad.character.WaddleDoo;
import java.awt.Shape;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.Model;
import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.Renderable;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.events.BeforeRender;
import net.runelite.api.events.ClientTick;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.NpcChanged;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.api.events.PreMapLoad;
import net.runelite.api.gameval.NpcID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.callback.Hooks;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

@Slf4j
@PluginDescriptor(
	name = "Kirby Jad",
	description = "Replaces Jad with Kirby and the healers with Waddle Dees",
	tags = {"jad", "kirby", "fight caves", "cosmetic"}
)
public class KirbyJadPlugin extends Plugin
{
	// jad's model, merged a few times to get a model big enough to write into
	private static final int HOST_BASE_MODEL = 9319;

	// fight caves only, the other jads/healers have their own ids
	private static final Set<Integer> JAD_IDS = Set.of(
		NpcID.TZHAAR_FIGHTCAVE_SWARM_BOSS,
		NpcID.CLANCUP_TZHAAR_FIGHTCAVE_SWARM_BOSS
	);
	private static final int HEALER_ID = NpcID.TZHAAR_FIGHTCAVE_SWARM_BOSS_CLERIC;
	private static final MenuAction[] NPC_OPTIONS = {
		MenuAction.NPC_FIRST_OPTION,
		MenuAction.NPC_SECOND_OPTION,
		MenuAction.NPC_THIRD_OPTION,
		MenuAction.NPC_FOURTH_OPTION,
		MenuAction.NPC_FIFTH_OPTION,
	};

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private Hooks hooks;

	@Inject
	private KirbyJadConfig config;

	private final Hooks.RenderableDrawListener drawListener = this::shouldDraw;

	private Kirby kirby;
	private WaddleDee waddleDee;
	// the wave monsters, by npc id
	private Map<Integer, Toon> waves = Map.of();

	private final Map<NPC, Tracked> tracked = new HashMap<>();
	private final Dreamland dreamland = new Dreamland();
	// set from the maploader thread
	private volatile boolean inDreamland;
	private volatile List<Dreamland.Spot> pendingScenery;
	private volatile boolean clearScenery;
	private float pendingSince = -1;
	private DreamlandScenery scenery;
	private StarBurst stars;
	private float lastFrame;
	private boolean skyApplied;
	private int skyBefore;

	private final long startNanos = System.nanoTime();

	private static class Tracked
	{
		final ToonObject object;
		int lastAnimation = -1;
		int lastFrame = -1;

		Tracked(ToonObject object)
		{
			this.object = object;
		}
	}

	@Provides
	KirbyJadConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(KirbyJadConfig.class);
	}

	@Override
	protected void startUp()
	{
		hooks.registerRenderableDrawListener(drawListener);
		Tone.brightness = config.brightness() / 100f;
		scenery = new DreamlandScenery(client);
		stars = new StarBurst(client);
		clientThread.invokeLater(() ->
		{
			if (kirby == null)
			{
				kirby = new Kirby();
				waddleDee = new WaddleDee();
				waves = waveToons();
			}
			rescan();
		});
	}

	@Override
	protected void shutDown()
	{
		hooks.unregisterRenderableDrawListener(drawListener);
		clientThread.invokeLater(() ->
		{
			restoreSky();
			if (scenery != null)
			{
				scenery.clear();
			}
			if (stars != null)
			{
				stars.clear();
			}
			dreamland.restore();
			if (inDreamland)
			{
				inDreamland = false;
				if (client.getGameState() == GameState.LOGGED_IN)
				{
					client.setGameState(GameState.LOADING);
				}
			}
		});
		clientThread.invokeLater(this::clearTracked);
	}

	private float now()
	{
		return (System.nanoTime() - startNanos) / 1e9f;
	}

	private Toon toonFor(NPC npc)
	{
		if (kirby == null)
		{
			return null;
		}

		int id = npc.getId();
		if (config.replaceJad() && JAD_IDS.contains(id))
		{
			return kirby;
		}

		if (config.replaceHealers() && id == HEALER_ID)
		{
			return waddleDee;
		}

		if (config.replaceWaves())
		{
			return waves.get(id);
		}

		return null;
	}

	private static Map<Integer, Toon> waveToons()
	{
		BrontoBurt bronto = new BrontoBurt();
		Scarfy scarfy = new Scarfy(100);
		Gordo gordo = new Gordo();
		WaddleDoo doo = new WaddleDoo();
		MetaKnight meta = new MetaKnight();
		Map<Integer, Toon> m = new HashMap<>();
		m.put(NpcID.TZHAAR_FIGHTCAVE_SWARM_1A, bronto);
		m.put(NpcID.TZHAAR_FIGHTCAVE_SWARM_1B, bronto);
		m.put(NpcID.TZHAAR_FIGHTCAVE_SWARM_2A, scarfy);
		m.put(NpcID.TZHAAR_FIGHTCAVE_SWARM_2B, scarfy);
		// the two little ones a tz-kek splits into
		m.put(NpcID.TZHAAR_FIGHTCAVE_SWARM_2SPAWN, new Scarfy(55));
		m.put(NpcID.TZHAAR_FIGHTCAVE_SWARM_3A, gordo);
		m.put(NpcID.TZHAAR_FIGHTCAVE_SWARM_3B, gordo);
		m.put(NpcID.TZHAAR_FIGHTCAVE_SWARM_4A, doo);
		m.put(NpcID.TZHAAR_FIGHTCAVE_SWARM_4B, doo);
		m.put(NpcID.TZHAAR_FIGHTCAVE_SWARM_5A, meta);
		m.put(NpcID.TZHAAR_FIGHTCAVE_SWARM_5B, meta);
		return m;
	}

	private void rescan()
	{
		clearTracked();
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}

		for (NPC npc : client.getTopLevelWorldView().npcs())
		{
			track(npc);
		}
	}

	private void track(NPC npc)
	{
		Toon toon = toonFor(npc);
		if (toon == null || tracked.containsKey(npc))
		{
			return;
		}

		ToonObject object = newObject(toon);
		if (object != null)
		{
			tracked.put(npc, new Tracked(object));
		}
	}

	private ToonObject newObject(Toon toon)
	{
		Model host = ModelWriter.createHost(client, HOST_BASE_MODEL, toon.mesh.vertexCount, toon.mesh.faceCount);
		if (host == null)
		{
			log.warn("Unable to create host model for {} verts / {} faces", toon.mesh.vertexCount, toon.mesh.faceCount);
			return null;
		}

		return new ToonObject(toon, host, (float) (Math.random() * 10));
	}

	private void untrack(NPC npc)
	{
		Tracked t = tracked.remove(npc);
		if (t != null)
		{
			client.removeRuneLiteObject(t.object);
		}
	}

	private void clearTracked()
	{
		for (Tracked t : tracked.values())
		{
			client.removeRuneLiteObject(t.object);
		}
		tracked.clear();
	}

	@Subscribe
	public void onNpcSpawned(NpcSpawned event)
	{
		track(event.getNpc());
	}

	@Subscribe
	public void onNpcDespawned(NpcDespawned event)
	{
		untrack(event.getNpc());
	}

	@Subscribe
	public void onNpcChanged(NpcChanged event)
	{
		untrack(event.getNpc());
		track(event.getNpc());
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGIN_SCREEN || event.getGameState() == GameState.HOPPING)
		{
			clearTracked();
		}
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!KirbyJadConfig.GROUP.equals(event.getGroup()))
		{
			return;
		}

		clientThread.invokeLater(() ->
		{
			rescan();
			// the cave colours are baked in when the scene loads, so reload it
			Tone.brightness = config.brightness() / 100f;
			if ((event.getKey().equals("dreamland") || event.getKey().equals("brightness")) && client.getGameState() == GameState.LOGGED_IN)
			{
				client.setGameState(GameState.LOADING);
			}
		});
	}

	// hidden npcs can't be clicked, so add their options while the mouse is over the model
	@Subscribe
	public void onClientTick(ClientTick event)
	{
		if (tracked.isEmpty() || client.isMenuOpen())
		{
			return;
		}

		Point mouse = client.getMouseCanvasPosition();
		if (mouse == null || mouse.getX() < 0)
		{
			return;
		}

		MenuEntry[] existing = client.getMenu().getMenuEntries();
		// kirby first so the dees end up above him in the menu
		List<NPC> order = new ArrayList<>(tracked.keySet());
		order.sort(Comparator.comparing(npc -> tracked.get(npc).object.getToon() != kirby));
		for (NPC npc : order)
		{
			ToonObject o = tracked.get(npc).object;
			if (npc.isDead() || !client.isRuneLiteObjectRegistered(o) || hasEntries(existing, npc))
			{
				continue;
			}

			Shape clickbox = Perspective.getClickbox(client, npc.getWorldView(), o.getModel(), o.getOrientation(),
				o.getX(), o.getY(), o.getZ());
			if (clickbox != null && clickbox.contains(mouse.getX(), mouse.getY()))
			{
				addEntries(npc, o.getToon() == kirby);
			}
		}
	}

	private static boolean hasEntries(MenuEntry[] entries, NPC npc)
	{
		for (MenuEntry entry : entries)
		{
			if (entry.getNpc() == npc)
			{
				return true;
			}
		}
		return false;
	}

	private void addEntries(NPC npc, boolean isKirby)
	{
		NPCComposition comp = npc.getTransformedComposition();
		if (comp == null)
		{
			return;
		}

		String name = isKirby ? comp.getName().replace("-Jad", "-Kirby") : comp.getName();
		String target = "<col=ffff00>" + name;
		if (comp.getCombatLevel() > 0)
		{
			target += "<col=ff0000>  (level-" + comp.getCombatLevel() + ")";
		}
		int worldView = npc.getWorldView() != null ? npc.getWorldView().getId() : -1;

		// later entries go on top, so add them backwards to get the first option as left click
		client.getMenu().createMenuEntry(-1)
			.setOption("Examine")
			.setTarget(target)
			.setType(MenuAction.EXAMINE_NPC)
			.setIdentifier(npc.getIndex())
			.setWorldViewId(worldView);

		String[] actions = comp.getActions();
		for (int i = Math.min(actions.length, NPC_OPTIONS.length) - 1; i >= 0; i--)
		{
			if (Strings.isNullOrEmpty(actions[i]))
			{
				continue;
			}

			client.getMenu().createMenuEntry(-1)
				.setOption(actions[i])
				.setTarget(target)
				.setType(NPC_OPTIONS[i])
				.setIdentifier(npc.getIndex())
				.setWorldViewId(worldView);
		}
	}

	private boolean shouldDraw(Renderable renderable, boolean drawingUI)
	{
		// keep hp bars/hitsplats/overheads, just hide the model
		return drawingUI || !(renderable instanceof NPC && tracked.containsKey(renderable));
	}

	@Subscribe
	public void onPreMapLoad(PreMapLoad event)
	{
		// maploader thread, so only touch the scene here
		boolean caves = config.dreamland() && Dreamland.isFightCaves(event.getScene());
		if (caves)
		{
			pendingScenery = dreamland.paint(event.getScene());
		}
		else
		{
			// put back any cave models that were blanked, they can show up in tzhaar city too
			dreamland.restore();
			if (inDreamland)
			{
				clearScenery = true;
			}
		}
		inDreamland = caves;
	}

	private void restoreSky()
	{
		if (skyApplied)
		{
			skyApplied = false;
			client.setSkyboxColor(skyBefore);
		}
	}

	// runs after the skybox plugin, which sets the sky colour every frame too
	@Subscribe(priority = -1)
	public void onBeforeRender(BeforeRender event)
	{
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}

		if (inDreamland)
		{
			if (!skyApplied)
			{
				skyBefore = client.getSkyboxColor();
				skyApplied = true;
			}
			client.setSkyboxColor(Dreamland.SKY);
		}
		else
		{
			restoreSky();
		}

		float now = now();
		if (clearScenery)
		{
			clearScenery = false;
			scenery.clear();
		}
		// objects added while the map is still loading never get drawn, so wait for it to settle
		List<Dreamland.Spot> spots = pendingScenery;
		if (spots != null && client.getLocalPlayer() != null)
		{
			if (pendingSince < 0)
			{
				pendingSince = now;
			}
			else if (now - pendingSince > 0.5f)
			{
				pendingScenery = null;
				pendingSince = -1;
				scenery.spawn(spots, dreamland::isWater, dreamland::isLand, now);
			}
		}
		scenery.tick(now, now - lastFrame);
		stars.tick(now);
		lastFrame = now;

		for (Map.Entry<NPC, Tracked> entry : tracked.entrySet())
		{
			NPC npc = entry.getKey();
			Tracked t = entry.getValue();
			LocalPoint lp = npc.getLocalLocation();
			WorldView wv = npc.getWorldView();
			if (lp == null || wv == null)
			{
				continue;
			}

			place(t.object, lp, wv.getPlane(), npc.getCurrentOrientation());
			animate(npc, t, now);
			t.object.update(now);
		}

	}

	private void place(ToonObject object, LocalPoint lp, int plane, int orientation)
	{
		boolean registered = client.isRuneLiteObjectRegistered(object);
		if (registered && object.getWorldView() != lp.getWorldView())
		{
			client.removeRuneLiteObject(object);
			registered = false;
		}

		object.setLocation(lp, plane);
		object.setZ(Perspective.getTileHeight(client, lp, plane));
		object.setOrientation(orientation);
		if (!registered)
		{
			client.registerRuneLiteObject(object);
		}
	}

	private void animate(NPC npc, Tracked t, float now)
	{
		int animation = npc.getAnimation();
		int animFrame = npc.getAnimationFrame();
		Anim mapped = animation == -1 ? null : NpcAnimations.map(animation);
		boolean started = animation != -1 && animation != t.lastAnimation;
		// same anim again, e.g. jad doing two mage attacks in a row
		boolean restarted = animation != -1 && animation == t.lastAnimation && animFrame < t.lastFrame;
		t.lastAnimation = animation;
		t.lastFrame = animFrame;

		if (mapped == null && npc.isDead())
		{
			mapped = Anim.DEATH;
		}

		ToonObject o = t.object;
		if (mapped == Anim.DEATH && o.getAnim() != Anim.DEATH && o.getAnim() != Anim.DANCE)
		{
			// poof of stars as it goes down
			stars.spawn(npc.getLocalLocation(), now);
		}
		// kirby does his victory dance instead of dying
		if (mapped == Anim.DEATH && o.getToon() == kirby)
		{
			mapped = Anim.DANCE;
		}
		if (mapped != null)
		{
			if (started || restarted || o.getAnim() != mapped)
			{
				o.play(mapped, now, started || restarted);
			}
			return;
		}

		// let attacks finish even if the npc anim ended slightly early
		if (!o.getAnim().loops() && !o.finished(now) && o.getAnim() != Anim.DEFEND)
		{
			return;
		}

		boolean moving = npc.getPoseAnimation() != npc.getIdlePoseAnimation();
		o.play(moving ? Anim.WALK : Anim.IDLE, now, false);
	}
}
