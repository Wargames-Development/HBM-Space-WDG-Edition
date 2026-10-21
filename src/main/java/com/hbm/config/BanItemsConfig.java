package com.hbm.config;

import com.hbm.entity.mob.EntityBFAngel;
import com.hbm.entity.mob.EntityHunterChopper;
import com.hbm.entity.mob.EntityUFO;
import com.hbm.entity.mob.botprime.EntityBOTPrimeBase;
import net.minecraft.entity.Entity;
import net.minecraftforge.common.config.Configuration;

/** Reversible boss/reward part of the reviewed BanItems selection.
 * Entity registrations remain stable for saved-world compatibility.
 */
public final class BanItemsConfig {
    private BanItemsConfig() { }

    public static boolean disableSelectedBosses = true;
    public static boolean disableSelectedBossRewards = true;
    public static boolean disableSmeltingBonusItems = true;

    public static void loadFromConfig(Configuration config) {
        String category = "wdg_banitems";
        BanItems.load(config);
        disableSelectedBosses = config.get(category, "disableSelectedBosses", true,
            "Block Hunter Chopper, BOTPrime (including segments), UFO and Radioactive Angel on world entry. Includes saved entities; back up worlds before enabling.").getBoolean(true);
        disableSelectedBossRewards = config.get(category, "disableSelectedBossRewards", true,
            "Suppress item drops from those four boss families, including direct UFO/worm coins, angel cores and chopper debris. Achievements and XP are retained.").getBoolean(true);
        disableSmeltingBonusItems = config.get(category, "disableSmeltingBonusItems", true,
            "Disable incidental lodestone and plutonic quartz smelting bonuses. Normal smelting output and achievements are retained.").getBoolean(true);
    }

    public static boolean isSelectedBossType(Class<? extends Entity> type) {
        return EntityHunterChopper.class.isAssignableFrom(type)
            || EntityBOTPrimeBase.class.isAssignableFrom(type)
            || EntityUFO.class.isAssignableFrom(type)
            || EntityBFAngel.class.isAssignableFrom(type);
    }

    public static boolean blocksSpawn(Entity entity) {
        return disableSelectedBosses && entity != null && isSelectedBossType(entity.getClass());
    }
}
