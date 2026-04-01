package com.hbm.explosion;

import api.hbm.wgc.Integrations;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;

import java.util.Set;
import java.util.UUID;

public class ExplosionNukeAdvanced {
	public int posX;
	public int posY;
	public int posZ;
	public int lastposX = 0;
	public int lastposZ = 0;
	public int radius;
	public int radius2;
	public World worldObj;
	private int n = 1;
	private int nlimit;
	private int shell;
	private int leg;
	private int element;
	public float explosionCoefficient = 1.0F;
	public int type = 0;
	public UUID ownedParty;
	private Set<ChunkCoordIntPair> expProtectedChunks;
	private Set<ChunkCoordIntPair> contamProtectedChunks;

	public void saveToNbt(NBTTagCompound nbt, String name) {
		nbt.setInteger(name + "posX", posX);
		nbt.setInteger(name + "posY", posY);
		nbt.setInteger(name + "posZ", posZ);
		nbt.setInteger(name + "lastposX", lastposX);
		nbt.setInteger(name + "lastposZ", lastposZ);
		nbt.setInteger(name + "radius", radius);
		nbt.setInteger(name + "radius2", radius2);
		nbt.setInteger(name + "n", n);
		nbt.setInteger(name + "nlimit", nlimit);
		nbt.setInteger(name + "shell", shell);
		nbt.setInteger(name + "leg", leg);
		nbt.setInteger(name + "element", element);
		nbt.setFloat(name + "explosionCoefficient", explosionCoefficient);
		nbt.setInteger(name + "type", type);
		if (ownedParty != null) {
			nbt.setLong("ownerMost", ownedParty.getMostSignificantBits());
			nbt.setLong("ownerLeast", ownedParty.getLeastSignificantBits());
		}
	}

	public void readFromNbt(NBTTagCompound nbt, String name) {
		posX = nbt.getInteger(name + "posX");
		posY = nbt.getInteger(name + "posY");
		posZ = nbt.getInteger(name + "posZ");
		lastposX = nbt.getInteger(name + "lastposX");
		lastposZ = nbt.getInteger(name + "lastposZ");
		radius = nbt.getInteger(name + "radius");
		radius2 = nbt.getInteger(name + "radius2");
		n = nbt.getInteger(name + "n");
		nlimit = nbt.getInteger(name + "nlimit");
		shell = nbt.getInteger(name + "shell");
		leg = nbt.getInteger(name + "leg");
		element = nbt.getInteger(name + "element");
		explosionCoefficient = nbt.getFloat(name + "explosionCoefficient");
		type = nbt.getInteger(name + "type");
		if(nbt.hasKey("ownerMost")&&nbt.hasKey("ownerLeast"))
		{
			this.ownedParty = new UUID(
				nbt.getLong("ownerMost"),
				nbt.getLong("ownerLeast")
			);
		}
	}

	public ExplosionNukeAdvanced(UUID party,int x, int y, int z, World world, int rad, float coefficient, int typ) {
		this.ownedParty = party;
		this.posX = x;
		this.posY = y;
		this.posZ = z;
		this.worldObj = world;
		this.radius = rad;
		this.radius2 = this.radius * this.radius;
		this.explosionCoefficient = Math.min(Math.max((rad + coefficient * (y - 60)) / (coefficient * rad), 1 / coefficient), 1.0f);
		this.type = typ;
		this.nlimit = this.radius2 * 4;
		this.expProtectedChunks = Integrations.getExplosionProtectedChunksWGC(party,world,x,z,rad+16);
		this.contamProtectedChunks = Integrations.getContamProtectedChunksWGC(party,world,x,z,rad+16);
	}

	public boolean update() {
		switch (this.type) {
			case 0: breakColumn(this.lastposX, this.lastposZ); break;
			case 1: vapor(this.lastposX, this.lastposZ); break;
			case 2: waste(this.lastposX, this.lastposZ); break;
		}
		this.shell = (int)Math.floor((Math.sqrt(n) + 1) / 2);
		int shell2 = this.shell * 2;
		this.leg = (int)Math.floor((this.n - (shell2 - 1) * (shell2 - 1)) / shell2);
		this.element = (this.n - (shell2 - 1) * (shell2 - 1)) - shell2 * this.leg - this.shell + 1;
		this.lastposX = this.leg == 0 ? this.shell : this.leg == 1 ? -this.element : this.leg == 2 ? -this.shell : this.element;
		this.lastposZ = this.leg == 0 ? this.element : this.leg == 1 ? this.shell : this.leg == 2 ? -this.element : -this.shell;
		this.n++;
		return this.n > this.nlimit;
	}

	private void breakColumn(int x, int z) {
		if(expProtectedChunks.contains(new ChunkCoordIntPair(x>>4, z>>4))) return;

		int dist = this.radius2 - (x * x + z * z);
		if (dist > 0) {
			dist = (int)Math.sqrt(dist);
			for (int y = dist; y > -dist * this.explosionCoefficient; y--) {
				int wx = this.posX + x;
				int wy = this.posY + y;
				int wz = this.posZ + z;

				if (y < 8) {
					// Keep the fast skip only when allowed
					y -= ExplosionNukeGeneric.destruction(this.worldObj, wx, wy, wz);
				} else {
					ExplosionNukeGeneric.destruction(this.worldObj, wx, wy, wz);
				}
			}
		}
	}


	private void vapor(int x, int z) {
		if(expProtectedChunks.contains(new ChunkCoordIntPair(x>>4, z>>4))) return;

		int dist = this.radius2 - (x * x + z * z);
		if (dist > 0) {
			dist = (int)Math.sqrt(dist);
			for (int y = dist; y > -dist * this.explosionCoefficient; y--) {
				int wx = this.posX + x;
				int wy = this.posY + y;
				int wz = this.posZ + z;

					// Only apply the bigger vertical skip when allowed
					y -= ExplosionNukeGeneric.vaporDest(this.worldObj, wx, wy, wz);
			}
		}
	}


	private void waste(int x, int z) {
		if(contamProtectedChunks.contains(new ChunkCoordIntPair(x>>4, z>>4))) return;

		int dist = this.radius2 - (x * x + z * z);
		if (dist > 0) {
			dist = (int)Math.sqrt(dist);
			for (int y = dist; y > -dist * this.explosionCoefficient; y--) {
				int wx = this.posX + x;
				int wy = this.posY + y;
				int wz = this.posZ + z;

				if (radius >= 95)
					ExplosionNukeGeneric.wasteDest(this.worldObj, wx, wy, wz);
				else
					ExplosionNukeGeneric.wasteDestNoSchrab(this.worldObj, wx, wy, wz);
			}
		}
	}

}
