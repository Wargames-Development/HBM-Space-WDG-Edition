package com.hbm.blocks.bomb;

import java.util.Random;
import java.util.UUID;

import api.hbm.wgc.Integrations;
import com.hbm.blocks.ModBlocks;
import com.hbm.config.BombConfig;
import com.hbm.entity.effect.EntityCloudFleija;
import com.hbm.entity.effect.EntityCloudFleijaRainbow;
import com.hbm.entity.effect.EntityNukeTorex;
import com.hbm.entity.logic.EntityBalefire;
import com.hbm.entity.logic.EntityNukeExplosionMK3;
import com.hbm.entity.logic.EntityNukeExplosionMK5;
import com.hbm.entity.projectile.EntityFallingNuke;
import com.hbm.explosion.ExplosionLarge;
import com.hbm.interfaces.IBomb;
import com.hbm.interfaces.NotableComments;
import com.hbm.main.MainRegistry;
import com.hbm.tileentity.bomb.TileEntityNukeCustom;

import cpw.mods.fml.common.network.internal.FMLNetworkHandler;
import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

// this entire class sucks ass
@NotableComments
public class NukeCustom extends BlockContainer implements IBomb{
	private static boolean keepInventory = false;
	private final static Random field_149933_a = new Random();

	private static final ThreadLocal<UUID> explosionOwnerCache = new ThreadLocal<>();

	public NukeCustom(Material p_i45386_1_) {
		super(p_i45386_1_);
	}

	@Override
	public TileEntity createNewTileEntity(World p_149915_1_, int p_149915_2_) {
		return new TileEntityNukeCustom();
	}

	@Override
	public Item getItemDropped(int p_149650_1_, Random p_149650_2_, int p_149650_3_) {
		return Item.getItemFromBlock(ModBlocks.nuke_custom);
	}

	@Override
	public void breakBlock(World world, int x, int y, int z, Block block, int meta) {
		explosionOwnerCache.set(BlockPartyOwned.getOwner(world, x, y, z));
		if (!keepInventory) {
			TileEntityNukeCustom tileentityfurnace = (TileEntityNukeCustom) world.getTileEntity(x,
					y, z);

			if (tileentityfurnace != null) {
				for (int i1 = 0; i1 < tileentityfurnace.getSizeInventory(); ++i1) {
					ItemStack itemstack = tileentityfurnace.getStackInSlot(i1);

					if (itemstack != null) {
						float f = NukeCustom.field_149933_a.nextFloat() * 0.8F + 0.1F;
						float f1 = NukeCustom.field_149933_a.nextFloat() * 0.8F + 0.1F;
						float f2 = NukeCustom.field_149933_a.nextFloat() * 0.8F + 0.1F;

						while (itemstack.stackSize > 0) {
							int j1 = NukeCustom.field_149933_a.nextInt(21) + 10;

							if (j1 > itemstack.stackSize) {
								j1 = itemstack.stackSize;
							}

							itemstack.stackSize -= j1;
							EntityItem entityitem = new EntityItem(world, x + f, y + f1,
									z + f2,
									new ItemStack(itemstack.getItem(), j1, itemstack.getItemDamage()));

							if (itemstack.hasTagCompound()) {
								entityitem.getEntityItem()
										.setTagCompound((NBTTagCompound) itemstack.getTagCompound().copy());
							}

							float f3 = 0.05F;
							entityitem.motionX = (float) NukeCustom.field_149933_a.nextGaussian() * f3;
							entityitem.motionY = (float) NukeCustom.field_149933_a.nextGaussian() * f3 + 0.2F;
							entityitem.motionZ = (float) NukeCustom.field_149933_a.nextGaussian() * f3;
							world.spawnEntityInWorld(entityitem);
						}
					}
				}

				world.func_147453_f(x, y, z, block);
			}
		}

		super.breakBlock(world, x, y, z, block, meta);
	}

