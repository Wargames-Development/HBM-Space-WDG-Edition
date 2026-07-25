package com.hbm.explosion.vanillant;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;

import com.hbm.explosion.vanillant.interfaces.IBlockAllocator;
import com.hbm.explosion.vanillant.interfaces.IBlockProcessor;
import com.hbm.explosion.vanillant.interfaces.IEntityProcessor;
import com.hbm.explosion.vanillant.interfaces.IExplosionSFX;
import com.hbm.explosion.vanillant.interfaces.IPlayerProcessor;
import com.hbm.explosion.vanillant.standard.BlockAllocatorStandard;
import com.hbm.explosion.vanillant.standard.BlockProcessorStandard;
import com.hbm.explosion.vanillant.standard.CustomDamageHandlerAmat;
import com.hbm.explosion.vanillant.standard.EntityProcessorStandard;
import com.hbm.explosion.vanillant.standard.ExplosionEffectAmat;
import com.hbm.explosion.vanillant.standard.ExplosionEffectStandard;
import com.hbm.explosion.vanillant.standard.PlayerProcessorStandard;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.Vec3;
import net.minecraft.world.ChunkPosition;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;
import api.hbm.wgc.Integrations;
/**
 * Time to over-engineer this into fucking oblivion so that I never have to write a vanilla-esque explosion class ever again
 * @author hbm
 *
 */
public class ExplosionVNT {

	//explosions only need one of these, in the unlikely event that we do need to combine different types we can just write a wrapper that acts as a chainloader
	private IBlockAllocator blockAllocator;
	private IEntityProcessor entityProcessor;
	private IBlockProcessor blockProcessor;
	private IPlayerProcessor playerProcessor;
	//since we want to reduce each effect to the bare minimum (sound, particles, etc. being separate) we definitely need multiple most of the time
	private IExplosionSFX[] sfx;

	public World world;
	public double posX;
	public double posY;
	public double posZ;
	public float size;
	public Entity exploder;

	private UUID ownerParty;
	private Map compatPlayers = new HashMap();
	public Explosion compat;

	public ExplosionVNT(World world, double x, double y, double z, float size) {
		this(world, x, y, z, size, null, null);
	}

	public ExplosionVNT(World world, double x, double y, double z, float size, UUID ownerParty) {
		this(world, x, y, z, size, ownerParty, null);
	}

	public ExplosionVNT(World world, double x, double y, double z, float size, UUID ownerParty, Entity exploder) {
		this.world = world;
		this.posX = x;
		this.posY = y;
		this.posZ = z;
		this.size = size;
		this.exploder = exploder;
		this.ownerParty = ownerParty;

		this.compat = new Explosion(world, exploder, x, y, z, size) {

			@Override
			public Map func_77277_b() {
				return ExplosionVNT.this.compatPlayers;
			}
		};
	}

	public void explode() {

		if(this.world == null) return;
		if(!this.world.isRemote && !Integrations.canDetonateWGC(this.ownerParty, this.world, (int) Math.floor(this.posX), (int) Math.floor(this.posY), (int) Math.floor(this.posZ))) return;

		this.compat.exploder = this.exploder;
		this.compat.explosionSize = this.size;

		boolean processBlocks = blockAllocator != null && blockProcessor != null;
		boolean processEntities = entityProcessor != null && playerProcessor != null;

		HashSet<ChunkPosition> affectedBlocks = null;
		HashMap<EntityPlayer, Vec3> affectedPlayers = null;

		//allocation
		if(processBlocks) {
			affectedBlocks = blockAllocator.allocate(this, world, posX, posY, posZ, size, ownerParty);

			/*
			 * WGCore compatibility:
			 *
			 * ExplosionVNT is used by many HBM/NTM timed charges and VNT-backed
			 * projectile explosions. The normal allocator only returns blocks that
			 * NTM intends to break. High-resistance blocks, such as NTM concrete,
			 * may absorb the ray and never appear in that list.
			 *
			 * This helper adds candidate absorbing blocks, then lets WGCore apply
			 * territory rules and partial block damage. The returned set is the
			 * final set that NTM is allowed to break.
			 */
			affectedBlocks = Integrations.filterExplosionVNTAffectedBlocksWGC(
				this.ownerParty,
				this.world,
				this,
				affectedBlocks
			);

			this.compat.affectedBlockPositions.addAll(affectedBlocks);
		}

		if(processEntities) affectedPlayers = entityProcessor.process(this, world, posX, posY, posZ, size);
		// technically not necessary, as the affected entity list is a separate parameter during the Detonate event
		if(processEntities) this.compatPlayers.putAll(affectedPlayers);

		//serverside processing
		if(processBlocks) blockProcessor.process(this, world, posX, posY, posZ, affectedBlocks);
		if(processEntities) playerProcessor.process(this, world, posX, posY, posZ, affectedPlayers);

		if(sfx != null) {
			for(IExplosionSFX fx : sfx) {
				fx.doEffect(this, world, posX, posY, posZ, size);
			}
		}
	}

	public ExplosionVNT setBlockAllocator(IBlockAllocator blockAllocator) {
		this.blockAllocator = blockAllocator;
		return this;
	}
	public ExplosionVNT setEntityProcessor(IEntityProcessor entityProcessor) {
		this.entityProcessor = entityProcessor;
		return this;
	}
	public ExplosionVNT setBlockProcessor(IBlockProcessor blockProcessor) {
		this.blockProcessor = blockProcessor;
		return this;
	}
	public ExplosionVNT setPlayerProcessor(IPlayerProcessor playerProcessor) {
		this.playerProcessor = playerProcessor;
		return this;
	}

	public UUID getOwnerParty() {
		return ownerParty;
	}

	public ExplosionVNT setSFX(IExplosionSFX... sfx) {
		this.sfx = sfx;
		return this;
	}

	public ExplosionVNT makeStandard() {
		this.setBlockAllocator(new BlockAllocatorStandard());
		this.setBlockProcessor(new BlockProcessorStandard());
		this.setEntityProcessor(new EntityProcessorStandard());
		this.setPlayerProcessor(new PlayerProcessorStandard());
		this.setSFX(new ExplosionEffectStandard());
		return this;
	}

	public ExplosionVNT makeAmat() {
		this.setBlockAllocator(new BlockAllocatorStandard(this.size < 15 ? 16 : 32));
		this.setBlockProcessor(new BlockProcessorStandard()
				.setNoDrop());
		this.setEntityProcessor(new EntityProcessorStandard()
				.withRangeMod(2F)
				.withDamageMod(new CustomDamageHandlerAmat(50F)));
		this.setPlayerProcessor(new PlayerProcessorStandard());
		this.setSFX(new ExplosionEffectAmat());
		return this;
	}
}
