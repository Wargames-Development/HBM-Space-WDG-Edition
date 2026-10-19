import com.hbm.tileentity.TileEntityLoadedBase;
import com.hbm.tileentity.TilePort;
import com.hbm.module.portmanager.ModulePortManPower;
import com.hbm.module.portmanager.ModulePortManFluidAdaptive;
import com.hbm.saveddata.satellites.SatelliteScience;
import com.hbm.util.HitscanUtil;
import net.minecraft.world.World;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.Vec3;
import net.minecraft.util.MovingObjectPosition;
public class IntegrationChecks {
 static int checks; static void check(boolean b,String s){checks++;if(!b)throw new AssertionError(s);}
 static class Port extends TilePort {int releases; public void disableIfPresent(World w){releases++;}}
 static class Connector extends net.minecraft.tileentity.TileEntity implements api.hbm.energymk2.IEnergyConnectorMK2, api.hbm.fluidmk2.IFluidConnectorMK2 {
  public boolean canConnect(net.minecraftforge.common.util.ForgeDirection d){return true;}
  public boolean canConnect(com.hbm.inventory.fluid.FluidType t,net.minecraftforge.common.util.ForgeDirection d){return true;}
 }
 public static void main(String[] args) throws Exception {
  TileEntityLoadedBase tile=new TileEntityLoadedBase();
  ModulePortManPower power=new ModulePortManPower(tile);Port p=new Port();power.powerPorts=new TilePort[]{p};
  ModulePortManFluidAdaptive fluid=new ModulePortManFluidAdaptive(tile);Port in=new Port();Port out=new Port();fluid.inPorts=new TilePort[]{in};fluid.outPorts=new TilePort[]{out};
  tile.onChunkUnload();check(p.releases==1&&in.releases==1&&out.releases==1,"all managers release on unload");
  tile.invalidate();check(p.releases==2&&in.releases==2&&out.releases==2,"all managers release on invalidate");
  TileEntityLoadedBase.destroyPortsOnInvalidate=false;tile.invalidate();check(p.releases==2,"furnace replacement preserves ports");TileEntityLoadedBase.destroyPortsOnInvalidate=true;
  com.hbm.saveddata.satellites.XSatelliteRegistry.registerSatellite(SatelliteScience.class,new net.minecraft.item.Item(),0.2F,0.64F,0.8F);
  SatelliteScience sat=new SatelliteScience();sat.assemblerProgress=0.625;sat.sensorProgress=100;sat.assemblerCount=2;NBTTagCompound nbt=new NBTTagCompound();sat.writeToNBT(nbt);SatelliteScience read=new SatelliteScience();read.readFromNBT(nbt);check(read.assemblerProgress==0.625&&read.sensorProgress==100&&read.assemblerCount==2,"science progress survives save/load");
  MovingObjectPosition hit=new HitscanUtil.CollisionHandlerBullet().handleSpecial(null,0,0,0,Vec3.createVectorHelper(-1,0.5,0.5),Vec3.createVectorHelper(2,0.5,0.5),net.minecraft.init.Blocks.fence,0);check(hit!=null&&Math.abs(hit.hitVec.xCoord-0.375)<1e-8,"fence post collides");
  MovingObjectPosition miss=new HitscanUtil.CollisionHandlerBullet().handleSpecial(null,0,0,0,Vec3.createVectorHelper(-1,0.5,0.2),Vec3.createVectorHelper(2,0.5,0.2),net.minecraft.init.Blocks.fence,0);check(miss==null,"fence gap passes");
  net.minecraft.world.IBlockAccess access=(net.minecraft.world.IBlockAccess)java.lang.reflect.Proxy.newProxyInstance(IntegrationChecks.class.getClassLoader(),new Class[]{net.minecraft.world.IBlockAccess.class},(proxy,method,values)->{if(method.getName().equals("getHeight"))throw new AssertionError("IBlockAccess height must not be invoked on Crucible");if(method.getName().equals("getTileEntity"))return ((Integer)values[1]>=0&&(Integer)values[1]<384)?new Connector():null;return null;});
  check(com.hbm.lib.Library.canConnect(access,0,300,0,net.minecraftforge.common.util.ForgeDirection.UP),"power connects above 255");
  check(com.hbm.lib.Library.canConnectFluid(access,0,383,0,net.minecraftforge.common.util.ForgeDirection.UP,null),"fluid connects below extended roof");
  check(!com.hbm.lib.Library.canConnect(access,0,384,0,net.minecraftforge.common.util.ForgeDirection.UP),"power rejects roof boundary");
  check(!com.hbm.lib.Library.canConnectFluid(access,0,-1,0,net.minecraftforge.common.util.ForgeDirection.UP,null),"fluid rejects below world");
  java.lang.reflect.Method readConfig=com.hbm.config.ServerConfig.class.getDeclaredMethod("readConfig",java.io.File.class);readConfig.setAccessible(true);
  java.lang.reflect.Method defaults=com.hbm.config.ServerConfig.class.getDeclaredMethod("initDefaults");defaults.setAccessible(true);defaults.invoke(null);
  java.nio.file.Path config=java.nio.file.Files.createTempFile("hbm-legacy-config", ".json");
  try {
   java.nio.file.Files.write(config,"{\"TAINT_TRAILS\":true}".getBytes(java.nio.charset.StandardCharsets.UTF_8));
   com.hbm.config.ServerConfig.ENTITY_TAINT_TRAILS.set(false);readConfig.invoke(null,config.toFile());
   check(com.hbm.config.ServerConfig.ENTITY_TAINT_TRAILS.get(),"legacy taint config survives rename");
   java.nio.file.Files.write(config,"{\"TAINT_TRAILS\":true,\"ENTITY_TAINT_TRAILS\":false}".getBytes(java.nio.charset.StandardCharsets.UTF_8));
   readConfig.invoke(null,config.toFile());check(!com.hbm.config.ServerConfig.ENTITY_TAINT_TRAILS.get(),"canonical config overrides alias");
  } finally {java.nio.file.Files.delete(config);}
  Class<?> fluidType=com.hbm.inventory.fluid.FluidType.class;
  check(api.hbm.fluidmk2.IFluidUserMK2.class.getMethod("getAllTanks").getReturnType()==com.hbm.inventory.fluid.tank.FluidTank[].class,"WGCore tank inspection API");
  check(api.hbm.fluidmk2.IFluidReceiverMK2.class.getMethod("getDemand",fluidType,int.class).getReturnType()==long.class,"WGCore fluid demand API");
  check(api.hbm.fluidmk2.IFluidReceiverMK2.class.getMethod("transferFluid",fluidType,int.class,long.class).getReturnType()==long.class,"WGCore fluid transfer API");
  check(api.hbm.fluidmk2.IFluidReceiverMK2.class.getMethod("getReceivingPressureRange",fluidType).getReturnType()==int[].class,"WGCore pressure API");
  check(api.hbm.fluidmk2.IFluidStandardReceiverMK2.class.getMethod("getReceivingTanks").getReturnType()==com.hbm.inventory.fluid.tank.FluidTank[].class,"WGCore receiver tank API");
  check(com.hbm.inventory.fluid.tank.FluidTank.class.getMethod("setTankType",fluidType)!=null,"WGCore tank type setter API");
  check(com.hbm.inventory.fluid.tank.FluidTank.class.getMethod("getFill").getReturnType()==int.class,"WGCore tank fill API");
  check(com.hbm.inventory.fluid.tank.FluidTank.class.getMethod("getMaxFill").getReturnType()==int.class,"WGCore tank capacity API");
  check(com.hbm.inventory.fluid.tank.FluidTank.class.getMethod("getPressure").getReturnType()==int.class,"WGCore tank pressure API");
  check(com.hbm.blocks.BlockDummyable.class.getMethod("findCore",net.minecraft.world.IBlockAccess.class,int.class,int.class,int.class).getReturnType()==int[].class,"WGCore multiblock core resolution API");
  com.hbm.tileentity.machine.TileEntityTransporterRocket transporter=new com.hbm.tileentity.machine.TileEntityTransporterRocket();
  java.lang.reflect.Field fuelModule=com.hbm.tileentity.machine.TileEntityTransporterBase.class.getDeclaredField("moduleFuelPorts");fuelModule.setAccessible(true);
  ModulePortManFluidAdaptive fuelPorts=(ModulePortManFluidAdaptive)fuelModule.get(transporter);Port fuelPort=new Port();fuelPorts.inPorts=new TilePort[]{fuelPort};
  java.lang.reflect.Field fuelReceiver=com.hbm.tileentity.machine.TileEntityTransporterBase.class.getDeclaredField("fuelReceiver");fuelReceiver.setAccessible(true);
  api.hbm.tile.ILoadedTile delegate=(api.hbm.tile.ILoadedTile)fuelReceiver.get(transporter);
  transporter.onChunkUnload();check(fuelPort.releases==1&&!delegate.isLoaded(),"transporter delegate releases fuel ports and follows unload");
  com.hbm.tileentity.machine.TileEntityTransporterRocket removed=new com.hbm.tileentity.machine.TileEntityTransporterRocket();
  ModulePortManFluidAdaptive removedPorts=(ModulePortManFluidAdaptive)fuelModule.get(removed);Port removedPort=new Port();removedPorts.inPorts=new TilePort[]{removedPort};
  removed.invalidate();check(removedPort.releases==1&&!((api.hbm.tile.ILoadedTile)fuelReceiver.get(removed)).isLoaded(),"transporter delegate releases fuel ports and follows invalidation");
  System.out.println("PASS: "+checks+" integration assertions against compiled Minecraft/HBM classes");
 }
}
