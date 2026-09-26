package com.hbm.extprop;

import java.util.ArrayList;
import java.util.List;

import com.hbm.entity.train.EntityRailCarBase;
import com.hbm.handler.ArmorModHandler;
import com.hbm.handler.HbmKeybinds.EnumKeybind;
import com.hbm.inventory.InventoryDriveCrate;
import com.hbm.items.armor.ItemModShield;
import com.hbm.main.MainRegistry;
import com.hbm.packet.PacketDispatcher;
import com.hbm.packet.toclient.PlayerInformPacket;
import com.hbm.tileentity.IGUIProvider;

import cpw.mods.fml.common.network.internal.FMLNetworkHandler;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.World;
import net.minecraftforge.common.IExtendedEntityProperties;

public class HbmPlayerProps implements IExtendedEntityProperties {

	public static final String key = "NTM_EXT_PLAYER";
	public EntityPlayer player;

	/* Toggles for keybind */
	public boolean enableHUD = true;
	public boolean enableBackpack = true;
	public boolean enableMagnet = true;

	/** Keybind tracking */
	private boolean[] keysPressed = new boolean[EnumKeybind.values().length];

	
	/* Dashes for bismuth armor/cloud in a bottle */
	public boolean dashActivated = true;
	public int dashCooldown = 0;
	public int totalDashCount = 0;
	public int stamina = 0;
	public static final int dashCooldownLength = 5;

	/** Cooldown for armor plinking noise when canceling damage */
	public int plinkCooldown = 0;
	public static final int plinkCooldownLength = 10;

	/** Shield infusion */
	public float shield = 0;
	public float maxShield = 0;
	public int lastDamage = 0;
	public int nitanCount = 0;
	public int nitanHealth = nitanCount*10;
	public static final float shieldCap = 100;

	/** Latnern repair/destroy count */
	public int reputation;

	/** Hack for allowing ladders on multiblocks */
	public boolean isOnLadder = false;
	
	/** Pulling the pin on a grenade - it's a player prop instead of an NBT trait */
	public int grenadeDeployment;

	public boolean hasWarped = false;

	public int lastDimension = 0;
	
	/** Maskman timer */
	public int maskManTimer = 0;

	/** Private per-player Drive Crate storage. Never belongs to a block or item stack. */
	public ItemStack[] driveCrateInventory = new ItemStack[InventoryDriveCrate.SIZE];
	private final List<ItemStack> driveCrateRecovery = new ArrayList<ItemStack>();

	public HbmPlayerProps(EntityPlayer player) {
		this.player = player;
	}

	public static HbmPlayerProps registerData(EntityPlayer player) {
		player.registerExtendedProperties(key, new HbmPlayerProps(player));
		return (HbmPlayerProps) player.getExtendedProperties(key);
	}

	public static HbmPlayerProps getData(EntityPlayer player) {
		HbmPlayerProps props = (HbmPlayerProps) player.getExtendedProperties(key);
		return props != null ? props : registerData(player);
	}

	public boolean getKeyPressed(EnumKeybind key) {
		return keysPressed[key.ordinal()];
	}

	public boolean isJetpackActive() {
		return this.enableBackpack && getKeyPressed(EnumKeybind.JETPACK);
	}

	public boolean isMagnetActive(){
		return this.enableMagnet;
	}

