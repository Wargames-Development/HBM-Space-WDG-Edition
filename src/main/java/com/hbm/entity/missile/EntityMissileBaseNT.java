package com.hbm.entity.missile;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import api.hbm.wgc.Integrations;
import com.google.common.collect.ImmutableSet;
import com.hbm.entity.logic.IChunkLoader;
import com.hbm.entity.projectile.EntityThrowableInterp;
import com.hbm.explosion.BunkerBusterTerrain;
import com.hbm.explosion.ExplosionLarge;
import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.standard.BlockAllocatorStandard;
import com.hbm.explosion.vanillant.standard.BlockMutatorFire;
import com.hbm.explosion.vanillant.standard.BlockProcessorStandard;
import com.hbm.explosion.vanillant.standard.EntityProcessorCross;
import com.hbm.explosion.vanillant.standard.ExplosionEffectStandard;
import com.hbm.explosion.vanillant.standard.PlayerProcessorStandard;
import com.hbm.items.weapon.ItemMissile;
import com.hbm.main.MainRegistry;
import com.hbm.util.TrackerUtil;

import api.hbm.entity.IRadarDetectableNT;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.entity.EntityTrackerEntry;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.ForgeChunkManager;
import net.minecraftforge.common.ForgeChunkManager.Ticket;
import net.minecraftforge.common.ForgeChunkManager.Type;

public abstract class EntityMissileBaseNT extends EntityThrowableInterp implements IChunkLoader, IRadarDetectableNT {

	public int startX;
	public int startZ;
	public int targetX;
	public int targetZ;
	public double velocity;
	public double decelY;
	public double accelXZ;
	public boolean isCluster = false;
	private Ticket loaderTicket;
	public int health = 50;
	protected UUID ownerParty;
	private static final double BUNKER_MAX_DEPTH = 100D;
	private static final double BUNKER_HEALTH_EPSILON = 1.0E-9D;
	// Keep the version-one disk scale so in-flight missiles preserve their remaining fraction.
	private static final double BUNKER_SAVED_HEALTH_SCALE = 35D / 20D;
	private double firstBunkerImpactY = Double.NaN;
	private double remainingBunkerHealth;

	public EntityMissileBaseNT(World world) {
		super(world);
		this.ignoreFrustumCheck = true;
		startX = (int) posX;
		startZ = (int) posZ;
		targetX = (int) posX;
		targetZ = (int) posZ;
	}

	public EntityMissileBaseNT(World world, float x, float y, float z, int a, int b,UUID ownerParty) {
		super(world);
		this.ignoreFrustumCheck = true;
		this.setLocationAndAngles(x, y, z, 0, 0);
		startX = (int) x;
		startZ = (int) z;
		targetX = a;
		targetZ = b;
		this.motionY = 2;
		this.ownerParty = ownerParty;

		Vec3 vector = Vec3.createVectorHelper(targetX - startX, 0, targetZ - startZ);
		accelXZ = decelY = 1 / vector.lengthVector();
		decelY *= 2;
		velocity = 0;

		this.rotationYaw = (float) (Math.atan2(targetX - posX, targetZ - posZ) * 180.0D / Math.PI);

		this.setSize(1.5F, 1.5F);
	}

	/** Auto-generates radar blip level and all that from the item */
	public abstract ItemStack getMissileItemForInfo();

	@Override
	public boolean canBeSeenBy(Object radar) {
		return true;
	}

	@Override
	public boolean paramsApplicable(RadarScanParams params) {
		if(!params.scanMissiles) return false;
		return true;
	}

	@Override
	public boolean suppliesRedstone(RadarScanParams params) {
		if(params.smartMode && this.motionY >= 0) return false;
		return true;
	}

	@Override
	protected void entityInit() {
		super.entityInit();
		init(ForgeChunkManager.requestTicket(MainRegistry.instance, worldObj, Type.ENTITY));
		this.dataWatcher.addObject(3, new Byte((byte) 5));
	}

