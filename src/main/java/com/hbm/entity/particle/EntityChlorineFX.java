package com.hbm.entity.particle;

import com.hbm.explosion.ExplosionChaos;

import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

import java.util.UUID;

public class EntityChlorineFX extends EntityModFX {

	public EntityChlorineFX(World world) {
		super(null, world, 0, 0, 0);
	}

	public EntityChlorineFX(UUID owner,World world,  double posX, double posY, double posZ,
							double velX, double velY, double velZ) {
		this(owner,world, posX, posY, posZ, velX, velY, velZ, 1.0F);
	}

	public EntityChlorineFX(UUID owner,World world, double posX, double posY, double posZ,
			double velX, double velY, double velZ, float scale) {
		super(owner,world, posX, posY, posZ, 0.0D, 0.0D, 0.0D);
		this.motionX *= 0.10000000149011612D;
		this.motionY *= 0.10000000149011612D;
		this.motionZ *= 0.10000000149011612D;
		this.motionX += velX;
		this.motionY += velY;
		this.motionZ += velZ;
		this.particleRed = this.particleGreen = this.particleBlue = (float) (Math.random() * 0.30000001192092896D);
		this.particleScale *= 0.75F;
		this.particleScale *= scale;
		this.smokeParticleScale = this.particleScale;
		// this.particleMaxAge = (int)(8.0D / (Math.random() * 0.8D + 0.2D));
		// this.particleMaxAge = (int)((float)this.particleMaxAge *
		// scale);
		this.noClip = false;
	}

	/**
	 * Called to update the entity's position/logic.
	 */

	@Override
	public void onUpdate() {

		this.prevPosX = this.posX;
		this.prevPosY = this.posY;
		this.prevPosZ = this.posZ;

		if (maxAge < 700) {
			maxAge = rand.nextInt(101) + 700;
		}

		if (rand.nextInt(50) == 0)
			ExplosionChaos.poison(owner,worldObj, (int) posX, (int) posY, (int) posZ, 2);

		this.particleAge++;

		if (this.particleAge >= maxAge) {
			this.setDead();
		}

		this.motionX *= 0.7599999785423279D;
		this.motionY *= 0.7599999785423279D;
		this.motionZ *= 0.7599999785423279D;

		if (this.onGround) {
			this.motionX *= 0.699999988079071D;
			this.motionZ *= 0.699999988079071D;
		}

		if(worldObj.isRaining() && worldObj.canBlockSeeTheSky(MathHelper.floor_double(this.posX), MathHelper.floor_double(this.posY), MathHelper.floor_double(this.posZ))) {
			this.motionY -= 0.01;
		}

		double subdivisions = 4;

		for(int i = 0; i < subdivisions; i++) {

			this.posX += this.motionX/subdivisions;
			this.posY += this.motionY/subdivisions;
			this.posZ += this.motionZ/subdivisions;

			if (worldObj.getBlock((int) posX, (int) posY, (int) posZ).isNormalCube()) {

				if(rand.nextInt(5) != 0)
					this.setDead();

				this.posX -= this.motionX/subdivisions;
				this.posY -= this.motionY/subdivisions;
				this.posZ -= this.motionZ/subdivisions;

				this.motionX = 0;
				this.motionY = 0;
				this.motionZ = 0;
			}
		}
	}
}