	public void setKeyPressed(EnumKeybind key, boolean pressed) {

		if(!getKeyPressed(key) && pressed) {

			if(key == EnumKeybind.TOGGLE_JETPACK) {

				if(!player.worldObj.isRemote) {
					this.enableBackpack = !this.enableBackpack;

					if(this.enableBackpack)
						PacketDispatcher.wrapper.sendTo(new PlayerInformPacket(EnumChatFormatting.GREEN + "Jetpack ON", MainRegistry.proxy.ID_JETPACK, 1000), (EntityPlayerMP) player);
					else
						PacketDispatcher.wrapper.sendTo(new PlayerInformPacket(EnumChatFormatting.RED + "Jetpack OFF", MainRegistry.proxy.ID_JETPACK, 1000), (EntityPlayerMP) player);
				}
			}
			if (key == EnumKeybind.TOGGLE_MAGNET){
				if (!player.worldObj.isRemote){
					this.enableMagnet = !this.enableMagnet;

					if(this.enableMagnet)
						PacketDispatcher.wrapper.sendTo(new PlayerInformPacket(EnumChatFormatting.GREEN + "Magnet ON", MainRegistry.proxy.ID_MAGNET, 1000), (EntityPlayerMP) player);
					else
						PacketDispatcher.wrapper.sendTo(new PlayerInformPacket(EnumChatFormatting.RED + "Magnet OFF", MainRegistry.proxy.ID_MAGNET, 1000), (EntityPlayerMP) player);
				}
			}
			if(key == EnumKeybind.TOGGLE_HEAD) {

				if(!player.worldObj.isRemote) {
					this.enableHUD = !this.enableHUD;

					if(this.enableHUD)
						PacketDispatcher.wrapper.sendTo(new PlayerInformPacket(EnumChatFormatting.GREEN + "HUD ON", MainRegistry.proxy.ID_HUD, 1000), (EntityPlayerMP) player);
					else
						PacketDispatcher.wrapper.sendTo(new PlayerInformPacket(EnumChatFormatting.RED + "HUD OFF", MainRegistry.proxy.ID_HUD, 1000), (EntityPlayerMP) player);
				}
			}

			if(key == EnumKeybind.TRAIN) {

				if(!this.player.worldObj.isRemote) {

					if(player.ridingEntity != null && player.ridingEntity instanceof EntityRailCarBase && player.ridingEntity instanceof IGUIProvider) {
						FMLNetworkHandler.openGui(player, MainRegistry.instance, 0, player.worldObj, player.ridingEntity.getEntityId(), 0, 0);
					}
				}
			}
		}

		keysPressed[key.ordinal()] = pressed;
	}

	public void setDashCooldown(int cooldown) {
		this.dashCooldown = cooldown;
		return;
	}

	public int getDashCooldown() {
		return this.dashCooldown;
	}

	public void setStamina(int stamina) {
		this.stamina = stamina;
		return;
	}

	public int getStamina() {
		return this.stamina;
	}

	public void setDashCount(int count) {
		this.totalDashCount = count;
		return;
	}

	public int getDashCount() {
		return this.totalDashCount;
	}

	public static void plink(EntityPlayer player, String sound, float volume, float pitch) {
		HbmPlayerProps props = HbmPlayerProps.getData(player);

		if(props.plinkCooldown <= 0) {
			player.worldObj.playSoundAtEntity(player, sound, volume, pitch);
			props.plinkCooldown = props.plinkCooldownLength;
		}
	}

	public float getEffectiveMaxShield() {

		float max = this.maxShield;

		if(player.getCurrentArmor(2) != null) {
			ItemStack[] mods = ArmorModHandler.pryMods(player.getCurrentArmor(2));
			if(mods[ArmorModHandler.kevlar] != null && mods[ArmorModHandler.kevlar].getItem() instanceof ItemModShield) {
				ItemModShield mod = (ItemModShield) mods[ArmorModHandler.kevlar].getItem();
				max += mod.shield;
			}
		}

		return max;
	}

	public void copyDriveCrateDataFrom(HbmPlayerProps original) {
		this.driveCrateInventory = new ItemStack[InventoryDriveCrate.SIZE];
		for(int i = 0; i < this.driveCrateInventory.length; i++) {
			ItemStack stack = original.driveCrateInventory[i];
			this.driveCrateInventory[i] = stack == null ? null : stack.copy();
		}

		this.driveCrateRecovery.clear();
		for(ItemStack stack : original.driveCrateRecovery) {
			if(stack != null) this.driveCrateRecovery.add(stack.copy());
		}
	}

	public void recoverInvalidDriveCrateItems() {
		if(player == null || player.worldObj == null || player.worldObj.isRemote) return;

		for(int i = 0; i < driveCrateInventory.length; i++) {
			ItemStack stack = driveCrateInventory[i];
			if(stack != null && !InventoryDriveCrate.isAllowedDrive(stack)) {
				driveCrateInventory[i] = null;
				driveCrateRecovery.add(stack);
			}
		}

		for(int i = driveCrateRecovery.size() - 1; i >= 0; i--) {
			ItemStack recovery = driveCrateRecovery.remove(i);
			if(recovery == null || recovery.stackSize <= 0) continue;
			if(!player.inventory.addItemStackToInventory(recovery) && recovery.stackSize > 0) {
				player.dropPlayerItemWithRandomChoice(recovery, false);
			}
		}
		player.inventoryContainer.detectAndSendChanges();
	}

	@Override
	public void init(Entity entity, World world) { }

