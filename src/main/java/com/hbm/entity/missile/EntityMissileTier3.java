package com.hbm.entity.missile;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.hbm.entity.logic.EntityNukeExplosionMK5;
import com.hbm.explosion.ExplosionChaos;
import com.hbm.explosion.ExplosionLarge;
import com.hbm.explosion.ExplosionNT;
import com.hbm.explosion.ExplosionNT.ExAttrib;
import com.hbm.items.ModItems;
import com.hbm.particle.helper.ExplosionCreator;

import api.hbm.entity.IRadarDetectableNT;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

public abstract class EntityMissileTier3 extends EntityMissileBaseNT {

	public EntityMissileTier3(World world) { super(world); }
	public EntityMissileTier3(World world, float x, float y, float z, int a, int b, UUID ownerParty) { super(world, x, y, z, a, b, ownerParty); }

	@Override
	public List<ItemStack> getDebris() {
		List<ItemStack> list = new ArrayList<ItemStack>();

		list.add(new ItemStack(ModItems.plate_steel, 16));
		list.add(new ItemStack(ModItems.plate_titanium, 10));
		list.add(new ItemStack(ModItems.thruster_large, 1));

		return list;
	}

	@Override
	public String getUnlocalizedName() {
		return "radar.target.tier3";
	}

	@Override
	public int getBlipLevel() {
		return IRadarDetectableNT.TIER3;
	}

	@Override
	protected void spawnContrail() {

		Vec3 thrust = Vec3.createVectorHelper(0, 0, 0.5);
		thrust.rotateAroundY((this.rotationYaw + 90) * (float) Math.PI / 180F);
		thrust.rotateAroundX(this.rotationPitch * (float) Math.PI / 180F);
		thrust.rotateAroundY(-(this.rotationYaw + 90) * (float) Math.PI / 180F);

		this.spawnContraolWithOffset(thrust.xCoord, thrust.yCoord, thrust.zCoord);
		this.spawnContraolWithOffset(-thrust.zCoord, thrust.yCoord, thrust.xCoord);
		this.spawnContraolWithOffset(-thrust.xCoord, -thrust.zCoord, -thrust.zCoord);
		this.spawnContraolWithOffset(thrust.zCoord, -thrust.zCoord, -thrust.xCoord);
	}

	public static class EntityMissileBurst extends EntityMissileTier3 {
		public EntityMissileBurst(World world) { super(world); }
		public EntityMissileBurst(World world, float x, float y, float z, int a, int b, UUID ownerParty) { super(world, x, y, z, a, b, ownerParty); }
		@Override public void onMissileImpact(MovingObjectPosition mop) {
			this.explodeStandard(50F, 48, false);
			ExplosionCreator.composeEffectLarge(worldObj, posX, posY, posZ);
		}
		@Override public ItemStack getDebrisRareDrop() { return new ItemStack(ModItems.warhead_generic_large); }
		@Override public ItemStack getMissileItemForInfo() { return new ItemStack(ModItems.missile_burst); }
	}

	public static class EntityMissileInferno extends EntityMissileTier3 {
		public EntityMissileInferno(World world) { super(world); }
		public EntityMissileInferno(World world, float x, float y, float z, int a, int b, UUID ownerParty) { super(world, x, y, z, a, b, ownerParty); }
		@Override public void onMissileImpact(MovingObjectPosition mop) {
			this.explodeStandard(50F, 48, true);
			ExplosionCreator.composeEffectLarge(worldObj, posX, posY, posZ);
			ExplosionChaos.burn(this.worldObj, (int)this.posX, (int)this.posY, (int)this.posZ, 10);
			ExplosionChaos.flameDeath(this.worldObj, (int)this.posX, (int)this.posY, (int)this.posZ, 25);
		}
		@Override public ItemStack getDebrisRareDrop() { return new ItemStack(ModItems.warhead_incendiary_large); }
		@Override public ItemStack getMissileItemForInfo() { return new ItemStack(ModItems.missile_inferno); }
	}

	public static class EntityMissileRain extends EntityMissileTier3 {
		public static final int DETHEIGHT = 50;
		public EntityMissileRain(World world) { super(world); }
		public EntityMissileRain(World world, float x, float y, float z, int a, int b, UUID ownerParty) { super(world, x, y, z, a, b, ownerParty);this.isCluster = true; }
		@Override public void onMissileImpact(MovingObjectPosition mop) {
			if(mop == null) {
				ExplosionChaos.cluster(ownerParty,this.worldObj, (int) this.posX, (int) this.posY, (int) this.posZ, 100, Vec3.createVectorHelper(velocityX, velocityY, velocityZ), 100);
				this.killMissile();
			}
			else {
				this.worldObj.createExplosion(this, this.posX, this.posY, this.posZ, 25F, true);
				ExplosionChaos.cluster(ownerParty,this.worldObj, (int) this.posX, (int) this.posY, (int) this.posZ, 100, Vec3.createVectorHelper(velocityX, velocityY, velocityZ), 100);
			}
		}
		@Override public void cluster() { this.onMissileImpact(null); }
		@Override public void airburstCheck(Vec3 pos, Vec3 nextPos){
			if(checkForAirburst(pos, nextPos, DETHEIGHT)) {
				this.onMissileImpact(null);
			}
		}
		@Override public ItemStack getDebrisRareDrop() { return new ItemStack(ModItems.warhead_cluster_large); }
		@Override public ItemStack getMissileItemForInfo() { return new ItemStack(ModItems.missile_rain); }
	}

	public static class EntityMissileDrill extends EntityMissileTier3 {
		private static final double DEFPEN = 1000.0;
		private static final int ARMDIST = 50;
		private BBParams bbparams;
		public EntityMissileDrill(World world) {
			super(world);
			bbparams = new BBParams(false, 0, ARMDIST, DEFPEN);
		}
		public EntityMissileDrill(World world, float x, float y, float z, int a, int b, UUID ownerParty) {
			super(world, x, y, z, a, b, ownerParty);
			bbparams = new BBParams(false, 0, ARMDIST, DEFPEN);
		}
		@Override public void onMissileImpact(MovingObjectPosition mop) {
			worldObj.spawnEntityInWorld(EntityNukeExplosionMK5.statFacNoRad(worldObj, 45, this.posX, this.posY, this.posZ,ownerParty));
			ExplosionLarge.spawnParticles(worldObj, this.posX, this.posY, this.posZ, 8);
			ExplosionLarge.spawnShrapnels(worldObj, this.posX, this.posY, this.posZ, 8);
			ExplosionLarge.spawnRubble(worldObj, this.posX, this.posY, this.posZ, 8);
			ExplosionLarge.jolt(worldObj, this.posX, this.posY, this.posZ, 10, 50, 1);
		}
		@Override
		public void onBlockCollide(MovingObjectPosition mop, Vec3 pos, Vec3 nextPos){
			bbparams = onCollideBunkerBuster(mop,pos,nextPos, bbparams);
		}
		@Override public ItemStack getDebrisRareDrop() { return new ItemStack(ModItems.warhead_buster_large); }
		@Override public ItemStack getMissileItemForInfo() { return new ItemStack(ModItems.missile_drill); }
	}
}
