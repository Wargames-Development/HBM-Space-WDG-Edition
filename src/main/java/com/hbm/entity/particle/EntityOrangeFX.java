package com.hbm.entity.particle;

import api.hbm.wgc.Integrations;
import com.hbm.explosion.ExplosionChaos;
import com.hbm.explosion.ExplosionNukeGeneric;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

import java.util.UUID;

public class EntityOrangeFX extends EntityModFX {

	public EntityOrangeFX(World world) {
		super(null,world, 0, 0, 0);
	}

	public EntityOrangeFX(UUID owner, World p_i1225_1_, double p_i1225_2_, double p_i1225_4_, double p_i1225_6_, double p_i1225_8_, double p_i1225_10_, double p_i1225_12_) {
		this(owner,p_i1225_1_, p_i1225_2_, p_i1225_4_, p_i1225_6_, p_i1225_8_, p_i1225_10_, p_i1225_12_, 1.0F);
	}

	public EntityOrangeFX(UUID owner, World p_i1226_1_, double p_i1226_2_, double p_i1226_4_, double p_i1226_6_, double p_i1226_8_, double p_i1226_10_, double p_i1226_12_, float p_i1226_14_) {
		super(owner, p_i1226_1_, p_i1226_2_, p_i1226_4_, p_i1226_6_, 0.0D, 0.0D, 0.0D);
		this.motionX *= 0.1D;
		this.motionY *= 0.1D;
		this.motionZ *= 0.1D;
		this.motionX += p_i1226_8_;
		this.motionY += p_i1226_10_;
		this.motionZ += p_i1226_12_;
		this.particleRed = this.particleGreen = this.particleBlue = (float) (Math.random() * 0.3D);
		this.particleScale *= 0.75F;
		this.particleScale *= p_i1226_14_;
		this.smokeParticleScale = this.particleScale;
		this.noClip = false;
	}

	@Override
	public void onUpdate() {

		this.prevPosX = this.posX;
		this.prevPosY = this.posY;
		this.prevPosZ = this.posZ;

		if(maxAge < 900) {
			maxAge = rand.nextInt(301) + 900;
		}

		this.particleAge++;

		if(this.particleAge >= maxAge) {
			this.setDead();
			return;
		}

		if(!worldObj.isRemote && rand.nextInt(50) == 0)
			ExplosionChaos.poison(naturalSource, owner, worldObj, posX, posY, posZ, 2);

		this.motionX *= 0.86D;
		this.motionY *= 0.86D;
		this.motionZ *= 0.86D;

		this.motionY -= 0.1;

		double subdivisions = 4;

		for(int i = 0; i < subdivisions; i++) {

			this.posX += this.motionX / subdivisions;
			this.posY += this.motionY / subdivisions;
			this.posZ += this.motionZ / subdivisions;

			int x = MathHelper.floor_double(posX);
			int y = MathHelper.floor_double(posY);
			int z = MathHelper.floor_double(posZ);

			if(!worldObj.blockExists(x, y, z)) {
				this.setDead();
				return;
			}

			if(worldObj.getBlock(x, y, z).getMaterial() != Material.air) {
				this.setDead();
				// WGCore reads server territory data; clients only simulate the particle.
				if(worldObj.isRemote) return;

				for(int a = -1; a < 2; a++) {
					for(int c = -1; c < 2; c++) {
						if(!(owner == null && naturalSource != null ? naturalSource.canAffect(worldObj,x + a,z + c,"CONTAMINATION") : Integrations.canContaminateBlockWGC(owner, worldObj, x + a, z + c))){continue;}
						for(int b = -1; b < 2; b++) {
							if(!worldObj.blockExists(x + a, y + b, z + c)) continue;
							Block bl = worldObj.getBlock(x + a, y + b, z + c);

							if(bl == Blocks.grass) {
								worldObj.setBlock(x + a, y + b, z + c, Blocks.dirt, 1, 3);
							} else {
								ExplosionNukeGeneric.solinium(worldObj, x + a, y + b, z + c);
							}
						}
					}
				}
				return;
			}
		}
	}

	@Override
	public boolean writeToNBTOptional(NBTTagCompound nbt) {
		return false;
	}

	@Override
	public void readEntityFromNBT(NBTTagCompound nbt) {
		super.readEntityFromNBT(nbt);
		this.setDead();
	}
}