	@Override
	protected double motionMult() {
		return velocity;
	}

	@Override
	public boolean doesImpactEntities() {
		return false;
	}

	@Override
	public void onUpdate() {
		this.lastTickPosX = this.posX;
		this.lastTickPosY = this.posY;
		this.lastTickPosZ = this.posZ;
		super.onUpdate();
		if (isDead) return;

		if(velocity < 4) velocity += MathHelper.clamp_double(this.ticksExisted / 60D * 0.05D, 0, 0.05);

		if(!worldObj.isRemote) {

			if(hasPropulsion()) {
				this.motionY -= decelY * velocity;

				Vec3 vector = Vec3.createVectorHelper(targetX - startX, 0, targetZ - startZ);
				vector = vector.normalize();
				vector.xCoord *= accelXZ;
				vector.zCoord *= accelXZ;

				if(motionY > 0) {
					motionX += vector.xCoord * velocity;
					motionZ += vector.zCoord * velocity;
				}

				if(motionY < 0) {
					motionX -= vector.xCoord * velocity;
					motionZ -= vector.zCoord * velocity;
				}
			} else {
				motionX *= 0.99;
				motionZ *= 0.99;

				if(motionY > -1.5)
					motionY -= 0.05;
			}

			if(motionY < -1.5 && this.isCluster) {
				cluster();
				this.setDead();
				return;
			}

			this.rotationYaw = (float) (Math.atan2(targetX - posX, targetZ - posZ) * 180.0D / Math.PI);
			float f2 = MathHelper.sqrt_double(this.motionX * this.motionX + this.motionZ * this.motionZ);
			for(this.rotationPitch = (float) (Math.atan2(this.motionY, f2) * 180.0D / Math.PI) - 90; this.rotationPitch - this.prevRotationPitch < -180.0F; this.prevRotationPitch -= 360.0F);
			EntityTrackerEntry tracker = TrackerUtil.getTrackerEntry((WorldServer) worldObj, this.getEntityId());
			if(tracker != null) tracker.lastYaw += 100; //coax the tracker into sending smother updates

			loadNeighboringChunks((int) Math.floor(posX / 16), (int) Math.floor(posZ / 16));
		} else {
			this.spawnContrail();
		}

		while(this.rotationPitch - this.prevRotationPitch >= 180.0F) this.prevRotationPitch += 360.0F;
		while(this.rotationYaw - this.prevRotationYaw < -180.0F) this.prevRotationYaw -= 360.0F;
		while(this.rotationYaw - this.prevRotationYaw >= 180.0F) this.prevRotationYaw += 360.0F;
	}
	protected double maxBunkerBusterHealth() {
		return 0D;
	}

