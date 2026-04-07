package com.hbm.blocks;

import com.hbm.handler.MultiblockHandlerXR;
import com.hbm.tileentity.IPersistentNBT;
import net.minecraft.block.material.Material;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import java.util.UUID;

public abstract class BlockDummyableClaimlocked extends BlockDummyable {

	public BlockDummyableClaimlocked(Material mat) {
		super(mat);
	}

	@Override
	public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase player, ItemStack itemStack) {
		super.onBlockPlacedBy(world,x,y,z,player,itemStack);

		if(!(player instanceof EntityPlayer))
			return;

		ForgeDirection placedSide = ForgeDirection.getOrientation(world.getBlockMetadata(x, y, z));

		EntityPlayer pl = (EntityPlayer) player;
		// The direction the player is facing, for offsetting the block away from the player
		ForgeDirection facingDir = ForgeDirection.NORTH;


		int i = MathHelper.floor_double(player.rotationYaw * 4.0F / 360.0F + 0.5D) & 3;

		if(placedSide == ForgeDirection.UP || placedSide == ForgeDirection.DOWN) {
			if(i == 0) facingDir = ForgeDirection.getOrientation(2);
			if(i == 1) facingDir = ForgeDirection.getOrientation(5);
			if(i == 2) facingDir = ForgeDirection.getOrientation(3);
			if(i == 3) facingDir = ForgeDirection.getOrientation(4);
		} else {
			facingDir = placedSide;
		}

		ForgeDirection dir = getDirModified(facingDir);

		int o = -getOffset();

		int ox = x + facingDir.offsetX * o;
		int oy = y + getHeightOffset();
		int oz = z + facingDir.offsetZ * o;

		if(!checkRequirement(world, ox - dir.offsetX * o, oy, oz - dir.offsetZ * o, dir, o)) {

			if(!pl.capabilities.isCreativeMode) {
				ItemStack stack = pl.inventory.mainInventory[pl.inventory.currentItem];
				Item item = Item.getItemFromBlock(this);

				if(stack == null) {
					pl.inventory.mainInventory[pl.inventory.currentItem] = new ItemStack(this);
				} else {
					if(stack.getItem() != item || stack.stackSize == stack.getMaxStackSize()) {
						pl.inventory.addItemStackToInventory(new ItemStack(this));
					} else {
						pl.getHeldItem().stackSize++;
					}
				}
			}
		}
	}

	protected boolean checkRequirementPartyOwned(World world, int x, int y, int z, ForgeDirection dir, int o, UUID party) {
		return MultiblockHandlerXR.checkSpaceClaimLocked(world, x + dir.offsetX * o, y + dir.offsetY * o, z + dir.offsetZ * o, getDimensions(), x, y, z, dir,party);
	}

}
