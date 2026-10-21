package com.hbm.config;

import java.io.InputStreamReader;
import java.util.*;
import com.google.gson.*;
import com.hbm.items.ModItems;
import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.block.Block;
import net.minecraft.item.*;
import net.minecraft.inventory.IInventory;
import net.minecraft.util.WeightedRandomChestContent;
import net.minecraftforge.common.config.Configuration;

/** Startup-only availability policy. Objects and enum ordinals remain intact. */
public final class BanItems {
    private BanItems() { }
    public static boolean enabled = true;
    private static final Set<String> names = new HashSet<String>();
    private static final Set<String> defaultNames = new HashSet<String>();
    private static final List<String> defaultMetadata = new ArrayList<String>();
    private static final Map<Block, String> internalBlocks = new IdentityHashMap<Block, String>();
    private static final Map<String, Set<Integer>> metas = new HashMap<String, Set<Integer>>();
    private static final Map<String, Item> internalItems = new HashMap<String, Item>();
    private static final Map<Item, String> internalNames = new IdentityHashMap<Item, String>();
    static {
        try(InputStreamReader reader = new InputStreamReader(BanItems.class.getResourceAsStream("/assets/hbm/banitems-default.json"), "UTF-8")) {
            JsonObject defaults = new JsonParser().parse(reader).getAsJsonObject();
            for(JsonElement e : defaults.getAsJsonArray("disabled")) { names.add(e.getAsString()); defaultNames.add(e.getAsString()); }
            for(JsonElement e : defaults.getAsJsonArray("metadata")) { addMeta(e.getAsString()); defaultMetadata.add(e.getAsString()); }
        } catch(Exception e) { throw new ExceptionInInitializerError(e); }
    }
    public static void load(Configuration config) {
        enabled = config.get("wdg_banitems", "enableItemProfile", true,
            "Startup-only BanItems registry/metadata policy. Match this on server and clients. Restore removed names and restart to re-enable content; back up saves before changing registrations.").getBoolean(true);
        String[] defaults = defaultNames.toArray(new String[0]); Arrays.sort(defaults);
        String[] configured = config.get("wdg_banitems", "disabledRegistrations", defaults,
            "Exact hbm registry names. Removing a name restores its registration on next launch. Block names suppress both Block and ItemBlock registration.").getStringList();
        names.clear();
        for(String name : configured) {
            validateName(name);
            if(!defaultNames.contains(name)) throw new IllegalArgumentException("Name outside curated BanItems registration hooks: " + name);
            names.add(name);
        }
        List<String> selected = new ArrayList<String>(defaultMetadata);
        Collections.sort(selected);
        configured = config.get("wdg_banitems", "disabledMetadata", selected.toArray(new String[0]),
            "Exact hbm registry@metadata variants; shared parents and unlisted variants stay registered. Do not remove or reorder enum entries.").getStringList();
        metas.clear(); for(String entry : configured) {
            int separator = entry.lastIndexOf('@');
            if(separator < 0) throw new IllegalArgumentException("Expected registry@metadata: " + entry);
            String parent = entry.substring(0, separator);
            boolean supported = false;
            for(String original : defaultMetadata) if(original.startsWith(parent + "@")) { supported = true; break; }
            if(!supported) throw new IllegalArgumentException("Name outside audited metadata hooks: " + entry);
            addMeta(entry);
        }
    }
    private static void validateName(String name) {
        if(!name.matches("hbm:(item|tile)\\.[a-zA-Z0-9_]+")) throw new IllegalArgumentException("Invalid BanItems name: " + name);
    }
    private static void addMeta(String entry) {
        int separator = entry.lastIndexOf('@');
        if(separator < 0) throw new IllegalArgumentException("Expected registry@metadata: " + entry);
        String name = entry.substring(0, separator); validateName(name);
        int meta = Integer.parseInt(entry.substring(separator + 1));
        if(meta < 0 || meta >= 32767) throw new IllegalArgumentException("Exact metadata required: " + entry);
        Set<Integer> set = metas.get(name); if(set == null) metas.put(name, set = new HashSet<Integer>()); set.add(meta);
    }
    public static boolean disabledName(String name) { return enabled && names.contains(name); }
    public static String name(Item item) {
        if(item == null) return null;
        String registered = Item.itemRegistry.getNameForObject(item);
        return registered != null ? registered : internalNames.get(item);
    }
    public static boolean disabled(Item item) { return item != null && disabledName(name(item)); }
    public static boolean disabled(Block block) {
        if(block == null) return false;
        String registered = Block.blockRegistry.getNameForObject(block);
        return disabledName(registered != null ? registered : internalBlocks.get(block));
    }
    public static boolean disabled(ItemStack stack) {
        if(!enabled || stack == null) return false;
        if(stack.getItem() == null) return true;
        String name = name(stack.getItem());
        if(names.contains(name)) return true;
        Set<Integer> selected = metas.get(name);
        return selected != null && selected.contains(stack.getItemDamage());
    }
    public static void registerItem(Item item, String name) {
        internalItems.put("hbm:" + name, item);
        internalNames.put(item, "hbm:" + name);
        if(!disabledName("hbm:" + name)) GameRegistry.registerItem(item, name);
    }
    public static Block registerBlock(Block block, String name) {
        return registerBlock(block, ItemBlock.class, name);
    }
    public static Block registerBlock(Block block, Class<? extends ItemBlock> itemClass, String name, Object... args) {
        internalBlocks.put(block, "hbm:" + name);
        if(!disabledName("hbm:" + name)) return GameRegistry.registerBlock(block, itemClass, name, args);
        // Data-only identity for old JSON loot/recipes; never register it or attach it to the global block-item map.
        Item record = new ItemBlock(block);
        internalItems.put("hbm:" + name, record); internalNames.put(record, "hbm:" + name);
        return block;
    }
    public static Item itemForData(Block block) {
        Item registered = Item.getItemFromBlock(block);
        return registered != null ? registered : internalItems.get(internalBlocks.get(block));
    }
    public static Item findItem(String name) {
        Item registered = (Item)Item.itemRegistry.getObject(name);
        return registered != null ? registered : internalItems.get(name);
    }
    public static void registerOre(String key, Item item) { if(!disabled(item)) net.minecraftforge.oredict.OreDictionary.registerOre(key, item); }
    public static void registerOre(String key, Block block) { if(!disabled(block)) net.minecraftforge.oredict.OreDictionary.registerOre(key, block); }
    public static void registerOre(String key, ItemStack stack) { if(!disabled(stack)) net.minecraftforge.oredict.OreDictionary.registerOre(key, stack); }
    public static Item achievementIcon(Item item) { return disabled(item) ? ModItems.achievement_icon : item; }
    public static Item achievementIcon(Block block) { return disabled(block) ? ModItems.achievement_icon : Item.getItemFromBlock(block); }
    public static void addCreative(List list, ItemStack stack) { if(!disabled(stack)) list.add(stack); }
    public static void addLoot(Collection<ItemStack> list, ItemStack stack) { if(stack != null && !disabled(stack)) list.add(stack); }
    public static ItemStack copyAllowed(ItemStack stack) { return stack == null || disabled(stack) ? null : stack.copy(); }
    public static List<ItemStack> selectedVariants() {
        List<ItemStack> stacks = new ArrayList<ItemStack>();
        if(enabled) for(Map.Entry<String, Set<Integer>> entry : metas.entrySet()) {
            Item item = findItem(entry.getKey());
            if(item != null) for(int meta : entry.getValue()) stacks.add(new ItemStack(item, 1, meta));
        }
        return stacks;
    }
    public static void clean(IInventory inventory) {
        if(!enabled || inventory == null) return;
        boolean changed = false;
        for(int slot = 0; slot < inventory.getSizeInventory(); slot++)
            if(disabled(inventory.getStackInSlot(slot))) { inventory.setInventorySlotContents(slot, null); changed = true; }
        if(changed) inventory.markDirty();
    }
    /** Keeps the original probability mass while generating zero stacks. */
    public static final class EmptyLootEntry extends WeightedRandomChestContent {
        private EmptyLootEntry(ItemStack source, int min, int max, int weight) { super(source, min, max, weight); }
        @Override protected ItemStack[] generateChestContent(Random random, IInventory inventory) { return new ItemStack[0]; }
    }
    public static WeightedRandomChestContent[] noLoot() {
        // The internal marker is never inserted into inventories or persisted as a pool entry.
        return new WeightedRandomChestContent[] { new EmptyLootEntry(new ItemStack(ModItems.nothing), 0, 0, 1) };
    }
    public static WeightedRandomChestContent loot(WeightedRandomChestContent entry) {
        if(!disabled(entry.theItemId)) return entry;
        return new EmptyLootEntry(entry.theItemId, entry.theMinimumChanceToGenerateItem, entry.theMaximumChanceToGenerateItem, entry.itemWeight);
    }
}
