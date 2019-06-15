package com.skyblockedmc.visit;

import org.bukkit.plugin.java.JavaPlugin;

import com.skyblockedmc.visit.commands.VisitCommands;
import com.skyblockedmc.visit.panels.PlayerPanel;
import com.skyblockedmc.visit.utils.HeadGetter;

public class Visit extends JavaPlugin {
	private static Visit plugin;
	
	private Players players;
	private Messages messages;
	private PlayerPanel playerPanel;
	private HeadGetter headGetter;
	
	@Override
	public void onEnable() {
		plugin = this;
		players = new Players(this);
		messages = new Messages(this);
		playerPanel = new PlayerPanel(this);
		headGetter = new HeadGetter(this);
		registerCommands();
		registerEvents();
	}
	
	@Override
	public void onDisable() {
		players.save();
	}
	
	public void reload() {
		messages.load();
	}
	
	public void registerCommands() {
		VisitCommands visitCmds = new VisitCommands(this);
		getCommand("setvisit").setExecutor(visitCmds);
		getCommand("setvisit").setTabCompleter(visitCmds);
		getCommand("delvisit").setExecutor(visitCmds);
		getCommand("delvisit").setTabCompleter(visitCmds);
		getCommand("visit").setExecutor(visitCmds);
		getCommand("visit").setTabCompleter(visitCmds);
		getCommand("players").setExecutor(visitCmds);
		getCommand("players").setTabCompleter(visitCmds);
		getCommand("visitcfg").setExecutor(visitCmds);
		getCommand("visitcfg").setTabCompleter(visitCmds);
	}
	
	public void registerEvents() {
		getServer().getPluginManager().registerEvents(playerPanel, this);
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
}
