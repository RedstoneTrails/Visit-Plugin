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
		
		// Core plugin settings
		Settings.safeTeleport = config.getBoolean("Safe-Teleport", true);
		Settings.centerUpDown = config.getBoolean("Center-Up-Down", true);
		Settings.cardinalRounding = config.getInt("Cardinal-Rounding", 45);
		if (Settings.cardinalRounding < 0)
			Settings.cardinalRounding = 0;
		//Settings.clickChatToVisit = config.getBoolean("Click-Chat-To-Visit", true);
		//Settings.instantVisitOnClick = config.getBoolean("Instant-Visit-On-Click", true);
		Settings.messagesDefaultOn = config.getBoolean("Messages-Default-On", true);
		Settings.maxPlayersPerPage = config.getInt("Max-Players-Per-Page", 45);
		// Additional options for visits
		Settings.customCommands = config.getStringList("Custom-Set-Visit-Commands");
		Settings.allowAdditionalParameters = config.getBoolean("Allow-Additional-Parameters", false);
		Settings.sendMessageOnCustomCommand = config.getBoolean("Send-Message-On-Custom-Command", true);
		Settings.blockSetVistCommand = config.getBoolean("Block-Set-Visit-Command", false);
	}
}
