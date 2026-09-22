package com.hbm.tileentity.machine;

import java.util.UUID;

import api.hbm.energymk2.IEnergyReceiverMK2;
import api.hbm.wgc.Integrations;

import com.hbm.dim.CelestialBody;
import com.hbm.dim.SolarSystemWorldSavedData;
import com.hbm.dim.orbit.OrbitalStation;
import com.hbm.interfaces.IControlReceiver;
import com.hbm.inventory.container.ContainerStationDriveTerminal;
import com.hbm.inventory.gui.GUIStationDriveTerminal;
import com.hbm.items.ItemRaidDrive;
import com.hbm.items.ItemVOTVdrive;
import com.hbm.items.ModItems;
import com.hbm.lib.Library;
import com.hbm.tileentity.IGUIProvider;
import com.hbm.tileentity.TileEntityMachineBase;
import com.hbm.util.BufferUtil;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

/**
 * Physical replacement for player-facing /ntm station create/raid programming.
 * HBM owns drives/station reservations; WGCore optionally supplies faction and Breach authority.
 */
public class TileEntityMachineStationDriveTerminal extends TileEntityMachineBase implements IGUIProvider, IControlReceiver, IEnergyReceiverMK2 {

    public static final int MODE_NONE = 0;
    public static final int MODE_ORBIT = 1;
    public static final int MODE_BREACH = 2;

    public long power;
    public long maxPower = 2_000L;
    public String status = EnumChatFormatting.GREEN + "READY";
    public int selectedMode = MODE_NONE;
    private int statusTimer;

    public TileEntityMachineStationDriveTerminal() {
        super(3);
    }

    @Override
    public void updateEntity() {
        if(worldObj.isRemote) return;
        power = Library.chargeTEFromItems(slots, 2, power, maxPower);
        for(ForgeDirection dir : ForgeDirection.VALID_DIRECTIONS) {
            trySubscribe(worldObj, xCoord + dir.offsetX, yCoord + dir.offsetY, zCoord + dir.offsetZ, dir);
        }
        if(statusTimer > 0) statusTimer--;
        if(statusTimer <= 0) {
            if(power < requiredPower()) status = EnumChatFormatting.RED + "NO POWER";
            else status = EnumChatFormatting.GREEN + "READY";
        }
        if(ItemVOTVdrive.isNormalStationDrive(slots[0])) ItemVOTVdrive.validateNormalStationDrive(slots[0], worldObj);
        if(ItemVOTVdrive.isNormalStationDrive(slots[1])) ItemVOTVdrive.validateNormalStationDrive(slots[1], worldObj);
        networkPackNT(15);
    }

    private long requiredPower() { return (long)(maxPower * 0.75D); }

    private boolean requirePower() {
        if(power >= requiredPower()) return true;
        setStatus(EnumChatFormatting.RED + "NO POWER", 80);
        return false;
    }

    private void consumeProgrammingPower() {
        power = Math.max(0L, power - 500L);
    }

    private void cloneDrive(EntityPlayer player) {
        if(!requirePower()) return;
        if(slots[0] == null) {
            setStatus(EnumChatFormatting.RED + "CLN: SOURCE REQUIRED", 100);
            return;
        }

        if(ItemRaidDrive.isProgrammed(slots[0])) {
            if(slots[1] == null || !ItemRaidDrive.isReusableProgrammingTarget(slots[1], worldObj)) {
                setStatus(EnumChatFormatting.RED + "CLN: EMPTY TARGET REQUIRED", 120);
                return;
            }
            if(player == null || !ItemVOTVdrive.canPlayerUseStationDriveForLaunch(
                    slots[0], worldObj, player.getUniqueID())) {
                setStatus(EnumChatFormatting.RED + "CLN: ACCESS DENIED", 120);
                return;
            }
            ItemStack copy = slots[0].copy();
            copy.stackSize = 1;
            slots[1] = copy;
            consumeProgrammingPower();
            setStatus(EnumChatFormatting.GREEN + "CLN: DRIVE COPIED", 100);
            markDirty();
            return;
        }

        if(slots[0].getItem() != ModItems.full_drive) {
            setStatus(EnumChatFormatting.RED + "CLN: SOURCE INVALID", 100);
            return;
        }
        if(slots[1] == null || slots[1].getItem() != ModItems.hard_drive) {
            setStatus(EnumChatFormatting.RED + "CLN: EMPTY TARGET REQUIRED", 100);
            return;
        }
        ItemVOTVdrive.markCopied(slots[0]);
        slots[1] = slots[0].copy();
        slots[1].stackSize = 1;
        consumeProgrammingPower();
        setStatus(EnumChatFormatting.GREEN + "CLN: DRIVE COPIED", 100);
        markDirty();
    }

