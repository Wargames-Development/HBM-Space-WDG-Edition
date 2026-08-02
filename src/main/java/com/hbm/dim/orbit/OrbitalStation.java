package com.hbm.dim.orbit;

import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Stack;
import java.util.UUID;

import com.hbm.blocks.BlockDummyable;
import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.machine.BlockOrbitalStation;
import com.hbm.dim.CelestialBody;
import com.hbm.dim.SolarSystem;
import com.hbm.dim.SolarSystemWorldSavedData;
import com.hbm.dim.WorldProviderCelestial;
import com.hbm.dim.SolarSystem.AstroMetric;
import com.hbm.entity.missile.EntityRideableRocket;
import com.hbm.entity.missile.EntityRideableRocket.RocketState;
import com.hbm.handler.ThreeInts;
import com.hbm.items.ItemVOTVdrive.Destination;
import com.hbm.tileentity.machine.TileEntityOrbitalStation;
import com.hbm.tileentity.machine.TileEntityOrbitalStationRaidingPort;
import com.hbm.util.BobMathUtil;
import com.hbm.util.BufferUtil;

import api.hbm.tile.IPropulsion;
import io.netty.buffer.ByteBuf;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

public class OrbitalStation {

	public String name = ""; // I dub thee

	public CelestialBody orbiting;
	public CelestialBody target;

	private double eclipseAmount;

	public StationState state = StationState.ORBIT;
	public int stateTimer;
	public int maxStateTimer = 100;

	public boolean hasStation = false;

	/** Stable identity used to reject stale drives when a grid cell is reused. */
	public String stationKey = "";
	public int generation = 0;
	public boolean reservedForLaunch = false;
	public boolean deleting = false;

	/** Required-computer state. The countdown uses server-running ticks. */
	public boolean computerRequired = false;
	public boolean hasComputer = false;
	public int computerX = Integer.MIN_VALUE;
	public int computerY = Integer.MIN_VALUE;
	public int computerZ = Integer.MIN_VALUE;
	public long computerCrashTicksRemaining = -1L;
	/** Remaining-countdown threshold for the next persisted 10-minute warning. */
	public long computerNextWarningTicks = -1L;

	/** One active raid port is supported per station. */
	public boolean raidPortActive = false;
	public String raidToken = "";
	public int raidPortX = 0;
	public int raidPortY = 127;
	public int raidPortZ = 0;
	public long raidExpiresAt = 0L;
	public long raidCleanupAt = 0L;
	/** Persisted five-minute warning interval index; -1 means no warning has been sent yet. */
	public long raidLastWarningInterval = -1L;
	/** Legacy compatibility mirror for older saves and tooling. */
	public boolean raidExpirationWarningSent = false;

	// the coordinates of the station within the dimension
	public int dX;
	public int dZ;

	public boolean hasEngines = true;
	public List<ThreeInts> errorsAt = new ArrayList<ThreeInts>();
	public int errorTimer;

	public float gravityMultiplier = 1;

	public enum StationState {
		ORBIT, // big chillin
		LEAVING, // prepare engines for transfer
		TRANSFER, // going from A to B
		ARRIVING, // spool down engines
	}

	private TileEntityOrbitalStation mainPort;
	private TileEntityOrbitalStation raidPort;
	private HashMap<ThreeInts, TileEntityOrbitalStation> ports = new HashMap<>();
	private int portIndex = 0;

	private HashSet<IPropulsion> engines = new HashSet<>();

	public static OrbitalStation clientStation = new OrbitalStation(CelestialBody.getBody(0));
	public static List<OrbitalStation> orbitingStations = new ArrayList<OrbitalStation>();

	public static final int CHUNK_SIZE = 16;
	public static final int STATION_CHUNKS = 64;
	public static final int STATION_SIZE = STATION_CHUNKS * CHUNK_SIZE;
	public static final int WARNING_SIZE = 32; // advisory warning only; falling begins after leaving the full 64x64 area
	public static final int BOTTOM_FALL_Y = -32;
	public static final int CORE_Y = 127;
	public static final int INNER_RAID_BOX_CHUNKS = 48;
	public static final int RAID_PORT_CHUNKS = 2;
	public static final int RAID_CLEANUP_CHUNKS = 10;



