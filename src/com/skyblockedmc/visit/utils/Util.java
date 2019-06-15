package com.skyblockedmc.visit.utils;

import org.bukkit.entity.Player;

import com.skyblockedmc.visit.Visit;

public class Util {
	
	private static final Visit plugin = Visit.plugin();
	
	public static void runCommand(final Player player, final String string) {
        if (plugin.getServer().isPrimaryThread()) {
            player.performCommand(string);
        } else {
            plugin.getServer().getScheduler().runTask(plugin, () -> player.performCommand(string));
        }
	}
}
