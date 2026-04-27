package com.hbm.blocks.turret;

import java.util.Random;

import com.hbm.blocks.bomb.BlockPartyOwned;
import com.hbm.tileentity.turret.TileEntityTurretSentryDamaged;

import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public class TurretSentryDamaged extends BlockContainer {

	public TurretSentryDamaged() {
		super(Material.iron);
	}

	@Override
	public TileEntity createNewTileEntity(World world, int meta) {
		return new TileEntityTurretSentryDamaged();
	}

	@Override
	public int getRenderType() {
		return -1;
	}

	@Override
	public boolean isOpaqueCube() {
		return false;
	}

	@Override
	public boolean renderAsNormalBlock() {
		return false;
	}

	@Override
	public Item getItemDropped(int i, Random rand, int j) {
		return null;
	}

	@Override
	public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase entitylivingbase, ItemStack itemStack) {
		if(!world.isRemote) {
			BlockPartyOwned.setOwner(world,x,y,z,entitylivingbase.getUniqueID());
		}
	}
}
