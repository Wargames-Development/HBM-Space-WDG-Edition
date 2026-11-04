package com.hbm.util;

import api.hbm.wgc.Integrations;
import com.hbm.tileentity.deco.TileEntityGeysir;
import com.hbm.blocks.bomb.BlockVolcano.TileEntityVolcanoCore;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

/** Server-created worldgen provenance. A null owner alone never creates this context. */
public final class NaturalHazardSource {
    private final int dimension, x, y, z;
    private NaturalHazardSource(int dimension, int x, int y, int z) {
        this.dimension = dimension; this.x = x; this.y = y; this.z = z;
    }
    public static void markGenerated(World world, int x, int y, int z) {
        if(world == null || world.isRemote || world.provider == null) return;
        TileEntity tile = world.getTileEntity(x,y,z);
        if(tile instanceof TileEntityGeysir) ((TileEntityGeysir)tile).setNaturalOrigin(new NaturalHazardSource(world.provider.dimensionId,x,y,z));
        if(tile instanceof TileEntityVolcanoCore) ((TileEntityVolcanoCore)tile).setNaturalOrigin(new NaturalHazardSource(world.provider.dimensionId,x,y,z));
    }
    public boolean isAt(World world, int x, int y, int z) {
        return world != null && world.provider != null && dimension == world.provider.dimensionId
                && this.x == x && this.y == y && this.z == z;
    }
    public boolean isValid(World world) {
        if(world == null || world.isRemote || !isAt(world,x,y,z) || !world.blockExists(x,y,z)) return false;
        TileEntity tile = world.getTileEntity(x,y,z);
        NaturalHazardSource source = tile instanceof TileEntityGeysir ? ((TileEntityGeysir)tile).getNaturalOrigin()
                : tile instanceof TileEntityVolcanoCore ? ((TileEntityVolcanoCore)tile).getNaturalOrigin() : null;
        return source != null && source.isAt(world,x,y,z);
    }
    public boolean canAffect(World world, int targetX, int targetZ, String effect) {
        return isValid(world) && Integrations.canNaturalHazardAffectWGC(world,x,z,targetX,targetZ,effect);
    }
    public NBTTagCompound write() {
        NBTTagCompound tag = new NBTTagCompound(); tag.setInteger("dimension",dimension);
        tag.setInteger("x",x); tag.setInteger("y",y); tag.setInteger("z",z); return tag;
    }
    public static NaturalHazardSource read(NBTTagCompound tag) {
        if(tag == null || !tag.hasKey("dimension") || !tag.hasKey("x") || !tag.hasKey("y") || !tag.hasKey("z")) return null;
        return new NaturalHazardSource(tag.getInteger("dimension"),tag.getInteger("x"),tag.getInteger("y"),tag.getInteger("z"));
    }
}
