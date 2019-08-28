package com.reddevtrails.visit;

import org.bukkit.plugin.java.JavaPlugin;

import com.reddevtrails.visit.commands.VisitCommands;
import com.reddevtrails.visit.listeners.ChatListener;
import com.reddevtrails.visit.panels.PlayerPanel;
import com.reddevtrails.visit.utils.HeadGetter;

public class Visit extends JavaPlugin {
	private static Visit plugin;
	
	private Players players;
	private Messages messages;
	private PlayerPanel playerPanel;
	private HeadGetter headGetter;
	private ChatListener chatListener;
	private VisitCommands visitCommands;
	
	@Override
	public void onEnable() {
		plugin = this;
		players = new Players(this);
		messages = new Messages(this);
		playerPanel = new PlayerPanel(this);
		headGetter = new HeadGetter(this);
		chatListener = new ChatListener(this);
		registerCommands();
		registerEvents();
		
		saveDefaultConfig();
		Config.load(this);
	}
	
	@Override
	public void onDisable() {
		players.save();
	}
	
	public void reload() {
		messages.load();
		reloadConfig();
		Config.load(this);
		playerPanel = new PlayerPanel(this);
	}
	
	public void registerCommands() {
		visitCommands = new VisitCommands(this);
		getCommand("setvisit").setExecutor(visitCommands);
		getCommand("setvisit").setTabCompleter(visitCommands);
		getCommand("delvisit").setExecutor(visitCommands);
		getCommand("delvisit").setTabCompleter(visitCommands);
		getCommand("visit").setExecutor(visitCommands);
		getCommand("visit").setTabCompleter(visitCommands);
		getCommand("players").setExecutor(visitCommands);
		getCommand("players").setTabCompleter(visitCommands);
		getCommand("visitcfg").setExecutor(visitCommands);
		getCommand("visitcfg").setTabCompleter(visitCommands);
	}
	
	public void registerEvents() {
		getServer().getPluginManager().registerEvents(playerPanel, this);
		getServer().getPluginManager().registerEvents(chatListener, this);
	}
	
	public static Visit plugin() {
		return plugin;
	}
	
	public Players players() {
		return players;
	}
	
	public Messages messages() {
		return messages;
	}
	
	public PlayerPanel playerPanel() {
		return playerPanel;
	}
	
	public HeadGetter headGetter() {
		return headGetter;
	}
	
	public VisitCommands visitCommands() {
		return visitCommands;
	}
}
