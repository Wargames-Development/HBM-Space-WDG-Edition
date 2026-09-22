package api.hbm.wgc;

import com.hbm.blocks.ModBlocks;
import com.hbm.dim.CelestialBody;
import com.hbm.dim.orbit.OrbitalStation;
import com.hbm.tileentity.machine.TileEntityOrbitalStationComputer;

import cpw.mods.fml.common.eventhandler.Event.Result;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraft.world.ChunkPosition;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.Action;
import net.minecraftforge.event.world.BlockEvent.BreakEvent;
import net.minecraftforge.event.world.ExplosionEvent;

import java.util.Iterator;
import java.util.List;

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
        if (!isTrackedStationComputer(station, event.x, event.y, event.z)) return;

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

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onStationComputerBreak(BreakEvent event) {
        if (event == null || event.getPlayer() == null || event.world == null || event.world.isRemote) return;
        if (!CelestialBody.inOrbit(event.world)) return;
        if (!isLockedTrackedStationComputer(event.world, event.x, event.y, event.z)) return;

        event.setCanceled(true);
        sendWarMessage(event.getPlayer(),
            "The registered Orbital Station Computer is locked while this station is engaged in a Breach.");
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onExplosionDetonate(ExplosionEvent.Detonate event) {
        if (event == null || event.world == null || event.world.isRemote || !CelestialBody.inOrbit(event.world)) return;

        List<ChunkPosition> affectedBlocks = event.getAffectedBlocks();
        if (affectedBlocks == null || affectedBlocks.isEmpty()) return;

        Iterator<ChunkPosition> iterator = affectedBlocks.iterator();
        while (iterator.hasNext()) {
            ChunkPosition position = iterator.next();
            if (position != null && isLockedTrackedStationComputer(
                    event.world, position.chunkPosX, position.chunkPosY, position.chunkPosZ)) {
                iterator.remove();
            }
        }
    }

    private void sendWarMessage(net.minecraft.entity.player.EntityPlayer player, String message) {
        if (player == null || message == null || message.isEmpty()) return;
        ChatComponentText prefix = new ChatComponentText("[WG War] ");
        prefix.setChatStyle(new ChatStyle().setColor(EnumChatFormatting.RED));
        ChatComponentText body = new ChatComponentText(message);
        body.setChatStyle(new ChatStyle().setColor(EnumChatFormatting.GRAY));
        prefix.appendSibling(body);
        player.addChatMessage(prefix);
    }

    static boolean isLockedTrackedStationComputer(net.minecraft.world.World world, int x, int y, int z) {
        if (world == null
            || world.isRemote
            || !CelestialBody.inOrbit(world)
            || world.getBlock(x, y, z) != ModBlocks.orbital_station_computer) {
            return false;
        }

        OrbitalStation station = OrbitalStation.getStationFromPosition(x, z);
        return isTrackedStationComputer(station, x, y, z)
            && Integrations.isBreachStationComputerLockedWGC(
                world, station.stationKey, station.generation);
    }

    private static boolean isTrackedStationComputer(OrbitalStation station, int x, int y, int z) {
        return station != null
            && station.hasStation
            && !station.deleting
            && station.hasComputer
            && station.stationKey != null
            && !station.stationKey.isEmpty()
            && station.computerX == x
            && station.computerY == y
            && station.computerZ == z;
    }

}
