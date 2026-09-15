package api.hbm.wgc;

import com.hbm.blocks.ModBlocks;
import com.hbm.dim.CelestialBody;
import com.hbm.dim.orbit.OrbitalStation;
import com.hbm.tileentity.machine.TileEntityOrbitalStationComputer;

import cpw.mods.fml.common.eventhandler.Event.Result;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.Action;

/**
 * Cancellation-safe bridge from the physical HBM Station Computer to WGCore's
 * server-authoritative Breach hack objective.
 */
public final class BreachStationComputerInteractionHandler {

    static final BreachStationComputerInteractionHandler INSTANCE = new BreachStationComputerInteractionHandler();

    private BreachStationComputerInteractionHandler() { }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public void onStationComputerInteract(PlayerInteractEvent event) {
        if (event == null || event.entityPlayer == null || event.world == null || event.world.isRemote) return;
        if (event.action != Action.RIGHT_CLICK_BLOCK) return;
        if (!CelestialBody.inOrbit(event.world)) return;
        if (event.world.getBlock(event.x, event.y, event.z) != ModBlocks.orbital_station_computer) return;

        OrbitalStation station = OrbitalStation.getStationFromPosition(event.x, event.z);
        if (station == null || station.stationKey == null || station.stationKey.isEmpty()) return;

        boolean started = Integrations.beginBreachHackWGC(
            event.world,
            event.entityPlayer.getUniqueID(),
            station.stationKey,
            station.generation,
            event.x,
            event.y,
            event.z
        );
        if (!started) return;

        long remaining = Integrations.getBreachHackRemainingMillisWGC(
            event.world,
            station.stationKey,
            station.generation
        );

        TileEntity tile = event.world.getTileEntity(event.x, event.y, event.z);
        if (tile instanceof TileEntityOrbitalStationComputer) {
            ((TileEntityOrbitalStationComputer) tile).beginBreachHackDisplay(remaining);
        }

        event.setCanceled(true);
        event.useBlock = Result.DENY;
        event.useItem = Result.DENY;
    }
}
