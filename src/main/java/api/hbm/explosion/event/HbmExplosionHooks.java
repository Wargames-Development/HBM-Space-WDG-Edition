package api.hbm.explosion.event;

import net.minecraftforge.common.MinecraftForge; // <-- correct import on 1.7.10
import net.minecraft.world.World;
import net.minecraft.world.ChunkPosition;

import java.util.List;

/** Internal helper so HBM engines can post the public API events with one-liners. */
public final class HbmExplosionHooks {
	private HbmExplosionHooks() {}

	/** @return true if canceled (veto the explosion) */
	public static boolean pre(World w, double x, double y, double z,
							  float size, Object src, String engine) {
		return !w.isRemote && MinecraftForge.EVENT_BUS.post(
			new HbmExplosionPreEvent(w, x, y, z, size, src, engine)
		);
	}

	/** Expose a mutable block list for pruning (server only). */
	public static void filter(World w, double x, double y, double z,
							  float size, String engine, List<ChunkPosition> blocks) {
		if (!w.isRemote) {
			MinecraftForge.EVENT_BUS.post(
				new HbmExplosionFilterEvent(w, x, y, z, size, engine, blocks)
			);
		}
	}

	/** @return true if this particular block edit should be denied */
	public static boolean blockDenied(World w, int x, int y, int z, String engine) {
		return !w.isRemote && MinecraftForge.EVENT_BUS.post(
			new HbmExplosionBlockCheckEvent(w, x, y, z, engine)
		);
	}
}
