package com.hbm.blocks.machine;

import com.hbm.tileentity.machine.TileEntityOrbitalStationRaidingPort;

import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

/** A generated docking port that only accepts Raid Hard Drive arrivals. */
public class BlockOrbitalStationRaidingPort extends BlockOrbitalStation {

	public BlockOrbitalStationRaidingPort(Material material) {
		super(material);
	}

	@Override
	public TileEntity createNewTileEntity(World world, int meta) {
		if(meta >= 12) return new TileEntityOrbitalStationRaidingPort();
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
