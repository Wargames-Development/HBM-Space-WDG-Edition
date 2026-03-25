package com.hbm.entity.grenade;

import api.hbm.wgc.Integrations;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

import java.util.UUID;

public class EntityGrenadeDynamite extends EntityGrenadeBouncyBase {

	public EntityGrenadeDynamite(World world) {
		super(world);
	}

	public EntityGrenadeDynamite(World world, EntityLivingBase living) {
		super(world, living);
	}

	public EntityGrenadeDynamite(World world, double x, double y, double z) {
		super(world, x, y, z);
	}

	@Override
	public void explode() {
		UUID party = null;
		if(getThrower() instanceof EntityPlayer) party = getThrower().getUniqueID();

		if(Integrations.canDetonateWGC(party,worldObj,(int)posX,(int)posY,(int)posZ)) {
			worldObj.newExplosion(this, posX, posY + 0.25D, posZ, 3F, false, false);
		}
		this.setDead();
	}

	@Override
	protected int getMaxTimer() {
		return 60;
	}

	@Override
	protected double getBounceMod() {
		return 0.5D;
	}
}
