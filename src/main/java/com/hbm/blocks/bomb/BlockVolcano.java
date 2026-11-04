package com.hbm.blocks.bomb;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.apache.logging.log4j.Level;

import com.hbm.blocks.IBlockMulti;
import com.hbm.blocks.ITooltipProvider;
import com.hbm.blocks.ModBlocks;
import com.hbm.config.GeneralConfig;
import com.hbm.entity.projectile.EntityShrapnel;
import com.hbm.explosion.ExplosionNT;
import com.hbm.explosion.ExplosionNT.ExAttrib;
import com.hbm.main.MainRegistry;
import com.hbm.packet.toclient.AuxParticlePacketNT;
import com.hbm.handler.threading.PacketThreading;

import cpw.mods.fml.common.network.NetworkRegistry.TargetPoint;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.World;

public class BlockVolcano extends BlockContainer implements ITooltipProvider, IBlockMulti {

	public BlockVolcano() {
		super(Material.iron);
	}

	@Override
	public TileEntity createNewTileEntity(World world, int meta) {
		return new TileEntityVolcanoCore();
	}

	@Override
	public int getSubCount() {
		return 5;
	}

	@SideOnly(Side.CLIENT)
	public void getSubBlocks(Item item, CreativeTabs tab, List list) {

		for(int i = 0; i < 5; ++i) {
			list.add(new ItemStack(item, 1, i));
		}
	}

	@Override
	public void addInformation(ItemStack stack, EntityPlayer player, List list, boolean ext) {

		int meta = stack.getItemDamage();

		if(meta == META_SMOLDERING) {
			list.add(EnumChatFormatting.GOLD + "SHIELD VOLCANO");
			return;
		}

		list.add(BlockVolcano.isGrowing(meta) ? (EnumChatFormatting.RED + "DOES GROW") : (EnumChatFormatting.DARK_GRAY + "DOES NOT GROW"));
		list.add(BlockVolcano.isExtinguishing(meta) ? (EnumChatFormatting.RED + "DOES EXTINGUISH") : (EnumChatFormatting.DARK_GRAY + "DOES NOT EXTINGUISH"));
	}

	public static final int META_STATIC_ACTIVE = 0;
	public static final int META_STATIC_EXTINGUISHING = 1;
	public static final int META_GROWING_ACTIVE = 2;
	public static final int META_GROWING_EXTINGUISHING = 3;
	public static final int META_SMOLDERING = 4;

	public static boolean isGrowing(int meta) {
		return meta == META_GROWING_ACTIVE || meta == META_GROWING_EXTINGUISHING;
	}

	public static boolean isExtinguishing(int meta) {
		return meta == META_STATIC_EXTINGUISHING || meta == META_GROWING_EXTINGUISHING;
	}

	public static class TileEntityVolcanoCore extends TileEntity {

		private static List<ExAttrib> volcanoExplosion = Arrays.asList(new ExAttrib[] {ExAttrib.NODROP, ExAttrib.LAVA_V, ExAttrib.NOSOUND, ExAttrib.ALLMOD, ExAttrib.NOHURT});
		private static List<ExAttrib> volcanoRadExplosion = Arrays.asList(new ExAttrib[] {ExAttrib.NODROP, ExAttrib.LAVA_R, ExAttrib.NOSOUND, ExAttrib.ALLMOD, ExAttrib.NOHURT});

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
        private boolean canAlter(int x, int z) {
            com.hbm.util.NaturalHazardSource source = getNaturalOrigin();
            return source != null ? source.canAffect(worldObj,x,z,"DEBRIS") : api.hbm.wgc.Integrations.canExplodeBlockWGC(null,worldObj,x,z);
        }
		public int volcanoTimer;

		@Override
		public void updateEntity() {

			if(!worldObj.isRemote) {
                if(!canAlter(xCoord,zCoord)) return;
				this.volcanoTimer++;

				if(this.volcanoTimer % 10 == 0) {
					//if that type has a vertical channel, blast it open and raise the magma
					if(this.hasVerticalChannel()) {
						this.blastMagmaChannel();
						this.raiseMagma();
					}

					double magmaChamber = this.magmaChamberSize();
					if(magmaChamber > 0) this.blastMagmaChamber(magmaChamber);

					Object[] melting = this.surfaceMeltingParams();
					if(melting != null) this.meltSurface((int)melting[0], (double)melting[1], (double)melting[2]);

					//self-explanatory
					if(this.isSpewing()) this.spawnBlobs();
					if(this.isSmoking()) this.spawnSmoke();

					//generates a 3x3x3 cube of lava
					this.surroundLava();
				}

				if(this.volcanoTimer >= this.getUpdateRate()) {
					this.volcanoTimer = 0;

					if(this.shouldGrow()) {
						worldObj.setBlock(xCoord, yCoord + 1, zCoord, this.getBlockType(), this.getBlockMetadata(), 3);
                        if(getNaturalOrigin() != null) com.hbm.util.NaturalHazardSource.markGenerated(worldObj,xCoord,yCoord + 1,zCoord);
						worldObj.setBlock(xCoord, yCoord, zCoord, getLava());
						return;
					} else if(this.isExtinguishing()) {
						worldObj.setBlock(xCoord, yCoord, zCoord, getLava());
						return;
					}
				}
			}
		}

