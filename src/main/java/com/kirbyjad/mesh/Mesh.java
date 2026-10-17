package com.kirbyjad.mesh;

// y up, z = front. ModelWriter flips y and z for the game.
public final class Mesh
{
	public static final int FLAG_EMISSIVE = 1;
	public static final int FLAG_STONE = 2;

	public final int vertexCount;
	public final float[] x;
	public final float[] y;
	public final float[] z;
	public final int[] bone;

	public final int faceCount;
	public final int[] a;
	public final int[] b;
	public final int[] c;
	public final int[] rgb;
	public final int[] flags;

	public final String[] morphNames;
	public final int[][] morphIndices;
	public final float[][] morphDx;
	public final float[][] morphDy;
	public final float[][] morphDz;

	Mesh(int vertexCount, float[] x, float[] y, float[] z, int[] bone,
		int faceCount, int[] a, int[] b, int[] c, int[] rgb, int[] flags,
		String[] morphNames, int[][] morphIndices, float[][] morphDx, float[][] morphDy, float[][] morphDz)
	{
		this.vertexCount = vertexCount;
		this.x = x;
		this.y = y;
		this.z = z;
		this.bone = bone;
		this.faceCount = faceCount;
		this.a = a;
		this.b = b;
		this.c = c;
		this.rgb = rgb;
		this.flags = flags;
		this.morphNames = morphNames;
		this.morphIndices = morphIndices;
		this.morphDx = morphDx;
		this.morphDy = morphDy;
		this.morphDz = morphDz;
	}

	public int morphIndex(String name)
	{
		for (int i = 0; i < morphNames.length; i++)
		{
			if (morphNames[i].equals(name))
			{
				return i;
			}
		}
		throw new IllegalArgumentException("no morph " + name);
	}
}
