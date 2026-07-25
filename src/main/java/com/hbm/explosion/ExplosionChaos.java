package com.hbm.explosion;

import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import api.hbm.wgc.Integrations;
import com.hbm.blocks.ModBlocks;
import com.hbm.entity.particle.EntityCloudFX;
import com.hbm.entity.particle.EntityModFX;
import com.hbm.entity.particle.EntityOrangeFX;
import com.hbm.entity.particle.EntityPinkCloudFX;
import com.hbm.entity.projectile.EntityBulletBaseMK4;
import com.hbm.interfaces.Spaghetti;
import com.hbm.items.weapon.sedna.factory.XFactoryCatapult;
import com.hbm.lib.ModDamageSource;
import com.hbm.potion.HbmPotion;
import com.hbm.util.ArmorRegistry;
import com.hbm.util.ArmorUtil;
import com.hbm.util.ArmorRegistry.HazardClass;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.passive.EntitySheep;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

@Deprecated
@Spaghetti("my eyes are bleeding")
public class ExplosionChaos { //TODO: destroy this entire class

	private final static Random random = new Random();
	private static Random rand = new Random();

	public static void hardenVirus(World world, int x, int y, int z, int bombStartStrength) {
		hardenVirus(null, world, x, y, z, bombStartStrength);
	}

	public static void hardenVirus(UUID ownerParty, World world, int x, int y, int z, int bombStartStrength) {
		int r = bombStartStrength;
		Set<ChunkCoordIntPair> protectedChunks = Integrations.getExplosionProtectedChunksWGC(ownerParty, world, x, z, r + 16);
		int r22 = r * r / 2;
		for(int xx = -r; xx < r; xx++) {
			int X = xx + x;
			int XX = xx * xx;
			for(int zz = -r; zz < r; zz++) {
				int Z = zz + z;
				if(protectedChunks.contains(new ChunkCoordIntPair(X >> 4, Z >> 4))) continue;
				int ZZ = XX + zz * zz;
				for(int yy = -r; yy < r; yy++) {
					int Y = yy + y;
					if(ZZ + yy * yy < r22 && world.getBlock(X, Y, Z) == ModBlocks.crystal_virus) {
						world.setBlock(X, Y, Z, ModBlocks.crystal_hardened);
					}
				}
			}
		}
	}

	public static void igniteFlammableBlocks(World world, int x, int y, int z, int bound) {
		igniteFlammableBlocks(null, world, x, y, z, bound);
	}

	public static void igniteFlammableBlocks(UUID ownerParty, World world, int x, int y, int z, int bound) {
		int r = bound;
		Set<ChunkCoordIntPair> protectedChunks = Integrations.getExplosionProtectedChunksWGC(ownerParty, world, x, z, r + 16);
		int r22 = r * r / 2;
		for(int xx = -r; xx < r; xx++) {
			int X = xx + x;
			int XX = xx * xx;
			for(int zz = -r; zz < r; zz++) {
				int Z = zz + z;
				if(protectedChunks.contains(new ChunkCoordIntPair(X >> 4, Z >> 4))) continue;
				int ZZ = XX + zz * zz;
				for(int yy = -r; yy < r; yy++) {
					int Y = yy + y;
					if(ZZ + yy * yy < r22 && world.getBlock(X, Y, Z).isFlammable(world, X, Y, Z, ForgeDirection.UP) && world.getBlock(X, Y + 1, Z) == Blocks.air) {
						world.setBlock(X, Y + 1, Z, Blocks.fire);
					}
				}
			}
		}
	}

	@Deprecated public static void flameDeath(UUID ownerParty, World world, int x, int y, int z, int bound) {
		igniteFlammableBlocks(ownerParty, world, x, y, z, bound);
	}

	public static void igniteAllBlocks(World world, int x, int y, int z, int bound) {
		igniteAllBlocks(null, world, x, y, z, bound);
	}

	public static void igniteAllBlocks(UUID ownerParty, World world, int x, int y, int z, int bound) {
		int r = bound;
		Set<ChunkCoordIntPair> protectedChunks = Integrations.getExplosionProtectedChunksWGC(ownerParty, world, x, z, r + 16);
		int r22 = r * r / 2;
		for(int xx = -r; xx < r; xx++) {
			int X = xx + x;
			int XX = xx * xx;
			for(int zz = -r; zz < r; zz++) {
				int Z = zz + z;
				if(protectedChunks.contains(new ChunkCoordIntPair(X >> 4, Z >> 4))) continue;
				int ZZ = XX + zz * zz;
				for(int yy = -r; yy < r; yy++) {
					int Y = yy + y;
					if(ZZ + yy * yy < r22 && (world.getBlock(X, Y + 1, Z) == Blocks.air || world.getBlock(X, Y + 1, Z) == Blocks.snow_layer) && world.getBlock(X, Y, Z) != Blocks.air) {
						world.setBlock(X, Y + 1, Z, Blocks.fire);
					}
				}
			}
		}
	}

