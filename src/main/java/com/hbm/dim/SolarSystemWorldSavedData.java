package com.hbm.dim;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Random;
import java.util.UUID;

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
	private static final long COMPUTER_WARNING_INTERVAL_TICKS = 10L * 60L * 20L;
	private static final long RAID_WARNING_INTERVAL_MILLIS = 5L * 60L * 1000L;

	private Random rand = new Random();
	private HashMap<String, HashMap<Class<? extends CelestialBodyTrait>, CelestialBodyTrait>> traitMap = new HashMap<String, HashMap<Class<? extends CelestialBodyTrait>, CelestialBodyTrait>>();
	private HashMap<ChunkCoordIntPair, OrbitalStation> stations = new HashMap<ChunkCoordIntPair, OrbitalStation>();
	private HashMap<String, RaidDriveAuthorization> raidDriveAuthorizations = new HashMap<String, RaidDriveAuthorization>();
	private HashMap<ChunkCoordIntPair, Integer> stationGenerations = new HashMap<ChunkCoordIntPair, Integer>();
	private List<CleanupTask> cleanupTasks = new ArrayList<CleanupTask>();

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

	@Override
	public synchronized void readFromNBT(NBTTagCompound nbt) {
		traitMap.clear();
		stations.clear();
		raidDriveAuthorizations.clear();
		stationGenerations.clear();
		cleanupTasks.clear();

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
			raidDriveAuthorizations.put(token, new RaidDriveAuthorization(token, tag.getInteger("x"), tag.getInteger("z"), tag.getString("stationKey"), Math.max(0, tag.getInteger("stationGeneration")), expiresAt));
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

		cleanupRaidDriveAuthorizations(System.currentTimeMillis());
		NBTTagList authorizationList = new NBTTagList();
		for(RaidDriveAuthorization authorization : raidDriveAuthorizations.values()) {
			NBTTagCompound tag = new NBTTagCompound();
			tag.setString("token", authorization.token);
			tag.setInteger("x", authorization.x);
			tag.setInteger("z", authorization.z);
			tag.setString("stationKey", authorization.stationKey);
			tag.setInteger("stationGeneration", authorization.generation);
			tag.setLong("expiresAt", authorization.expiresAt);
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

	/** A station owns its player-facing name until its authoritative cleanup is complete. */
	public synchronized boolean isStationNameInUse(String name) {
		String normalized = normalizeStationName(name);
		if(normalized.isEmpty()) return false;
		for(OrbitalStation station : stations.values()) {
			if(station == null || station.name == null || !station.name.trim().equalsIgnoreCase(normalized)) continue;
			if(station.hasStation || station.reservedForLaunch || station.deleting) return true;
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

	/** Finds a truly empty 64x64 station cell; reserved cells are never reused. */
	public synchronized ChunkCoordIntPair findSafeFreeSpace() {
		int size = Math.max(1, SpaceConfig.maxStationDistance / OrbitalStation.STATION_SIZE);
		for(int i = 0; i < 4096; i++) {
			ChunkCoordIntPair pos = new ChunkCoordIntPair(rand.nextInt(size * 2) - size, rand.nextInt(size * 2) - size);
			if(stations.containsKey(pos) || isAnyCleanupQueuedAt(pos.chunkXPos, pos.chunkZPos)) continue;
			return pos;
		}
		return null;
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
			cleanupTasks.add(task);
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

		station.ensureIdentity();
		long now = System.currentTimeMillis();
		long duration = Math.max(1L, SpaceConfig.stationCodeLifetimeSeconds) * 1000L;
		long expiresAt = safeAdd(now, duration);

		for(int attempt = 0; attempt < 16; attempt++) {
			ItemStack programmed = ItemRaidDrive.createProgrammed(station, expiresAt, 1);
			if(programmed == null || !programmed.hasTagCompound()) return null;
			String token = programmed.stackTagCompound.getString(ItemRaidDrive.TAG_RAID_TOKEN);
			if(token.isEmpty() || raidDriveAuthorizations.containsKey(token)) continue;

			ItemStack rechecked = player.inventory.getStackInSlot(player.inventory.currentItem);
			if(rechecked != held || rechecked.getItem() != ModItems.raid_drive || !ItemRaidDrive.isUnprogrammed(rechecked)) return null;
			if(!matchesDriveIdentity(station, programmed, true)) return null;

			raidDriveAuthorizations.put(token, new RaidDriveAuthorization(token, station.dX, station.dZ, station.stationKey, station.generation, expiresAt));
			player.inventory.setInventorySlotContents(player.inventory.currentItem, programmed);
			player.inventory.markDirty();
			if(player.inventoryContainer != null) player.inventoryContainer.detectAndSendChanges();
			markDirty();
			return programmed;
		}
		return null;
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
		while(iterator.hasNext()) {
			RaidDriveAuthorization authorization = iterator.next();
			if(authorization.expiresAt <= 0L || now >= authorization.expiresAt) { iterator.remove(); changed = true; }
		}
		if(changed) markDirty();
	}

	private static long safeAdd(long value, long amount) {
		if(amount > 0L && value > Long.MAX_VALUE - amount) return Long.MAX_VALUE;
		return value + amount;
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
	public synchronized boolean activateNormalStation(ItemStack drive, CelestialBody sourceBody, WorldServer orbitWorld) {
		if(drive == null || orbitWorld == null || !ItemVOTVdrive.isNormalStationDrive(drive)) return false;
		ItemVOTVdrive.Destination destination = ItemVOTVdrive.getDestinationUnchecked(drive);
		if(destination == null || destination.body != SolarSystem.Body.ORBIT) return false;
		OrbitalStation station = getStationAtGrid(destination.x, destination.z);
		if(station == null || station.deleting || !matchesDriveIdentity(station, drive, false)) return false;
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

	public synchronized boolean canLaunchRaidDrive(ItemStack drive, WorldServer orbitWorld) {
		OrbitalStation station = getStationForRaidDrive(drive, true);
		if(station == null || orbitWorld == null) return false;
		String token = drive.stackTagCompound.getString(ItemRaidDrive.TAG_RAID_TOKEN);
		if(station.raidPortActive) return token.equals(station.raidToken) && station.raidCleanupAt > System.currentTimeMillis()
			&& orbitWorld.getBlock(station.raidPortX, station.raidPortY, station.raidPortZ) == ModBlocks.orbital_station_raiding_port;
		return findRaidPortPosition(station, token, orbitWorld) != null;
	}

	/** Generates or reuses the one raid port associated with this drive token. */
	public synchronized boolean activateRaidPort(ItemStack drive, WorldServer orbitWorld) {
		OrbitalStation station = getStationForRaidDrive(drive, true);
		if(station == null || orbitWorld == null) return false;
		String token = drive.stackTagCompound.getString(ItemRaidDrive.TAG_RAID_TOKEN);
		long expiresAt = drive.stackTagCompound.getLong(ItemRaidDrive.TAG_EXPIRES_AT);
		if(token.isEmpty() || expiresAt <= System.currentTimeMillis()) return false;
		if(station.raidPortActive) return token.equals(station.raidToken) && orbitWorld.getBlock(station.raidPortX, station.raidPortY, station.raidPortZ) == ModBlocks.orbital_station_raiding_port;

		int[] position = findRaidPortPosition(station, token, orbitWorld);
		if(position == null || !OrbitalStation.spawnRaidPort(orbitWorld, position[0], position[1])) return false;
		station.raidPortActive = true;
		station.raidToken = token;
		station.raidPortX = position[0];
		station.raidPortY = OrbitalStation.CORE_Y;
		station.raidPortZ = position[1];
		station.raidExpiresAt = expiresAt;
		station.raidCleanupAt = safeAdd(expiresAt, Math.max(1L, SpaceConfig.raidPortCleanupDelaySeconds) * 1000L);
		station.raidLastWarningInterval = -1L;
		station.raidExpirationWarningSent = false;
		markDirty();
		return true;
	}

	public synchronized OrbitalStation getStationForRaidDrive(ItemStack drive, boolean requireActive) {
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
			if(expiresAt <= System.currentTimeMillis()) return null;
			token = UUID.randomUUID().toString();
			drive.stackTagCompound.setString(ItemRaidDrive.TAG_RAID_TOKEN, token);
			drive.stackTagCompound.setString(ItemVOTVdrive.TAG_STATION_KEY, station.stationKey);
			drive.stackTagCompound.setInteger(ItemVOTVdrive.TAG_STATION_GENERATION, station.generation);
			drive.stackTagCompound.setString(ItemVOTVdrive.TAG_STATION_DRIVE_TYPE, ItemVOTVdrive.DRIVE_TYPE_RAID);
			drive.stackTagCompound.setInteger("sDim", station.orbiting == null ? 0 : station.orbiting.dimensionId);
			drive.stackTagCompound.setBoolean("sHas", station.hasStation);
			authorization = new RaidDriveAuthorization(token, station.dX, station.dZ, station.stationKey, station.generation, expiresAt);
			raidDriveAuthorizations.put(token, authorization);
			markDirty();
		}

		if(authorization == null) return null;
		if(authorization.expiresAt != drive.stackTagCompound.getLong(ItemRaidDrive.TAG_EXPIRES_AT) || authorization.x != destination.x || authorization.z != destination.z) return null;
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
		int[][] centers = new int[][] {
			{centerChunkX + 25, centerChunkZ},
			{centerChunkX, centerChunkZ + 25},
			{centerChunkX - 25, centerChunkZ},
			{centerChunkX, centerChunkZ - 25}
		};
		int start = token == null ? 0 : (token.hashCode() & Integer.MAX_VALUE) % centers.length;
		for(int i = 0; i < centers.length; i++) {
			int[] candidate = centers[(start + i) % centers.length];
			int blockX = candidate[0] * OrbitalStation.CHUNK_SIZE;
			int blockZ = candidate[1] * OrbitalStation.CHUNK_SIZE;
			int cleanupMinX = candidate[0] - OrbitalStation.RAID_CLEANUP_CHUNKS / 2;
			int cleanupMinZ = candidate[1] - OrbitalStation.RAID_CLEANUP_CHUNKS / 2;
			if(isChunkAreaEmpty(world, cleanupMinX, cleanupMinZ, OrbitalStation.RAID_CLEANUP_CHUNKS, OrbitalStation.RAID_CLEANUP_CHUNKS)
				&& !cleanupAreaOverlaps(cleanupMinX, cleanupMinZ, OrbitalStation.RAID_CLEANUP_CHUNKS, OrbitalStation.RAID_CLEANUP_CHUNKS)) return new int[] {blockX, blockZ};
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
		if(repairing) broadcastToStation(world, station, EnumChatFormatting.GREEN + "The Orbital Station Computer has been restored. The destruction countdown has been cancelled.");
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
		if(repairing) broadcastToStation(world, station, EnumChatFormatting.GREEN + "The Orbital Station Computer has been restored. The destruction countdown has been cancelled.");
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
		long crashSeconds = Math.max(1L, SpaceConfig.stationComputerCrashTimeSeconds);
		station.computerCrashTicksRemaining = safeMultiplyByTwenty(crashSeconds);
		station.computerNextWarningTicks = nextComputerWarningThreshold(station.computerCrashTicksRemaining);
		markDirty();
		broadcastComputerWarning(world, station);
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
		long threshold = fromTicks - COMPUTER_WARNING_INTERVAL_TICKS;
		return threshold > 0L ? threshold : -1L;
	}

	private void broadcastComputerWarning(World world, OrbitalStation station) {
		if(station == null) return;
		long remainingTicks = Math.max(0L, station.computerCrashTicksRemaining);
		long remainingSeconds = remainingTicks / 20L + (remainingTicks % 20L == 0L ? 0L : 1L);
		IChatComponent warning = new ChatComponentText("Warning station will crash in "
			+ formatDurationSeconds(remainingSeconds) + " replace orbital station computer to stop");
		warning.setChatStyle(new ChatStyle().setColor(EnumChatFormatting.RED));
		broadcastToStation(world, station, warning);
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

	/** Called exactly once per server tick by ModEventHandler. */
	public synchronized void tickMaintenance() {
		long now = System.currentTimeMillis();
		World world = DimensionManager.getWorld(SpaceConfig.orbitDimension);
		WorldServer orbitWorld = world instanceof WorldServer ? (WorldServer)world : null;
		boolean changed = false;
		for(OrbitalStation station : new ArrayList<OrbitalStation>(stations.values())) {
			if(station == null || station.deleting) continue;
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
						broadcastToStation(orbitWorld, station, EnumChatFormatting.RED + "Warning: Raid station will crash in " + formatDurationSeconds(remainingSeconds) + ".");
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
		if(orbitWorld != null && !cleanupTasks.isEmpty()) processCleanupTask(orbitWorld, cleanupTasks.get(0));
		if(changed) markDirty();
	}

	private void queueRaidCleanup(OrbitalStation station) {
		if(station == null || !station.raidPortActive || isCleanupQueued(CleanupType.RAID, station.dX, station.dZ, station.raidToken)) return;
		CleanupTask task = CleanupTask.raid(station);
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
		// Compatibility for station cleanup tasks saved before immediate priority
		// deprogramming was added. Do this before relocating players or clearing the first chunk.
		if(task.type == CleanupType.STATION && !task.playerDrivesDeprogrammed) {
			resetPlayerNormalStationDrives(task.stationX, task.stationZ, task.identity, task.generation);
			resetLaunchPadNormalStationDrives(task.stationX, task.stationZ, task.identity, task.generation);
			task.playerDrivesDeprogrammed = true;
			markDirty();
		}

		relocatePlayers(world, task);
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
		finishCleanup(task);
		cleanupTasks.remove(task);
		markDirty();
	}

	@SuppressWarnings("unchecked")
	private void relocatePlayers(WorldServer world, CleanupTask task) {
		double minX = task.minChunkX * 16D;
		double minZ = task.minChunkZ * 16D;
		double maxX = (task.minChunkX + task.width) * 16D;
		double maxZ = (task.minChunkZ + task.height) * 16D;
		List<EntityPlayerMP> players = new ArrayList<EntityPlayerMP>();
		for(Object object : world.playerEntities) {
			if(!(object instanceof EntityPlayerMP)) continue;
			EntityPlayerMP player = (EntityPlayerMP)object;
			if(player.posX >= minX && player.posX < maxX && player.posZ >= minZ && player.posZ < maxZ) players.add(player);
		}
		for(EntityPlayerMP player : players) {
			player.mountEntity(null);
			OrbitalStation station = getStationAtGrid(task.stationX, task.stationZ);
			CelestialBody body = CelestialBody.getBody(task.bodyName);
			if(body == null) body = station != null ? station.orbiting : CelestialBody.getBody(0);
			if(body != null) CelestialTeleporter.teleport(player, body.dimensionId, randomProbeCoordinate(), 800D, randomProbeCoordinate(), false);
		}
	}

	private int randomProbeCoordinate() {
		int range = Math.max(1, SpaceConfig.maxProbeDistance);
		return MathHelper.floor_double((rand.nextDouble() * 2D - 1D) * range);
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

	private void finishCleanup(CleanupTask task) {
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
			return;
		}

		ChunkCoordIntPair pos = new ChunkCoordIntPair(task.stationX, task.stationZ);
		if(station == null || task.identity.isEmpty() || (task.identity.equals(station.stationKey) && task.generation == station.generation)) {
			int next = Math.max(getNextGeneration(pos), task.generation + 1);
			stationGenerations.put(pos, next);
			stations.remove(pos);
			invalidateRaidDriveAuthorizationsForStation(task.stationX, task.stationZ);
			resetLoadedNormalStationDrives(task.stationX, task.stationZ, task.identity, task.generation);
		}
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
		private final long expiresAt;
		private RaidDriveAuthorization(String token, int x, int z, String stationKey, int generation, long expiresAt) {
			this.token = token == null ? "" : token;
			this.x = x;
			this.z = z;
			this.stationKey = stationKey == null ? "" : stationKey;
			this.generation = generation;
			this.expiresAt = expiresAt;
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
			task.minChunkX = task.coreChunkX - 5; task.minChunkZ = task.coreChunkZ - 5;
			task.width = OrbitalStation.RAID_CLEANUP_CHUNKS; task.height = OrbitalStation.RAID_CLEANUP_CHUNKS;
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
				task.width = tag.hasKey("width") ? tag.getInteger("width") : OrbitalStation.RAID_CLEANUP_CHUNKS;
				task.height = tag.hasKey("height") ? tag.getInteger("height") : OrbitalStation.RAID_CLEANUP_CHUNKS;
				task.coreX = tag.hasKey("coreX") ? tag.getInteger("coreX") : (task.minChunkX + 5) * OrbitalStation.CHUNK_SIZE;
				task.coreY = tag.hasKey("coreY") ? tag.getInteger("coreY") : OrbitalStation.CORE_Y;
				task.coreZ = tag.hasKey("coreZ") ? tag.getInteger("coreZ") : (task.minChunkZ + 5) * OrbitalStation.CHUNK_SIZE;
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
				int expectedMinX = task.coreChunkX - OrbitalStation.RAID_CLEANUP_CHUNKS / 2;
				int expectedMinZ = task.coreChunkZ - OrbitalStation.RAID_CLEANUP_CHUNKS / 2;
				if(task.minChunkX != expectedMinX || task.minChunkZ != expectedMinZ
					|| task.width != OrbitalStation.RAID_CLEANUP_CHUNKS || task.height != OrbitalStation.RAID_CLEANUP_CHUNKS) {
					MainRegistry.logger.warn("[StationMaintenance] Repairing raid cleanup bounds station=" + getStationId(task.stationX, task.stationZ)
						+ " token=" + shortIdentity(task.identity) + " coreChunk=" + task.coreChunkX + "," + task.coreChunkZ);
				}
				task.minChunkX = expectedMinX;
				task.minChunkZ = expectedMinZ;
				task.width = OrbitalStation.RAID_CLEANUP_CHUNKS;
				task.height = OrbitalStation.RAID_CLEANUP_CHUNKS;
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
