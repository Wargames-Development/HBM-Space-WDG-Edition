package com.hbm.inventory.container;

import com.hbm.items.ModItems;

import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

public class ContainerStationDriveTerminal extends ContainerBase {

    public ContainerStationDriveTerminal(InventoryPlayer player, IInventory machine) {
        super(player, machine);

        // Primary drive: empty/full normal drive or blank/programmed Breach drive.
        addSlotToContainer(new Slot(machine, 0, 29, 18) {
            @Override
            public boolean isItemValid(ItemStack stack) {
                if(stack == null) return false;
                return stack.getItem() == ModItems.hard_drive
                    || stack.getItem() == ModItems.full_drive
                    || stack.getItem() == ModItems.raid_drive;
            }
        });

        // Clone target / standalone RAD target reference.
        addSlotToContainer(new Slot(machine, 1, 83, 18) {
            @Override
            public boolean isItemValid(ItemStack stack) {
                if(stack == null) return false;
                return stack.getItem() == ModItems.hard_drive
                    || stack.getItem() == ModItems.full_drive
                    || stack.getItem() == ModItems.raid_drive;
            }
        });

        addSlotToContainer(new Slot(machine, 2, 135, 72));
        playerInv(player, 8, 125, 183);
    }
}
