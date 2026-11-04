import java.lang.reflect.Field;
import java.util.*;
import com.hbm.entity.particle.*;
import com.hbm.explosion.ExplosionChaos;
import com.hbm.render.util.RenderAccessoryUtility;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture.Type;
import net.minecraft.block.Block;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.util.*;
import net.minecraft.world.*;
import net.minecraft.world.chunk.IChunkProvider;
import sun.misc.Unsafe;

/** Small in-memory worlds exercise compiled game code without a Minecraft client. */
public class EnvironmentalChecks {
    static class TestBlock extends Block {
        TestBlock(net.minecraft.block.material.Material material) { super(material); }
    }
    static void setBlock(String name, Block block) throws Exception {
        Class.forName(Blocks.class.getName(), true, Blocks.class.getClassLoader());
        Field field = Blocks.class.getField(name);
        unsafe.putObject(unsafe.staticFieldBase(field), unsafe.staticFieldOffset(field), block);
    }
    static int checks;
    static Unsafe unsafe;
    static void check(boolean ok, String message) {
        if(!ok) throw new AssertionError(message);
        checks++;
    }
    public static class TestWorld extends World {
        Block terrain;
        public int getBlockMetadata(int x,int y,int z) { return 0; }
        public void markTileEntityChunkModified(int x,int y,int z,net.minecraft.tileentity.TileEntity tile) { }
        public void func_147453_f(int x,int y,int z,Block block) { }
        net.minecraft.tileentity.TileEntity sourceTile;
        public net.minecraft.tileentity.TileEntity getTileEntity(int x,int y,int z) { return sourceTile; }
        int reads, writes, queries;
        boolean loaded = true;
        Set<String> changed = new HashSet<>();
        TestWorld() { super(null, "test", (WorldProvider)null, null, null); }
        protected IChunkProvider createChunkProvider() { return null; }
        protected int func_152379_p() { return 0; }
        public Entity getEntityByID(int id) { return null; }
        public boolean blockExists(int x, int y, int z) { return loaded; }
        public Block getBlock(int x, int y, int z) { reads++; return terrain; }
        public boolean setBlock(int x, int y, int z, Block b, int meta, int flags) {
            writes++; changed.add(x+","+z); return true;
        }
        public List getEntitiesWithinAABB(Class type, AxisAlignedBB box) {
            queries++; return Collections.emptyList();
        }
        public boolean isRaining() { return false; }
    }
    public static class TestRegistry extends net.minecraft.util.RegistryNamespaced {
        String name = "hbm:tile.geysir_chlorine";
        @Override public String getNameForObject(Object value) { return name; }
    }
    public static class Player extends AbstractClientPlayer {
        UUID id;
        Player() { super(null, new GameProfile(UUID.randomUUID(), "test")); }
        public UUID getUniqueID() { return id; }
        public String getDisplayName() { return "test"; }
        public void addChatMessage(IChatComponent message) { }
        public boolean canCommandSenderUseCommand(int level, String command) { return false; }
        public ChunkCoordinates getPlayerCoordinates() { return new ChunkCoordinates(0, 0, 0); }
    }
    public static class Shrapnel extends com.hbm.entity.projectile.EntityShrapnel {
        Shrapnel() { super((World)null); }
        public void impact(MovingObjectPosition hit) { onImpact(hit); }
    }
    static TestWorld world(boolean client) throws Exception {
        TestWorld w = (TestWorld)unsafe.allocateInstance(TestWorld.class);
        w.isRemote = client; w.loaded = true; w.terrain = Blocks.grass;
        w.changed = new HashSet<>(); return w;
    }
    static <T extends EntityModFX> T particle(Class<T> type, TestWorld w, boolean poison) throws Exception {
        T p = (T)unsafe.allocateInstance(type);
        p.worldObj = w; p.posX = -0.25; p.posY = 64.5; p.posZ = -16.25;
        p.maxAge = 1200;
        Field rand = Entity.class.getDeclaredField("rand"); rand.setAccessible(true);
        rand.set(p, new Random() { public int nextInt(int bound) { return poison ? 0 : bound-1; } });
        return p;
    }
    public static void main(String[] args) throws Exception {
        Field f = Unsafe.class.getDeclaredField("theUnsafe"); f.setAccessible(true); unsafe = (Unsafe)f.get(null);
        // Supply only the terrain identities needed here; full Forge bootstrap needs a game launcher.
        setBlock("grass", new TestBlock(net.minecraft.block.material.Material.grass));
        setBlock("dirt", new TestBlock(net.minecraft.block.material.Material.ground));
        TestWorld client = world(true);
        EntityOrangeFX orange = particle(EntityOrangeFX.class, client, true);
        orange.onUpdate();
        check(orange.isDead && client.writes == 0 && client.queries == 0, "client orange never queries WGCore or mutates terrain/health");
        EntityChlorineFX chlorine = particle(EntityChlorineFX.class, client, true);
        chlorine.onUpdate();
        check(client.queries == 0, "client chlorine never queries damage targets");
        ExplosionChaos.poison(client, 0, 0, 0, 2);
        ExplosionChaos.pc(client, 0, 0, 0, 2);
        ExplosionChaos.c(client, 0, 0, 0, 2);
        check(client.queries == 0, "shared gas damage routines reject client calls");
        TestWorld server = world(false);
        api.hbm.wgc.Integrations.reset();
        orange = particle(EntityOrangeFX.class, server, false); orange.onUpdate();
        check(server.writes == 27, "server orange contaminates one impact only");
        check(server.changed.equals(api.hbm.wgc.Integrations.checked), "negative-coordinate permission checks match every changed column");
        check(server.changed.contains("-2,-18") && !server.changed.contains("1,-15"), "impact coordinates use floor rather than truncation");
        server = world(false); api.hbm.wgc.Integrations.allow = false;
        particle(EntityOrangeFX.class, server, false).onUpdate();
        check(server.writes == 0, "denied contamination leaves terrain alone");
        server = world(false); server.loaded = false;
        orange = particle(EntityOrangeFX.class, server, false); orange.onUpdate();
        check(orange.isDead && server.reads == 0, "unloaded impact never loads terrain");
        for(Class<? extends EntityModFX> type : Arrays.asList(EntityOrangeFX.class, EntityChlorineFX.class, EntityCloudFX.class, EntityPinkCloudFX.class)) {
            server = world(false); EntityModFX p = particle(type, server, true); p.particleAge = p.maxAge;
            p.onUpdate(); check(p.isDead && server.reads == 0 && server.queries == 0, "expired "+type.getSimpleName()+" stops moving");
        }
        Shrapnel shrapnel = (Shrapnel)unsafe.allocateInstance(Shrapnel.class);
        shrapnel.worldObj = client;
        shrapnel.impact(new MovingObjectPosition((Entity)unsafe.allocateInstance(Player.class)));
        check(!shrapnel.isDead, "client shrapnel ignores player impact without permission lookups");
        shrapnel.worldObj = world(false); shrapnel.ticksExisted = 6;
        shrapnel.impact(new MovingObjectPosition(0, 64, 0, 0, Vec3.createVectorHelper(0, 64, 0)));
        check(shrapnel.isDead, "protected terrain consumes shrapnel");
        for(Class<? extends com.hbm.blocks.gas.BlockGasBase> gas : Arrays.asList(
                com.hbm.blocks.gas.BlockGasClorine.class, com.hbm.blocks.gas.BlockGasMonoxide.class,
                com.hbm.blocks.gas.BlockGasRadon.class, com.hbm.blocks.gas.BlockGasRadonDense.class,
                com.hbm.blocks.gas.BlockGasRadonTomb.class, com.hbm.blocks.gas.BlockGasMeltdown.class,
                com.hbm.blocks.gas.BlockGasCoal.class, com.hbm.blocks.gas.BlockGasAsbestos.class)) {
            com.hbm.blocks.gas.BlockGasBase block = (com.hbm.blocks.gas.BlockGasBase)unsafe.allocateInstance(gas);
            block.onEntityCollidedWithBlock(client, 0, 64, 0, (Entity)unsafe.allocateInstance(Player.class));
            check(true, "client "+gas.getSimpleName()+" ignores exposure and filter wear");
        }
        server = world(false);
        Field provider = World.class.getDeclaredField("provider"); provider.setAccessible(true);
        provider.set(server,new net.minecraft.world.WorldProviderSurface());
        net.minecraft.nbt.NBTTagCompound originTag = new net.minecraft.nbt.NBTTagCompound();
        originTag.setInteger("dimension",0); originTag.setInteger("x",5); originTag.setInteger("y",64); originTag.setInteger("z",5);
        com.hbm.util.NaturalHazardSource source = com.hbm.util.NaturalHazardSource.read(originTag);
        com.hbm.tileentity.deco.TileEntityGeysir geyser = new com.hbm.tileentity.deco.TileEntityGeysir();
        geyser.setNaturalOrigin(source); geyser.setWorldObj(server); geyser.xCoord=5; geyser.yCoord=64; geyser.zCoord=5;
        server.sourceTile = geyser;
        api.hbm.wgc.Integrations.allow=false; api.hbm.wgc.Integrations.naturalAllow=true;
        orange = particle(EntityOrangeFX.class,server,false); orange.naturalSource=source; orange.onUpdate();
        check(server.writes==27,"verified natural source uses environmental policy without needing an owner");
        server.writes=0; api.hbm.wgc.Integrations.naturalAllow=false;
        orange = particle(EntityOrangeFX.class,server,false); orange.naturalSource=source; orange.onUpdate();
        check(server.writes==0,"protected destination denies natural contamination");
        api.hbm.wgc.Integrations.naturalAllow=true; geyser.xCoord++;
        orange = particle(EntityOrangeFX.class,server,false); orange.naturalSource=source; orange.onUpdate();
        check(server.writes==0,"moved source NBT cannot grant environmental permission");
        geyser.xCoord--;
        orange = particle(EntityOrangeFX.class,server,false); orange.naturalSource=source; orange.owner=UUID.randomUUID(); orange.onUpdate();
        check(server.writes==0,"owned effects cannot borrow a natural-source exception");
        check(com.hbm.util.NaturalHazardSource.read(source.write()).isValid(server),"source provenance round-trips through NBT");
        java.nio.file.Path manifestFile = java.nio.file.Files.createTempFile("hbm-natural-sources", ".json");
        String manifest = "{\"schema_version\":1,\"sources\":[{\"dimension\":0,\"x\":5,\"y\":64,\"z\":5,\"block\":\"hbm:tile.geysir_chlorine\",\"tile_entity\":\"tileentity_geysir\"}]}";
        Field registryField = Block.class.getDeclaredField("blockRegistry");
        Object registryBase = unsafe.staticFieldBase(registryField); long registryOffset = unsafe.staticFieldOffset(registryField);
        Object originalRegistry = unsafe.getObject(registryBase,registryOffset);
        TestRegistry testRegistry = new TestRegistry();
        try {
            java.nio.file.Files.write(manifestFile,manifest.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            com.hbm.util.NaturalHazardCertification.configure(manifestFile.toFile());
            check(com.hbm.util.NaturalHazardCertification.parse(new java.io.StringReader(manifest)).size()==1,"drop-in source list parses exact positions");
            unsafe.putObject(registryBase,registryOffset,testRegistry);
            com.hbm.tileentity.deco.TileEntityGeysir listed = new com.hbm.tileentity.deco.TileEntityGeysir();
            listed.setWorldObj(server); listed.xCoord=5; listed.yCoord=64; listed.zCoord=5; server.sourceTile=listed;
            check(listed.getNaturalOrigin()!=null,"listed source is certified automatically before server effects");
            int reads = server.reads; listed.getNaturalOrigin();
            check(server.reads==reads,"automatic certification is attempted once per loaded tile");
            listed.setNaturalOrigin(null);
            check(listed.getNaturalOrigin()==null,"explicit clear is not immediately undone by the source list");
            net.minecraft.tileentity.TileEntity.addMapping(com.hbm.tileentity.deco.TileEntityGeysir.class,"hbm_environmental_test_geyser");
            net.minecraft.nbt.NBTTagCompound savedOptOut = new net.minecraft.nbt.NBTTagCompound();
            savedOptOut.setTag("NaturalOrigin",source.write());
            listed.writeToNBT(savedOptOut);
            check(savedOptOut.getBoolean("NaturalOriginDisabled") && !savedOptOut.hasKey("NaturalOrigin"),"clear persists opt-out and removes stale origin NBT");
            com.hbm.tileentity.deco.TileEntityGeysir savedCopy = new com.hbm.tileentity.deco.TileEntityGeysir();
            savedCopy.readFromNBT(savedOptOut); savedCopy.setWorldObj(server); server.sourceTile=savedCopy;
            check(savedCopy.getNaturalOrigin()==null,"real tile save/load preserves automatic-certification opt-out");
            net.minecraft.nbt.NBTTagCompound disabled = new net.minecraft.nbt.NBTTagCompound();
            disabled.setInteger("x",5); disabled.setInteger("y",64); disabled.setInteger("z",5); disabled.setBoolean("NaturalOriginDisabled",true);
            listed.readFromNBT(disabled);
            check(listed.getNaturalOrigin()==null,"saved opt-out prevents certification after reload");
            com.hbm.tileentity.deco.TileEntityGeysir unlisted = new com.hbm.tileentity.deco.TileEntityGeysir();
            unlisted.setWorldObj(server); unlisted.xCoord=6; unlisted.yCoord=64; unlisted.zCoord=5;
            check(unlisted.getNaturalOrigin()==null,"unlisted coordinates remain uncertified");
            testRegistry.name="minecraft:stone";
            com.hbm.tileentity.deco.TileEntityGeysir replaced = new com.hbm.tileentity.deco.TileEntityGeysir();
            replaced.setWorldObj(server); replaced.xCoord=5; replaced.yCoord=64; replaced.zCoord=5;
            check(replaced.getNaturalOrigin()==null,"changed live block cannot use a matching coordinate alone");
            try {
                com.hbm.util.NaturalHazardCertification.parse(new java.io.StringReader(manifest.replace("\"dimension\":0","\"dimension\":0.5")));
                throw new AssertionError("fractional dimension accepted");
            } catch(IllegalArgumentException expected) { check(true,"fractional source coordinates are rejected"); }
            String duplicate = manifest.replace("}]}","},"+manifest.substring(manifest.indexOf("[{" )+1,manifest.length()-2)+"]}");
            try {
                com.hbm.util.NaturalHazardCertification.parse(new java.io.StringReader(duplicate));
                throw new AssertionError("duplicate source accepted");
            } catch(IllegalArgumentException expected) { check(true,"duplicate source positions are rejected"); }
        } finally {
            unsafe.putObject(registryBase,registryOffset,originalRegistry);
            java.nio.file.Files.delete(manifestFile);
            com.hbm.util.NaturalHazardCertification.configure(manifestFile.toFile());
        }
        Player player = (Player)unsafe.allocateInstance(Player.class);
        player.id = UUID.fromString(com.hbm.util.ShadyUtil.Dafnik);
        ResourceLocation original = new ResourceLocation("test:original");
        player.func_152121_a(Type.CAPE, original);
        RenderAccessoryUtility.beginCapeRender(player, true);
        check(!original.equals(player.getLocationCape()), "eligible NTM cape applied during render");
        RenderAccessoryUtility.endCapeRender(player);
        check(original.equals(player.getLocationCape()), "original cape restored after render");
        RenderAccessoryUtility.beginCapeRender(player, true);
        RenderAccessoryUtility.beginCapeRender(player, false);
        check(original.equals(player.getLocationCape()), "disabled render clears pending override even when previous post was skipped");
        RenderAccessoryUtility.beginCapeRender(player, true);
        ResourceLocation downloaded = new ResourceLocation("test:downloaded");
        player.func_152121_a(Type.CAPE, downloaded);
        RenderAccessoryUtility.endCapeRender(player);
        check(downloaded.equals(player.getLocationCape()), "new texture supplied by another mod is preserved");
        player.func_152121_a(Type.CAPE, null);
        RenderAccessoryUtility.beginCapeRender(player, true); RenderAccessoryUtility.endCapeRender(player);
        check(player.getLocationCape() == null, "player without original cape does not retain NTM override");
        if(args.length > 0) {
            try(java.io.Reader reader = java.nio.file.Files.newBufferedReader(java.nio.file.Paths.get(args[0]),java.nio.charset.StandardCharsets.UTF_8)) {
                check(com.hbm.util.NaturalHazardCertification.parse(reader).size()==22,"actual Earth40 config loads all 22 template-verified sources");
            }
        }
        System.out.println("PASS: "+checks+" environmental/cape assertions");
    }
}
