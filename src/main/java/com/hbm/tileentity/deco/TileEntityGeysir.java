package com.hbm.tileentity.deco;

import java.util.Random;

import com.hbm.blocks.ModBlocks;
import com.hbm.entity.particle.EntityOrangeFX;
import com.hbm.entity.projectile.EntityShrapnel;
import com.hbm.handler.threading.PacketThreading;
import com.hbm.packet.toclient.AuxParticlePacketNT;

import cpw.mods.fml.common.network.NetworkRegistry.TargetPoint;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;

public class TileEntityGeysir extends TileEntity {

    private com.hbm.util.NaturalHazardSource naturalOrigin;
        private boolean naturalManifestChecked;
        private boolean naturalCertificationDisabled;
    public void setNaturalOrigin(com.hbm.util.NaturalHazardSource source) { naturalOrigin = source; naturalCertificationDisabled = source == null; markDirty(); }
    public com.hbm.util.NaturalHazardSource getNaturalOrigin() {
            if(!naturalManifestChecked && worldObj != null && !worldObj.isRemote) {
                naturalManifestChecked = true;
                if(!naturalCertificationDisabled && naturalOrigin == null) com.hbm.util.NaturalHazardCertification.certifyListed(this);
            }
        return naturalOrigin != null && naturalOrigin.isAt(worldObj,xCoord,yCoord,zCoord) ? naturalOrigin : null;
    }
    @Override public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        if(naturalCertificationDisabled) tag.setBoolean("NaturalOriginDisabled",true);
        else tag.removeTag("NaturalOriginDisabled");
        if(getNaturalOrigin() != null) tag.setTag("NaturalOrigin",naturalOrigin.write());
        else tag.removeTag("NaturalOrigin");
    }
    @Override public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        naturalManifestChecked = false;
        naturalCertificationDisabled = tag.getBoolean("NaturalOriginDisabled");
        naturalOrigin = tag.hasKey("NaturalOrigin",10) ? com.hbm.util.NaturalHazardSource.read(tag.getCompoundTag("NaturalOrigin")) : null;
    }

	int timer;

	@Override
	public void updateEntity() {
        getNaturalOrigin();

		if (!this.worldObj.isRemote && worldObj.getBlock(xCoord, yCoord + 1, zCoord) == Blocks.air) {

			timer--;

			int meta = worldObj.getBlockMetadata(xCoord, yCoord, zCoord);

			if(timer <= 0) {
				timer = getDelay();

				if(meta == 0)
					worldObj.setBlockMetadataWithNotify(xCoord, yCoord, zCoord, 1, 2);
				else
					worldObj.setBlockMetadataWithNotify(xCoord, yCoord, zCoord, 0, 2);
			}

			if(meta == 1) {
				perform();
			}
		}
	}

	private void chlorine() {

		for(int i = 0; i < 3; i++) {
			EntityOrangeFX fx = new EntityOrangeFX(null,worldObj, xCoord + 0.5, yCoord + 1.5, zCoord + 0.5, 0.0, 0.0, 0.0);

			fx.naturalSource = getNaturalOrigin();

			fx.motionX = worldObj.rand.nextGaussian() * 0.45;
			fx.motionZ = worldObj.rand.nextGaussian() * 0.45;
			fx.motionY = timer * 0.3;

			worldObj.spawnEntityInWorld(fx);
		}

	}

	private void fire() {

		int range = 32;
		if(worldObj.getEntitiesWithinAABB(EntityPlayer.class, AxisAlignedBB.getBoundingBox(xCoord + 0.5, yCoord + 0.5, zCoord + 0.5, xCoord + 0.5, yCoord + 0.5, zCoord + 0.5).expand(range, range, range)).isEmpty())
			return;

		if(worldObj.rand.nextInt(3) == 0) {
			EntityShrapnel fx = new EntityShrapnel(worldObj,xCoord + 0.5, yCoord + 1.5, zCoord + 0.5);
			fx.naturalSource = getNaturalOrigin();

			fx.motionX = worldObj.rand.nextGaussian() * 0.05;
			fx.motionZ = worldObj.rand.nextGaussian() * 0.05;
			fx.motionY = 0.5 + worldObj.rand.nextDouble() * timer * 0.01;
			fx.owner = null;

			worldObj.spawnEntityInWorld(fx);
		}

		if(timer % 2 == 0) {
			NBTTagCompound data = new NBTTagCompound();
			data.setString("type", "gasfire");
			data.setDouble("mX", worldObj.rand.nextGaussian() * 0.05);
			data.setDouble("mY", 0.2);
			data.setDouble("mZ", worldObj.rand.nextGaussian() * 0.05);
			PacketThreading.createAllAroundThreadedPacket(new AuxParticlePacketNT(data, this.xCoord + 0.5F, this.yCoord + 1.1F, this.zCoord + 0.5F), new TargetPoint(worldObj.provider.dimensionId, xCoord, yCoord, zCoord, 75));
		}
	}

	private int getDelay() {

		Block b = worldObj.getBlock(xCoord, yCoord, zCoord);
		int meta = worldObj.getBlockMetadata(xCoord, yCoord, zCoord);
		Random rand = worldObj.rand;

		if(b == ModBlocks.geysir_chlorine) {
			return (meta == 0 ? 20 : 400 + rand.nextInt(100));
		} else if(b == ModBlocks.geysir_nether) {
			return (meta == 0 ? (rand.nextBoolean() ? 300 : 450) : 80 + rand.nextInt(60));
		}

		return 0;
	}

	private void perform() {

		Block b = worldObj.getBlock(xCoord, yCoord, zCoord);

		if(b == ModBlocks.geysir_chlorine) chlorine();
		else if(b == ModBlocks.geysir_nether) fire();
	}

}
