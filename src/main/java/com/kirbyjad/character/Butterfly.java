package com.kirbyjad.character;

import static com.kirbyjad.character.Curve.TAU;
import static com.kirbyjad.character.Curve.sin;
import com.kirbyjad.mesh.Ellipsoid;
import com.kirbyjad.mesh.Mesh;
import com.kirbyjad.mesh.MeshBuilder;
import com.kirbyjad.mesh.Pose;
import com.kirbyjad.mesh.Rig;
import com.kirbyjad.mesh.Shapes;

// flaps its wings, the scenery moves it around
public final class Butterfly extends Toon
{
	private static final int ROOT = 0, WING_A = 1, WING_B = 2;

	public Butterfly(int color)
	{
		super(build(color), new Rig(new int[]{-1, ROOT, ROOT}, new float[]{0, 0, 0}, new float[]{0, 0, 0}, new float[]{0, 0, 0}));
	}

	private static Mesh build(int color)
	{
		MeshBuilder mb = new MeshBuilder();
		Shapes.ellipsoid(mb, new Ellipsoid(0, 0, 0, 2.5f, 2.5f, 9), 4, 8, ROOT, Shapes.solid(0x3a2a40), 0);
		for (int s = -1; s <= 1; s += 2)
		{
			int bone = s < 0 ? WING_A : WING_B;
			// big front wing and a smaller back one, both flat
			Shapes.ellipsoid(mb, new Ellipsoid(s * 10, 0, 4, 10, 1, 8, 0, s * 0.3f, 0), 3, 10, bone, Shapes.solid(color), 0);
			Shapes.ellipsoid(mb, new Ellipsoid(s * 8, 0, -6, 7, 1, 6, 0, -s * 0.3f, 0), 3, 10, bone, Shapes.solid(0xffffff), 0);
		}
		return mb.build();
	}

	@Override
	public float duration(Anim anim)
	{
		return 0;
	}

	@Override
	public Anim[] showcase()
	{
		return new Anim[]{Anim.IDLE};
	}

	@Override
	public void pose(Anim anim, float t, float clock, Pose p)
	{
		p.reset();
		float flap = 0.9f * sin(TAU * clock / 0.28f);
		p.roll[WING_A] = -flap;
		p.roll[WING_B] = flap;
		p.ty[ROOT] = 4 * sin(TAU * clock / 0.28f);
	}
}
