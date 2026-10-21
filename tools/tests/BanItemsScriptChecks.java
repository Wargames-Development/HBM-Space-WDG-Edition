import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import minetweaker.MineTweakerAPI;
import minetweaker.api.game.IGame;
import minetweaker.api.item.*;
import minetweaker.api.recipes.IRecipeManager;
import stanhebben.zenscript.*;

public class BanItemsScriptChecks {
    private static final List<IItemDefinition> definitions = new ArrayList<IItemDefinition>();
    private static final List<String> removals = new ArrayList<String>();
    private static IItemDefinition definition(final String id) {
        return (IItemDefinition)Proxy.newProxyInstance(BanItemsScriptChecks.class.getClassLoader(), new Class[]{IItemDefinition.class}, (proxy, method, args) -> {
            if(method.getName().equals("getId")) return id;
            if(method.getName().equals("makeStack")) {
                final int meta = (Integer)args[0];
                return Proxy.newProxyInstance(BanItemsScriptChecks.class.getClassLoader(),new Class[]{IItemStack.class}, (stack, call, values) -> {
                    if(call.getName().equals("getDamage")) return meta;
                    if(call.getName().equals("getDefinition")) return proxy;
                    return null;
                });
            }
            return null;
        });
    }
    public static void main(String[] args) throws Exception {
        MineTweakerAPI.game = (IGame)Proxy.newProxyInstance(BanItemsScriptChecks.class.getClassLoader(), new Class[]{IGame.class}, (proxy, method, values) -> method.getName().equals("getItems") ? definitions : null);
        MineTweakerAPI.recipes = (IRecipeManager)Proxy.newProxyInstance(BanItemsScriptChecks.class.getClassLoader(), new Class[]{IRecipeManager.class}, (proxy, method, values) -> {
            if(method.getName().equals("remove")) {
                IItemStack stack = (IItemStack)values[0];
                removals.add(stack.getDefinition().getId()+"@"+stack.getDamage());
            }
            if(method.getReturnType() == int.class) return 0;
            if(method.getReturnType() == boolean.class) return false;
            return null;
        });
        String staged = new String(Files.readAllBytes(Paths.get(args[0])), "UTF-8");
        String helper = staged.substring(0, staged.indexOf("// File generated"));
        String script = helper + "\nefrBanItemsRemoveRegistered(\"hbm:item.gun_b92\", 0);\nefrBanItemsRemoveRegistered(\"hbm:item.ammo_standard\", 75);\nefrBanItemsRemoveRegistered(\"hbm:tile.nuke_custom\", 32767);\n";
        Field field = minetweaker.runtime.GlobalRegistry.class.getDeclaredField("environment");field.setAccessible(true);
        ZenModule module = ZenModule.compileScriptString(script,"banitems-helper-test.zs",(IZenCompileEnvironment)field.get(null),BanItemsScriptChecks.class.getClassLoader());
        if(module == null) throw new AssertionError("No compiled script");
        Runnable compiled = module.getMain();
        if(compiled == null) throw new AssertionError("No compiled entry point");
        definitions.add(definition("hbm:item.ammo_standard"));
        compiled.run();
        if(!removals.equals(Arrays.asList("hbm:item.ammo_standard@75"))) throw new AssertionError("Missing IDs must be ignored without losing exact metadata: "+removals);
        definitions.add(definition("hbm:item.gun_b92"));definitions.add(definition("hbm:tile.nuke_custom"));
        compiled.run();
        if(!removals.equals(Arrays.asList("hbm:item.ammo_standard@75","hbm:item.gun_b92@0","hbm:item.ammo_standard@75","hbm:tile.nuke_custom@32767"))) throw new AssertionError("Restored IDs must regain their original recipe removals: "+removals);
        System.out.println("PASS: real CraftTweaker compiler/runtime adapter check; missing IDs, restored IDs, exact metadata and wildcard metadata");
    }
}