    private void selectMode(int mode) {
        if(!requirePower()) return;
        if(mode != MODE_ORBIT && mode != MODE_BREACH) mode = MODE_NONE;
        selectedMode = selectedMode == mode ? MODE_NONE : mode;
        if(selectedMode == MODE_ORBIT) {
            setStatus(EnumChatFormatting.YELLOW + "ORB SELECTED", 80);
        } else if(selectedMode == MODE_BREACH) {
            setStatus(EnumChatFormatting.YELLOW + "RAD SELECTED", 80);
        } else {
            setStatus(EnumChatFormatting.YELLOW + "SELECT ORB OR RAD", 80);
        }
    }

    private void startSelected(EntityPlayer player) {
        if(!requirePower()) return;
        if(selectedMode == MODE_ORBIT) {
            programOrbitalDrive(player);
        } else if(selectedMode == MODE_BREACH) {
            programBreachDrive(player);
        } else {
            setStatus(EnumChatFormatting.YELLOW + "SELECT ORB OR RAD", 100);
        }
    }

    private void programOrbitalDrive(EntityPlayer player) {
        if(!requirePower()) return;
        if(player == null) return;
        if(slots[0] == null) {
            setStatus(EnumChatFormatting.RED + "HARD DRIVE REQUIRED", 120);
            return;
        }
        if(slots[0].getItem() != ModItems.hard_drive) {
            setStatus(EnumChatFormatting.RED + (slots[0].getItem() == ModItems.full_drive
                ? "EMPTY DRIVE REQUIRED" : "HARD DRIVE REQUIRED"), 120);
            return;
        }

        UUID ownerId;
        boolean factionOwner;
        String stationName;
        if(Integrations.isWGCoreActive()) {
            ownerId = Integrations.getPlayerFaction(worldObj, player.getUniqueID());
            if(ownerId == null) {
                setStatus(EnumChatFormatting.RED + "NO FACTION", 140);
                return;
            }
            stationName = Integrations.getFactionNameWGC(worldObj, ownerId);
            if(stationName == null || stationName.trim().isEmpty()) {
                setStatus(EnumChatFormatting.RED + "FACTION ERROR", 140);
                return;
            }
            factionOwner = true;
        } else {
            ownerId = player.getUniqueID();
            stationName = player.getCommandSenderName() + " Station";
            factionOwner = false;
        }

        SolarSystemWorldSavedData data = SolarSystemWorldSavedData.get(worldObj);
        CelestialBody body = CelestialBody.getBody(worldObj);
        if(body == null) body = CelestialBody.getBody(0);
        OrbitalStation existing = data.findStationByDriveOwner(worldObj, ownerId, factionOwner);
        boolean reissue = existing != null;
        OrbitalStation station = existing != null ? existing : data.getOrCreateDriveOwnerStation(worldObj, body, stationName, ownerId, factionOwner);
        if(station == null) {
            setStatus(EnumChatFormatting.RED + "NO SAFE CELL", 140);
            return;
        }
        if(station.deleting) {
            setStatus(EnumChatFormatting.RED + "CLEANUP ACTIVE", 140);
            return;
        }

        ItemStack programmed = ItemVOTVdrive.createNormalStationDrive(station);
        if(programmed == null) {
            setStatus(EnumChatFormatting.RED + "PROGRAM FAILED", 140);
            return;
        }
        slots[0] = programmed;
        consumeProgrammingPower();
        setStatus(EnumChatFormatting.GREEN + (reissue ? "REISSUED " : "CREATED ") + stationName, 160);
        markDirty();
    }

