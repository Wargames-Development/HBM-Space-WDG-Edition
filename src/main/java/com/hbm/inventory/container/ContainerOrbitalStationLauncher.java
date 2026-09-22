package com.hbm.inventory.container;

import com.hbm.handler.RocketStruct;
import com.hbm.inventory.SlotNonRetarded;
import com.hbm.inventory.SlotTakeOnly;
import com.hbm.inventory.SlotRocket.SlotCapsule;
import com.hbm.inventory.SlotRocket.SlotDrive;
import com.hbm.inventory.SlotRocket.SlotRocketPart;
import com.hbm.items.ItemVOTVdrive;
import com.hbm.items.weapon.ItemCustomMissilePart.PartType;
import com.hbm.tileentity.machine.TileEntityOrbitalStationLauncher;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

public class ContainerOrbitalStationLauncher extends ContainerBase {

	private final TileEntityOrbitalStationLauncher launcher;
	private final EntityPlayer viewingPlayer;

	public ContainerOrbitalStationLauncher(InventoryPlayer invPlayer, TileEntityOrbitalStationLauncher machine) {
		super(invPlayer, machine);
		this.launcher = machine;
		this.viewingPlayer = invPlayer.player;

		int slotId = 0;

		// Target drive. Keep insertion deterministic on both client and server;
		// authorization is presented as launcher state and enforced at launch.
		addSlotToContainer(new SlotNonRetarded(machine, slotId++, 42, 55));

		// Fuel in
		addSlotToContainer(new Slot(machine, slotId++, 42, 73));

		// Fuel out must never accept shift-clicked station drives.
		addSlotToContainer(new SlotTakeOnly(machine, slotId++, 42, 91));

		// Capsule slot
		addSlotToContainer(new SlotCapsule(machine, slotId++, 18, 13));

		// Stages
		for(int i = 0; i < RocketStruct.MAX_STAGES; i++) {
			addSlotToContainer(new SlotRocketPart(machine, slotId++, 18, 44, i, PartType.FUSELAGE));
			addSlotToContainer(new SlotRocketPart(machine, slotId++, 18, 62, i, PartType.FINS));
			addSlotToContainer(new SlotRocketPart(machine, slotId++, 18, 80, i, PartType.THRUSTER));
		}

		// Drives
		for(int i = 0; i < RocketStruct.MAX_STAGES; i++) {
			addSlotToContainer(new SlotDrive(machine, slotId++, 161, 54, i));
			addSlotToContainer(new SlotDrive(machine, slotId++, 170, 87, i));
		}

		addSlots(invPlayer, 9, 8, 142, 3, 9);
		addSlots(invPlayer, 0, 8, 200, 1, 9);

		if(!invPlayer.player.worldObj.isRemote) launcher.refreshDriveAuthorization(invPlayer.player);
	}

	@Override
	public ItemStack transferStackInSlot(EntityPlayer player, int index) {
		if(index >= tile.getSizeInventory() && index < inventorySlots.size()) {
			Slot source = (Slot)inventorySlots.get(index);
			ItemStack stack = source != null ? source.getStack() : null;
			if(ItemVOTVdrive.isUsableDrive(stack) && !launcher.isDriveAuthorizedForPlayer(stack, player)) {
				launcher.noteDriveAuthorizationFailure(player, stack);
			}
			// Preserve the launcher's existing no-shift-in behavior.
			return null;
		}
		return super.transferStackInSlot(player, index);
	}

	@Override
	public ItemStack slotClick(int index, int button, int mode, EntityPlayer player) {
		// Prevent broad machine-slot shift-click routing; it can otherwise place a
		// rejected target drive into unrelated fuel/output slots.
		if(index >= tile.getSizeInventory() && mode == 1) {
			ItemStack source = ((Slot)this.inventorySlots.get(index)).getStack();
			if(ItemVOTVdrive.isUsableDrive(source) && !launcher.isDriveAuthorizedForPlayer(source, player)) {
				launcher.noteDriveAuthorizationFailure(player, source);
			}
			return null;
		}

		if(index < tile.getSizeInventory() - RocketStruct.MAX_STAGES * 2 || index >= tile.getSizeInventory()) {
			ItemStack result = super.slotClick(index, button, mode, player);
			launcher.refreshDriveAuthorization(player);
			return result;
		}

		Slot slot = this.getSlot(index);
		ItemStack ret = null;
		ItemStack held = player.inventory.getItemStack();
		if(held != null && !slot.isItemValid(held)) return null;
		if(slot.getHasStack()) ret = slot.getStack().copy();
		slot.putStack(held != null ? held.copy() : null);
		if(slot.getHasStack()) slot.getStack().stackSize = 1;
		slot.onSlotChanged();
		launcher.refreshDriveAuthorization(player);
		return ret;
	}
	@Override
	public void detectAndSendChanges() {
		if(viewingPlayer != null && !viewingPlayer.worldObj.isRemote) {
			launcher.refreshDriveAuthorization(viewingPlayer);
		}
		super.detectAndSendChanges();
	}

}
