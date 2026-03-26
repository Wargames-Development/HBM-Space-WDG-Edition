package com.hbm.blocks.bomb;

import com.hbm.config.GeneralConfig;
import com.hbm.entity.item.EntityTNTPrimedBase;

import com.hbm.main.MainRegistry;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.apache.logging.log4j.Level;

public class BlockC4 extends BlockTNTBase {

	@Override
	public void explodeEntity(World world, double x, double y, double z, EntityTNTPrimedBase entity) {
		world.createExplosion(entity, x, y, z, 15F, true);
	}
	@Override
	public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase player, ItemStack itemStack) {
		if(!world.isRemote) {
			BlockPartyOwned.setOwner(world,x,y,z, player.getUniqueID());
		}
	}
}