	/**
	 * Space station spatial space
	 * - Stations are spread out over the orbital dimension, in restricted areas
	 * - Attempting to leave the region your station is in will cause you to fall back to the orbited planet
	 * - The current station you are near is fetched using the player's XZ coordinate
	 * - The station will determine what body is being orbited, and therefore what to show in the skybox
	 */

	// For client
	public OrbitalStation(CelestialBody orbiting) {
		this.orbiting = orbiting;
		this.target = orbiting;
	}

	// For server
	public OrbitalStation(CelestialBody orbiting, int x, int z) {
		this(orbiting);
		this.dX = x;
		this.dZ = z;
	}

	public void ensureIdentity() {
		if(stationKey == null || stationKey.trim().isEmpty()) {
			stationKey = generation == 0 ? "legacy:" + dX + ":" + dZ : UUID.randomUUID().toString();
		}
	}

	public int getMinChunkX() { return dX * STATION_CHUNKS; }
	public int getMinChunkZ() { return dZ * STATION_CHUNKS; }
	public int getCenterChunkX() { return getMinChunkX() + STATION_CHUNKS / 2; }
	public int getCenterChunkZ() { return getMinChunkZ() + STATION_CHUNKS / 2; }
	public int getCenterBlockX() { return getCenterChunkX() * CHUNK_SIZE; }
	public int getCenterBlockZ() { return getCenterChunkZ() * CHUNK_SIZE; }

	public boolean containsBlock(double x, double z) {
		int chunkX = MathHelper.floor_double(x) >> 4;
		int chunkZ = MathHelper.floor_double(z) >> 4;
		return chunkX >= getMinChunkX() && chunkX < getMinChunkX() + STATION_CHUNKS
			&& chunkZ >= getMinChunkZ() && chunkZ < getMinChunkZ() + STATION_CHUNKS;
	}

	public void travelTo(World world, CelestialBody target) {
		if(state != StationState.ORBIT) return; // only when at rest can we start a new journey
		if(!canTravel(orbiting, target)) return;

		setState(StationState.LEAVING, getLeaveTime());
		this.target = target;
	}

	public void update(World world) {
		if(!world.isRemote) {
			eclipseAmount = -1;

			if(state == StationState.LEAVING) {
				if(stateTimer > maxStateTimer) {
					setState(StationState.TRANSFER, getTransferTime());
				}
			} else if(state == StationState.TRANSFER) {
				if(stateTimer > maxStateTimer) {
					setState(StationState.ARRIVING, getArriveTime());
					orbiting = target;
				}
			} else if(state == StationState.ARRIVING) {
				if(stateTimer > maxStateTimer) {
					setState(StationState.ORBIT, 0);
				}
			}

			SolarSystemWorldSavedData.get(world).markDirty();

			hasEngines = engines.size() > 0;

			errorTimer--;
			if(errorTimer <= 0) {
				errorsAt = new ArrayList<ThreeInts>();
				errorTimer = 0;
			}
		}

		stateTimer++;
	}

	private boolean canTravel(CelestialBody from, CelestialBody to) {
		if(engines.size() == 0) return false;

		double deltaV = SolarSystem.getDeltaVBetween(from, to);
		int shipMass = 200_000; // Always static, to not punish building big cool stations
		float totalThrust = getTotalThrust();

		boolean canTravel = true;
		errorsAt = new ArrayList<ThreeInts>();

		for(IPropulsion engine : engines) {
			float massPortion = engine.getThrust() / totalThrust;
			if(!engine.canPerformBurn(Math.round(shipMass * massPortion), deltaV)) {
				TileEntity te = engine.getTileEntity();
				canTravel = false;
				errorsAt.add(new ThreeInts(te.xCoord, te.yCoord, te.zCoord));
				errorTimer = 100;
			}
		}

		return canTravel;
	}

	private float getTotalThrust() {
		float thrust = 0;
		for(IPropulsion engine : engines) {
			thrust += engine.getThrust();
		}
		return thrust;
	}

	// Has the side effect of beginning engine burns
	private int getLeaveTime() {
		int leaveTime = 20;
		for(IPropulsion engine : engines) {
			int time = engine.startBurn();
			if(time > leaveTime) leaveTime = time;
		}
		return leaveTime;
	}

