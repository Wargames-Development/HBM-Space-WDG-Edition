package com.hbm.explosion;

import api.hbm.wgc.Integrations;
import net.minecraft.block.Block;
import net.minecraft.block.BlockFence;
import net.minecraft.util.MathHelper;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.ChunkPosition;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.DimensionManager;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

/** Server-thread terrain work for the three stock bunker busters. */
public final class BunkerBusterTerrain {
    private static final long TICK_BUDGET_NANOS = 4_000_000L;
    private static final int MAX_ACTIVE = 3;
    private static final int MAX_WAITING = 80;
    private static final int RESERVED_FINAL_BLASTS = 16;
    private static final int MAX_TARGETS = 32768;
    private static final int TRACE_SLICE = 128;
    private static final int APPLY_SLICE = 16;
    private static final double MIN_INCOMING = 0.05D;
    private static final double GOLDEN_ANGLE = Math.PI * (3D - Math.sqrt(5D));
    private static final Deque<BunkerBusterTerrain> WAITING = new ArrayDeque<>();
    private static final Deque<BunkerBusterTerrain> ACTIVE = new ArrayDeque<>();

    private final World world;
    private final UUID owner;
    private final Explosion explosion;
    private final double sourceDamage;
    private final double radius;
    private final int rayCount;
    private final Consumer<HashSet<ChunkPosition>> destroy;
    private final Map<ChunkPosition, Target> targets = new LinkedHashMap<>();
    private Set<ChunkCoordIntPair> protectedChunks;
    private Iterator<Map.Entry<ChunkPosition, Target>> pending;
    private int rayIndex;
    private Ray ray;

    public static void enqueue(World world, UUID owner, Explosion explosion, float strength, boolean trail,
            Consumer<HashSet<ChunkPosition>> destroy) {
        int limit = trail ? MAX_WAITING - RESERVED_FINAL_BLASTS : MAX_WAITING;
        if (world.isRemote || WAITING.size() >= limit) return;
        BunkerBusterTerrain job = new BunkerBusterTerrain(world, owner, explosion, strength, trail, destroy);
        if (trail) WAITING.addLast(job);
        else WAITING.addFirst(job);
    }

    public static void tick() {
        long start = System.nanoTime();
        while (System.nanoTime() - start < TICK_BUDGET_NANOS) {
            while (ACTIVE.size() < MAX_ACTIVE && !WAITING.isEmpty()) ACTIVE.addLast(WAITING.removeFirst());
            if (ACTIVE.isEmpty()) return;
            BunkerBusterTerrain job = ACTIVE.removeFirst();
            if (DimensionManager.getWorld(job.world.provider.dimensionId) != job.world) continue;
            if (!job.runSlice()) ACTIVE.addLast(job);
        }
    }

    public static void unload(World world) {
        if (world.isRemote) return;
        WAITING.removeIf(job -> job.world == world);
        ACTIVE.removeIf(job -> job.world == world);
    }

    private BunkerBusterTerrain(World world, UUID owner, Explosion explosion, float strength, boolean trail,
            Consumer<HashSet<ChunkPosition>> destroy) {
        this.world = world;
        this.owner = owner;
        this.explosion = explosion;
        this.destroy = destroy;
        this.radius = strength;
        this.sourceDamage = 1.35D * strength * strength;
        int resolution = strength <= 30F ? 20 : strength <= 60F ? 28 : 36;
        this.rayCount = trail ? 96 : 6 * resolution * resolution;
    }

    private boolean runSlice() {
        if (protectedChunks == null) {
            int x = MathHelper.floor_double(explosion.explosionX);
            int y = MathHelper.floor_double(explosion.explosionY);
            int z = MathHelper.floor_double(explosion.explosionZ);
            if (!Integrations.canDetonateWGC(owner, world, x, y, z)) return true;
            protectedChunks = Integrations.getExplosionProtectedChunksWGC(owner, world, x, z, (int) radius + 16);
        }
        if (pending == null) {
            traceSlice();
            return false;
        }
        applySlice();
        return !pending.hasNext();
    }

    private void traceSlice() {
        if (ray != null) ray.chunk = null;
        for (int step = 0; step < TRACE_SLICE; step++) {
            if (ray == null) {
                if (rayIndex == rayCount || targets.size() == MAX_TARGETS) {
                    pending = targets.entrySet().iterator();
                    return;
                }
                double directionY = 1D - 2D * (rayIndex + 0.5D) / rayCount;
                double horizontal = Math.sqrt(1D - directionY * directionY);
                double angle = rayIndex++ * GOLDEN_ANGLE;
                ray = new Ray(Math.cos(angle) * horizontal, directionY, Math.sin(angle) * horizontal);
            }
            if (!visit(ray)) ray = null;
        }
    }

    private boolean visit(Ray current) {
        if (current.distance >= radius || incoming(current.distance * current.distance, current.transmission) < MIN_INCOMING
                || current.y < 0 || current.y >= world.getHeight()
                || Integrations.isProtected(current.x, current.z, protectedChunks)
                || !current.findLoadedChunk()) return false;

        double length = Math.min(radius, current.exitDistance()) - current.distance;
        if (length > 0D) {
            Block block = current.chunk.getBlock(current.x & 15, current.y, current.z & 15);
            if (!block.isAir(world, current.x, current.y, current.z)) {
                double resistance = record(current, block);
                double materialLength = block instanceof BlockFence ? length * 0.25D : length;
                current.transmission *= Math.exp(-0.35D * resistance * materialLength);
            }
        }
        current.advance();
        return true;
    }

