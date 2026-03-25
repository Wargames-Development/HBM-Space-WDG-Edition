package com.hbm.blocks.bomb;

import com.hbm.blocks.generic.BlockFlammable;
import com.hbm.entity.item.EntityTNTPrimedBase;

import api.hbm.block.IFuckingExplode;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;

import java.util.UUID;

public abstract class BlockDetonatable extends BlockFlammable implements IFuckingExplode {

	protected int popFuse; // A shorter fuse for when this explosive is dinked by another
	protected boolean detonateOnCollision;
	protected boolean detonateOnShot;
	protected UUID ownerParty;

	public BlockDetonatable(Material mat, int en, int flam, int popFuse, boolean detonateOnCollision, boolean detonateOnShot) {
		super(mat, en, flam);
		this.popFuse = popFuse;
		this.detonateOnCollision = detonateOnCollision;
		this.detonateOnShot = detonateOnShot;
	}

	@Override
	public void onBlockDestroyedByExplosion(World world, int x, int y, int z, Explosion explosion) {
		if(!world.isRemote) {
			EntityTNTPrimedBase tntPrimed = new EntityTNTPrimedBase(world, x + 0.5D, y + 0.5D, z + 0.5D, explosion != null ? explosion.getExplosivePlacedBy() : null, this);
			tntPrimed.fuse = popFuse <= 0 ? 0 : world.rand.nextInt(popFuse) + popFuse / 2;
			tntPrimed.detonateOnCollision = detonateOnCollision;
			world.spawnEntityInWorld(tntPrimed);
		}
	}

	@Override
	public boolean canDropFromExplosion(Explosion explosion) {
		return false;
	}

	@Override
	public void onNeighborBlockChange(World world, int x, int y, int z, Block block) {
		if(!world.isRemote && shouldIgnite(world, x, y, z)) {
			world.setBlockToAir(x, y, z);
			onBlockDestroyedByExplosion(world, x, y, z, null);
		}
	}

	public void onShot(World world, int x, int y, int z) {
		if (!detonateOnShot) return;

		world.setBlockToAir(x, y, z);
		explodeEntity(world, x, y, z, null); // insta-explod
	}
	public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase entitylivingbase) {
		if(!world.isRemote) {
			ownerParty = entitylivingbase.getUniqueID();
		}
	}

	public UUID getOwnerParty() {return ownerParty;}

}
