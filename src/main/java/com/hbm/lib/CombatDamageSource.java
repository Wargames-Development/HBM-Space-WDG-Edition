package com.hbm.lib;

import java.util.UUID;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EntityDamageSourceIndirect;
import net.minecraft.world.World;

/** Retains explosion provenance for WGQOL without adding a dependency on it. */
public final class CombatDamageSource extends EntityDamageSourceIndirect {
    private final DamageSource original;
    public final net.minecraft.item.ItemStack combatItem;
    private static final ThreadLocal<com.hbm.entity.missile.EntityMissileBaseNT> IMPACT = new ThreadLocal<com.hbm.entity.missile.EntityMissileBaseNT>();
    public static Entity currentMissile() { return IMPACT.get(); }
    /** Exact synchronous impact scope, restored even when a missile starts a nested explosion. */
    public static void impact(com.hbm.entity.missile.EntityMissileBaseNT missile, net.minecraft.util.MovingObjectPosition hit) {
        if (!cpw.mods.fml.common.Loader.isModLoaded("wgqol")) { missile.onMissileImpact(hit); return; }
        com.hbm.entity.missile.EntityMissileBaseNT previous = IMPACT.get();
        IMPACT.set(missile);
        try { missile.onMissileImpact(hit); }
        finally { if (previous == null) IMPACT.remove(); else IMPACT.set(previous); }
    }
    public static net.minecraft.item.ItemStack itemFor(Entity source) {
        if (source instanceof com.hbm.entity.missile.EntityMissileBaseNT)
            return ((com.hbm.entity.missile.EntityMissileBaseNT)source).getMissileItemForInfo();
        if (source != null && source.getEntityData().hasKey("hbm.combatItem", 10))
            return net.minecraft.item.ItemStack.loadItemStackFromNBT(source.getEntityData().getCompoundTag("hbm.combatItem"));
        return null;
    }
    /** Deferred blast entities retain the item's identity in their normal persistent Forge data. */
    public static void capture(Entity blast, UUID owner) {
        com.hbm.entity.missile.EntityMissileBaseNT missile = IMPACT.get();
        if (missile == null || !java.util.Objects.equals(owner, missile.getOwner())) return;
        net.minecraft.item.ItemStack item = itemFor(missile);
        if (item == null) return;
        net.minecraft.nbt.NBTTagCompound tag = new net.minecraft.nbt.NBTTagCompound();
        new net.minecraft.item.ItemStack(item.getItem(), 1, item.getItemDamage()).writeToNBT(tag);
        blast.getEntityData().setTag("hbm.combatItem", tag);
    }
    private CombatDamageSource(DamageSource original, Entity projectile, Entity owner) {
        super(original.getDamageType(), projectile, owner);
        this.original = original;
        this.combatItem = itemFor(projectile);
        if (original.isExplosion()) setExplosion();
        if (original.isProjectile()) setProjectile();
        if (original.isUnblockable()) setDamageBypassesArmor();
        if (original.isDamageAbsolute()) setDamageIsAbsolute();
        if (original.isFireDamage()) setFireDamage();
        if (original.isMagicDamage()) setMagicDamage();
        if (original.canHarmInCreative()) setDamageAllowedInCreativeMode();
        if (original.isDifficultyScaled()) setDifficultyScaled();
    }
    @Override public boolean isDifficultyScaled() { return original.isDifficultyScaled(); }
    @Override public float getHungerDamage() { return original.getHungerDamage(); }
    private static java.lang.reflect.Method ignite;
    private static boolean checked;
    public static void ignite(Entity victim, Entity projectile, int seconds) {
        if (victim.worldObj.isRemote) return;
        if (!checked) {
            checked = true;
            try { ignite = Class.forName("com.wdg.wgqol.stats.CombatService").getMethod("ignite", Entity.class, Entity.class, int.class); }
            catch (ReflectiveOperationException | LinkageError absent) { }
        }
        if (ignite != null) try { ignite.invoke(null, victim, projectile, seconds); }
        catch (ReflectiveOperationException error) { org.apache.logging.log4j.LogManager.getLogger("WGQOL").warn("Cannot record NTM fire provenance", error); }
    }
    public static DamageSource enrich(DamageSource original, Entity projectile, UUID ownerId, World world) {
        if (!cpw.mods.fml.common.Loader.isModLoaded("wgqol")) return original;
        com.hbm.entity.missile.EntityMissileBaseNT impact = IMPACT.get();
        if (projectile == null && impact != null && java.util.Objects.equals(ownerId, impact.getOwner())) projectile = impact;
        Entity owner = original.getEntity();
        if (owner == null && ownerId != null && world != null) owner = world.func_152378_a(ownerId);
        if (owner == null && projectile instanceof EntityLivingBase) owner = projectile;
        return new CombatDamageSource(original, projectile, owner);
    }
}
