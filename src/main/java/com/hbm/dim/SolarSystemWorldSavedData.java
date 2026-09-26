package com.hbm.dim;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Random;
import java.util.UUID;

import api.hbm.wgc.Integrations;
import com.hbm.blocks.BlockDummyable;
import com.hbm.blocks.ModBlocks;
import com.hbm.config.SpaceConfig;
import com.hbm.dim.orbit.OrbitalStation;
import com.hbm.dim.orbit.OrbitalStation.StationState;
import com.hbm.dim.trait.CelestialBodyTrait;
import com.hbm.entity.missile.EntityRideableRocket;
import com.hbm.items.ItemRaidDrive;
import com.hbm.items.ItemVOTVdrive;
import com.hbm.items.ModItems;
import com.hbm.main.ChunkLoaderManager;
import com.hbm.main.MainRegistry;
import com.hbm.tileentity.bomb.TileEntityLaunchPadRocket;

import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.MathHelper;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;
import net.minecraft.world.WorldSavedData;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.util.Constants.NBT;
import net.minecraftforge.common.util.ForgeDirection;

public class SolarSystemWorldSavedData extends WorldSavedData {

	private static final String DATA_NAME = "SolarSystemData";
	private static final String RAID_DRIVE_AUTHORIZATIONS_TAG = "hbmRaidDriveAuthorizations";
	private static final String LEGACY_DELETED_STATIONS_TAG = "hbmDeletedStations";
	private static final String GENERATIONS_TAG = "hbmStationGenerations";
	private static final String CLEANUP_TASKS_TAG = "hbmStationCleanupTasks";
	private static final String RAID_RUNTIME_CLOCK_TAG = "hbmRaidUsesServerRuntimeClock";
	private static final String PLAYER_ORBIT_CONTEXT_TAG = "hbmOrbitalCleanupContext";
	private static final String PLAYER_ORBIT_STATION_KEY_TAG = "stationKey";
	private static final String PLAYER_ORBIT_STATION_GENERATION_TAG = "stationGeneration";
	private static final String PLAYER_ORBIT_RAID_TOKEN_TAG = "raidToken";
	private static final String PLAYER_ORBIT_RETURN_CONTEXT_TAG = "hbmOrbitalReturnContext";
	private static final String PLAYER_ORBIT_RETURN_DIMENSION_TAG = "dimension";
	private static final String PLAYER_ORBIT_RETURN_X_TAG = "x";
	private static final String PLAYER_ORBIT_RETURN_Z_TAG = "z";
	private static final long[] COMPUTER_WARNING_THRESHOLDS_TICKS = new long[] {
		30L * 60L * 20L,
		15L * 60L * 20L,
		10L * 60L * 20L,
		5L * 60L * 20L,
		60L * 20L,
		30L * 20L,
		10L * 20L
	};
	private static final long RAID_WARNING_INTERVAL_MILLIS = 5L * 60L * 1000L;
	private static final int CLEANUP_KEEPALIVE_KEY_X = Integer.MIN_VALUE;
	private static final int CLEANUP_KEEPALIVE_KEY_Y = 0;
	private static final int CLEANUP_KEEPALIVE_KEY_Z = Integer.MIN_VALUE;
	private static final ChunkCoordIntPair CLEANUP_KEEPALIVE_CHUNK = new ChunkCoordIntPair(0, 0);

	private Random rand = new Random();
	private HashMap<String, HashMap<Class<? extends CelestialBodyTrait>, CelestialBodyTrait>> traitMap = new HashMap<String, HashMap<Class<? extends CelestialBodyTrait>, CelestialBodyTrait>>();
	private HashMap<ChunkCoordIntPair, OrbitalStation> stations = new HashMap<ChunkCoordIntPair, OrbitalStation>();
	private HashMap<String, RaidDriveAuthorization> raidDriveAuthorizations = new HashMap<String, RaidDriveAuthorization>();
	private HashMap<ChunkCoordIntPair, Integer> stationGenerations = new HashMap<ChunkCoordIntPair, Integer>();
	private List<CleanupTask> cleanupTasks = new ArrayList<CleanupTask>();
	private boolean raidRuntimeClockMigrationPending;

	public SolarSystemWorldSavedData(String name) {
		super(name);
	}

	public static SolarSystemWorldSavedData get() {
		World[] worlds = DimensionManager.getWorlds();
		if(worlds.length == 0) return null;
		return get(worlds[0]);
	}

	public static SolarSystemWorldSavedData get(World world) {
		if(world == null) return null;
		SolarSystemWorldSavedData result = (SolarSystemWorldSavedData) world.mapStorage.loadData(SolarSystemWorldSavedData.class, DATA_NAME);
		if(result == null) {
			world.mapStorage.setData(DATA_NAME, new SolarSystemWorldSavedData(DATA_NAME));
			result = (SolarSystemWorldSavedData) world.mapStorage.loadData(SolarSystemWorldSavedData.class, DATA_NAME);
		}
		return result;
	}

	/**
	 * Login safety for players who were offline while an orbital cleanup completed.
	 *
	 * The player context is recorded while they are online in orbit. If the
	 * recorded station generation or temporary raid outpost no longer exists when
	 * they next log in, return them to their recorded surface launch area (or a safe spawn fallback).
	 */
	public synchronized boolean recoverPlayerFromRetiredOrbitalLocation(EntityPlayerMP player) {
		if(player == null || player.worldObj == null || player.worldObj.isRemote) return false;

		NBTTagCompound persisted = getPersistedPlayerData(player);
		if(player.worldObj.provider.dimensionId != SpaceConfig.orbitDimension) {
			persisted.removeTag(PLAYER_ORBIT_CONTEXT_TAG);
			return false;
		}
		if(!persisted.hasKey(PLAYER_ORBIT_CONTEXT_TAG)) return false;

		NBTTagCompound context = persisted.getCompoundTag(PLAYER_ORBIT_CONTEXT_TAG);
		String savedStationKey = context.getString(PLAYER_ORBIT_STATION_KEY_TAG);
		int savedGeneration = Math.max(0, context.getInteger(PLAYER_ORBIT_STATION_GENERATION_TAG));
		String savedRaidToken = context.getString(PLAYER_ORBIT_RAID_TOKEN_TAG);

		int blockX = MathHelper.floor_double(player.posX);
		int blockZ = MathHelper.floor_double(player.posZ);
		OrbitalStation station = getStationFromPosition(blockX, blockZ);

		boolean stationMatches = station != null
			&& station.stationKey != null
			&& station.stationKey.equals(savedStationKey)
			&& station.generation == savedGeneration;

		boolean retired = !stationMatches;
		if(!retired && station.deleting && isBreachSettlementCleanupQueued(station.stationKey, station.generation)) {
			retired = true;
		}
		if(!retired && savedRaidToken != null && !savedRaidToken.isEmpty()) {
			retired = !station.raidPortActive
				|| !savedRaidToken.equals(station.raidToken)
				|| !isInsideRaidOutpostFootprint(station, player.posX, player.posZ);
		}

		if(!retired) return false;

		CelestialBody fallbackBody = station != null ? station.orbiting : CelestialBody.getBody(0);
		if(!returnPlayerToSurface(
			player,
			fallbackBody,
			"Your previous orbital location was removed while you were offline. Returning you safely to the surface.")) {
			return false;
		}
		MainRegistry.logger.info("[StationMaintenance] Recovered offline player "
			+ player.getCommandSenderName() + " from retired orbital location stationKey="
			+ shortIdentity(savedStationKey) + " generation=" + savedGeneration
			+ (savedRaidToken == null || savedRaidToken.isEmpty()
				? "" : " raidToken=" + shortIdentity(savedRaidToken)) + ".");
		return true;
	}

	public static void rememberPlayerOrbitalReturn(EntityPlayer player, int dimensionId, int x, int z) {
		if(player == null || player.worldObj == null || player.worldObj.isRemote) return;
		NBTTagCompound persisted = getPersistedPlayerData(player);
		NBTTagCompound context = new NBTTagCompound();
		context.setInteger(PLAYER_ORBIT_RETURN_DIMENSION_TAG, dimensionId);
		context.setInteger(PLAYER_ORBIT_RETURN_X_TAG, x);
		context.setInteger(PLAYER_ORBIT_RETURN_Z_TAG, z);
		persisted.setTag(PLAYER_ORBIT_RETURN_CONTEXT_TAG, context);
	}

	public synchronized boolean returnPlayerToSurface(EntityPlayerMP player, CelestialBody fallbackBody, String message) {
		if(player == null || player.worldObj == null || player.worldObj.isRemote) return false;

		NBTTagCompound persisted = getPersistedPlayerData(player);
		int targetDimension = fallbackBody != null ? fallbackBody.dimensionId : 0;
		int baseX;
		int baseZ;
		boolean savedReturn = persisted.hasKey(PLAYER_ORBIT_RETURN_CONTEXT_TAG, NBT.TAG_COMPOUND);
		if(savedReturn) {
			NBTTagCompound context = persisted.getCompoundTag(PLAYER_ORBIT_RETURN_CONTEXT_TAG);
			targetDimension = context.getInteger(PLAYER_ORBIT_RETURN_DIMENSION_TAG);
			baseX = context.getInteger(PLAYER_ORBIT_RETURN_X_TAG);
			baseZ = context.getInteger(PLAYER_ORBIT_RETURN_Z_TAG);
		} else {
			World targetWorld = getOrLoadWorld(targetDimension);
			if(!(targetWorld instanceof WorldServer)) {
				targetDimension = 0;
				targetWorld = getOrLoadWorld(0);
			}
			if(!(targetWorld instanceof WorldServer)) return false;
			ChunkCoordinates spawn = ((WorldServer)targetWorld).getSpawnPoint();
			baseX = spawn.posX;
			baseZ = spawn.posZ;
		}

		World targetWorld = getOrLoadWorld(targetDimension);
		if(!(targetWorld instanceof WorldServer)) {
			targetDimension = 0;
			targetWorld = getOrLoadWorld(0);
			if(!(targetWorld instanceof WorldServer)) return false;
			ChunkCoordinates spawn = ((WorldServer)targetWorld).getSpawnPoint();
			baseX = spawn.posX;
			baseZ = spawn.posZ;
		}

		int[] returnPoint = resolveSafeSurfaceReturnPoint(player, (WorldServer)targetWorld, baseX, baseZ);
		if(returnPoint == null) {
			MainRegistry.logger.warn("[StationMaintenance] Refusing unsafe orbital return for "
				+ player.getCommandSenderName() + " in dimension " + targetDimension
				+ "; no Wilderness/own-faction landing point was found near launch or spawn.");
			return false;
		}
		double targetX = returnPoint[0] + 0.5D;
		double targetZ = returnPoint[1] + 0.5D;

		Entity riding = player.ridingEntity;
		if(riding instanceof EntityRideableRocket && !riding.isDead) {
			((EntityRideableRocket)riding).prepareForcedSurfaceLanding(targetDimension, returnPoint[0], returnPoint[1]);
		} else {
			player.mountEntity(null);
		}

		CelestialTeleporter.teleport(player, targetDimension, targetX, 800D, targetZ, false);
		persisted.removeTag(PLAYER_ORBIT_CONTEXT_TAG);
		persisted.removeTag(PLAYER_ORBIT_RETURN_CONTEXT_TAG);
		if(message != null && !message.isEmpty()) {
			if(!Integrations.isWGCoreActive()
				|| !Integrations.notifyPlayerWGC(player.worldObj, player.getUniqueID(), message)) {
				ChatComponentText component = new ChatComponentText(message);
				component.setChatStyle(new ChatStyle().setColor(EnumChatFormatting.YELLOW));
				player.addChatMessage(component);
			}
		}
		return true;
	}

	private int[] resolveSafeSurfaceReturnPoint(EntityPlayerMP player, WorldServer targetWorld, int baseX, int baseZ) {
		int preferredRadius = Math.max(16, SpaceConfig.orbitalReturnRadiusBlocks);
		if(Integrations.isWGCoreActive()) {
			int configuredRadius = Integrations.getOrbitalReturnRadiusBlocksWGC(targetWorld);
			if(configuredRadius >= 16) preferredRadius = configuredRadius;
		}

		int[] point = findSafeSurfaceReturnPoint(player, targetWorld, baseX, baseZ, 16, preferredRadius, 8);
		if(point != null) return point;

		// If the preferred launch-area window is entirely hostile/admin territory,
		// walk outward by chunk-sized rings rather than dropping the player into an
		// illegal claim. World spawn is only used when it is legal; otherwise search
		// around spawn and refuse the return if no legal fallback can be found.
		int maxExpandedRadius = Math.max(preferredRadius, 2048);
		point = findSafeSurfaceReturnPoint(player, targetWorld, baseX, baseZ,
			preferredRadius + 16, maxExpandedRadius, 16);
		if(point != null) return point;

		ChunkCoordinates spawn = targetWorld.getSpawnPoint();
		if(!Integrations.isWGCoreActive()
			|| Integrations.isSafeOrbitalReturnLocationWGC(targetWorld, player.getUniqueID(), spawn.posX, spawn.posZ)) {
			return new int[] {spawn.posX, spawn.posZ};
		}

		// WGCore may also protect world spawn. Search around it before accepting the
		// absolute fallback so we still prefer Wilderness/own territory.
		point = findSafeSurfaceReturnPoint(player, targetWorld, spawn.posX, spawn.posZ, 16, 2048, 16);
		return point;
	}

	private int[] findSafeSurfaceReturnPoint(EntityPlayerMP player, WorldServer world,
		int baseX, int baseZ, int minRadius, int maxRadius, int step) {
		int safeStep = Math.max(1, step);
		int start = Math.max(12, minRadius);
		for(int radius = start; radius <= maxRadius; radius += safeStep) {
			for(int offset = -radius; offset <= radius; offset += safeStep) {
				int[][] candidates = new int[][] {
					{baseX + offset, baseZ - radius},
					{baseX + radius, baseZ + offset},
					{baseX - offset, baseZ + radius},
					{baseX - radius, baseZ - offset}
				};
				for(int[] candidate : candidates) {
					if(isSafeSurfaceReturnCandidate(player, world, candidate[0], candidate[1])) return candidate;
				}
			}
		}
		return null;
	}

	private boolean isSafeSurfaceReturnCandidate(EntityPlayerMP player, WorldServer world, int blockX, int blockZ) {
		if(player == null || world == null) return false;
		if(Integrations.isWGCoreActive()
			&& !Integrations.isSafeOrbitalReturnLocationWGC(world, player.getUniqueID(), blockX, blockZ)) {
			return false;
		}
		return true;
	}

	private static World getOrLoadWorld(int dimensionId) {
		World world = DimensionManager.getWorld(dimensionId);
		if(world != null) return world;
		try {
			DimensionManager.initDimension(dimensionId);
		} catch(RuntimeException ex) {
			return null;
		}
		return DimensionManager.getWorld(dimensionId);
	}

