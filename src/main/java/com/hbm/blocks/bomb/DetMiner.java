package com.hbm.blocks.bomb;

import java.util.Random;
import java.util.UUID;

import api.hbm.wgc.Integrations;
import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.machine.BlockPillar;
import com.hbm.entity.item.EntityTNTPrimedBase;
import com.hbm.explosion.ExplosionLarge;
import com.hbm.explosion.ExplosionNT;
import com.hbm.explosion.ExplosionNT.ExAttrib;
import com.hbm.interfaces.IBomb;

import api.hbm.block.IFuckingExplode;
import com.hbm.lib.RefStrings;
import com.hbm.tileentity.bomb.TileEntityPartyOwned;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.util.MathHelper;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.item.Item;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;

public class DetMiner extends BlockPartyOwned implements IBomb, IFuckingExplode {

	@SideOnly(Side.CLIENT)
	private IIcon iconTop;

	public DetMiner(Material mat) {
		super(mat);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void registerBlockIcons(IIconRegister iconRegister) {
		this.iconTop = iconRegister.registerIcon(RefStrings.MODID + ":det_miner_top");
		this.blockIcon = iconRegister.registerIcon(RefStrings.MODID + ":det_miner_side");
	}

	@Override
	@SideOnly(Side.CLIENT)
	public IIcon getIcon(int side, int metadata) {
		return side == 1 ? this.iconTop : (side == 0 ? this.iconTop : this.blockIcon);
	}

	@Override
	public Item getItemDropped(int i, Random rand, int j) {
		return null;
	}

	@Override
	public BombReturnCode explode(World world, int x, int y, int z) {

		if(!world.isRemote) {
			if(Integrations.canDetonateWGC(getOwner(world,x,y,z), world, x, y, z)) {
				world.func_147480_a(x, y, z, false);
				UUID owner = getOwner(world, x, y, z);
				ExplosionNT explosion = new ExplosionNT(world, null, x + 0.5, y + 0.5, z + 0.5, 4, owner);
				explosion.atttributes.add(ExAttrib.ALLDROP);
				explosion.atttributes.add(ExAttrib.NOHURT);
				explosion.doExplosionA();
				explosion.doExplosionB(false);

				ExplosionLarge.spawnParticles(world, x + 0.5, y + 0.5, z + 0.5, 30);
			}
			else{
				return BombReturnCode.ERROR_BLOCKED;
			}
		}

		return BombReturnCode.DETONATED;
	}

	@Override
	public void onBlockDestroyedByExplosion(World world, int x, int y, int z, Explosion explosion) {
		if(!world.isRemote) {
			EntityTNTPrimedBase tntPrimed = new EntityTNTPrimedBase(world, x + 0.5D, y + 0.5D, z + 0.5D, explosion != null ? explosion.getExplosivePlacedBy() : null, getOwner(world,x,y,z), this);
			tntPrimed.fuse = 0;
			tntPrimed.detonateOnCollision = false;
			world.spawnEntityInWorld(tntPrimed);
		}
	}

	@Override
	public void onNeighborBlockChange(World world, int x, int y, int z, Block p_149695_5_) {
		if(world.isBlockIndirectlyGettingPowered(x, y, z)) {
			this.explode(world, x, y, z);
		}
	}

	@Override
	public void explodeEntity(World world, double x, double y, double z, EntityTNTPrimedBase entity) {
		explode(world, MathHelper.floor_double(x), MathHelper.floor_double(y), MathHelper.floor_double(z));
	}
	@Override
	public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase player, ItemStack itemStack) {
		setOwner(world,x,y,z,player.getUniqueID());
	}

	@Override
	public boolean hasTileEntity(int metadata) {//Party Owned blocks will have a tileEntity.
		return true;
	}
	@Override
	public void breakBlock(World world, int x, int y, int z, Block block, int meta) {
		super.breakBlock(world, x, y, z, block, meta);
	}
}
