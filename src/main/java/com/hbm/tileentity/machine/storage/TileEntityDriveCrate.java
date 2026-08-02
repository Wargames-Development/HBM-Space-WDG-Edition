package com.hbm.tileentity.machine.storage;

import com.hbm.inventory.InventoryDriveCrate;
import com.hbm.inventory.container.ContainerDriveCrate;
import com.hbm.inventory.gui.GUIDriveCrate;
import com.hbm.tileentity.IGUIProvider;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

/** GUI anchor only. Drive contents are never stored in this tile entity. */
public class TileEntityDriveCrate extends TileEntity implements IGUIProvider {

	@Override
	public Container provideContainer(int ID, EntityPlayer player, World world, int x, int y, int z) {
		if(player == null || world == null || world.getTileEntity(x, y, z) != this) return null;
		return new ContainerDriveCrate(player.inventory, new InventoryDriveCrate(player, this));
	}

	@Override
	@SideOnly(Side.CLIENT)
	public Object provideGUI(int ID, EntityPlayer player, World world, int x, int y, int z) {
		if(player == null || world == null || world.getTileEntity(x, y, z) != this) return null;
		return new GUIDriveCrate(player.inventory, new InventoryDriveCrate(player, this));
	}
}