    private double record(Ray current, Block block) {
        int x = current.x;
        int y = current.y;
        int z = current.z;
        ChunkPosition position = new ChunkPosition(x, y, z);
        Target target = targets.get(position);
        int metadata = current.chunk.getBlockMetadata(x & 15, y, z & 15);
        double resistance;
        if (target != null && target.block == block && target.metadata == metadata) {
            resistance = target.resistance;
        } else {
            double nativeResistance = block.getExplosionResistance(explosion.exploder, world, x, y, z,
                    explosion.explosionX, explosion.explosionY, explosion.explosionZ);
            resistance = Integrations.bunkerBusterResistance(world, x, y, z, nativeResistance);
            resistance = Double.isFinite(resistance) ? Math.max(0D, resistance) : Double.POSITIVE_INFINITY;
        }

        double dx = x + 0.5D - explosion.explosionX;
        double dy = y + 0.5D - explosion.explosionY;
        double dz = z + 0.5D - explosion.explosionZ;
        double distanceSquared = dx * dx + dy * dy + dz * dz;
        double damage = incoming(distanceSquared, current.transmission);
        if (!block.getMaterial().isLiquid() && distanceSquared <= radius * radius && damage >= MIN_INCOMING
                && (explosion.exploder == null
                    || explosion.exploder.func_145774_a(explosion, world, x, y, z, block, (float) damage))) {
            if (target == null || target.block != block || target.metadata != metadata) {
                if (!targets.containsKey(position) && targets.size() == MAX_TARGETS) return resistance;
                target = new Target(block, metadata, resistance);
                targets.put(position, target);
            }
            // Multiple rays from the same blast contribute only the highest arriving damage.
            target.damage = Math.max(target.damage, damage);
        }
        return resistance;
    }

    private double incoming(double distanceSquared, double transmission) {
        return sourceDamage * transmission / Math.max(1D, distanceSquared);
    }

    private void applySlice() {
        Map<ChunkPosition, Double> damage = new LinkedHashMap<>();
        Map<ChunkPosition, Target> expected = new LinkedHashMap<>();
        for (int i = 0; i < APPLY_SLICE && pending.hasNext(); i++) {
            Map.Entry<ChunkPosition, Target> entry = pending.next();
            ChunkPosition position = entry.getKey();
            Target target = entry.getValue();
            pending.remove();
            if (!matches(position, target)) continue;
            damage.put(position, target.damage);
            expected.put(position, target);
        }
        if (damage.isEmpty()) return;
        HashSet<ChunkPosition> approved = new HashSet<>();
        for (ChunkPosition position : Integrations.filterBunkerBusterTerrain(owner, world, explosion, damage)) {
            if (matches(position, expected.get(position))) approved.add(position);
        }
        if (!approved.isEmpty()) destroy.accept(approved);
    }

    private boolean matches(ChunkPosition position, Target target) {
        int x = position.chunkPosX;
        int y = position.chunkPosY;
        int z = position.chunkPosZ;
        return target != null && world.blockExists(x, y, z) && world.getBlock(x, y, z) == target.block
                && world.getBlockMetadata(x, y, z) == target.metadata;
    }

    private static final class Target {
        final Block block;
        final int metadata;
        final double resistance;
        double damage;

        Target(Block block, int metadata, double resistance) {
            this.block = block;
            this.metadata = metadata;
            this.resistance = resistance;
        }
    }

    private final class Ray {
        int x = MathHelper.floor_double(explosion.explosionX);
        int y = MathHelper.floor_double(explosion.explosionY);
        int z = MathHelper.floor_double(explosion.explosionZ);
        final int stepX, stepY, stepZ;
        final double deltaX, deltaY, deltaZ;
        double nextX, nextY, nextZ;
        double distance;
        double transmission = 1D;
        Chunk chunk;
        int chunkX, chunkZ;

        Ray(double dx, double dy, double dz) {
            stepX = dx >= 0D ? 1 : -1;
            stepY = dy >= 0D ? 1 : -1;
            stepZ = dz >= 0D ? 1 : -1;
            deltaX = delta(dx);
            deltaY = delta(dy);
            deltaZ = delta(dz);
            nextX = firstCrossing(explosion.explosionX, x, stepX, deltaX);
            nextY = firstCrossing(explosion.explosionY, y, stepY, deltaY);
            nextZ = firstCrossing(explosion.explosionZ, z, stepZ, deltaZ);
        }

        boolean findLoadedChunk() {
            int nextChunkX = x >> 4;
            int nextChunkZ = z >> 4;
            if (chunk != null && chunkX == nextChunkX && chunkZ == nextChunkZ) return true;
            if (!world.getChunkProvider().chunkExists(nextChunkX, nextChunkZ)) return false;
            chunkX = nextChunkX;
            chunkZ = nextChunkZ;
            chunk = world.getChunkFromChunkCoords(chunkX, chunkZ);
            return true;
        }

        double exitDistance() {
            return Math.min(nextX, Math.min(nextY, nextZ));
        }

        void advance() {
            distance = exitDistance();
            if (nextX == distance) {
                x += stepX;
                nextX += deltaX;
            }
            if (nextY == distance) {
                y += stepY;
                nextY += deltaY;
            }
            if (nextZ == distance) {
                z += stepZ;
                nextZ += deltaZ;
            }
        }
    }

    private static double delta(double direction) {
        return direction == 0D ? Double.POSITIVE_INFINITY : 1D / Math.abs(direction);
    }

    private static double firstCrossing(double origin, int block, int step, double delta) {
        if (Double.isInfinite(delta)) return delta;
        return (step > 0 ? block + 1D - origin : origin - block) * delta;
    }
}
