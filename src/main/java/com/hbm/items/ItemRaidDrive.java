package com.hbm.items;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.hbm.dim.SolarSystem;
import com.hbm.dim.orbit.OrbitalStation;
import com.hbm.lib.RefStrings;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;

/** A temporary station drive programmed with /ntm station raid <name>. */
public class ItemRaidDrive extends ItemVOTVdrive {

	public static final String TAG_RAID_DRIVE = "hbmRaidDrive";
	public static final String TAG_EXPIRES_AT = "hbmRaidExpiresAt";
	public static final String TAG_RAID_TOKEN = "hbmRaidToken";
	public static final String TAG_RUNTIME_CLOCK = "hbmRaidUsesServerRuntimeClock";

	@SideOnly(Side.CLIENT)
	private IIcon emptyIcon;
	@SideOnly(Side.CLIENT)
	private IIcon programmedIcon;
	@SideOnly(Side.CLIENT)
	private IIcon orbitIcon;
	@SideOnly(Side.CLIENT)
	private IIcon blankIcon;

	public ItemRaidDrive() {
		super();
		setMaxStackSize(1);
	}

	public static boolean isRaidDrive(ItemStack stack) {
		return stack != null && stack.getItem() == ModItems.raid_drive;
	}

	public static boolean isProgrammed(ItemStack stack) {
		return isRaidDrive(stack)
			&& stack.hasTagCompound()
			&& stack.stackTagCompound.getBoolean(TAG_RAID_DRIVE)
			&& stack.stackTagCompound.getLong(TAG_EXPIRES_AT) > 0
			&& stack.stackTagCompound.getBoolean("Processed")
			&& stack.getItemDamage() == SolarSystem.Body.ORBIT.ordinal();
	}

	/** Rejects malformed or previously programmed Raid Hard Drives instead of overwriting their NBT. */
	public static boolean isUnprogrammed(ItemStack stack) {
		if(!isRaidDrive(stack)) return false;
		if(!stack.hasTagCompound()) return true;
		NBTTagCompound tag = stack.stackTagCompound;
		return !tag.hasKey(TAG_RAID_DRIVE)
			&& !tag.hasKey(TAG_EXPIRES_AT)
			&& !tag.hasKey(TAG_RAID_TOKEN)
			&& !tag.hasKey(TAG_RUNTIME_CLOCK)
			&& !tag.hasKey(ItemVOTVdrive.TAG_STATION_KEY)
			&& !tag.hasKey(ItemVOTVdrive.TAG_STATION_GENERATION)
			&& !tag.hasKey(ItemVOTVdrive.TAG_STATION_DRIVE_TYPE)
			&& !tag.hasKey("x")
			&& !tag.hasKey("z")
			&& !tag.hasKey("Processed");
	}

	public static boolean isExpired(ItemStack stack) {
		if(!isProgrammed(stack)) return false;
		long now = currentServerRuntimeMillis();
		if(now < 0L) return false;
		migrateLegacyDeadline(stack, now);
		return stack.stackTagCompound.getLong(TAG_EXPIRES_AT) <= now;
	}

	/**
	 * Validates a programmed raid drive and converts an expired stack in place.
	 * New drives use server-running world time, so their lifetime pauses while
	 * the server is offline. Legacy wall-clock drives are migrated once using
	 * whatever remaining duration they had when first observed by the server.
	 */
	public static boolean validate(ItemStack stack) {
		if(!isProgrammed(stack)) return false;
		long now = currentServerRuntimeMillis();
		if(now < 0L) return true;
		migrateLegacyDeadline(stack, now);
		if(stack.stackTagCompound.getLong(TAG_EXPIRES_AT) <= now) {
			corrupt(stack);
			return false;
		}
		return true;
	}

	private static void migrateLegacyDeadline(ItemStack stack, long runtimeNow) {
		if(stack == null || !stack.hasTagCompound() || stack.stackTagCompound.getBoolean(TAG_RUNTIME_CLOCK)) return;
		long legacyDeadline = stack.stackTagCompound.getLong(TAG_EXPIRES_AT);
		long remaining = Math.max(0L, legacyDeadline - System.currentTimeMillis());
		stack.stackTagCompound.setLong(TAG_EXPIRES_AT, safeAdd(runtimeNow, remaining));
		stack.stackTagCompound.setBoolean(TAG_RUNTIME_CLOCK, true);
	}

	private static long currentServerRuntimeMillis() {
		World overworld = DimensionManager.getWorld(0);
		if(overworld == null || overworld.isRemote) return -1L;
		return ticksToMillis(overworld.getTotalWorldTime());
	}

	private static long ticksToMillis(long ticks) {
		if(ticks <= 0L) return 0L;
		return ticks > Long.MAX_VALUE / 50L ? Long.MAX_VALUE : ticks * 50L;
	}

	private static long safeAdd(long value, long amount) {
		if(amount > 0L && value > Long.MAX_VALUE - amount) return Long.MAX_VALUE;
		return value + amount;
	}

	public static void corrupt(ItemStack stack) {
		if(stack == null || stack.getItem() != ModItems.raid_drive) return;
		int size = Math.max(1, Math.min(stack.stackSize, ModItems.corrupted_drive.getItemStackLimit()));
		stack.func_150996_a(ModItems.corrupted_drive);
		stack.setItemDamage(0);
		stack.stackTagCompound = null;
		stack.stackSize = size;
	}

