package com.hbm.explosion.vanillant.standard;

import java.util.HashSet;
import java.util.Iterator;

import api.hbm.explosion.event.HbmExplosionHooks;
import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.interfaces.IBlockMutator;
import com.hbm.explosion.vanillant.interfaces.IBlockProcessor;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.world.ChunkPosition;
import net.minecraft.world.World;

public class BlockProcessorNoDamage implements IBlockProcessor {

	protected IBlockMutator convert;

	public BlockProcessorNoDamage() { }

	public BlockProcessorNoDamage withBlockEffect(IBlockMutator convert) {
		this.convert = convert;
		return this;
	}

	@Override
	public void process(ExplosionVNT explosion, World world, double x, double y, double z, HashSet<ChunkPosition> affectedBlocks) {

		Iterator iterator = affectedBlocks.iterator();

		while(iterator.hasNext()) {
			ChunkPosition chunkposition = (ChunkPosition) iterator.next();
			int blockX = chunkposition.chunkPosX;
			int blockY = chunkposition.chunkPosY;
			int blockZ = chunkposition.chunkPosZ;
			Block block = world.getBlock(blockX, blockY, blockZ);

			if(block.getMaterial() != Material.air) {
				// Safezone/claim guard — skip mutator pre-pass in protected coords
				if(this.convert != null && !HbmExplosionHooks.blockDenied(world, blockX, blockY, blockZ, "VNT.NODMG.PRE")) {
					this.convert.mutatePre(explosion, block, world.getBlockMetadata(blockX, blockY, blockZ), blockX, blockY, blockZ);
				}
			}
		}


		if(this.convert != null) {
			iterator = affectedBlocks.iterator();

			while(iterator.hasNext()) {
				ChunkPosition chunkposition = (ChunkPosition) iterator.next();
				int blockX = chunkposition.chunkPosX;
				int blockY = chunkposition.chunkPosY;
				int blockZ = chunkposition.chunkPosZ;
				Block block = world.getBlock(blockX, blockY, blockZ);

				if(block.getMaterial() == Material.air) {
					// Safezone/claim guard — skip mutator post-pass in protected coords
					if(!HbmExplosionHooks.blockDenied(world, blockX, blockY, blockZ, "VNT.NODMG.POST")) {
						this.convert.mutatePost(explosion, blockX, blockY, blockZ);
					}
				}
			}
		}

		affectedBlocks.clear(); //tricks the standard SFX to not do the block damage particles
	}
}
