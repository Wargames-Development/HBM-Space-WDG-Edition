package com.hbm.explosion;

import java.util.HashSet;
import java.util.List;

import api.hbm.explosion.event.HbmExplosionHooks;
import com.hbm.blocks.ModBlocks;
import com.hbm.util.ArmorUtil;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.passive.EntityOcelot;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

public class ExplosionThermo {

	public static void freeze(World world, int x, int y, int z, int bombStartStrength) {
		int r = bombStartStrength * 2;

		// Global veto: cancel the entire freeze effect if origin is protected
		if (HbmExplosionHooks.pre(world, x + 0.5, y + 0.5, z + 0.5, r, null, "THERMO.FREEZE")) return;

		int r2 = r * r;
		int r22 = r2 / 2;
		for (int xx = -r; xx < r; xx++) {
			int X = xx + x;
			int XX = xx * xx;
			for (int yy = -r; yy < r; yy++) {
				int Y = yy + y;
				int YY = XX + yy * yy;
				for (int zz = -r; zz < r; zz++) {
					int Z = zz + z;
					int ZZ = YY + zz * zz;
					if (ZZ < r22 + world.rand.nextInt(r22 / 2)) {

						// Per-block veto: skip freezing inside protected coords
						if (HbmExplosionHooks.blockDenied(world, X, Y, Z, "THERMO.FREEZE")) continue;

						freezeDest(world, X, Y, Z);
					}
				}
			}
		}
	}

	public static void snow(World world, int x, int y, int z, int bound) {

		int r = bound;

		// Global veto: cancel the whole snow effect if origin is protected
		if (HbmExplosionHooks.pre(world, x + 0.5, y + 0.5, z + 0.5, r, null, "THERMO.SNOW")) return;

		int r2 = r * r;
		int r22 = r2 / 2;
		for (int xx = -r; xx < r; xx++) {
			int X = xx + x;
			int XX = xx * xx;
			for (int yy = -r; yy < r; yy++) {
				int Y = yy + y;
				int YY = XX + yy * yy;
				for (int zz = -r; zz < r; zz++) {
					int Z = zz + z;
					int ZZ = YY + zz * zz;
					if (ZZ < r22) {
						// Only modify the block at (X, Y+1, Z); guard that coordinate
						if (HbmExplosionHooks.blockDenied(world, X, Y + 1, Z, "THERMO.SNOW.PLACE"))
							continue;

						if (Blocks.snow_layer.canPlaceBlockAt(world, X, Y + 1, Z)
							&& (world.getBlock(X, Y + 1, Z) == Blocks.air || world.getBlock(X, Y + 1, Z) == Blocks.fire)) {
							world.setBlock(X, Y + 1, Z, Blocks.snow_layer);
						}
					}
				}
			}
		}
	}


	public static void scorch(World world, int x, int y, int z, int bombStartStrength) {
		int r = bombStartStrength * 2;

		// Global veto: cancel entire scorch effect if origin is protected
		if (HbmExplosionHooks.pre(world, x + 0.5, y + 0.5, z + 0.5, r, null, "THERMO.SCORCH")) return;

		int r2 = r * r;
		int r22 = r2 / 2;
		for (int xx = -r; xx < r; xx++) {
			int X = xx + x;
			int XX = xx * xx;
			for (int yy = -r; yy < r; yy++) {
				int Y = yy + y;
				int YY = XX + yy * yy;
				for (int zz = -r; zz < r; zz++) {
					int Z = zz + z;
					int ZZ = YY + zz * zz;
					if (ZZ < r22 + world.rand.nextInt(r22 / 2)) {

						// Per-block veto: skip scorch changes inside protected coords
						if (HbmExplosionHooks.blockDenied(world, X, Y, Z, "THERMO.SCORCH")) continue;

						scorchDest(world, X, Y, Z);
					}
				}
			}
		}
	}


