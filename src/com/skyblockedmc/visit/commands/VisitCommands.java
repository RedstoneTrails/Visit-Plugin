package com.skyblockedmc.visit.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World.Environment;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import com.skyblockedmc.visit.Messages;
import com.skyblockedmc.visit.Messages.AdminMessage;
import com.skyblockedmc.visit.Messages.ErrorMessage;
import com.skyblockedmc.visit.Messages.Message;
import com.skyblockedmc.visit.Messages.UsageMessage;
import com.skyblockedmc.visit.VPlayer;
import com.skyblockedmc.visit.Visit;
import com.skyblockedmc.visit.utils.LocationUtil;

public class VisitCommands extends LocationUtil implements CommandExecutor, TabCompleter {
	private Visit plugin;

	public VisitCommands(Visit plugin) {
		this.plugin = plugin;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		// Return if not a player
		if (!(sender instanceof Player) && !command.getName().toLowerCase().equalsIgnoreCase(Messages.COMMAND_LABEL)) {
			sender.sendMessage(
					ChatColor.RED + "Console commands currently are not enabled for this command. Please use commands in-game.");
			return true;
		}
		
		// Get basic info
		Player player = (Player) sender;
		UUID uuid = player.getUniqueId();
		String cmd = command.getName().toLowerCase();

		// Using /setvisit
		if (cmd.equalsIgnoreCase("setvisit")) {
			// Generic player use
			if (args.length == 0) {
				setVisit(player, uuid, player.getLocation());
			}
			// Admin use to set other player's visit
			else if (args.length == 1 && isAdmin(player)) {
				Player target = findPlayer(args[0]);
				if (target == null)
					sender.sendMessage(ErrorMessage.PLAYER_NOT_FOUND.toString().replaceAll(Messages.PLAYER_LABEL, args[0]));
				else
					setVisit(player, target.getUniqueId(), player.getLocation());
			}
			// Too many args, send proper usage
			else {
				sender.sendMessage(UsageMessage.SET_VISIT.toString());
			}

			return true;
		}
		// Using /delvisit
		else if (cmd.equalsIgnoreCase("delvisit")) {
			// Generic player use
			if (args.length == 0) {
				delVisit(player, uuid);
			}
			// Admin use to delete other player's visit
			else if (args.length == 1 && isAdmin(player)) {
				Player target = findPlayer(args[0]);
				if (target == null)
					sender.sendMessage(ErrorMessage.PLAYER_NOT_FOUND.toString().replaceAll(Messages.PLAYER_LABEL, args[0]));
				else
					delVisit(player, target.getUniqueId());
			}
			// Too many args, send proper usage
			else {
				sender.sendMessage(UsageMessage.DEL_VISIT.toString());
			}

			return true;
		}
		// Using /visit
		else if (cmd.equalsIgnoreCase("visit")) {
			// Visiting a player
			if (args.length == 1) {
				visit(player, args[0]);
			}
			// Not enough args, or too many
			else {
				sender.sendMessage(UsageMessage.VISIT.toString());
			}
			
			return true;
		}
		// Using /players
		else if (cmd.equalsIgnoreCase("players")) {
			//TODO: Add in cases where page numbers are given taking args into consideration
			showPlayers(player);
			
			return true;
		}
		// Customization commands
		else if (cmd.equalsIgnoreCase(Messages.COMMAND_LABEL)) {
			// Send help / show usage of commands
			if (args.length == 0 || (args.length == 1 && args[0].equalsIgnoreCase("help"))) {
				showHelp(sender);
				return true;
			} else if (args.length == 1) {
				if (args[0].equalsIgnoreCase("messages")) {
					sender.sendMessage(UsageMessage.MESSAGE_TOGGLE.toString());
					return true;
				} else if (args[0].equalsIgnoreCase("reload") && isAdmin(player)) {
					plugin.reload();
					sender.sendMessage(ChatColor.GREEN + "Successfully reloaded Visit configuration.");
					return true;
				}
			} else if (args.length == 2) {
				if (args[0].equalsIgnoreCase("messages")) {
					if (plugin.players().find(uuid) == null) {
						sender.sendMessage(ErrorMessage.NO_VISIT_LOCATION.toString());
						return true;
					}
					VPlayer vPlayer = plugin.players().find(uuid);
					if (args[1].equalsIgnoreCase("on")) {
						if (vPlayer.wantsMessages()) {
							sender.sendMessage(ErrorMessage.MESSAGES_ALREADY_ON.toString());
							return true;
						}
						
						vPlayer.setMessages(true);
						sender.sendMessage(Message.MESSAGES_ON.toString());
						return true;
					} else if (args[1].equalsIgnoreCase("off")) {
						if (!vPlayer.wantsMessages()) {
							sender.sendMessage(ErrorMessage.MESSAGES_ALREADY_OFF.toString());
							return true;
						}
						
						vPlayer.setMessages(false);
						sender.sendMessage(Message.MESSAGES_OFF.toString());
						return true;
					}
				}
			}
			
			// All else failed, but they made an attempt. Show them help and move on.
			showHelp(sender);
			return true;
		}

		return false;
	}
	
