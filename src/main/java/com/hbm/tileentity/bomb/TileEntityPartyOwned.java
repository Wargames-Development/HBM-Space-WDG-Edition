package com.hbm.tileentity.bomb;

import api.hbm.tile.IPartyOwned;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;

import java.util.UUID;

public class TileEntityPartyOwned extends TileEntity implements IPartyOwned {
	//Owner Serialization
	public UUID ownerParty;

	@Override
	public UUID getOwner() {
		return ownerParty;
	}

	@Override
	public void writeToNBT(NBTTagCompound nbt) {
		super.writeToNBT(nbt);
		if (ownerParty != null) {
			nbt.setLong("ownerMost", ownerParty.getMostSignificantBits());
			nbt.setLong("ownerLeast", ownerParty.getLeastSignificantBits());
		}
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);

		if (nbt.hasKey("ownerMost") && nbt.hasKey("ownerLeast")) {
			this.ownerParty = new UUID(
				nbt.getLong("ownerMost"),
				nbt.getLong("ownerLeast")
			);
		}
	}
}
