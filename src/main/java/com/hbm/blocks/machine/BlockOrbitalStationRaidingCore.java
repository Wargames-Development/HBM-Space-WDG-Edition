package com.hbm.blocks.machine;

import com.hbm.tileentity.machine.TileEntityOrbitalStationRaidingCore;

import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

/** Distinct generated raid docking core; it is never the station's normal main port. */
public class BlockOrbitalStationRaidingCore extends BlockOrbitalStation {

	public BlockOrbitalStationRaidingCore(Material mat) {
		super(mat);
	}

	@Override
	public TileEntity createNewTileEntity(World world, int meta) {
		if(meta >= 12) return new TileEntityOrbitalStationRaidingCore();
		return super.createNewTileEntity(world, meta);
	}

	@Override
	public boolean canPlaceBlockAt(World world, int x, int y, int z) {
		return false;
	}

	@Override
	public boolean removedByPlayer(World world, EntityPlayer player, int x, int y, int z, boolean willHarvest) {
		return false;
	}
}
