import com.google.gson.*;
import com.hbm.config.*;
import com.hbm.inventory.recipes.loader.*;
import com.hbm.itempool.*;
import com.hbm.items.ModItems;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLServerStartedEvent;
import net.minecraft.block.Block;
import net.minecraft.item.*;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.WeightedRandomChestContent;
import net.minecraft.server.MinecraftServer;
import java.io.*;
import java.nio.file.*;
import java.util.*;

@Mod(modid="banitemschecks", name="Disposable BanItems Lifecycle Checks", version="1", dependencies="required-after:hbm")
public class BanItemsLifecycleChecks {
    private int checks;
    private void check(boolean result, String message) { checks++; if(!result) throw new AssertionError(message); }
    @Mod.EventHandler public void started(FMLServerStartedEvent event) {
        try {
            runChecks();
            exportLootAudit();
            String result = "PASS: " + checks + " lifecycle assertions; profile=" + BanItems.enabled;
            Files.write(Paths.get("banitems-check-result.txt"), result.getBytes("UTF-8"));
            System.out.println(result);
        } catch(Throwable failure) {
            failure.printStackTrace();
            try { Files.write(Paths.get("banitems-check-result.txt"), ("FAIL: " + failure).getBytes("UTF-8")); } catch(IOException ignored) { }
        } finally { MinecraftServer.getServer().initiateShutdown(); }
    }
    private void exportLootAudit() throws Exception {
        Properties language = new Properties();
        try(Reader reader = new InputStreamReader(BanItems.class.getResourceAsStream("/assets/hbm/lang/en_US.lang"), "UTF-8")) { language.load(reader); }
        JsonObject pools = new JsonObject();
        for(Map.Entry<String, ItemPool> pool : ItemPool.pools.entrySet()) {
            if(!pool.getKey().startsWith("POOL_")) continue;
            JsonArray rows = new JsonArray();
            for(WeightedRandomChestContent entry : pool.getValue().pool) {
                ItemStack stack = entry.theItemId; JsonObject row = new JsonObject();
                String key = stack.getUnlocalizedName() + ".name";
                row.addProperty("registry", BanItems.name(stack.getItem())); row.addProperty("meta", stack.getItemDamage());
                row.addProperty("language_key", key); row.addProperty("display_name", language.getProperty(key, key));
                row.addProperty("min", entry.theMinimumChanceToGenerateItem); row.addProperty("max", entry.theMaximumChanceToGenerateItem); row.addProperty("weight", entry.itemWeight);
                row.addProperty("disabled", BanItems.disabled(stack)); row.addProperty("max_stack_size", stack.getMaxStackSize());
                if(stack.hasTagCompound()) row.addProperty("nbt", stack.stackTagCompound.toString());
                rows.add(row);
            }
            pools.add(pool.getKey(), rows);
        }
        Files.write(Paths.get("loot-audit.json"), new GsonBuilder().setPrettyPrinting().create().toJson(pools).getBytes("UTF-8"));
    }
    private void runChecks() throws Exception {
        JsonObject profile;
        try(Reader reader = new InputStreamReader(BanItems.class.getResourceAsStream("/assets/hbm/banitems-default.json"), "UTF-8")) {
            profile = new JsonParser().parse(reader).getAsJsonObject();
        }
        check(profile.getAsJsonArray("disabled").size() == 313, "whole registration fixture count");
        check(profile.getAsJsonArray("metadata").size() == 93, "exact metadata fixture count");
        for(JsonElement element : profile.getAsJsonArray("disabled")) {
            String name = element.getAsString();
            check(Item.itemRegistry.containsKey(name) != BanItems.enabled, "item registration " + name);
            if(name.startsWith("hbm:tile.")) check(Block.blockRegistry.containsKey(name) != BanItems.enabled, "block registration " + name);
        }
        for(JsonElement element : profile.getAsJsonArray("metadata")) {
            String[] parts = element.getAsString().split("@");
            Item item = (Item)Item.itemRegistry.getObject(parts[0]);
            if(BanItems.disabledName(parts[0])) {
                check(item == null, "whole-item restriction supersedes former metadata rule " + parts[0]);
                item = BanItems.findItem(parts[0]);
            } else check(item != null, "shared parent remains " + parts[0]);
            check(BanItems.disabled(new ItemStack(item, 1, Integer.parseInt(parts[1]))) == BanItems.enabled, "selected exact metadata " + element);
        }
        int retained = 0;
        for(String line : Files.readAllLines(Paths.get("keep-variants.csv"))) {
            if(line.startsWith("registry,") || line.trim().isEmpty()) continue;
            String[] parts = line.split(",", 3);
            Item item = (Item)Item.itemRegistry.getObject(parts[0]);
            check(item != null && !BanItems.disabled(new ItemStack(item, 1, Integer.parseInt(parts[1]))), "retained variant " + parts[0] + "@" + parts[1]); retained++;
        }
        check(retained == 44, "all intentional ammo/caller gaps tested");
        for(int meta : new int[]{0, 1, 6, 75, 172, 32767})
            check(BanItems.disabled(new ItemStack(ModItems.disperser_canister, 1, meta)) == BanItems.enabled, "all canister metadata restricted: " + meta);
        check(BanItems.disabled(new ItemStack(ModItems.glitch)) == BanItems.enabled, "Glitch availability follows profile");
        for(int meta = 0; meta <= 5; meta++)
            check(BanItems.disabled(new ItemStack(com.hbm.blocks.ModBlocks.block_cap, 1, meta)) == (BanItems.enabled && meta != 0), "bottle-cap block exact scope: " + meta);
        for(int meta = 0; meta <= 7; meta++)
            check(BanItems.disabled(new ItemStack(ModItems.ammo_secret, 1, meta)) == (BanItems.enabled && meta == 6), "secret ammo exact scope: " + meta);
        // Raw Forge additions must preserve empty draws, not reinsert disabled identities.
        int guardedHooks = 0;
        for(String hook : new String[]{net.minecraftforge.common.ChestGenHooks.VILLAGE_BLACKSMITH,
            net.minecraftforge.common.ChestGenHooks.MINESHAFT_CORRIDOR, net.minecraftforge.common.ChestGenHooks.DUNGEON_CHEST,
            net.minecraftforge.common.ChestGenHooks.PYRAMID_DESERT_CHEST, net.minecraftforge.common.ChestGenHooks.PYRAMID_JUNGLE_CHEST,
            net.minecraftforge.common.ChestGenHooks.BONUS_CHEST}) {
            for(WeightedRandomChestContent entry : net.minecraftforge.common.ChestGenHooks.getItems(hook,new Random(1))) {
                if(entry instanceof BanItems.EmptyLootEntry) guardedHooks++;
                if(BanItems.disabled(entry.theItemId)) check(entry instanceof BanItems.EmptyLootEntry, "Forge hook no-drop guard: " + hook);
            }
        }
        check(guardedHooks == (BanItems.enabled ? 9 : 0), "nine selected Forge hook entries guarded; disabled profile unchanged");
        net.minecraft.world.WorldServer lootWorld = MinecraftServer.getServer().worldServerForDimension(0);
        com.hbm.itempool.ItemPool pedestalPool = ItemPool.pools.get("POOL_RED_PEDESTAL");
        WeightedRandomChestContent[] originalPedestal = pedestalPool.pool;
        try {
            pedestalPool.pool = new WeightedRandomChestContent[]{BanItems.loot(new WeightedRandomChestContent(ModItems.ammo_standard,4,1,1,1))};
            com.hbm.blocks.generic.BlockKeyhole.spawnPedestalItem(lootWorld,32,120,32);
            com.hbm.blocks.generic.BlockPedestal.TileEntityPedestal tile = (com.hbm.blocks.generic.BlockPedestal.TileEntityPedestal)lootWorld.getTileEntity(32,120,32);
            check((tile.item == null) == BanItems.enabled, "pedestal chosen banned roll stays empty without reroll");
            tile.item = new ItemStack(ModItems.ammo_standard,1,4);
            net.minecraft.nbt.NBTTagCompound persisted = new net.minecraft.nbt.NBTTagCompound(); tile.writeToNBT(persisted);
            check(persisted.hasKey("item") != BanItems.enabled, "pedestal write excludes banned metadata");
            new ItemStack(ModItems.ammo_standard,1,4).writeToNBT(persisted.getCompoundTag("item"));
            net.minecraft.nbt.NBTTagCompound savedItem = new net.minecraft.nbt.NBTTagCompound();
            new ItemStack(ModItems.ammo_standard,1,4).writeToNBT(savedItem);persisted.setTag("item",savedItem);
            tile.readFromNBT(persisted);check((tile.item == null) == BanItems.enabled, "pedestal read excludes banned metadata");
            tile.item = new ItemStack(ModItems.ammo_standard,1,4);tile.updateEntity();
            check((tile.item == null) == BanItems.enabled, "pedestal tick excludes directly inserted banned metadata");
            tile.item = new ItemStack(ModItems.ammo_standard,1,75);tile.writeToNBT(persisted);tile.readFromNBT(persisted);
            check(tile.item != null && tile.item.getItemDamage() == 75, "pedestal retains allowed metadata through save/read");
        } finally { pedestalPool.pool = originalPedestal; lootWorld.setBlockToAir(32,120,32); }
        com.hbm.itempool.ItemPool pillPool = ItemPool.pools.get("POOL_PILE_MED_PILLS");
        com.hbm.itempool.ItemPool syringePool = ItemPool.pools.get("POOL_PILE_MED_SYRINGE");
        WeightedRandomChestContent[] originalPills=pillPool.pool, originalSyringes=syringePool.pool;
        try {
            pillPool.pool=BanItems.noLoot();syringePool.pool=BanItems.noLoot();
            lootWorld.setBlock(32,120,32,com.hbm.blocks.ModBlocks.deco_loot);
            com.hbm.blocks.generic.BlockLoot.TileEntityLoot tile = (com.hbm.blocks.generic.BlockLoot.TileEntityLoot)lootWorld.getTileEntity(32,120,32);
            com.hbm.util.LootGenerator.applyLoot(lootWorld,32,120,32,com.hbm.util.LootGenerator.LOOT_MEDICINE);
            check(tile.items.isEmpty() == BanItems.enabled, "empty restricted medicine layout cannot fall through; legacy behavior retained");
        } finally { pillPool.pool=originalPills;syringePool.pool=originalSyringes;lootWorld.setBlockToAir(32,120,32); }
        Item foreign = new Item().setUnlocalizedName("gun_amat");
        check(!BanItems.disabled(foreign), "foreign unregistered item is not mistaken for HBM by unlocalized name");
        if(BanItems.enabled) {
            for(net.minecraft.item.crafting.IRecipe recipe : (List<net.minecraft.item.crafting.IRecipe>)net.minecraft.item.crafting.CraftingManager.getInstance().getRecipeList())
                check(!BanItems.disabled(recipe.getRecipeOutput()), "crafting result allowed");
            for(SerializableRecipe handler : SerializableRecipe.recipeHandlers)
                check(!BanItemsRecipes.contains(handler.getRecipeObject()), "machine recipe data allowed: " + handler.getClass().getSimpleName());
            check(!BanItemsRecipes.contains(com.hbm.inventory.recipes.anvil.AnvilRecipes.smithingRecipes), "anvil smithing dependencies");
            check(!BanItemsRecipes.contains(com.hbm.inventory.recipes.anvil.AnvilRecipes.constructionRecipes), "anvil construction dependencies");
            for(com.hbm.inventory.FluidContainer container : com.hbm.inventory.FluidContainerRegistry.allContainers)
                check(!BanItems.disabled(container.fullContainer) && !BanItems.disabled(container.emptyContainer), "fluid conversion allowed");
            for(String key : net.minecraftforge.oredict.OreDictionary.getOreNames()) for(ItemStack stack : net.minecraftforge.oredict.OreDictionary.getOres(key))
                check(!BanItems.disabled(stack), "ore dictionary stack allowed: " + key);
            for(Object object : net.minecraft.stats.AchievementList.achievementList)
                check(!BanItems.disabled(((net.minecraft.stats.Achievement)object).theItemStack), "achievement icon remains registered");
            check(!BanItemsRecipes.contains(com.hbm.inventory.recipes.MagicRecipes.getRecipes()), "magic recipes have no removed dependencies");
            check(!BanItemsRecipes.contains(com.hbm.inventory.recipes.LemegetonRecipes.recipes), "lemegeton recipes have no removed dependencies");
            JsonParser parser = new JsonParser();
            check(BanItemsRecipes.containsJson(parser.parse("[\"hbm:item.gun_b92\",1,0]")), "JSON whole-item output omitted");
            check(BanItemsRecipes.containsJson(parser.parse("[\"hbm:item.ammo_standard\",1,4]")), "JSON selected ammo output omitted");
            check(!BanItemsRecipes.containsJson(parser.parse("[\"hbm:item.ammo_standard\",1,75]")), "JSON retained ammo output preserved");
            check(BanItemsRecipes.containsJson(parser.parse("[\"item\",\"hbm:item.ammo_standard\",1,4]")), "JSON selected AStack input omitted");
            check(!BanItemsRecipes.containsJson(parser.parse("[\"item\",\"hbm:item.ammo_standard\",1,75]")), "JSON retained AStack input preserved");
            com.hbm.tileentity.TileEntityInventoryBase machine = new com.hbm.tileentity.TileEntityInventoryBase(2) {
                public String getName() { return "test"; }
            };
            machine.setInventorySlotContents(0, new ItemStack(ModItems.ammo_standard, 1, 4));
            machine.setInventorySlotContents(1, new ItemStack(ModItems.ammo_standard, 1, 75));
            check(machine.slots[0] == null && machine.slots[1] != null, "machine ingress rejects banned exact metadata");
            Random random = new Random(1337);
            int suppressed = 0;
            for(ItemPool pool : ItemPool.pools.values()) for(WeightedRandomChestContent entry : pool.pool) if(BanItems.disabled(entry.theItemId)) {
                suppressed++;
                TileEntityChest inventory = new TileEntityChest();
                WeightedRandomChestContent[] single = { entry };
                WeightedRandomChestContent.generateChestContents(random, single, inventory, 5);
                for(int slot = 0; slot < inventory.getSizeInventory(); slot++) check(inventory.getStackInSlot(slot) == null, "suppressed chest draw is empty");
                check(ItemPool.getStack(single, random) == null, "suppressed direct draw is empty");
            }
            check(suppressed > 100, "affected loot entries were retained as empty draws");
            check(ItemPool.getStack("POOL_WEAPONS", random) == null, "all-disabled weapon pool does not crash or substitute");
            check(ItemPool.getStack("missing-pool-test", random) == null, "missing pool has no vanilla fallback");
            TileEntityChest missing = new TileEntityChest();
            WeightedRandomChestContent.generateChestContents(random, ItemPool.getPool("missing-pool-test"), missing, 10);
            new ItemPool("empty-pool-test").build();
            WeightedRandomChestContent.generateChestContents(random, ItemPool.getPool("empty-pool-test"), missing, 10);
            for(int slot = 0; slot < missing.getSizeInventory(); slot++) check(missing.getStackInSlot(slot) == null, "empty/missing pool chest generation produces no placeholder");
            ItemStack oldBlock = com.hbm.config.ItemPoolConfigJSON.readItemStack(new JsonParser().parse("[\"hbm:tile.nuke_custom\",1,0]").getAsJsonArray());
            check(BanItems.disabled(oldBlock), "old removed-block JSON preserves banned identity");
            check(Item.getItemFromBlock(com.hbm.blocks.ModBlocks.nuke_custom) == null, "data identity does not restore global ItemBlock mapping");
            TileEntityChest inventory = new TileEntityChest();
            inventory.setInventorySlotContents(0, new ItemStack(ModItems.ammo_standard, 1, 4));
            inventory.setInventorySlotContents(1, new ItemStack(ModItems.ammo_standard, 1, 75));
            BanItems.clean(inventory);
            check(inventory.getStackInSlot(0) == null && inventory.getStackInSlot(1) != null, "inventory filter preserves retained metadata");
            com.hbm.entity.mob.EntityUFO ufo = new com.hbm.entity.mob.EntityUFO(MinecraftServer.getServer().worldServerForDimension(0));
            net.minecraftforge.event.entity.EntityJoinWorldEvent spawn = new net.minecraftforge.event.entity.EntityJoinWorldEvent(ufo, ufo.worldObj);
            check(net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(spawn) && spawn.isCanceled(), "boss world-entry gate runs in real Forge");
        }
        for(String line : Files.readAllLines(Paths.get("pool-baseline.csv"))) {
            if(line.startsWith("pool,") || line.trim().isEmpty()) continue;
            String[] parts = line.split(",");
            ItemPool pool = ItemPool.pools.get(parts[0]);
            check(pool != null && pool.pool.length == Integer.parseInt(parts[1]), "original entry count retained: " + parts[0]);
            int weight = 0; for(WeightedRandomChestContent entry : pool.pool) weight += entry.itemWeight;
            check(weight == Integer.parseInt(parts[4]), "original probability mass retained: " + parts[0]);
        }
    }
}
