package com.kirbyjad;

import com.kirbyjad.mesh.Mesh;
import com.kirbyjad.mesh.Pose;
import com.kirbyjad.mesh.Rig;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.JagexColor;
import net.runelite.api.Model;
import net.runelite.api.ModelData;

// there's no way to make a Model from scratch, so merge a cache model until it's big enough
// and overwrite its arrays every frame. leftover faces get color3 = -2 so they aren't drawn
@Slf4j
final class ModelWriter
{
	// light from the south west
	private static final float LX = -0.5f, LY = -0.75f, LZ = -0.45f;

	private final Map<Integer, Short> hslCache = new HashMap<>();

	static Model createHost(Client client, int baseModelId, int vertices, int faces)
	{
		ModelData base = client.loadModelData(baseModelId);
		if (base == null)
		{
			return null;
		}
		int copies = Math.max(1, Math.max(
			(vertices + base.getVerticesCount() - 1) / base.getVerticesCount(),
			(faces + base.getFaceCount() - 1) / base.getFaceCount()));
		for (int attempt = 0; attempt < 4; attempt++)
		{
			// merge joins verts in the same spot, so spread the copies out
			ModelData[] parts = new ModelData[copies];
			for (int i = 0; i < copies; i++)
			{
				parts[i] = base.shallowCopy().cloneVertices().translate(i * 2048, 0, 0);
			}
			ModelData merged = client.mergeModels(parts, copies);
			Model model = merged == null ? null : merged.light();
			if (model != null && model.getVerticesCount() >= vertices && model.getFaceCount() >= faces)
			{
				prepare(model);
				return model;
			}
			log.debug("host attempt with {} copies gave {} vertices / {} faces", copies,
				model == null ? -1 : model.getVerticesCount(), model == null ? -1 : model.getFaceCount());
			copies *= 2;
		}
		return null;
	}

	private static void prepare(Model model)
	{
		short[] tex = model.getFaceTextures();
		if (tex != null)
		{
			java.util.Arrays.fill(tex, (short) -1);
		}
		byte[] alpha = model.getFaceTransparencies();
		if (alpha != null)
		{
			java.util.Arrays.fill(alpha, (byte) 0);
		}
		byte[] prio = model.getFaceRenderPriorities();
		if (prio != null)
		{
			java.util.Arrays.fill(prio, (byte) 0);
		}
	}

	void write(Model model, Mesh mesh, Rig rig, Pose pose, int orientation)
	{
		// keep the light fixed when the npc turns
		double ang = orientation * Math.PI * 2 / 2048;
		float s = (float) Math.sin(ang), c = (float) Math.cos(ang);
		float mx = -LZ * s + LX * c;
		float mz = LZ * c + LX * s;
		float l = (float) Math.sqrt(mx * mx + LY * LY + mz * mz);
		rig.apply(mesh, pose, mx / l, -LY / l, -mz / l);

		float[] vx = model.getVerticesX(), vy = model.getVerticesY(), vz = model.getVerticesZ();
		int vc = mesh.vertexCount;
		for (int i = 0; i < vc; i++)
		{
			vx[i] = rig.outX[i];
			vy[i] = -rig.outY[i];
			vz[i] = -rig.outZ[i];
		}
		for (int i = vc, n = model.getVerticesCount(); i < n; i++)
		{
			vx[i] = 0;
			vy[i] = 0;
			vz[i] = 0;
		}

		int[] i1 = model.getFaceIndices1(), i2 = model.getFaceIndices2(), i3 = model.getFaceIndices3();
		int[] c1 = model.getFaceColors1(), c2 = model.getFaceColors2(), c3 = model.getFaceColors3();
		int fc = mesh.faceCount;
		for (int f = 0; f < fc; f++)
		{
			i1[f] = mesh.a[f];
			i2[f] = mesh.b[f];
			i3[f] = mesh.c[f];
			int rgb = rig.outRgb[f];
			if (rgb < 0)
			{
				c3[f] = -2;
				continue;
			}
			short hsl = hsl(rgb);
			int h = JagexColor.unpackHue(hsl), sat = JagexColor.unpackSaturation(hsl), lum = JagexColor.unpackLuminance(hsl);
			int base = h << 10 | Tone.sat(sat) << 7;
			c1[f] = base | lum(lum, rig.outI1[f]);
			c2[f] = base | lum(lum, rig.outI2[f]);
			c3[f] = base | lum(lum, rig.outI3[f]);
		}
		for (int f = fc, n = model.getFaceCount(); f < n; f++)
		{
			i1[f] = 0;
			i2[f] = 0;
			i3[f] = 0;
			c3[f] = -2;
		}
		model.calculateBoundsCylinder();
	}

	private short hsl(int rgb)
	{
		Short v = hslCache.get(rgb);
		if (v == null)
		{
			if (hslCache.size() > 4096)
			{
				hslCache.clear();
			}
			v = JagexColor.rgbToHSL(rgb, 1.0);
			hslCache.put(rgb, v);
		}
		return v;
	}

	private static int lum(int lum, float intensity)
	{
		return Tone.lum(Math.round(lum * intensity));
	}
}