	public static void scorchLight(World world, int x, int y, int z, int bombStartStrength) {
		int r = bombStartStrength * 2;

		// Global veto: cancel entire scorch-light pass if origin is protected
		if (HbmExplosionHooks.pre(world, x + 0.5, y + 0.5, z + 0.5, r, null, "THERMO.SCORCH_LIGHT")) return;

		int r2 = r * r;
		int r22 = r2 / 2;
		for (int xx = -r; xx < r; xx++) {
			int X = xx + x;
			int XX = xx * xx;
			for (int yy = -r; yy < r; yy++) {
				int Y = yy + y;
				int YY = XX + yy * yy;
				for (int zz = -r; zz < r; zz++) {
					int Z = zz + z;
					int ZZ = YY + zz * zz;
					if (ZZ < r22 + world.rand.nextInt(r22 / 2)) {

						// Per-block veto: skip edits in protected coords
						if (HbmExplosionHooks.blockDenied(world, X, Y, Z, "THERMO.SCORCH_LIGHT")) continue;

						scorchDestLight(world, X, Y, Z);
					}
				}
			}
		}
	}


	public static void freezeDest(World world, int x, int y, int z) {
		// Safezone/claim guard — skip any edits at protected coords
		if (HbmExplosionHooks.blockDenied(world, x, y, z, "THERMO.FREEZE")) return;

		Block block = world.getBlock(x, y, z);

		if(block == ModBlocks.volcanic_lava_block)
		{
			world.setBlock(x, y, z, Blocks.cobblestone);
		}

		if(block == Blocks.grass)
		{
			world.setBlock(x, y, z, ModBlocks.frozen_grass);
		}

		if(block == Blocks.dirt)
		{
			world.setBlock(x, y, z, ModBlocks.frozen_dirt);
		}

		if(block == Blocks.log)
		{
			world.setBlock(x, y, z, ModBlocks.frozen_log);
		}

		if(block == Blocks.log2)
		{
			world.setBlock(x, y, z, ModBlocks.frozen_log);
		}

		if(block == Blocks.planks)
		{
			world.setBlock(x, y, z, ModBlocks.frozen_planks);
		}

		if(block == ModBlocks.waste_log)
		{
			world.setBlock(x, y, z, ModBlocks.frozen_log);
		}

		if(block == ModBlocks.waste_planks)
		{
			world.setBlock(x, y, z, ModBlocks.frozen_planks);
		}

		if(block == Blocks.stone)
		{
			world.setBlock(x, y, z, Blocks.packed_ice);
		}

		if(block == Blocks.cobblestone)
		{
			world.setBlock(x, y, z, Blocks.packed_ice);
		}

		if(block == Blocks.stonebrick)
		{
			world.setBlock(x, y, z, Blocks.packed_ice);
		}

		if(block == Blocks.leaves)
		{
			world.setBlock(x, y, z, Blocks.snow);
		}

		if(block == Blocks.leaves2)
		{
			world.setBlock(x, y, z, Blocks.snow);
		}

		if(block == Blocks.lava)
		{
			world.setBlock(x, y, z, Blocks.obsidian);
		}

		if(block == Blocks.flowing_lava)
		{
			world.setBlock(x, y, z, Blocks.obsidian);
		}

		if(block == Blocks.water)
		{
			world.setBlock(x, y, z, Blocks.ice);
		}

		if(block == Blocks.flowing_water)
		{
			world.setBlock(x, y, z, Blocks.ice);
		}
	}

