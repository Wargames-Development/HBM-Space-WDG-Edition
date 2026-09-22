package com.hbm.inventory;

import com.hbm.items.ItemVOTVdrive;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

/**
 * Player-aware launcher slot. WGCore-backed station/Breach drives are rejected
 * before they can be inserted by a player outside the authorized faction.
 */
public class SlotAuthorizedStationDrive extends SlotNonRetarded {

    private final EntityPlayer player;
    private final World world;

    public SlotAuthorizedStationDrive(IInventory inventory, int id, int x, int y, EntityPlayer player, World world) {
        super(inventory, id, x, y);
        this.player = player;
        this.world = world;
    }

    @Override
    public boolean isItemValid(ItemStack stack) {
        if(!super.isItemValid(stack)) return false;
        if(world == null || world.isRemote || player == null) return true;
        return ItemVOTVdrive.canPlayerUseStationDriveForLaunch(stack, world, player.getUniqueID());
    }
}
