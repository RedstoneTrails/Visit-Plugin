package com.reddevtrails.visit;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

public class Players {
	private Visit plugin;
	private Map<UUID, VPlayer> players;
	
	private File playersFile;
	private FileConfiguration config;
	
	public Players(Visit plugin) {
		this.plugin = plugin;
		// Add to scheduler instead of just outright running
		// to avoid deserializing locations before worlds load
		Bukkit.getScheduler().runTask(plugin, () -> load());

		// TODO: Let users specify save interval?
		// Start timer to auto-save players' locations every 15 minutes
		Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> save(), 18000, 18000);
	}
	
	public Map<UUID, VPlayer> map() {
		return players;
	}
	
	public boolean wantsMessages(UUID uuid) {
		if (players.containsKey(uuid))
			return players.get(uuid).wantsMessages();
		
		return false;
	}
	
	public List<String> names() {
		List<String> returned = new ArrayList<>();
		for (UUID uuid : players.keySet()) 
			returned.add(Bukkit.getOfflinePlayer(uuid).getName());
		
		return returned;
	}
	
	public VPlayer find(UUID uuid) {
		VPlayer player = null;
		if (players.containsKey(uuid))
			player = players.get(uuid);
		
		return player;
	}
	
	public Location findLocation(UUID uuid) {
		Location loc = null;
		if (players.containsKey(uuid))
			loc = players.get(uuid).getLocation();
		
		return loc;
	}
	
	public void add(UUID uuid, Location loc) {
		if (players.containsKey(uuid))
			players.remove(uuid);
		
		VPlayer newPlayer = new VPlayer(loc);
		players.put(uuid, newPlayer);
		
		plugin.playerPanel().addVisit(uuid);
	}
	
	public void remove(UUID uuid) {
		if (players.containsKey(uuid))
			players.remove(uuid);
		
		if (config.contains(uuid.toString()))
			config.set(uuid.toString(), null);

		plugin.playerPanel().updatePanel();
	}
	
	public void save() {
		for (UUID uuid : players.keySet()) {
			Location loc = players.get(uuid).getLocation();
			boolean wantsMessages = players.get(uuid).wantsMessages();
			ConfigurationSection newSection = config.createSection(uuid.toString());
			newSection.set(Messages.MESSAGES_CONFIG, wantsMessages);
			newSection.createSection(Messages.LOCATION_CONFIG, loc.serialize());
		}
		saveConfig();
	}
	
	private void saveConfig() {
		try {
			config.save(playersFile);
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}
	
	private void load() {
		loadConfig();
		players = new HashMap<UUID, VPlayer>();
		for (String uuidString : config.getKeys(false)) {
			UUID uuid;
			try {
				uuid = UUID.fromString(uuidString);
			} catch (IllegalArgumentException ex) {
				uuid = null;
			}
			// Only happens if someone poorly tampers with the save file
			if (uuid == null) {
				plugin.getLogger().severe("Invalid UUID found: " + uuidString + ". Skipping...");
				continue;
			}
			players.put(uuid, new VPlayer(config.getConfigurationSection(uuidString)));
		}
	}
	
	private void loadConfig() {
		createConfig();
		config = YamlConfiguration.loadConfiguration(playersFile);
	}
	
	private void createConfig() {
		playersFile = new File(plugin.getDataFolder(), "players.yml");
		try {
			if (!plugin.getDataFolder().isDirectory()) {
				plugin.getDataFolder().mkdirs();
			}
			if (!playersFile.exists()) {
				playersFile.createNewFile();
			}
		} catch (IOException e) {
			plugin.getLogger().log(Level.SEVERE, "Could not create config file! Disabling plugin!", e);
			plugin.getServer().getPluginManager().disablePlugin(plugin);
		}
	}
}
