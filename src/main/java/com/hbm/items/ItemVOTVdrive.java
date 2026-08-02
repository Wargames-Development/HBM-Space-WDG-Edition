package com.hbm.items;

import java.util.List;

import com.hbm.config.SpaceConfig;
import com.hbm.dim.CelestialBody;
import com.hbm.dim.SolarSystem;
import com.hbm.dim.SolarSystemWorldSavedData;
import com.hbm.dim.orbit.OrbitalStation;
import com.hbm.entity.missile.EntityRideableRocket;
import com.hbm.entity.missile.EntityRideableRocket.RocketState;
import com.hbm.lib.RefStrings;
import com.hbm.util.i18n.I18nUtil;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IIcon;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;

public class ItemVOTVdrive extends ItemEnumMulti {

	private static final String TAG_STATION_DELETED = "hbmStationDeleted";
	public static final String TAG_STATION_KEY = "hbmStationKey";
	public static final String TAG_STATION_GENERATION = "hbmStationGeneration";
	public static final String TAG_STATION_DRIVE_TYPE = "hbmStationDriveType";
	public static final String DRIVE_TYPE_NORMAL = "normal";
	public static final String DRIVE_TYPE_RAID = "raid";

	private IIcon[] overlays;

	public ItemVOTVdrive() {
		super(SolarSystem.Body.class, false, false);
		this.setMaxStackSize(1);
		this.canRepair = false;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public void addInformation(ItemStack stack, EntityPlayer player, List list, boolean bool) {
		super.addInformation(stack, player, list, bool);

		Destination destination = getDestination(stack);
		if(destination == null || destination.body == null) {
			list.add(EnumChatFormatting.RED + "Station no longer exists");
			return;
		}

		if(destination.body == SolarSystem.Body.ORBIT) {
			String identifier = stack.stackTagCompound.getString("stationName");

			if(identifier.equals("")) identifier = "0x" + Integer.toHexString(new ChunkCoordIntPair(destination.x, destination.z).hashCode()).toUpperCase();

			list.add("Destination: ORBITAL STATION");
			list.add("Station: " + identifier);

			if(player.worldObj.provider.dimensionId != destination.body.getDimensionId()) {
				for(String s : I18nUtil.resolveKey("item.hard_drive_full.orbit.desc").split("\\$")) {
					list.add(EnumChatFormatting.GOLD + s);
				}
			}

			return;
		}

		int processingLevel = destination.body.getProcessingLevel(CelestialBody.getBody(player.worldObj));

		list.add("Destination: " + EnumChatFormatting.AQUA + I18nUtil.resolveKey("body." + destination.body.name));

		if(destination.x == 0 && destination.z == 0) {
			list.add(EnumChatFormatting.GOLD + "Needs destination coordinates!");
		} else if(!getProcessed(stack)) {
			// Display processing level info if not processed
			list.add("Process requirement: Level " + processingLevel);
			list.add(EnumChatFormatting.GOLD + "Needs processing!");
			list.add("Target coordinates: " + destination.x + ", " + destination.z);
		} else {
			// Display destination info if processed
			list.add(EnumChatFormatting.GREEN + "Processed!");
			list.add("Target coordinates: " + destination.x + ", " + destination.z);
		}

		if(player.worldObj.provider.dimensionId == destination.body.getDimensionId()) {
			for(String s : I18nUtil.resolveKey("item.hard_drive_full.surface.desc").split("\\$")) {
				list.add(EnumChatFormatting.GOLD + s);
			}
		}
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void registerIcons(IIconRegister iconRegister) {
		overlays = new IIcon[SolarSystem.Body.values().length];

		for(int i = 0; i < overlays.length; i++) {
			SolarSystem.Body body = SolarSystem.Body.values()[i];
			String name = body != SolarSystem.Body.ORBIT ? body.name : "orbit";
			overlays[i] = iconRegister.registerIcon(RefStrings.MODID + ":votv." + name);
		}

		itemIcon = iconRegister.registerIcon(RefStrings.MODID + ":votv_f"); // Base icon for unprocessed drives
	}

	@Override
	@SideOnly(Side.CLIENT)
	public boolean requiresMultipleRenderPasses() {
		return true;
	}

	@Override
	public int getRenderPasses(int metadata) {
		return 2;
	}

	@SideOnly(Side.CLIENT)
	public IIcon getIconFromDamageForRenderPass(int meta, int pass) {
		if(pass == 0) return this.getIconFromDamage(meta);
		return this.overlays[meta];
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	@SideOnly(Side.CLIENT)
	public void getSubItems(Item item, CreativeTabs tab, List list) {
		NBTTagCompound stackTag = new NBTTagCompound();
		stackTag.setInteger("x", 1);
		stackTag.setInteger("ax", 1);
		stackTag.setBoolean("Processed", true);
		for(int i = 0; i < theEnum.getEnumConstants().length; i++) {
			if(SolarSystem.Body.values()[i] == SolarSystem.Body.ORBIT) continue;
			ItemStack stack = new ItemStack(item, 1, i);
			stack.stackTagCompound = stackTag;
			list.add(stack);
		}
	}

	public static SolarSystem.Body getBody(ItemStack stack) {
		return SolarSystem.Body.values()[stack.getItemDamage() % SolarSystem.Body.values().length];
	}

	/** Returns true only for normal drives and currently valid programmed raid drives. */
	public static boolean isUsableDrive(ItemStack stack) {
		if(stack == null || !(stack.getItem() instanceof ItemVOTVdrive)) return false;
		if(stack.getItem() == ModItems.raid_drive) return ItemRaidDrive.validate(stack);
		return true;
	}

	public static Destination getDestination(ItemStack stack) {
		if(!isUsableDrive(stack)) return null;
		return getDestinationUnchecked(stack);
	}

	/** Reads destination NBT without causing recursive raid-drive validation. */
	public static Destination getDestinationUnchecked(ItemStack stack) {
		if(stack == null || !(stack.getItem() instanceof ItemVOTVdrive)) return null;
		if(!stack.hasTagCompound()) stack.stackTagCompound = new NBTTagCompound();
		SolarSystem.Body body = getBody(stack);
		int x = stack.stackTagCompound.getInteger("x");
		int z = stack.stackTagCompound.getInteger("z");
		if(body == SolarSystem.Body.ORBIT && stack.stackTagCompound.getBoolean(TAG_STATION_DELETED)) return null;
		return new Destination(body, x, z);
	}

	public static boolean isRaidStationDrive(ItemStack stack) {
		return ItemRaidDrive.isRaidDrive(stack) || (stack != null && stack.hasTagCompound() && DRIVE_TYPE_RAID.equals(stack.stackTagCompound.getString(TAG_STATION_DRIVE_TYPE)));
	}

	public static boolean isNormalStationDrive(ItemStack stack) {
		if(stack == null || stack.getItem() != ModItems.full_drive || getBody(stack) != SolarSystem.Body.ORBIT || !stack.hasTagCompound()) return false;
		String type = stack.stackTagCompound.getString(TAG_STATION_DRIVE_TYPE);
		return type.isEmpty() || DRIVE_TYPE_NORMAL.equals(type);
	}

	/** Converts a deleted normal station drive into the mod's ordinary empty Hard Drive. */
	public static boolean resetToEmptyHardDrive(ItemStack stack) {
		if(!isNormalStationDrive(stack)) return false;
		int size = Math.max(1, Math.min(stack.stackSize, ModItems.hard_drive.getItemStackLimit()));
		stack.func_150996_a(ModItems.hard_drive);
		stack.setItemDamage(0);
		stack.stackTagCompound = null;
		stack.stackSize = size;
		return true;
	}

	/** Used by bounded loaded-inventory sweeps when a specific station deletion completes. */
	public static boolean resetIfMatchesStation(ItemStack stack, int stationX, int stationZ, String stationKey, int generation) {
		if(!isNormalStationDrive(stack)) return false;
		Destination destination = getDestinationUnchecked(stack);
		if(destination == null || destination.body != SolarSystem.Body.ORBIT || destination.x != stationX || destination.z != stationZ) return false;

		NBTTagCompound tag = stack.stackTagCompound;
		int driveGeneration = tag.hasKey(TAG_STATION_GENERATION) ? tag.getInteger(TAG_STATION_GENERATION) : 0;
		if(driveGeneration != generation) return false;

		String driveKey = tag.getString(TAG_STATION_KEY);
		String expectedKey = stationKey == null ? "" : stationKey;
		if(driveKey.isEmpty()) {
			if(!expectedKey.isEmpty() && !expectedKey.startsWith("legacy:")) return false;
		} else if(!driveKey.equals(expectedKey)) {
			return false;
		}

		return resetToEmptyHardDrive(stack);
	}

	/**
	 * Server-authoritative normal station-drive validation. A drive is unusable as
	 * soon as deletion is queued, but it is converted only after the station's
	 * persisted generation proves that authoritative deletion has completed.
	 */
	public static boolean validateNormalStationDrive(ItemStack stack, World world) {
		if(!isNormalStationDrive(stack)) return false;
		if(world == null || world.isRemote) return true;

		Destination destination = getDestinationUnchecked(stack);
		if(destination == null || destination.body != SolarSystem.Body.ORBIT) return false;
		SolarSystemWorldSavedData data = SolarSystemWorldSavedData.get(world);
		if(data == null) return false;

		OrbitalStation station = data.getStationAtGrid(destination.x, destination.z);
		if(station != null && data.matchesDriveIdentity(station, stack, false)) return !station.deleting;
		if(data.shouldResetNormalStationDrive(stack)) resetToEmptyHardDrive(stack);
		return false;
	}

	/** Validates every normal station drive in an inventory and marks it dirty if changed. */
	public static boolean validateNormalStationDrives(IInventory inventory, World world) {
		if(inventory == null || world == null || world.isRemote) return false;
		boolean changed = false;
		for(int slot = 0; slot < inventory.getSizeInventory(); slot++) {
			ItemStack stack = inventory.getStackInSlot(slot);
			if(isNormalStationDrive(stack)) {
				Item before = stack.getItem();
				validateNormalStationDrive(stack, world);
				if(stack.getItem() != before) changed = true;
			}
		}
		if(changed) inventory.markDirty();
		return changed;
	}

	public static ItemStack createNormalStationDrive(OrbitalStation station) {
		if(station == null) return null;
		station.ensureIdentity();
		ItemStack drive = new ItemStack(ModItems.full_drive, 1, SolarSystem.Body.ORBIT.ordinal());
		drive.stackTagCompound = new NBTTagCompound();
		drive.stackTagCompound.setInteger("x", station.dX);
		drive.stackTagCompound.setInteger("z", station.dZ);
		drive.stackTagCompound.setBoolean("Processed", true);
		drive.stackTagCompound.setString("stationName", station.name == null ? "" : station.name);
		drive.stackTagCompound.setString(TAG_STATION_KEY, station.stationKey);
		drive.stackTagCompound.setInteger(TAG_STATION_GENERATION, station.generation);
		drive.stackTagCompound.setString(TAG_STATION_DRIVE_TYPE, DRIVE_TYPE_NORMAL);
		drive.stackTagCompound.setInteger("sDim", station.orbiting == null ? 0 : station.orbiting.dimensionId);
		drive.stackTagCompound.setBoolean("sHas", station.hasStation);
		return drive;
	}

	public static Target getTarget(ItemStack stack, World world) {
		if(isNormalStationDrive(stack) && world != null && !world.isRemote && !validateNormalStationDrive(stack, world)) return new Target(null, true, false);
		if(!isUsableDrive(stack)) return new Target(null, false, false);
		if(!stack.hasTagCompound()) stack.stackTagCompound = new NBTTagCompound();
		Destination destination = getDestinationUnchecked(stack);
		if(destination == null) return new Target(null, false, false);

		if(destination.body == SolarSystem.Body.ORBIT) {
			if(world == null || world.isRemote) {
				CelestialBody body = CelestialBody.getBody(stack.stackTagCompound.getInteger("sDim"));
				return new Target(body, true, stack.stackTagCompound.getBoolean("sHas"));
			}

			SolarSystemWorldSavedData data = SolarSystemWorldSavedData.get(world);
			if(data == null) return new Target(null, true, false);
			OrbitalStation station = data.getStationAtGrid(destination.x, destination.z);
			boolean raid = isRaidStationDrive(stack);
			boolean identityValid = station != null && data.matchesDriveIdentity(station, stack, raid);
			if(!identityValid || station.deleting || (raid && !station.hasStation)) {
				stack.stackTagCompound.setBoolean("sHas", false);
				return new Target(null, true, false);
			}

			station.ensureIdentity();
			stack.stackTagCompound.setBoolean(TAG_STATION_DELETED, false);
			stack.stackTagCompound.setString("stationName", station.name == null ? "" : station.name);
			stack.stackTagCompound.setInteger("sDim", station.orbiting == null ? 0 : station.orbiting.dimensionId);
			stack.stackTagCompound.setBoolean("sHas", station.hasStation);
			if(!stack.stackTagCompound.hasKey(TAG_STATION_GENERATION) && station.generation == 0) {
				// Preserve legacy generation-zero drives without rewriting unrelated NBT.
			} else {
				stack.stackTagCompound.setInteger(TAG_STATION_GENERATION, station.generation);
			}
			return new Target(station.orbiting, true, station.hasStation);
		}

		return new Target(destination.body.getBody(), false, true);
	}

	public static boolean validateOrbitLaunch(ItemStack stack, World world) {
		if(isNormalStationDrive(stack) && world != null && !world.isRemote && !validateNormalStationDrive(stack, world)) return false;
		Destination destination = getDestinationUnchecked(stack);
		if(destination == null || destination.body != SolarSystem.Body.ORBIT || world == null || world.isRemote) return destination != null;
		SolarSystemWorldSavedData data = SolarSystemWorldSavedData.get(world);
		if(data == null) return false;
		OrbitalStation station = data.getStationAtGrid(destination.x, destination.z);
		if(station == null || station.deleting || !data.matchesDriveIdentity(station, stack, isRaidStationDrive(stack))) return false;
		net.minecraft.world.WorldServer orbit = net.minecraftforge.common.DimensionManager.getWorld(SpaceConfig.orbitDimension);
		if(orbit == null) {
			net.minecraftforge.common.DimensionManager.initDimension(SpaceConfig.orbitDimension);
			orbit = net.minecraftforge.common.DimensionManager.getWorld(SpaceConfig.orbitDimension);
		}
		if(orbit == null) return false;
		if(isRaidStationDrive(stack)) return data.canLaunchRaidDrive(stack, orbit);
		return data.canLaunchNormalDrive(stack, orbit);
	}

	public static int getOrbitArrivalX(ItemStack stack, World world) {
		Destination destination = getDestinationUnchecked(stack);
		if(destination == null) return 0;
		SolarSystemWorldSavedData data = SolarSystemWorldSavedData.get(world);
		OrbitalStation station = data == null ? null : data.getStationAtGrid(destination.x, destination.z);
		if(station == null) return destination.x * OrbitalStation.STATION_SIZE + OrbitalStation.STATION_SIZE / 2;
		return isRaidStationDrive(stack) && station.raidPortActive ? station.raidPortX : station.getCenterBlockX();
	}

	public static int getOrbitArrivalZ(ItemStack stack, World world) {
		Destination destination = getDestinationUnchecked(stack);
		if(destination == null) return 0;
		SolarSystemWorldSavedData data = SolarSystemWorldSavedData.get(world);
		OrbitalStation station = data == null ? null : data.getStationAtGrid(destination.x, destination.z);
		if(station == null) return destination.z * OrbitalStation.STATION_SIZE + OrbitalStation.STATION_SIZE / 2;
		return isRaidStationDrive(stack) && station.raidPortActive ? station.raidPortZ : station.getCenterBlockZ();
	}

	public static void setCoordinates(ItemStack stack, int x, int z) {
		if(!stack.hasTagCompound())
			stack.stackTagCompound = new NBTTagCompound();

		stack.stackTagCompound.setInteger("x", x);
		stack.stackTagCompound.setInteger("z", z);
	}

	public static int getProcessingTier(ItemStack stack, CelestialBody from) {
		SolarSystem.Body body = getBody(stack);
		return body.getProcessingLevel(from);
	}

	public static boolean getProcessed(ItemStack stack) {
		if(!isUsableDrive(stack)) return false;
		if(!stack.hasTagCompound())
			stack.stackTagCompound = new NBTTagCompound();

		return stack.stackTagCompound.getBoolean("Processed");
	}

	public static void setProcessed(ItemStack stack, boolean processed) {
		if(!stack.hasTagCompound())
			stack.stackTagCompound = new NBTTagCompound();

		stack.stackTagCompound.setBoolean("Processed", processed);
	}

	// Returns an area for the Stardar to draw, so the player can pick a safe spot to land
	public static Destination getApproximateDestination(ItemStack stack) {
		if(!stack.hasTagCompound())
			stack.stackTagCompound = new NBTTagCompound();

		SolarSystem.Body body = getBody(stack);
		if(!stack.stackTagCompound.hasKey("ax") || !stack.stackTagCompound.hasKey("az")) {
			stack.stackTagCompound.setInteger("ax", itemRand.nextInt(SpaceConfig.maxProbeDistance * 2) - SpaceConfig.maxProbeDistance);
			stack.stackTagCompound.setInteger("az", itemRand.nextInt(SpaceConfig.maxProbeDistance * 2) - SpaceConfig.maxProbeDistance);
		}
		int ax = stack.stackTagCompound.getInteger("ax");
		int az = stack.stackTagCompound.getInteger("az");
		return new Destination(body, ax, az);
	}

	public static void markCopied(ItemStack stack) {
		if(!stack.hasTagCompound())
			stack.stackTagCompound = new NBTTagCompound();

		stack.stackTagCompound.setBoolean("copied", true);
	}

	public static boolean wasCopied(ItemStack stack) {
		if(!stack.hasTagCompound()) return false;
		return stack.stackTagCompound.getBoolean("copied");
	}

	@Override
	public void onUpdate(ItemStack stack, World world, Entity entity, int slot, boolean held) {
		if(world == null || world.isRemote || !isNormalStationDrive(stack)) return;
		Item before = stack.getItem();
		validateNormalStationDrive(stack, world);
		if(stack.getItem() != before && entity instanceof EntityPlayer) {
			EntityPlayer player = (EntityPlayer)entity;
			player.inventory.markDirty();
			if(player.inventoryContainer != null) player.inventoryContainer.detectAndSendChanges();
		}
	}

	@Override
	public boolean onEntityItemUpdate(EntityItem entityItem) {
		if(entityItem != null && entityItem.worldObj != null && !entityItem.worldObj.isRemote) {
			ItemStack stack = entityItem.getEntityItem();
			Item before = stack == null ? null : stack.getItem();
			if(isNormalStationDrive(stack)) validateNormalStationDrive(stack, entityItem.worldObj);
			if(stack != null && stack.getItem() != before) entityItem.setEntityItemStack(stack);
		}
		return false;
	}

	@Override
	public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
		if(!world.isRemote && isNormalStationDrive(stack) && !validateNormalStationDrive(stack, world)) return stack;
		Destination destination = getDestination(stack);
		if(destination == null || destination.body == null) return stack;
		boolean isProcessed = getProcessed(stack);
		boolean onDestination = world.provider.dimensionId == destination.body.getDimensionId();

		// If we're on the body (or in creative), immediately process
		if(!isProcessed && (player.capabilities.isCreativeMode || onDestination)) {
			isProcessed = true;
			setProcessed(stack, true);
		}

		ItemStack newStack = stack;

		if(isProcessed && player.ridingEntity != null && player.ridingEntity instanceof EntityRideableRocket) {
			EntityRideableRocket rocket = (EntityRideableRocket) player.ridingEntity;

			if(rocket.getRocket().stages.size() > 0 || world.provider.dimensionId == SpaceConfig.orbitDimension || rocket.isReusable()) {
				if(rocket.getState() == RocketState.LANDED || rocket.getState() == RocketState.AWAITING) {
					// Replace our held stack with the rocket drive and place our held drive into the rocket
					if(rocket.navDrive != null) {
						newStack = rocket.navDrive;
					} else {
						newStack.stackSize = 0;
					}

					rocket.navDrive = stack.copy();
					rocket.navDrive.stackSize = 1;

					if(!world.isRemote) {
						rocket.setState(RocketState.AWAITING);
					}

					world.playSoundEffect(player.posX, player.posY, player.posZ, "hbm:item.upgradePlug", 1.0F, 1.0F);
				}
			}
		}

		return newStack;
	}


	@Override
	public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side, float fx, float fy, float fz) {
		if(!world.isRemote && isNormalStationDrive(stack) && !validateNormalStationDrive(stack, world)) return false;
		Destination destination = getDestination(stack);
		if(destination == null || destination.body == null) return false;
		if(destination.body == SolarSystem.Body.ORBIT) {
			if(isRaidStationDrive(stack)) return false;
			if(world.provider.dimensionId == SpaceConfig.orbitDimension) return false;

			if(!world.isRemote) {
				SolarSystemWorldSavedData data = SolarSystemWorldSavedData.get(world);
				OrbitalStation station = data == null || data.isStationDeleted(destination.x, destination.z) ? null : data.getStationAtGrid(destination.x, destination.z);

				Destination target = new Destination(CelestialBody.getEnum(world), x, z);

				if(station != null && station.recallPod(target)) {
					player.addChatMessage(new ChatComponentText(EnumChatFormatting.YELLOW + "" + EnumChatFormatting.ITALIC + "Recalling drop pod to coordinates: " + x + ", " + z));
				} else {
					player.addChatMessage(new ChatComponentText(EnumChatFormatting.YELLOW + "" + EnumChatFormatting.ITALIC + "Could not recall drop pod from station!"));
				}
			}

			return true;
		}

		boolean onDestination = world.provider.dimensionId == destination.body.getDimensionId();
		if(!onDestination)
			return false;

		setCoordinates(stack, x, z);
		setProcessed(stack, true);

		if(!world.isRemote)
			player.addChatMessage(new ChatComponentText(EnumChatFormatting.YELLOW + "" + EnumChatFormatting.ITALIC + "Set landing coordinates to: " + x + ", " + z));

		return true;
	}

	public static class Destination {

		public int x;
		public int z;
		public SolarSystem.Body body;

		public Destination(SolarSystem.Body body, int x, int z) {
			this.body = body;
			this.x = x;
			this.z = z;
		}

		public ChunkCoordIntPair getChunk() {
			return new ChunkCoordIntPair(x >> 4, z >> 4);
		}

	}

	public static class Target {

		public CelestialBody body;
		public boolean inOrbit;
		public boolean isValid;

		public Target(CelestialBody body, boolean inOrbit, boolean isValid) {
			this.body = body;
			this.inOrbit = inOrbit;
			this.isValid = isValid;
		}

	}

}