	@Deprecated public static void burn(UUID ownerParty, World world, int x, int y, int z, int bound) {
		igniteAllBlocks(ownerParty, world, x, y, z, bound);
	}

	@Deprecated public static void spawnPoisonCloud(World world, double x, double y, double z, int count, double speed, int type) {
		spawnPoisonCloud(null, world, x, y, z, count, speed, type);
	}

	@Deprecated public static void spawnPoisonCloud(UUID ownerParty, World world, double x, double y, double z, int count, double speed, int type) {
		for(int i = 0; i < count; i++) {
			EntityModFX fx;
			if(type == 1) fx = new EntityCloudFX(ownerParty, world, x, y, z, 0.0, 0.0, 0.0);
			else if(type == 2) fx = new EntityPinkCloudFX(ownerParty, world, x, y, z, 0.0, 0.0, 0.0);
			else fx = new EntityOrangeFX(ownerParty, world, x, y, z, 0.0, 0.0, 0.0);
			fx.motionY = rand.nextGaussian() * speed;
			fx.motionX = rand.nextGaussian() * speed;
			fx.motionZ = rand.nextGaussian() * speed;
			world.spawnEntityInWorld(fx);
		}
	}

	public static void spawnVolley(World world, double x, double y, double z, int count, double speed) {
		spawnVolley(null, world, x, y, z, count, speed);
	}

	public static void spawnVolley(UUID ownerParty, World world, double x, double y, double z, int count, double speed) {
		for(int i = 0; i < count; i++) {
			EntityModFX fx = new EntityOrangeFX(ownerParty, world, x, y, z, 0.0, 0.0, 0.0);
			fx.motionX = rand.nextGaussian() * speed;
			fx.motionZ = rand.nextGaussian() * speed;
			fx.motionY = rand.nextDouble() * speed * 7.5D;
			world.spawnEntityInWorld(fx);
		}
	}

	public static void cluster(World world, double x, double y, double z, int count, float yaw, float pitch, float yawRand, float pitchRand, float speed) {
		cluster(null, world, x, y, z, count, yaw, pitch, yawRand, pitchRand, speed);
	}

	public static void cluster(UUID ownerParty, World world, double x, double y, double z, int count, float yaw, float pitch, float yawRand, float pitchRand, float speed) {
		if(world == null || world.isRemote || !Integrations.canDetonateWGC(ownerParty, world, (int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z))) return;
		for(int i = 0; i < count; i++) {
			EntityBulletBaseMK4 bullet = new EntityBulletBaseMK4(world, XFactoryCatapult.cluster_submunition, 50F, 0F,
				yaw + (float) (yawRand * world.rand.nextGaussian()),
				pitch + (float) (pitchRand * world.rand.nextGaussian()), ownerParty);
			bullet.setPosition(x, y, z);
			bullet.motionX *= speed;
			bullet.motionY *= speed;
			bullet.motionZ *= speed;
			world.spawnEntityInWorld(bullet);
		}
	}

	public static void poison(World world, double x, double y, double z, double range) { poison(null, world, x, y, z, range); }

	public static void poison(UUID ownerParty, World world, double x, double y, double z, double range) {
		List<EntityLivingBase> affected = world.getEntitiesWithinAABB(EntityLivingBase.class, AxisAlignedBB.getBoundingBox(x - range, y - range, z - range, x + range, y + range, z + range));
		for(EntityLivingBase entity : affected) {
			if(entity instanceof EntityPlayer && !Integrations.canHarmPlayerWGC(ownerParty, entity.getUniqueID(), world)) continue;
			if(entity.getDistance(x, y, z) > range) continue;
			if(ArmorRegistry.hasAnyProtection(entity, 3, HazardClass.GAS_LUNG, HazardClass.GAS_BLISTERING)) ArmorUtil.damageGasMaskFilter(entity, 1);
			else {
				entity.addPotionEffect(new PotionEffect(Potion.blindness.getId(), 5 * 20, 0));
				entity.addPotionEffect(new PotionEffect(Potion.poison.getId(), 20 * 20, 2));
				entity.addPotionEffect(new PotionEffect(Potion.wither.getId(), 1 * 20, 1));
				entity.addPotionEffect(new PotionEffect(Potion.moveSlowdown.getId(), 30 * 20, 1));
				entity.addPotionEffect(new PotionEffect(Potion.digSlowdown.getId(), 30 * 20, 2));
			}
		}
	}

