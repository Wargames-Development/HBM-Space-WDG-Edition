package com.hbm.commands;

import com.hbm.blocks.bomb.BlockVolcano.TileEntityVolcanoCore;
import com.hbm.tileentity.deco.TileEntityGeysir;
import com.hbm.util.NaturalHazardSource;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentText;
import net.minecraft.world.World;

/** Explicit operator certification for known natural sources in existing/imported terrain. */
public final class CommandNaturalHazard extends CommandBase {
    @Override public String getCommandName() { return "ntmnatural"; }
    @Override public int getRequiredPermissionLevel() { return 4; }
    @Override public String getCommandUsage(ICommandSender sender) { return "/ntmnatural <certify|clear> <x> <y> <z>"; }
    @Override public void processCommand(ICommandSender sender, String[] args) {
        if(args.length != 4 || !("certify".equals(args[0]) || "clear".equals(args[0])))
            throw new WrongUsageException(getCommandUsage(sender));
        World world = sender.getEntityWorld();
        int x = parseInt(sender,args[1]), y = parseInt(sender,args[2]), z = parseInt(sender,args[3]);
        if(world == null || world.isRemote || !world.blockExists(x,y,z)) {
            sender.addChatMessage(new ChatComponentText("Source chunk must already be loaded.")); return;
        }
        TileEntity tile = world.getTileEntity(x,y,z);
        if(!(tile instanceof TileEntityGeysir) && !(tile instanceof TileEntityVolcanoCore)) {
            sender.addChatMessage(new ChatComponentText("Only an existing geyser or volcano core can be certified.")); return;
        }
        if("certify".equals(args[0])) NaturalHazardSource.markGenerated(world,x,y,z);
        else if(tile instanceof TileEntityGeysir) ((TileEntityGeysir)tile).setNaturalOrigin(null);
        else ((TileEntityVolcanoCore)tile).setNaturalOrigin(null);
        sender.addChatMessage(new ChatComponentText("Natural-source provenance " + ("certify".equals(args[0]) ? "certified" : "cleared") + ". WGCore territory restrictions still apply."));
    }
}