	public void serialize(ByteBuf buf) {
		buf.writeFloat(this.shield);
		buf.writeFloat(this.maxShield);
		buf.writeBoolean(this.enableBackpack);
		buf.writeBoolean(this.enableHUD);
		buf.writeInt(this.reputation);
		buf.writeBoolean(this.isOnLadder);
		buf.writeBoolean(this.enableMagnet);
	}

	public void deserialize(ByteBuf buf) {
		if(buf.readableBytes() > 0) {
			this.shield = buf.readFloat();
			this.maxShield = buf.readFloat();
			this.enableBackpack = buf.readBoolean();
			this.enableHUD = buf.readBoolean();
			this.reputation = buf.readInt();
			this.isOnLadder = buf.readBoolean();
			this.enableMagnet = buf.readBoolean();
		}
	}

	@Deprecated
	@Override
	public void saveNBTData(NBTTagCompound nbt) {

		NBTTagCompound props = new NBTTagCompound();

		props.setFloat("shield", shield);
		props.setFloat("maxShield", maxShield);
		props.setFloat("nitan", nitanCount);
		props.setBoolean("enableBackpack", enableBackpack);
		props.setBoolean("enableMagnet", enableMagnet);
		props.setBoolean("enableHUD", enableHUD);
		props.setInteger("reputation", reputation);
		props.setBoolean("isOnLadder", isOnLadder);
		props.setBoolean("hasWarped", hasWarped);
		props.setInteger("lastDimension", lastDimension);
		props.setInteger("maskManTimer", maskManTimer);

		NBTTagList driveInventory = new NBTTagList();
		for(int i = 0; i < driveCrateInventory.length; i++) {
			ItemStack stack = driveCrateInventory[i];
			if(stack == null) continue;
			NBTTagCompound slot = new NBTTagCompound();
			slot.setByte("slot", (byte)i);
			stack.writeToNBT(slot);
			driveInventory.appendTag(slot);
		}
		props.setTag("driveCrateInventory", driveInventory);

		NBTTagList recovery = new NBTTagList();
		for(ItemStack stack : driveCrateRecovery) {
			if(stack == null) continue;
			NBTTagCompound recovered = new NBTTagCompound();
			stack.writeToNBT(recovered);
			recovery.appendTag(recovered);
		}
		props.setTag("driveCrateRecovery", recovery);

		nbt.setTag("HbmPlayerProps", props);
	}

	@Deprecated
	@Override
	public void loadNBTData(NBTTagCompound nbt) {

		NBTTagCompound props = (NBTTagCompound) nbt.getTag("HbmPlayerProps");

		if(props != null) {
			this.shield = props.getFloat("shield");
			this.nitanCount = props.getInteger("nitan");
			this.maxShield = props.getFloat("maxShield");
			this.enableBackpack = props.getBoolean("enableBackpack");
			this.enableMagnet = props.getBoolean("enableMagnet");
			this.enableHUD = props.getBoolean("enableHUD");
			this.reputation = props.getInteger("reputation");
			this.isOnLadder = props.getBoolean("isOnLadder");
			this.hasWarped = props.getBoolean("hasWarped");
			this.lastDimension = props.getInteger("lastDimension");
			this.maskManTimer = props.getInteger("maskManTimer");

			this.driveCrateInventory = new ItemStack[InventoryDriveCrate.SIZE];
			this.driveCrateRecovery.clear();
			NBTTagList driveInventory = props.getTagList("driveCrateInventory", 10);
			for(int i = 0; i < driveInventory.tagCount(); i++) {
				NBTTagCompound slot = driveInventory.getCompoundTagAt(i);
				int index = slot.getByte("slot") & 255;
				ItemStack stack = ItemStack.loadItemStackFromNBT(slot);
				if(stack == null) continue;
				if(index >= 0 && index < driveCrateInventory.length && InventoryDriveCrate.isAllowedDrive(stack) && driveCrateInventory[index] == null) {
					driveCrateInventory[index] = stack;
				} else {
					driveCrateRecovery.add(stack);
				}
			}

			NBTTagList recovery = props.getTagList("driveCrateRecovery", 10);
			for(int i = 0; i < recovery.tagCount(); i++) {
				ItemStack stack = ItemStack.loadItemStackFromNBT(recovery.getCompoundTagAt(i));
				if(stack != null) driveCrateRecovery.add(stack);
			}
		}
	}
}