	// And this one will end engine burns
	private int getArriveTime() {
		int arriveTime = 20;
		for(IPropulsion engine : engines) {
			int time = engine.endBurn();
			if(time > arriveTime) arriveTime = time;
		}
		return arriveTime;
	}

	private int getTransferTime() {
		if(mainPort == null) return -1;

		int size = calculateSize();
		double distance = SolarSystem.calculateDistanceBetweenTwoBodies(mainPort.getWorldObj(), orbiting, target);
		float thrust = getTotalThrust();

		return calculateTransferTime(distance, size, thrust);
	}

	public static int calculateTransferTime(double distance, int size, float thrust) {
		return (int)(Math.log(1 + (distance * size / thrust * 100)) * 150);
	}

	public void setState(StationState state, int timeUntilNext) {
		this.state = state;
		stateTimer = 0;
		maxStateTimer = timeUntilNext;
	}

	public boolean recallPod(Destination destination) {
		if(!hasStation) return false;
		if(destination.body.getBody() != orbiting) return false;

		for(TileEntityOrbitalStation port : ports.values()) {
			EntityRideableRocket rocket = port.getDocked();

			if(rocket == null || !rocket.isReusable()) continue;

			// ensure the rocket has fuel before sending it off
			RocketState state = rocket.getState();
			if(state != RocketState.AWAITING && state != RocketState.LANDED) continue;

			// and make sure it doesn't have a rider!!
			if(rocket.riddenByEntity != null) continue;

			rocket.recallPod(destination);
			return true;
		}

		return false;
	}

	public static void addPropulsion(IPropulsion propulsion) {
		TileEntity te = propulsion.getTileEntity();
		OrbitalStation station = getStationFromPosition(te.xCoord, te.zCoord);
		station.engines.add(propulsion);
	}

	public static void removePropulsion(IPropulsion propulsion) {
		TileEntity te = propulsion.getTileEntity();
		OrbitalStation station = getStationFromPosition(te.xCoord, te.zCoord);
		station.engines.remove(propulsion);
	}

	public void addPort(TileEntityOrbitalStation port) {
		if(port == null) return;
		ports.put(new ThreeInts(port.xCoord, port.yCoord, port.zCoord), port);
		if(port.getBlockType() == ModBlocks.orbital_station) mainPort = port;
		if(port instanceof TileEntityOrbitalStationRaidingPort || port.getBlockType() == ModBlocks.orbital_station_raiding_port) raidPort = port;
	}

	public void removePort(TileEntityOrbitalStation port) {
		if(port == null) return;
		ports.remove(new ThreeInts(port.xCoord, port.yCoord, port.zCoord));
		if(mainPort == port) mainPort = null;
		if(raidPort == port) raidPort = null;
	}

	private TileEntityOrbitalStation choosePort(List<TileEntityOrbitalStation> candidates) {
		if(candidates.isEmpty()) return null;
		int index = 0;
		for(TileEntityOrbitalStation port : candidates) {
			if(!port.hasDocked && !port.isReserved) {
				portIndex = index;
				return port;
			}
			index++;
		}
		portIndex++;
		if(portIndex >= candidates.size()) portIndex = 0;
		return candidates.get(portIndex);
	}

	public TileEntityOrbitalStation getNormalPort() {
		List<TileEntityOrbitalStation> normal = new ArrayList<TileEntityOrbitalStation>();
		for(TileEntityOrbitalStation port : ports.values()) {
			if(!(port instanceof TileEntityOrbitalStationRaidingPort) && port.getBlockType() != ModBlocks.orbital_station_raiding_port) normal.add(port);
		}
		return choosePort(normal);
	}

	public TileEntityOrbitalStation getRaidPort() {
		if(raidPort != null && !raidPort.isInvalid()) return raidPort;
		List<TileEntityOrbitalStation> raid = new ArrayList<TileEntityOrbitalStation>();
		for(TileEntityOrbitalStation port : ports.values()) {
			if(port instanceof TileEntityOrbitalStationRaidingPort || port.getBlockType() == ModBlocks.orbital_station_raiding_port) raid.add(port);
		}
		return choosePort(raid);
	}

	/** Legacy callers always receive a normal station port. */
	public TileEntityOrbitalStation getPort() { return getNormalPort(); }

