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
		return canHarmPlayer(party,targetedPlayer,world);
	}
	public static boolean canTargetChunkWGC(UUID party, World world,ChunkCoordIntPair chunkCoords) {
		return canTargetChunk(party,world, chunkCoords.chunkXPos, chunkCoords.chunkZPos); //TODO
	}
	public static boolean canTargetBlockWGC(UUID party,World world, int x, int y, int z) {
		return canTargetBlock(party, world, x, y, z);
	}
	public static boolean canDetonateWGC(UUID party, World world, int x, int y, int z){
		if(party == null){
			System.out.println("Null UUID!");
		}
		else {
			System.out.println("Detonate UUID:" + party.toString());
		}
		return canDetonate(party, world, x, y, z);
	}
	public static boolean canExplodeChunkWGC(UUID party, World world, int chunkX, int chunkZ) {
		return canExplodeChunk(party, world, chunkX, chunkZ);
	}
	public static Set<ChunkCoordIntPair> getExplosionProtectedChunksWGC(UUID party, World world, int x, int z, int r){
		return getExplosionProtectedChunks(party,world,x,z,r);
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
	public static boolean canPlaceClaimLockedBlockWGC(UUID party, World world, int x, int y, int z){
		return canPlaceClaimLockedBlock(party,world,x,z);
	}

	public static UUID getChunkOwnerWGC(World world, ChunkCoordIntPair chunkCoords){
		return getChunkOwner(world,chunkCoords.chunkXPos, chunkCoords.chunkZPos);
	}
	public static UUID getPlayerFaction(World world, UUID player){
		return getPlayerFaction(world,player);
	}
	public static boolean isProtected(int blockX, int blockZ, Set<ChunkCoordIntPair> protectedChunks){
		if(protectedChunks==null || protectedChunks.isEmpty()) return false; //Skips checking if protectedChunks is empty
		return protectedChunks.contains(getChunkCoordIntPair(blockX,blockZ));
	}
	private static ChunkCoordIntPair getChunkCoordIntPair(int blockX, int blockZ){
		return new ChunkCoordIntPair(blockX >> 4,blockZ >> 4);
	}
}
