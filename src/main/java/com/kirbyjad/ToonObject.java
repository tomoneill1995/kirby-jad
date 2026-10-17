package com.kirbyjad;

import com.kirbyjad.character.Anim;
import com.kirbyjad.character.Toon;
import com.kirbyjad.mesh.Pose;
import lombok.Getter;
import net.runelite.api.Model;
import net.runelite.api.RuneLiteObjectController;

class ToonObject extends RuneLiteObjectController
{
	private static final float BLEND_TIME = 0.16f;

	@Getter
	private final Toon toon;
	private final Model model;
	private final ModelWriter writer = new ModelWriter();
	private final Pose target;
	private final Pose from;
	private final Pose out;
	// so multiple dees don't blink in sync
	private final float phase;

	@Getter
	private Anim anim = Anim.IDLE;
	private float animStart;
	private float blendStart = -1;

	ToonObject(Toon toon, Model model, float phase)
	{
		this.toon = toon;
		this.model = model;
		this.phase = phase;
		target = toon.newPose();
		from = toon.newPose();
		out = toon.newPose();
		toon.pose(Anim.IDLE, 0, phase, out);
	}

	void play(Anim next, float now, boolean restart)
	{
		if (next == anim && !restart)
		{
			return;
		}

		from.copyFrom(out);
		blendStart = now;
		anim = next;
		animStart = now;
	}

	boolean finished(float now)
	{
		return !anim.loops() && now - animStart >= toon.duration(anim);
	}

	void update(float now)
	{
		float t = now - animStart;
		if (!anim.loops())
		{
			t = Math.min(t, toon.duration(anim));
		}
		toon.pose(anim, t, now + phase, target);

		float k = blendStart < 0 ? 1 : (now - blendStart) / BLEND_TIME;
		if (k < 1)
		{
			k = k * k * (3 - 2 * k);
			out.blend(from, target, k);
		}
		else
		{
			out.copyFrom(target);
		}

		writer.write(model, toon.mesh, toon.rig, out, getOrientation());
	}

	@Override
	public Model getModel()
	{
		return model;
	}
}