	public static TileEntityOrbitalStation getPort(int x, int z) {
		OrbitalStation station = getStationFromPosition(x, z);
		return station == null ? null : station.getNormalPort();
	}

	// I can't stop pronouncing this as hors d'oeuvre
	private static final ForgeDirection[] horDir = new ForgeDirection[] { ForgeDirection.NORTH, ForgeDirection.SOUTH, ForgeDirection.WEST, ForgeDirection.EAST };

	// calculates the top down area of the station
	// super fucking fast but like, don't call it every frame
	public int calculateSize() {
		if(mainPort == null) return 0;
		World world = mainPort.getWorldObj();

		int minX, maxX;
		int minZ, maxZ;
		minX = maxX = mainPort.xCoord;
		minZ = maxZ = mainPort.zCoord;

		Stack<ThreeInts> stack = new Stack<ThreeInts>();
		stack.push(new ThreeInts(mainPort.xCoord, mainPort.yCoord, mainPort.zCoord));

		HashSet<ThreeInts> visited = new HashSet<ThreeInts>();

		while(!stack.isEmpty()) {
			ThreeInts pos = stack.pop();
			visited.add(pos);

			if(pos.x < minX) minX = pos.x;
			if(pos.x > maxX) maxX = pos.x;
			if(pos.z < minZ) minZ = pos.z;
			if(pos.z > maxZ) maxZ = pos.z;

			for(ForgeDirection dir : horDir) {
				ThreeInts nextPos = pos.getPositionAtOffset(dir);

				if(!visited.contains(nextPos) && isInStation(world, nextPos)) {
					stack.push(nextPos);
				}
			}
		}

		return (maxX - minX + 1) * (maxZ - minZ + 1);
	}

	private boolean isInStation(World world, ThreeInts pos) {
		if(world.getHeightValue(pos.x, pos.z) > 1) return true;
		return Math.abs(pos.x - mainPort.xCoord) < 5 && Math.abs(pos.z - mainPort.zCoord) < 5; // minimum station size
	}

	public double getUnscaledProgress(float partialTicks) {
		if(state == StationState.ORBIT) return 0;
		return MathHelper.clamp_double(((double)stateTimer + partialTicks) / (double)maxStateTimer, 0, 1);
	}

	public double getTransferProgress(float partialTicks) {
		if(state != StationState.TRANSFER) return 0;
		return easeInOutCirc(getUnscaledProgress(partialTicks));
	}

	public double getEclipseAmount(World world) {
		if(eclipseAmount > -1) return eclipseAmount;
		
		double sunSize = SolarSystem.calculateSunSize(orbiting);

		double progress = getTransferProgress(0);

		List<AstroMetric> metrics;

		// Get our orrery of bodies, this is cached for reuse in sky rendering
		if(state == StationState.ORBIT) {
			double altitude = WorldProviderOrbit.getOrbitalAltitude(orbiting);
			metrics = SolarSystem.calculateMetricsFromSatellite(world, 0, orbiting, altitude);
		} else {
			double fromAlt = WorldProviderOrbit.getOrbitalAltitude(orbiting);
			double toAlt = WorldProviderOrbit.getOrbitalAltitude(target);
			metrics = SolarSystem.calculateMetricsBetweenSatelliteOrbits(world, 0, orbiting, target, fromAlt, toAlt, progress);

			double sunTargetSize = SolarSystem.calculateSunSize(target);
			sunSize = BobMathUtil.lerp(progress, sunSize, sunTargetSize);
		}

		// Get our eclipse amount
		eclipseAmount = WorldProviderCelestial.getEclipseFactor(metrics, sunSize, SolarSystem.MAX_APPARENT_SIZE_ORBIT);

		return eclipseAmount;
	}

	private double easeInOutCirc(double t) {
		return t < 0.5
			? (1 - Math.sqrt(1 - Math.pow(2 * t, 3))) / 2
			: (Math.sqrt(1 - Math.pow(-2 * t + 2, 3)) + 1) / 2;
	}

