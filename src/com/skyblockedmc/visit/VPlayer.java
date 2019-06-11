package com.skyblockedmc.visit;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;

public class VPlayer {
	private Location loc;
	private boolean wantsMessages;
	
	public VPlayer(Location loc, boolean wantsMessages) {
		this.loc = loc;
		this.wantsMessages = wantsMessages;
	}
	
	public VPlayer(Location loc) {
		this(loc, false);
	}
	
	public VPlayer(ConfigurationSection config) {
		wantsMessages = config.getBoolean(Messages.MESSAGES_CONFIG);
		loc = Location.deserialize(config.getConfigurationSection(Messages.LOCATION_CONFIG).getValues(true));
	}
	
	public Location getLocation() {
		return loc;
	}
	
	public boolean wantsMessages() {
		return wantsMessages;
	}
	
	public void setMessages(boolean wantsMessages) {
		this.wantsMessages = wantsMessages;
	}
}
