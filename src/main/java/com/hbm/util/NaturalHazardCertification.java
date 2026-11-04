package com.hbm.util;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hbm.blocks.bomb.BlockVolcano.TileEntityVolcanoCore;
import com.hbm.tileentity.deco.TileEntityGeysir;

import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

/** Server-authored allowlist for imported terrain. No commands, chunk scans or forced loads. */
public final class NaturalHazardCertification {
    private static final Logger LOGGER = LogManager.getLogger("HBM/NaturalSources");
    private static Map<String, Entry> entries = Collections.emptyMap();
    private static int certifiedCount;

    private NaturalHazardCertification() { }

    public static void configure(File file) {
        entries = Collections.emptyMap();
        certifiedCount = 0;
        if(!file.isFile()) return;
        try(Reader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
            entries = parse(reader);
            LOGGER.info("Loaded {} template-listed natural sources from {}. Matching sources will be certified as their chunks tick.", entries.size(), file);
        } catch(IOException | RuntimeException error) {
            // Reject the whole malformed list. Ordinary saved/worldgen provenance is unaffected.
            entries = Collections.emptyMap();
            LOGGER.error("Natural-source list {} was rejected; no sources from this file will be certified.", file, error);
        }
    }

    public static Map<String, Entry> parse(Reader reader) {
        JsonObject root = new JsonParser().parse(reader).getAsJsonObject();
        if(integer(root, "schema_version") != 1) throw new IllegalArgumentException("Unsupported source-list schema");
        JsonArray sources = root.getAsJsonArray("sources");
        if(sources == null || sources.size() == 0 || sources.size() > 100000)
            throw new IllegalArgumentException("Expected 1..100000 explicit sources");
        Map<String, Entry> parsed = new HashMap<>();
        for(JsonElement value : sources) {
            JsonObject source = value.getAsJsonObject();
            Entry entry = new Entry(integer(source,"dimension"), integer(source,"x"), integer(source,"y"), integer(source,"z"),
                    source.get("block").getAsString(), source.get("tile_entity").getAsString());
            if(!entry.isSupported() || entry.y < 0 || entry.y >= 2048)
                throw new IllegalArgumentException("Unsupported natural source at " + entry.key());
            if(parsed.put(entry.key(),entry) != null) throw new IllegalArgumentException("Duplicate natural source " + entry.key());
        }
        return Collections.unmodifiableMap(parsed);
    }

    private static int integer(JsonObject object, String name) {
        JsonElement value = object.get(name);
        if(value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()
                || !value.getAsString().matches("-?(0|[1-9][0-9]*)"))
            throw new IllegalArgumentException("Expected integer " + name);
        return Integer.parseInt(value.getAsString());
    }

    /** Called once by a loaded source tile, before its first server effect. */
    public static boolean certifyListed(TileEntity tile) {
        World world = tile != null ? tile.getWorldObj() : null;
        if(world == null || world.isRemote || world.provider == null || tile.isInvalid()) return false;
        Entry entry = entries.get(key(world.provider.dimensionId,tile.xCoord,tile.yCoord,tile.zCoord));
        if(entry == null || !world.blockExists(tile.xCoord,tile.yCoord,tile.zCoord)) return false;
        Object registryName = Block.blockRegistry.getNameForObject(world.getBlock(tile.xCoord,tile.yCoord,tile.zCoord));
        if(!entry.matches(tile,registryName != null ? registryName.toString() : "")) {
            LOGGER.warn("Skipped template-listed natural source at {}: live block/tile does not match {} / {}.", entry.key(), entry.block, entry.tileEntity);
            return false;
        }
        NaturalHazardSource.markGenerated(world,tile.xCoord,tile.yCoord,tile.zCoord);
        certifiedCount++;
        LOGGER.info("Certified template-listed natural source {} at {} ({} certified this session).", entry.block, entry.key(), certifiedCount);
        return true;
    }

    public static String key(int dimension, int x, int y, int z) { return dimension + ":" + x + ":" + y + ":" + z; }

    public static final class Entry {
        public final int dimension, x, y, z;
        public final String block, tileEntity;
        Entry(int dimension, int x, int y, int z, String block, String tileEntity) {
            this.dimension=dimension; this.x=x; this.y=y; this.z=z; this.block=block; this.tileEntity=tileEntity;
        }
        public String key() { return NaturalHazardCertification.key(dimension,x,y,z); }
        boolean isSupported() {
            return (("hbm:tile.geysir_chlorine".equals(block) || "hbm:tile.geysir_nether".equals(block)) && "tileentity_geysir".equals(tileEntity))
                    || (("hbm:tile.volcano_core".equals(block) || "hbm:tile.volcano_rad_core".equals(block)) && "tileentity_volcano_core".equals(tileEntity));
        }
        public boolean matches(TileEntity tile, String registryName) {
            return block.equals(registryName) && ("tileentity_geysir".equals(tileEntity) ? tile instanceof TileEntityGeysir : tile instanceof TileEntityVolcanoCore);
        }
    }
}
