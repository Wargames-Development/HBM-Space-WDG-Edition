package com.hbm.config;

import java.lang.reflect.*;
import java.util.*;
import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.recipes.loader.*;
import net.minecraft.block.Block;
import net.minecraft.item.*;
import net.minecraft.item.crafting.*;
import net.minecraftforge.oredict.*;

/** Inspects recipe data only, without changing unrelated quantities or ingredients. */
public final class BanItemsRecipes {
    private BanItemsRecipes() { }
    public static boolean contains(Object value) {
        return BanItems.enabled && contains(value, Collections.newSetFromMap(new IdentityHashMap<Object,Boolean>()));
    }
    private static boolean contains(Object value, Set<Object> seen) {
        if(value == null || !seen.add(value)) return false;
        if(value instanceof ItemStack) return BanItems.disabled((ItemStack)value);
        if(value instanceof Item) return BanItems.disabled((Item)value);
        if(value instanceof Block) return BanItems.disabled((Block)value);
        if(value instanceof ComparableStack) return ((ComparableStack)value).item == null || BanItems.disabled(((ComparableStack)value).toStack());
        if(value instanceof AStack) {
            List<ItemStack> matches = ((AStack)value).extractForNEI();
            return !matches.isEmpty() && matches.stream().allMatch(BanItems::disabled);
        }
        if(value instanceof Map.Entry) return contains(((Map.Entry)value).getKey(), seen) || contains(((Map.Entry)value).getValue(), seen);
        if(value instanceof Map) return contains(((Map)value).entrySet(), seen);
        if(value instanceof Iterable) { for(Object element : (Iterable)value) if(contains(element, seen)) return true; return false; }
        if(value.getClass().isArray()) {
            if(value.getClass().getComponentType().isPrimitive()) return false;
            for(Object element : (Object[])value) if(contains(element, seen)) return true;
            return false;
        }
        // Recipe wrappers expose their data publicly. Never walk entity/world/mod singletons.
        String type = value.getClass().getName();
        if(type.startsWith("com.hbm.inventory.recipes.") || type.startsWith("com.hbm.util.Tuple$")) {
            for(Field field : dataFields(value.getClass())) {
                if(Modifier.isStatic(field.getModifiers())) continue;
                try { if(contains(field.get(value), seen)) return true; }
                catch(IllegalAccessException e) { throw new IllegalStateException("Cannot inspect recipe field " + field, e); }
            }
        }
        return false;
    }
    private static final Map<Class<?>, List<Field>> fields = new HashMap<Class<?>, List<Field>>();
    private static List<Field> dataFields(Class<?> type) {
        List<Field> result = fields.get(type);
        if(result != null) return result;
        result = new ArrayList<Field>();
        for(Class<?> current = type; current != null; current = current.getSuperclass()) {
            String name = current.getName();
            if(!name.startsWith("com.hbm.inventory.recipes.") && !name.startsWith("com.hbm.util.Tuple$")) break;
            for(Field field : current.getDeclaredFields()) {
                if(Modifier.isStatic(field.getModifiers()) || field.isSynthetic() || "icon".equals(field.getName())) continue;
                field.setAccessible(true); result.add(field);
            }
        }
        fields.put(type, result); return result;
    }
    public static boolean containsJson(com.google.gson.JsonElement value) {
        if(!BanItems.enabled || value == null || value.isJsonNull()) return false;
        if(value.isJsonObject()) {
            for(Map.Entry<String, com.google.gson.JsonElement> entry : value.getAsJsonObject().entrySet()) if(containsJson(entry.getValue())) return true;
        } else if(value.isJsonArray()) {
            com.google.gson.JsonArray array = value.getAsJsonArray();
            try {
                int index = array.size() > 0 && array.get(0).isJsonPrimitive() && ("item".equals(array.get(0).getAsString()) || "nbt".equals(array.get(0).getAsString())) ? 1 : 0;
                if(array.size() > index && array.get(index).isJsonPrimitive()) {
                    String name = array.get(index).getAsString();
                    if(BanItems.disabledName(name)) return true;
                    Item item = BanItems.findItem(name);
                    if(item != null) {
                        int meta = array.size() > index + 2 && array.get(index + 2).isJsonPrimitive() ? array.get(index + 2).getAsInt() : 0;
                        if(BanItems.disabled(new ItemStack(item, 1, meta))) return true;
                    }
                }
            } catch(IllegalStateException | NumberFormatException ignored) { /* Other JSON array shapes are inspected recursively. */ }
            for(com.google.gson.JsonElement entry : array) if(containsJson(entry)) return true;
        }
        return false;
    }
    public static void prune(Object recipes) {
        if(!BanItems.enabled || recipes == null) return;
        if(recipes instanceof Map) ((Map)recipes).entrySet().removeIf(BanItemsRecipes::contains);
        else if(recipes instanceof Collection) ((Collection)recipes).removeIf(BanItemsRecipes::contains);
        else throw new IllegalStateException("Unsupported recipe container: " + recipes.getClass().getName());
    }
    public static void filterHandler(SerializableRecipe handler) {
        if(!BanItems.enabled) return;
        if(handler instanceof GenericRecipes) {
            GenericRecipes recipes = (GenericRecipes)handler;
            prune(recipes.recipeOrderedList); prune(recipes.recipeNameMap);
            for(Object group : recipes.autoSwitchGroups.values()) prune(group);
            prune(GenericRecipes.nameToRecipeGlobal);
            for(List<String> pool : GenericRecipes.blueprintPools.values()) pool.removeIf(name -> !GenericRecipes.nameToRecipeGlobal.containsKey(name));
        } else prune(handler.getRecipeObject());
    }
    public static void finish() {
        if(!BanItems.enabled) return;
        for(String key : OreDictionary.getOreNames()) OreDictionary.getOres(key).removeIf(BanItems::disabled);
        OreDictionary.rebakeMap();
        List<IRecipe> recipes = CraftingManager.getInstance().getRecipeList();
        recipes.removeIf(recipe -> {
            if(BanItems.disabled(recipe.getRecipeOutput())) return true;
            if(recipe instanceof ShapedRecipes) return contains(((ShapedRecipes)recipe).recipeItems);
            if(recipe instanceof ShapelessRecipes) return contains(((ShapelessRecipes)recipe).recipeItems);
            if(recipe instanceof ShapedOreRecipe) return badOreIngredients(Arrays.asList(((ShapedOreRecipe)recipe).getInput()));
            if(recipe instanceof ShapelessOreRecipe) return badOreIngredients(((ShapelessOreRecipe)recipe).getInput());
            return false;
        });
        prune(FurnaceRecipes.smelting().getSmeltingList());
        prune(com.hbm.inventory.recipes.MagicRecipes.getRecipes());
        prune(com.hbm.inventory.recipes.LemegetonRecipes.recipes);
        prune(com.hbm.inventory.recipes.anvil.AnvilRecipes.smithingRecipes);
        prune(com.hbm.inventory.recipes.anvil.AnvilRecipes.constructionRecipes);
        for(SerializableRecipe handler : SerializableRecipe.recipeHandlers) filterHandler(handler);
    }
    private static boolean badOreIngredients(List<?> ingredients) {
        for(Object ingredient : ingredients) {
            if(ingredient instanceof List) {
                List<?> choices = (List<?>)ingredient;
                if(choices.isEmpty() || choices.stream().allMatch(BanItemsRecipes::contains)) return true;
            } else if(contains(ingredient)) return true;
        }
        return false;
    }
}
