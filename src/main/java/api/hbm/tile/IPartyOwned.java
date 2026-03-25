package api.hbm.tile;

import java.util.UUID;

public interface IPartyOwned {

	//Use TileEntityPartyOwned as pattern for implementation, be sure to include NBT reading and writing of ownerID
	public UUID getOwner();
	public void setOwner(UUID owner);
}
