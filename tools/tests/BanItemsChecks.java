import com.hbm.config.BanItemsConfig;
import com.hbm.entity.mob.*;
import com.hbm.entity.mob.botprime.*;
import net.minecraft.entity.Entity;
import net.minecraftforge.common.config.Configuration;
import java.io.File;
import java.nio.file.Files;

public class BanItemsChecks {
    private static int checks;
    private static void check(boolean value, String name) {
        if(!value) throw new AssertionError(name);
        checks++;
    }
    public static void main(String[] args) throws Exception {
        Class<?>[] blocked = { EntityHunterChopper.class, EntityUFO.class,
            EntityBFAngel.class, EntityBOTPrimeHead.class, EntityBOTPrimeBody.class };
        for(Class<?> type : blocked)
            check(BanItemsConfig.isSelectedBossType(type.asSubclass(Entity.class)), "selected boss " + type.getName());
        check(!BanItemsConfig.isSelectedBossType(EntityDuck.class), "ordinary duck remains");
        check(!BanItemsConfig.isSelectedBossType(EntityCreeperNuclear.class), "nuclear creeper and retained ammo drop remain");
        check(!BanItemsConfig.isSelectedBossType(net.minecraft.entity.boss.EntityDragon.class), "vanilla boss remains");
        check(!BanItemsConfig.isSelectedBossType(EntityMaskMan.class), "unselected boss remains");
        check(!BanItemsConfig.blocksSpawn(null), "null entity is not a spawn");
        File directory = Files.createTempDirectory("hbm-banitems-config-").toFile();
        File path = new File(directory, "hbm.cfg");
        java.lang.reflect.Field home = cpw.mods.fml.relauncher.FMLInjectionData.class.getDeclaredField("minecraftHome");
        home.setAccessible(true);
        Object previousHome = home.get(null);
        home.set(null, directory);
        try {
            Configuration config = new Configuration(path);
            BanItemsConfig.loadFromConfig(config);
            check(BanItemsConfig.disableSelectedBosses, "boss suppression default");
            check(BanItemsConfig.disableSelectedBossRewards, "reward suppression default");
            check(BanItemsConfig.disableSmeltingBonusItems, "bonus suppression default");
            config.get("wdg_banitems", "disableSelectedBosses", true).set(false);
            config.get("wdg_banitems", "disableSelectedBossRewards", true).set(false);
            config.get("wdg_banitems", "disableSmeltingBonusItems", true).set(false);
            config.save();
            Configuration restored = new Configuration(path);
            restored.load();
            BanItemsConfig.loadFromConfig(restored);
            check(!BanItemsConfig.disableSelectedBosses, "boss suppression reversible after reload");
            check(!BanItemsConfig.disableSelectedBossRewards, "reward suppression reversible after reload");
            check(!BanItemsConfig.disableSmeltingBonusItems, "bonus suppression reversible after reload");
        } finally {
            home.set(null, previousHome);
            Files.deleteIfExists(path.toPath());
            Files.delete(directory.toPath());
        }
        System.out.println("PASS: " + checks + " BanItems policy assertions");
    }
}
