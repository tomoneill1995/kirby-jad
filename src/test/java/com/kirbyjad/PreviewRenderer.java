package com.kirbyjad;

import com.kirbyjad.character.Anim;
import com.kirbyjad.character.BrontoBurt;
import com.kirbyjad.character.Butterfly;
import com.kirbyjad.character.Gordo;
import com.kirbyjad.character.Kirby;
import com.kirbyjad.character.MetaKnight;
import com.kirbyjad.character.Prop;
import com.kirbyjad.character.Scarfy;
import com.kirbyjad.character.Toon;
import com.kirbyjad.character.WaddleDee;
import com.kirbyjad.character.WaddleDoo;
import com.kirbyjad.character.WhispyWoods;
import com.kirbyjad.mesh.Mesh;
import com.kirbyjad.mesh.Pose;
import com.kirbyjad.mesh.Rig;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Arrays;
import javax.imageio.ImageIO;

// renders every anim to a png so i don't have to log in to check the models.
// backfaces are culled like the game so bad winding shows up as holes
public class PreviewRenderer
{
	private static final int CELL = 260;
	private static final int FRAMES = 7;

	public static void main(String[] args) throws Exception
	{
		File out = new File(args.length > 0 ? args[0] : "build/preview");
		out.mkdirs();
		float yaw = args.length > 1 ? Float.parseFloat(args[1]) : 30;
		sheet(new Kirby(), 760, yaw, new File(out, "kirby.png"));
		sheet(new WaddleDee(), 200, yaw, new File(out, "waddledee.png"));
		hero(new Kirby(), new File(out, "kirby_hero.png"));
		props(new File(out, "props.png"));
		sheet(new BrontoBurt(), 260, yaw, new File(out, "bronto.png"));
		sheet(new WhispyWoods(260), 3200, yaw, new File(out, "whispy.png"));
		sheet(new Butterfly(0xff9fd0), 60, yaw, new File(out, "butterfly.png"));
		sheet(new Scarfy(100), 420, yaw, new File(out, "scarfy.png"));
		sheet(new Gordo(), 700, yaw, new File(out, "gordo.png"));
		sheet(new WaddleDoo(), 520, yaw, new File(out, "waddledoo.png"));
		sheet(new MetaKnight(), 900, yaw, new File(out, "metaknight.png"));
		tells(new Kirby(), new File(out, "kirby_tells.png"));
		System.out.println("wrote " + out.getAbsolutePath());
	}

