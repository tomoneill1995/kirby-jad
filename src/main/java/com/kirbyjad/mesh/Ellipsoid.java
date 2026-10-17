package com.kirbyjad.mesh;

public final class Ellipsoid
{
	public final float cx, cy, cz;
	public final float rx, ry, rz;
	final float[] m;

	public Ellipsoid(float cx, float cy, float cz, float rx, float ry, float rz)
	{
		this(cx, cy, cz, rx, ry, rz, 0, 0, 0);
	}

	public Ellipsoid(float cx, float cy, float cz, float rx, float ry, float rz, float pitch, float yaw, float roll)
	{
		this.cx = cx;
		this.cy = cy;
		this.cz = cz;
		this.rx = rx;
		this.ry = ry;
		this.rz = rz;
		this.m = Mat.rotation(pitch, yaw, roll);
	}

	// d is a unit direction in local space
	public void surface(float dx, float dy, float dz, float offset, float[] out)
	{
		float px = dx * rx, py = dy * ry, pz = dz * rz;
		if (offset != 0)
		{
			float nx = dx / rx, ny = dy / ry, nz = dz / rz;
			float l = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
			px += nx / l * offset;
			py += ny / l * offset;
			pz += nz / l * offset;
		}
		out[0] = cx + m[0] * px + m[1] * py + m[2] * pz;
		out[1] = cy + m[3] * px + m[4] * py + m[5] * pz;
		out[2] = cz + m[6] * px + m[7] * py + m[8] * pz;
	}

	public void normal(float dx, float dy, float dz, float[] out)
	{
		float nx = dx / rx, ny = dy / ry, nz = dz / rz;
		out[0] = m[0] * nx + m[1] * ny + m[2] * nz;
		out[1] = m[3] * nx + m[4] * ny + m[5] * nz;
		out[2] = m[6] * nx + m[7] * ny + m[8] * nz;
	}
}
