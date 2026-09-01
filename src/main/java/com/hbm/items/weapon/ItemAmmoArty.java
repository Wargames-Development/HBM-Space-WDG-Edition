package com.hbm.items.weapon;

import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import com.hbm.blocks.ModBlocks;
import com.hbm.config.BombConfig;
import com.hbm.entity.effect.EntityMist;
import com.hbm.entity.effect.EntityNukeTorex;
import com.hbm.entity.logic.EntityNukeExplosionMK5;
import com.hbm.entity.projectile.EntityArtilleryShell;
import api.hbm.wgc.Integrations;
import com.hbm.explosion.ExplosionChaos;
import com.hbm.explosion.ExplosionLarge;
import com.hbm.explosion.ExplosionNukeSmall;
import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.interfaces.ICustomDamageHandler;
import com.hbm.explosion.vanillant.standard.BlockAllocatorStandard;
import com.hbm.explosion.vanillant.standard.BlockMutatorDebris;
import com.hbm.explosion.vanillant.standard.BlockProcessorStandard;
import com.hbm.explosion.vanillant.standard.EntityProcessorCross;
import com.hbm.explosion.vanillant.standard.PlayerProcessorStandard;
import com.hbm.handler.pollution.PollutionHandler;
import com.hbm.handler.pollution.PollutionHandler.PollutionType;
import com.hbm.handler.threading.PacketThreading;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.lib.RefStrings;
import com.hbm.main.MainRegistry;
import com.hbm.packet.toclient.AuxParticlePacketNT;
import com.hbm.particle.SpentCasing;
import com.hbm.particle.SpentCasing.CasingType;
import com.hbm.particle.helper.ExplosionCreator;
import com.hbm.potion.HbmPotion;

import cpw.mods.fml.common.network.NetworkRegistry.TargetPoint;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.init.Blocks;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IIcon;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;

public class ItemAmmoArty extends Item {

	public static Random rand = new Random();
	public static ArtilleryShell[] itemTypes =	new ArtilleryShell[ /* >>> */ 10 /* <<< */ ];
	/* item types */
	public final int NORMAL = 0;
	public final int AIRBURST = 1;
	public final int GUIDED = 2;
	public final int MINI_NUKE = 3;
	public final int NUKE = 4;
	public final int PHOSPHORUS = 5;
	public final int EMPTY = 6;
	public final int CHLORINE = 7;
	public final int PHOSGENE = 8;
	public final int MUSTARD = 9;
	/* non-item shell types */

