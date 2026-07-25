package api.hbm.wgc;

import com.hbm.explosion.vanillant.ExplosionVNT;
import com.wdg.wgcore.integration.api.WGCoreIntegrationAccess;
import com.wdg.wgcore.integration.model.ActionAttribution;
import com.wdg.wgcore.integration.model.ActionSourceType;
import com.wdg.wgcore.integration.model.ExplosionActionContext;
import com.wdg.wgcore.integration.model.ExplosionDecision;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.util.MathHelper;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.ChunkPosition;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static com.wdg.wgcore.integration.api.WGCoreIntegrationAccess.*;

public class Integrations {
	public static boolean canTargetPlayerWGC(UUID party, UUID targetedPlayer, World world) {
		return canTargetPlayer(party,targetedPlayer, world);
	}
	public static boolean canHarmPlayerWGC(UUID party, UUID targetedPlayer, World world) {
		return canHarmPlayer(party,targetedPlayer,world);
	}
	public static boolean canTargetChunkWGC(UUID party, World world,ChunkCoordIntPair chunkCoords) {
		return canTargetChunk(party,world, chunkCoords.chunkXPos, chunkCoords.chunkZPos); //TODO
	}
	public static boolean canTargetBlockWGC(UUID party,World world, int x, int y, int z) {
		return canTargetBlock(party, world, x, y, z);
	}
	public static boolean canDetonateWGC(UUID party, World world, int x, int y, int z){
		return canDetonate(party, world, x, y, z);
	}
	public static boolean canExplodeChunkWGC(UUID party, World world, int chunkX, int chunkZ) {
		return canExplodeChunk(party, world, chunkX, chunkZ);
	}
	public static boolean canExplodeBlockWGC(UUID party, World world, int x, int z) {return canExplodeChunkWGC(party,world, x >> 4, z >> 4);}

	public static boolean canExplodeBlockWGC(UUID party, World world, double x, double z) {
		return canExplodeChunkWGC(party,world, ((int)Math.floor(x)) >> 4, ((int)Math.floor(z)) >> 4);
	}

	public static Set<ChunkCoordIntPair> getExplosionProtectedChunksWGC(UUID party, World world, int x, int z, int r){
		return getExplosionProtectedChunks(party,world,x,z,r);
	}
	public static List<ChunkPosition> filterExplosionAffectedBlocksWGC(UUID party,
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

		ActionAttribution attribution = buildExplosionAttributionWGC(party, world);

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

	public static HashSet<ChunkPosition> filterExplosionVNTAffectedBlocksWGC(UUID party,
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

		HashSet<ChunkPosition> partialCandidates = collectExplosionVNTPartialCandidatesWGC(
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

		List<ChunkPosition> filteredBlocks = filterExplosionAffectedBlocksWGC(
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

	private static HashSet<ChunkPosition> collectExplosionVNTPartialCandidatesWGC(UUID party,
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

		Set<ChunkCoordIntPair> protectedChunks = getExplosionProtectedChunksWGC(
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

						if (isProtected(blockX, blockZ, protectedChunks)) {
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

	private static ActionAttribution buildExplosionAttributionWGC(UUID party, World world) {
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
			return ActionAttribution.directPlayer(
				party,
				factionId,
				"hbm",
				ActionSourceType.EXPLOSIVE
			);
		}

		/*
		 * Some HBM/NTM systems pass a faction-like owner party instead of a
		 * direct player UUID. If WGCore cannot resolve it as a player, preserve
		 * it as the owning faction id. If it is neither a player nor a faction,
		 * WGCore will safely deny protected actions.
		 */
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
	public static boolean canIrradiateWGC(UUID party, World world, int chunkX, int chunkZ){
		return canIrradiateChunk(party, world, chunkX, chunkZ);
	}
	public static Set<ChunkCoordIntPair> getRadProtectedChunksWGC(UUID party, World world, int chunkX, int chunkZ, int radius){
		return getRadProtectedChunks(party,world,chunkX,chunkZ,radius);
	}
	public static boolean canContaminateChunkWGC(UUID party, World world, int chunkX, int chunkZ){
		return canContaminateChunk(party, world, chunkX, chunkZ);
	}
	public static boolean canContaminateBlockWGC(UUID party, World world, int blockX, int blockZ){
		return canContaminateChunkWGC(party, world, blockX >>4, blockZ >>4);
	}
	public static boolean canContaminateBlockWGC(UUID party, World world, double blockX, double blockZ){
		return canContaminateChunkWGC(party, world, ((int)Math.floor(blockX)) >>4, ((int)Math.floor(blockZ)) >>4);
	}
	public static Set<ChunkCoordIntPair> getContamProtectedChunksWGC(UUID party, World world, int chunkX, int chunkZ, int radius){
		return getContamProtectedChunks(party,world,chunkX,chunkZ,radius);
	}
	public static boolean canCrossContaminateWGC(World world, ChunkCoordIntPair chunk1, ChunkCoordIntPair chunk2){
		return canCrossContaminate(world,chunk1,chunk2);
	}
	public static boolean canPlaceClaimLockedBlockWGC(UUID party, World world, int x, int y, int z){
		return canPlaceClaimLockedBlock(party,world,x,z);
	}

	public static UUID getChunkOwnerWGC(World world, ChunkCoordIntPair chunkCoords){
		return getChunkOwner(world,chunkCoords.chunkXPos, chunkCoords.chunkZPos);
	}
	public static UUID getPlayerFaction(World world, UUID player){
		return WGCoreIntegrationAccess.getPlayerFaction(world, player);
	}
	public static boolean isProtected(int blockX, int blockZ, Set<ChunkCoordIntPair> protectedChunks){
		if(protectedChunks==null || protectedChunks.isEmpty()) return false; //Skips checking if protectedChunks is empty
		return protectedChunks.contains(getChunkCoordIntPair(blockX,blockZ));
	}
	public static ChunkCoordIntPair getChunkCoordIntPair(int blockX, int blockZ){
		return new ChunkCoordIntPair(blockX >> 4,blockZ >> 4);
	}
}
