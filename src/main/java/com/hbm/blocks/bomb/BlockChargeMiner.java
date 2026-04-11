package com.hbm.blocks.bomb;

import java.util.List;
import java.util.UUID;

import api.hbm.wgc.Integrations;
import com.hbm.explosion.ExplosionNT;
import com.hbm.explosion.ExplosionNT.ExAttrib;
import com.hbm.particle.helper.ExplosionSmallCreator;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.World;

public class BlockChargeMiner extends BlockChargeBase {

	@Override
	public BombReturnCode explode(World world, int x, int y, int z) {

		if(!world.isRemote) {
			UUID owner = getOwner(world,x,y,z);
			if(owner == null) {
				owner = explosionOwnerCache.get();
			}
			safe = true;
			world.setBlockToAir(x, y, z);
			safe = false;
			if(!Integrations.canDetonateWGC(owner,world,x,y,z)) {
				return BombReturnCode.ERROR_BLOCKED;
			}
			ExplosionNT exp = new ExplosionNT(world, null, x + 0.5, y + 0.5, z + 0.5, 4F, owner);
			exp.addAllAttrib(ExAttrib.NOHURT, ExAttrib.ALLDROP);
			exp.explode();
			ExplosionSmallCreator.composeEffect(world, x + 0.5, y + 0.5, z + 0.5, 15, 3F, 1.25F);

			return BombReturnCode.DETONATED;
		}

		return BombReturnCode.UNDEFINED;
	}

	@Override
	public int getRenderType() {
		return BlockChargeDynamite.renderID;
	}

	@Override
	public void addInformation(ItemStack stack, EntityPlayer player, List list, boolean ext) {
		super.addInformation(stack, player, list, ext);
		list.add(EnumChatFormatting.BLUE + "Will drop all blocks.");
		list.add(EnumChatFormatting.BLUE + "Does not do damage.");
	}

}
