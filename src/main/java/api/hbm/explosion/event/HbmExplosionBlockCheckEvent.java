package api.hbm.explosion.event;

import cpw.mods.fml.common.eventhandler.Cancelable;
import cpw.mods.fml.common.eventhandler.Event;
import net.minecraft.world.World;

/** Cancel this to deny a single block edit at (x,y,z) for a given engine label. */
@Cancelable
public class HbmExplosionBlockCheckEvent extends Event {
	public final World world;
	public final int x, y, z;
	public final String engine;

	public HbmExplosionBlockCheckEvent(World world, int x, int y, int z, String engine) {
		this.world  = world;
		this.x = x; this.y = y; this.z = z;
		this.engine = engine;
	}
}
