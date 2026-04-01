package com.hbm.entity.projectile;

import api.hbm.wgc.Integrations;
import com.hbm.blocks.ModBlocks;
import com.hbm.explosion.ExplosionNT;
import com.hbm.explosion.ExplosionNT.ExAttrib;
import com.hbm.lib.ModDamageSource;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityThrowable;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

import java.util.UUID;

public class EntityShrapnel extends EntityThrowable {
	public UUID owner;
	public EntityShrapnel(World world) {
		super(world);
		this.isImmuneToFire = true;
	}

	public EntityShrapnel(World world, EntityLivingBase entity) {
		super(world, entity);
	}

	@Override
	public void entityInit() {
		this.dataWatcher.addObject(16, Byte.valueOf((byte) 0));
	}

	public EntityShrapnel(World world, double posX, double posY, double posZ) {
		super(world, posX, posY, posZ);
	}

	@Override
	public void onUpdate() {
		super.onUpdate();

		if(worldObj.isRemote && this.dataWatcher.getWatchableObjectByte(16) == 1)
			worldObj.spawnParticle("flame", posX, posY, posZ, 0.0, 0.0, 0.0);
	}

	@Override
	protected void onImpact(MovingObjectPosition mop) {
		System.out.println("Impact! Hit at tick count: " + this.ticksExisted);
		if(mop.entityHit != null) {
			byte b0 = 15;
			if(!(mop.entityHit instanceof EntityPlayer && !Integrations.canHarmPlayerWGC(owner,mop.entityHit.getUniqueID(),worldObj))) {
				mop.entityHit.attackEntityFrom(ModDamageSource.shrapnel, b0);
			}
		}
		String canexplodeit = Integrations.canExplodeBlockWGC(owner,worldObj,mop.blockX,mop.blockZ) ? "Yes!" : "No.";
		System.out.println("Can I explode the block? " + canexplodeit);
		if(this.ticksExisted > 5 && Integrations.canExplodeBlockWGC(owner,worldObj,mop.blockX,mop.blockZ)) {

			if(!worldObj.isRemote)
				this.setDead();

			int b = this.dataWatcher.getWatchableObjectByte(16);
			if(b == 2 || b == 4) {

				if(!worldObj.isRemote) {
					if(motionY < -0.2D) {

						if(worldObj.getBlock(mop.blockX, mop.blockY + 1, mop.blockZ).isReplaceable(worldObj, mop.blockX, mop.blockY + 1, mop.blockZ))
							worldObj.setBlock(mop.blockX, mop.blockY + 1, mop.blockZ, b == 2 ? ModBlocks.volcanic_lava_block : ModBlocks.rad_lava_block);

						for(int x = mop.blockX - 1; x <= mop.blockX + 1; x++) {
							for(int y = mop.blockY; y <= mop.blockY + 2; y++) {
								for(int z = mop.blockZ - 1; z <= mop.blockZ + 1; z++) {
									if(worldObj.getBlock(x, y, z) == Blocks.air)
										worldObj.setBlock(x, y, z, ModBlocks.gas_monoxide);
								}
							}
						}
					}

					if(motionY > 0) {
						ExplosionNT explosion = new ExplosionNT(worldObj, null, mop.blockX + 0.5, mop.blockY + 0.5, mop.blockZ + 0.5, 7);
						explosion.addAttrib(ExAttrib.NODROP);
						explosion.addAttrib(b == 2 ? ExAttrib.LAVA_V : ExAttrib.LAVA_R);
						explosion.addAttrib(ExAttrib.NOSOUND);
						explosion.addAttrib(ExAttrib.ALLMOD);
						explosion.addAttrib(ExAttrib.NOHURT);
						explosion.explode();
					}
				}

			} else if(this.dataWatcher.getWatchableObjectByte(16) == 3) {

				if(worldObj.getBlock(mop.blockX, mop.blockY + 1, mop.blockZ).isReplaceable(worldObj, mop.blockX, mop.blockY + 1, mop.blockZ)) {
					worldObj.setBlock(mop.blockX, mop.blockY + 1, mop.blockZ, ModBlocks.mud_block);
				}

			} else {

				for(int i = 0; i < 5; i++) worldObj.spawnParticle("lava", posX, posY, posZ, 0.0, 0.0, 0.0);
			}

			worldObj.playSoundEffect(posX, posY, posZ, "random.fizz", 1.0F, 1.0F);
		}
	}

	public void setTrail(boolean b) {
		this.dataWatcher.updateObject(16, (byte) (b ? 1 : 0));
	}

	public void setVolcano(boolean b) {
		this.dataWatcher.updateObject(16, (byte) (b ? 2 : 0));
	}

	public void setWatz(boolean b) {
		this.dataWatcher.updateObject(16, (byte) (b ? 3 : 0));
	}

	public void setRadVolcano(boolean b) {
		this.dataWatcher.updateObject(16, (byte) (b ? 4 : 0));
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