	@Override
	public void onBlockCollide(MovingObjectPosition hit, Vec3 start, Vec3 clippedEnd) {
		double capacity = maxBunkerBusterHealth();
		if (capacity == 0D) {
			super.onBlockCollide(hit, start, clippedEnd);
			return;
		}

		// The superclass endpoint is clipped to the first hit. Every block in the full step must be paid for.
		Vec3 fullEnd = start.addVector(motionX * motionMult(), motionY * motionMult(), motionZ * motionMult());
		List<Vec3> trail = new ArrayList<>();
		for (int steps = 0; hit != null && hit.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK
				&& !isDead && steps < 256; steps++) {
			Vec3 impact = hit.hitVec;
			if (impact.yCoord <= firstBunkerImpactY - BUNKER_MAX_DEPTH) break;

			int x = hit.blockX;
			int y = hit.blockY;
			int z = hit.blockZ;
			Block block = worldObj.getBlock(x, y, z);
			if (block == Blocks.portal) {
				setInPortal();
				break;
			}
			if (!block.isAir(worldObj, x, y, z)) {
				if (Double.isNaN(firstBunkerImpactY)) {
					firstBunkerImpactY = impact.yCoord;
					remainingBunkerHealth = capacity;
				}

				UUID owner = getOwner();
				ChunkCoordIntPair chunk = new ChunkCoordIntPair(x >> 4, z >> 4);
				if (!Integrations.canTargetChunkWGC(owner, worldObj, chunk)
						|| !Integrations.canExplodeBlockWGC(owner, worldObj, x, z)
						|| Integrations.isBunkerBusterProtected(worldObj, x, y, z)) {
					detonateBunkerBuster(hit);
					break;
				}

				double cost = getBunkerBusterCost(block, x, y, z);
				if (!(cost > 0D && cost <= remainingBunkerHealth + BUNKER_HEALTH_EPSILON)
						|| !worldObj.setBlockToAir(x, y, z)) {
					detonateBunkerBuster(hit);
					break;
				}

				remainingBunkerHealth -= cost;
				trail.add(Vec3.createVectorHelper(x, y, z));
				if (remainingBunkerHealth <= BUNKER_HEALTH_EPSILON) {
					detonateBunkerBuster(hit);
					break;
				}
			}

			// World ray tracing mutates its start vector. Keep the original tick start for the depth fuse.
			hit = worldObj.func_147447_a(Vec3.createVectorHelper(start.xCoord, start.yCoord, start.zCoord),
					fullEnd, false, true, false);
		}

		for (Vec3 position : trail) {
			explodeStandardAt(position.xCoord, position.yCoord, position.zCoord, 3F, 12, true, 3F);
		}
	}

	@Override
	public void airburstCheck(Vec3 start, Vec3 clippedEnd) {
		if (isDead || Double.isNaN(firstBunkerImpactY)) return;
		double depthY = firstBunkerImpactY - BUNKER_MAX_DEPTH;
		double verticalStep = motionY * motionMult();
		if (verticalStep >= 0D || start.yCoord + verticalStep > depthY) return;

		// Fuse at the depth plane even when the missile is crossing an air pocket.
		double travel = Math.max(0D, (depthY - start.yCoord) / verticalStep) * motionMult();
		Vec3 impact = start.addVector(motionX * travel, motionY * travel, motionZ * travel);
		detonateBunkerBuster(new MovingObjectPosition(MathHelper.floor_double(impact.xCoord),
				MathHelper.floor_double(impact.yCoord), MathHelper.floor_double(impact.zCoord), 1, impact));
	}

	private void detonateBunkerBuster(MovingObjectPosition impact) {
		setPosition(impact.hitVec.xCoord, impact.hitVec.yCoord, impact.hitVec.zCoord);
		onImpact(impact);
	}

	private double getBunkerBusterCost(Block block, int x, int y, int z) {
		if (!(block.getBlockHardness(worldObj, x, y, z) >= 0F)) return Double.POSITIVE_INFINITY;
		double resistance;
		if (block == Blocks.sandstone) {
			resistance = Blocks.stone.getExplosionResistance(null);
		} else if (block == Blocks.obsidian) {
			resistance = Blocks.stone.getExplosionResistance(null) * 3D;
		} else {
			resistance = block.getExplosionResistance(this, worldObj, x, y, z, posX, posY, posZ);
		}
		return Math.max(0.1D, Integrations.bunkerBusterResistance(worldObj, x, y, z, resistance));
	}

	public boolean checkForAirburst(Vec3 pos, Vec3 nextPos, int detHeight) {
		if(nextPos.yCoord - pos.yCoord > -2.0){
			return false;
		}
		Vec3 rayTip = Vec3.createVectorHelper(pos.xCoord, pos.yCoord-detHeight, pos.zCoord);
		MovingObjectPosition mop = this.worldObj.func_147447_a(pos, rayTip, false, true, false);
		return(mop != null);
	}

	public boolean hasPropulsion() {
		return true;
	}

	protected void spawnContrail() {
		this.spawnContrailWithOffset(0, 0, 0);
	}
	
