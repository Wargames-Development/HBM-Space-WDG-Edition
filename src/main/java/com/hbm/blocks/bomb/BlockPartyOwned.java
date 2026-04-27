package com.hbm.blocks.bomb;

import api.hbm.tile.IPartyOwned;
import appeng.api.parts.IPart;
import com.hbm.tileentity.bomb.TileEntityPartyOwned;
import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.material.Material;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import java.util.UUID;

public class BlockPartyOwned extends Block{
	protected BlockPartyOwned(Material p_i45394_1_) {
		super(p_i45394_1_);
	}

	@Override
	public boolean hasTileEntity(int metadata) {//Party Owned blocks will have a tileEntity.
		return true;
	}
	@Override
	public void breakBlock(World world, int x, int y, int z, Block block, int meta) {
		super.breakBlock(world, x, y, z, block, meta);
	}
	@Override
	public TileEntity createTileEntity(World world, int meta) {
		return new TileEntityPartyOwned();
	}

	public static UUID getOwner(World world, int x, int y, int z) {
		TileEntity te = world.getTileEntity(x, y, z);
		UUID owner = null;

		if(te instanceof IPartyOwned){
			owner = ((IPartyOwned) te).getOwner();
		}
		return owner;
	}
	public static void setOwner(World world, int x, int y, int z, UUID newOwner) {
		TileEntity te = world.getTileEntity(x, y, z);

		if (te instanceof IPartyOwned) {
			((IPartyOwned) te).setOwner(newOwner);
			te.markDirty();
		}
	}

}