		public boolean isRadioacitve() {
			return this.getBlockType() == ModBlocks.volcano_rad_core;
		}

		protected Block getLava() {
			if(isRadioacitve()) return ModBlocks.rad_lava_block;
			return ModBlocks.volcanic_lava_block;
		}

		protected List<ExAttrib> getExpAttrb() {
			return this.isRadioacitve() ? this.volcanoRadExplosion : this.volcanoExplosion;
		}

		@Override
		public void readFromNBT(NBTTagCompound nbt) {
			super.readFromNBT(nbt);
            naturalManifestChecked = false;
            naturalCertificationDisabled = nbt.getBoolean("NaturalOriginDisabled");
			this.volcanoTimer = nbt.getInteger("timer");
            naturalOrigin = nbt.hasKey("NaturalOrigin",10) ? com.hbm.util.NaturalHazardSource.read(nbt.getCompoundTag("NaturalOrigin")) : null;
		}

		@Override
		public void writeToNBT(NBTTagCompound nbt) {
			super.writeToNBT(nbt);
            if(naturalCertificationDisabled) nbt.setBoolean("NaturalOriginDisabled",true);
            else nbt.removeTag("NaturalOriginDisabled");
			nbt.setInteger("timer", this.volcanoTimer);
            if(getNaturalOrigin() != null) nbt.setTag("NaturalOrigin",naturalOrigin.write());
            else nbt.removeTag("NaturalOrigin");
		}

		private boolean shouldGrow() {
			return isGrowing() && yCoord < 200;
		}

		private boolean isGrowing() {
			int meta = this.getBlockMetadata();
			return meta == META_GROWING_ACTIVE || meta == META_GROWING_EXTINGUISHING;
		}

		private boolean isExtinguishing() {
			int meta = this.getBlockMetadata();
			return meta == META_STATIC_EXTINGUISHING || meta == META_GROWING_EXTINGUISHING;
		}

		private boolean isSmoking() {
			return this.getBlockMetadata() != META_SMOLDERING;
		}

		private boolean isSpewing() {
			return this.getBlockMetadata() != META_SMOLDERING;
		}

		private boolean hasVerticalChannel() {
			return this.getBlockMetadata() != META_SMOLDERING;
		}

		private double magmaChamberSize() {
			return this.getBlockMetadata() == META_SMOLDERING ? 15 : 0;
		}

		/* count per tick, radius, depth */
		private Object[] surfaceMeltingParams() {
			return this.getBlockMetadata() == META_SMOLDERING ? new Object[] {50, 50D, 10D} : null;
		}

		private int getUpdateRate() {
			switch(this.getBlockMetadata()) {
			case META_STATIC_EXTINGUISHING: return 60 * 60 * 20; //once per hour
			case META_GROWING_ACTIVE:
			case META_GROWING_EXTINGUISHING: return 60 * 60 * 20 / 250; //250x per hour
			default: return 10;
			}
		}

		/** Causes two magma explosions, one from bedrock to the core and one from the core to 15 blocks above. */
		private void blastMagmaChannel() {
			ExplosionNT explosion = new ExplosionNT(worldObj, null, xCoord + 0.5, yCoord + worldObj.rand.nextInt(15) + 1.5, zCoord + 0.5, 7);
			explosion.setNaturalSource(getNaturalOrigin()).addAllAttrib(getExpAttrb()).explode();
			ExplosionNT explosion2 = new ExplosionNT(worldObj, null, xCoord + 0.5 + worldObj.rand.nextGaussian() * 3, worldObj.rand.nextInt(yCoord + 1), zCoord + 0.5 + worldObj.rand.nextGaussian() * 3, 10);
			explosion2.setNaturalSource(getNaturalOrigin()).addAllAttrib(getExpAttrb()).explode();
		}

