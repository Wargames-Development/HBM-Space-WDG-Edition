package com.hbm.blocks.turret;

import api.hbm.wgc.Integrations;
import com.hbm.blocks.BlockDummyable;

import com.hbm.blocks.bomb.BlockPartyOwned;
import li.cil.oc.api.driver.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import java.util.UUID;

public abstract class TurretBaseNT extends BlockDummyable {

	public TurretBaseNT(Material mat) {
		super(mat);
	}

	@Override
	public int[] getDimensions() {
		return new int[] { 0, 0, 1, 0, 1, 0 };
	}

	@Override
	public int getOffset() {
		return 0;
	}

	@Override
	public void setBlockBoundsBasedOnState(IBlockAccess p_149719_1_, int p_149719_2_, int p_149719_3_, int p_149719_4_) {
		this.setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 0.5F, 1.0F);
	}

	@Override
	public AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int x, int y, int z) {
		this.setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 0.5F, 1.0F);
		return AxisAlignedBB.getBoundingBox(x + this.minX, y + this.minY, z + this.minZ, x + this.maxX, y + this.maxY, z + this.maxZ);
	}

	@Override
	public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX, float hitY, float hitZ) {
		return this.standardOpenBehavior(world, x, y, z, player, 0);
	}

	@Override
	public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase entitylivingbase, ItemStack itemStack) {
		super.onBlockPlacedBy(world,x,y,z,entitylivingbase,itemStack);
		if(!world.isRemote) {
			//Resolve factionID to avoid turrets taking the player's side.
			System.out.println("Getting faction ID");
			UUID factionID = Integrations.getPlayerFactionWGC(world, entitylivingbase.getUniqueID());
			if(factionID != null) {
				System.out.println("Faction ID found: " + factionID);
				BlockPartyOwned.setOwner(world, x, y, z, factionID);
			}
			else{
				System.out.println("Faction ID not found, falling back to player: " + entitylivingbase.getUniqueID());
				BlockPartyOwned.setOwner(world, x, y, z, entitylivingbase.getUniqueID());
			}
		}
	}
}
