package com.kirbyjad;

import java.io.File;
import java.util.Arrays;
import net.runelite.cache.ConfigType;
import net.runelite.cache.IndexType;
import net.runelite.cache.NpcManager;
import net.runelite.cache.definitions.ModelDefinition;
import net.runelite.cache.definitions.NpcDefinition;
import net.runelite.cache.definitions.SequenceDefinition;
import net.runelite.cache.definitions.loaders.ModelLoader;
import net.runelite.cache.definitions.loaders.SequenceLoader;
import net.runelite.cache.fs.Archive;
import net.runelite.cache.fs.FSFile;
import net.runelite.cache.fs.Index;
import net.runelite.cache.fs.Store;

// dumps jad/healer model sizes + anim lengths from the cache
public class CacheProbe
{
	public static void main(String[] args) throws Exception
	{
		File dir = new File(System.getProperty("user.home"), ".runelite/jagexcache/oldschool/LIVE");
		try (Store store = new Store(dir))
		{
			store.load();
			NpcManager npcs = new NpcManager(store);
			npcs.load();

			Index models = store.getIndex(IndexType.MODELS);
			for (int id : new int[]{3116, 3118, 3120, 3121, 3123, 3125, 3127, 3128})
			{
				NpcDefinition def = npcs.get(id);
				System.out.printf("npc %d %s size=%d scale=%d/%d stand=%d walk=%d models=%s%n", id, def.name, def.size,
					def.widthScale, def.heightScale, def.standingAnimation, def.walkingAnimation, Arrays.toString(def.models));
				for (int modelId : def.models)
				{
					Archive archive = models.getArchive(modelId);
					byte[] data = archive.decompress(store.getStorage().loadArchive(archive));
					probe(new ModelLoader().load(modelId, data));
				}
			}

			Archive seqArchive = store.getIndex(IndexType.CONFIGS).getArchive(ConfigType.SEQUENCE.getId());
			SequenceLoader loader = new SequenceLoader().configureForRevision(seqArchive.getRevision());
			for (FSFile file : seqArchive.getFiles(store.getStorage().loadArchive(seqArchive)).getFiles())
			{
				int id = file.getFileId();
				if (id >= 2618 && id <= 2656)
				{
					SequenceDefinition seq = loader.load(id, file.getContents());
					int total = 0;
					if (seq.frameLengths != null)
					{
						for (int length : seq.frameLengths)
						{
							total += length;
						}
					}
					System.out.printf("seq %d frames=%d cycles=%d (%.2fs) lengths=%s%n", id,
						seq.frameIDs == null ? 0 : seq.frameIDs.length, total, total * 0.02, Arrays.toString(seq.frameLengths));
				}
			}
		}
	}

	private static void probe(ModelDefinition m)
	{
		int minX = Integer.MAX_VALUE;
		int maxX = Integer.MIN_VALUE;
		int minY = Integer.MAX_VALUE;
		int maxY = Integer.MIN_VALUE;
		int minZ = Integer.MAX_VALUE;
		int maxZ = Integer.MIN_VALUE;
		double cx = 0;
		double cy = 0;
		double cz = 0;
		for (int i = 0; i < m.vertexCount; i++)
		{
			minX = Math.min(minX, m.vertexX[i]);
			maxX = Math.max(maxX, m.vertexX[i]);
			minY = Math.min(minY, m.vertexY[i]);
			maxY = Math.max(maxY, m.vertexY[i]);
			minZ = Math.min(minZ, m.vertexZ[i]);
			maxZ = Math.max(maxZ, m.vertexZ[i]);
			cx += m.vertexX[i];
			cy += m.vertexY[i];
			cz += m.vertexZ[i];
		}
		cx /= m.vertexCount;
		cy /= m.vertexCount;
		cz /= m.vertexCount;

		// which way the faces wind compared to pointing away from the middle
		int outward = 0;
		for (int f = 0; f < m.faceCount; f++)
		{
			int a = m.faceIndices1[f];
			int b = m.faceIndices2[f];
			int c = m.faceIndices3[f];
			double ux = m.vertexX[b] - m.vertexX[a];
			double uy = m.vertexY[b] - m.vertexY[a];
			double uz = m.vertexZ[b] - m.vertexZ[a];
			double vx = m.vertexX[c] - m.vertexX[a];
			double vy = m.vertexY[c] - m.vertexY[a];
			double vz = m.vertexZ[c] - m.vertexZ[a];
			double nx = uy * vz - uz * vy;
			double ny = uz * vx - ux * vz;
			double nz = ux * vy - uy * vx;
			double px = (m.vertexX[a] + m.vertexX[b] + m.vertexX[c]) / 3.0 - cx;
			double py = (m.vertexY[a] + m.vertexY[b] + m.vertexY[c]) / 3.0 - cy;
			double pz = (m.vertexZ[a] + m.vertexZ[b] + m.vertexZ[c]) / 3.0 - cz;
			if (nx * px + ny * py + nz * pz > 0)
			{
				outward++;
			}
		}

		System.out.printf("  model %d verts=%d faces=%d x[%d..%d] y[%d..%d] z[%d..%d] outward=%d/%d%n",
			m.id, m.vertexCount, m.faceCount, minX, maxX, minY, maxY, minZ, maxZ, outward, m.faceCount);
	}
}