		/** Causes two magma explosions at a random position around the core, one at normal and one at half range. */
		private void blastMagmaChamber(double size) {

			for(int i = 0; i < 2; i++) {
				double dist = size / (double) (i + 1);
				ExplosionNT explosion = new ExplosionNT(worldObj, null, xCoord + 0.5 + worldObj.rand.nextGaussian() * dist, yCoord + 0.5 + worldObj.rand.nextGaussian() * dist, zCoord + 0.5 + worldObj.rand.nextGaussian() * dist, 7);
				explosion.setNaturalSource(getNaturalOrigin()).addAllAttrib(getExpAttrb()).explode();
			}
		}

		/** Randomly selects surface blocks and converts them into lava if solid or air if not solid. */
		private void meltSurface(int count, double radius, double depth) {

			for(int i = 0; i < count; i++) {
				int x = (int) Math.floor(xCoord + worldObj.rand.nextGaussian() * radius);
				int z = (int) Math.floor(zCoord + worldObj.rand.nextGaussian() * radius);
				if(!canAlter(x,z) || !worldObj.getChunkProvider().chunkExists(x >> 4,z >> 4)) continue;
                //gaussian distribution makes conversions more likely on the surface and rarer at the bottom
				int y = worldObj.getHeightValue(x, z) + 1 - (int) Math.floor(Math.abs(worldObj.rand.nextGaussian() * depth));

				Block b = worldObj.getBlock(x, y, z);

				if(!b.isAir(worldObj, x, y, z) && b.getExplosionResistance(null) < Blocks.obsidian.getExplosionResistance(null)) {
					//turn into lava if solid block, otherwise just break
					worldObj.setBlock(x, y, z, b.isNormalCube() ? this.getLava() : Blocks.air);
				}
			}
		}

		/** Increases the magma level in a small radius around the core. */
		private void raiseMagma() {

			int rX = xCoord - 10 + worldObj.rand.nextInt(21);
			int rY = yCoord + worldObj.rand.nextInt(11);
			int rZ = zCoord - 10 + worldObj.rand.nextInt(21);

			if(canAlter(rX,rZ) && worldObj.blockExists(rX,rY,rZ) && worldObj.getBlock(rX, rY, rZ) == Blocks.air && worldObj.getBlock(rX, rY - 1, rZ) == this.getLava())
				worldObj.setBlock(rX, rY, rZ, this.getLava());
		}

		/** Creates a 3x3x3 lava sphere around the core. */
		private void surroundLava() {

			for(int i = -1; i <= 1; i++) {
				for(int j = -1; j <= 1; j++) {
					for(int k = -1; k <= 1; k++) {

						if((i != 0 || j != 0 || k != 0) && canAlter(xCoord + i,zCoord + k) && worldObj.blockExists(xCoord + i,yCoord + j,zCoord + k)) {
							worldObj.setBlock(xCoord + i, yCoord + j, zCoord + k, this.getLava());
						}
					}
				}
			}
		}

		/** Spews specially tagged shrapnels which create volcanic lava and monoxide clouds. */
		private void spawnBlobs() {

			for(int i = 0; i < 3; i++) {
				EntityShrapnel frag = new EntityShrapnel(worldObj);
				frag.setLocationAndAngles(xCoord + 0.5, yCoord + 1.5, zCoord + 0.5, 0.0F, 0.0F);
				frag.owner = null;
                frag.naturalSource = getNaturalOrigin();
				frag.motionY = 1D + worldObj.rand.nextDouble();
				frag.motionX = worldObj.rand.nextGaussian() * 0.2D;
				frag.motionZ = worldObj.rand.nextGaussian() * 0.2D;
				if(this.isRadioacitve()) {
					frag.setRadVolcano(true);
				} else {
					frag.setVolcano(true);
				}
				worldObj.spawnEntityInWorld(frag);
			}
		}

		/** I SEE SMOKE, AND WHERE THERE'S SMOKE THERE'S FIRE! */
		private void spawnSmoke() {
			NBTTagCompound dPart = new NBTTagCompound();
			dPart.setString("type", "vanillaExt");
			dPart.setString("mode", "volcano");
			PacketThreading.createAllAroundThreadedPacket(new AuxParticlePacketNT(dPart, xCoord + 0.5, yCoord + 10, zCoord + 0.5), new TargetPoint(worldObj.provider.dimensionId, xCoord + 0.5, yCoord + 10, zCoord + 0.5, 250));
		}
	}

	@Override
	public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase player, ItemStack itemStack) {
		if(!world.isRemote) {
				if(GeneralConfig.enableExtendedLogging) {
				MainRegistry.logger.log(Level.INFO, "[BOMBPL]" + this.getLocalizedName() + " placed at " + x + " / " + y + " / " + z + "! " + "by "+ player.getCommandSenderName());
			}
		}
	}
}