	/**
	 * Attempts to find a Player based upon given String
	 * 
	 * @param nameString - String corresponding to a players name
	 * @return Player corresponding to given String, null if no player is found
	 */
	private Player findPlayer(String nameString) {
		Player player = null;
		
		// Check through our existing list of players first
		for (OfflinePlayer testPlayer : plugin.getServer().getOfflinePlayers()) {
			// Check if name exactly matches
			if (testPlayer.getName().toLowerCase().equals(nameString.toLowerCase())) {
				player = testPlayer.getPlayer();
				break;
			}
			// Check if they typed first part of a name
			else if (testPlayer.getName().toLowerCase().startsWith(nameString.toLowerCase())) {
				player = testPlayer.getPlayer();
			}
		}
		
		return player;
	}
	
	/**
	 * Sets visit for the given UUID at the given location
	 * 
	 * @param player - Player calling this function. This player will receive error and confirmation messages
	 * @param uuid - UUID of the player who's visit location is being set. Works whether they are online or offline
	 * @param loc - Location to set the visit to
	 */
	private void setVisit(Player player, UUID uuid, Location loc) {
		// TODO: Allow customization of what worlds players can set their visit in
		// Prevent setting visit in disallowed world
		if (loc.getWorld().getEnvironment() != Environment.NORMAL) {
			player.sendMessage(ErrorMessage.INVALID_LOCATION.toString());
			return;
		}
		// Prevent setting unsafe visit location
		if (blockIsUnsafeForUser(player, loc)) {
			player.sendMessage(ErrorMessage.UNSAFE_LOCATION.toString());
		}
		
		// All is well, set their visit here
		plugin.players().add(uuid, loc);
		
		// Send appropriate confirmation message
		String message;
		String x = String.valueOf(loc.getBlockX());
		String y = String.valueOf(loc.getBlockY());
		String z = String.valueOf(loc.getBlockZ());
		// Player set their own visit
		if (player.getUniqueId().equals(uuid)) {
			message = Message.SET_VISIT.toString();
		}
		// Called by admin
		else {
			OfflinePlayer target =  Bukkit.getOfflinePlayer(uuid);
			String targetName = target.getName();
			message = AdminMessage.SET_VISIT.toString().replaceAll(Messages.PLAYER_LABEL, targetName);
			if (target.isOnline()) {
				target.getPlayer().sendMessage(Message.SET_VISIT.toString().replaceAll("{X}", x).replaceAll("{Y}", y).replaceAll("{Z}", z));
			}
		}
		
		player.sendMessage(message.replaceAll("{X}", x).replaceAll("{Y}", y).replaceAll("{Z}", z));
	}
	
	/**
	 * Removes visit location for the given UUID
	 * 
	 * @param player - Player calling this function. This player will receive error and confirmation messages
	 * @param uuid - UUID of the player who's visit location is being deleted. Works whether they are online or offline
	 */
	private void delVisit(Player player, UUID uuid) {
		if (plugin.players().find(uuid) == null) {
			player.sendMessage(ErrorMessage.NO_HOME_TO_DELETE.toString());
			return;
		}
		
		plugin.players().remove(uuid);
		
		String message;
		// Player deleted their own home
		if (player.getUniqueId().equals(uuid)) {
			message = Message.DEL_VISIT.toString();
		}
		// Called by admin
		else {
			OfflinePlayer target =  Bukkit.getOfflinePlayer(uuid);
			String targetName = target.getName();
			message = AdminMessage.DEL_VISIT.toString().replaceAll(Messages.PLAYER_LABEL, targetName);
			if (target.isOnline()) {
				target.getPlayer().sendMessage(Message.DEL_VISIT.toString());
			}
		}
		
		player.sendMessage(message);
	}
	
