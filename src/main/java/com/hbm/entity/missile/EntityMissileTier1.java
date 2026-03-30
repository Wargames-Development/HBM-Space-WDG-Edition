package com.hbm.entity.missile;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import api.hbm.wgc.Integrations;
import com.hbm.explosion.ExplosionChaos;
import com.hbm.explosion.ExplosionLarge;
import com.hbm.items.ModItems;
import com.hbm.particle.helper.ExplosionCreator;

import api.hbm.entity.IRadarDetectableNT;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;

public abstract class EntityMissileTier1 extends EntityMissileBaseNT {

	public EntityMissileTier1(World world) { super(world); }
	public EntityMissileTier1(World world, float x, float y, float z, int a, int b, UUID ownerParty) { super(world, x, y, z, a, b, ownerParty); }

	@Override
	public List<ItemStack> getDebris() {
		List<ItemStack> list = new ArrayList<ItemStack>();
		list.add(new ItemStack(ModItems.plate_titanium, 4));
		list.add(new ItemStack(ModItems.thruster_small, 1));
		return list;
	}

	@Override
	protected float getContrailScale() {
		return 0.5F;
	}

	public static class EntityMissileGeneric extends EntityMissileTier1 {
		public EntityMissileGeneric(World world) { super(world); }
		public EntityMissileGeneric(World world, float x, float y, float z, int a, int b, UUID ownerParty) { super(world, x, y, z, a, b, ownerParty); }
		@Override public void onMissileImpact(MovingObjectPosition mop) { this.explodeStandard(15F, 24, false); ExplosionCreator.composeEffectSmall(worldObj, posX, posY, posZ); }
		@Override public ItemStack getDebrisRareDrop() { return new ItemStack(ModItems.warhead_generic_small); }
		@Override public ItemStack getMissileItemForInfo() { return new ItemStack(ModItems.missile_generic); }
	}

	public static class EntityMissileDecoy extends EntityMissileTier1 {
		public EntityMissileDecoy(World world) { super(world); }
		public EntityMissileDecoy(World world, float x, float y, float z, int a, int b, UUID ownerParty) { super(world, x, y, z, a, b, ownerParty); }
		@Override public void onMissileImpact(MovingObjectPosition mop) { worldObj.newExplosion(this, posX, posY, posZ, 4F, false, false); }
		@Override public ItemStack getDebrisRareDrop() { return new ItemStack(ModItems.ingot_steel); }
		@Override public String getUnlocalizedName() { return "radar.target.tier4"; }
		@Override public int getBlipLevel() { return IRadarDetectableNT.TIER4; }
		@Override public ItemStack getMissileItemForInfo() { return new ItemStack(ModItems.missile_decoy); }
	}

	public static class EntityMissileIncendiary extends EntityMissileTier1 {
		public EntityMissileIncendiary(World world) { super(world); }
		public EntityMissileIncendiary(World world, float x, float y, float z, int a, int b, UUID ownerParty) { super(world, x, y, z, a, b, ownerParty); }
		@Override public void onMissileImpact(MovingObjectPosition mop) { this.explodeStandard(15F, 24, true); ExplosionCreator.composeEffectSmall(worldObj, posX, posY, posZ); }
		@Override public ItemStack getDebrisRareDrop() { return new ItemStack(ModItems.warhead_incendiary_small); }
		@Override public ItemStack getMissileItemForInfo() { return new ItemStack(ModItems.missile_incendiary); }
	}

	public static class EntityMissileCluster extends EntityMissileTier1 {
		public static final int DETHEIGHT = 32;
		public EntityMissileCluster(World world) { super(world); }
		public EntityMissileCluster(World world, float x, float y, float z, int a, int b, UUID ownerParty) { super(world, x, y, z, a, b, ownerParty); this.isCluster = true; }
		@Override public void onMissileImpact(MovingObjectPosition mop) {
			if(mop == null) {
				ExplosionChaos.cluster(this.worldObj, (int) this.posX, (int) this.posY, (int) this.posZ, 25, Vec3.createVectorHelper(velocityX, velocityY, velocityZ), 100);
				this.killMissile();
			}
			else{
				this.worldObj.createExplosion(this, this.posX, this.posY, this.posZ, 5F, true);
				ExplosionChaos.cluster(this.worldObj, (int) this.posX, (int) this.posY, (int) this.posZ, 25, Vec3.createVectorHelper(0, 0, 0), 100);
			}
		}
		@Override public void cluster() { this.onMissileImpact(null); }
		@Override public void airburstCheck(Vec3 pos, Vec3 nextPos){
			if(checkForAirburst(pos, nextPos, DETHEIGHT)) {
				this.onMissileImpact(null);
			}
		}
		@Override public ItemStack getDebrisRareDrop() { return new ItemStack(ModItems.warhead_cluster_small); }
		@Override public ItemStack getMissileItemForInfo() { return new ItemStack(ModItems.missile_cluster); }
	}

	public static class EntityMissileBunkerBuster extends EntityMissileTier1 {
		private static final double DEFPEN = 200.0;
		private static final int ARMDIST = 20;
		private BBParams bbparams;
		public EntityMissileBunkerBuster(World world) {
			super(world);
			bbparams = new BBParams(false, 0, ARMDIST, DEFPEN);
		}
		public EntityMissileBunkerBuster(World world, float x, float y, float z, int a, int b, UUID ownerParty) {
			super(world, x, y, z, a, b, ownerParty);
			bbparams = new BBParams(false, 0, ARMDIST, DEFPEN);
		}
		@Override public void onMissileImpact(MovingObjectPosition mop) {
			this.explodeStandard(15F, 24, false);
			ExplosionCreator.composeEffectSmall(worldObj, posX, posY, posZ);
		}
		@Override
		public void onBlockCollide(MovingObjectPosition mop, Vec3 pos, Vec3 nextPos){
			bbparams = onCollideBunkerBuster(mop,pos,nextPos, bbparams);
		}
		@Override public ItemStack getDebrisRareDrop() { return new ItemStack(ModItems.warhead_buster_small); }
		@Override public ItemStack getMissileItemForInfo() { return new ItemStack(ModItems.missile_buster); }
	}
}
