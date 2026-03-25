package com.hbm.tileentity.bomb;

import api.hbm.tile.IPartyOwned;
import com.hbm.blocks.bomb.BlockChargeBase;

import com.hbm.tileentity.TileEntityLoadedBase;
import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.NBTTagCompound;

import java.util.UUID;

public class TileEntityCharge extends TileEntityLoadedBase implements IPartyOwned {

	public boolean started;
	public int timer;
	public UUID ownerParty;

	@Override
	public void updateEntity() {

		if (!worldObj.isRemote) {

			if (started) {
				timer--;

				if (timer % 20 == 0 && timer > 0)
					worldObj.playSoundEffect(xCoord, yCoord, zCoord, "hbm:weapon.fstbmbPing", 1.0F, 1.0F);

				if (timer <= 0) {
					((BlockChargeBase) this.getBlockType()).explode(worldObj, xCoord, yCoord, zCoord);
				}
			}

			networkPackNT(100);
		}
	}

	@Override
	public void serialize(ByteBuf buf) {
		buf.writeInt(this.timer);
		buf.writeBoolean(this.started);
	}

	@Override
	public void deserialize(ByteBuf buf) {
		this.timer = buf.readInt();
		this.started = buf.readBoolean();
	}

	public String getMinutes() {

		String mins = "" + (timer / 1200);

		if (mins.length() == 1)
			mins = "0" + mins;

		return mins;
	}

	public String getSeconds() {

		String mins = "" + ((timer / 20) % 60);

		if (mins.length() == 1)
			mins = "0" + mins;

		return mins;
	}
	public UUID getOwner() {
		return ownerParty;
	}
	public void setOwner(UUID owner) {
		ownerParty = owner;
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
