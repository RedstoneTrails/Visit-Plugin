package com.reddevtrails.visit;

import org.bukkit.configuration.file.FileConfiguration;

public class Config {
	
	public static boolean SAFE_TELEPORT;
	public static boolean CENTER_UP_DOWN;
	public static int CARDINAL_ROUNDING;

	public static void load(Visit plugin) {
		try {
            plugin.getConfig();
        } catch (final Exception e) {
            e.printStackTrace();
            return;
        }
		
		FileConfiguration config = plugin.getConfig();
		
		Settings.safeTeleport = config.getBoolean("Safe-Teleport", true);
		Settings.centerUpDown = config.getBoolean("Center-Up-Down", true);
		Settings.cardinalRounding = config.getInt("Cardinal-Rounding", 45);
		if (Settings.cardinalRounding < 0)
			Settings.cardinalRounding = 0;
		Settings.messagesDefaultOn = config.getBoolean("Messages-Default-On", true);
		Settings.maxPlayersPerPage = config.getInt("Max-Players-Per-Page", 45);
	}
}