	public static void pc(World world, double x, double y, double z, double range) { pc(null, world, x, y, z, range); }

	public static void pc(UUID ownerParty, World world, double x, double y, double z, double range) {
		List<EntityLivingBase> affected = world.getEntitiesWithinAABB(EntityLivingBase.class, AxisAlignedBB.getBoundingBox(x - range, y - range, z - range, x + range, y + range, z + range));
		for(EntityLivingBase entity : affected) {
			if(entity instanceof EntityPlayer && !Integrations.canHarmPlayerWGC(ownerParty, entity.getUniqueID(), world)) continue;
			if(entity.getDistance(x, y, z) > range) continue;
			for(int slot = 0; slot < 4; slot++) ArmorUtil.damageSuit(entity, slot, 25);
			entity.attackEntityFrom(ModDamageSource.pc, 5);
		}
	}

	public static void c(World world, double x, double y, double z, double range) { c(null, world, x, y, z, range); }

	public static void c(UUID ownerParty, World world, double x, double y, double z, double range) {
		List<EntityLivingBase> affected = world.getEntitiesWithinAABB(EntityLivingBase.class, AxisAlignedBB.getBoundingBox(x - range, y - range, z - range, x + range, y + range, z + range));
		for(EntityLivingBase entity : affected) {
			if(entity instanceof EntityPlayer && !Integrations.canHarmPlayerWGC(ownerParty, entity.getUniqueID(), world)) continue;
			if(entity.getDistance(x, y, z) > range) continue;
			for(int slot = 0; slot < 4; slot++) ArmorUtil.damageSuit(entity, slot, 25);
			if(ArmorUtil.checkForHazmat(entity)) continue;
			if(entity.isPotionActive(HbmPotion.taint.id)) {
				entity.removePotionEffect(HbmPotion.taint.id);
				entity.addPotionEffect(new PotionEffect(HbmPotion.mutation.id, 60 * 60 * 20, 0, false));
			}
			entity.attackEntityFrom(ModDamageSource.cloud, 5);
		}
	}

	public static void floater(World world, int x, int y, int z, int radi, int height) { floater(null, world, x, y, z, radi, height); }

	public static void floater(UUID ownerParty, World world, int x, int y, int z, int radi, int height) {
		int r = radi;
		int r22 = r * r / 2;
		for(int xx = -r; xx < r; xx++) {
			int X = xx + x;
			int XX = xx * xx;
			for(int zz = -r; zz < r; zz++) {
				int Z = zz + z;
				int ZZ = XX + zz * zz;
				for(int yy = -r; yy < r; yy++) {
					int Y = yy + y;
					if(ZZ + yy * yy >= r22) continue;
					if(!Integrations.canExplodeBlockWGC(ownerParty, world, X, Z)) continue;
					Block save = world.getBlock(X, Y, Z);
					if(save == Blocks.air) continue;
					int destY = Y + height;
					if(!Integrations.canTargetBlockWGC(ownerParty, world, X, destY, Z)) continue;
					int meta = world.getBlockMetadata(X, Y, Z);
					world.setBlock(X, Y, Z, Blocks.air);
					world.setBlock(X, destY, Z, save);
					world.setBlockMetadataWithNotify(X, destY, Z, meta, 2);
				}
			}
		}
	}

	public static void move(World world, int x, int y, int z, int radius, int a, int b, int c) { move(null, world, x, y, z, radius, a, b, c); }

	public static void move(UUID ownerParty, World world, int x, int y, int z, int radius, int a, int b, int c) {
		double range = radius;
		List list = world.getEntitiesWithinAABBExcludingEntity(null, AxisAlignedBB.getBoundingBox(x - range - 1, y - range - 1, z - range - 1, x + range + 1, y + range + 1, z + range + 1));
		for(Object object : list) {
			Entity entity = (Entity) object;
			if(entity instanceof EntityPlayer && !Integrations.canHarmPlayerWGC(ownerParty, entity.getUniqueID(), world)) continue;
			if(entity.getDistance(x, y, z) > range) continue;
			if(entity instanceof EntityLiving && !(entity instanceof EntitySheep)) ((EntityLiving) entity).setCustomNameTag(random.nextBoolean() ? "Dinnerbone" : "Grumm");
			if(entity instanceof EntitySheep) ((EntityLiving) entity).setCustomNameTag("jeb_");
			entity.setPosition(entity.posX + a, entity.posY + b, entity.posZ + c);
		}
	}


}
