package com.hbm.main;

import codechicken.nei.api.API;
import codechicken.nei.api.IConfigureNEI;
import codechicken.nei.api.IHighlightHandler;
import codechicken.nei.api.ItemInfo.Layout;
import codechicken.nei.recipe.*;
import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.generic.BlockPlushie.TileEntityPlushie;
import com.hbm.config.ClientConfig;
import com.hbm.config.CustomMachineConfigJSON;
import com.hbm.handler.nei.AnvilOverlayHandler;
import com.hbm.handler.nei.CustomMachineHandler;
import com.hbm.items.ItemEnums.EnumIngotMetal;
import com.hbm.inventory.gui.GUIAnvil;
import com.hbm.items.ItemEnums.EnumSecretType;
import com.hbm.items.ModItems;
import com.hbm.items.machine.ItemBattery;
import com.hbm.items.special.ItemBedrockOreNew;
import com.hbm.items.special.ItemBedrockOreNew.BedrockOreGrade;
import com.hbm.items.special.ItemBedrockOreNew.CelestialBedrockOre;
import com.hbm.items.special.ItemBedrockOreNew.CelestialBedrockOreType;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.items.weapon.sedna.factory.GunFactory.EnumAmmoSecret;
import com.hbm.lib.RefStrings;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;
import net.minecraftforge.oredict.OreDictionary;

import java.util.List;

public class NEIConfig implements IConfigureNEI {

