package com.skyblockedmc.visit;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

public class Messages {
	private Visit plugin;

	private File messagesFile = new File(plugin.getDataFolder(), "messages.yml");
	private static FileConfiguration config;
	
	public static final String PLAYER_LABEL = "{player}";
	public static final String COMMAND_LABEL = "visitcfg";
	public static final String LOCATION_CONFIG = "Location";
	public static final String MESSAGES_CONFIG = "Wants Messages";

	public Messages(Visit plugin) {
		this.plugin = plugin;
		load();
	}

	public void load() {
		createConfig();
		config = YamlConfiguration.loadConfiguration(messagesFile);
		addMessageDefaults();
	}

	private int addMessage(String node, String message) {
		if (config.get(node) == null) {
			config.set(node, message);
			return 1;
		}

		return 0;
	}

	private void addMessageDefaults() {
		int toSave = 0;

		toSave += addMessage("set-visit", "&aSet your visit location to {X} {Y} {Z}.");
		toSave += addMessage("del-visit", "&aYour visit location has been deleted.");
		toSave += addMessage("visit", "&aVisiting " + PLAYER_LABEL + "...");
		toSave += addMessage("visited-you", "&a" + PLAYER_LABEL + " visited you.");
		toSave += addMessage("messages-on", "&aVisit notifications have been turned on.");
		toSave += addMessage("messages-off", "&aVisit notifications have been turned off.");
		toSave += addMessage("players-title", "&aPlayers");
		toSave += addMessage("usage.set-visit", "&aUse &e/setvisit &ato set your visit to current location.");
		toSave += addMessage("usage.del-visit", "&aUse &e/delvisit &ato remove your visit location.");
		toSave += addMessage("usage.visit", "&aUse &e/visit " + PLAYER_LABEL + " &ato visit a player.");
		toSave += addMessage("usage.message-toggle", "&aUse &e " + COMMAND_LABEL + " messages on/off &ato enable/disable notifications when someone visits you.");
		toSave += addMessage("error.player-not-found", "&4Player &e" + PLAYER_LABEL + " &4was not found.");
		toSave += addMessage("error.visit-not-set", "&4Player &e" + PLAYER_LABEL + " &4does not have a visit location.");
		toSave += addMessage("error.no-visit-locations", "&4No visit locations have been set.");
		toSave += addMessage("error.no-visit-location", "&4You do not have a visit location set.");
		toSave += addMessage("error.unsafe-location", "&4Location is not safe.");
		toSave += addMessage("error.invalid-location", "&4You cannot set your visit location in this world.");
		toSave += addMessage("error.no-home-to-delete", "&4No visit location was previously set.");
		toSave += addMessage("error.messages-already-on", "&4Visit notificaitons are already on.");
		toSave += addMessage("error.messages-already-off", "&4Visit notificaitons are already off.");
		toSave += addMessage("admin.set-visit", "&aSet " + PLAYER_LABEL + "'s visit location to {X} {Y} {Z}.");
		toSave += addMessage("admin.del-visit", "&aSuccessfuly deleted " + PLAYER_LABEL + "'s visit location.");

		if (toSave > 0)
			saveConfig();
	}

	private void createConfig() {
		try {
			if (!plugin.getDataFolder().isDirectory()) {
				plugin.getDataFolder().mkdirs();
			}
			if (!messagesFile.exists()) {
				messagesFile.createNewFile();
			}
		} catch (IOException e) {
			plugin.getLogger().log(Level.SEVERE, "Could not create config file! Disabling plugin!", e);
			plugin.getServer().getPluginManager().disablePlugin(plugin);
		}
	}

	private void saveConfig() {
		try {
			config.save(messagesFile);
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	public static String colorize(String str) {
		return ChatColor.translateAlternateColorCodes('&', str);
	}

	public static enum Message {
		SET_VISIT, DEL_VISIT, VISIT, VISITED_YOU, MESSAGES_ON, MESSAGES_OFF, PLAYERS_TITLE;

		@Override
		public String toString() {
			return colorize(config.getString(name().toLowerCase().replace("_", "-")));
		}
	}

	public static enum UsageMessage {
		SET_VISIT, DEL_VISIT, VISIT, MESSAGE_TOGGLE;

		@Override
		public String toString() {
			return colorize("usage." + config.getString(name().toLowerCase().replace("_", "-")));
		}
	}

	public static enum ErrorMessage {
		PLAYER_NOT_FOUND, VISIT_NOT_SET, NO_VISIT_LOCATIONS, NO_VISIT_LOCATION, UNSAFE_LOCATION, INVALID_LOCATION, NO_HOME_TO_DELETE, MESSAGES_ALREADY_ON, MESSAGES_ALREADY_OFF;

		@Override
		public String toString() {
			return colorize("error." + config.getString(name().toLowerCase().replace("_", "-")));
		}
	}

	public static enum AdminMessage {
		SET_VISIT, DEL_VISIT;

		@Override
		public String toString() {
			return colorize(config.getString("admin." + name().toLowerCase().replace("_", "-")));
		}
	}
}
