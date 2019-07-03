package com.reddevtrails.visit;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

public class Messages {
	private Visit plugin;

	private File messagesFile;
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
	
	
	//TODO: revisit this. Seems you can only add comments writing line by line.
//	private int addComment(String comment) {
//		String node = "# " + comment;
//		if (config.get(node) == null) {
//			config.set(node, "");
//			return 1;
//		}
//		
//		return 0;
//	}

	private void addMessageDefaults() {
		int toSave = 0;
		// Visit GUI messages
		//toSave += addComment("Title for player visit GUI");
		toSave += addMessage("players-title", "&5Players");
		
		//toSave += addComment("Direction tools for multi-page visit GUI");
		toSave += addMessage("players-next", "Next");
		toSave += addMessage("players-previous", "Previous");
		
		toSave += addMessage("players-name-color", "&f"); 
		
		// General messages
		//toSave += addComment("Mesages for general visit creation/deletion and use");
		toSave += addMessage("set-visit", "&aSet your visit location to {X} {Y} {Z}.");
		toSave += addMessage("del-visit", "&aYour visit location has been deleted.");
		toSave += addMessage("visit", "&aVisiting " + PLAYER_LABEL + "...");
		toSave += addMessage("visited-you", "&a" + PLAYER_LABEL + " visited you.");
		toSave += addMessage("messages-on", "&aVisit notifications have been turned on.");
		toSave += addMessage("messages-off", "&aVisit notifications have been turned off.");
		
		// Usage messages
		//toSave += addComment("Messages sent when a player incorrectly uses a command");
		toSave += addMessage("usage.set-visit", "&aUse &e/setvisit &ato set your visit to current location.");
		toSave += addMessage("usage.del-visit", "&aUse &e/delvisit &ato remove your visit location.");
		toSave += addMessage("usage.visit", "&aUse &e/visit " + PLAYER_LABEL + " &ato visit a player.");
		toSave += addMessage("usage.message-toggle", "&aUse &e " + COMMAND_LABEL + " messages on/off &ato enable/disable notifications when someone visits you.");
		
		// Error messages
		//toSave += addComment("Messages sent when there was an issue completing a command operation");
		toSave += addMessage("error.player-not-found", "&4Player &e" + PLAYER_LABEL + " &4was not found.");
		toSave += addMessage("error.visit-not-set", "&4Player &e" + PLAYER_LABEL + " &4does not have a visit location.");
		toSave += addMessage("error.no-visit-locations", "&4No visit locations have been set.");
		toSave += addMessage("error.no-visit-location", "&4You do not have a visit location set.");
		toSave += addMessage("error.unsafe-location", "&4Location is not safe.");
		toSave += addMessage("error.invalid-location", "&4You cannot set your visit location in this world.");
		toSave += addMessage("error.no-home-to-delete", "&4No visit location was previously set.");
		toSave += addMessage("error.messages-already-on", "&4Visit notificaitons are already on.");
		toSave += addMessage("error.messages-already-off", "&4Visit notificaitons are already off.");
		
		// Help messages
		toSave += addMessage("help.header", "&a--- Visit Commands ---");
		toSave += addMessage("help.set-visit", "&6/setvisit&r: Sets your visit to your current location.");
		toSave += addMessage("help.del-visit", "&6/delvisit&r: Deletes your visit location.");
		toSave += addMessage("help.visit", "&6/visit <player>&r: Teleports you to a player's visit location.");
		toSave += addMessage("help.players", "&6/players&r: Shows a GUI of all players you can visit.");
		toSave += addMessage("help.help", "&6/" + COMMAND_LABEL + " help&r: Shows list of commands for this plugin.");
		toSave += addMessage("help.messages", "&6/" + COMMAND_LABEL + " messages on/off&r: Turns on/off your notifications for when someone visits you.");
		// Admin help messages
		toSave += addMessage("help.admin-header", "&a--- Admin Commands ---");
		toSave += addMessage("help.admin-set-visit", "&6/setvisit <player>&r: Sets visit for target player to your current location.");
		toSave += addMessage("help.admin-del-visit", "&6/delvisit <player>&r: Deletes visit location for target player.");
		toSave += addMessage("help.reload", "&6/" + COMMAND_LABEL + " reload&r: Reloads messages and configuration settings.");
		
		// Admin messages
		//toSave += addComment("Messages sent when executing admin commands");
		toSave += addMessage("admin.set-visit", "&aSet " + PLAYER_LABEL + "'s visit location to {X} {Y} {Z}.");
		toSave += addMessage("admin.del-visit", "&aSuccessfuly deleted " + PLAYER_LABEL + "'s visit location.");

		if (toSave > 0)
			saveConfig();
	}

	private void createConfig() {
		messagesFile = new File(plugin.getDataFolder(), "messages.yml");
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
		PLAYERS_TITLE, PLAYERS_NEXT, PLAYERS_PREVIOUS, PLAYERS_NAME_COLOR, SET_VISIT, DEL_VISIT, VISIT, VISITED_YOU, MESSAGES_ON, MESSAGES_OFF;

		@Override
		public String toString() {
			return colorize(config.getString(name().toLowerCase().replace("_", "-")));
		}
	}

	public static enum UsageMessage {
		SET_VISIT, DEL_VISIT, VISIT, MESSAGE_TOGGLE;

		@Override
		public String toString() {
			return colorize(config.getString("usage." + name().toLowerCase().replace("_", "-")));
		}
	}

	public static enum ErrorMessage {
		PLAYER_NOT_FOUND, VISIT_NOT_SET, NO_VISIT_LOCATIONS, NO_VISIT_LOCATION, UNSAFE_LOCATION, INVALID_LOCATION, NO_HOME_TO_DELETE, MESSAGES_ALREADY_ON, MESSAGES_ALREADY_OFF;

		@Override
		public String toString() {
			return colorize(config.getString("error." + name().toLowerCase().replace("_", "-")));
		}
	}

	public static enum HelpMessage {
		HEADER, SET_VISIT, DEL_VISIT, VISIT, PLAYERS, HELP, MESSAGES, ADMIN_HEADER, ADMIN_SET_VISIT, ADMIN_DEL_VISIT, RELOAD;

		@Override
		public String toString() {
			return colorize(config.getString("help." + name().toLowerCase().replace("_", "-")));
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
