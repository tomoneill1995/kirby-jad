package com.kirbyjad.mesh;

public final class Mat
{
	private Mat()
	{
	}

	// Ry * Rx * Rz, +pitch leans forward
	public static float[] rotation(float pitch, float yaw, float roll)
	{
		float[] r = new float[9];
		rotation(pitch, yaw, roll, r);
		return r;
	}

	public static void rotation(float pitch, float yaw, float roll, float[] r)
	{
		float cp = (float) Math.cos(pitch), sp = (float) Math.sin(pitch);
		float cy = (float) Math.cos(yaw), sy = (float) Math.sin(yaw);
		float cr = (float) Math.cos(roll), sr = (float) Math.sin(roll);
		// Rx * Rz first
		float a00 = cr, a01 = -sr, a02 = 0;
		float a10 = cp * sr, a11 = cp * cr, a12 = -sp;
		float a20 = sp * sr, a21 = sp * cr, a22 = cp;
		r[0] = cy * a00 + sy * a20;
		r[1] = cy * a01 + sy * a21;
		r[2] = cy * a02 + sy * a22;
		r[3] = a10;
		r[4] = a11;
		r[5] = a12;
		r[6] = -sy * a00 + cy * a20;
		r[7] = -sy * a01 + cy * a21;
		r[8] = -sy * a02 + cy * a22;
	}

	// 3x4 affine, last row is implied
	public static void mul(float[] a, float[] b, float[] out)
	{
		float[] o = new float[12];
		for (int i = 0; i < 3; i++)
		{
			for (int j = 0; j < 4; j++)
			{
				float v = a[i * 4] * b[j] + a[i * 4 + 1] * b[4 + j] + a[i * 4 + 2] * b[8 + j];
				if (j == 3)
				{
					v += a[i * 4 + 3];
				}
				o[i * 4 + j] = v;
			}
		}
		System.arraycopy(o, 0, out, 0, 12);
	}
}
