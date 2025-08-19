package api.hbm.explosion.event;

import cpw.mods.fml.common.eventhandler.Event;
import net.minecraft.world.World;
import net.minecraft.world.ChunkPosition;

import java.util.List;

/**
 * Fired on SERVER after an engine has built a mutable list of target blocks
 * but before it applies them. List is mutable: listeners may remove entries.
 */
public class HbmExplosionFilterEvent extends Event {
	public final World world;
	public final double x, y, z;
	public final float size;
	public final String engine;               // typically "VNT"
	public final List<ChunkPosition> blocks;  // MUTABLE

	public HbmExplosionFilterEvent(World world, double x, double y, double z,
								   float size, String engine, List<ChunkPosition> blocks) {
		this.world = world;
		this.x = x; this.y = y; this.z = z;
		this.size = size;
		this.engine = engine;
		this.blocks = blocks;
	}
}
