#!/usr/bin/env python3
"""Compile real altitude/rocket sources against minimal Minecraft test doubles.

Requires Python 3 and a JDK (JAVA_HOME or javac/java on PATH). This checks pure
height and rocket lifecycle behavior; it does not replace a Forge server test.
"""
from pathlib import Path
import subprocess,tempfile,os,shutil
root=Path(__file__).resolve().parents[1]
java=Path(os.environ['JAVA_HOME'])/'bin' if 'JAVA_HOME' in os.environ else Path(shutil.which('javac') or 'javac').parent
stubs={
'net/minecraft/world/World.java':'''package net.minecraft.world; public class World { public boolean isRemote; private int height; public World(int h, boolean client) {height=h;isRemote=client;} public int getHeight(){return height;} }''',
'net/minecraft/entity/Entity.java':'''package net.minecraft.entity; import net.minecraft.world.World; import net.minecraft.nbt.NBTTagCompound; public abstract class Entity {public World worldObj; public boolean ignoreFrustumCheck,isDead; public double motionX,motionY,motionZ,posX,posY,posZ,prevPosX,prevPosY,prevPosZ,lastTickPosX,lastTickPosY,lastTickPosZ; public Entity(World w){worldObj=w;} public void onUpdate(){} protected abstract void entityInit(); public void setPosition(double x,double y,double z){posX=x;posY=y;posZ=z;} public void setDead(){isDead=true;} public abstract void readEntityFromNBT(NBTTagCompound n); public abstract void writeEntityToNBT(NBTTagCompound n); public boolean isInRangeToRenderDist(double d){return false;} }''',
'net/minecraft/item/ItemStack.java':'''package net.minecraft.item; import net.minecraft.nbt.NBTTagCompound; public class ItemStack { public Object getItem(){return null;} public static ItemStack loadItemStackFromNBT(NBTTagCompound n){return new ItemStack();} public void writeToNBT(NBTTagCompound n){} }''',
'net/minecraft/nbt/NBTTagCompound.java':'''package net.minecraft.nbt; import java.util.*; public class NBTTagCompound {Map<String,Object> data=new HashMap<>(); public NBTTagList getTagList(String s,int t){return (NBTTagList)data.get(s);} public byte getByte(String s){return (Byte)data.get(s);} public void setByte(String s,byte b){data.put(s,b);} public void setTag(String s,NBTTagList l){data.put(s,l);} }''',
'net/minecraft/nbt/NBTTagList.java':'''package net.minecraft.nbt; import java.util.*; public class NBTTagList {List<NBTTagCompound> tags=new ArrayList<>(); public int tagCount(){return tags.size();} public NBTTagCompound getCompoundTagAt(int i){return tags.get(i);} public void appendTag(NBTTagCompound n){tags.add(n);} }''',
'cpw/mods/fml/relauncher/Side.java':'''package cpw.mods.fml.relauncher; public enum Side {CLIENT,SERVER}''',
'cpw/mods/fml/relauncher/SideOnly.java':'''package cpw.mods.fml.relauncher; public @interface SideOnly {Side value();}''',
'com/hbm/items/ISatChip.java':'''package com.hbm.items; import net.minecraft.item.ItemStack; public interface ISatChip {static int getFreqS(ItemStack s){return 0;} }''',
'com/hbm/saveddata/satellites/XSatelliteRegistry.java':'''package com.hbm.saveddata.satellites; import net.minecraft.world.World; import net.minecraft.item.ItemStack; public class XSatelliteRegistry {public static void orbit(World w,ItemStack s,int f,double x,double y,double z){} }''',
'SpaceflightCheck.java':'''import com.hbm.dim.CelestialAltitudeCompat; import com.hbm.entity.missile.EntityRocketBase; import net.minecraft.world.World; import net.minecraft.item.ItemStack; import net.minecraft.nbt.NBTTagCompound; import java.util.Arrays;
public class SpaceflightCheck {
 static int checks; static void check(boolean ok,String name){checks++;if(!ok)throw new AssertionError(name);}
 static class Rocket extends EntityRocketBase {int deployments; Rocket(World w){super(w,18);} protected void deployPayload(){deployments++;} ItemStack item(int i){return payload[i];}}
 public static void main(String[] args){
  check(CelestialAltitudeCompat.heightOffset(null)==0,"null world");
  for(int height:new int[]{128,256,384,512}){World w=new World(height,false);int offset=Math.max(0,height-256);
   check(CelestialAltitudeCompat.heightOffset(w)==offset,"height offset");
   check(CelestialAltitudeCompat.departureY(w)==900+offset,"departure");
   check(CelestialAltitudeCompat.reentryStartY(w)==800+offset,"reentry");
   check(CelestialAltitudeCompat.soyuzDeploymentY(w)==600+offset,"deployment");
   check(CelestialAltitudeCompat.reentryHeatStartY(w)==300+offset,"heat start");
   check(CelestialAltitudeCompat.reentryHeatEndY(w)==1000+offset,"heat end");
   check(CelestialAltitudeCompat.atmosphereFadeStartY(w)==600+offset,"fog start");
   check(CelestialAltitudeCompat.atmosphereFadeEndY(w)==1000+offset,"fog end");
   check(CelestialAltitudeCompat.meteorEndY(w)==500+offset,"meteor end");
   check(CelestialAltitudeCompat.cargoPodY(w,64)==300+offset,"cargo low pad");
   check(CelestialAltitudeCompat.cargoPodY(w,height+200)==height+232,"cargo high pad");
   Rocket r=new Rocket(w);r.motionY=2;r.setPosition(0,598+offset,0);r.onUpdate();
   check(r.deployments==0&&!r.isDead,"no early deployment at threshold");r.onUpdate();
   check(r.deployments==1&&r.isDead,"deploy and retire above threshold");
   Rocket client=new Rocket(new World(height,true));client.setPosition(0,10000,0);client.onUpdate();
   check(client.deployments==0&&!client.isDead,"no client payload deployment");
   Rocket original=new Rocket(w);original.setPayload(Arrays.asList(new ItemStack(),null,new ItemStack()));
   NBTTagCompound tag=new NBTTagCompound();original.writeEntityToNBT(tag);Rocket restored=new Rocket(w);restored.readEntityFromNBT(tag);
   check(restored.item(0)!=null&&restored.item(1)==null&&restored.item(2)!=null,"legacy payload NBT slots survive shared base");
  }
  System.out.println("PASS: "+checks+" altitude, cargo clearance, server/client deployment and payload persistence assertions");
 }
}'''}
with tempfile.TemporaryDirectory(prefix='hbm-spaceflight-') as tmp:
 p=Path(tmp)
 (p/'classes').mkdir()
 for name,content in stubs.items():
  f=p/name;f.parent.mkdir(parents=True,exist_ok=True);f.write_text(content)
 sources=[str(f) for f in p.rglob('*.java')]+[str(root/'src/main/java/com/hbm/dim/CelestialAltitudeCompat.java'),str(root/'src/main/java/com/hbm/entity/missile/EntityRocketBase.java')]
 subprocess.run([str(java/'javac'),'-d',str(p/'classes'),*sources],check=True)
 subprocess.run([str(java/'java'),'-cp',str(p/'classes'),'SpaceflightCheck'],check=True)