	protected void spawnContrailWithOffset(double offsetX, double offsetY, double offsetZ) {
		Vec3 vec = Vec3.createVectorHelper(this.lastTickPosX - this.posX, this.lastTickPosY - this.posY, this.lastTickPosZ - this.posZ);
		double len = vec.lengthVector();
		vec = vec.normalize();
		Vec3 thrust = Vec3.createVectorHelper(0, 1, 0);
		thrust.rotateAroundZ(this.rotationPitch * (float) Math.PI / 180F);
		thrust.rotateAroundY((this.rotationYaw + 90) * (float) Math.PI / 180F);

		for(int i = 0; i < Math.max(Math.min(len, 10), 1); i++) {
			double j = i - len;
			NBTTagCompound data = new NBTTagCompound();
			data.setDouble("posX", posX - vec.xCoord * j + offsetX);
			data.setDouble("posY", posY - vec.yCoord * j + offsetY);
			data.setDouble("posZ", posZ - vec.zCoord * j + offsetZ);
			data.setString("type", "missileContrail");
			data.setFloat("scale", this.getContrailScale());
			data.setDouble("moX", -thrust.xCoord);
			data.setDouble("moY", -thrust.yCoord);
			data.setDouble("moZ", -thrust.zCoord);
			data.setInteger("maxAge", 60 + rand.nextInt(20));
			MainRegistry.proxy.effectNT(data);
		}
	}

	protected float getContrailScale() {
		return 1F;
	}

	@Override
	public void readEntityFromNBT(NBTTagCompound nbt) {
		super.readEntityFromNBT(nbt);
		motionX = nbt.getDouble("moX");
		motionY = nbt.getDouble("moY");
		motionZ = nbt.getDouble("moZ");
		posX = nbt.getDouble("poX");
		posY = nbt.getDouble("poY");
		posZ = nbt.getDouble("poZ");
		decelY = nbt.getDouble("decel");
		accelXZ = nbt.getDouble("accel");
		targetX = nbt.getInteger("tX");
		targetZ = nbt.getInteger("tZ");
		startX = nbt.getInteger("sX");
		startZ = nbt.getInteger("sZ");
		velocity = nbt.getDouble("veloc");
		if (nbt.hasKey("bunkerDrill", 10) && maxBunkerBusterHealth() > 0D) {
			NBTTagCompound drill = nbt.getCompoundTag("bunkerDrill");
			double firstImpact = drill.getDouble("firstImpactY");
			double remaining = drill.getDouble("remainingHealth") / BUNKER_SAVED_HEALTH_SCALE;
			if (drill.getInteger("version") == 1 && drill.hasKey("firstImpactY", 99)
					&& Double.isFinite(firstImpact) && remaining > 0D && remaining <= maxBunkerBusterHealth()) {
				firstBunkerImpactY = firstImpact;
				remainingBunkerHealth = remaining;
			}
		}
		if(nbt.hasKey("ownerPartyMost") && nbt.hasKey("ownerPartyLeast")) {
			ownerParty = new UUID(nbt.getLong("ownerPartyMost"), nbt.getLong("ownerPartyLeast"));
		}
	}

	@Override
	public void writeEntityToNBT(NBTTagCompound nbt) {
		super.writeEntityToNBT(nbt);
		nbt.setDouble("moX", motionX);
		nbt.setDouble("moY", motionY);
		nbt.setDouble("moZ", motionZ);
		nbt.setDouble("poX", posX);
		nbt.setDouble("poY", posY);
		nbt.setDouble("poZ", posZ);
		nbt.setDouble("decel", decelY);
		nbt.setDouble("accel", accelXZ);
		nbt.setInteger("tX", targetX);
		nbt.setInteger("tZ", targetZ);
		nbt.setInteger("sX", startX);
		nbt.setInteger("sZ", startZ);
		nbt.setDouble("veloc", velocity);
		if (!Double.isNaN(firstBunkerImpactY)) {
			NBTTagCompound drill = new NBTTagCompound();
			drill.setInteger("version", 1);
			drill.setDouble("firstImpactY", firstBunkerImpactY);
			drill.setDouble("remainingHealth", remainingBunkerHealth * BUNKER_SAVED_HEALTH_SCALE);
			nbt.setTag("bunkerDrill", drill);
		}
		if(ownerParty != null) {
			nbt.setLong("ownerPartyMost", ownerParty.getMostSignificantBits());
			nbt.setLong("ownerPartyLeast", ownerParty.getLeastSignificantBits());
		}
	}

