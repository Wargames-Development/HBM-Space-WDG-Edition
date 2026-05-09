package api.hbm.wgc;

import com.hbm.explosion.vanillant.ExplosionVNT;
import com.wdg.wgcore.integration.api.WGCoreIntegrationAccess;
import com.wdg.wgcore.integration.model.ActionAttribution;
import com.wdg.wgcore.integration.model.ActionSourceType;
import com.wdg.wgcore.integration.model.ExplosionActionContext;
import com.wdg.wgcore.integration.model.ExplosionDecision;
import cpw.mods.fml.common.Loader;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.util.MathHelper;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.ChunkPosition;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static com.wdg.wgcore.integration.api.WGCoreIntegrationAccess.*;

/**
 * Stable HBM-facing facade for the optional WGCore integration.
 *
 * <p>The facade and no-op backend contain no WGCore bytecode references. The
 * WGCore-backed implementation is isolated in a separate package-private class
 * in this source file and is loaded by name only when WGCore is available.</p>
 */
public final class Integrations {
	public static boolean canTargetPlayerWGC(UUID party, UUID targetedPlayer, World world) {
		return WGCoreIntegrationAccess.canTargetPlayer(party,targetedPlayer, world);
	}
	public static boolean canHarmPlayerWGC(UUID party, UUID targetedPlayer, World world) {
		return WGCoreIntegrationAccess.canHarmPlayer(party,targetedPlayer,world);
	}
	public static boolean canTargetChunkWGC(UUID party, World world,ChunkCoordIntPair chunkCoords) {
		return WGCoreIntegrationAccess.canTargetChunk(party,world, chunkCoords.chunkXPos, chunkCoords.chunkZPos); //TODO
	}
	public static boolean canTargetBlockWGC(UUID party,World world, int x, int y, int z) {
		return WGCoreIntegrationAccess.canTargetBlock(party, world, x, y, z);
	}
	public static boolean canDetonateWGC(UUID party, World world, int x, int y, int z){
		/**if(party == null){
			System.out.println("Null UUID!");
		}
		else {
			System.out.println("Detonate UUID:" + party.toString());
		}*/
		return WGCoreIntegrationAccess.canDetonate(party, world, x, y, z);
	}
	public static boolean canExplodeChunkWGC(UUID party, World world, int chunkX, int chunkZ) {
		return WGCoreIntegrationAccess.canExplodeChunk(party, world, chunkX, chunkZ);
	}
	public static boolean canExplodeBlockWGC(UUID party, World world, int x, int z) {return canExplodeChunkWGC(party,world, x >> 4, z >> 4);}

    private static final Logger LOGGER = LogManager.getLogger("HBM/WGCore");
    private static final String WGCORE_MOD_ID = "wgcore";
    private static final String WGCORE_API_CLASS = "com.wdg.wgcore.integration.api.WGCoreIntegrationAccess";
    private static final String WGCORE_BACKEND_CLASS = "api.hbm.wgc.WGCoreIntegrationBackend";

    private Integrations() { }
    