	@Override
	public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX, float hitY, float hitZ) {

		if (world.isRemote) {
			return true;

		} else if (!player.isSneaking()) {

			TileEntityNukeCustom entity = (TileEntityNukeCustom) world.getTileEntity(x, y, z);

			if (entity != null) {
				FMLNetworkHandler.openGui(player, MainRegistry.instance, 0, world, x, y, z);
			}
			return true;

		} else {
			return false;
		}
	}

	@Override
	public void onNeighborBlockChange(World world, int x, int y, int z, Block p_149695_5_) {

		if(world.isBlockIndirectlyGettingPowered(x, y, z)) {
			this.explode(world, x, y, z);
		}
	}

	public static final int maxTnt = 150;
	public static final int maxNuke = 200;
	public static final int maxHydro = 350;
	public static final int maxAmat = 350;
	public static final int maxSchrab = 250;

	// genuinely some of the worst fucking code i've ever written
	public static void explodeCustom(World worldObj, double xCoord, double yCoord, double zCoord, float tnt, float nuke, float hydro, float amat, float dirty, float schrab, float euph) {

		dirty = Math.min(dirty, 100);

		UUID owner = BlockPartyOwned.getOwner(worldObj,(int)Math.floor(xCoord),(int)Math.floor(yCoord),(int)Math.floor(zCoord));
		if(owner == null) {
			owner = explosionOwnerCache.get();
		}

		/// EUPHEMIUM ///
		if(euph > 0) {

			EntityNukeExplosionMK3 ex = new EntityNukeExplosionMK3(worldObj);
			ex.posX = xCoord;
			ex.posY = yCoord;
			ex.posZ = zCoord;
			ex.destructionRange = 150;
			ex.speed = BombConfig.blastSpeed;
			ex.coefficient = 1.0F;
			ex.waste = false;
			worldObj.spawnEntityInWorld(ex);

			worldObj.playSoundEffect(xCoord, yCoord, zCoord, "random.explode", 100000.0F, 1.0F);

			EntityCloudFleijaRainbow cloud = new EntityCloudFleijaRainbow(worldObj, 50);
			cloud.posX = xCoord;
			cloud.posY = yCoord;
			cloud.posZ = zCoord;
			worldObj.spawnEntityInWorld(cloud);

		// SCHRABIDIUM ///
		} else if(schrab > 0) {

			schrab += amat / 2 + hydro / 4 + nuke / 8 + tnt / 16;
			schrab = Math.min(schrab, maxSchrab);

			EntityNukeExplosionMK3 ex = EntityNukeExplosionMK3.statFacFleija(worldObj, xCoord + 0.5, yCoord + 0.5, zCoord + 0.5, (int) schrab);
			if(!ex.isDead) {
				worldObj.spawnEntityInWorld(ex);

				EntityCloudFleija cloud = new EntityCloudFleija(worldObj, (int) schrab);
				cloud.setPosition(xCoord + 0.5, yCoord + 0.5, zCoord + 0.5);
				worldObj.spawnEntityInWorld(cloud);
			}

    	/// ANTIMATTER ///
		} else if(amat > 0) {

			amat += hydro / 2 + nuke / 4 + tnt / 8;
			amat = Math.min(amat, maxAmat);

			EntityBalefire bf = new EntityBalefire(worldObj);
			bf.ownerParty = owner;
			bf.antimatter();
    		bf.setPosition(xCoord + 0.5, yCoord + 0.5, zCoord + 0.5);
			bf.destructionRange = (int) amat;
			worldObj.spawnEntityInWorld(bf);
			EntityNukeTorex.startFacAnti(worldObj, xCoord + 0.5, yCoord + 5, zCoord + 0.5, amat);

		/// HYDROGEN ///
		} else if(hydro > 0) {

			hydro += nuke / 2 + tnt / 4;
			hydro = Math.min(hydro, maxHydro);
			dirty *= 0.25F;

			worldObj.spawnEntityInWorld(EntityNukeExplosionMK5.statFac(worldObj, (int)hydro, xCoord + 0.5, yCoord + 0.5, zCoord + 0.5,owner).moreFallout((int)dirty));
			EntityNukeTorex.statFacStandard(worldObj, xCoord + 0.5, yCoord + 5, zCoord + 0.5, hydro);

		/// NUCLEAR ///
		} else if(nuke > 0) {

			nuke += tnt / 2;
			nuke = Math.min(nuke, maxNuke);

			worldObj.spawnEntityInWorld(EntityNukeExplosionMK5.statFac(worldObj, (int)nuke, xCoord + 0.5, yCoord + 5, zCoord + 0.5,owner).moreFallout((int)dirty));
			EntityNukeTorex.statFacStandard(worldObj, xCoord + 0.5, yCoord + 5, zCoord + 0.5, nuke);

		/// NON-NUCLEAR ///
		} else if(tnt >= 75) {

			tnt = Math.min(tnt, maxTnt);

			worldObj.spawnEntityInWorld(EntityNukeExplosionMK5.statFacNoRad(worldObj, (int)tnt, xCoord + 0.5, yCoord + 0.5, zCoord + 0.5,owner));
			EntityNukeTorex.statFacStandard(worldObj, xCoord + 0.5, yCoord + 5, zCoord + 0.5, tnt);
		} else if(tnt > 0) {

			ExplosionLarge.explode(owner,worldObj, xCoord + 0.5, yCoord + 0.5, zCoord + 0.5, tnt, true, true, true);
		}
	}

	@Override
	public int getRenderType() {
		return -1;
	}

	@Override
	public boolean isOpaqueCube() {
		return false;
	}

	@Override
	public boolean renderAsNormalBlock() {
		return false;
	}

	@Override
	public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase player, ItemStack itemStack) {
		int i = MathHelper.floor_double(player.rotationYaw * 4.0F / 360.0F + 0.5D) & 3;
		BlockPartyOwned.setOwner(world,x,y,z,player.getUniqueID());
		if (i == 0) {
			world.setBlockMetadataWithNotify(x, y, z, 5, 2);
		}
		if (i == 1) {
			world.setBlockMetadataWithNotify(x, y, z, 3, 2);
		}
		if (i == 2) {
			world.setBlockMetadataWithNotify(x, y, z, 4, 2);
		}
		if (i == 3) {
			world.setBlockMetadataWithNotify(x, y, z, 2, 2);
		}
	}

	@Override
	public BombReturnCode explode(World world, int x, int y, int z) {

		if(!world.isRemote & Integrations.canDetonateWGC(BlockPartyOwned.getOwner(world,x,y,z),world,x,y,z)) {
			TileEntityNukeCustom entity = (TileEntityNukeCustom) world.getTileEntity(x, y, z);

			if(!entity.isFalling()) {

				entity.clearSlots();
				world.func_147480_a(x, y, z, false);
				NukeCustom.explodeCustom(world, x + 0.5, y + 0.5, z + 0.5, entity.tnt, entity.nuke, entity.hydro, entity.amat, entity.dirty, entity.schrab, entity.euph);
				return BombReturnCode.DETONATED;

			} else {

				EntityFallingNuke bomb = new EntityFallingNuke(world, entity.tnt, entity.nuke, entity.hydro, entity.amat, entity.dirty, entity.schrab, entity.euph);
				bomb.getDataWatcher().updateObject(20, (byte)world.getBlockMetadata(x, y, z));
				bomb.setPositionAndRotation(x + 0.5, y, z + 0.5, 0, 0);
				entity.clearSlots();
				world.setBlockToAir(x, y, z);
				world.spawnEntityInWorld(bomb);
				return BombReturnCode.TRIGGERED;
			}
		}

		return BombReturnCode.UNDEFINED;
	}
	public UUID getOwnerParty(World world, int x, int y, int z) {
		return BlockPartyOwned.getOwner(world,x,y,z);
	}
}
