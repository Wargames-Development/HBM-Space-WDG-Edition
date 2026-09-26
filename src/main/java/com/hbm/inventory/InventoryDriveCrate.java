package com.hbm.inventory;

import com.hbm.blocks.ModBlocks;
import com.hbm.extprop.HbmPlayerProps;
import com.hbm.items.ModItems;
import com.hbm.tileentity.machine.storage.TileEntityDriveCrate;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;

/**
 * Per-player Drive Crate inventory view. The authoritative slots are stored in
 * HbmPlayerProps; this object only supplies container access and interaction context.
 */
public class InventoryDriveCrate implements IInventory {

	public static final int SIZE = 36;

	private final EntityPlayer player;
	private final TileEntityDriveCrate placedCrate;

	public InventoryDriveCrate(EntityPlayer player, TileEntityDriveCrate placedCrate) {
		this.player = player;
		this.placedCrate = placedCrate;
	}


	private HbmPlayerProps props() {
		return HbmPlayerProps.getData(player);
	}

	public static boolean isAllowedDrive(ItemStack stack) {
		if(stack == null) return false;
		return stack.getItem() == ModItems.hard_drive
				|| stack.getItem() == ModItems.full_drive
				|| stack.getItem() == ModItems.raid_drive
				|| stack.getItem() == ModItems.corrupted_drive;
	}

	@Override
	public int getSizeInventory() {
		return SIZE;
	}

	@Override
	public ItemStack getStackInSlot(int slot) {
		if(slot < 0 || slot >= SIZE) return null;
		return props().driveCrateInventory[slot];
	}

	@Override
	public ItemStack decrStackSize(int slot, int amount) {
		ItemStack stack = getStackInSlot(slot);
		if(stack == null || amount <= 0) return null;

		if(stack.stackSize <= amount) {
			props().driveCrateInventory[slot] = null;
			markDirty();
			return stack;
		}

		ItemStack split = stack.splitStack(amount);
		if(stack.stackSize <= 0) props().driveCrateInventory[slot] = null;
		markDirty();
		return split;
	}

	@Override
	public ItemStack getStackInSlotOnClosing(int slot) {
		if(slot < 0 || slot >= SIZE) return null;
		ItemStack stack = props().driveCrateInventory[slot];
		props().driveCrateInventory[slot] = null;
		markDirty();
		return stack;
	}

	@Override
	public void setInventorySlotContents(int slot, ItemStack stack) {
		if(slot < 0 || slot >= SIZE) return;

		if(stack != null && !isAllowedDrive(stack)) return;

		if(stack != null && stack.stackSize > getInventoryStackLimit()) {
			stack.stackSize = getInventoryStackLimit();
		}
		props().driveCrateInventory[slot] = stack;
		markDirty();
	}

	@Override
	public String getInventoryName() {
		return "container.driveCrate";
	}

	@Override
	public boolean hasCustomInventoryName() {
		return false;
	}

	@Override
	public int getInventoryStackLimit() {
		return 64;
	}

	@Override
	public void markDirty() {
		for(int i = 0; i < SIZE; i++) {
			ItemStack stack = props().driveCrateInventory[i];
			if(stack != null && stack.stackSize <= 0) props().driveCrateInventory[i] = null;
		}
	}

	@Override
	public boolean isUseableByPlayer(EntityPlayer user) {
		if(user == null || user != player) return false;

		if(placedCrate == null || placedCrate.isInvalid() || placedCrate.getWorldObj() == null) return false;
		TileEntity current = placedCrate.getWorldObj().getTileEntity(placedCrate.xCoord, placedCrate.yCoord, placedCrate.zCoord);
		if(current != placedCrate || placedCrate.getWorldObj().getBlock(placedCrate.xCoord, placedCrate.yCoord, placedCrate.zCoord) != ModBlocks.drive_crate) return false;
		return user.getDistanceSq(placedCrate.xCoord + 0.5D, placedCrate.yCoord + 0.5D, placedCrate.zCoord + 0.5D) <= 64.0D;
	}

	@Override
	public void openInventory() {
		props().recoverInvalidDriveCrateItems();
		if(player == null || player.worldObj == null) return;
		if(placedCrate != null) {
			player.worldObj.playSoundEffect(placedCrate.xCoord + 0.5D, placedCrate.yCoord + 0.5D, placedCrate.zCoord + 0.5D, "hbm:block.crateOpen", 1.0F, 1.0F);
		}
	}

	@Override
	public void closeInventory() {
		if(player == null || player.worldObj == null) return;
		if(placedCrate != null && !placedCrate.isInvalid()) {
			player.worldObj.playSoundEffect(placedCrate.xCoord + 0.5D, placedCrate.yCoord + 0.5D, placedCrate.zCoord + 0.5D, "hbm:block.crateClose", 1.0F, 1.0F);
		}
	}

	@Override
	public boolean isItemValidForSlot(int slot, ItemStack stack) {
		return slot >= 0 && slot < SIZE && isAllowedDrive(stack);
	}
}
