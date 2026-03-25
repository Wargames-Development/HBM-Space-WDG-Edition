package com.hbm.entity.grenade;

import api.hbm.wgc.Integrations;
import com.hbm.explosion.ExplosionLarge;
import com.hbm.items.ModItems;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import java.util.UUID;

public class EntityGrenadeSmart extends EntityGrenadeBase {

    public EntityGrenadeSmart(World p_i1773_1_)
    {
        super(p_i1773_1_);
    }

    public EntityGrenadeSmart(World p_i1774_1_, EntityLivingBase p_i1774_2_)
    {
        super(p_i1774_1_, p_i1774_2_);
    }

    public EntityGrenadeSmart(World p_i1775_1_, double p_i1775_2_, double p_i1775_4_, double p_i1775_6_)
    {
        super(p_i1775_1_, p_i1775_2_, p_i1775_4_, p_i1775_6_);
    }

    @Override
    public void explode() {

        if (!this.worldObj.isRemote) {
			this.setDead();

			if (this.ticksExisted > 10) {
				Entity thrown = this.getThrower();
				UUID party = null;
				if (thrown instanceof EntityPlayer) party = thrown.getUniqueID();
				if (owner != null){
					party = owner;
				}

				if (Integrations.canDetonateWGC(party, worldObj, (int) posX, (int) posY, (int) posZ)) {
					ExplosionLarge.explode(party, worldObj, posX, posY, posZ, 5.0F, true, false, false);
				} else
					worldObj.spawnEntityInWorld(new EntityItem(worldObj, posX, posY, posZ, new ItemStack(ModItems.grenade_smart)));
			}
		}
    }
}
