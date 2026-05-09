package com.hbm.inventory.container;

import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;

public class ContainerDriveProcessor extends ContainerBase {

	public ContainerDriveProcessor(InventoryPlayer invPlayer, IInventory machine) {
		super(invPlayer, machine);

		// 0 - active drive slot
		// 1 - cloning drive slot

		// 2 - upgrade slot

		// 3 - battery slot

		// 4- designator slot

		addSlotToContainer(new Slot(machine, 0, 44, 18));
		addSlotToContainer(new Slot(machine, 1, 64, 38));

		addSlotToContainer(new Slot(machine, 2, 95, 24));

		addSlotToContainer(new Slot(machine, 3, 148, 72));

		addSlotToContainer(new Slot(machine,4,7,51));

		playerInv(invPlayer, 8, 125, 183);
	}

}
