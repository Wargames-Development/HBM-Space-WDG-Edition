package com.hbm.explosion.vanillant.standard;

import api.hbm.explosion.event.HbmExplosionHooks;
import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.interfaces.ICustomDamageHandler;
import com.hbm.util.ContaminationUtil;
import com.hbm.util.ContaminationUtil.ContaminationType;
import com.hbm.util.ContaminationUtil.HazardType;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;

public class CustomDamageHandlerAmat implements ICustomDamageHandler {

	protected float radiation;

	public CustomDamageHandlerAmat(float radiation) {
		this.radiation = radiation;
	}

	@Override
	public void handleAttack(ExplosionVNT explosion, Entity entity, double distanceScaled) {
		if (!(entity instanceof EntityLivingBase)) return;

		// Per-entity safezone/claim veto: skip radiation in protected areas
		if (HbmExplosionHooks.pre(entity.worldObj, entity.posX, entity.posY, entity.posZ, 0F, entity, "VNT.AMAT.DAMAGE"))
			return;

		EntityLivingBase target = (EntityLivingBase) entity;

		ContaminationUtil.contaminate(target, HazardType.RADIATION, ContaminationType.CREATIVE, (float) (radiation * (1D - distanceScaled) * explosion.size));
	}
}
