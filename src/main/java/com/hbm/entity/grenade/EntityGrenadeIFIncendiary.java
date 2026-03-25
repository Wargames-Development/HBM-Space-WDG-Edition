package com.hbm.entity.grenade;

import api.hbm.wgc.Integrations;
import com.hbm.explosion.ExplosionChaos;
import com.hbm.explosion.ExplosionLarge;
import com.hbm.explosion.ExplosionThermo;
import com.hbm.items.ModItems;
import com.hbm.items.weapon.ItemGrenade;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

import java.util.UUID;

public class EntityGrenadeIFIncendiary extends EntityGrenadeBouncyBase {

    public EntityGrenadeIFIncendiary(World p_i1773_1_)
    {
        super(p_i1773_1_);
    }

    public EntityGrenadeIFIncendiary(World p_i1774_1_, EntityLivingBase p_i1774_2_)
    {
        super(p_i1774_1_, p_i1774_2_);
    }

    public EntityGrenadeIFIncendiary(World p_i1775_1_, double p_i1775_2_, double p_i1775_4_, double p_i1775_6_)
    {
        super(p_i1775_1_, p_i1775_2_, p_i1775_4_, p_i1775_6_);
    }

    @Override
    public void explode() {

        if (!this.worldObj.isRemote)
        {
            this.setDead();
			UUID party = null;
			if(thrower instanceof EntityPlayer) party = thrower.getUniqueID();

			if(Integrations.canDetonateWGC(party,worldObj,(int)posX,(int)posY,(int)posZ)) {
				ExplosionLarge.jolt(worldObj, posX, posY, posZ, 5, 200, 0.25);
				ExplosionLarge.explode(party,worldObj, posX, posY, posZ, 5, true, true, true);
				ExplosionThermo.setEntitiesOnFire(party,worldObj, (int)posX, (int)posY, (int)posZ, 8);
				ExplosionChaos.flameDeath(party,worldObj, (int)posX, (int)posY, (int)posZ, 15);
				ExplosionChaos.burn(party,worldObj, (int)posX, (int)posY, (int)posZ, 10);
			}
        }
    }

	@Override
	protected int getMaxTimer() {
		return ItemGrenade.getFuseTicks(ModItems.grenade_if_incendiary);
	}

	@Override
	protected double getBounceMod() {
		return 0.25D;
	}
}
