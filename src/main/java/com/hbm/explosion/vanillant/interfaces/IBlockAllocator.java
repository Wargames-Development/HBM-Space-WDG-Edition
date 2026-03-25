package com.hbm.explosion.vanillant.interfaces;

import java.util.HashSet;
import java.util.UUID;

import com.hbm.explosion.vanillant.ExplosionVNT;

import net.minecraft.world.ChunkPosition;
import net.minecraft.world.World;

public interface IBlockAllocator {

	public HashSet<ChunkPosition> allocate(ExplosionVNT explosion, World world, double x, double y, double z, float size, UUID ownerParty);
}
