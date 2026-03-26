package com.hbm.blocks.bomb;

import com.hbm.blocks.generic.BlockFlammable;
import com.hbm.entity.item.EntityTNTPrimedBase;

import api.hbm.block.IFuckingExplode;
import com.hbm.tileentity.bomb.TileEntityPartyOwned;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;

import java.util.UUID;

public abstract class BlockDetonatable extends BlockFlammable implements IFuckingExplode {

	protected int popFuse; // A shorter fuse for when this explosive is dinked by another
	protected boolean detonateOnCollision;
	protected boolean detonateOnShot;
	private static final ThreadLocal<UUID> explosionOwnerCache = new ThreadLocal<>();

	public BlockDetonatable(Material mat, int en, int flam, int popFuse, boolean detonateOnCollision, boolean detonateOnShot) {
		super(mat, en, flam);
		this.popFuse = popFuse;
		this.detonateOnCollision = detonateOnCollision;
		this.detonateOnShot = detonateOnShot;
	}

	@Override
	public void onBlockDestroyedByExplosion(World world, int x, int y, int z, Explosion explosion) {
		UUID owner = explosionOwnerCache.get();
		if(!world.isRemote) {
			EntityTNTPrimedBase tntPrimed = new EntityTNTPrimedBase(world, x + 0.5D, y + 0.5D, z + 0.5D, explosion != null ? explosion.getExplosivePlacedBy() : null, owner, this);
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
			setOwnerParty(world,x,y,z,entitylivingbase.getUniqueID());
		}
	}
	@Override
	public boolean hasTileEntity(int metadata) {//Detonatable blocks will have a tileEntity.
		return true;
	}
	@Override
	public void breakBlock(World world, int x, int y, int z, Block block, int meta) {
		explosionOwnerCache.set(BlockPartyOwned.getOwner(world, x, y, z));
		super.breakBlock(world, x, y, z, block, meta);
	}

	@Override
	public TileEntity createTileEntity(World world, int meta) {
		return new TileEntityPartyOwned();
	}

	public UUID getOwner(World world, int x, int y, int z) {
		TileEntity te = world.getTileEntity(x, y, z);
		UUID owner = null;

		if (te instanceof TileEntityPartyOwned) {
			owner = ((TileEntityPartyOwned) te).ownerParty;
		}
		return owner;
	}
	public void setOwnerParty(World world, int x, int y, int z, UUID newOwner) {
		TileEntity te = world.getTileEntity(x, y, z);

		if (te instanceof TileEntityPartyOwned) {
			((TileEntityPartyOwned) te).ownerParty = newOwner;
		}
	}

}
