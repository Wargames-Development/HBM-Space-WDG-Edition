package com.hbm.explosion;

import java.util.List;

import api.hbm.explosion.event.HbmExplosionHooks;
import com.hbm.util.ContaminationUtil;
import com.hbm.util.ContaminationUtil.ContaminationType;
import com.hbm.util.ContaminationUtil.HazardType;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

public class ExplosionHurtUtil {

	/**
	 * Adds radiation to entities in an AoE
	 * @param world
	 * @param x
	 * @param y
	 * @param z
	 * @param outer The least amount of radiation received on the very edge of the AoE
	 * @param inner The greatest amount of radiation received on the very center of the AoE
	 * @param radius
	 */
	public static void doRadiation(World world, double x, double y, double z, float outer, float inner, double radius) {
		if (radius <= 0) return;
		if (HbmExplosionHooks.pre(world, x, y, z, (float) radius, null, "RADIATION")) return;
		if (world.isRemote) return;

		List<EntityLivingBase> entities = world.getEntitiesWithinAABB(
			EntityLivingBase.class,
			AxisAlignedBB.getBoundingBox(x - radius, y - radius, z - radius, x + radius, y + radius, z + radius)
		);

		for (EntityLivingBase entity : entities) {
			Vec3 v = Vec3.createVectorHelper(x - entity.posX, y - entity.posY, z - entity.posZ);
			double dist = v.lengthVector();
			if (dist > radius) continue;

			// per-entity veto
			if (HbmExplosionHooks.pre(world, entity.posX, entity.posY, entity.posZ, 0F, entity, "RADIATION.HIT")) continue;

			double t = 1.0 - (dist / radius);
			float rad = (float)(outer + (inner - outer) * t);
			ContaminationUtil.contaminate(entity, HazardType.RADIATION, ContaminationType.CREATIVE, rad);
		}
	}


}