	public static Set<ChunkCoordIntPair> getExplosionProtectedChunksWGC(UUID party, World world, int x, int z, int r){
		return WGCoreIntegrationAccess.getExplosionProtectedChunks(party,world,x,z,r);
	}
	public static List<ChunkPosition> filterExplosionAffectedBlocksWGC(UUID party,
																	   World world,
																	   Explosion explosion,
																	   List affectedBlocks,
																	   String explosionTypeId) {
		if (world == null) {
			return Collections.emptyList();
		}

    private static IntegrationBackend backend() {
        return BackendHolder.INSTANCE;
    }

    private static final class BackendHolder {
        private static final IntegrationBackend INSTANCE = createBackend();
    }

    private static IntegrationBackend createBackend() {
        boolean modLoaded = false;
        try {
            modLoaded = Loader.isModLoaded(WGCORE_MOD_ID);
        } catch (RuntimeException ignored) {
            // The API-presence check below remains authoritative.
        }

        boolean apiPresent = isClassPresent(WGCORE_API_CLASS);
        if (!modLoaded && !apiPresent) {
            LOGGER.info("WGCore is not installed; HBM will run without WDG ownership and protection checks.");
            return new NoOpIntegrationBackend();
        }

        if (!apiPresent) {
            throw incompatibleWGCore(null);
        }

        try {
            Class<?> backendClass = Class.forName(WGCORE_BACKEND_CLASS, true, Integrations.class.getClassLoader());
            IntegrationBackend backend = (IntegrationBackend) backendClass.newInstance();
            LOGGER.info("WGCore integration active; HBM ownership and protection checks are enabled.");
            return backend;
        } catch (ClassNotFoundException e) {
            throw incompatibleWGCore(e);
        } catch (InstantiationException e) {
            throw incompatibleWGCore(e);
        } catch (IllegalAccessException e) {
            throw incompatibleWGCore(e);
        } catch (ClassCastException e) {
            throw incompatibleWGCore(e);
        } catch (RuntimeException e) {
            throw incompatibleWGCore(e);
        } catch (LinkageError e) {
            throw incompatibleWGCore(e);
        }
    }

    private static boolean isClassPresent(String className) {
        try {
            Class.forName(className, false, Integrations.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        } catch (LinkageError error) {
            throw incompatibleWGCore(error);
        }
    }

    private static IllegalStateException incompatibleWGCore(Throwable cause) {
        String message = "HBM Space WDG detected WGCore, but its integration API is incompatible. "
            + "Install the WGCore build required by this HBM release, or remove WGCore to run "
            + "HBM without WDG claim protection.";
        return cause != null ? new IllegalStateException(message, cause) : new IllegalStateException(message);
    }

    public static boolean canTargetPlayerWGC(UUID party, UUID targetedPlayer, World world) {
        return backend().canTargetPlayer(party, targetedPlayer, world);
    }

    public static boolean canHarmPlayerWGC(UUID party, UUID targetedPlayer, World world) {
        return backend().canHarmPlayer(party, targetedPlayer, world);
    }

    public static boolean canTargetChunkWGC(UUID party, World world, ChunkCoordIntPair chunkCoords) {
        return backend().canTargetChunk(party, world, chunkCoords);
    }

    public static boolean canTargetBlockWGC(UUID party, World world, int x, int y, int z) {
        return backend().canTargetBlock(party, world, x, y, z);
    }

    public static boolean canDetonateWGC(UUID party, World world, int x, int y, int z) {
        return backend().canDetonate(party, world, x, y, z);
    }

    public static boolean canExplodeChunkWGC(UUID party, World world, int chunkX, int chunkZ) {
        return backend().canExplodeChunk(party, world, chunkX, chunkZ);
    }

    public static boolean canExplodeBlockWGC(UUID party, World world, int x, int z) {
        return canExplodeChunkWGC(party, world, x >> 4, z >> 4);
    }

    public static boolean canExplodeBlockWGC(UUID party, World world, double x, double z) {
        return canExplodeChunkWGC(party, world, ((int) Math.floor(x)) >> 4, ((int) Math.floor(z)) >> 4);
    }

    public static Set<ChunkCoordIntPair> getExplosionProtectedChunksWGC(UUID party, World world, int x, int z, int r) {
        return backend().getExplosionProtectedChunks(party, world, x, z, r);
    }

    public static List<ChunkPosition> filterExplosionAffectedBlocksWGC(UUID party,
                                                                       World world,
                                                                       Explosion explosion,
                                                                       List affectedBlocks,
                                                                       String explosionTypeId) {
        return backend().filterExplosionAffectedBlocks(party, world, explosion, affectedBlocks, explosionTypeId);
    }

    public static HashSet<ChunkPosition> filterExplosionVNTAffectedBlocksWGC(UUID party,
                                                                             World world,
                                                                             ExplosionVNT explosion,
                                                                             HashSet<ChunkPosition> affectedBlocks) {
        return backend().filterExplosionVNTAffectedBlocks(party, world, explosion, affectedBlocks);
    }

    public static boolean canIrradiateWGC(UUID party, World world, int chunkX, int chunkZ) {
        return backend().canIrradiate(party, world, chunkX, chunkZ);
    }

    public static Set<ChunkCoordIntPair> getRadProtectedChunksWGC(UUID party, World world, int chunkX, int chunkZ, int radius) {
        return backend().getRadProtectedChunks(party, world, chunkX, chunkZ, radius);
    }

    public static boolean canContaminateChunkWGC(UUID party, World world, int chunkX, int chunkZ) {
        return backend().canContaminateChunk(party, world, chunkX, chunkZ);
    }

    public static boolean canContaminateBlockWGC(UUID party, World world, int blockX, int blockZ) {
        return canContaminateChunkWGC(party, world, blockX >> 4, blockZ >> 4);
    }

    public static boolean canContaminateBlockWGC(UUID party, World world, double blockX, double blockZ) {
        return canContaminateChunkWGC(party, world, ((int) Math.floor(blockX)) >> 4, ((int) Math.floor(blockZ)) >> 4);
    }

    public static Set<ChunkCoordIntPair> getContamProtectedChunksWGC(UUID party, World world, int chunkX, int chunkZ, int radius) {
        return backend().getContamProtectedChunks(party, world, chunkX, chunkZ, radius);
    }

    public static boolean canCrossContaminateWGC(World world, ChunkCoordIntPair chunk1, ChunkCoordIntPair chunk2) {
        return backend().canCrossContaminate(world, chunk1, chunk2);
    }

    public static boolean canPlaceClaimLockedBlockWGC(UUID party, World world, int x, int y, int z) {
        return backend().canPlaceClaimLockedBlock(party, world, x, y, z);
    }

    public static UUID getChunkOwnerWGC(World world, ChunkCoordIntPair chunkCoords) {
        return backend().getChunkOwner(world, chunkCoords);
    }

    public static UUID getPlayerFaction(World world, UUID player) {
        return backend().getPlayerFaction(world, player);
    }

    public static boolean isProtected(int blockX, int blockZ, Set<ChunkCoordIntPair> protectedChunks) {
        if (protectedChunks == null || protectedChunks.isEmpty()) {
            return false;
        }
        return protectedChunks.contains(getChunkCoordIntPair(blockX, blockZ));
    }

    public static ChunkCoordIntPair getChunkCoordIntPair(int blockX, int blockZ) {
        return new ChunkCoordIntPair(blockX >> 4, blockZ >> 4);
    }
}

interface IntegrationBackend {
    boolean canTargetPlayer(UUID party, UUID targetedPlayer, World world);
    boolean canHarmPlayer(UUID party, UUID targetedPlayer, World world);
    boolean canTargetChunk(UUID party, World world, ChunkCoordIntPair chunkCoords);
    boolean canTargetBlock(UUID party, World world, int x, int y, int z);
    boolean canDetonate(UUID party, World world, int x, int y, int z);
    boolean canExplodeChunk(UUID party, World world, int chunkX, int chunkZ);
    Set<ChunkCoordIntPair> getExplosionProtectedChunks(UUID party, World world, int x, int z, int radius);
    List<ChunkPosition> filterExplosionAffectedBlocks(UUID party, World world, Explosion explosion, List affectedBlocks, String explosionTypeId);
    HashSet<ChunkPosition> filterExplosionVNTAffectedBlocks(UUID party, World world, ExplosionVNT explosion, HashSet<ChunkPosition> affectedBlocks);
    boolean canIrradiate(UUID party, World world, int chunkX, int chunkZ);
    Set<ChunkCoordIntPair> getRadProtectedChunks(UUID party, World world, int chunkX, int chunkZ, int radius);
    boolean canContaminateChunk(UUID party, World world, int chunkX, int chunkZ);
    Set<ChunkCoordIntPair> getContamProtectedChunks(UUID party, World world, int chunkX, int chunkZ, int radius);
    boolean canCrossContaminate(World world, ChunkCoordIntPair chunk1, ChunkCoordIntPair chunk2);
    boolean canPlaceClaimLockedBlock(UUID party, World world, int x, int y, int z);
    UUID getChunkOwner(World world, ChunkCoordIntPair chunkCoords);
    UUID getPlayerFaction(World world, UUID player);
}

final class NoOpIntegrationBackend implements IntegrationBackend {

    public boolean canTargetPlayer(UUID party, UUID targetedPlayer, World world) {
        return true;
    }

    public boolean canHarmPlayer(UUID party, UUID targetedPlayer, World world) {
        return true;
    }

    public boolean canTargetChunk(UUID party, World world, ChunkCoordIntPair chunkCoords) {
        return true;
    }

    public boolean canTargetBlock(UUID party, World world, int x, int y, int z) {
        return true;
    }

    public boolean canDetonate(UUID party, World world, int x, int y, int z) {
        return true;
    }

    public boolean canExplodeChunk(UUID party, World world, int chunkX, int chunkZ) {
        return true;
    }

    public Set<ChunkCoordIntPair> getExplosionProtectedChunks(UUID party, World world, int x, int z, int radius) {
        return new HashSet<ChunkCoordIntPair>();
    }

    public List<ChunkPosition> filterExplosionAffectedBlocks(UUID party,
                                                              World world,
                                                              Explosion explosion,
                                                              List affectedBlocks,
                                                              String explosionTypeId) {
        ArrayList<ChunkPosition> result = new ArrayList<ChunkPosition>();
        if (affectedBlocks != null) {
            for (Object object : affectedBlocks) {
                if (object instanceof ChunkPosition) {
                    result.add((ChunkPosition) object);
                }
            }
        }
        return result;
    }

    public HashSet<ChunkPosition> filterExplosionVNTAffectedBlocks(UUID party,
                                                                   World world,
                                                                   ExplosionVNT explosion,
                                                                   HashSet<ChunkPosition> affectedBlocks) {
        HashSet<ChunkPosition> result = new HashSet<ChunkPosition>();
        if (affectedBlocks != null) {
            result.addAll(affectedBlocks);
        }
        return result;
    }

    public boolean canIrradiate(UUID party, World world, int chunkX, int chunkZ) {
        return true;
    }

    public Set<ChunkCoordIntPair> getRadProtectedChunks(UUID party, World world, int chunkX, int chunkZ, int radius) {
        return new HashSet<ChunkCoordIntPair>();
    }

    public boolean canContaminateChunk(UUID party, World world, int chunkX, int chunkZ) {
        return true;
    }

    public Set<ChunkCoordIntPair> getContamProtectedChunks(UUID party, World world, int chunkX, int chunkZ, int radius) {
        return new HashSet<ChunkCoordIntPair>();
    }

    public boolean canCrossContaminate(World world, ChunkCoordIntPair chunk1, ChunkCoordIntPair chunk2) {
        return true;
    }

    public boolean canPlaceClaimLockedBlock(UUID party, World world, int x, int y, int z) {
        return true;
    }

    public UUID getChunkOwner(World world, ChunkCoordIntPair chunkCoords) {
        return null;
    }

    public UUID getPlayerFaction(World world, UUID player) {
        return null;
    }
}

/**
 * Direct, type-safe WGCore implementation. This class must never be referenced
 * directly by Integrations; it is loaded by name only when the WGCore API is
 * present, keeping the public facade loadable without WGCore at runtime.
 */
final class WGCoreIntegrationBackend implements IntegrationBackend {

    WGCoreIntegrationBackend() {
        verifyCompatibility();
    }

    private static void verifyCompatibility() {
        try {
            requireMethod("canTargetPlayer", UUID.class, UUID.class, World.class);
            requireMethod("canHarmPlayer", UUID.class, UUID.class, World.class);
            requireMethod("canTargetChunk", UUID.class, World.class, Integer.TYPE, Integer.TYPE);
            requireMethod("canTargetBlock", UUID.class, World.class, Integer.TYPE, Integer.TYPE, Integer.TYPE);
            requireMethod("canDetonate", UUID.class, World.class, Integer.TYPE, Integer.TYPE, Integer.TYPE);
            requireMethod("canExplodeChunk", UUID.class, World.class, Integer.TYPE, Integer.TYPE);
            requireMethod("getExplosionProtectedChunks", UUID.class, World.class, Integer.TYPE, Integer.TYPE, Integer.TYPE);
            requireMethod("evaluateExplosion", ExplosionActionContext.class);
            requireMethod("canIrradiateChunk", UUID.class, World.class, Integer.TYPE, Integer.TYPE);
            requireMethod("getRadProtectedChunks", UUID.class, World.class, Integer.TYPE, Integer.TYPE, Integer.TYPE);
            requireMethod("canContaminateChunk", UUID.class, World.class, Integer.TYPE, Integer.TYPE);
            requireMethod("getContamProtectedChunks", UUID.class, World.class, Integer.TYPE, Integer.TYPE, Integer.TYPE);
            requireMethod("canCrossContaminate", World.class, ChunkCoordIntPair.class, ChunkCoordIntPair.class);
            requireMethod("canPlaceClaimLockedBlock", UUID.class, World.class, Integer.TYPE, Integer.TYPE);
            requireMethod("getChunkOwner", World.class, Integer.TYPE, Integer.TYPE);
            requireMethod("getPlayerFaction", World.class, UUID.class);
        } catch (NoSuchMethodException error) {
            throw new IllegalStateException("WGCore integration API is missing a required method.", error);
        }
    }

    private static Method requireMethod(String name, Class<?>... parameterTypes) throws NoSuchMethodException {
        return WGCoreIntegrationAccess.class.getMethod(name, parameterTypes);
    }

    public boolean canTargetPlayer(UUID party, UUID targetedPlayer, World world) {
        return WGCoreIntegrationAccess.canTargetPlayer(party, targetedPlayer, world);
    }

    public boolean canHarmPlayer(UUID party, UUID targetedPlayer, World world) {
        return WGCoreIntegrationAccess.canHarmPlayer(party, targetedPlayer, world);
    }

    public boolean canTargetChunk(UUID party, World world, ChunkCoordIntPair chunkCoords) {
        return WGCoreIntegrationAccess.canTargetChunk(party, world, chunkCoords.chunkXPos, chunkCoords.chunkZPos);
    }

    public boolean canTargetBlock(UUID party, World world, int x, int y, int z) {
        return WGCoreIntegrationAccess.canTargetBlock(party, world, x, y, z);
    }

    public boolean canDetonate(UUID party, World world, int x, int y, int z) {
        return WGCoreIntegrationAccess.canDetonate(party, world, x, y, z);
    }

    public boolean canExplodeChunk(UUID party, World world, int chunkX, int chunkZ) {
        return WGCoreIntegrationAccess.canExplodeChunk(party, world, chunkX, chunkZ);
    }

    public Set<ChunkCoordIntPair> getExplosionProtectedChunks(UUID party, World world, int x, int z, int radius) {
        return WGCoreIntegrationAccess.getExplosionProtectedChunks(party, world, x, z, radius);
    }

    public List<ChunkPosition> filterExplosionAffectedBlocks(UUID party,
                                                              World world,
                                                              Explosion explosion,
                                                              List affectedBlocks,
                                                              String explosionTypeId) {
        if (world == null) {
            return Collections.emptyList();
        }

        List<ChunkPosition> safeAffectedBlocks = new ArrayList<ChunkPosition>();
        if (affectedBlocks != null) {
            for (Object object : affectedBlocks) {
                if (object instanceof ChunkPosition) {
                    safeAffectedBlocks.add((ChunkPosition) object);
                }
            }
        }

        if (safeAffectedBlocks.isEmpty()) {
            return Collections.emptyList();
        }

        ActionAttribution attribution = buildExplosionAttribution(party, world);
        ExplosionActionContext context = new ExplosionActionContext(
            world,
            (int) Math.floor(explosion != null ? explosion.explosionX : 0.0D),
            (int) Math.floor(explosion != null ? explosion.explosionY : 0.0D),
            (int) Math.floor(explosion != null ? explosion.explosionZ : 0.0D),
            explosion,
            attribution,
            explosionTypeId != null && !explosionTypeId.trim().isEmpty()
                ? explosionTypeId
                : "hbm:explosion_nt",
            safeAffectedBlocks
        );

        ExplosionDecision decision = WGCoreIntegrationAccess.evaluateExplosion(context);
        if (decision == null || !decision.isExplosionAllowed() || !decision.isBlockDamageAllowed()) {
            return Collections.emptyList();
        }

        if (decision.isFiltered()) {
            return decision.getFilteredAffectedBlocks();
        }

        return safeAffectedBlocks;
    }

    public HashSet<ChunkPosition> filterExplosionVNTAffectedBlocks(UUID party,
                                                                   World world,
                                                                   ExplosionVNT explosion,
                                                                   HashSet<ChunkPosition> affectedBlocks) {
        HashSet<ChunkPosition> result = new HashSet<ChunkPosition>();
        if (world == null || explosion == null) {
            if (affectedBlocks != null) {
                result.addAll(affectedBlocks);
            }
            return result;
        }

        ArrayList<ChunkPosition> wgcoreInputBlocks = new ArrayList<ChunkPosition>();
        if (affectedBlocks != null && !affectedBlocks.isEmpty()) {
            wgcoreInputBlocks.addAll(affectedBlocks);
        }

        HashSet<ChunkPosition> partialCandidates = collectExplosionVNTPartialCandidates(
            party,
            world,
            explosion,
            explosion.posX,
            explosion.posY,
            explosion.posZ,
            explosion.size
        );

        if (!partialCandidates.isEmpty()) {
            wgcoreInputBlocks.addAll(partialCandidates);
        }

        if (wgcoreInputBlocks.isEmpty()) {
            return result;
        }

        List<ChunkPosition> filteredBlocks = filterExplosionAffectedBlocks(
            party,
            world,
            explosion.compat,
            wgcoreInputBlocks,
            "hbm:explosion_vnt"
        );

        if (filteredBlocks != null) {
            result.addAll(filteredBlocks);
        }

        return result;
    }

    private HashSet<ChunkPosition> collectExplosionVNTPartialCandidates(UUID party,
                                                                        World world,
                                                                        ExplosionVNT explosion,
                                                                        double x,
                                                                        double y,
                                                                        double z,
                                                                        float size) {
        HashSet<ChunkPosition> candidates = new HashSet<ChunkPosition>();
        if (world == null || explosion == null || size <= 0.0F) {
            return candidates;
        }

        int resolution = 16;
        float stepSize = 0.3F;
        Set<ChunkCoordIntPair> protectedChunks = getExplosionProtectedChunks(
            party,
            world,
            (int) x,
            (int) z,
            (int) Math.ceil(size) + 16
        );

        for (int i = 0; i < resolution; ++i) {
            for (int j = 0; j < resolution; ++j) {
                for (int k = 0; k < resolution; ++k) {
                    if (i != 0 && i != resolution - 1
                        && j != 0 && j != resolution - 1
                        && k != 0 && k != resolution - 1) {
                        continue;
                    }

                    double d0 = (double) ((float) i / ((float) resolution - 1.0F) * 2.0F - 1.0F);
                    double d1 = (double) ((float) j / ((float) resolution - 1.0F) * 2.0F - 1.0F);
                    double d2 = (double) ((float) k / ((float) resolution - 1.0F) * 2.0F - 1.0F);
                    double d3 = Math.sqrt(d0 * d0 + d1 * d1 + d2 * d2);
                    if (d3 <= 0.0D) {
                        continue;
                    }

                    d0 /= d3;
                    d1 /= d3;
                    d2 /= d3;

                    float powerRemaining = size;
                    double currentX = x;
                    double currentY = y;
                    double currentZ = z;

                    while (powerRemaining > 0.0F) {
                        int blockX = MathHelper.floor_double(currentX);
                        int blockY = MathHelper.floor_double(currentY);
                        int blockZ = MathHelper.floor_double(currentZ);

                        if (Integrations.isProtected(blockX, blockZ, protectedChunks)) {
                            break;
                        }

                        Block block = world.getBlock(blockX, blockY, blockZ);
                        if (block != null && block.getMaterial() != Material.air) {
                            candidates.add(new ChunkPosition(blockX, blockY, blockZ));
                            float blockResistance = explosion.exploder != null
                                ? explosion.exploder.func_145772_a(explosion.compat, world, blockX, blockY, blockZ, block)
                                : block.getExplosionResistance(explosion.exploder, world, blockX, blockY, blockZ, x, y, z);
                            powerRemaining -= (blockResistance + 0.3F) * stepSize;
                        }

                        currentX += d0 * (double) stepSize;
                        currentY += d1 * (double) stepSize;
                        currentZ += d2 * (double) stepSize;
                        powerRemaining -= stepSize * 0.75F;
                    }
                }
            }
        }

        return candidates;
    }

    private static ActionAttribution buildExplosionAttribution(UUID party, World world) {
        if (party == null) {
            return new ActionAttribution(
                null,
                null,
                null,
                null,
                "hbm",
                ActionSourceType.EXPLOSIVE,
                true,
                null
            );
        }

        UUID factionId = WGCoreIntegrationAccess.getPlayerFaction(world, party);
        if (factionId != null) {
            return ActionAttribution.directPlayer(party, factionId, "hbm", ActionSourceType.EXPLOSIVE);
        }

        return new ActionAttribution(
            null,
            party,
            null,
            party,
            "hbm",
            ActionSourceType.EXPLOSIVE,
            true,
            null
        );
    }

    public boolean canIrradiate(UUID party, World world, int chunkX, int chunkZ) {
        return WGCoreIntegrationAccess.canIrradiateChunk(party, world, chunkX, chunkZ);
    }

    public Set<ChunkCoordIntPair> getRadProtectedChunks(UUID party, World world, int chunkX, int chunkZ, int radius) {
        return WGCoreIntegrationAccess.getRadProtectedChunks(party, world, chunkX, chunkZ, radius);
    }

    public boolean canContaminateChunk(UUID party, World world, int chunkX, int chunkZ) {
        return WGCoreIntegrationAccess.canContaminateChunk(party, world, chunkX, chunkZ);
    }

    public Set<ChunkCoordIntPair> getContamProtectedChunks(UUID party, World world, int chunkX, int chunkZ, int radius) {
        return WGCoreIntegrationAccess.getContamProtectedChunks(party, world, chunkX, chunkZ, radius);
    }

    public boolean canCrossContaminate(World world, ChunkCoordIntPair chunk1, ChunkCoordIntPair chunk2) {
        return WGCoreIntegrationAccess.canCrossContaminate(world, chunk1, chunk2);
    }

    public boolean canPlaceClaimLockedBlock(UUID party, World world, int x, int y, int z) {
        return WGCoreIntegrationAccess.canPlaceClaimLockedBlock(party, world, x, z);
    }

    public UUID getChunkOwner(World world, ChunkCoordIntPair chunkCoords) {
        return WGCoreIntegrationAccess.getChunkOwner(world, chunkCoords.chunkXPos, chunkCoords.chunkZPos);
    }

    public UUID getPlayerFaction(World world, UUID player) {
        return WGCoreIntegrationAccess.getPlayerFaction(world, player);
    }
}
