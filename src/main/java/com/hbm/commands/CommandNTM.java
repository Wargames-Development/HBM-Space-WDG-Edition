package com.hbm.commands;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import com.hbm.dim.SolarSystemWorldSavedData;
import com.hbm.dim.orbit.OrbitalStation;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.World;

/** Administrative NTM command root for the drive-based station workflow. */
public class CommandNTM extends CommandBase {

	@Override
	public String getCommandName() {
		return "ntm";
	}

	@Override
	public String getCommandUsage(ICommandSender sender) {
		return "/ntm station <list|delete> [station name]";
	}

	@Override
	public void processCommand(ICommandSender sender, String[] args) {
		if(args.length < 2 || !"station".equalsIgnoreCase(args[0])) {
			throw new WrongUsageException(getCommandUsage(sender), new Object[0]);
		}

		World world = sender.getEntityWorld();
		if(world == null) {
			error(sender, "No server world is available for this command.");
			return;
		}

		SolarSystemWorldSavedData data = SolarSystemWorldSavedData.get(world);
		if(data == null) {
			error(sender, "Solar-system saved data is unavailable.");
			return;
		}

		String operation = args[1].toLowerCase(Locale.ROOT);
		if("create".equals(operation) || "raid".equals(operation)) {
			error(sender, "Station and Breach drives are programmed with the Station Drive Terminal.");
			return;
		}
		if("list".equals(operation)) {
			listStations(sender, data);
			return;
		}

		String name = joinName(args, 2);
		if(name.isEmpty()) {
			error(sender, "A station name is required.");
			return;
		}
		if(name.length() > 64) {
			error(sender, "Station names may not exceed 64 characters.");
			return;
		}

		if("delete".equals(operation)) {
			deleteStation(sender, data, name);
		} else {
			throw new WrongUsageException(getCommandUsage(sender), new Object[0]);
		}
	}

	private void listStations(ICommandSender sender, SolarSystemWorldSavedData data) {
		List<OrbitalStation> active = new ArrayList<OrbitalStation>();
		for(OrbitalStation station : data.getStations().values()) {
			if(station != null && station.hasStation && !station.deleting) active.add(station);
		}

		if(active.isEmpty()) {
			error(sender, "No active orbital stations exist.");
			return;
		}

		Collections.sort(active, new Comparator<OrbitalStation>() {
			@Override
			public int compare(OrbitalStation left, OrbitalStation right) {
				return displayName(left).compareToIgnoreCase(displayName(right));
			}
		});

		success(sender, "Active orbital stations:");
		for(OrbitalStation station : active) {
			success(sender, "- " + displayName(station) + " (" + SolarSystemWorldSavedData.getStationId(station) + ")");
		}
	}

	private void deleteStation(ICommandSender sender, SolarSystemWorldSavedData data, String name) {
		OrbitalStation station = resolveUnique(sender, data, name, true);
		if(station == null) return;

		String stationName = displayName(station);
		String stationId = SolarSystemWorldSavedData.getStationId(station);
		if(!station.hasStation) {
			if(!data.cancelStationReservation(station)) {
				error(sender, "The unlaunched station reservation could not be cancelled.");
				return;
			}
			success(sender, "Cancelled the unlaunched station reservation for " + stationName + " (" + stationId + "). Its normal station drives are now empty Hard Drives.");
			return;
		}

		if(!data.deleteActiveStation(station)) {
			error(sender, "The station could not be deleted.");
			return;
		}

		success(sender, "Station deletion queued for " + stationName + " (" + stationId + "). Players will be removed first and the full 64x64 chunk area will be cleared safely.");
	}

	private OrbitalStation resolveUnique(ICommandSender sender, SolarSystemWorldSavedData data, String name, boolean includeReservations) {
		List<OrbitalStation> matches = includeReservations ? data.findStationsByName(name, true) : data.findActiveStationsByName(name);
		if(matches.isEmpty()) {
			error(sender, (includeReservations ? "No active station or unlaunched reservation matches \"" : "No active station matches \"") + name + "\".");
			return null;
		}
		if(matches.size() > 1) {
			StringBuilder ids = new StringBuilder();
			for(OrbitalStation station : matches) {
				if(ids.length() > 0) ids.append(", ");
				ids.append(SolarSystemWorldSavedData.getStationId(station));
			}
			error(sender, "Station name is ambiguous. Matching IDs: " + ids.toString());
			return null;
		}
		return matches.get(0);
	}

	private static String joinName(String[] args, int start) {
		StringBuilder name = new StringBuilder();
		for(int i = start; i < args.length; i++) {
			if(name.length() > 0) name.append(' ');
			name.append(args[i]);
		}
		return name.toString().trim();
	}

	private static String displayName(OrbitalStation station) {
		if(station == null || station.name == null || station.name.trim().isEmpty()) return "<unnamed>";
		return station.name.trim();
	}

	private static void success(ICommandSender sender, String text) {
		ChatComponentText message = new ChatComponentText(text);
		message.getChatStyle().setColor(EnumChatFormatting.GREEN);
		sender.addChatMessage(message);
	}

	private static void error(ICommandSender sender, String text) {
		ChatComponentText message = new ChatComponentText(text);
		message.getChatStyle().setColor(EnumChatFormatting.RED);
		sender.addChatMessage(message);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public List addTabCompletionOptions(ICommandSender sender, String[] args) {
		if(args.length == 1) return getListOfStringsMatchingLastWord(args, "station");
		if(args.length == 2 && "station".equalsIgnoreCase(args[0])) {
			return getListOfStringsMatchingLastWord(args, "list", "delete");
		}
		if(args.length == 3 && "station".equalsIgnoreCase(args[0]) && "delete".equalsIgnoreCase(args[1])) {
			World world = sender.getEntityWorld();
			SolarSystemWorldSavedData data = SolarSystemWorldSavedData.get(world);
			if(data == null) return Collections.emptyList();
			List<String> names = new ArrayList<String>();
			for(OrbitalStation station : data.getStations().values()) {
				if(station != null && !station.deleting && (station.hasStation || station.reservedForLaunch) && station.name != null && !station.name.trim().isEmpty()) names.add(station.name.trim());
			}
			return getListOfStringsMatchingLastWord(args, names.toArray(new String[names.size()]));
		}
		return Collections.emptyList();
	}
}
