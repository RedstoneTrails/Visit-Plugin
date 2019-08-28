package com.reddevtrails.visit.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;
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
import org.bukkit.util.StringUtil;

import com.reddevtrails.visit.Messages;
import com.reddevtrails.visit.Settings;
import com.reddevtrails.visit.VPlayer;
import com.reddevtrails.visit.Visit;
import com.reddevtrails.visit.Messages.AdminMessage;
import com.reddevtrails.visit.Messages.ErrorMessage;
import com.reddevtrails.visit.Messages.HelpMessage;
import com.reddevtrails.visit.Messages.Message;
import com.reddevtrails.visit.Messages.UsageMessage;
import com.reddevtrails.visit.utils.LocationUtil;

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
		if (cmd.equalsIgnoreCase("setvisit") && !Settings.blockSetVistCommand) {
			// Generic player use
			if (args.length == 0) {
				setVisit(player, uuid, player.getLocation(), true);
			}
			// Admin use to set other player's visit
			else if (args.length == 1 && isAdmin(player)) {
				UUID target = findPlayer(args[0]);
				if (target == null)
					sender.sendMessage(ErrorMessage.PLAYER_NOT_FOUND.toString().replace(Messages.PLAYER_LABEL, args[0]));
				else
					setVisit(player, target, player.getLocation(), true);
			}
			// Too many args, send proper usage
			else {
				sender.sendMessage(UsageMessage.SET_VISIT.toString());
			}

			return true;
		}
		// Using /delvisit
		else if (cmd.equalsIgnoreCase("delvisit") && !Settings.blockSetVistCommand) {
			// Generic player use
			if (args.length == 0) {
				delVisit(player, uuid);
			}
			// Admin use to delete other player's visit
			else if (args.length == 1 && isAdmin(player)) {
				UUID target = findPlayer(args[0]);
				if (target == null)
					sender.sendMessage(ErrorMessage.PLAYER_NOT_FOUND.toString().replace(Messages.PLAYER_LABEL, args[0]));
				else
					delVisit(player, target);
			}
			// Too many args, send proper usage
			else {
				sender.sendMessage(UsageMessage.DEL_VISIT.toString());
			}

			return true;
		}
		// Using /visit
		else if (cmd.equalsIgnoreCase("visit")) {
			// Pull up visit GUI
			if (args.length == 0) {
				showPlayers(player);
			}
			// Visiting a player
			else if (args.length == 1) {
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
			if (args.length == 0)
				showPlayers(player);
			else
				showPlayers(player, Integer.valueOf(args[0]));
			
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
	private UUID findPlayer(String nameString) {
		UUID uuid = null;
		
		// Check through our existing list of players first
		for (OfflinePlayer testPlayer : plugin.getServer().getOfflinePlayers()) {
			// Check if name exactly matches
			if (testPlayer.getName().toLowerCase().equals(nameString.toLowerCase())) {
				uuid = testPlayer.getUniqueId();
				break;
			}
			// Check if they typed first part of a name
			else if (testPlayer.getName().toLowerCase().startsWith(nameString.toLowerCase())) {
				uuid = testPlayer.getUniqueId();
			}
		}
		
		return uuid;
	}
	
	/**
	 * Sets visit for the given UUID at the given location
	 * 
	 * @param player - Player calling this function. This player will receive error and confirmation messages
	 * @param uuid - UUID of the player who's visit location is being set. Works whether they are online or offline
	 * @param loc - Location to set the visit to
	 */
	public void setVisit(Player player, UUID uuid, Location loc, boolean sendMessage) {
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
		
		// Adjust location to have more suitable values
		loc = getRoundedDestination(loc);
		
		// All is well, set their visit here
		plugin.players().add(uuid, loc);
		
		// Leave now if we don't want to send any messages
		if (!sendMessage) return;
		
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
			message = AdminMessage.SET_VISIT.toString().replace(Messages.PLAYER_LABEL, targetName);
			if (target.isOnline()) {
				target.getPlayer().sendMessage(Message.SET_VISIT.toString().replace("{X}", x).replace("{Y}", y).replace("{Z}", z));
			}
		}
		
		player.sendMessage(message.replace("{X}", x).replace("{Y}", y).replace("{Z}", z));
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
			message = AdminMessage.DEL_VISIT.toString().replace(Messages.PLAYER_LABEL, targetName);
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
		UUID target = findPlayer(str);
		if (target == null) {
			player.sendMessage(ErrorMessage.PLAYER_NOT_FOUND.toString().replace(Messages.PLAYER_LABEL, str));
			return;
		}
		OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(target);
		
		// Try to find their visit location, send error if they don't have one set
		Location destination = plugin.players().findLocation(target);
		if (destination == null) {
			player.sendMessage(ErrorMessage.VISIT_NOT_SET.toString().replace(Messages.PLAYER_LABEL, targetPlayer.getName()));
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
		if (!player.getUniqueId().equals(target)) {
			if (targetPlayer.isOnline() && plugin.players().wantsMessages(target)) {
				targetPlayer.getPlayer().sendMessage(Message.VISITED_YOU.toString().replace(Messages.PLAYER_LABEL, player.getName()));
			}
			player.sendMessage(Message.VISIT.toString().replace(Messages.PLAYER_LABEL, targetPlayer.getName()));
		}
		player.teleport(destination);
	}
	
	private void showPlayers(Player player) {
		showPlayers(player, 1);
	}
	
	private void showPlayers(Player player, int page) {
		page = page - 1;
		if (plugin.players().map().isEmpty()) {
			player.sendMessage(ErrorMessage.NO_VISIT_LOCATIONS.toString());
			return;
		}
			
		player.openInventory(plugin.playerPanel().getPanel(page));
	}
	
	private void showHelp(CommandSender sender) {
		// TODO: change these to read from messages config
		sender.sendMessage("");
		sender.sendMessage(HelpMessage.HEADER.toString());
		sender.sendMessage(HelpMessage.SET_VISIT.toString());
		sender.sendMessage(HelpMessage.DEL_VISIT.toString());
		sender.sendMessage(HelpMessage.VISIT.toString());
		sender.sendMessage(HelpMessage.PLAYERS.toString());
		sender.sendMessage(HelpMessage.HELP.toString());
		sender.sendMessage(HelpMessage.MESSAGES.toString());
		
		// If console or admin is calling, show admin commands
		if (!(sender instanceof Player) || (sender instanceof Player && isAdmin((Player)sender))) {
			sender.sendMessage("");
			sender.sendMessage(HelpMessage.ADMIN_HEADER.toString());
			sender.sendMessage(HelpMessage.ADMIN_SET_VISIT.toString());
			sender.sendMessage(HelpMessage.ADMIN_DEL_VISIT.toString());
			sender.sendMessage(HelpMessage.RELOAD.toString());
		}
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		Player player = null;
		if (sender instanceof Player)
			player = (Player) sender;
		String cmd = command.getName().toLowerCase();
		List<String> allOptions = new ArrayList<>();
		List<String> options = new ArrayList<>();
		
		if (args.length == 0 || args.length == 1) {
			if (player == null || isAdmin(player)) {
				if (cmd.equalsIgnoreCase("setvisit"))
					allOptions.addAll(onlinePlayerList(player));
				else if (cmd.equalsIgnoreCase("delvisit"))
					allOptions.addAll(getAllPlayers(player, args));
			}
			
			if (cmd.equalsIgnoreCase("visit")) {
				allOptions.addAll(getAllPlayers(player, args));
			}
			else if (cmd.equalsIgnoreCase(Messages.COMMAND_LABEL)) {
				allOptions.add("help");
				allOptions.add("messages");
				if (player == null || isAdmin(player)) {
					allOptions.add("reload");
				}
			}
		} 
		if (args.length == 1 || args.length == 2) {
			if (args[0].equalsIgnoreCase("messages")) {
				allOptions.add("on");
				allOptions.add("off");
			}
		}
		
		allOptions.removeAll(Arrays.asList("", null));
		
		// Sort out our array
		if (args.length == 0) {
			options = allOptions;
		} else if (args.length == 1) {
			if (StringUtils.isEmpty(args[0]))
				options = allOptions;
			else
				StringUtil.copyPartialMatches(args[0], allOptions, options);
		} else if (args.length == 2) {
			if (StringUtils.isEmpty(args[1]))
				options = allOptions;
			else
				StringUtil.copyPartialMatches(args[1], allOptions, options);
		}
		Collections.sort(options);
		return options;
	}
	
	private List<String> getAllPlayers(Player player, String[] args) {
		List<String> allPlayers = new ArrayList<>();
		allPlayers.addAll(plugin.players().names());
		allPlayers.addAll(onlinePlayerList(player));
		
		return allPlayers;
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