	@Override
	public void loadConfig() {
        for(ItemStack stack : com.hbm.config.BanItems.selectedVariants()) hideAvailableItem(stack);
		for (TemplateRecipeHandler handler: NEIRegistry.listAllHandlers()) {
			registerHandler(handler);
		}
		API.registerGuiOverlayHandler(GUIAnvil.class, new AnvilOverlayHandler(), "ntmAnvil");

		for(CustomMachineConfigJSON.MachineConfiguration conf : CustomMachineConfigJSON.niceList) {
			registerHandlerBypass(new CustomMachineHandler(conf));
		}

		if(ClientConfig.NEI_HIDE_SECRETS.get()) {
			for(Item item : ItemGunBaseNT.secrets) hideAvailableItem(new ItemStack(item));
			for(int i = 0; i < EnumAmmoSecret.values().length; i++) hideAvailableItem(new ItemStack(ModItems.ammo_secret, 1, i));
			for(int i = 0; i < EnumSecretType.values().length; i++) hideAvailableItem(new ItemStack(ModItems.item_secret, 1, i));
		}

		for(int i = 0; i < EnumIngotMetal.values().length; i++) hideAvailableItem(new ItemStack(ModItems.ingot_metal, 1, i));

		//Some things are even beyond my control...or are they?
		hideAvailableItem(ItemBattery.getEmptyBattery(ModItems.memory));
		hideAvailableItem(ItemBattery.getFullBattery(ModItems.memory));

		hideAvailableItem(new ItemStack(ModBlocks.machine_electric_furnace_on));
		hideAvailableItem(new ItemStack(ModBlocks.machine_difurnace_on));
		hideAvailableItem(new ItemStack(ModBlocks.machine_rtg_furnace_on));
		hideAvailableItem(new ItemStack(ModBlocks.reinforced_lamp_on));
		hideAvailableItem(new ItemStack(ModBlocks.statue_elb_f));
		hideAvailableItem(new ItemStack(ModItems.euphemium_kit));
		hideAvailableItem(new ItemStack(ModItems.bobmazon_hidden));
		hideAvailableItem(new ItemStack(ModItems.book_lore)); //the broken nbt-less one shouldn't show up in normal play anyway
		if(MainRegistry.polaroidID != 11) {
			hideAvailableItem(new ItemStack(ModItems.book_secret));
			hideAvailableItem(new ItemStack(ModItems.book_of_));
			hideAvailableItem(new ItemStack(ModItems.burnt_bark));
			hideAvailableItem(new ItemStack(ModItems.ams_core_thingy));
		}
		hideAvailableItem(new ItemStack(ModBlocks.dummy_block_blast));
		hideAvailableItem(new ItemStack(ModBlocks.dummy_port_compact_launcher));
		hideAvailableItem(new ItemStack(ModBlocks.dummy_port_launch_table));
		hideAvailableItem(new ItemStack(ModBlocks.dummy_plate_compact_launcher));
		hideAvailableItem(new ItemStack(ModBlocks.dummy_plate_launch_table));

		hideAvailableItem(new ItemStack(ModBlocks.pink_log));
		hideAvailableItem(new ItemStack(ModBlocks.pink_planks));
		hideAvailableItem(new ItemStack(ModBlocks.pink_slab));
		hideAvailableItem(new ItemStack(ModBlocks.pink_double_slab));
		hideAvailableItem(new ItemStack(ModBlocks.pink_stairs));

		hideAvailableItem(new ItemStack(ModBlocks.spotlight_incandescent_off));
		hideAvailableItem(new ItemStack(ModBlocks.spotlight_fluoro_off));
		hideAvailableItem(new ItemStack(ModBlocks.spotlight_halogen_off));
		hideAvailableItem(new ItemStack(ModBlocks.spotlight_beam));

		hideAvailableItem(new ItemStack(ModItems.rocket_custom));
		hideAvailableItem(new ItemStack(ModBlocks.orbital_station));

		hideAvailableItem(new ItemStack(ModBlocks.conveyor));
		hideAvailableItem(new ItemStack(ModBlocks.conveyor_chute));
		hideAvailableItem(new ItemStack(ModBlocks.conveyor_lift));
		hideAvailableItem(new ItemStack(ModBlocks.conveyor_express));
		hideAvailableItem(new ItemStack(ModBlocks.conveyor_double));
		hideAvailableItem(new ItemStack(ModBlocks.conveyor_triple));
		
		hideAvailableItem(new ItemStack(ModBlocks.war_controller));
		hideAvailableItem(new ItemStack(ModItems.sat_war));

		hideAvailableItem(new ItemStack(ModBlocks.furnace));
		hideAvailableItem(new ItemStack(ModBlocks.lit_furnace));

		for(BedrockOreGrade grade : BedrockOreGrade.values()) {
			if(grade == BedrockOreGrade.BASE) continue;
			for(CelestialBedrockOreType type : CelestialBedrockOre.getAllTypes()) {
				hideAvailableItem(ItemBedrockOreNew.make(grade, type));
			}
		}

		hideAvailableItem(new ItemStack(ModBlocks.brick_forgotten, 1, OreDictionary.WILDCARD_VALUE));
		hideAvailableItem(new ItemStack(ModBlocks.brick_forgotten_lock, 1, OreDictionary.WILDCARD_VALUE));
		hideAvailableItem(new ItemStack(ModItems.coal_eternal));

		API.registerHighlightIdentifier(ModBlocks.plushie, new IHighlightHandler() {
			@Override public ItemStack identifyHighlight(World world, EntityPlayer player, MovingObjectPosition mop) {
				int x = mop.blockX;
				int y = mop.blockY;
				int z = mop.blockZ;
				TileEntity te = world.getTileEntity(x, y, z);
				if(te instanceof TileEntityPlushie) {
					TileEntityPlushie plush = (TileEntityPlushie) te;
					return new ItemStack(ModBlocks.plushie, 1, plush.type.ordinal());
				}
				return null;
			}
			@Override public List<String> handleTextData(ItemStack itemStack, World world, EntityPlayer player, MovingObjectPosition mop, List<String> currenttip, Layout layout) { return currenttip; }
		});
	}

    private static void hideAvailableItem(ItemStack stack) {
        if(stack != null && stack.getItem() != null && Item.itemRegistry.getNameForObject(stack.getItem()) != null) API.hideItem(stack);
    }

	public static void registerHandler(Object o) {
		API.registerRecipeHandler((ICraftingHandler) o);
		API.registerUsageHandler((IUsageHandler) o);
	}

	/** Bypasses the utterly useless restriction of one registered handler per class */
	public static void registerHandlerBypass(Object o) {
		GuiCraftingRecipe.craftinghandlers.add((ICraftingHandler) o);
		GuiUsageRecipe.usagehandlers.add((IUsageHandler) o);
	}

	@Override
	public String getName() {
		return "Nuclear Tech NEI Plugin";
	}

	@Override
	public String getVersion() {
		return RefStrings.VERSION;
	}
}
