package com.hbm.entity.grenade;

import api.hbm.wgc.Integrations;
import com.hbm.explosion.ExplosionLarge;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

import com.hbm.explosion.ExplosionChaos;
import com.hbm.items.ModItems;
import com.hbm.items.weapon.ItemGrenade;

import java.util.UUID;

public class EntityGrenadeCluster extends EntityGrenadeBouncyBase
{
    public EntityGrenadeCluster(World p_i1773_1_)
    {
        super(p_i1773_1_);
    }

    public EntityGrenadeCluster(World p_i1774_1_, EntityLivingBase p_i1774_2_)
    {
        super(p_i1774_1_, p_i1774_2_);
    }

    public EntityGrenadeCluster(World p_i1775_1_, double p_i1775_2_, double p_i1775_4_, double p_i1775_6_)
    {
        super(p_i1775_1_, p_i1775_2_, p_i1775_4_, p_i1775_6_);
    }

    @Override
    public void explode() {

        if (!this.worldObj.isRemote)
        {
            this.setDead();
			Entity thrower = getThrower();
			UUID party = null;
			if(thrower instanceof EntityPlayer) party = thrower.getUniqueID();
			if(Integrations.canDetonateWGC(party,worldObj,(int)posX,(int)posY,(int)posZ)) {
				ExplosionChaos.cluster(party,worldObj, (int)this.posX, (int)this.posY, (int)this.posZ, 10, Vec3.createVectorHelper(0,0,0), 50);
				this.worldObj.createExplosion(this, this.posX, this.posY, this.posZ, 1.5F, true);
			}
        }
    }

	@Override
	protected int getMaxTimer() {
		return ItemGrenade.getFuseTicks(ModItems.grenade_cluster);
	}

	@Override
	protected double getBounceMod() {
		return 0.25D;
	}
}