	public boolean canBeCollidedWith() {
		return true;
	}

	public boolean attackEntityFrom(DamageSource source, float amount) {
		if(this.isEntityInvulnerable()) {
			return false;
		} else {
			if(this.health > 0 && !this.worldObj.isRemote) {
				health -= amount;

				if(this.health <= 0) {
					this.killMissile();
				}
			}

			return true;
		}
	}

	protected void killMissile() {
		if(!this.isDead) {
			this.setDead();
			if(Integrations.canDetonateWGC(ownerParty,worldObj,(int)posX,(int)posY,(int)posZ)) {
				ExplosionLarge.explode(ownerParty,worldObj, posX, posY, posZ, 5, true, false, true);
				ExplosionLarge.spawnShrapnelShower(worldObj, posX, posY, posZ, motionX, motionY, motionZ, 15, 0.075,ownerParty);
				ExplosionLarge.spawnMissileDebris(worldObj, posX, posY, posZ, motionX, motionY, motionZ, 0.25, getDebris(), getDebrisRareDrop());
			}
		}
	}

	@Override
	@SideOnly(Side.CLIENT)
	public boolean isInRangeToRenderDist(double distance) {
		return true;
	}

	protected boolean canDetonateAt(double x, double y, double z) {
		return Integrations.canDetonateWGC(ownerParty, worldObj, (int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z));
	}

	protected boolean canDetonateHere() {
		return this.canDetonateAt(this.posX, this.posY, this.posZ);
	}

	@Override
	protected void onImpact(MovingObjectPosition mop) {
		if(this.worldObj.isRemote) {
			this.setDead();
			return;
		}
		if(mop != null && mop.typeOfHit == mop.typeOfHit.BLOCK) {
			double impactX = mop.hitVec != null ? mop.hitVec.xCoord : mop.blockX + 0.5D;
			double impactY = mop.hitVec != null ? mop.hitVec.yCoord : mop.blockY + 0.5D;
			double impactZ = mop.hitVec != null ? mop.hitVec.zCoord : mop.blockZ + 0.5D;
			if(this.canDetonateAt(impactX, impactY, impactZ)) com.hbm.lib.CombatDamageSource.impact(this, mop);
			this.setDead();
		}
	}

	public abstract void onMissileImpact(MovingObjectPosition mop);
	public abstract List<ItemStack> getDebris();
	public abstract ItemStack getDebrisRareDrop();
	public void cluster() { }

	@Override
	public double getGravityVelocity() {
		return 0.0D;
	}

	@Override
	protected float getAirDrag() {
		return 1F;
	}

	@Override
	protected float getWaterDrag() {
		return 1F;
	}

	@Override
	public void init(Ticket ticket) {
		if(!worldObj.isRemote) {

			if(ticket != null) {

				if(loaderTicket == null) {

					loaderTicket = ticket;
					loaderTicket.bindEntity(this);
					loaderTicket.getModData();
				}

				ForgeChunkManager.forceChunk(loaderTicket, new ChunkCoordIntPair(chunkCoordX, chunkCoordZ));
			}
		}
	}

	public void loadNeighboringChunks(int newChunkX, int newChunkZ) {
		if(!worldObj.isRemote && loaderTicket != null) {
			ChunkCoordIntPair newChunk = new ChunkCoordIntPair(newChunkX, newChunkZ);

			for(ChunkCoordIntPair chunk : loaderTicket.getChunkList()) {
				if(chunk.equals(newChunk)) return; // cancel everything, we're already loaded
				ForgeChunkManager.unforceChunk(loaderTicket, chunk);
			}

			ForgeChunkManager.forceChunk(loaderTicket, newChunk);
		}
	}


