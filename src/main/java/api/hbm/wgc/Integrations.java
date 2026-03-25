package api.hbm.wgc;

import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;

import java.util.Set;
import java.util.UUID;

import static com.wdg.wgcore.integration.api.WGCoreIntegrationAccess.*;


public class Integrations {
	public static boolean canTargetPlayerWGC(UUID party, UUID targetedPlayer, World world) {
		return canTargetPlayer(party,targetedPlayer, world);
	}
	public static boolean canHarmPlayerWGC(UUID party, UUID targetedPlayer, World world) {
		return canHarmPlayer(party,targetedPlayer,world); //TODO
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
	public static Set<ChunkCoordIntPair> getExplosionProtectedChunksWGC(UUID party, World world, int x, int z, int r){
		return getExplosionProtectedChunks(party,world,x,z,r);
	}
	public static UUID getChunkOwnerWGC(ChunkCoordIntPair chunkCoord){
		return null; //TODO
	}

	public static boolean isProtected(int blockX, int blockZ, Set<ChunkCoordIntPair> protectedChunks){
		if(protectedChunks==null || protectedChunks.isEmpty()) return false; //Skips checking if protectedChunks is empty
		return protectedChunks.contains(getChunkCoordIntPair(blockX,blockZ));
	}
	private static ChunkCoordIntPair getChunkCoordIntPair(int blockX, int blockZ){
		return new ChunkCoordIntPair(blockX >> 4,blockZ >> 4);
	}
}
