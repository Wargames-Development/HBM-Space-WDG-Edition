package com.hbm.entity.grenade;

import api.hbm.wgc.Integrations;
import com.hbm.explosion.ExplosionChaos;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

import java.util.UUID;

public class EntityGrenadeCloud extends EntityGrenadeBase {

	public EntityGrenadeCloud(World p_i1773_1_) {
		super(p_i1773_1_);
	}

	public EntityGrenadeCloud(World p_i1774_1_, EntityLivingBase p_i1774_2_) {
		super(p_i1774_1_, p_i1774_2_);
	}

	public EntityGrenadeCloud(World p_i1775_1_, double p_i1775_2_, double p_i1775_4_, double p_i1775_6_) {
		super(p_i1775_1_, p_i1775_2_, p_i1775_4_, p_i1775_6_);
	}

	@Override
	public void explode() {

		if (!this.worldObj.isRemote) {
			this.setDead();
			UUID party = null;
			if(getThrower() instanceof EntityPlayer) party = getThrower().getUniqueID();

			if(Integrations.canDetonateWGC(party,worldObj,(int)posX,(int)posY,(int)posZ)) {
				this.worldObj.playAuxSFX(2002, (int) Math.round(this.posX), (int) Math.round(this.posY), (int) Math.round(this.posZ), 0);
				ExplosionChaos.spawnPoisonCloud(party,worldObj, posX, posY, posZ, 250, 1.5, 1);
			}
		}
	}
}
