package com.hbm.entity.missile;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.hbm.entity.logic.EntityEMP;
import com.hbm.entity.logic.EntityNukeExplosionMK5;
import com.hbm.explosion.ExplosionChaos;
import com.hbm.explosion.ExplosionLarge;
import com.hbm.items.ModItems;
import com.hbm.particle.helper.ExplosionCreator;

import api.hbm.entity.IRadarDetectableNT;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

public abstract class EntityMissileTier2 extends EntityMissileBaseNT {

	public EntityMissileTier2(World world) { super(world); }
	public EntityMissileTier2(World world, float x, float y, float z, int a, int b, UUID ownerParty) { super(world, x, y, z, a, b, ownerParty); }

	@Override
	public List<ItemStack> getDebris() {
		List<ItemStack> list = new ArrayList<ItemStack>();

		list.add(new ItemStack(ModItems.plate_steel, 10));
		list.add(new ItemStack(ModItems.plate_titanium, 6));
		list.add(new ItemStack(ModItems.thruster_medium, 1));

		return list;
	}

	@Override
	public String getUnlocalizedName() {
		return "radar.target.tier2";
	}

	@Override
	public int getBlipLevel() {
		return IRadarDetectableNT.TIER2;
	}

	public static class EntityMissileStrong extends EntityMissileTier2 {
		public EntityMissileStrong(World world) { super(world); }
		public EntityMissileStrong(World world, float x, float y, float z, int a, int b, UUID ownerParty) { super(world, x, y, z, a, b, ownerParty);}
		@Override public void onMissileImpact(MovingObjectPosition mop) { this.explodeStandard(30F, 32, false); ExplosionCreator.composeEffectStandard(worldObj, posX, posY, posZ); }
		@Override public ItemStack getDebrisRareDrop() { return new ItemStack(ModItems.warhead_generic_medium); }
		@Override public ItemStack getMissileItemForInfo() { return new ItemStack(ModItems.missile_strong); }
	}

	public static class EntityMissileIncendiaryStrong extends EntityMissileTier2 {
		public EntityMissileIncendiaryStrong(World world) { super(world); }
		public EntityMissileIncendiaryStrong(World world, float x, float y, float z, int a, int b, UUID ownerParty) { super(world, x, y, z, a, b, ownerParty);}
		@Override public void onMissileImpact(MovingObjectPosition mop) {
			this.explodeStandard(30F, 32, true);
			ExplosionCreator.composeEffectStandard(worldObj, posX, posY, posZ);
			ExplosionChaos.flameDeath(ownerParty,this.worldObj, (int)((float)this.posX + 0.5F), (int)((float)this.posY + 0.5F), (int)((float)this.posZ + 0.5F), 25);
		}
		@Override public ItemStack getDebrisRareDrop() { return new ItemStack(ModItems.warhead_incendiary_medium); }
		@Override public ItemStack getMissileItemForInfo() { return new ItemStack(ModItems.missile_incendiary_strong); }
	}

	public static class EntityMissileClusterStrong extends EntityMissileTier2 {
		public static final int DETHEIGHT = 40;
		public EntityMissileClusterStrong(World world) { super(world); }
		public EntityMissileClusterStrong(World world, float x, float y, float z, int a, int b, UUID ownerParty) { super(world, x, y, z, a, b, ownerParty); this.isCluster = true; }
		@Override public void onMissileImpact(MovingObjectPosition mop) {
			if(mop == null) {
				ExplosionChaos.cluster(ownerParty,this.worldObj, (int) this.posX, (int) this.posY, (int) this.posZ, 50, Vec3.createVectorHelper(velocityX, velocityY, velocityZ), 100);
				this.killMissile();
			}
			else{
			this.worldObj.createExplosion(this, this.posX, this.posY, this.posZ, 15F, true);
			ExplosionChaos.cluster(ownerParty,this.worldObj, (int) this.posX, (int) this.posY, (int) this.posZ, 50, Vec3.createVectorHelper(velocityX, velocityY, velocityZ), 100);
			}
		}
		@Override public void cluster() { this.onMissileImpact(null); }
		@Override public void airburstCheck(Vec3 pos, Vec3 nextPos){
			if(checkForAirburst(pos, nextPos, DETHEIGHT)) {
				this.onMissileImpact(null);
			}
		}
		@Override public ItemStack getDebrisRareDrop() { return new ItemStack(ModItems.warhead_cluster_medium); }
		@Override public ItemStack getMissileItemForInfo() { return new ItemStack(ModItems.missile_cluster_strong); }
	}

	public static class EntityMissileBusterStrong extends EntityMissileTier2 {
		private static final double DEFPEN = 500.0;
		private static final int ARMDIST = 30;
		private BBParams bbparams;
		public EntityMissileBusterStrong(World world){
			super(world);
			bbparams = new BBParams(false, 0, ARMDIST, DEFPEN);
		}
		public EntityMissileBusterStrong(World world, float x, float y, float z, int a, int b, UUID ownerParty) {
			super(world, x, y, z, a, b, ownerParty);
			bbparams = new BBParams(false, 0, ARMDIST, DEFPEN);
		}
		@Override public void onMissileImpact(MovingObjectPosition mop) {
			this.explodeStandard(30F, 32, false);
			ExplosionCreator.composeEffectStandard(worldObj, posX, posY, posZ);
		}
		@Override
		public void onBlockCollide(MovingObjectPosition mop, Vec3 pos, Vec3 nextPos){
			bbparams = onCollideBunkerBuster(mop,pos,nextPos, bbparams);
		}
		@Override public ItemStack getDebrisRareDrop() { return new ItemStack(ModItems.warhead_buster_medium); }
		@Override public ItemStack getMissileItemForInfo() { return new ItemStack(ModItems.missile_buster_strong); }
	}

	public static class EntityMissileEMPStrong extends EntityMissileTier2 {
		public EntityMissileEMPStrong(World world) { super(world); }
		public EntityMissileEMPStrong(World world, float x, float y, float z, int a, int b, UUID ownerParty) { super(world, x, y, z, a, b, ownerParty); }
		@Override public void onMissileImpact(MovingObjectPosition mop) {
			EntityEMP emp = new EntityEMP(worldObj,ownerParty);
			emp.posX = posX;
			emp.posY = posY;
			emp.posZ = posZ;
			worldObj.spawnEntityInWorld(emp);
		}
		@Override public ItemStack getDebrisRareDrop() { return new ItemStack(ModItems.warhead_generic_medium); }
		@Override public ItemStack getMissileItemForInfo() { return new ItemStack(ModItems.missile_emp_strong); }
	}
}