    private void programBreachDrive(EntityPlayer player) {
        if(!requirePower()) return;
        if(player == null) return;
        if(slots[0] == null || slots[0].getItem() != ModItems.raid_drive) {
            setStatus(EnumChatFormatting.RED + "BREACH DRIVE REQUIRED", 120);
            return;
        }
        ItemStack programmingMedium = ItemRaidDrive.blankProgrammingCopy(slots[0], worldObj);
        if(programmingMedium == null) {
            setStatus(EnumChatFormatting.RED + "EMPTY DRIVE REQUIRED", 120);
            return;
        }
        SolarSystemWorldSavedData data = SolarSystemWorldSavedData.get(worldObj);
        OrbitalStation target = null;

        if(Integrations.isWGCoreActive()) {
            UUID factionId = Integrations.getPlayerFaction(worldObj, player.getUniqueID());
            if(factionId == null) {
                setStatus(EnumChatFormatting.RED + "NO FACTION", 140);
                return;
            }
            String key = Integrations.getBreachDriveTargetStationKeyWGC(worldObj, player.getUniqueID());
            int generation = Integrations.getBreachDriveTargetStationGenerationWGC(worldObj, player.getUniqueID());
            if(key == null || key.trim().isEmpty() || generation < 0) {
                setStatus(EnumChatFormatting.RED + "NO BREACH AVAILABLE", 140);
                return;
            }
            target = data.getStationByIdentity(key, generation, true);
            if(target == null) {
                setStatus(EnumChatFormatting.RED + "TARGET MISSING", 140);
                return;
            }
        } else {
            if(!ItemVOTVdrive.isNormalStationDrive(slots[1]) || !ItemVOTVdrive.validateNormalStationDrive(slots[1], worldObj)) {
                setStatus(EnumChatFormatting.RED + "TARGET DRIVE REQUIRED", 140);
                return;
            }
            ItemVOTVdrive.Destination destination = ItemVOTVdrive.getDestinationUnchecked(slots[1]);
            target = destination != null ? data.getStationAtGrid(destination.x, destination.z) : null;
            if(target == null || !target.hasStation || target.deleting) {
                setStatus(EnumChatFormatting.RED + "TARGET INVALID", 140);
                return;
            }
        }

        UUID ownerFactionId = Integrations.isWGCoreActive()
            ? Integrations.getPlayerFaction(worldObj, player.getUniqueID()) : null;
        ItemStack programmed = data.programRaidDrive(programmingMedium, target, ownerFactionId, Integrations.isWGCoreActive());
        if(programmed == null) {
            setStatus(EnumChatFormatting.RED + "PROGRAM FAILED", 140);
            return;
        }
        slots[0] = programmed;
        consumeProgrammingPower();
        setStatus(EnumChatFormatting.GREEN + "BREACH READY\n" + safeName(target), 160);
        markDirty();
    }

    private static String safeName(OrbitalStation station) {
        return station != null && station.name != null && !station.name.trim().isEmpty() ? station.name.trim() : "TARGET";
    }

    private void setStatus(String message, int ticks) {
        status = message == null ? "" : message;
        statusTimer = Math.max(0, ticks);
    }

    @Override
    public void receiveControl(EntityPlayer player, NBTTagCompound data) {
        if(data == null) return;
        if(data.getBoolean("clone")) { cloneDrive(player); return; }
        if(data.getBoolean("start")) { startSelected(player); return; }
        if(data.getBoolean("orbit")) { selectMode(MODE_ORBIT); return; }
        if(data.getBoolean("raid")) selectMode(MODE_BREACH);
    }

    @Override
    public void receiveControl(NBTTagCompound data) {
        // Intentionally empty: the player-aware overload above is authoritative for faction checks.
    }

    @Override
    public boolean hasPermission(EntityPlayer player) { return isUseableByPlayer(player); }

    @Override
    public void serialize(ByteBuf buf) {
        super.serialize(buf);
        buf.writeLong(power);
        BufferUtil.writeString(buf, status);
        buf.writeByte(selectedMode);
    }

    @Override
    public void deserialize(ByteBuf buf) {
        super.deserialize(buf);
        power = buf.readLong();
        status = BufferUtil.readString(buf);
        selectedMode = buf.readUnsignedByte();
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        nbt.setLong("power", power);
        nbt.setString("status", status == null ? "" : status);
        nbt.setInteger("statusTimer", statusTimer);
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        power = nbt.getLong("power");
        status = nbt.getString("status");
        statusTimer = Math.max(0, nbt.getInteger("statusTimer"));
    }

    @Override
    public Container provideContainer(int ID, EntityPlayer player, World world, int x, int y, int z) {
        return new ContainerStationDriveTerminal(player.inventory, this);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public Object provideGUI(int ID, EntityPlayer player, World world, int x, int y, int z) {
        return new GUIStationDriveTerminal(player.inventory, this);
    }

    @Override
    public String getName() { return "container.stationDriveTerminal"; }

    @Override
    public int getInventoryStackLimit() { return 1; }

    @Override public long getPower() { return power; }
    @Override public void setPower(long power) { this.power = Math.max(0L, Math.min(maxPower, power)); }
    @Override public long getMaxPower() { return maxPower; }

    private AxisAlignedBB renderBox;
    @Override
    public AxisAlignedBB getRenderBoundingBox() {
        if(renderBox == null) renderBox = AxisAlignedBB.getBoundingBox(xCoord - 1, yCoord, zCoord - 1, xCoord + 2, yCoord + 2, zCoord + 2);
        return renderBox;
    }
}