	public ItemAmmoArty() {
		this.setHasSubtypes(true);
		this.setCreativeTab(MainRegistry.weaponTab);
		init();
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void getSubItems(Item item, CreativeTabs tab, List list) {
		list.add(new ItemStack(item, 1, NORMAL));
		list.add(new ItemStack(item, 1, AIRBURST));
		list.add(new ItemStack(item, 1, GUIDED));
		list.add(new ItemStack(item, 1, MINI_NUKE));
		list.add(new ItemStack(item, 1, NUKE));
		list.add(new ItemStack(item, 1, PHOSPHORUS));
		list.add(new ItemStack(item, 1, EMPTY));
		list.add(new ItemStack(item, 1, CHLORINE));
		list.add(new ItemStack(item, 1, PHOSGENE));
		list.add(new ItemStack(item, 1, MUSTARD));
	}

	@Override
	public void addInformation(ItemStack stack, EntityPlayer player, List list, boolean bool) {

		String r = EnumChatFormatting.RED + "";
		String y = EnumChatFormatting.YELLOW + "";
		String b = EnumChatFormatting.BLUE + "";

		switch(stack.getItemDamage()) {
		case NORMAL:
			list.add(y + "Standard Shell");
			list.add(y + "Highly innacurate");
			list.add(y + "Destroys blocks");
			break;
		case AIRBURST:
			list.add(y + "Airburst Shell");
			list.add(y + "Increased Player Damage");
			list.add(y + "Highly innacurate");
			list.add(b + "Only destroys foliage");
			break;
		case GUIDED:
			list.add(y + "GPS Guided Shell");
			list.add(y + "Pinpoint accuracy");
			list.add(b + "Destroys blocks");
			break;
		case PHOSPHORUS:
			list.add(y + "Phosphorus splash");
			list.add(y + "Highly innacurate");
			list.add(b + "Does not destroy blocks");
			break;
		case MINI_NUKE:
			list.add(y + "Strength: 20");
			list.add(y + "Highly innacurate");
			list.add(r + "Destroys blocks");
			break;
		case NUKE:
			list.add(r + "☠");
			list.add(r + "(that is the best skull and crossbones");
			list.add(r + "minecraft's unicode has to offer)");
			list.add(y + "Highly innacurate");
			break;
		case EMPTY:
			list.add(b + "Casing without the payload");
			list.add(b + "Doubles as a cargo container");
			list.add(y + "10 block accuracy");
			if(stack.hasTagCompound() && stack.stackTagCompound.getCompoundTag("cargo") != null) {
				ItemStack cargo = ItemStack.loadItemStackFromNBT(stack.stackTagCompound.getCompoundTag("cargo"));
				list.add(y + cargo.getDisplayName());
			} else {
				list.add(r + "Empty");
			}
			break;
		}
	}

	private IIcon[] icons = new IIcon[itemTypes.length];
	private IIcon iconCargo;

	@SideOnly(Side.CLIENT)
	public void registerIcons(IIconRegister reg) {

		this.icons = new IIcon[itemTypes.length];

		for(int i = 0; i < icons.length; i++) {
			this.icons[i] = reg.registerIcon(RefStrings.MODID + ":" + itemTypes[i].name);
		}

		this.iconCargo = reg.registerIcon(RefStrings.MODID + ":ammo_arty_cargo_full");
	}

	@SideOnly(Side.CLIENT)
	public IIcon getIconIndex(ItemStack stack) {

		if(stack.getItemDamage() == EMPTY && stack.hasTagCompound() && stack.stackTagCompound.getCompoundTag("cargo") != null) {
			return this.iconCargo;
		}

		return this.getIconFromDamage(stack.getItemDamage());
	}

	@Override
	@SideOnly(Side.CLIENT)
	public IIcon getIconFromDamage(int meta) {
		return this.icons[meta];
	}

	@Override
	public String getUnlocalizedName(ItemStack stack) {
		return "item." + itemTypes[Math.abs(stack.getItemDamage()) % itemTypes.length].name;
	}

	protected static SpentCasing SIXTEEN_INCH_CASE = new SpentCasing(CasingType.STRAIGHT).setScale(15F, 15F, 10F).setupSmoke(1F, 1D, 200, 60).setMaxAge(300).setBounceMotion(1F, 0.5F);

	public abstract class ArtilleryShell {

		String name;
		public SpentCasing casing;
		public double inaccuracy;

		public ArtilleryShell(String name, int casingColor, double inaccuracy) {
			this.name = name;
			this.inaccuracy = inaccuracy;//in radians
			this.casing = SIXTEEN_INCH_CASE.clone().register(name).setColor(casingColor);
		}

		public abstract void onImpact(EntityArtilleryShell shell, MovingObjectPosition mop);
		public void onUpdate(EntityArtilleryShell shell) { }
	}

	public static void standardExplosion(EntityArtilleryShell shell, MovingObjectPosition mop, float size, float rangeMod, boolean breaksBlocks, int stripRadius, double offset) {
		Vec3 vec = Vec3.createVectorHelper(shell.motionX, shell.motionY, shell.motionZ).normalize();
		double explosionX = mop.hitVec.xCoord - vec.xCoord;
		double explosionY = mop.hitVec.yCoord - vec.yCoord + offset;
		double explosionZ = mop.hitVec.zCoord - vec.zCoord;
		ExplosionVNT xnt = new ExplosionVNT(shell.worldObj, explosionX, explosionY, explosionZ, size, shell.getOwnerParty());
		if (stripRadius > 0) {
			stripTerrain(shell.worldObj, shell.getOwnerParty(), explosionX, explosionY, explosionZ, stripRadius);
		}
		if(breaksBlocks) {
			xnt.setBlockAllocator(new BlockAllocatorStandard(48));
			xnt.setBlockProcessor(new BlockProcessorStandard().setNoDrop().withBlockEffect(new BlockMutatorDebris(ModBlocks.block_slag, 1)));
		}
		xnt.setEntityProcessor(new EntityProcessorCross(7.5D)
			.withRangeMod(rangeMod)
			.withDamageMod(new ICustomDamageHandler() { //custom handler which copies the default damage behavior with a multipler of 4.0F
				@Override
				public void handleAttack(ExplosionVNT explosion, Entity entity, double distanceScaled) {
					if(entity == null || !entity.isEntityAlive() || distanceScaled > 1.0D) return;

					double knockback = 1.0D - distanceScaled;
					float baseDamage = (float) ((int) (((knockback * knockback + knockback) / 2.0D) * 8.0D * explosion.size + 1.0D));
					entity.attackEntityFrom(EntityProcessorCross.setExplosionSource(explosion.compat), baseDamage * 4.0F);
				}
			}));
		xnt.setPlayerProcessor(new PlayerProcessorStandard());
		//xnt.setSFX(new ExplosionEffectStandard());
		xnt.explode();
		shell.killAndClear();
	}

	private static void stripTerrain(World world, UUID ownerParty, double centerX, double centerY, double centerZ, float radius) {
		if(world.isRemote) return;

		int centerBlockX = (int) Math.floor(centerX);
		int centerBlockY = (int) Math.floor(centerY);
		int centerBlockZ = (int) Math.floor(centerZ);
		int blockRadius = (int) Math.ceil(radius);
		double radiusSquared = radius * radius;
		float falloffRadius = Math.min(4F, radius * 0.35F);
		double innerRadius = Math.max(0D, radius - falloffRadius);
		double innerRadiusSquared = innerRadius * innerRadius;
		Set<ChunkCoordIntPair> protectedChunks = Integrations.getExplosionProtectedChunksWGC(ownerParty, world, centerBlockX, centerBlockZ, blockRadius + 16);
		int minX = centerBlockX - blockRadius;
		int maxX = centerBlockX + blockRadius;
		int minZ = centerBlockZ - blockRadius;
		int maxZ = centerBlockZ + blockRadius;

		for(int chunkX = minX >> 4; chunkX <= maxX >> 4; chunkX++) {
			int chunkMinX = Math.max(minX, chunkX << 4);
			int chunkMaxX = Math.min(maxX, (chunkX << 4) + 15);
			for(int chunkZ = minZ >> 4; chunkZ <= maxZ >> 4; chunkZ++) {
				if(protectedChunks.contains(new ChunkCoordIntPair(chunkX, chunkZ))) continue;

				int chunkMinZ = Math.max(minZ, chunkZ << 4);
				int chunkMaxZ = Math.min(maxZ, (chunkZ << 4) + 15);
				for(int x = chunkMinX; x <= chunkMaxX; x++) {
					double offsetX = x - centerX;
					for(int z = chunkMinZ; z <= chunkMaxZ; z++) {
						double offsetZ = z - centerZ;
						double horizontalSquared = offsetX * offsetX + offsetZ * offsetZ;
						if(horizontalSquared > radiusSquared) continue;
						if(horizontalSquared > innerRadiusSquared) {
							double distance = Math.sqrt(horizontalSquared);
							double effectChance = (radius - distance) / falloffRadius;
							if(rand.nextDouble() > effectChance) continue;
						}

						int verticalRadius = (int) Math.sqrt(radiusSquared - horizontalSquared);
						int minY = Math.max(0, centerBlockY - verticalRadius);
						int maxY = Math.min(255, centerBlockY + verticalRadius);
						int lastBlockY = Math.min(maxY, world.getHeightValue(x, z) - 1);
						for(int offset = 0; offset <= verticalRadius; offset++) {
							int aboveY = lastBlockY + offset;
							if(aboveY >= minY && aboveY <= maxY && stripTerrainBlock(world, x, aboveY, z)) break;

							if(offset == 0) continue;
							int belowY = lastBlockY - offset;
							if(belowY >= minY && belowY <= maxY && stripTerrainBlock(world, x, belowY, z)) break;
						}
					}
				}
			}
		}
	}

	private static boolean stripTerrainBlock(World world, int x, int y, int z) {
		Block block = world.getBlock(x, y, z);
		if(block.isLeaves(world, x, y, z)) {
			world.setBlockToAir(x, y, z);
		} else if(block == Blocks.grass) {
			if(rand.nextInt(2) == 0) {
				world.setBlock(x, y, z, ModBlocks.dirt_dead, 0, 3);
			} else {
				world.setBlock(x, y, z, Blocks.dirt, 0, 3);
			}
			return true;
		}
		return false;
	}

	public static void standardCluster(EntityArtilleryShell shell, int clusterType, int amount, double splitHeight, double deviation) {
		if(!shell.getWhistle() || shell.motionY > 0) return;
		if(shell.getTargetHeight() + splitHeight < shell.posY) return;

		shell.killAndClear();

		NBTTagCompound data = new NBTTagCompound();
		data.setString("type", "plasmablast");
		data.setFloat("r", 1.0F);
		data.setFloat("g", 1.0F);
		data.setFloat("b", 1.0F);
		data.setFloat("scale", 50F);
		PacketThreading.createAllAroundThreadedPacket(new AuxParticlePacketNT(data, shell.posX, shell.posY, shell.posZ),
				new TargetPoint(shell.dimension, shell.posX, shell.posY, shell.posZ, 500));

		for(int i = 0; i < amount; i++) {
			EntityArtilleryShell cluster = new EntityArtilleryShell(shell.worldObj);
			cluster.setType(clusterType);
			cluster.motionX = i == 0 ? shell.motionX : (shell.motionX + rand.nextGaussian() * deviation);
			cluster.motionY = shell.motionY;
			cluster.motionZ = i == 0 ? shell.motionZ : (shell.motionZ + rand.nextGaussian() * deviation);
			cluster.setPositionAndRotation(shell.posX, shell.posY, shell.posZ, shell.rotationYaw, shell.rotationPitch);
			double[] target = shell.getTarget();
			cluster.setTarget(target[0], target[1], target[2]);
			cluster.setWhistle(shell.getWhistle() && !shell.didWhistle());
			shell.worldObj.spawnEntityInWorld(cluster);
		}
	}

	private void init() {
		/* STANDARD SHELLS */
		this.itemTypes[NORMAL] = new ArtilleryShell("ammo_arty", SpentCasing.COLOR_CASE_16INCH, 0.08) { public void onImpact(EntityArtilleryShell shell, MovingObjectPosition mop) { standardExplosion(shell, mop, 4F, 1.6F, false, 0, 0.0); ExplosionCreator.composeEffect(shell.worldObj, mop.hitVec.xCoord, mop.hitVec.yCoord, mop.hitVec.zCoord, 10, 2F, 0.5F, 25F, 5, 0, 20, 0.75F, 1F, -2F, 150); }}; //temp set to not deal block dmg or strip
		this.itemTypes[AIRBURST] = new ArtilleryShell("ammo_arty_airburst", SpentCasing.COLOR_CASE_16INCH, 0.0) { public void onImpact(EntityArtilleryShell shell, MovingObjectPosition mop) { standardExplosion(shell, mop, 4F, 2.0F, false, 14, 6.0); ExplosionCreator.composeEffect(shell.worldObj, mop.hitVec.xCoord, mop.hitVec.yCoord + 20, mop.hitVec.zCoord, 10, 2F, 0.5F, 25F, 5, 0, 20, 0.75F, 1F, -2F, 150); }};
		this.itemTypes[GUIDED] = new ArtilleryShell("ammo_arty_guided", SpentCasing.COLOR_CASE_16INCH, 0.02) { public void onImpact(EntityArtilleryShell shell, MovingObjectPosition mop) { standardExplosion(shell, mop, 4F, 1.6F, true, 10, 0.0); ExplosionCreator.composeEffect(shell.worldObj, mop.hitVec.xCoord, mop.hitVec.yCoord, mop.hitVec.zCoord, 10, 2F, 0.5F, 25F, 5, 0, 20, 0.75F, 1F, -2F, 150); }};

		/* MINI NUKE */
		this.itemTypes[MINI_NUKE] = new ArtilleryShell("ammo_arty_mini_nuke", SpentCasing.COLOR_CASE_16INCH_NUKE, 0) {
			public void onImpact(EntityArtilleryShell shell, MovingObjectPosition mop) {
				shell.killAndClear();
				Vec3 vec = Vec3.createVectorHelper(shell.motionX, shell.motionY, shell.motionZ).normalize();
				ExplosionNukeSmall.explode(shell.worldObj, mop.hitVec.xCoord - vec.xCoord, mop.hitVec.yCoord - vec.yCoord, mop.hitVec.zCoord - vec.zCoord, shell.getOwnerParty(),ExplosionNukeSmall.PARAMS_MEDIUM);
			}
		};

		/* FULL NUKE */
		this.itemTypes[NUKE] = new ArtilleryShell("ammo_arty_nuke", SpentCasing.COLOR_CASE_16INCH_NUKE, 0) {
			public void onImpact(EntityArtilleryShell shell, MovingObjectPosition mop) {
				shell.worldObj.spawnEntityInWorld(EntityNukeExplosionMK5.statFac(shell.worldObj, BombConfig.missileRadius, mop.hitVec.xCoord, mop.hitVec.yCoord, mop.hitVec.zCoord,shell.getOwnerParty()));
				EntityNukeTorex.statFacStandard(shell.worldObj, mop.hitVec.xCoord, mop.hitVec.yCoord, mop.hitVec.zCoord, BombConfig.missileRadius);
				shell.setDead();
			}
		};

		/* PHOSPHORUS */
		this.itemTypes[PHOSPHORUS] = new ArtilleryShell("ammo_arty_phosphorus", SpentCasing.COLOR_CASE_16INCH_PHOS, 0.1) {
			public void onImpact(EntityArtilleryShell shell, MovingObjectPosition mop) {
				shell.worldObj.playSoundEffect(shell.posX, shell.posY, shell.posZ, "hbm:weapon.explosionMedium", 20.0F, 0.9F + rand.nextFloat() * 0.2F);
				//shell.worldObj.playSoundEffect(shell.posX, shell.posY, shell.posZ, "hbm:weapon.explosionMedium", 20.0F, 0.9F + shell.worldObj.rand.nextFloat() * 0.2F);
				ExplosionLarge.spawnShrapnels(shell.worldObj, (int) mop.hitVec.xCoord, (int) mop.hitVec.yCoord, (int) mop.hitVec.zCoord, 15,shell.getOwnerParty());
				ExplosionChaos.igniteSomeBlocks(shell.getOwnerParty(),shell.worldObj, (int) mop.hitVec.xCoord, (int) mop.hitVec.yCoord, (int) mop.hitVec.zCoord, 16, 0.2D);
				int radius = 15;
				List<Entity> hit = shell.worldObj.getEntitiesWithinAABBExcludingEntity(shell, AxisAlignedBB.getBoundingBox(shell.posX - radius, shell.posY - radius, shell.posZ - radius, shell.posX + radius, shell.posY + radius, shell.posZ + radius));
				for(Entity e : hit) {
					e.setFire(5);
					if(e instanceof EntityLivingBase) {
						PotionEffect eff = new PotionEffect(HbmPotion.phosphorus.id, 15 * 20, 0, true);
						eff.getCurativeItems().clear();
						((EntityLivingBase)e).addPotionEffect(eff);
					}
				}
				for(int i = 0; i < 5; i++) {
					NBTTagCompound haze = new NBTTagCompound();
					haze.setString("type", "haze");
					PacketThreading.createAllAroundThreadedPacket(new AuxParticlePacketNT(haze, mop.hitVec.xCoord + shell.worldObj.rand.nextGaussian() * 10, mop.hitVec.yCoord, mop.hitVec.zCoord + shell.worldObj.rand.nextGaussian() * 10), new TargetPoint(shell.dimension, shell.posX, shell.posY, shell.posZ, 150));
				}
				NBTTagCompound data = new NBTTagCompound();
				data.setString("type", "rbmkmush");
				data.setFloat("scale", 10);
				PacketThreading.createAllAroundThreadedPacket(new AuxParticlePacketNT(data, mop.hitVec.xCoord, mop.hitVec.yCoord, mop.hitVec.zCoord), new TargetPoint(shell.dimension, shell.posX, shell.posY, shell.posZ, 250));
			}
		};

		/* THIS DOOFUS */
		this.itemTypes[EMPTY] = new ArtilleryShell("ammo_arty_empty", SpentCasing.COLOR_CASE_16INCH, 10) { public void onImpact(EntityArtilleryShell shell, MovingObjectPosition mop) {
			if(mop.typeOfHit == MovingObjectType.BLOCK) {
				shell.setPosition(mop.hitVec.xCoord, mop.hitVec.yCoord, mop.hitVec.zCoord);
				shell.getStuck(mop.blockX, mop.blockY, mop.blockZ, mop.sideHit);
			}
		}};

		/* GAS */
		this.itemTypes[CHLORINE] = new ArtilleryShell("ammo_arty_chlorine", SpentCasing.COLOR_CASE_16INCH, 0.1) {
			public void onImpact(EntityArtilleryShell shell, MovingObjectPosition mop) {
				shell.killAndClear();
				Vec3 vec = Vec3.createVectorHelper(shell.motionX, shell.motionY, shell.motionZ).normalize();
				shell.worldObj.createExplosion(shell, mop.hitVec.xCoord - vec.xCoord, mop.hitVec.yCoord - vec.yCoord, mop.hitVec.zCoord - vec.zCoord, 5F, false);
				EntityMist mist = new EntityMist(shell.worldObj,shell.getOwnerParty());
				mist.setType(Fluids.CHLORINE);
				mist.maxAge = 600;
				mist.setPosition(mop.hitVec.xCoord - vec.xCoord, mop.hitVec.yCoord - vec.yCoord - 3, mop.hitVec.zCoord - vec.zCoord);
				mist.setArea(20F, 10F);
				shell.worldObj.spawnEntityInWorld(mist);
			}
		};
		this.itemTypes[PHOSGENE] = new ArtilleryShell("ammo_arty_phosgene", SpentCasing.COLOR_CASE_16INCH_NUKE, 0.1) {
			public void onImpact(EntityArtilleryShell shell, MovingObjectPosition mop) {
				shell.killAndClear();
				Vec3 vec = Vec3.createVectorHelper(shell.motionX, shell.motionY, shell.motionZ).normalize();
				shell.worldObj.createExplosion(shell, mop.hitVec.xCoord - vec.xCoord, mop.hitVec.yCoord - vec.yCoord, mop.hitVec.zCoord - vec.zCoord, 5F, false);
				for(int i = 0; i < 2; i++) {
					EntityMist mist = new EntityMist(shell.worldObj,shell.getOwnerParty());
					mist.setType(Fluids.PHOSGENE);
					double x = mop.hitVec.xCoord - vec.xCoord;
					double z = mop.hitVec.zCoord - vec.zCoord;
					if(i > 0) {
						x += rand.nextGaussian() * 15;
						z += rand.nextGaussian() * 15;
					}
					mist.maxAge = 900;
					mist.setPosition(x, mop.hitVec.yCoord - vec.yCoord - 5, z);
					mist.setArea(20F, 10F);
					shell.worldObj.spawnEntityInWorld(mist);
				}
			}
		};
		this.itemTypes[MUSTARD] = new ArtilleryShell("ammo_arty_mustard_gas", SpentCasing.COLOR_CASE_16INCH_NUKE, 0.1) {
			public void onImpact(EntityArtilleryShell shell, MovingObjectPosition mop) {
				shell.killAndClear();
				Vec3 vec = Vec3.createVectorHelper(shell.motionX, shell.motionY, shell.motionZ).normalize();
				shell.worldObj.createExplosion(shell, mop.hitVec.xCoord - vec.xCoord, mop.hitVec.yCoord - vec.yCoord, mop.hitVec.zCoord - vec.zCoord, 5F, false);
				for(int i = 0; i < 4; i++) {
					EntityMist mist = new EntityMist(shell.worldObj,shell.getOwnerParty());
					mist.setType(Fluids.MUSTARDGAS);
					double x = mop.hitVec.xCoord - vec.xCoord;
					double z = mop.hitVec.zCoord - vec.zCoord;
					if(i > 0) {
						x += rand.nextGaussian() * 25;
						z += rand.nextGaussian() * 25;
					}
					mist.maxAge = 1200;
					mist.setPosition(x, mop.hitVec.yCoord - vec.yCoord - 5, z);
					mist.setArea(20F, 10F);
					shell.worldObj.spawnEntityInWorld(mist);
				}
			}
		};

		/* CLUSTER SHELLS */
	}
}