	/**
	 * Teleports player to visit location corresponding to given String parameter
	 * 
	 * @param player - Player who will be visiting another player
	 * @param str - String corresponding to the name of a player that will be visited
	 */
	private void visit(Player player, String str) {
		// Try to find a player from the string, send error if no player found
		Player target = findPlayer(str);
		if (target == null) {
			player.sendMessage(ErrorMessage.PLAYER_NOT_FOUND.toString().replaceAll(Messages.PLAYER_LABEL, str));
			return;
		}
		
		// Try to find their visit location, send error if they don't have one set
		Location destination = plugin.players().findLocation(target.getUniqueId());
		if (destination == null) {
			player.sendMessage(ErrorMessage.VISIT_NOT_SET.toString().replaceAll(Messages.PLAYER_LABEL, target.getName()));
			return;
		}
		
		// Try to find a safe location, send error message if visit is unsafe and none can be found
		try {
			destination = getSafeDestination(player, destination);
		} catch (Exception ex) {
			player.sendMessage(ErrorMessage.UNSAFE_LOCATION.toString());
			return;
		}
		
		// All is well, send messages and teleport them
		if (target.isOnline() && plugin.players().wantsMessages(target.getUniqueId())) {
			target.sendMessage(Message.VISITED_YOU.toString().replaceAll(Messages.PLAYER_LABEL, player.getName()));
		}
		player.sendMessage(Message.VISIT.toString().replaceAll(Messages.PLAYER_LABEL, target.getName()));
		player.teleport(destination);
	}
	
	private void showPlayers(Player player) {
		if (plugin.players().map().isEmpty()) {
			player.sendMessage(ErrorMessage.NO_VISIT_LOCATIONS.toString());
			return;
		}
			
	}
	
	private void showHelp(CommandSender sender) {
		sender.sendMessage(Messages.colorize("&a--- Visit Commands ---"));
		sender.sendMessage(Messages.colorize("&6/setvisit&r: Sets your visit to your current location."));
		sender.sendMessage(Messages.colorize("&6/delvisit&r: Deletes your visit location."));
		sender.sendMessage(Messages.colorize("&6/visit <player>&r: Teleports you to a player's visit location."));
		sender.sendMessage(Messages.colorize("&6/players&r: Shows a GUI of all players you can visit."));
		sender.sendMessage(Messages.colorize("&6/" + Messages.COMMAND_LABEL + " help&r: Shows list of commands for this plugin."));
		sender.sendMessage(Messages.colorize("&6/" + Messages.COMMAND_LABEL + " messages on/off&r: Turns on/off your notifications for when someone visits you."));
		
		// If console or admin is calling, show admin commands
		if (!(sender instanceof Player) || (sender instanceof Player && isAdmin((Player)sender))) {
			sender.sendMessage("");
			sender.sendMessage(Messages.colorize("&a--- Admin Commands ---"));
			sender.sendMessage(Messages.colorize("&6/setvisit <player>&r: Sets visit for target player to your current location."));
			sender.sendMessage(Messages.colorize("&6/delvisit <player>&r: Deletes visit location for target player."));
			sender.sendMessage(Messages.colorize("&6/" + Messages.COMMAND_LABEL + " reload&r: Reloads messages and configuration settings."));
		}
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		Player player = null;
		if (sender instanceof Player)
			player = (Player) sender;
		String cmd = command.getName().toLowerCase();
		List<String> options = new ArrayList<>();
		
		if (args.length == 0) {
			if (player == null || isAdmin(player)) {
				if (cmd.equalsIgnoreCase("setvisit"))
					options.addAll(onlinePlayerList(player));
				else if (cmd.equalsIgnoreCase("delvisit"))
					options.addAll(plugin.players().names());
			}
			if (cmd.equalsIgnoreCase("visit"))
				options.addAll(plugin.players().names());
			else if (cmd.equalsIgnoreCase(Messages.COMMAND_LABEL)) {
				options.add("help");
				options.add("messages");
				if (player == null || isAdmin(player)) {
					options.add("reload");
				}
			}
		} else if (args.length == 1) {
			if (cmd.equalsIgnoreCase("messages")) {
				options.add("on");
				options.add("off");
			}
		}

		return options;
	}
	
	private boolean isAdmin(Player player) {
		return player.isOp() || player.hasPermission("visit.admin");
	}
	
	/**
     * Return a list of online players this player can see
     * 
     * @param player - Player that the list will be made relative to
     * @return
     */
    public static List<String> onlinePlayerList(Player player) {
        final List<String> returned = new ArrayList<String>();
        for (Player p : Bukkit.getServer().getOnlinePlayers()) {
            if (player == null || player.canSee(p)) {
                returned.add(p.getName());
            }
        }
        return returned;
    }
}