	private static void sheet(Toon toon, float worldSize, float yawDeg, File file) throws Exception
	{
		Anim[] anims = toon.showcase();
		int labelW = 90;
		BufferedImage img = new BufferedImage(labelW + CELL * FRAMES, CELL * anims.length, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = img.createGraphics();
		g.setColor(new Color(0x1a1010));
		g.fillRect(0, 0, img.getWidth(), img.getHeight());
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g.setFont(new Font("SansSerif", Font.BOLD, 14));
		Pose pose = toon.newPose();
		for (int row = 0; row < anims.length; row++)
		{
			Anim a = anims[row];
			g.setColor(Color.WHITE);
			g.drawString(a.name(), 8, row * CELL + CELL / 2);
			float dur = a.loops() ? 2.4f : toon.duration(a);
			for (int f = 0; f < FRAMES; f++)
			{
				float t = dur * f / (FRAMES - 1);
				toon.pose(a, t, a.loops() ? t : 0.5f, pose);
				BufferedImage cell = render(toon, pose, CELL, CELL, worldSize, yawDeg, 18);
				g.drawImage(cell, labelW + f * CELL, row * CELL, null);
				g.setColor(Color.GRAY);
				g.drawString(String.format("%.2fs", t), labelW + f * CELL + 6, row * CELL + 16);
			}
		}
		g.dispose();
		ImageIO.write(img, "png", file);
	}

	private static void hero(Toon toon, File file) throws Exception
	{
		Pose pose = toon.newPose();
		BufferedImage img = new BufferedImage(1200, 600, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = img.createGraphics();
		toon.pose(Anim.IDLE, 0, 0.5f, pose);
		g.drawImage(render(toon, pose, 600, 600, 700, 20, 12), 0, 0, null);
		toon.pose(Anim.MAGIC, 1.25f, 0.5f, pose);
		g.drawImage(render(toon, pose, 600, 600, 900, -25, 12), 600, 0, null);
		g.dispose();
		ImageIO.write(img, "png", file);
	}

	private static void props(File file) throws Exception
	{
		Prop[] props = {Prop.hill(300, 1), Prop.mesa(260, 480, 4), Prop.tree(160, 3), Prop.flower(0xff8fc8), Prop.cloud(5), Prop.star(90),
			Prop.rainbow(2600, 130), Prop.cloud(21, 0xffc9b0, 3)};
		float[] sizes = {900, 1000, 700, 110, 1100, 300, 6000, 3200};
		BufferedImage img = new BufferedImage(CELL * props.length, CELL, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = img.createGraphics();
		for (int i = 0; i < props.length; i++)
		{
			g.drawImage(render(props[i].mesh, props[i].rig, props[i].newPose(), CELL, CELL, sizes[i], 25, 20), i * CELL, 0, null);
		}
		g.dispose();
		ImageIO.write(img, "png", file);
	}

	// first half second of mage vs range
	private static void tells(Toon toon, File file) throws Exception
	{
		Pose pose = toon.newPose();
		float[] times = {0, 0.1f, 0.2f, 0.3f, 0.4f, 0.5f};
		BufferedImage img = new BufferedImage(90 + CELL * times.length, CELL * 2, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = img.createGraphics();
		g.setFont(new Font("SansSerif", Font.BOLD, 14));
		Anim[] rows = {Anim.MAGIC, Anim.RANGED};
		for (int r = 0; r < 2; r++)
		{
			g.setColor(Color.WHITE);
			g.drawString(rows[r].name(), 8, r * CELL + CELL / 2);
			for (int i = 0; i < times.length; i++)
			{
				toon.pose(rows[r], times[i], 0.5f, pose);
				g.drawImage(render(toon, pose, CELL, CELL, 900, 0, 18), 90 + i * CELL, r * CELL, null);
				g.setColor(Color.GRAY);
				g.drawString(String.format("%.1fs", times[i]), 96 + i * CELL, r * CELL + 16);
			}
		}
		g.dispose();
		ImageIO.write(img, "png", file);
	}

	static BufferedImage render(Toon toon, Pose pose, int w, int h, float worldSize, float yawDeg, float pitchDeg)
	{
		return render(toon.mesh, toon.rig, pose, w, h, worldSize, yawDeg, pitchDeg);
	}

	static BufferedImage render(Mesh mesh, Rig rig, Pose pose, int w, int h, float worldSize, float yawDeg, float pitchDeg)
	{
		float lx = -0.5f, ly = 0.75f, lz = 0.45f;
		float ll = (float) Math.sqrt(lx * lx + ly * ly + lz * lz);
		rig.apply(mesh, pose, lx / ll, ly / ll, lz / ll);

		double yaw = Math.toRadians(yawDeg), pitch = Math.toRadians(pitchDeg);
		float cy = (float) Math.cos(yaw), sy = (float) Math.sin(yaw), cp = (float) Math.cos(pitch), sp = (float) Math.sin(pitch);
		float scale = w / worldSize;
		int n = mesh.vertexCount;
		float[] sxv = new float[n], syv = new float[n], szv = new float[n];
		float yMid = worldSize * 0.42f;
		for (int i = 0; i < n; i++)
		{
			float x = rig.outX[i], y = rig.outY[i] - yMid, z = rig.outZ[i];
			float x1 = x * cy + z * sy, z1 = -x * sy + z * cy;
			float y2 = y * cp - z1 * sp, z2 = y * sp + z1 * cp;
			sxv[i] = w / 2f + x1 * scale;
			syv[i] = h / 2f - y2 * scale;
			szv[i] = z2;
		}
		BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
		int[] px = new int[w * h];
		Arrays.fill(px, 0x2a1414);
		for (int yy = h * 3 / 4; yy < h; yy++)
		{
			for (int xx = 0; xx < w; xx++)
			{
				px[yy * w + xx] = 0x140a0a;
			}
		}
		float[] zb = new float[w * h];
		Arrays.fill(zb, -Float.MAX_VALUE);
		for (int f = 0; f < mesh.faceCount; f++)
		{
			int rgb = rig.outRgb[f];
			if (rgb < 0)
			{
				continue;
			}
			int a = mesh.a[f], b = mesh.b[f], c = mesh.c[f];
			float ax = sxv[a], ay = syv[a], bx = sxv[b], by = syv[b], cx = sxv[c], cyy = syv[c];
			float area = (bx - ax) * (cyy - ay) - (by - ay) * (cx - ax);
			// backface
			if (area >= 0)
			{
				continue;
			}
			raster(px, zb, w, h, ax, ay, szv[a], bx, by, szv[b], cx, cyy, szv[c], rgb,
				rig.outI1[f], rig.outI2[f], rig.outI3[f], area);
		}
		img.setRGB(0, 0, w, h, px, 0, w);
		return img;
	}

	private static void raster(int[] px, float[] zb, int w, int h,
		float ax, float ay, float az, float bx, float by, float bz, float cx, float cy, float cz,
		int rgb, float ia, float ib, float ic, float area)
	{
		int minX = Math.max(0, (int) Math.floor(Math.min(ax, Math.min(bx, cx))));
		int maxX = Math.min(w - 1, (int) Math.ceil(Math.max(ax, Math.max(bx, cx))));
		int minY = Math.max(0, (int) Math.floor(Math.min(ay, Math.min(by, cy))));
		int maxY = Math.min(h - 1, (int) Math.ceil(Math.max(ay, Math.max(by, cy))));
		int r = rgb >> 16 & 255, g = rgb >> 8 & 255, b = rgb & 255;
		for (int y = minY; y <= maxY; y++)
		{
			for (int x = minX; x <= maxX; x++)
			{
				float pxf = x + 0.5f, pyf = y + 0.5f;
				float w0 = ((bx - pxf) * (cy - pyf) - (by - pyf) * (cx - pxf)) / area;
				float w1 = ((cx - pxf) * (ay - pyf) - (cy - pyf) * (ax - pxf)) / area;
				float w2 = 1 - w0 - w1;
				if (w0 < 0 || w1 < 0 || w2 < 0)
				{
					continue;
				}
				float z = w0 * az + w1 * bz + w2 * cz;
				int i = y * w + x;
				if (z <= zb[i])
				{
					continue;
				}
				zb[i] = z;
				float in = w0 * ia + w1 * ib + w2 * ic;
				px[i] = clamp(r * in) << 16 | clamp(g * in) << 8 | clamp(b * in);
			}
		}
	}

	private static int clamp(float v)
	{
		return v < 0 ? 0 : v > 255 ? 255 : (int) v;
	}
}
