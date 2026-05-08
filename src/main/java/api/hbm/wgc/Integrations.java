package api.hbm.wgc;

import com.wdg.wgcore.integration.api.WGCoreIntegrationAccess;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;

import java.util.Set;
import java.util.UUID;

import static com.wdg.wgcore.integration.api.WGCoreIntegrationAccess.*;


public class Integrations {
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

	public static boolean canExplodeBlockWGC(UUID party, World world, double x, double z) {
		return canExplodeChunkWGC(party,world, ((int)Math.floor(x)) >> 4, ((int)Math.floor(z)) >> 4);
	}

	public static Set<ChunkCoordIntPair> getExplosionProtectedChunksWGC(UUID party, World world, int x, int z, int r){
		return WGCoreIntegrationAccess.getExplosionProtectedChunks(party,world,x,z,r);
	}
	public static boolean canIrradiateWGC(UUID party, World world, int chunkX, int chunkZ){
		return WGCoreIntegrationAccess.canIrradiateChunk(party, world, chunkX, chunkZ);
	}
	public static Set<ChunkCoordIntPair> getRadProtectedChunksWGC(UUID party, World world, int chunkX, int chunkZ, int radius){
		return WGCoreIntegrationAccess.getRadProtectedChunks(party,world,chunkX,chunkZ,radius);
	}
	public static boolean canContaminateChunkWGC(UUID party, World world, int chunkX, int chunkZ){
		return WGCoreIntegrationAccess.canContaminateChunk(party, world, chunkX, chunkZ);
	}
	public static boolean canContaminateBlockWGC(UUID party, World world, int blockX, int blockZ){
		return canContaminateChunkWGC(party, world, blockX >>4, blockZ >>4);
	}
	public static boolean canContaminateBlockWGC(UUID party, World world, double blockX, double blockZ){
		return canContaminateChunkWGC(party, world, ((int)Math.floor(blockX)) >>4, ((int)Math.floor(blockZ)) >>4);
	}
	public static Set<ChunkCoordIntPair> getContamProtectedChunksWGC(UUID party, World world, int chunkX, int chunkZ, int radius){
		return WGCoreIntegrationAccess.getContamProtectedChunks(party,world,chunkX,chunkZ,radius);
	}
	public static boolean canCrossContaminateWGC(World world, ChunkCoordIntPair chunk1, ChunkCoordIntPair chunk2){
		return WGCoreIntegrationAccess.canCrossContaminate(world,chunk1,chunk2);
	}
	public static boolean canPlaceClaimLockedBlockWGC(UUID party, World world, int x, int y, int z){
		return WGCoreIntegrationAccess.canPlaceClaimLockedBlock(party,world,x,z);
	}

	public static UUID getChunkOwnerWGC(World world, ChunkCoordIntPair chunkCoords){
		return WGCoreIntegrationAccess.getChunkOwner(world,chunkCoords.chunkXPos, chunkCoords.chunkZPos);
	}
	public static UUID getPlayerFactionWGC(World world, UUID player){
		return WGCoreIntegrationAccess.getPlayerFaction(world,player);
	}
	public static boolean isProtected(int blockX, int blockZ, Set<ChunkCoordIntPair> protectedChunks){
		if(protectedChunks==null || protectedChunks.isEmpty()) return false; //Skips checking if protectedChunks is empty
		return protectedChunks.contains(getChunkCoordIntPair(blockX,blockZ));
	}
	public static ChunkCoordIntPair getChunkCoordIntPair(int blockX, int blockZ){
		return new ChunkCoordIntPair(blockX >> 4,blockZ >> 4);
	}
}
