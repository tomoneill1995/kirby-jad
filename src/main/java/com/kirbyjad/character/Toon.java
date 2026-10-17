package com.kirbyjad.character;

import com.kirbyjad.mesh.Mesh;
import com.kirbyjad.mesh.Pose;
import com.kirbyjad.mesh.Rig;

public abstract class Toon
{
	public final Mesh mesh;
	public final Rig rig;

	protected Toon(Mesh mesh, Rig rig)
	{
		this.mesh = mesh;
		this.rig = rig;
	}

	public Pose newPose()
	{
		return new Pose(rig.boneCount(), mesh.morphNames.length);
	}

	// seconds, 0 for looping anims
	public abstract float duration(Anim anim);

	// t = time since the anim started, clock = always running (loops, blinking)
	public abstract void pose(Anim anim, float t, float clock, Pose out);

	public abstract Anim[] showcase();
}
