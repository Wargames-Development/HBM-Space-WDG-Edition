package com.hbm.dim;

import net.minecraft.world.World;

/**
 * Keeps the original HBM Space atmosphere and rocket altitudes in 256-block worlds.
 *
 * Extended-height worlds shift the same atmospheric envelope upward by the
 * amount their effective vertical build height exceeds 256. Read the active
 * world instead of requiring or importing EFR+: EFR+ exposes its optional
 * 384-block Overworld through World#getHeight().
 *
 * In particular, do not use getHeightValue(x,z): terrain surface height varies
 * by location and does not describe the configured build height.
 */
public final class CelestialAltitudeCompat {

    private static final int LEGACY_HEIGHT = 256;

    private CelestialAltitudeCompat() { }

    public static int heightOffset(World world) {
        if(world == null) return 0;
        return Math.max(0, world.getHeight() - LEGACY_HEIGHT);
    }

    /** Rideable rocket's atmospheric exit threshold (vanilla: Y=900). */
    public static int departureY(World world) {
        return 900 + heightOffset(world);
    }

    /** Beginning of planetary descent after a celestial transfer (vanilla: Y=800). */
    public static int reentryStartY(World world) {
        return 800 + heightOffset(world);
    }

    /** Shared satellite rocket deployment and Soyuz capsule re-entry height (vanilla: Y=600). */
    public static int soyuzDeploymentY(World world) {
        return 600 + heightOffset(world);
    }

    /** Cargo pods must spawn above both the build envelope and their destination pad. */
    public static int cargoPodY(World world, int dockingY) {
        return Math.max(300 + heightOffset(world), dockingY + 32);
    }

    /** High-speed player atmospheric flame effects start above Y=300. */
    public static int reentryHeatStartY(World world) {
        return 300 + heightOffset(world);
    }

    /** Upper end of the high-speed player atmospheric flame effect. */
    public static int reentryHeatEndY(World world) {
        return 1000 + heightOffset(world);
    }

    /** High-atmosphere fog colour begins fading at Y=600. */
    public static int atmosphereFadeStartY(World world) {
        return 600 + heightOffset(world);
    }

    /** High-atmosphere fog colour completes fading at Y=1000. */
    public static int atmosphereFadeEndY(World world) {
        return 1000 + heightOffset(world);
    }

    /** The meteor visual effect is culled below this altitude (vanilla: Y=500). */
    public static int meteorEndY(World world) {
        return 500 + heightOffset(world);
    }
}