	@Override
	public void setDead() {
		super.setDead();
		this.clearChunkLoader();
	}

	public void clearChunkLoader() {
		if(!worldObj.isRemote && loaderTicket != null) {
			ForgeChunkManager.releaseTicket(loaderTicket);
			this.loaderTicket = null;
		}
	}

	public void explodeStandard(float strength, int resolution, boolean fire) {
		this.explodeStandardAt(posX, posY, posZ, strength, resolution, fire);
	}

	protected void explodeStandardAt(double x, double y, double z, float strength, int resolution, boolean fire) {
		this.explodeStandardAt(x, y, z, strength, resolution, fire, 0F);
	}

	protected void explodeBunkerBuster(float strength, int resolution, float terrainStrength) {
		this.explodeStandardAt(posX, posY, posZ, strength, resolution, false, terrainStrength);
	}

	private void explodeStandardAt(double x, double y, double z, float strength, int resolution, boolean fire,
			float terrainStrength) {
		if(!this.canDetonateAt(x, y, z)) return;
		ExplosionVNT xnt = new ExplosionVNT(worldObj, x, y, z, strength, ownerParty, this);
		BlockProcessorStandard processor = new BlockProcessorStandard().setNoDrop()
				.withBlockEffect(fire ? new BlockMutatorFire() : null);
		boolean deferredTerrain = terrainStrength > 0F && Integrations.supportsBunkerBusterTerrain();
		if(!deferredTerrain) {
			xnt.setBlockAllocator(new BlockAllocatorStandard(resolution));
			xnt.setBlockProcessor(processor);
		}
		xnt.setEntityProcessor(new EntityProcessorCross(7.5D).withRangeMod(2));
		xnt.setPlayerProcessor(new PlayerProcessorStandard());
		xnt.explode();
		if(deferredTerrain) {
			BunkerBusterTerrain.enqueue(worldObj, ownerParty, xnt.compat, terrainStrength, fire,
					blocks -> processor.process(xnt, worldObj, x, y, z, blocks));
		}
	}

	protected void explodeEntityOnly(float strength) {
		if(!this.canDetonateHere()) return;
		ExplosionVNT xnt = new ExplosionVNT(worldObj, posX, posY, posZ, strength, ownerParty, this);
		xnt.setEntityProcessor(new EntityProcessorCross(0).withRangeMod(2));
		xnt.setPlayerProcessor(new PlayerProcessorStandard());
		xnt.setSFX(new ExplosionEffectStandard());
		xnt.explode();
	}

	@Override
	public String getUnlocalizedName() {
		ItemStack item = this.getMissileItemForInfo();
		if(item != null && item.getItem() instanceof ItemMissile) {
			ItemMissile missile = (ItemMissile) item.getItem();
			switch(missile.tier) {
			case TIER0: return "radar.target.tier0";
			case TIER1: return "radar.target.tier1";
			case TIER2: return "radar.target.tier2";
			case TIER3: return "radar.target.tier3";
			case TIER4: return "radar.target.tier4";
			default: return "Unknown";
			}
		}

		return "Unknown";
	}

	@Override
	public int getBlipLevel() {
		ItemStack item = this.getMissileItemForInfo();
		if(item != null && item.getItem() instanceof ItemMissile) {
			ItemMissile missile = (ItemMissile) item.getItem();
			switch(missile.tier) {
			case TIER0: return IRadarDetectableNT.TIER0;
			case TIER1: return IRadarDetectableNT.TIER1;
			case TIER2: return IRadarDetectableNT.TIER2;
			case TIER3: return IRadarDetectableNT.TIER3;
			case TIER4: return IRadarDetectableNT.TIER4;
			default: return IRadarDetectableNT.SPECIAL;
			}
		}

		return IRadarDetectableNT.SPECIAL;
	}

	public UUID getOwner() {
		return ownerParty;
	}
}
