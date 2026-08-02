package com.hbm.inventory.container;

import com.hbm.inventory.InventoryDriveCrate;

import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

@invtweaks.api.container.ChestContainer(rowSize = 9, isLargeChest = false)
public class ContainerDriveCrate extends ContainerCrateBase {

	public ContainerDriveCrate(InventoryPlayer invPlayer, InventoryDriveCrate inventory) {
		super(invPlayer, inventory);

		for(int row = 0; row < 4; row++) {
			for(int col = 0; col < 9; col++) {
				this.addSlotToContainer(new SlotDriveOnly(inventory, col + row * 9, 8 + col * 18, 18 + row * 18));
			}
		}
		this.playerInv(invPlayer, 8, 104, 162);
	}

	private static class SlotDriveOnly extends Slot {

		private final InventoryDriveCrate inventory;
		private final int slotIndex;

		public SlotDriveOnly(InventoryDriveCrate inventory, int slot, int x, int y) {
			super(inventory, slot, x, y);
			this.inventory = inventory;
			this.slotIndex = slot;
		}

		@Override
		public boolean isItemValid(ItemStack stack) {
			return inventory.isItemValidForSlot(slotIndex, stack);
		}
	}
}
