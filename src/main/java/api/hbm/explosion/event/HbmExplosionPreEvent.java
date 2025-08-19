package api.hbm.explosion.event;

import cpw.mods.fml.common.eventhandler.Cancelable;
import cpw.mods.fml.common.eventhandler.Event;
import net.minecraft.world.World;

/**
 * Fired on SERVER before an HBM explosion engine applies ANY block edits.
 * Cancel to veto the blast entirely.
 */
@Cancelable
public class HbmExplosionPreEvent extends Event {
	public final World world;
	public final double x, y, z;   // explosion center (block/world coords)
	public final float size;       // radius/power upper bound (engine-specific)
	public final Object source;    // engine/explosion instance, or null for statics
	public final String engine;    // e.g. "NT","VNT","LARGE","NUKE.ADV","NUKE.RAY.PAR",...

	public HbmExplosionPreEvent(World world, double x, double y, double z,
								float size, Object source, String engine) {
		this.world = world;
		this.x = x; this.y = y; this.z = z;
		this.size = size;
		this.source = source;
		this.engine = engine;
	}
}
