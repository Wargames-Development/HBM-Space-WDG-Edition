package com.hbm.tileentity.machine;

/** Separate tile identity so a raid core cannot become the normal station main port. */
public class TileEntityOrbitalStationRaidingCore extends TileEntityOrbitalStation {

	@Override
	public boolean isCore() {
		return false;
	}
}
