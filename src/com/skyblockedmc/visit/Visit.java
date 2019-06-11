package com.skyblockedmc.visit;

import org.bukkit.plugin.java.JavaPlugin;

public class Visit extends JavaPlugin {
	private Players players;
	private Messages messages;
	
	@Override
	public void onEnable() {
		players = new Players(this);
		messages = new Messages(this);
	}
	
	@Override
	public void onDisable() {
		players.save();
	}
	
	public void reload() {
		messages.load();
	}
	
	public Players players() {
		return players;
	}
	
	public Messages messages() {
		return messages;
	}
}