	public static void scorchDest(World world, int x, int y, int z) {
		// Safezone/claim guard — skip edits at protected coords
		if (HbmExplosionHooks.blockDenied(world, x, y, z, "THERMO.SCORCH")) return;

		Block block = world.getBlock(x, y, z);

		if(block == Blocks.grass)
		{
			world.setBlock(x, y, z, Blocks.dirt);
		}

		if(block == ModBlocks.frozen_grass)
		{
			world.setBlock(x, y, z, Blocks.dirt);
		}

		if(block == Blocks.dirt)
		{
			world.setBlock(x, y, z, Blocks.netherrack);
		}

		if(block == ModBlocks.frozen_dirt)
		{
			world.setBlock(x, y, z, Blocks.dirt);
		}

		if(block == Blocks.netherrack)
		{
			world.setBlock(x, y, z, Blocks.flowing_lava);
		}

		if(block == Blocks.log)
		{
			world.setBlock(x, y, z, ModBlocks.waste_log);
		}

		if(block == Blocks.log2)
		{
			world.setBlock(x, y, z, ModBlocks.waste_log);
		}

		if(block == ModBlocks.frozen_log)
		{
			world.setBlock(x, y, z, ModBlocks.waste_log);
		}

		if(block == ModBlocks.frozen_planks)
		{
			world.setBlock(x, y, z, ModBlocks.waste_planks);
		}

		if(block == Blocks.planks)
		{
			world.setBlock(x, y, z, ModBlocks.waste_planks);
		}

		if(block == Blocks.stone)
		{
			world.setBlock(x, y, z, Blocks.flowing_lava);
		}

		if(block == Blocks.cobblestone)
		{
			world.setBlock(x, y, z, Blocks.flowing_lava);
		}

		if(block == Blocks.stonebrick)
		{
			world.setBlock(x, y, z, Blocks.flowing_lava);
		}

		if(block == Blocks.obsidian)
		{
			world.setBlock(x, y, z, Blocks.flowing_lava);
		}

		if(block == Blocks.leaves)
		{
			world.setBlock(x, y, z, Blocks.air);
		}

		if(block == Blocks.leaves2)
		{
			world.setBlock(x, y, z, Blocks.air);
		}

		if(block == Blocks.water)
		{
			world.setBlock(x, y, z, Blocks.air);
		}

		if(block == Blocks.flowing_water)
		{
			world.setBlock(x, y, z, Blocks.air);
		}

		if(block == Blocks.packed_ice)
		{
			world.setBlock(x, y, z, Blocks.flowing_water);
		}

		if(block == Blocks.ice)
		{
			world.setBlock(x, y, z, Blocks.air);
		}
	}

	public static void scorchDestLight(World world, int x, int y, int z) {
		// Safezone/claim guard — skip edits at protected coords
		if (HbmExplosionHooks.blockDenied(world, x, y, z, "THERMO.SCORCH_LIGHT")) return;

		Block block = world.getBlock(x, y, z);

		if(block == Blocks.grass)
		{
			world.setBlock(x, y, z, Blocks.dirt);
		}

		if(block == ModBlocks.frozen_grass)
		{
			world.setBlock(x, y, z, Blocks.dirt);
		}

		if(block == Blocks.dirt)
		{
			world.setBlock(x, y, z, Blocks.netherrack);
		}

		if(block == ModBlocks.frozen_dirt)
		{
			world.setBlock(x, y, z, Blocks.dirt);
		}

		if(block == ModBlocks.waste_earth)
		{
			world.setBlock(x, y, z, Blocks.netherrack);
		}

		if(block == Blocks.log)
		{
			world.setBlock(x, y, z, ModBlocks.waste_log);
		}

		if(block == Blocks.log2)
		{
			world.setBlock(x, y, z, ModBlocks.waste_log);
		}

		if(block == ModBlocks.frozen_log)
		{
			world.setBlock(x, y, z, ModBlocks.waste_log);
		}

		if(block == ModBlocks.frozen_planks)
		{
			world.setBlock(x, y, z, ModBlocks.waste_planks);
		}

		if(block == Blocks.planks)
		{
			world.setBlock(x, y, z, ModBlocks.waste_planks);
		}

		if(block == Blocks.obsidian)
		{
			world.setBlock(x, y, z, ModBlocks.gravel_obsidian);
		}

		if(block == Blocks.leaves)
		{
			world.setBlock(x, y, z, Blocks.air);
		}

		if(block == Blocks.leaves2)
		{
			world.setBlock(x, y, z, Blocks.air);
		}

		if(block == Blocks.water)
		{
			world.setBlock(x, y, z, Blocks.air);
		}

		if(block == Blocks.flowing_water)
		{
			world.setBlock(x, y, z, Blocks.air);
		}

		if(block == Blocks.packed_ice)
		{
			world.setBlock(x, y, z, Blocks.flowing_water);
		}

		if(block == Blocks.ice)
		{
			world.setBlock(x, y, z, Blocks.air);
		}

		if(block == Blocks.sand)
		{
			world.setBlock(x, y, z, Blocks.glass);
		}

		if(block == Blocks.clay)
		{
			world.setBlock(x, y, z, Blocks.stained_hardened_clay);
			world.setBlockMetadataWithNotify(x, y, z, world.rand.nextInt(16), 3);
		}
	}

