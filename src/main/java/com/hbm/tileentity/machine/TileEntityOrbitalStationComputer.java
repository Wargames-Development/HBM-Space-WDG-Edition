package com.hbm.tileentity.machine;

import api.hbm.wgc.Integrations;
import com.hbm.dim.CelestialBody;
import com.hbm.dim.SolarSystemWorldSavedData;
import com.hbm.dim.orbit.OrbitalStation;
import com.hbm.dim.orbit.OrbitalStation.StationState;
import com.hbm.interfaces.IControlReceiver;
import com.hbm.inventory.container.ContainerOrbitalStationComputer;
import com.hbm.inventory.gui.GUIOrbitalStationComputer;
import com.hbm.tileentity.IGUIProvider;
import com.hbm.tileentity.TileEntityMachineBase;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;

public class TileEntityOrbitalStationComputer extends TileEntityMachineBase implements IGUIProvider, IControlReceiver {

	public boolean hasDrive;
	public boolean breachHackDisplayActive;
	public int breachHackDisplayRadius = 12;
	public int breachHackRemainingSeconds = -1;
	private long breachHackInitialRemainingMillis = -1L;
	private boolean breachHackProgressObserved;

	public TileEntityOrbitalStationComputer() {
		super(1);
	}

	public boolean travelTo(CelestialBody body, ItemStack drive) {
		OrbitalStation station = OrbitalStation.getStationFromPosition(xCoord, zCoord);

		if(station.orbiting == body) return false;

		station.travelTo(worldObj, body);
		slots[0] = drive;
		markChanged();

		return true;
	}

	public boolean isTravelling() {
		OrbitalStation station = OrbitalStation.getStationFromPosition(xCoord, zCoord);

		return station.state != StationState.ORBIT;
	}

	@Override
	public String getName() {
		return "container.orbitalStationComputer";
	}

	@Override
	public void updateEntity() {
		if(!worldObj.isRemote) {
			hasDrive = slots[0] != null;
			if(worldObj.getTotalWorldTime() % 100L == 0L) {
				SolarSystemWorldSavedData data = SolarSystemWorldSavedData.get(worldObj);
				if(data != null) data.registerComputerDiscovered(worldObj, xCoord, yCoord, zCoord);
			}
			if(breachHackDisplayActive && worldObj.getTotalWorldTime() % 10L == 0L) {
				refreshBreachHackDisplay();
			}
			networkPackNT(50);
		} else if(breachHackDisplayActive && worldObj.getTotalWorldTime() % 3L == 0L) {
			spawnBreachHackBoundary();
		}
	}

	public void beginBreachHackDisplay(long remainingMillis) {
		breachHackDisplayActive = true;
		breachHackDisplayRadius = 12;
		breachHackInitialRemainingMillis = Math.max(0L, remainingMillis);
		breachHackRemainingSeconds = (int)Math.ceil(breachHackInitialRemainingMillis / 1000.0D);
		breachHackProgressObserved = false;
		markDirty();
	}

	private void refreshBreachHackDisplay() {
		OrbitalStation station = OrbitalStation.getStationFromPosition(xCoord, zCoord);
		if(station == null || station.stationKey == null || station.stationKey.isEmpty()) {
			clearBreachHackDisplay();
			return;
		}

		String phase = Integrations.getBreachPhaseWGC(worldObj, station.stationKey, station.generation);
		if(!"ACTIVE".equals(phase)) {
			clearBreachHackDisplay();
			return;
		}

		long remaining = Integrations.getBreachHackRemainingMillisWGC(worldObj, station.stationKey, station.generation);
		if(remaining < 0L) {
			clearBreachHackDisplay();
			return;
		}

		if(breachHackInitialRemainingMillis < 0L) breachHackInitialRemainingMillis = remaining;
		if(remaining < breachHackInitialRemainingMillis) breachHackProgressObserved = true;
		if(breachHackProgressObserved && remaining >= breachHackInitialRemainingMillis) {
			clearBreachHackDisplay();
			return;
		}

		breachHackRemainingSeconds = (int)Math.ceil(remaining / 1000.0D);
		markDirty();
	}