	// Finds a space station for a given set of coordinates
	public static OrbitalStation getStationFromPosition(int x, int z) {
		SolarSystemWorldSavedData data = SolarSystemWorldSavedData.get();
		int gridX = MathHelper.floor_double((double)x / STATION_SIZE);
		int gridZ = MathHelper.floor_double((double)z / STATION_SIZE);
		if(data == null) return new OrbitalStation(CelestialBody.getBody(0), gridX, gridZ);

		OrbitalStation station = data.getStationFromPosition(x, z);

		// Do not silently recreate missing or deleting stations. Legacy callers receive
		// an inert, non-persisted placeholder instead.
		if(station == null || station.deleting) return new OrbitalStation(CelestialBody.getBody(0), gridX, gridZ);
		return station;
	}

	public static OrbitalStation getStation(int x, int z) {
		return getStationFromPosition(x * STATION_SIZE, z * STATION_SIZE);
	}

	public void serialize(ByteBuf buf) {
		buf.writeInt(orbiting.dimensionId);
		buf.writeInt(target.dimensionId);
		buf.writeInt(state.ordinal());
		buf.writeInt(stateTimer);
		buf.writeInt(maxStateTimer);
		buf.writeBoolean(hasEngines);
		buf.writeFloat(gravityMultiplier);

		BufferUtil.writeString(buf, name);

		buf.writeInt(errorsAt.size());
		for(ThreeInts error : errorsAt) {
			buf.writeInt(error.x);
			buf.writeInt(error.y);
			buf.writeInt(error.z);
		}
	}

	public static OrbitalStation deserialize(ByteBuf buf) {
		OrbitalStation station = new OrbitalStation(CelestialBody.getBody(buf.readInt()));
		station.target = CelestialBody.getBody(buf.readInt());
		station.state = StationState.values()[buf.readInt()];
		station.stateTimer = buf.readInt();
		station.maxStateTimer = buf.readInt();
		station.hasEngines = buf.readBoolean();
		station.gravityMultiplier = buf.readFloat();

		station.name = BufferUtil.readString(buf);

		station.errorsAt = new ArrayList<ThreeInts>();
		int count = buf.readInt();
		for(int i = 0; i < count; i++) {
			int x = buf.readInt();
			int y = buf.readInt();
			int z = buf.readInt();

			station.errorsAt.add(new ThreeInts(x, y, z));
		}
		return station;
	}

	public static boolean spawn(World world, int x, int z) {
		if(world == null) return false;
		if(world.getBlock(x, CORE_Y, z) == ModBlocks.orbital_station) return true;

		BlockOrbitalStation block = (BlockOrbitalStation) ModBlocks.orbital_station;
		boolean oldSafeRem = BlockDummyable.safeRem;
		BlockDummyable.safeRem = true;
		try {
			if(!world.setBlock(x, CORE_Y, z, block, 12, 3)) return false;
			block.fillSpace(world, x, CORE_Y, z, ForgeDirection.NORTH, 0);
			return world.getBlock(x, CORE_Y, z) == ModBlocks.orbital_station;
		} finally {
			BlockDummyable.safeRem = oldSafeRem;
		}
	}

	public static boolean spawnRaidPort(World world, int x, int z) {
		if(world == null) return false;
		if(world.getBlock(x, CORE_Y, z) == ModBlocks.orbital_station_raiding_port) return true;
		if(world.getBlock(x, CORE_Y, z) != net.minecraft.init.Blocks.air) return false;

		BlockOrbitalStation block = (BlockOrbitalStation) ModBlocks.orbital_station_raiding_port;
		boolean oldSafeRem = BlockDummyable.safeRem;
		BlockDummyable.safeRem = true;
		try {
			if(!world.setBlock(x, CORE_Y, z, block, 12, 3)) return false;
			block.fillSpace(world, x, CORE_Y, z, ForgeDirection.NORTH, 0);
			return world.getBlock(x, CORE_Y, z) == ModBlocks.orbital_station_raiding_port;
		} finally {
			BlockDummyable.safeRem = oldSafeRem;
		}
	}

	// Mark the station as travelable
	public static void addStation(int x, int z, CelestialBody body) {
		SolarSystemWorldSavedData data = SolarSystemWorldSavedData.get();
		if(data == null || data.isStationDeleted(x, z)) return;
		OrbitalStation station = data.getStationFromPosition(x * STATION_SIZE, z * STATION_SIZE);

		if(station == null) {
			station = data.addStation(x, z, body);
		}

		if(station == null) return;
		station.orbiting = station.target = body;
		station.hasStation = true;
		data.markDirty();
	}

}