	public static ItemStack createProgrammed(OrbitalStation station, long expiresAt, int stackSize) {
		if(station == null) return null;
		station.ensureIdentity();
		int size = Math.max(1, Math.min(stackSize, ModItems.raid_drive.getItemStackLimit()));
		ItemStack drive = new ItemStack(ModItems.raid_drive, size, SolarSystem.Body.ORBIT.ordinal());
		drive.stackTagCompound = new NBTTagCompound();
		drive.stackTagCompound.setInteger("x", station.dX);
		drive.stackTagCompound.setInteger("z", station.dZ);
		drive.stackTagCompound.setBoolean("Processed", true);
		drive.stackTagCompound.setString("stationName", station.name == null ? "" : station.name);
		drive.stackTagCompound.setBoolean(TAG_RAID_DRIVE, true);
		drive.stackTagCompound.setLong(TAG_EXPIRES_AT, expiresAt);
		drive.stackTagCompound.setBoolean(TAG_RUNTIME_CLOCK, true);
		drive.stackTagCompound.setString(TAG_RAID_TOKEN, UUID.randomUUID().toString());
		drive.stackTagCompound.setString(ItemVOTVdrive.TAG_STATION_KEY, station.stationKey);
		drive.stackTagCompound.setInteger(ItemVOTVdrive.TAG_STATION_GENERATION, station.generation);
		drive.stackTagCompound.setString(ItemVOTVdrive.TAG_STATION_DRIVE_TYPE, ItemVOTVdrive.DRIVE_TYPE_RAID);
		drive.stackTagCompound.setInteger("sDim", station.orbiting == null ? 0 : station.orbiting.dimensionId);
		drive.stackTagCompound.setBoolean("sHas", station.hasStation);
		return drive;
	}

	@Override
	public void onUpdate(ItemStack stack, World world, Entity entity, int slot, boolean held) {
		if(!world.isRemote && isProgrammed(stack)) validate(stack);
	}

	@Override
	public boolean onEntityItemUpdate(EntityItem entityItem) {
		if(entityItem != null && entityItem.worldObj != null && !entityItem.worldObj.isRemote) {
			ItemStack stack = entityItem.getEntityItem();
			if(isProgrammed(stack)) validate(stack);
		}
		return false;
	}

	@Override
	public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
		if(!validate(stack)) {
			if(!world.isRemote && isUnprogrammed(stack)) {
				player.addChatMessage(new ChatComponentText(EnumChatFormatting.YELLOW + "Use /ntm station raid <name> while holding this drive."));
			}
			return stack;
		}
		return super.onItemRightClick(stack, world, player);
	}

	@Override
	public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side, float fx, float fy, float fz) {
		if(!validate(stack)) return false;
		return super.onItemUse(stack, player, world, x, y, z, side, fx, fy, fz);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	@SideOnly(Side.CLIENT)
	public void addInformation(ItemStack stack, EntityPlayer player, List list, boolean bool) {
		if(!isProgrammed(stack)) {
			if(isUnprogrammed(stack)) {
				list.add(EnumChatFormatting.YELLOW + "Unprogrammed");
				list.add("Use /ntm station raid <name> while holding this drive.");
			} else {
				list.add(EnumChatFormatting.RED + "Invalid Raid Hard Drive data");
			}
			return;
		}

		long now;
		if(stack.stackTagCompound.getBoolean(TAG_RUNTIME_CLOCK) && player != null && player.worldObj != null) {
			now = ticksToMillis(player.worldObj.getTotalWorldTime());
		} else {
			now = System.currentTimeMillis();
		}
		long remaining = stack.stackTagCompound.getLong(TAG_EXPIRES_AT) - now;
		if(remaining <= 0) {
			list.add(EnumChatFormatting.RED + "Expired");
			return;
		}

		super.addInformation(stack, player, list, bool);
		long seconds = Math.max(1L, TimeUnit.MILLISECONDS.toSeconds(remaining));
		long minutes = seconds / 60L;
		long leftoverSeconds = seconds % 60L;
		list.add(EnumChatFormatting.YELLOW + String.format("Expires in %d:%02d", minutes, leftoverSeconds));
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	@SideOnly(Side.CLIENT)
	public void getSubItems(Item item, CreativeTabs tab, List list) {
		list.add(new ItemStack(item, 1, 0));
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void registerIcons(IIconRegister iconRegister) {
		emptyIcon = iconRegister.registerIcon(RefStrings.MODID + ":votv_raid_e");
		programmedIcon = iconRegister.registerIcon(RefStrings.MODID + ":votv_raid_f");
		orbitIcon = iconRegister.registerIcon(RefStrings.MODID + ":votv.orbit");
		blankIcon = iconRegister.registerIcon(RefStrings.MODID + ":pipette_empty");
		itemIcon = emptyIcon;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public boolean requiresMultipleRenderPasses() {
		return true;
	}

	@Override
	public int getRenderPasses(int metadata) {
		// Empty and programmed raid drives both use ORBIT metadata, so NBT selects a real or transparent second pass.
		return 2;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public IIcon getIcon(ItemStack stack, int pass) {
		boolean programmed = isProgrammed(stack);
		if(pass == 0) return programmed ? programmedIcon : emptyIcon;
		return programmed ? orbitIcon : blankIcon;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public IIcon getIconFromDamageForRenderPass(int metadata, int pass) {
		return pass == 0 ? emptyIcon : blankIcon;
	}
}