	public static void freezer(World world, int x, int y, int z, int bombStartStrength) {
		// Global veto: cancel whole effect if origin is protected
		if (HbmExplosionHooks.pre(world, x + 0.5, y + 0.5, z + 0.5, bombStartStrength, null, "THERMO.FREEZER.ORIGIN")) return;

		float f = bombStartStrength;
		new HashSet();
		int i;
		int j;
		int k;
		double d5;
		double d6;
		double d7;
		double wat = bombStartStrength;
		bombStartStrength *= 2.0F;
		i = MathHelper.floor_double(x - wat - 1.0D);
		j = MathHelper.floor_double(x + wat + 1.0D);
		k = MathHelper.floor_double(y - wat - 1.0D);
		int i2 = MathHelper.floor_double(y + wat + 1.0D);
		int l = MathHelper.floor_double(z - wat - 1.0D);
		int j2 = MathHelper.floor_double(z + wat + 1.0D);
		List list = world.getEntitiesWithinAABBExcludingEntity(null, AxisAlignedBB.getBoundingBox(i, k, l, j, i2, j2));
		Vec3.createVectorHelper(x, y, z);

		for (int i1 = 0; i1 < list.size(); ++i1)
		{
			Entity entity = (Entity)list.get(i1);
			double d4 = entity.getDistance(x, y, z) / bombStartStrength;

			if (d4 <= 1.0D)
			{
				d5 = entity.posX - x;
				d6 = entity.posY + entity.getEyeHeight() - y;
				d7 = entity.posZ - z;
				double d9 = MathHelper.sqrt_double(d5 * d5 + d6 * d6 + d7 * d7);
				if (d9 < wat && !(entity instanceof EntityOcelot) && entity instanceof EntityLivingBase)
				{
					// Per-entity veto: skip effects in protected zones
					if (HbmExplosionHooks.pre(world, entity.posX, entity.posY, entity.posZ, 0F, entity, "THERMO.FREEZER.EFFECT")) continue;

					for(int a = (int) entity.posX - 2; a < (int) entity.posX + 1; a++)
					{
						for(int b = (int) entity.posY; b < (int) entity.posY + 3; b++)
						{
							for(int c = (int) entity.posZ - 1; c < (int) entity.posZ + 2; c++)
							{
								// Per-block veto: don’t place ice in protected coords
								if (HbmExplosionHooks.blockDenied(world, a, b, c, "THERMO.FREEZER.CAGE")) continue;
								world.setBlock(a, b, c, Blocks.ice);
							}
						}
					}

					((EntityLivingBase) entity).addPotionEffect(new PotionEffect(Potion.weakness.getId(), 2 * 60 * 20, 4));
					((EntityLivingBase) entity).addPotionEffect(new PotionEffect(Potion.moveSlowdown.getId(), 90 * 20, 2));
					((EntityLivingBase) entity).addPotionEffect(new PotionEffect(Potion.digSlowdown.getId(), 3 * 60 * 20, 2));
				}
			}
		}

		bombStartStrength = (int)f;
	}


	public static void setEntitiesOnFire(World world, double x, double y, double z, int radius) {

		// Global veto: cancel the whole effect if the origin is protected
		if (HbmExplosionHooks.pre(world, x, y, z, radius, null, "THERMO.FIRE.ORIGIN")) return;

		List<Entity> list = world.getEntitiesWithinAABBExcludingEntity(
			null,
			AxisAlignedBB.getBoundingBox(x - radius, y - radius, z - radius, x + radius, y + radius, z + radius)
		);

		for (Entity e : list) {
			if (e.getDistance(x, y, z) <= radius) {

				// Per-entity veto: skip harmful effects in protected zones
				if (HbmExplosionHooks.pre(world, e.posX, e.posY, e.posZ, 0F, e, "THERMO.FIRE.HIT")) continue;

				if (!(e instanceof EntityPlayer && ArmorUtil.checkForAsbestos((EntityPlayer) e))) {
					if (e instanceof EntityLivingBase)
						((EntityLivingBase) e).addPotionEffect(new PotionEffect(Potion.weakness.getId(), 15 * 20, 4));

					e.setFire(10);
				}
			}
		}
	}


}