	private void clearBreachHackDisplay() {
		breachHackDisplayActive = false;
		breachHackRemainingSeconds = -1;
		breachHackInitialRemainingMillis = -1L;
		breachHackProgressObserved = false;
		markDirty();
	}

	private void spawnBreachHackBoundary() {
		double centerX = xCoord + 0.5D;
		double centerY = yCoord + 0.15D;
		double centerZ = zCoord + 0.5D;
		int points = 48;
		for(int i = 0; i < points; i++) {
			double angle = (Math.PI * 2.0D * i) / points;
			double px = centerX + Math.cos(angle) * breachHackDisplayRadius;
			double pz = centerZ + Math.sin(angle) * breachHackDisplayRadius;
			worldObj.spawnParticle("reddust", px, centerY, pz, 0.0D, 0.0D, 0.0D);
		}
	}

	@Override
	public void serialize(ByteBuf buf) {
		super.serialize(buf);
		buf.writeBoolean(hasDrive);
		buf.writeBoolean(breachHackDisplayActive);
		buf.writeInt(breachHackDisplayRadius);
		buf.writeInt(breachHackRemainingSeconds);
	}

	@Override
	public void deserialize(ByteBuf buf) {
		super.deserialize(buf);
		hasDrive = buf.readBoolean();
		breachHackDisplayActive = buf.readBoolean();
		breachHackDisplayRadius = buf.readInt();
		breachHackRemainingSeconds = buf.readInt();
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		breachHackDisplayActive = nbt.getBoolean("breachHackDisplayActive");
		breachHackDisplayRadius = nbt.hasKey("breachHackDisplayRadius") ? nbt.getInteger("breachHackDisplayRadius") : 12;
		breachHackRemainingSeconds = nbt.hasKey("breachHackRemainingSeconds") ? nbt.getInteger("breachHackRemainingSeconds") : -1;
		breachHackInitialRemainingMillis = nbt.hasKey("breachHackInitialRemainingMillis") ? nbt.getLong("breachHackInitialRemainingMillis") : -1L;
		breachHackProgressObserved = nbt.getBoolean("breachHackProgressObserved");
	}

	@Override
	public void writeToNBT(NBTTagCompound nbt) {
		super.writeToNBT(nbt);
		nbt.setBoolean("breachHackDisplayActive", breachHackDisplayActive);
		nbt.setInteger("breachHackDisplayRadius", breachHackDisplayRadius);
		nbt.setInteger("breachHackRemainingSeconds", breachHackRemainingSeconds);
		nbt.setLong("breachHackInitialRemainingMillis", breachHackInitialRemainingMillis);
		nbt.setBoolean("breachHackProgressObserved", breachHackProgressObserved);
	}

	@Override
	public Container provideContainer(int ID, EntityPlayer player, World world, int x, int y, int z) {
		return new ContainerOrbitalStationComputer();
	}

	@Override
	@SideOnly(Side.CLIENT)
	public Object provideGUI(int ID, EntityPlayer player, World world, int x, int y, int z) {
		return new GUIOrbitalStationComputer(this);
	}

	@Override
	public boolean hasPermission(EntityPlayer player) {
		return isUseableByPlayer(player);
	}

	@Override
	public void receiveControl(NBTTagCompound data) {
		if(data.hasKey("name")) {
			OrbitalStation station = OrbitalStation.getStationFromPosition(xCoord, zCoord);
			SolarSystemWorldSavedData savedData = SolarSystemWorldSavedData.get(worldObj);
			if(savedData != null) savedData.renameStation(station, data.getString("name"));
		}

		if(data.hasKey("gravity")) {
			OrbitalStation station = OrbitalStation.getStationFromPosition(xCoord, zCoord);
			station.gravityMultiplier = data.getBoolean("gravity") ? 1 : 0;
		}
	}

	AxisAlignedBB bb = null;
	
	@Override
	public AxisAlignedBB getRenderBoundingBox() {
		if(bb == null) {
			bb = AxisAlignedBB.getBoundingBox(
				xCoord,
				yCoord,
				zCoord,
				xCoord + 1,
				yCoord + 2,
				zCoord + 1
			);
		}
		
		return bb;
	}

}