	private static NBTTagCompound getPersistedPlayerData(EntityPlayer player) {
		NBTTagCompound entityData = player.getEntityData();
		NBTTagCompound persisted = entityData.getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG);
		entityData.setTag(EntityPlayer.PERSISTED_NBT_TAG, persisted);
		return persisted;
	}

	@Override
	public synchronized void readFromNBT(NBTTagCompound nbt) {
		traitMap.clear();
		stations.clear();
		raidDriveAuthorizations.clear();
		stationGenerations.clear();
		cleanupTasks.clear();
		raidRuntimeClockMigrationPending = !nbt.getBoolean(RAID_RUNTIME_CLOCK_TAG);

		for(CelestialBody body : CelestialBody.getAllBodies()) {
			if(nbt.hasKey("b_" + body.name)) {
				NBTTagCompound data = nbt.getCompoundTag("b_" + body.name);
				HashMap<Class<? extends CelestialBodyTrait>, CelestialBodyTrait> traits = new HashMap<Class<? extends CelestialBodyTrait>, CelestialBodyTrait>();
				for(Entry<String, Class<? extends CelestialBodyTrait>> entry : CelestialBodyTrait.traitMap.entrySet()) {
					if(data.hasKey(entry.getKey())) {
						try {
							CelestialBodyTrait trait = entry.getValue().newInstance();
							trait.readFromNBT(data.getCompoundTag(entry.getKey()));
							traits.put(trait.getClass(), trait);
						} catch(Exception ex) { }
					}
				}
				traitMap.put(body.name, traits);
			}
		}

		NBTTagList generationList = nbt.getTagList(GENERATIONS_TAG, NBT.TAG_COMPOUND);
		for(int i = 0; i < generationList.tagCount(); i++) {
			NBTTagCompound tag = generationList.getCompoundTagAt(i);
			stationGenerations.put(new ChunkCoordIntPair(tag.getInteger("x"), tag.getInteger("z")), Math.max(0, tag.getInteger("generation")));
		}

		NBTTagList stationList = nbt.getTagList("stations", NBT.TAG_COMPOUND);
		for(int i = 0; i < stationList.tagCount(); i++) {
			NBTTagCompound tag = stationList.getCompoundTagAt(i);
			int x = tag.getInteger("x");
			int z = tag.getInteger("z");
			CelestialBody orbiting = CelestialBody.getBody(tag.getString("orbiting"));
			if(orbiting == null) orbiting = CelestialBody.getBody(0);
			CelestialBody target = CelestialBody.getBody(tag.getString("target"));
			if(target == null) target = orbiting;

			int ordinal = tag.getInteger("state");
			StationState state = ordinal >= 0 && ordinal < StationState.values().length ? StationState.values()[ordinal] : StationState.ORBIT;
			OrbitalStation station = new OrbitalStation(orbiting, x, z);
			station.target = target;
			station.state = state;
			station.stateTimer = Math.max(0, tag.getInteger("stateTimer"));
			station.maxStateTimer = Math.max(0, tag.getInteger("maxStateTimer"));
			station.hasStation = tag.getBoolean("hasStation");
			station.name = tag.getString("name");
			station.gravityMultiplier = tag.hasKey("gravity") ? tag.getFloat("gravity") : 1F;
			if(Float.isNaN(station.gravityMultiplier) || Float.isInfinite(station.gravityMultiplier)) station.gravityMultiplier = 1F;

			station.generation = Math.max(0, tag.getInteger("stationGeneration"));
			station.stationKey = tag.getString("stationKey");
			station.reservedForLaunch = tag.getBoolean("reservedForLaunch");
			station.deleting = tag.getBoolean("deleting");
			station.driveOwnerId = tag.getString("driveOwnerId");
			station.driveOwnerIsFaction = tag.getBoolean("driveOwnerIsFaction");
			station.computerRequired = tag.hasKey("computerRequired") && tag.getBoolean("computerRequired");
			station.hasComputer = tag.getBoolean("hasComputer");
			station.computerX = tag.hasKey("computerX") ? tag.getInteger("computerX") : Integer.MIN_VALUE;
			station.computerY = tag.hasKey("computerY") ? tag.getInteger("computerY") : Integer.MIN_VALUE;
			station.computerZ = tag.hasKey("computerZ") ? tag.getInteger("computerZ") : Integer.MIN_VALUE;
			station.computerCrashTicksRemaining = tag.hasKey("computerCrashTicks") ? tag.getLong("computerCrashTicks") : -1L;
			long savedComputerWarning = tag.hasKey("computerNextWarningTicks") ? tag.getLong("computerNextWarningTicks") : -1L;
			station.computerNextWarningTicks = normalizeComputerWarningThreshold(station.computerCrashTicksRemaining, savedComputerWarning);
			station.raidPortActive = tag.getBoolean("raidPortActive");
			station.raidToken = tag.getString("raidToken");
			station.raidPortX = tag.getInteger("raidPortX");
			station.raidPortY = tag.hasKey("raidPortY") ? tag.getInteger("raidPortY") : OrbitalStation.CORE_Y;
			station.raidPortZ = tag.getInteger("raidPortZ");
			station.raidExpiresAt = tag.getLong("raidExpiresAt");
			station.raidCleanupAt = tag.getLong("raidCleanupAt");
			station.raidExpirationWarningSent = tag.hasKey("raidExpirationWarningSent") && tag.getBoolean("raidExpirationWarningSent");
			station.raidLastWarningInterval = tag.hasKey("raidLastWarningInterval")
				? Math.max(-1L, tag.getLong("raidLastWarningInterval"))
				: (station.raidExpirationWarningSent ? 0L : -1L);
			station.ensureIdentity();

			ChunkCoordIntPair pos = new ChunkCoordIntPair(x, z);
			Integer next = stationGenerations.get(pos);
			if(next == null || next.intValue() < station.generation) stationGenerations.put(pos, station.generation);
			stations.put(pos, station);
		}

		NBTTagList authorizationList = nbt.getTagList(RAID_DRIVE_AUTHORIZATIONS_TAG, NBT.TAG_COMPOUND);
		for(int i = 0; i < authorizationList.tagCount(); i++) {
			NBTTagCompound tag = authorizationList.getCompoundTagAt(i);
			String token = tag.getString("token");
			long expiresAt = tag.getLong("expiresAt");
			if(token.isEmpty() || expiresAt <= 0L) continue;
			raidDriveAuthorizations.put(token, new RaidDriveAuthorization(token, tag.getInteger("x"), tag.getInteger("z"),
				tag.getString("stationKey"), Math.max(0, tag.getInteger("stationGeneration")), expiresAt,
				tag.getString("ownerFactionId"), tag.getBoolean("wgcoreManaged")));
		}

		NBTTagList taskList = nbt.getTagList(CLEANUP_TASKS_TAG, NBT.TAG_COMPOUND);
		for(int i = 0; i < taskList.tagCount(); i++) {
			CleanupTask task = CleanupTask.read(taskList.getCompoundTagAt(i));
			if(task != null) cleanupTasks.add(task);
		}

		// Migrate old permanent tombstones into resumable cleanup tasks. The cell becomes
		// reusable after cleanup, while its next generation invalidates stale drives.
		NBTTagList deletedList = nbt.getTagList(LEGACY_DELETED_STATIONS_TAG, NBT.TAG_COMPOUND);
		for(int i = 0; i < deletedList.tagCount(); i++) {
			NBTTagCompound tag = deletedList.getCompoundTagAt(i);
			int x = tag.getInteger("x");
			int z = tag.getInteger("z");
			ChunkCoordIntPair pos = new ChunkCoordIntPair(x, z);
			Integer generation = stationGenerations.get(pos);
			stationGenerations.put(pos, Math.max(1, generation == null ? 1 : generation.intValue()));
			if(!isCleanupQueued(CleanupType.STATION, x, z, null)) {
				cleanupTasks.add(CleanupTask.station(x, z, "", 0, CelestialBody.getBody(0)));
			}
		}

	}

	@Override
	public synchronized void writeToNBT(NBTTagCompound nbt) {
		ensureRaidRuntimeClock();
		nbt.setBoolean(RAID_RUNTIME_CLOCK_TAG, !raidRuntimeClockMigrationPending);
		for(Entry<String, HashMap<Class<? extends CelestialBodyTrait>, CelestialBodyTrait>> entry : traitMap.entrySet()) {
			NBTTagCompound data = new NBTTagCompound();
			for(CelestialBodyTrait trait : entry.getValue().values()) {
				String name = CelestialBodyTrait.traitMap.inverse().get(trait.getClass());
				NBTTagCompound traitData = new NBTTagCompound();
				trait.writeToNBT(traitData);
				data.setTag(name, traitData);
			}
			nbt.setTag("b_" + entry.getKey(), data);
		}

		NBTTagList stationList = new NBTTagList();
		for(OrbitalStation station : stations.values()) {
			if(station == null || station.orbiting == null) continue;
			if(station.target == null) station.target = station.orbiting;
			station.ensureIdentity();
			NBTTagCompound tag = new NBTTagCompound();
			tag.setInteger("x", station.dX);
			tag.setInteger("z", station.dZ);
			tag.setString("orbiting", station.orbiting.name);
			tag.setString("target", station.target.name);
			tag.setInteger("state", station.state == null ? StationState.ORBIT.ordinal() : station.state.ordinal());
			tag.setInteger("stateTimer", station.stateTimer);
			tag.setInteger("maxStateTimer", station.maxStateTimer);
			tag.setBoolean("hasStation", station.hasStation);
			tag.setString("name", station.name == null ? "" : station.name);
			tag.setFloat("gravity", station.gravityMultiplier);
			tag.setString("stationKey", station.stationKey);
			tag.setInteger("stationGeneration", station.generation);
			tag.setBoolean("reservedForLaunch", station.reservedForLaunch);
			tag.setBoolean("deleting", station.deleting);
			tag.setString("driveOwnerId", station.driveOwnerId == null ? "" : station.driveOwnerId);
			tag.setBoolean("driveOwnerIsFaction", station.driveOwnerIsFaction);
			tag.setBoolean("computerRequired", station.computerRequired);
			tag.setBoolean("hasComputer", station.hasComputer);
			tag.setInteger("computerX", station.computerX);
			tag.setInteger("computerY", station.computerY);
			tag.setInteger("computerZ", station.computerZ);
			tag.setLong("computerCrashTicks", station.computerCrashTicksRemaining);
			tag.setLong("computerNextWarningTicks", station.computerNextWarningTicks);
			tag.setBoolean("raidPortActive", station.raidPortActive);
			tag.setString("raidToken", station.raidToken == null ? "" : station.raidToken);
			tag.setInteger("raidPortX", station.raidPortX);
			tag.setInteger("raidPortY", station.raidPortY);
			tag.setInteger("raidPortZ", station.raidPortZ);
			tag.setLong("raidExpiresAt", station.raidExpiresAt);
			tag.setLong("raidCleanupAt", station.raidCleanupAt);
			tag.setLong("raidLastWarningInterval", station.raidLastWarningInterval);
			tag.setBoolean("raidExpirationWarningSent", station.raidLastWarningInterval >= 0L);
			stationList.appendTag(tag);
		}
		nbt.setTag("stations", stationList);

		cleanupRaidDriveAuthorizations(currentServerRuntimeMillis());
		NBTTagList authorizationList = new NBTTagList();
		for(RaidDriveAuthorization authorization : raidDriveAuthorizations.values()) {
			NBTTagCompound tag = new NBTTagCompound();
			tag.setString("token", authorization.token);
			tag.setInteger("x", authorization.x);
			tag.setInteger("z", authorization.z);
			tag.setString("stationKey", authorization.stationKey);
			tag.setInteger("stationGeneration", authorization.generation);
			tag.setLong("expiresAt", authorization.expiresAt);
			tag.setString("ownerFactionId", authorization.ownerFactionId);
			tag.setBoolean("wgcoreManaged", authorization.wgcoreManaged);
			authorizationList.appendTag(tag);
		}
		nbt.setTag(RAID_DRIVE_AUTHORIZATIONS_TAG, authorizationList);

		NBTTagList generationList = new NBTTagList();
		for(Entry<ChunkCoordIntPair, Integer> entry : stationGenerations.entrySet()) {
			NBTTagCompound tag = new NBTTagCompound();
			tag.setInteger("x", entry.getKey().chunkXPos);
			tag.setInteger("z", entry.getKey().chunkZPos);
			tag.setInteger("generation", Math.max(0, entry.getValue()));
			generationList.appendTag(tag);
		}
		nbt.setTag(GENERATIONS_TAG, generationList);

		NBTTagList taskList = new NBTTagList();
		for(CleanupTask task : cleanupTasks) taskList.appendTag(task.write());
		nbt.setTag(CLEANUP_TASKS_TAG, taskList);
	}

	public void setTraits(String bodyName, CelestialBodyTrait... traits) {
		if(traits.length == 0) { clearTraits(bodyName); return; }
		HashMap<Class<? extends CelestialBodyTrait>, CelestialBodyTrait> newTraits = new HashMap<Class<? extends CelestialBodyTrait>, CelestialBodyTrait>();
		for(CelestialBodyTrait trait : traits) newTraits.put(trait.getClass(), trait);
		traitMap.put(bodyName, newTraits);
		markDirty();
	}

	public void clearTraits(String bodyName) { traitMap.remove(bodyName); markDirty(); }
	public HashMap<Class<? extends CelestialBodyTrait>, CelestialBodyTrait> getTraits(String bodyName) { return traitMap.get(bodyName); }
	public HashMap<ChunkCoordIntPair, OrbitalStation> getStations() { return stations; }
	public OrbitalStation getStationAtGrid(int x, int z) { return stations.get(new ChunkCoordIntPair(x, z)); }

	public OrbitalStation getStationFromPosition(int x, int z) {
		ChunkCoordIntPair pos = new ChunkCoordIntPair(MathHelper.floor_double((double)x / OrbitalStation.STATION_SIZE), MathHelper.floor_double((double)z / OrbitalStation.STATION_SIZE));
		return stations.get(pos);
	}

	public synchronized ChunkCoordIntPair findFreeSpace() { return findSafeFreeSpace(); }

	private static String normalizeStationName(String name) {
		return name == null ? "" : name.trim();
	}

	/** Active/reserved stations own their player-facing name; identity-bound cleanup does not. */
	public synchronized boolean isStationNameInUse(String name) {
		String normalized = normalizeStationName(name);
		if(normalized.isEmpty()) return false;
		for(OrbitalStation station : stations.values()) {
			if(station == null || station.deleting || station.name == null || !station.name.trim().equalsIgnoreCase(normalized)) continue;
			if(station.hasStation || station.reservedForLaunch) return true;
		}
		return false;
	}

	/** Applies GUI renames without allowing them to bypass the authoritative uniqueness rule. */
	public synchronized boolean renameStation(OrbitalStation station, String name) {
		if(station == null || station.deleting || getStationAtGrid(station.dX, station.dZ) != station) return false;
		String normalized = normalizeStationName(name);
		if(normalized.isEmpty() || normalized.length() > 64) return false;
		for(OrbitalStation other : stations.values()) {
			if(other == null || other == station || other.name == null) continue;
			if(!other.name.trim().equalsIgnoreCase(normalized)) continue;
			if(other.hasStation || other.reservedForLaunch || other.deleting) return false;
		}
		station.name = normalized;
		markDirty();
		return true;
	}

	/** Finds a truly empty 64x64 station cell; reserved/cleaning cells are never reused. */
	public synchronized ChunkCoordIntPair findSafeFreeSpace() {
		int size = Math.max(1, SpaceConfig.maxStationDistance / OrbitalStation.STATION_SIZE);
		int diameter = size * 2;
		long totalCells = (long)diameter * (long)diameter;
		int randomAttempts = (int)Math.min(4096L, totalCells);
		for(int i = 0; i < randomAttempts; i++) {
			ChunkCoordIntPair pos = new ChunkCoordIntPair(rand.nextInt(diameter) - size, rand.nextInt(diameter) - size);
			if(stations.containsKey(pos) || isAnyCleanupQueuedAt(pos.chunkXPos, pos.chunkZPos)) continue;
			return pos;
		}

		// With the normal/default allocation radius the complete grid is small enough to
		// prove exhaustion instead of reporting a false failure after unlucky random probes.
		if(totalCells <= 262144L) {
			for(int x = -size; x < size; x++) {
				for(int z = -size; z < size; z++) {
					ChunkCoordIntPair pos = new ChunkCoordIntPair(x, z);
					if(stations.containsKey(pos) || isAnyCleanupQueuedAt(x, z)) continue;
					return pos;
				}
			}
		}
		MainRegistry.logger.warn("[OrbitalStation] No free 64x64 station cell within +/-"
			+ SpaceConfig.maxStationDistance + " blocks (grid cells=" + totalCells
			+ ", station records=" + stations.size() + ", cleanup tasks=" + cleanupTasks.size() + ").");
		return null;
	}

	public synchronized OrbitalStation findStationByDriveOwner(World world, UUID ownerId, boolean factionOwner) {
		if(ownerId == null) return null;
		String expected = ownerId.toString();
		for(OrbitalStation station : stations.values()) {
			if(station == null || station.deleting || (!station.hasStation && !station.reservedForLaunch)) continue;
			if(expected.equals(station.driveOwnerId) && station.driveOwnerIsFaction == factionOwner) return station;

			// Migration bridge for stations created before the terminal existed. Active WGCore stations already
			// have authoritative ownership in WGCore, so adopt that binding once and persist it in HBM.
			if(factionOwner && (station.driveOwnerId == null || station.driveOwnerId.isEmpty()) && station.hasStation) {
				UUID registeredOwner = Integrations.getOrbitalStationOwnerWGC(world, station.stationKey, station.generation);
				if(ownerId.equals(registeredOwner)) {
					station.driveOwnerId = expected;
					station.driveOwnerIsFaction = true;
					markDirty();
					return station;
				}
			}
		}
		return null;
	}

	public synchronized OrbitalStation getOrCreateDriveOwnerStation(World world, CelestialBody orbiting, String name, UUID ownerId, boolean factionOwner) {
		if(ownerId == null) return null;
		OrbitalStation existing = findStationByDriveOwner(world, ownerId, factionOwner);
		if(existing != null) {
			String normalized = normalizeStationName(name);
			if(!normalized.isEmpty() && !normalized.equals(existing.name)) renameStation(existing, normalized);
			return existing;
		}

		// Conservative migration for an unlaunched command-created reservation: only adopt a unique exact
		// name match with no prior owner binding. This prevents a lost pre-launch drive from stranding the faction.
		List<OrbitalStation> named = findStationsByName(name, true);
		if(named.size() == 1) {
			OrbitalStation candidate = named.get(0);
			if(candidate.driveOwnerId == null || candidate.driveOwnerId.isEmpty()) {
				candidate.driveOwnerId = ownerId.toString();
				candidate.driveOwnerIsFaction = factionOwner;
				markDirty();
				return candidate;
			}
		}

		OrbitalStation created = reserveStation(orbiting, name);
		if(created != null) {
			created.driveOwnerId = ownerId.toString();
			created.driveOwnerIsFaction = factionOwner;
			markDirty();
		}
		return created;
	}

	public synchronized OrbitalStation getStationByIdentity(String stationKey, int generation, boolean requireActive) {
		if(stationKey == null || stationKey.trim().isEmpty()) return null;
		for(OrbitalStation station : stations.values()) {
			if(station == null || station.deleting || station.generation != generation) continue;
			station.ensureIdentity();
			if(!stationKey.equals(station.stationKey)) continue;
			if(requireActive && !station.hasStation) return null;
			return station;
		}
		return null;
	}

	public synchronized ItemStack programRaidDrive(ItemStack blankDrive, OrbitalStation station) {
		return programRaidDrive(blankDrive, station, null, false);
	}

	/**
	 * Programs another physical Breach Drive into one shared authorization group.
	 * All drives in the group carry the same token and therefore reuse one outpost
	 * and one authoritative timer. With WGCore installed, WGCore supplies that
	 * timer; standalone HBM uses stationCodeLifetimeSeconds.
	 */
	public synchronized ItemStack programRaidDrive(ItemStack blankDrive, OrbitalStation station,
	                                               UUID ownerFactionId, boolean wgcoreManaged) {
		if(!ItemRaidDrive.isUnprogrammed(blankDrive) || station == null || !station.hasStation || station.deleting
				|| getStationAtGrid(station.dX, station.dZ) != station) return null;
		station.ensureIdentity();
		ensureRaidRuntimeClock();
		long now = currentServerRuntimeMillis();
		if(now < 0L) return null;

		World authorityWorld = getAuthorityWorld();
		long remaining;
		if(wgcoreManaged) {
			if(ownerFactionId == null || authorityWorld == null) return null;
			remaining = Integrations.getBreachDriveAccessRemainingMillisWGC(
				authorityWorld, ownerFactionId, station.stationKey, station.generation);
			if(remaining <= 0L) return null;
		} else {
			remaining = Math.max(1L, SpaceConfig.stationCodeLifetimeSeconds) * 1000L;
		}

		RaidDriveAuthorization shared = findReusableRaidAuthorization(station, ownerFactionId, wgcoreManaged, now);
		if(shared == null) {
			String token = UUID.randomUUID().toString();
			long expiresAt = safeAdd(now, remaining);
			shared = new RaidDriveAuthorization(token, station.dX, station.dZ, station.stationKey,
				station.generation, expiresAt, ownerFactionId == null ? "" : ownerFactionId.toString(), wgcoreManaged);
			raidDriveAuthorizations.put(token, shared);
		} else if(wgcoreManaged) {
			shared.expiresAt = safeAdd(now, remaining);
		}

		ItemStack programmed = ItemRaidDrive.createProgrammed(station, shared.expiresAt, 1,
			shared.token, ownerFactionId, wgcoreManaged);
		if(programmed == null || !matchesDriveIdentity(station, programmed, true)) return null;
		markDirty();
		return programmed;
	}

	private RaidDriveAuthorization findReusableRaidAuthorization(OrbitalStation station, UUID ownerFactionId,
	                                                            boolean wgcoreManaged, long now) {
		String owner = ownerFactionId == null ? "" : ownerFactionId.toString();
		if(station.raidPortActive && station.raidToken != null && !station.raidToken.isEmpty()) {
			RaidDriveAuthorization active = raidDriveAuthorizations.get(station.raidToken);
			if(matchesAuthorizationOwner(active, station, owner, wgcoreManaged, now)) return active;
		}
		for(RaidDriveAuthorization authorization : raidDriveAuthorizations.values()) {
			if(matchesAuthorizationOwner(authorization, station, owner, wgcoreManaged, now)) return authorization;
		}
		return null;
	}

	private boolean matchesAuthorizationOwner(RaidDriveAuthorization authorization, OrbitalStation station,
	                                         String owner, boolean wgcoreManaged, long now) {
		if(authorization == null || station == null || authorization.wgcoreManaged != wgcoreManaged) return false;
		if(authorization.x != station.dX || authorization.z != station.dZ
				|| authorization.generation != station.generation
				|| !authorization.stationKey.equals(station.stationKey)
				|| !authorization.ownerFactionId.equals(owner)) return false;
		if(!wgcoreManaged) return authorization.expiresAt > now;
		if(authorization.expiresAt <= now) return false;
		World authorityWorld = getAuthorityWorld();
		UUID factionId = parseUuid(owner);
		return authorityWorld != null && factionId != null
			&& Integrations.getBreachDriveAccessRemainingMillisWGC(authorityWorld, factionId,
				station.stationKey, station.generation) > 0L;
	}

	private World getAuthorityWorld() {
		World world = DimensionManager.getWorld(0);
		if(world != null) return world;
		World[] worlds = DimensionManager.getWorlds();
		return worlds.length > 0 ? worlds[0] : null;
	}

	private static UUID parseUuid(String value) {
		if(value == null || value.isEmpty()) return null;
		try { return UUID.fromString(value); } catch(IllegalArgumentException ignored) { return null; }
	}

	public synchronized OrbitalStation reserveStation(CelestialBody orbiting, String name) {
		String normalizedName = normalizeStationName(name);
		if(normalizedName.isEmpty() || isStationNameInUse(normalizedName)) return null;
		ChunkCoordIntPair pos = findSafeFreeSpace();
		if(pos == null) return null;
		int generation = getNextGeneration(pos);
		OrbitalStation station = new OrbitalStation(orbiting == null ? CelestialBody.getBody(0) : orbiting, pos.chunkXPos, pos.chunkZPos);
		station.target = station.orbiting;
		station.name = normalizedName;
		station.generation = generation;
		station.stationKey = UUID.randomUUID().toString();
		station.reservedForLaunch = true;
		station.hasStation = false;
		station.state = StationState.ORBIT;
		stations.put(pos, station);
		markDirty();
		return station;
	}

	public OrbitalStation addStation(CelestialBody orbiting) {
		ChunkCoordIntPair pos = findSafeFreeSpace();
		return pos == null ? null : addStation(pos.chunkXPos, pos.chunkZPos, orbiting);
	}

	/** Legacy/debug creation; never overwrites a reservation or cleanup. */
	public synchronized OrbitalStation addStation(int x, int z, CelestialBody orbiting) {
		ChunkCoordIntPair pos = new ChunkCoordIntPair(x, z);
		if(isAnyCleanupQueuedAt(x, z)) return null;
		OrbitalStation station = stations.get(pos);
		if(station == null) {
			station = new OrbitalStation(orbiting == null ? CelestialBody.getBody(0) : orbiting, x, z);
			station.generation = getNextGeneration(pos);
			station.stationKey = UUID.randomUUID().toString();
			stations.put(pos, station);
		}
		markDirty();
		return station;
	}

	public synchronized void removeStation(OrbitalStation station) { if(station != null) removeStation(station.dX, station.dZ); }
	public synchronized void removeStation(int x, int z) {
		ChunkCoordIntPair pos = new ChunkCoordIntPair(x, z);
		OrbitalStation station = stations.get(pos);
		if(station == null || station.hasStation || station.deleting) return;

		// This is the first deletion action. Clear matching normal station drives
		// from online player inventories, then loaded rocket launch pads, before the station record is changed.
		deprogramPriorityDrivesBeforeDeletion(station);

		stations.remove(pos);
		markDirty();
	}

	/** Queues the authoritative bounded physical and logical station deletion. */
	public synchronized boolean deleteActiveStation(OrbitalStation station) {
		if(station == null || !station.hasStation || station.deleting) return false;
		return queueStationDeletion(station);
	}

	public synchronized boolean queueStationDeletion(OrbitalStation station) {
		return queueStationDeletion(station, false);
	}

	private synchronized boolean queueBreachSettlementDeletion(OrbitalStation station) {
		if(station == null || station.stationKey == null || station.stationKey.isEmpty()) return false;
		World integrationWorld = getIntegrationWorld();
		if(integrationWorld == null
			|| !Integrations.isBreachSettlementCompleteWGC(integrationWorld, station.stationKey, station.generation)) {
			return false;
		}
		return queueStationDeletion(station, true);
	}

	private synchronized boolean queueStationDeletion(OrbitalStation station, boolean breachSettlement) {
		if(station == null || station.deleting) return false;

		// This is the first deletion action. Physical cleanup may take many ticks,
		// so online player inventories and loaded rocket launch pads are deprogrammed first.
		deprogramPriorityDrivesBeforeDeletion(station);

		station.deleting = true;
		station.reservedForLaunch = false;
		station.computerCrashTicksRemaining = -1L;
		station.computerNextWarningTicks = -1L;
		invalidateRaidDriveAuthorizationsForStation(station.dX, station.dZ);
		if(!isCleanupQueued(CleanupType.STATION, station.dX, station.dZ, station.stationKey)) {
			CleanupTask task = CleanupTask.station(station.dX, station.dZ, station.stationKey, station.generation, station.orbiting);
			task.playerDrivesDeprogrammed = true;
			task.breachSettlement = breachSettlement;
			cleanupTasks.add(task);
			if(breachSettlement) {
				MainRegistry.logger.info("[BreachSettlement] Queued defeated station-cell cleanup station="
					+ getStationId(station) + " key=" + shortIdentity(station.stationKey)
					+ " generation=" + station.generation + ".");
			}
		}
		markDirty();
		return true;
	}

	/** Compatibility query: true only while a cell is actively being removed. */
	public synchronized boolean isStationDeleted(int x, int z) {
		OrbitalStation station = getStationAtGrid(x, z);
		return (station != null && station.deleting) || isCleanupQueued(CleanupType.STATION, x, z, null);
	}

	public synchronized List<OrbitalStation> findActiveStationsByName(String name) {
		List<OrbitalStation> matches = new ArrayList<OrbitalStation>();
		if(name == null) return matches;
		String normalized = name.trim();
		for(OrbitalStation station : stations.values()) {
			if(station != null && station.hasStation && !station.deleting && station.name != null && station.name.trim().equalsIgnoreCase(normalized)) matches.add(station);
		}
		return matches;
	}

	public synchronized List<OrbitalStation> findStationsByName(String name, boolean includeReservations) {
		List<OrbitalStation> matches = new ArrayList<OrbitalStation>();
		if(name == null) return matches;
		String normalized = name.trim();
		for(OrbitalStation station : stations.values()) {
			if(station == null || station.deleting || station.name == null || !station.name.trim().equalsIgnoreCase(normalized)) continue;
			if(station.hasStation || (includeReservations && station.reservedForLaunch)) matches.add(station);
		}
		return matches;
	}

	public synchronized boolean cancelStationReservation(OrbitalStation station) {
		if(station == null || station.hasStation || !station.reservedForLaunch || station.deleting) return false;
		ChunkCoordIntPair pos = new ChunkCoordIntPair(station.dX, station.dZ);
		if(stations.get(pos) != station) return false;

		// This is the first deletion action for an unlaunched reservation.
		// Player inventories are handled first, followed by loaded rocket launch pads.
		deprogramPriorityDrivesBeforeDeletion(station);

		stations.remove(pos);
		stationGenerations.put(pos, Math.max(getNextGeneration(pos), station.generation + 1));
		invalidateRaidDriveAuthorizationsForStation(station.dX, station.dZ);
		resetLoadedNormalStationDrives(station.dX, station.dZ, station.stationKey, station.generation);
		markDirty();
		return true;
	}

	public static String getStationId(OrbitalStation station) { return station == null ? "0x00000000" : getStationId(station.dX, station.dZ); }
	public static String getStationId(int x, int z) { return "0x" + Integer.toHexString(new ChunkCoordIntPair(x, z).hashCode()).toUpperCase(Locale.ROOT); }

	/** Atomically programs and authorizes only the Raid Hard Drive in the selected player slot. */
	public synchronized ItemStack programHeldRaidDrive(EntityPlayer player, OrbitalStation station) {
		if(player == null || station == null || !station.hasStation || station.deleting || getStationAtGrid(station.dX, station.dZ) != station) return null;
		ItemStack held = player.getHeldItem();
		if(!ItemRaidDrive.isUnprogrammed(held)) return null;

		ItemStack rechecked = player.inventory.getStackInSlot(player.inventory.currentItem);
		if(rechecked != held || rechecked.getItem() != ModItems.raid_drive || !ItemRaidDrive.isUnprogrammed(rechecked)) return null;
		ItemStack programmed = programRaidDrive(rechecked, station);
		if(programmed == null) return null;
		player.inventory.setInventorySlotContents(player.inventory.currentItem, programmed);
		player.inventory.markDirty();
		if(player.inventoryContainer != null) player.inventoryContainer.detectAndSendChanges();
		return programmed;
	}

	public synchronized void invalidateRaidDriveAuthorizationsForStation(int x, int z) {
		Iterator<RaidDriveAuthorization> iterator = raidDriveAuthorizations.values().iterator();
		boolean changed = false;
		while(iterator.hasNext()) {
			RaidDriveAuthorization authorization = iterator.next();
			if(authorization.x == x && authorization.z == z) { iterator.remove(); changed = true; }
		}
		if(changed) markDirty();
	}

	private void cleanupRaidDriveAuthorizations(long now) {
		Iterator<RaidDriveAuthorization> iterator = raidDriveAuthorizations.values().iterator();
		boolean changed = false;
		World authorityWorld = getAuthorityWorld();
		while(iterator.hasNext()) {
			RaidDriveAuthorization authorization = iterator.next();
			boolean expired;
			if(authorization.wgcoreManaged && Integrations.isWGCoreActive()) {
				/*
				 * A WGCore-managed physical-drive token represents one authorization epoch.
				 * Once its synchronized deadline has elapsed it must never be revived by a
				 * later READY opportunity for the same faction/station pair.
				 */
				if(authorization.expiresAt <= 0L || now >= authorization.expiresAt) {
					expired = true;
				} else {
					UUID factionId = parseUuid(authorization.ownerFactionId);
					long remaining = authorityWorld != null && factionId != null
						? Integrations.getBreachDriveAccessRemainingMillisWGC(authorityWorld, factionId,
							authorization.stationKey, authorization.generation)
						: 0L;
					if(remaining > 0L) {
						authorization.expiresAt = safeAdd(now, remaining);
						expired = false;
					} else {
						expired = true;
					}
				}
			} else {
				expired = authorization.expiresAt <= 0L || now >= authorization.expiresAt;
			}
			if(expired) { iterator.remove(); changed = true; }
		}
		if(changed) markDirty();
	}

	private static long safeAdd(long value, long amount) {
		if(amount > 0L && value > Long.MAX_VALUE - amount) return Long.MAX_VALUE;
		return value + amount;
	}

	private long currentServerRuntimeMillis() {
		World overworld = DimensionManager.getWorld(0);
		if(overworld == null || overworld.isRemote) return -1L;
		long ticks = overworld.getTotalWorldTime();
		if(ticks <= 0L) return 0L;
		return ticks > Long.MAX_VALUE / 50L ? Long.MAX_VALUE : ticks * 50L;
	}

	private World getIntegrationWorld() {
		World orbitWorld = DimensionManager.getWorld(SpaceConfig.orbitDimension);
		if(orbitWorld != null) return orbitWorld;
		World overworld = DimensionManager.getWorld(0);
		if(overworld != null) return overworld;
		World[] worlds = DimensionManager.getWorlds();
		return worlds.length > 0 ? worlds[0] : null;
	}

	private void ensureRaidRuntimeClock() {
		if(!raidRuntimeClockMigrationPending) return;
		long runtimeNow = currentServerRuntimeMillis();
		if(runtimeNow < 0L) return;

		long wallNow = System.currentTimeMillis();
		for(OrbitalStation station : stations.values()) {
			if(station == null) continue;
			station.raidExpiresAt = migrateLegacyDeadline(station.raidExpiresAt, wallNow, runtimeNow);
			station.raidCleanupAt = migrateLegacyDeadline(station.raidCleanupAt, wallNow, runtimeNow);
		}
		for(RaidDriveAuthorization authorization : raidDriveAuthorizations.values()) {
			if(authorization != null) {
				authorization.expiresAt = migrateLegacyDeadline(authorization.expiresAt, wallNow, runtimeNow);
			}
		}
		raidRuntimeClockMigrationPending = false;
		markDirty();
	}

	private long migrateLegacyDeadline(long legacyDeadline, long wallNow, long runtimeNow) {
		if(legacyDeadline <= 0L) return 0L;
		long remaining = Math.max(0L, legacyDeadline - wallNow);
		return safeAdd(runtimeNow, remaining);
	}

	private int getNextGeneration(ChunkCoordIntPair pos) {
		Integer value = stationGenerations.get(pos);
		return value == null ? 0 : Math.max(0, value.intValue());
	}

	public synchronized boolean matchesDriveIdentity(OrbitalStation station, ItemStack drive, boolean requireActive) {
		if(station == null || drive == null || !drive.hasTagCompound()) return false;
		NBTTagCompound tag = drive.stackTagCompound;
		if(tag.hasKey(ItemVOTVdrive.TAG_STATION_GENERATION)) {
			if(tag.getInteger(ItemVOTVdrive.TAG_STATION_GENERATION) != station.generation) return false;
		} else if(station.generation != 0) {
			return false;
		}
		String key = tag.getString(ItemVOTVdrive.TAG_STATION_KEY);
		if(key.isEmpty()) {
			if(station.stationKey == null || !station.stationKey.startsWith("legacy:")) return false;
		} else if(!key.equals(station.stationKey)) {
			return false;
		}
		return !requireActive || (station.hasStation && !station.deleting);
	}

	/** True only when persisted generation history proves that this normal station drive is stale. */
	public synchronized boolean shouldResetNormalStationDrive(ItemStack drive) {
		if(!ItemVOTVdrive.isNormalStationDrive(drive) || !drive.hasTagCompound()) return false;
		ItemVOTVdrive.Destination destination = ItemVOTVdrive.getDestinationUnchecked(drive);
		if(destination == null || destination.body != SolarSystem.Body.ORBIT) return false;

		OrbitalStation station = getStationAtGrid(destination.x, destination.z);
		if(station != null && matchesDriveIdentity(station, drive, false)) return false;

		int driveGeneration = drive.stackTagCompound.hasKey(ItemVOTVdrive.TAG_STATION_GENERATION)
			? Math.max(0, drive.stackTagCompound.getInteger(ItemVOTVdrive.TAG_STATION_GENERATION)) : 0;
		Integer currentGeneration = stationGenerations.get(new ChunkCoordIntPair(destination.x, destination.z));
		return currentGeneration != null && currentGeneration.intValue() > driveGeneration;
	}

	private void deprogramPriorityDrivesBeforeDeletion(OrbitalStation station) {
		if(station == null) return;
		station.ensureIdentity();

		// Priority order for every station deletion: player inventories first,
		// then the destination-drive slots of all currently loaded rocket launch pads.
		resetPlayerNormalStationDrives(station.dX, station.dZ, station.stationKey, station.generation);
		resetLaunchPadNormalStationDrives(station.dX, station.dZ, station.stationKey, station.generation);
	}

	private void resetPlayerNormalStationDrives(int stationX, int stationZ, String stationKey, int generation) {
		for(World loadedWorld : DimensionManager.getWorlds()) {
			if(loadedWorld == null || loadedWorld.isRemote) continue;
			for(Object object : new ArrayList<Object>(loadedWorld.playerEntities)) {
				if(!(object instanceof EntityPlayer)) continue;
				EntityPlayer player = (EntityPlayer)object;
				if(resetInventoryForStation(player.inventory, stationX, stationZ, stationKey, generation)) {
					player.inventoryContainer.detectAndSendChanges();
				}
			}
		}
	}

	private void resetLaunchPadNormalStationDrives(int stationX, int stationZ, String stationKey, int generation) {
		for(World loadedWorld : DimensionManager.getWorlds()) {
			if(loadedWorld == null || loadedWorld.isRemote) continue;
			for(Object object : new ArrayList<Object>(loadedWorld.loadedTileEntityList)) {
				if(!(object instanceof TileEntityLaunchPadRocket)) continue;
				TileEntityLaunchPadRocket launchPad = (TileEntityLaunchPadRocket)object;
				if(ItemVOTVdrive.resetIfMatchesStation(launchPad.getStackInSlot(1), stationX, stationZ, stationKey, generation)) {
					launchPad.markDirty();
				}
			}
		}
	}

	private void resetLoadedNormalStationDrives(int stationX, int stationZ, String stationKey, int generation) {
		resetPlayerNormalStationDrives(stationX, stationZ, stationKey, generation);
		for(World loadedWorld : DimensionManager.getWorlds()) {
			if(loadedWorld == null || loadedWorld.isRemote) continue;

			for(Object object : new ArrayList<Object>(loadedWorld.loadedTileEntityList)) {
				if(object instanceof IInventory) resetInventoryForStation((IInventory)object, stationX, stationZ, stationKey, generation);
			}

			for(Object object : new ArrayList<Object>(loadedWorld.loadedEntityList)) {
				if(object instanceof EntityItem) {
					EntityItem entityItem = (EntityItem)object;
					ItemStack stack = entityItem.getEntityItem();
					if(ItemVOTVdrive.resetIfMatchesStation(stack, stationX, stationZ, stationKey, generation)) entityItem.setEntityItemStack(stack);
				} else if(object instanceof IInventory) {
					resetInventoryForStation((IInventory)object, stationX, stationZ, stationKey, generation);
				}

				if(object instanceof EntityRideableRocket) {
					EntityRideableRocket rocket = (EntityRideableRocket)object;
					if(ItemVOTVdrive.resetIfMatchesStation(rocket.navDrive, stationX, stationZ, stationKey, generation)) rocket.setDrive(rocket.navDrive);
				}
			}
		}
	}

	private boolean resetInventoryForStation(IInventory inventory, int stationX, int stationZ, String stationKey, int generation) {
		if(inventory == null) return false;
		boolean changed = false;
		for(int slot = 0; slot < inventory.getSizeInventory(); slot++) {
			if(ItemVOTVdrive.resetIfMatchesStation(inventory.getStackInSlot(slot), stationX, stationZ, stationKey, generation)) changed = true;
		}
		if(changed) inventory.markDirty();
		return changed;
	}

	private boolean matchesIdentity(OrbitalStation station, String key, int generation, boolean requireActive) {
		if(station == null || station.generation != generation) return false;
		if(key == null || key.isEmpty()) {
			if(station.stationKey == null || !station.stationKey.startsWith("legacy:")) return false;
		} else if(!key.equals(station.stationKey)) {
			return false;
		}
		return !requireActive || (station.hasStation && !station.deleting);
	}

	/** Lightweight launch validation used by GUIs/held-key polling without force-loading orbit. */
	public synchronized boolean canLaunchNormalDriveSaved(ItemStack drive) {
		if(drive == null || !ItemVOTVdrive.isNormalStationDrive(drive)) return false;
		ItemVOTVdrive.Destination destination = ItemVOTVdrive.getDestinationUnchecked(drive);
		if(destination == null || destination.body != SolarSystem.Body.ORBIT) return false;
		OrbitalStation station = getStationAtGrid(destination.x, destination.z);
		return station != null && !station.deleting && matchesDriveIdentity(station, drive, false)
			&& (station.hasStation || station.reservedForLaunch);
	}

	/** Performs the launch-time reservation and structure-space checks without changing saved state. */
	public synchronized boolean canLaunchNormalDrive(ItemStack drive, WorldServer orbitWorld) {
		if(drive == null || orbitWorld == null || !ItemVOTVdrive.isNormalStationDrive(drive)) return false;
		ItemVOTVdrive.Destination destination = ItemVOTVdrive.getDestinationUnchecked(drive);
		if(destination == null || destination.body != SolarSystem.Body.ORBIT) return false;
		OrbitalStation station = getStationAtGrid(destination.x, destination.z);
		if(station == null || station.deleting || !matchesDriveIdentity(station, drive, false)) return false;
		if(station.hasStation) return orbitWorld.getBlock(station.getCenterBlockX(), OrbitalStation.CORE_Y, station.getCenterBlockZ()) == ModBlocks.orbital_station;
		return station.reservedForLaunch && isCoreAreaEmpty(orbitWorld, station.getCenterBlockX(), station.getCenterBlockZ());
	}

	/** Creates the normal core and required computer only for its reserved drive. */
	public synchronized boolean activateNormalStation(ItemStack drive, CelestialBody sourceBody, WorldServer orbitWorld, UUID ownerFactionId) {
		if(drive == null || orbitWorld == null || !ItemVOTVdrive.isNormalStationDrive(drive)) return false;
		ItemVOTVdrive.Destination destination = ItemVOTVdrive.getDestinationUnchecked(drive);
		if(destination == null || destination.body != SolarSystem.Body.ORBIT) return false;
		OrbitalStation station = getStationAtGrid(destination.x, destination.z);
		if(station == null || station.deleting || !matchesDriveIdentity(station, drive, false)) return false;
		if(station.driveOwnerIsFaction && station.driveOwnerId != null && !station.driveOwnerId.isEmpty()) {
			if(ownerFactionId == null || !station.driveOwnerId.equals(ownerFactionId.toString())) return false;
		}
		if(station.hasStation) return orbitWorld.getBlock(station.getCenterBlockX(), OrbitalStation.CORE_Y, station.getCenterBlockZ()) == ModBlocks.orbital_station;
		if(!station.reservedForLaunch) return false;

		int x = station.getCenterBlockX();
		int z = station.getCenterBlockZ();
		if(!isCoreAreaEmpty(orbitWorld, x, z) || !OrbitalStation.spawn(orbitWorld, x, z)) return false;

		int computerX = x + 2;
		int computerY = OrbitalStation.CORE_Y + 2;
		int computerZ = z;
		boolean oldSafeRem = BlockDummyable.safeRem;
		BlockDummyable.safeRem = true;
		boolean computerPlaced;
		try {
			computerPlaced = orbitWorld.setBlock(computerX, computerY, computerZ, ModBlocks.orbital_station_computer, ForgeDirection.WEST.ordinal() + BlockDummyable.offset, 3);
		} finally {
			BlockDummyable.safeRem = oldSafeRem;
		}
		if(!computerPlaced || orbitWorld.getBlock(computerX, computerY, computerZ) != ModBlocks.orbital_station_computer) {
			clearSmallCoreArea(orbitWorld, x, z);
			return false;
		}

		station.ensureIdentity();
		if(!Integrations.registerOrbitalStationWGC(
			orbitWorld,
			station.stationKey,
			station.generation,
			ownerFactionId,
			SpaceConfig.orbitDimension,
			station.dX,
			station.dZ
		)) {
			clearSmallCoreArea(orbitWorld, x, z);
			return false;
		}

		station.orbiting = sourceBody == null ? station.orbiting : sourceBody;
		if(station.orbiting == null) station.orbiting = CelestialBody.getBody(0);
		station.target = station.orbiting;
		station.hasStation = true;
		station.reservedForLaunch = false;
		station.state = StationState.ORBIT;
		station.stateTimer = 0;
		station.maxStateTimer = 0;
		station.computerRequired = true;
		station.hasComputer = true;
		station.computerX = computerX;
		station.computerY = computerY;
		station.computerZ = computerZ;
		station.computerCrashTicksRemaining = -1L;
		station.computerNextWarningTicks = -1L;
		markDirty();
		return true;
	}

	/** True when another still-active Raid Hard Drive already owns this station's single raid port. */
	public synchronized boolean hasConflictingRaidPort(ItemStack drive) {
		OrbitalStation station = getStationForRaidDrive(drive, true);
		if(station == null || !station.raidPortActive || drive == null || !drive.hasTagCompound()) return false;
		String token = drive.stackTagCompound.getString(ItemRaidDrive.TAG_RAID_TOKEN);
		return token.isEmpty() || !token.equals(station.raidToken);
	}

	public synchronized boolean canPlayerUseRaidDrive(ItemStack drive, World world, UUID playerId) {
		OrbitalStation station = getStationForRaidDrive(drive, true);
		if(station == null) return false;
		if(!Integrations.isWGCoreActive()) return true;
		if(playerId == null) return false;
		UUID factionId = Integrations.getPlayerFaction(world, playerId);
		if(factionId == null) return false;
		String token = drive.stackTagCompound.getString(ItemRaidDrive.TAG_RAID_TOKEN);
		RaidDriveAuthorization authorization = raidDriveAuthorizations.get(token);
		return authorization != null && authorization.wgcoreManaged
			&& factionId.toString().equals(authorization.ownerFactionId)
			&& Integrations.getBreachDriveAccessRemainingMillisWGC(world, factionId,
				station.stationKey, station.generation) > 0L;
	}

	/**
	 * Returns the remaining lifetime for this exact physical-drive token.
	 * A newer READY opportunity for the same faction/station pair must not revive
	 * a token whose synchronized epoch has already ended.
	 */
	public synchronized long getRaidDriveAuthorizationRemainingMillis(ItemStack drive, World world) {
		if(drive == null || !drive.hasTagCompound()) return 0L;
		String token = drive.stackTagCompound.getString(ItemRaidDrive.TAG_RAID_TOKEN);
		if(token == null || token.isEmpty()) return 0L;
		RaidDriveAuthorization authorization = raidDriveAuthorizations.get(token);
		long now = currentServerRuntimeMillis();
		if(now < 0L) return 0L;
		if(authorization == null) {
			drive.stackTagCompound.setLong(ItemRaidDrive.TAG_EXPIRES_AT, now);
			return 0L;
		}
		if(authorization.expiresAt <= 0L || now >= authorization.expiresAt) {
			drive.stackTagCompound.setLong(ItemRaidDrive.TAG_EXPIRES_AT, now);
			raidDriveAuthorizations.remove(token);
			markDirty();
			return 0L;
		}
		if(!authorization.wgcoreManaged || !Integrations.isWGCoreActive()) {
			return Math.max(0L, authorization.expiresAt - now);
		}
		UUID factionId = parseUuid(authorization.ownerFactionId);
		World authorityWorld = world != null ? world : getAuthorityWorld();
		long remaining = authorityWorld != null && factionId != null
			? Integrations.getBreachDriveAccessRemainingMillisWGC(authorityWorld, factionId,
				authorization.stationKey, authorization.generation)
			: 0L;
		if(remaining <= 0L) {
			drive.stackTagCompound.setLong(ItemRaidDrive.TAG_EXPIRES_AT, now);
			raidDriveAuthorizations.remove(token);
			markDirty();
			return 0L;
		}
		authorization.expiresAt = safeAdd(now, remaining);
		drive.stackTagCompound.setLong(ItemRaidDrive.TAG_EXPIRES_AT, authorization.expiresAt);
		return remaining;
	}

	/** Lightweight Breach-drive validation that never initializes the orbital dimension. */
	public synchronized boolean canLaunchRaidDriveSaved(ItemStack drive) {
		OrbitalStation station = getStationForRaidDrive(drive, true);
		if(station == null || drive == null || !drive.hasTagCompound()) return false;
		String token = drive.stackTagCompound.getString(ItemRaidDrive.TAG_RAID_TOKEN);
		if(token == null || token.isEmpty()) return false;
		if(station.raidPortActive) return token.equals(station.raidToken);

		RaidDriveAuthorization authorization = raidDriveAuthorizations.get(token);
		long now = currentServerRuntimeMillis();
		if(authorization == null || now < 0L) return false;
		if(!authorization.wgcoreManaged) return authorization.expiresAt > now;
		if(authorization.expiresAt <= now) return false;

		World authorityWorld = getAuthorityWorld();
		UUID factionId = parseUuid(authorization.ownerFactionId);
		return authorityWorld != null && factionId != null
			&& Integrations.getBreachDriveAccessRemainingMillisWGC(authorityWorld, factionId,
				station.stationKey, station.generation) > 0L;
	}

	public synchronized boolean canLaunchRaidDrive(ItemStack drive, WorldServer orbitWorld) {
		OrbitalStation station = getStationForRaidDrive(drive, true);
		if(station == null || orbitWorld == null) return false;
		String token = drive.stackTagCompound.getString(ItemRaidDrive.TAG_RAID_TOKEN);
		long now = currentServerRuntimeMillis();
		if(now < 0L) return false;
		if(station.raidPortActive) return token.equals(station.raidToken)
			&& orbitWorld.getBlock(station.raidPortX, station.raidPortY, station.raidPortZ) == ModBlocks.orbital_station_raiding_port;
		return findRaidPortPosition(station, token, orbitWorld) != null;
	}

	/** Generates or reuses the one raid port associated with this drive token. */
	public synchronized boolean activateRaidPort(ItemStack drive, WorldServer orbitWorld, UUID attackerFactionId) {
		OrbitalStation station = getStationForRaidDrive(drive, true);
		if(station == null || orbitWorld == null) return false;
		String token = drive.stackTagCompound.getString(ItemRaidDrive.TAG_RAID_TOKEN);
		RaidDriveAuthorization authorization = raidDriveAuthorizations.get(token);
		long expiresAt = authorization != null ? authorization.expiresAt : drive.stackTagCompound.getLong(ItemRaidDrive.TAG_EXPIRES_AT);
		long now = currentServerRuntimeMillis();
		if(token.isEmpty() || authorization == null || now < 0L || expiresAt <= now) return false;
		if(station.raidPortActive) return token.equals(station.raidToken) && orbitWorld.getBlock(station.raidPortX, station.raidPortY, station.raidPortZ) == ModBlocks.orbital_station_raiding_port;

		int[] position = findRaidPortPosition(station, token, orbitWorld);
		if(position == null || !OrbitalStation.spawnRaidPort(orbitWorld, position[0], position[1])) return false;

		station.ensureIdentity();
		int coreChunkX = position[0] >> 4;
		int coreChunkZ = position[1] >> 4;
		if(!Integrations.registerBreachOutpostWGC(
			orbitWorld,
			token,
			station.stationKey,
			station.generation,
			attackerFactionId,
			SpaceConfig.orbitDimension,
			coreChunkX,
			coreChunkZ
		)) {
			clearSmallCoreArea(orbitWorld, position[0], position[1]);
			return false;
		}

		station.raidPortActive = true;
		station.raidToken = token;
		station.raidPortX = position[0];
		station.raidPortY = OrbitalStation.CORE_Y;
		station.raidPortZ = position[1];
		station.raidExpiresAt = expiresAt;
		long cleanupDelayMillis = Math.max(1L, SpaceConfig.raidPortCleanupDelaySeconds) * 1000L;
		if(authorization.wgcoreManaged && Integrations.isWGCoreActive()) {
			long managedCleanupDelay = Integrations.getOrbitalStationCrashDurationMillisWGC(getIntegrationWorld());
			if(managedCleanupDelay >= 0L) cleanupDelayMillis = Math.max(1000L, managedCleanupDelay);
		}
		station.raidCleanupAt = safeAdd(expiresAt, cleanupDelayMillis);
		station.raidLastWarningInterval = -1L;
		station.raidExpirationWarningSent = false;
		markDirty();
		return true;
	}

	public synchronized OrbitalStation getStationForRaidDrive(ItemStack drive, boolean requireActive) {
		ensureRaidRuntimeClock();
		if(!ItemRaidDrive.validate(drive) || !drive.hasTagCompound()) return null;
		ItemVOTVdrive.Destination destination = ItemVOTVdrive.getDestinationUnchecked(drive);
		if(destination == null || destination.body != SolarSystem.Body.ORBIT) return null;

		OrbitalStation station = getStationAtGrid(destination.x, destination.z);
		String token = drive.stackTagCompound.getString(ItemRaidDrive.TAG_RAID_TOKEN);
		RaidDriveAuthorization authorization = raidDriveAuthorizations.get(token);

		// Migrate a valid pre-patch Raid Hard Drive once. Legacy drives had no token,
		// station key, generation, or type tag, so migration is restricted to a
		// generation-zero legacy station and immediately binds all modern identity tags.
		if(authorization == null && token.isEmpty() && isLegacyRaidDrive(drive)
			&& matchesIdentity(station, "", 0, requireActive)) {
			long expiresAt = drive.stackTagCompound.getLong(ItemRaidDrive.TAG_EXPIRES_AT);
			long now = currentServerRuntimeMillis();
			if(now < 0L || expiresAt <= now) return null;
			token = UUID.randomUUID().toString();
			drive.stackTagCompound.setString(ItemRaidDrive.TAG_RAID_TOKEN, token);
			drive.stackTagCompound.setString(ItemVOTVdrive.TAG_STATION_KEY, station.stationKey);
			drive.stackTagCompound.setInteger(ItemVOTVdrive.TAG_STATION_GENERATION, station.generation);
			drive.stackTagCompound.setString(ItemVOTVdrive.TAG_STATION_DRIVE_TYPE, ItemVOTVdrive.DRIVE_TYPE_RAID);
			drive.stackTagCompound.setInteger("sDim", station.orbiting == null ? 0 : station.orbiting.dimensionId);
			drive.stackTagCompound.setBoolean("sHas", station.hasStation);
			authorization = new RaidDriveAuthorization(token, station.dX, station.dZ, station.stationKey,
				station.generation, expiresAt, "", false);
			raidDriveAuthorizations.put(token, authorization);
			markDirty();
		}

		if(authorization == null) return null;
		long now = currentServerRuntimeMillis();
		if(now < 0L || authorization.x != destination.x || authorization.z != destination.z) return null;
		if(authorization.wgcoreManaged && Integrations.isWGCoreActive()) {
			if(authorization.expiresAt <= now) return null;
			UUID factionId = parseUuid(authorization.ownerFactionId);
			World authorityWorld = getAuthorityWorld();
			long remaining = authorityWorld != null && factionId != null
				? Integrations.getBreachDriveAccessRemainingMillisWGC(authorityWorld, factionId,
					authorization.stationKey, authorization.generation)
				: 0L;
			if(remaining <= 0L) return null;
			authorization.expiresAt = safeAdd(now, remaining);
			drive.stackTagCompound.setLong(ItemRaidDrive.TAG_EXPIRES_AT, authorization.expiresAt);
		} else if(authorization.expiresAt <= now) {
			return null;
		}
		if(!matchesIdentity(station, authorization.stationKey, authorization.generation, requireActive)) return null;
		return matchesDriveIdentity(station, drive, requireActive) ? station : null;
	}

	private boolean isLegacyRaidDrive(ItemStack drive) {
		if(drive == null || !drive.hasTagCompound()) return false;
		NBTTagCompound tag = drive.stackTagCompound;
		return !tag.hasKey(ItemRaidDrive.TAG_RAID_TOKEN)
			&& !tag.hasKey(ItemVOTVdrive.TAG_STATION_KEY)
			&& !tag.hasKey(ItemVOTVdrive.TAG_STATION_GENERATION)
			&& !tag.hasKey(ItemVOTVdrive.TAG_STATION_DRIVE_TYPE);
	}

	private int[] findRaidPortPosition(OrbitalStation station, String token, WorldServer world) {
		int centerChunkX = station.getCenterChunkX();
		int centerChunkZ = station.getCenterChunkZ();
		int radius = 25;
		List<int[]> centers = new ArrayList<int[]>(radius * 8);

		// Walk the entire square perimeter deterministically. This preserves the
		// WIP branch's 25-chunk attack distance while avoiding the old four-cardinal
		// placement restriction.
		for(int dx = -radius; dx <= radius; dx++) centers.add(new int[] {centerChunkX + dx, centerChunkZ - radius});
		for(int dz = -radius + 1; dz <= radius; dz++) centers.add(new int[] {centerChunkX + radius, centerChunkZ + dz});
		for(int dx = radius - 1; dx >= -radius; dx--) centers.add(new int[] {centerChunkX + dx, centerChunkZ + radius});
		for(int dz = radius - 1; dz > -radius; dz--) centers.add(new int[] {centerChunkX - radius, centerChunkZ + dz});

		int start = token == null || centers.isEmpty() ? 0 : (token.hashCode() & Integer.MAX_VALUE) % centers.size();
		for(int i = 0; i < centers.size(); i++) {
			int[] candidate = centers.get((start + i) % centers.size());
			int blockX = candidate[0] * OrbitalStation.CHUNK_SIZE;
			int blockZ = candidate[1] * OrbitalStation.CHUNK_SIZE;
			int cleanupMinX = candidate[0] - OrbitalStation.RAID_CLEANUP_CHUNKS / 2;
			int cleanupMinZ = candidate[1] - OrbitalStation.RAID_CLEANUP_CHUNKS / 2;
			if(isChunkAreaEmpty(world, cleanupMinX, cleanupMinZ, OrbitalStation.RAID_CLEANUP_CHUNKS, OrbitalStation.RAID_CLEANUP_CHUNKS)
				&& !cleanupAreaOverlaps(cleanupMinX, cleanupMinZ, OrbitalStation.RAID_CLEANUP_CHUNKS, OrbitalStation.RAID_CLEANUP_CHUNKS)) {
				return new int[] {blockX, blockZ};
			}
		}
		return null;
	}

	private boolean isTwoByTwoFootprintEmpty(WorldServer world, int centerChunkX, int centerChunkZ) {
		return isChunkAreaEmpty(world, centerChunkX - 1, centerChunkZ - 1, 2, 2);
	}

	private boolean isChunkAreaEmpty(WorldServer world, int minChunkX, int minChunkZ, int width, int height) {
		for(int chunkX = minChunkX; chunkX < minChunkX + width; chunkX++) {
			for(int chunkZ = minChunkZ; chunkZ < minChunkZ + height; chunkZ++) {
				Chunk chunk = world.getChunkFromChunkCoords(chunkX, chunkZ);
				for(ExtendedBlockStorage storage : chunk.getBlockStorageArray()) if(storage != null && !storage.isEmpty()) return false;
			}
		}
		return true;
	}

	private boolean isCoreAreaEmpty(WorldServer world, int centerX, int centerZ) {
		return isTwoByTwoFootprintEmpty(world, centerX >> 4, centerZ >> 4);
	}

	private void clearSmallCoreArea(WorldServer world, int centerX, int centerZ) {
		boolean oldSafeRem = BlockDummyable.safeRem;
		BlockDummyable.safeRem = true;
		try {
			for(int x = centerX - 3; x <= centerX + 3; x++) for(int z = centerZ - 3; z <= centerZ + 3; z++) for(int y = OrbitalStation.CORE_Y - 1; y <= OrbitalStation.CORE_Y + 2; y++) world.setBlockToAir(x, y, z);
		} finally { BlockDummyable.safeRem = oldSafeRem; }
	}

	public synchronized void registerComputerPlaced(World world, int x, int y, int z) {
		OrbitalStation station = getStationFromPosition(x, z);
		if(station == null || !station.hasStation || station.deleting) return;
		if(station.hasComputer && world != null && world.getBlock(station.computerX, station.computerY, station.computerZ) == ModBlocks.orbital_station_computer) return;
		boolean repairing = station.computerRequired && !station.hasComputer && station.computerCrashTicksRemaining >= 0L;
		station.computerRequired = true;
		station.hasComputer = true;
		station.computerX = x;
		station.computerY = y;
		station.computerZ = z;
		station.computerCrashTicksRemaining = -1L;
		station.computerNextWarningTicks = -1L;
		markDirty();
		if(repairing) broadcastStationMaintenance(world, station,
			"Orbital Station Computer restored. Station crash sequence cancelled.", EnumChatFormatting.GREEN);
	}

	/** Migrates or repairs an already-placed computer on an older station. */
	public synchronized void registerComputerDiscovered(World world, int x, int y, int z) {
		OrbitalStation station = getStationFromPosition(x, z);
		if(station == null || !station.hasStation || station.deleting) return;

		boolean trackedComputerPresent = station.computerRequired && station.hasComputer && world != null
			&& world.getBlock(station.computerX, station.computerY, station.computerZ) == ModBlocks.orbital_station_computer;
		if(trackedComputerPresent) return;

		boolean repairing = station.computerRequired && !station.hasComputer && station.computerCrashTicksRemaining >= 0L;
		station.computerRequired = true;
		station.hasComputer = true;
		station.computerX = x;
		station.computerY = y;
		station.computerZ = z;
		station.computerCrashTicksRemaining = -1L;
		station.computerNextWarningTicks = -1L;
		markDirty();
		if(repairing) broadcastStationMaintenance(world, station,
			"Orbital Station Computer restored. Station crash sequence cancelled.", EnumChatFormatting.GREEN);
	}

	public synchronized void registerComputerRemoved(World world, int x, int y, int z) {
		OrbitalStation station = getStationFromPosition(x, z);
		if(station == null || !station.hasStation || station.deleting || !station.computerRequired) return;
		if(!station.hasComputer && station.computerCrashTicksRemaining >= 0L) return;

		boolean removedTrackedComputer = station.computerX == x && station.computerY == y && station.computerZ == z;
		if(!removedTrackedComputer && station.hasComputer && world != null
			&& world.getBlock(station.computerX, station.computerY, station.computerZ) == ModBlocks.orbital_station_computer) {
			return;
		}

		if(!removedTrackedComputer) {
			MainRegistry.logger.info("[StationMaintenance] Recovered stale Orbital Station Computer coordinates station="
				+ getStationId(station) + " old=" + station.computerX + "," + station.computerY + "," + station.computerZ
				+ " removed=" + x + "," + y + "," + z);
		}

		station.hasComputer = false;
		station.computerX = x;
		station.computerY = y;
		station.computerZ = z;
		long crashSeconds = resolveStationCrashDurationSeconds(world);
		station.computerCrashTicksRemaining = safeMultiplyByTwenty(crashSeconds);
		station.computerNextWarningTicks = nextComputerWarningThreshold(station.computerCrashTicksRemaining);
		markDirty();
		broadcastComputerWarning(world, station);
	}

	private long resolveStationCrashDurationSeconds(World world) {
		if(Integrations.isWGCoreActive()) {
			World integrationWorld = getIntegrationWorld();
			long managedMillis = Integrations.getOrbitalStationCrashDurationMillisWGC(
				integrationWorld != null ? integrationWorld : world);
			if(managedMillis >= 0L) return Math.max(1L, (managedMillis + 999L) / 1000L);
		}
		return Math.max(1L, SpaceConfig.stationComputerCrashTimeSeconds);
	}

	private static long safeMultiplyByTwenty(long seconds) {
		return seconds > Long.MAX_VALUE / 20L ? Long.MAX_VALUE : seconds * 20L;
	}

	private static long normalizeComputerWarningThreshold(long remainingTicks, long savedThreshold) {
		if(remainingTicks <= 0L) return -1L;
		if(savedThreshold > 0L && savedThreshold < remainingTicks) return savedThreshold;
		return nextComputerWarningThreshold(remainingTicks);
	}

	private static long nextComputerWarningThreshold(long fromTicks) {
		for(long threshold : COMPUTER_WARNING_THRESHOLDS_TICKS) {
			if(threshold > 0L && threshold < fromTicks) return threshold;
		}
		return -1L;
	}

	private void broadcastComputerWarning(World world, OrbitalStation station) {
		if(station == null) return;
		long remainingTicks = Math.max(0L, station.computerCrashTicksRemaining);
		long remainingSeconds = remainingTicks / 20L + (remainingTicks % 20L == 0L ? 0L : 1L);
		broadcastStationMaintenance(world, station,
			"Orbital Station Computer lost. Station crash in " + formatDurationSeconds(remainingSeconds)
				+ ". Restore the computer to abort.", EnumChatFormatting.RED);
	}

	private static String shortIdentity(String identity) {
		if(identity == null || identity.isEmpty()) return "none";
		return identity.length() <= 8 ? identity : identity.substring(0, 8);
	}

	private static String formatDurationSeconds(long totalSeconds) {
		long remaining = Math.max(0L, totalSeconds);
		long days = remaining / 86400L;
		remaining %= 86400L;
		long hours = remaining / 3600L;
		remaining %= 3600L;
		long minutes = remaining / 60L;
		long seconds = remaining % 60L;
		StringBuilder result = new StringBuilder();
		if(days > 0L) result.append(days).append(days == 1L ? " day" : " days");
		if(hours > 0L) {
			if(result.length() > 0) result.append(' ');
			result.append(hours).append(hours == 1L ? " hour" : " hours");
		}
		if(minutes > 0L) {
			if(result.length() > 0) result.append(' ');
			result.append(minutes).append(minutes == 1L ? " minute" : " minutes");
		}
		if(seconds > 0L || result.length() == 0) {
			if(result.length() > 0) result.append(' ');
			result.append(seconds).append(seconds == 1L ? " second" : " seconds");
		}
		return result.toString();
	}

	private void broadcastToStation(World world, OrbitalStation station, String message) {
		broadcastToStation(world, station, new ChatComponentText(message));
	}

	private void broadcastToStation(World world, OrbitalStation station, IChatComponent message) {
		if(world == null || station == null || message == null) return;
		for(Object object : new ArrayList<Object>(world.playerEntities)) {
			if(object instanceof EntityPlayerMP) {
				EntityPlayerMP player = (EntityPlayerMP)object;
				if(station.containsBlock(player.posX, player.posZ)) player.addChatMessage(message);
			}
		}
	}

	/**
	 * WGCore owns integrated-pack station-maintenance chat. Standalone HBM keeps
	 * a local styled message so wrapped lines retain their colour either way.
	 */
	private void broadcastStationMaintenance(World world, OrbitalStation station, String message, EnumChatFormatting fallbackColor) {
		if(world == null || station == null || message == null || message.isEmpty()) return;
		for(Object object : new ArrayList<Object>(world.playerEntities)) {
			if(!(object instanceof EntityPlayerMP)) continue;
			EntityPlayerMP player = (EntityPlayerMP)object;
			if(!station.containsBlock(player.posX, player.posZ)) continue;
			if(Integrations.isWGCoreActive()
				&& Integrations.notifyPlayerWGC(world, player.getUniqueID(), message)) {
				continue;
			}
			ChatComponentText component = new ChatComponentText(message);
			component.setChatStyle(new ChatStyle().setColor(fallbackColor));
			player.addChatMessage(component);
		}
	}

	/**
	 * Persist enough orbital identity on each online player to distinguish a
	 * legitimate current station/outpost from one that was deleted while the
	 * player was offline.
	 */
	private void rememberOnlinePlayerOrbitalLocations(WorldServer orbitWorld) {
		if(orbitWorld == null || orbitWorld.provider.dimensionId != SpaceConfig.orbitDimension) return;
		for(Object object : new ArrayList<Object>(orbitWorld.playerEntities)) {
			if(!(object instanceof EntityPlayerMP)) continue;
			EntityPlayerMP player = (EntityPlayerMP)object;
			OrbitalStation station = getStationFromPosition(
				MathHelper.floor_double(player.posX),
				MathHelper.floor_double(player.posZ)
			);
			NBTTagCompound persisted = getPersistedPlayerData(player);
			if(station == null || station.stationKey == null || station.stationKey.isEmpty()) {
				persisted.removeTag(PLAYER_ORBIT_CONTEXT_TAG);
				continue;
			}

			NBTTagCompound context = new NBTTagCompound();
			context.setString(PLAYER_ORBIT_STATION_KEY_TAG, station.stationKey);
			context.setInteger(PLAYER_ORBIT_STATION_GENERATION_TAG, station.generation);
			context.setString(
				PLAYER_ORBIT_RAID_TOKEN_TAG,
				station.raidPortActive && station.raidToken != null && !station.raidToken.isEmpty()
					&& isInsideRaidOutpostFootprint(station, player.posX, player.posZ)
					? station.raidToken
					: ""
			);
			persisted.setTag(PLAYER_ORBIT_CONTEXT_TAG, context);
		}
	}

	private boolean isInsideRaidOutpostFootprint(OrbitalStation station, double blockX, double blockZ) {
		if(station == null || !station.raidPortActive || station.raidToken == null || station.raidToken.isEmpty()) {
			return false;
		}
		int coreChunkX = MathHelper.floor_double((double)station.raidPortX / OrbitalStation.CHUNK_SIZE);
		int coreChunkZ = MathHelper.floor_double((double)station.raidPortZ / OrbitalStation.CHUNK_SIZE);
		int minChunkX = coreChunkX - OrbitalStation.RAID_CLEANUP_CHUNKS / 2;
		int minChunkZ = coreChunkZ - OrbitalStation.RAID_CLEANUP_CHUNKS / 2;
		int playerChunkX = MathHelper.floor_double(blockX) >> 4;
		int playerChunkZ = MathHelper.floor_double(blockZ) >> 4;
		return playerChunkX >= minChunkX
			&& playerChunkX < minChunkX + OrbitalStation.RAID_CLEANUP_CHUNKS
			&& playerChunkZ >= minChunkZ
			&& playerChunkZ < minChunkZ + OrbitalStation.RAID_CLEANUP_CHUNKS;
	}

	private List<EntityPlayerMP> getPlayersInsideStationCell(WorldServer world, OrbitalStation station) {
		List<EntityPlayerMP> players = new ArrayList<EntityPlayerMP>();
		if(world == null || station == null) return players;
		for(Object object : world.playerEntities) {
			if(!(object instanceof EntityPlayerMP)) continue;
			EntityPlayerMP player = (EntityPlayerMP)object;
			if(station.containsBlock(player.posX, player.posZ)) players.add(player);
		}
		return players;
	}

	private boolean isBreachSettlementCleanupQueued(String stationKey, int generation) {
		if(stationKey == null || stationKey.isEmpty()) return false;
		for(CleanupTask task : cleanupTasks) {
			if(task.type == CleanupType.STATION
				&& task.breachSettlement
				&& stationKey.equals(task.identity)
				&& generation == task.generation) {
				return true;
			}
		}
		return false;
	}

	/** Called exactly once per server tick by ModEventHandler. */
	public synchronized void tickMaintenance() {
		ensureRaidRuntimeClock();
		long now = currentServerRuntimeMillis();
		if(now < 0L) return;
		World world = DimensionManager.getWorld(SpaceConfig.orbitDimension);
		WorldServer orbitWorld = world instanceof WorldServer ? (WorldServer)world : null;
		if(orbitWorld != null) {
			rememberOnlinePlayerOrbitalLocations(orbitWorld);
		}
		boolean changed = false;
		for(OrbitalStation station : new ArrayList<OrbitalStation>(stations.values())) {
			if(station == null) continue;
			if(station.deleting) {
				if(recoverMissingStationCleanupTask(station)) changed = true;
				continue;
			}

			// Successful-Breach ordering is authoritative:
			// WGCore evacuation timer expires -> HBM forces any remaining occupants
			// safely back to their recorded surface launch area -> WGCore points/disband
			// settlement -> HBM queues the bounded physical 64x64 cell wipe.
			// Settlement polling deliberately uses an always-loaded integration world so
			// orbit unloading cannot strand VICTORY_SETTLEMENT_READY forever.
			World integrationWorld = getIntegrationWorld();
			if(integrationWorld != null && station.hasStation && station.stationKey != null
				&& !station.stationKey.isEmpty() && ((now / 50L) % 20L == 0L)) {
				boolean settlementComplete = Integrations.isBreachSettlementCompleteWGC(
					integrationWorld, station.stationKey, station.generation);
				boolean settlementReady = !settlementComplete
					&& Integrations.isBreachSettlementReadyWGC(
						integrationWorld, station.stationKey, station.generation);

				if(settlementComplete || settlementReady) {
					if(orbitWorld == null) {
						World loadedOrbit = getOrLoadWorld(SpaceConfig.orbitDimension);
						if(loadedOrbit instanceof WorldServer) orbitWorld = (WorldServer)loadedOrbit;
					}

					if(orbitWorld != null) {
						List<EntityPlayerMP> remainingPlayers = getPlayersInsideStationCell(orbitWorld, station);
						if(!remainingPlayers.isEmpty()) {
							MainRegistry.logger.info("[BreachSettlement] Forcing " + remainingPlayers.size()
								+ " remaining online player(s) off defeated station " + getStationId(station)
								+ " before final WGCore settlement.");
							for(EntityPlayerMP player : new ArrayList<EntityPlayerMP>(remainingPlayers)) {
								returnPlayerToSurface(
									player,
									station.orbiting,
									Integrations.isWGCoreActive() ? null
										: "The defeated orbital station is being destroyed. Returning you to the surface.");
							}
							// CelestialTeleporter executes from its server queue after this maintenance
							// pass. Give it one tick before checking the cell again; otherwise a
							// successful queued evacuation is falsely reported as a failure.
							continue;
						}
					}

					if(!settlementComplete) {
						boolean accepted = Integrations.completeBreachSettlementWGC(
							integrationWorld, station.stationKey, station.generation);
						settlementComplete = accepted || Integrations.isBreachSettlementCompleteWGC(
							integrationWorld, station.stationKey, station.generation);
						if(!settlementComplete) {
							MainRegistry.logger.warn("[BreachSettlement] WGCore settlement was ready but not accepted station="
								+ getStationId(station) + " key=" + shortIdentity(station.stationKey)
								+ " generation=" + station.generation + "; will retry.");
							continue;
						}
					}

					if(queueBreachSettlementDeletion(station)) changed = true;
					continue;
				}
			}

			if(station.computerRequired && !station.hasComputer && station.computerCrashTicksRemaining >= 0L) {
				if(station.computerCrashTicksRemaining > 0L) station.computerCrashTicksRemaining--;
				changed = true;
				if(station.computerCrashTicksRemaining <= 0L) {
					queueStationDeletion(station);
				} else if(station.computerNextWarningTicks >= 0L && station.computerCrashTicksRemaining <= station.computerNextWarningTicks) {
					broadcastComputerWarning(orbitWorld, station);
					station.computerNextWarningTicks = nextComputerWarningThreshold(station.computerNextWarningTicks);
				}
			}

			if(station.raidPortActive && Integrations.isWGCoreActive()
				&& station.stationKey != null && !station.stationKey.isEmpty()) {
				// Once WGCore has accepted a physical Breach outpost it owns the entire
				// lifecycle. Do not fall back to HBM's standalone expiry/crash warning
				// path merely because the physical-drive authorization cache has expired.
				String phase = integrationWorld != null
					? Integrations.getBreachPhaseWGC(integrationWorld, station.stationKey, station.generation)
					: "";
				RaidDriveAuthorization authorization = raidDriveAuthorizations.get(station.raidToken);
				boolean managedAuthorization = authorization != null && authorization.wgcoreManaged;

				if(managedAuthorization && integrationWorld != null) {
					UUID factionId = parseUuid(authorization.ownerFactionId);
					long remaining = factionId != null
						? Integrations.getBreachDriveAccessRemainingMillisWGC(integrationWorld, factionId,
							station.stationKey, station.generation)
						: 0L;
					if(remaining > 0L) {
						station.raidExpiresAt = safeAdd(now, remaining);
						long managedCleanupDelay = Integrations.getOrbitalStationCrashDurationMillisWGC(integrationWorld);
						if(managedCleanupDelay < 0L) managedCleanupDelay = Math.max(1L, SpaceConfig.raidPortCleanupDelaySeconds) * 1000L;
						station.raidCleanupAt = safeAdd(station.raidExpiresAt, Math.max(1000L, managedCleanupDelay));
						changed = true;
					}
				}

				if("CLEANUP_READY".equals(phase) || (integrationWorld != null && (phase == null || phase.isEmpty()))) {
					queueRaidCleanup(station);
					continue;
				}

				// PREPARATION/ACTIVE/FAILED_WITHDRAWAL/victory states all remain
				// WGCore-managed, including player-facing warnings. If WGCore's storage
				// world is temporarily unavailable, fail closed rather than starting the
				// standalone HBM raid-expiry path.
				continue;
			}

			if(station.raidPortActive && station.raidExpiresAt > 0L) {
				if(station.raidCleanupAt <= 0L) {
					station.raidCleanupAt = safeAdd(station.raidExpiresAt, Math.max(1L, SpaceConfig.raidPortCleanupDelaySeconds) * 1000L);
					changed = true;
				}
				if(station.raidCleanupAt > 0L && now >= station.raidCleanupAt) {
					queueRaidCleanup(station);
				} else if(now >= station.raidExpiresAt) {
					long currentInterval = Math.max(0L, (now - station.raidExpiresAt) / RAID_WARNING_INTERVAL_MILLIS);
					if(currentInterval > station.raidLastWarningInterval) {
						long remainingMillis = Math.max(0L, station.raidCleanupAt - now);
						long remainingSeconds = (remainingMillis + 999L) / 1000L;
						broadcastToStation(orbitWorld, station, EnumChatFormatting.RED + "Warning: Breach outpost will close in " + formatDurationSeconds(remainingSeconds) + ". Evacuate now.");
						station.raidLastWarningInterval = currentInterval;
						station.raidExpirationWarningSent = true;
						changed = true;
					}
				}
			}
		}
		cleanupRaidDriveAuthorizations(now);
		if(orbitWorld == null && !cleanupTasks.isEmpty()) {
			DimensionManager.initDimension(SpaceConfig.orbitDimension);
			World loaded = DimensionManager.getWorld(SpaceConfig.orbitDimension);
			if(loaded instanceof WorldServer) orbitWorld = (WorldServer)loaded;
		}
		if(orbitWorld != null && !cleanupTasks.isEmpty()) {
			ensureCleanupDimensionKeepalive(orbitWorld);
			processCleanupTask(orbitWorld, cleanupTasks.get(0));
			if(cleanupTasks.isEmpty()) releaseCleanupDimensionKeepalive(orbitWorld);
		} else if(orbitWorld != null) {
			releaseCleanupDimensionKeepalive(orbitWorld);
		}
		if(changed) markDirty();
	}

	/**
	 * Defensive recovery for interrupted/older saves: deleting station records must
	 * always have a persisted resumable cleanup task. Normal saves already write
	 * the task progress every cleared chunk; this only repairs a missing task.
	 */
	private boolean recoverMissingStationCleanupTask(OrbitalStation station) {
		if(station == null || !station.deleting) return false;
		if(isCleanupQueued(CleanupType.STATION, station.dX, station.dZ, station.stationKey)) return false;

		World integrationWorld = getIntegrationWorld();
		CleanupTask task = CleanupTask.station(
			station.dX, station.dZ, station.stationKey, station.generation, station.orbiting);
		task.playerDrivesDeprogrammed = true;
		task.breachSettlement = integrationWorld != null
			&& station.stationKey != null && !station.stationKey.isEmpty()
			&& Integrations.isBreachSettlementCompleteWGC(
				integrationWorld, station.stationKey, station.generation);
		cleanupTasks.add(task);
		MainRegistry.logger.warn("[StationMaintenance] Recovered missing resumable station cleanup task station="
			+ getStationId(station) + " key=" + shortIdentity(station.stationKey)
			+ " generation=" + station.generation + ".");
		return true;
	}

	private void ensureCleanupDimensionKeepalive(WorldServer orbitWorld) {
		if(orbitWorld == null) return;
		ChunkLoaderManager.forceChunk(
			orbitWorld,
			CLEANUP_KEEPALIVE_KEY_X,
			CLEANUP_KEEPALIVE_KEY_Y,
			CLEANUP_KEEPALIVE_KEY_Z,
			CLEANUP_KEEPALIVE_CHUNK);
	}

	private void releaseCleanupDimensionKeepalive(WorldServer orbitWorld) {
		if(orbitWorld == null) return;
		ChunkLoaderManager.unforceChunk(
			orbitWorld,
			CLEANUP_KEEPALIVE_KEY_X,
			CLEANUP_KEEPALIVE_KEY_Y,
			CLEANUP_KEEPALIVE_KEY_Z,
			CLEANUP_KEEPALIVE_CHUNK);
	}

	private void queueRaidCleanup(OrbitalStation station) {
		if(station == null || !station.raidPortActive || isCleanupQueued(CleanupType.RAID, station.dX, station.dZ, station.raidToken)) return;
		CleanupTask task = CleanupTask.raid(station);
		hydrateRaidCleanupFactionRoles(getIntegrationWorld(), task, station);
		if(!task.containsCoreChunk()) {
			MainRegistry.logger.error("[StationMaintenance] Refusing invalid raid cleanup task for station " + getStationId(station)
				+ ": core chunk " + task.coreChunkX + "," + task.coreChunkZ + " is outside " + task.describeBounds());
			return;
		}
		cleanupTasks.add(task);
		MainRegistry.logger.info("[StationMaintenance] Queued raid cleanup station=" + getStationId(station)
			+ " token=" + shortIdentity(task.identity) + " core=" + task.coreX + "," + task.coreY + "," + task.coreZ
			+ " chunks=" + task.describeBounds());
		markDirty();
	}

	private void processCleanupTask(WorldServer world, CleanupTask task) {
		OrbitalStation cleanupStation = getStationAtGrid(task.stationX, task.stationZ);
		if(task.type == CleanupType.RAID && hydrateRaidCleanupFactionRoles(world, task, cleanupStation)) {
			markDirty();
		}

		// Orbital territory is virtual in WGCore. Remove its binding as soon as
		// physical cleanup begins so the cleared area resolves back to Deep Space.
		// Raid attacker/defender identities are captured above first because removing
		// the outpost intentionally retires WGCore's retained CLEANUP_READY session.
		if(task.type == CleanupType.RAID) {
			Integrations.unregisterBreachOutpostWGC(world, task.identity);
		} else if(task.type == CleanupType.STATION && !task.breachSettlement) {
			Integrations.unregisterOrbitalStationWGC(world, task.identity, task.generation);
		}

		// Compatibility for station cleanup tasks saved before immediate priority
		// deprogramming was added. Do this before relocating players or clearing the first chunk.
		if(task.type == CleanupType.STATION && !task.playerDrivesDeprogrammed) {
			resetPlayerNormalStationDrives(task.stationX, task.stationZ, task.identity, task.generation);
			resetLaunchPadNormalStationDrives(task.stationX, task.stationZ, task.identity, task.generation);
			task.playerDrivesDeprogrammed = true;
			markDirty();
		}

		if(task.type == CleanupType.STATION && task.breachSettlement) {
			List<EntityPlayerMP> remainingPlayers = getPlayersInsideTaskEvacuationArea(world, task);
			if(!remainingPlayers.isEmpty()) {
				MainRegistry.logger.info("[BreachSettlement] Safety-evacuating " + remainingPlayers.size()
					+ " player(s) before defeated station cleanup continues station="
					+ getStationId(task.stationX, task.stationZ) + ".");
				relocatePlayers(world, task);
				if(!getPlayersInsideTaskEvacuationArea(world, task).isEmpty()) return;
			}
		} else {
			relocatePlayers(world, task);
		}
		if(task.type == CleanupType.RAID && !task.coreCleared) {
			clearRaidCore(world, task);
			task.coreCleared = true;
			markDirty();
		}
		int totalChunks = task.getTotalChunks();
		if(task.progress < totalChunks) {
			int localX = task.progress % task.width;
			int localZ = task.progress / task.width;
			clearChunk(world, task.minChunkX + localX, task.minChunkZ + localZ);
			task.progress++;
			if(task.type == CleanupType.RAID && (task.progress == 1 || task.progress == totalChunks || task.progress % 25 == 0)) {
				MainRegistry.logger.info("[StationMaintenance] Raid cleanup progress station=" + getStationId(task.stationX, task.stationZ)
					+ " token=" + shortIdentity(task.identity) + " chunks=" + task.progress + "/" + totalChunks);
			}
			markDirty();
			return;
		}
		if(finishCleanup(world, task)) {
			cleanupTasks.remove(task);
			markDirty();
		}
	}

	@SuppressWarnings("unchecked")
	private List<EntityPlayerMP> getPlayersInsideCleanupArea(WorldServer world, CleanupTask task) {
		List<EntityPlayerMP> players = new ArrayList<EntityPlayerMP>();
		if(world == null || task == null) return players;
		double minX = task.minChunkX * 16D;
		double minZ = task.minChunkZ * 16D;
		double maxX = (task.minChunkX + task.width) * 16D;
		double maxZ = (task.minChunkZ + task.height) * 16D;
		for(Object object : world.playerEntities) {
			if(!(object instanceof EntityPlayerMP)) continue;
			EntityPlayerMP player = (EntityPlayerMP)object;
			if(player.posX >= minX && player.posX < maxX && player.posZ >= minZ && player.posZ < maxZ) {
				players.add(player);
			}
		}
		return players;
	}

	/**
	 * A failed Breach removes only the temporary outpost. Never use the parent
	 * station's whole 64x64 cell as the evacuation boundary for a RAID task.
	 */
	private List<EntityPlayerMP> getPlayersInsideTaskEvacuationArea(WorldServer world, CleanupTask task) {
		if(task == null || task.type != CleanupType.RAID) return getPlayersInsideCleanupArea(world, task);
		OrbitalStation station = getStationAtGrid(task.stationX, task.stationZ);
		if(station == null || !station.raidPortActive || station.raidToken == null
			|| !station.raidToken.equals(task.identity)) {
			return getPlayersInsideCleanupArea(world, task);
		}

		List<EntityPlayerMP> players = new ArrayList<EntityPlayerMP>();
		for(Object object : world.playerEntities) {
			if(!(object instanceof EntityPlayerMP)) continue;
			EntityPlayerMP player = (EntityPlayerMP)object;
			boolean insideOutpost = isInsideRaidOutpostFootprint(station, player.posX, player.posZ);
			boolean attackingFactionInsideTargetStation = isRaidAttacker(player, world, task)
				&& isInsidePermanentStationTerritory(world, station, player.posX, player.posZ);
			if(insideOutpost || attackingFactionInsideTargetStation) players.add(player);
		}
		return players;
	}

	private void relocatePlayers(WorldServer world, CleanupTask task) {
		OrbitalStation station = getStationAtGrid(task.stationX, task.stationZ);
		CelestialBody body = CelestialBody.getBody(task.bodyName);
		if(body == null) body = station != null ? station.orbiting : CelestialBody.getBody(0);
		String message;
		if(Integrations.isWGCoreActive() && (task.type == CleanupType.RAID || task.breachSettlement)) {
			// WGCore already emitted the WAR lifecycle notice; do not duplicate it
			// with a second unprefixed HBM line.
			message = null;
		} else if(task.type == CleanupType.RAID) {
			message = "The temporary Breach outpost is closing. Returning you to the surface.";
		} else {
			message = "Orbital station collapse is underway. Returning you to the surface.";
		}
		for(EntityPlayerMP player : new ArrayList<EntityPlayerMP>(getPlayersInsideTaskEvacuationArea(world, task))) {
			boolean insideOutpost = task.type == CleanupType.RAID && station != null
				&& isInsideRaidOutpostFootprint(station, player.posX, player.posZ);
			if(task.type == CleanupType.RAID && station != null && insideOutpost
					&& isRaidDefender(player, world, task, station)
					&& relocatePlayerIntoPermanentStation(world, player, station)) {
				Integrations.notifyPlayerWGC(world, player.getUniqueID(),
					"Enemy Breach outpost collapsed. You were moved back inside your station.");
				continue;
			}
			returnPlayerToSurface(player, body, message);
		}
	}

	private boolean hydrateRaidCleanupFactionRoles(World world, CleanupTask task, OrbitalStation station) {
		if(task == null || task.type != CleanupType.RAID || station == null) return false;
		boolean changed = false;
		World authorityWorld = world != null ? world : getIntegrationWorld();

		if((task.attackerFactionId == null || task.attackerFactionId.isEmpty())
				&& Integrations.isWGCoreActive() && authorityWorld != null
				&& station.stationKey != null && !station.stationKey.isEmpty()) {
			UUID attacker = Integrations.getBreachAttackerFactionWGC(
				authorityWorld, station.stationKey, station.generation);
			if(attacker != null) {
				task.attackerFactionId = attacker.toString();
				changed = true;
			}
		}

		if(task.attackerFactionId == null || task.attackerFactionId.isEmpty()) {
			RaidDriveAuthorization authorization = raidDriveAuthorizations.get(task.identity);
			if(authorization != null && authorization.ownerFactionId != null
					&& !authorization.ownerFactionId.isEmpty()) {
				task.attackerFactionId = authorization.ownerFactionId;
				changed = true;
			}
		}

		if((task.defenderFactionId == null || task.defenderFactionId.isEmpty())
				&& Integrations.isWGCoreActive() && authorityWorld != null
				&& station.stationKey != null && !station.stationKey.isEmpty()) {
			UUID defender = Integrations.getBreachDefenderFactionWGC(
				authorityWorld, station.stationKey, station.generation);
			if(defender == null) {
				defender = Integrations.getOrbitalStationOwnerWGC(
					authorityWorld, station.stationKey, station.generation);
			}
			if(defender != null) {
				task.defenderFactionId = defender.toString();
				changed = true;
			}
		}

		if((task.defenderFactionId == null || task.defenderFactionId.isEmpty())
				&& station.driveOwnerIsFaction && station.driveOwnerId != null
				&& !station.driveOwnerId.isEmpty()) {
			task.defenderFactionId = station.driveOwnerId;
			changed = true;
		}
		return changed;
	}

	private boolean isRaidAttacker(EntityPlayerMP player, World world, CleanupTask task) {
		return isPlayerInFaction(player, world, task != null ? task.attackerFactionId : null);
	}

	private boolean isRaidDefender(EntityPlayerMP player, World world, CleanupTask task, OrbitalStation station) {
		if(isPlayerInFaction(player, world, task != null ? task.defenderFactionId : null)) return true;
		if(player == null || world == null || station == null || !Integrations.isWGCoreActive()) return false;
		if(station.stationKey == null || station.stationKey.isEmpty()) return false;
		UUID playerFaction = Integrations.getPlayerFaction(world, player.getUniqueID());
		UUID stationOwner = Integrations.getOrbitalStationOwnerWGC(world, station.stationKey, station.generation);
		if(playerFaction != null && stationOwner != null && playerFaction.equals(stationOwner)) return true;
		UUID hbmOwner = station.driveOwnerIsFaction ? parseUuid(station.driveOwnerId) : null;
		return playerFaction != null && hbmOwner != null && playerFaction.equals(hbmOwner);
	}

	private boolean isPlayerInFaction(EntityPlayerMP player, World world, String expectedFactionId) {
		if(player == null || world == null || expectedFactionId == null || expectedFactionId.isEmpty()
				|| !Integrations.isWGCoreActive()) return false;
		UUID expected = parseUuid(expectedFactionId);
		UUID actual = Integrations.getPlayerFaction(world, player.getUniqueID());
		return expected != null && actual != null && expected.equals(actual);
	}

	private boolean isInsidePermanentStationTerritory(World world, OrbitalStation station, double x, double z) {
		if(world == null || station == null || !Integrations.isWGCoreActive()
				|| station.stationKey == null || station.stationKey.isEmpty()) return false;
		return Integrations.isInsideOrbitalStationTerritoryWGC(
			world, station.stationKey, station.generation,
			MathHelper.floor_double(x), MathHelper.floor_double(z));
	}

	/**
	 * Failed Breach cleanup destroys only the temporary outpost. Defenders caught
	 * inside that outpost are returned to their surviving station, while members
	 * of the attacking faction still inside the protected target-station territory
	 * are removed to the surface when the failed assault closes.
	 * Prefer the defender's current Y level and fall back to the station deck only
	 * when no safe two-block standing space exists at that height.
	 */
	private boolean relocatePlayerIntoPermanentStation(WorldServer world, EntityPlayerMP player, OrbitalStation station) {
		if(world == null || player == null || station == null) return false;
		int centerX = station.getCenterBlockX();
		int centerZ = station.getCenterBlockZ();
		int preferredY = MathHelper.floor_double(player.posY);

		int[] destination = findSafeStationPosition(world, centerX, preferredY, centerZ);
		if(destination == null) {
			destination = findSafeStationPosition(world, centerX, OrbitalStation.CORE_Y + 1, centerZ);
		}
		if(destination == null) return false;

		if(player.ridingEntity != null) player.mountEntity(null);
		player.motionX = 0.0D;
		player.motionY = 0.0D;
		player.motionZ = 0.0D;
		player.fallDistance = 0.0F;
		player.setPositionAndUpdate(destination[0] + 0.5D, destination[1], destination[2] + 0.5D);
		return true;
	}

	private int[] findSafeStationPosition(WorldServer world, int centerX, int y, int centerZ) {
		if(world == null) return null;
		// Stay well inside WGCore's 22x22 permanent-station footprint. A compact
		// spiral around the physical station centre avoids loading distant empty space.
		final int maxRadius = 64;
		for(int radius = 0; radius <= maxRadius; radius += 2) {
			for(int dx = -radius; dx <= radius; dx += 2) {
				int[] north = safeStandingPosition(world, centerX + dx, y, centerZ - radius);
				if(north != null) return north;
				if(radius > 0) {
					int[] south = safeStandingPosition(world, centerX + dx, y, centerZ + radius);
					if(south != null) return south;
				}
			}
			for(int dz = -radius + 2; dz <= radius - 2; dz += 2) {
				int[] west = safeStandingPosition(world, centerX - radius, y, centerZ + dz);
				if(west != null) return west;
				if(radius > 0) {
					int[] east = safeStandingPosition(world, centerX + radius, y, centerZ + dz);
					if(east != null) return east;
				}
			}
		}
		return null;
	}

	private int[] safeStandingPosition(WorldServer world, int x, int y, int z) {
		if(y <= 1 || y >= world.getActualHeight() - 2) return null;
		if(!world.isAirBlock(x, y, z) || !world.isAirBlock(x, y + 1, z)) return null;
		if(world.isAirBlock(x, y - 1, z) || !world.getBlock(x, y - 1, z).getMaterial().isSolid()) return null;
		return new int[] { x, y, z };
	}


	private void clearRaidCore(WorldServer world, CleanupTask task) {
		if(world == null || task == null) return;
		boolean oldSafeRem = BlockDummyable.safeRem;
		BlockDummyable.safeRem = true;
		try {
			world.removeTileEntity(task.coreX, task.coreY, task.coreZ);
			world.setBlockToAir(task.coreX, task.coreY, task.coreZ);
		} finally {
			BlockDummyable.safeRem = oldSafeRem;
		}
		MainRegistry.logger.info("[StationMaintenance] Raid core removal station=" + getStationId(task.stationX, task.stationZ)
			+ " token=" + shortIdentity(task.identity) + " core=" + task.coreX + "," + task.coreY + "," + task.coreZ);
	}

	@SuppressWarnings("unchecked")
	private void clearChunk(WorldServer world, int chunkX, int chunkZ) {
		Chunk chunk = world.getChunkFromChunkCoords(chunkX, chunkZ);
		AxisAlignedBB bounds = AxisAlignedBB.getBoundingBox(chunkX * 16D, -30000000D, chunkZ * 16D, chunkX * 16D + 16D, 30000000D, chunkZ * 16D + 16D);
		for(Entity entity : (List<Entity>)world.getEntitiesWithinAABB(Entity.class, bounds)) if(!(entity instanceof EntityPlayerMP)) entity.setDead();

		for(Object object : new ArrayList<Object>(world.loadedTileEntityList)) {
			if(!(object instanceof TileEntity)) continue;
			TileEntity tile = (TileEntity)object;
			if((tile.xCoord >> 4) != chunkX || (tile.zCoord >> 4) != chunkZ) continue;
			tile.invalidate();
			world.removeTileEntity(tile.xCoord, tile.yCoord, tile.zCoord);
		}

		ExtendedBlockStorage[] storage = chunk.getBlockStorageArray();
		for(int section = 0; section < storage.length; section++) storage[section] = null;
		chunk.generateSkylightMap();
		chunk.setChunkModified();
	}

	private boolean finishCleanup(WorldServer world, CleanupTask task) {
		OrbitalStation station = getStationAtGrid(task.stationX, task.stationZ);
		if(task.type == CleanupType.RAID) {
			if(station == null) {
				MainRegistry.logger.warn("[StationMaintenance] Raid cleanup finished physically but station record is missing station="
					+ getStationId(task.stationX, task.stationZ) + " token=" + shortIdentity(task.identity));
			} else if(!task.identity.isEmpty() && !task.identity.equals(station.raidToken)) {
				MainRegistry.logger.warn("[StationMaintenance] Raid cleanup finished physically but token changed station="
					+ getStationId(task.stationX, task.stationZ) + " taskToken=" + shortIdentity(task.identity)
					+ " activeToken=" + shortIdentity(station.raidToken));
			} else {
				station.raidPortActive = false;
				station.raidToken = "";
				station.raidPortX = station.raidPortZ = 0;
				station.raidPortY = OrbitalStation.CORE_Y;
				station.raidExpiresAt = 0L;
				station.raidCleanupAt = 0L;
				station.raidLastWarningInterval = -1L;
				station.raidExpirationWarningSent = false;
			}
			MainRegistry.logger.info("[StationMaintenance] Raid cleanup complete station=" + getStationId(task.stationX, task.stationZ)
				+ " token=" + shortIdentity(task.identity) + " chunks=" + task.getTotalChunks());
			return true;
		}

		// Successful-Breach cleanup may retire its local HBM station record only if
		// WGCore's durable receipt proves the points/disband transaction completed
		// before this physical wipe was queued.
		if(task.breachSettlement) {
			if(task.identity == null || task.identity.isEmpty()) {
				MainRegistry.logger.error("[BreachSettlement] Refusing completed cleanup without station identity at "
					+ getStationId(task.stationX, task.stationZ) + ".");
				return false;
			}
			if(!Integrations.isBreachSettlementCompleteWGC(world, task.identity, task.generation)) {
				MainRegistry.logger.warn("[BreachSettlement] Physical station-cell cleanup reached completion without a WGCore COMPLETED settlement receipt station="
					+ getStationId(task.stationX, task.stationZ) + " key=" + shortIdentity(task.identity)
					+ " generation=" + task.generation + "; retaining cleanup task for recovery.");
				return false;
			}
			MainRegistry.logger.info("[BreachSettlement] Physical defeated-station cleanup complete after confirmed WGCore settlement station="
				+ getStationId(task.stationX, task.stationZ) + " key=" + shortIdentity(task.identity)
				+ " generation=" + task.generation + ".");
		}

		ChunkCoordIntPair pos = new ChunkCoordIntPair(task.stationX, task.stationZ);
		if(station == null || task.identity.isEmpty() || (task.identity.equals(station.stationKey) && task.generation == station.generation)) {
			int next = Math.max(getNextGeneration(pos), task.generation + 1);
			stationGenerations.put(pos, next);
			stations.remove(pos);
			invalidateRaidDriveAuthorizationsForStation(task.stationX, task.stationZ);
			resetLoadedNormalStationDrives(task.stationX, task.stationZ, task.identity, task.generation);
		}
		return true;
	}

	private boolean isAnyCleanupQueuedAt(int stationX, int stationZ) {
		for(CleanupTask task : cleanupTasks) if(task.stationX == stationX && task.stationZ == stationZ) return true;
		return false;
	}

	private boolean isCleanupQueued(CleanupType type, int stationX, int stationZ, String identity) {
		for(CleanupTask task : cleanupTasks) {
			if(task.type != type || task.stationX != stationX || task.stationZ != stationZ) continue;
			if(identity == null || identity.isEmpty() || identity.equals(task.identity)) return true;
		}
		return false;
	}

	private boolean cleanupAreaOverlaps(int minX, int minZ, int width, int height) {
		int maxX = minX + width;
		int maxZ = minZ + height;
		for(CleanupTask task : cleanupTasks) if(minX < task.minChunkX + task.width && maxX > task.minChunkX && minZ < task.minChunkZ + task.height && maxZ > task.minChunkZ) return true;
		return false;
	}

	private static class RaidDriveAuthorization {
		private final String token;
		private final int x;
		private final int z;
		private final String stationKey;
		private final int generation;
		private final String ownerFactionId;
		private final boolean wgcoreManaged;
		private long expiresAt;
		private RaidDriveAuthorization(String token, int x, int z, String stationKey, int generation,
		                               long expiresAt, String ownerFactionId, boolean wgcoreManaged) {
			this.token = token == null ? "" : token;
			this.x = x;
			this.z = z;
			this.stationKey = stationKey == null ? "" : stationKey;
			this.generation = generation;
			this.expiresAt = expiresAt;
			this.ownerFactionId = ownerFactionId == null ? "" : ownerFactionId;
			this.wgcoreManaged = wgcoreManaged;
		}
	}

	private enum CleanupType { STATION, RAID }

	private static class CleanupTask {
		private CleanupType type;
		private int stationX;
		private int stationZ;
		private String identity;
		private int generation;
		private String bodyName;
		private int minChunkX;
		private int minChunkZ;
		private int width;
		private int height;
		private int progress;
		private int coreX;
		private int coreY;
		private int coreZ;
		private int coreChunkX;
		private int coreChunkZ;
		private boolean coreCleared;
		private boolean playerDrivesDeprogrammed;
		private boolean breachSettlement;
		private String attackerFactionId = "";
		private String defenderFactionId = "";

		private static CleanupTask station(int stationX, int stationZ, String key, int generation, CelestialBody body) {
			CleanupTask task = new CleanupTask();
			task.type = CleanupType.STATION;
			task.stationX = stationX; task.stationZ = stationZ; task.identity = key == null ? "" : key; task.generation = generation;
			task.bodyName = body == null ? "" : body.name;
			task.minChunkX = stationX * OrbitalStation.STATION_CHUNKS;
			task.minChunkZ = stationZ * OrbitalStation.STATION_CHUNKS;
			task.width = OrbitalStation.STATION_CHUNKS; task.height = OrbitalStation.STATION_CHUNKS;
			return task;
		}

		private static CleanupTask raid(OrbitalStation station) {
			CleanupTask task = new CleanupTask();
			task.type = CleanupType.RAID;
			task.stationX = station.dX; task.stationZ = station.dZ; task.identity = station.raidToken == null ? "" : station.raidToken; task.generation = station.generation;
			task.bodyName = station.orbiting == null ? "" : station.orbiting.name;
			task.coreX = station.raidPortX; task.coreY = station.raidPortY; task.coreZ = station.raidPortZ;
			task.coreChunkX = MathHelper.floor_double((double)task.coreX / 16D);
			task.coreChunkZ = MathHelper.floor_double((double)task.coreZ / 16D);
			task.minChunkX = task.coreChunkX - OrbitalStation.RAID_CLEANUP_CHUNKS / 2;
			task.minChunkZ = task.coreChunkZ - OrbitalStation.RAID_CLEANUP_CHUNKS / 2;
			task.width = OrbitalStation.RAID_CLEANUP_CHUNKS;
			task.height = OrbitalStation.RAID_CLEANUP_CHUNKS;
			return task;
		}

		private NBTTagCompound write() {
			NBTTagCompound tag = new NBTTagCompound();
			tag.setInteger("type", type.ordinal()); tag.setInteger("stationX", stationX); tag.setInteger("stationZ", stationZ);
			tag.setString("identity", identity == null ? "" : identity); tag.setInteger("generation", generation); tag.setString("body", bodyName == null ? "" : bodyName);
			tag.setInteger("minChunkX", minChunkX); tag.setInteger("minChunkZ", minChunkZ); tag.setInteger("width", width); tag.setInteger("height", height); tag.setInteger("progress", progress);
			tag.setInteger("coreX", coreX); tag.setInteger("coreY", coreY); tag.setInteger("coreZ", coreZ);
			tag.setInteger("coreChunkX", coreChunkX); tag.setInteger("coreChunkZ", coreChunkZ); tag.setBoolean("coreCleared", coreCleared);
			tag.setBoolean("playerDrivesDeprogrammed", playerDrivesDeprogrammed);
			tag.setBoolean("breachSettlement", breachSettlement);
			tag.setString("attackerFactionId", attackerFactionId == null ? "" : attackerFactionId);
			tag.setString("defenderFactionId", defenderFactionId == null ? "" : defenderFactionId);
			return tag;
		}

		private static CleanupTask read(NBTTagCompound tag) {
			int ordinal = tag.getInteger("type");
			if(ordinal < 0 || ordinal >= CleanupType.values().length) {
				MainRegistry.logger.warn("[StationMaintenance] Ignoring cleanup task with invalid type " + ordinal);
				return null;
			}
			CleanupTask task = new CleanupTask();
			task.type = CleanupType.values()[ordinal];
			task.stationX = tag.getInteger("stationX");
			task.stationZ = tag.getInteger("stationZ");
			task.identity = tag.getString("identity");
			task.generation = Math.max(0, tag.getInteger("generation"));
			task.bodyName = tag.getString("body");
			task.playerDrivesDeprogrammed = tag.hasKey("playerDrivesDeprogrammed") && tag.getBoolean("playerDrivesDeprogrammed");
			task.breachSettlement = task.type == CleanupType.STATION
				&& tag.hasKey("breachSettlement") && tag.getBoolean("breachSettlement");
			task.attackerFactionId = tag.hasKey("attackerFactionId") ? tag.getString("attackerFactionId") : "";
			task.defenderFactionId = tag.hasKey("defenderFactionId") ? tag.getString("defenderFactionId") : "";
			if(task.type == CleanupType.STATION) {
				int expectedMinX = task.stationX * OrbitalStation.STATION_CHUNKS;
				int expectedMinZ = task.stationZ * OrbitalStation.STATION_CHUNKS;
				int savedMinX = tag.hasKey("minChunkX") ? tag.getInteger("minChunkX") : expectedMinX;
				int savedMinZ = tag.hasKey("minChunkZ") ? tag.getInteger("minChunkZ") : expectedMinZ;
				int savedWidth = tag.hasKey("width") ? tag.getInteger("width") : OrbitalStation.STATION_CHUNKS;
				int savedHeight = tag.hasKey("height") ? tag.getInteger("height") : OrbitalStation.STATION_CHUNKS;
				if(savedMinX != expectedMinX || savedMinZ != expectedMinZ
					|| savedWidth != OrbitalStation.STATION_CHUNKS || savedHeight != OrbitalStation.STATION_CHUNKS) {
					MainRegistry.logger.warn("[StationMaintenance] Repairing invalid station cleanup bounds for " + getStationId(task.stationX, task.stationZ));
				}
				task.minChunkX = expectedMinX;
				task.minChunkZ = expectedMinZ;
				task.width = OrbitalStation.STATION_CHUNKS;
				task.height = OrbitalStation.STATION_CHUNKS;
			} else {
				task.minChunkX = tag.getInteger("minChunkX");
				task.minChunkZ = tag.getInteger("minChunkZ");
				int savedWidth = tag.hasKey("width") ? tag.getInteger("width") : OrbitalStation.RAID_CLEANUP_CHUNKS;
				int savedHeight = tag.hasKey("height") ? tag.getInteger("height") : OrbitalStation.RAID_CLEANUP_CHUNKS;
				// Allow an already-persisted WIP 10x10 cleanup to finish at its original
				// bounds. New raid outposts always use the final 8x8 footprint.
				int cleanupChunks = savedWidth == 10 && savedHeight == 10 ? 10 : OrbitalStation.RAID_CLEANUP_CHUNKS;
				task.width = cleanupChunks;
				task.height = cleanupChunks;
				task.coreX = tag.hasKey("coreX") ? tag.getInteger("coreX")
					: (task.minChunkX + cleanupChunks / 2) * OrbitalStation.CHUNK_SIZE;
				task.coreY = tag.hasKey("coreY") ? tag.getInteger("coreY") : OrbitalStation.CORE_Y;
				task.coreZ = tag.hasKey("coreZ") ? tag.getInteger("coreZ")
					: (task.minChunkZ + cleanupChunks / 2) * OrbitalStation.CHUNK_SIZE;
				task.coreChunkX = MathHelper.floor_double((double)task.coreX / 16D);
				task.coreChunkZ = MathHelper.floor_double((double)task.coreZ / 16D);
				task.coreCleared = tag.hasKey("coreCleared") && tag.getBoolean("coreCleared");
				int stationMinX = task.stationX * OrbitalStation.STATION_CHUNKS;
				int stationMinZ = task.stationZ * OrbitalStation.STATION_CHUNKS;
				if(task.coreChunkX < stationMinX || task.coreChunkZ < stationMinZ
					|| task.coreChunkX >= stationMinX + OrbitalStation.STATION_CHUNKS
					|| task.coreChunkZ >= stationMinZ + OrbitalStation.STATION_CHUNKS) {
					MainRegistry.logger.warn("[StationMaintenance] Ignoring raid cleanup with out-of-station core station=" + getStationId(task.stationX, task.stationZ)
						+ " token=" + shortIdentity(task.identity) + " coreChunk=" + task.coreChunkX + "," + task.coreChunkZ);
					return null;
				}
				int expectedMinX = task.coreChunkX - cleanupChunks / 2;
				int expectedMinZ = task.coreChunkZ - cleanupChunks / 2;
				if(task.minChunkX != expectedMinX || task.minChunkZ != expectedMinZ
					|| savedWidth != cleanupChunks || savedHeight != cleanupChunks) {
					MainRegistry.logger.warn("[StationMaintenance] Repairing raid cleanup bounds station=" + getStationId(task.stationX, task.stationZ)
						+ " token=" + shortIdentity(task.identity) + " coreChunk=" + task.coreChunkX + "," + task.coreChunkZ);
				}
				task.minChunkX = expectedMinX;
				task.minChunkZ = expectedMinZ;
				task.width = cleanupChunks;
				task.height = cleanupChunks;
				if(task.minChunkX < stationMinX || task.minChunkZ < stationMinZ
					|| task.minChunkX + task.width > stationMinX + OrbitalStation.STATION_CHUNKS
					|| task.minChunkZ + task.height > stationMinZ + OrbitalStation.STATION_CHUNKS
					|| !task.containsCoreChunk()) {
					MainRegistry.logger.warn("[StationMaintenance] Ignoring unsafe raid cleanup bounds station=" + getStationId(task.stationX, task.stationZ)
						+ " token=" + shortIdentity(task.identity) + " chunks=" + task.describeBounds());
					return null;
				}
			}
			long total = (long)task.width * (long)task.height;
			task.progress = (int)Math.max(0L, Math.min((long)tag.getInteger("progress"), total));
			return task;
		}

		private int getTotalChunks() {
			return width * height;
		}

		private boolean containsCoreChunk() {
			return coreChunkX >= minChunkX && coreChunkX < minChunkX + width
				&& coreChunkZ >= minChunkZ && coreChunkZ < minChunkZ + height;
		}

		private String describeBounds() {
			return "[" + minChunkX + "," + minChunkZ + " -> " + (minChunkX + width - 1) + "," + (minChunkZ + height - 1) + "]";
		}
	}

	// Client sync
	public static HashMap<String, HashMap<Class<? extends CelestialBodyTrait>, CelestialBodyTrait>> clientTraits = new HashMap<String, HashMap<Class<? extends CelestialBodyTrait>, CelestialBodyTrait>>();
	public static void updateClientTraits(HashMap<String, HashMap<Class<? extends CelestialBodyTrait>, CelestialBodyTrait>> traits) { clientTraits = traits; if(clientTraits == null) clientTraits = new HashMap<String, HashMap<Class<? extends CelestialBodyTrait>, CelestialBodyTrait>>(); }
	public static HashMap<Class<? extends CelestialBodyTrait>, CelestialBodyTrait> getClientTraits(String bodyName) { return clientTraits.get(bodyName); }
}
