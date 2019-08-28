/*******************************************************************************
 * This file is part of ASkyBlock.
 *
 *     ASkyBlock is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     ASkyBlock is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with ASkyBlock.  If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package com.reddevtrails.visit.panels;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType.SlotType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import com.reddevtrails.visit.Messages;
import com.reddevtrails.visit.Settings;
import com.reddevtrails.visit.Visit;
import com.reddevtrails.visit.Messages.Message;
import com.reddevtrails.visit.utils.Requester;
import com.reddevtrails.visit.utils.Util;
import com.reddevtrails.visit.utils.HeadGetter.HeadInfo;

public class PlayerPanel implements Listener, Requester {
    private Visit plugin;
    private List<Inventory> playerPanel;
    // The list of all players who have visit locations and their corresponding inventory icon
    // A stack of zero amount will mean they are not active
    private Map<UUID, ItemStack> cachedPlayers;
    
    /**
     * @param plugin - Visit plugin object
     */
    public PlayerPanel(Visit plugin) {
        this.plugin = plugin;
        playerPanel = new ArrayList<Inventory>();
        cachedPlayers = new HashMap<UUID, ItemStack>();
        // Load the cache once server starts
        Bukkit.getScheduler().runTask(plugin, () -> loadCache());
    }
    
    private void loadCache() {
    	for (UUID playerUUID : plugin.players().map().keySet()) {
            addToCache(playerUUID);
        }
        updatePanel();
    }
    
    private void addToCache(UUID playerUUID) {
    	String playerName = Bukkit.getOfflinePlayer(playerUUID).getName();
        if (playerName == null || playerName.isEmpty()) {
        	plugin.getLogger().warning("Error finding name for player with UUID: " + playerUUID.toString() + ". Skipping...");
            return;
        }
        
        ItemStack playerSkull = new ItemStack(Material.PLAYER_HEAD, 1);
        SkullMeta meta = (SkullMeta) playerSkull.getItemMeta();
        meta.setOwningPlayer(Bukkit.getOfflinePlayer(playerUUID));
        meta.setDisplayName(ChatColor.RESET + Message.PLAYERS_NAME.toString().replace(Messages.PLAYER_LABEL, playerName));
        playerSkull.setItemMeta(meta);
        cachedPlayers.put(playerUUID, playerSkull);
        // Get the player head async
        addName(playerUUID, playerName);
    }

    /**
     * Adds a name to the list of player head requests
     * @param playerUUID
     * @param name
     */
    public void addName(UUID playerUUID, String playerName) {
        plugin.headGetter().getHead(playerUUID, this);
    }


    /**
     * Only change the text of the visit
     * @param playerUUID - the player's UUID
     */
    public void updateVisit(UUID playerUUID) {
        plugin.headGetter().getHead(playerUUID, this);
    }

    /**
     * Update the text on all the visit icons.
     */
    public void updateAllVisitText() {
        for (UUID playerUUID : cachedPlayers.keySet()) {
            plugin.headGetter().getHead(playerUUID, this);
        }
        updatePanel();
    }

    /**
     * Adds a new visit
     * @param playerUUID - the player's UUID
     */
    public void addVisit(UUID playerUUID) {
        // Check cached players
        if (cachedPlayers.containsKey(playerUUID)) {
            updatePanel();
            return;
        }
        addToCache(playerUUID);
        updatePanel();
    }

    /**
     * Creates the inventory panels from the visit list and adds nav buttons
     */
    public void updatePanel() {
        List<Inventory> updated = new ArrayList<>();
        Collection<UUID> activeVisits = plugin.players().map().keySet();
        
        // Get panel size, protect us from dumb values
        int panelSize = Math.round(Settings.maxPlayersPerPage / 9) * 9;
        if (panelSize < 9)
        	panelSize = 9;
        else if (panelSize > 45)
        	panelSize = 45;
        
        // Create the player panels
        int size = activeVisits.size();
        int panelNumber = size / (panelSize - 2);
        int remainder = (size % (panelSize - 2)) + 8 + 2;
        remainder -= (remainder % 9);
        int i = 0;
        
        for (i = 0; i < panelNumber; i++) 
            updated.add(Bukkit.createInventory(null, panelSize, Message.PLAYERS_TITLE.toString() + " #" + (i + 1)));
        
        // Make the last panel
        updated.add(Bukkit.createInventory(null, remainder, Message.PLAYERS_TITLE.toString() + " #" + (i + 1)));
        playerPanel = new ArrayList<>(updated);
        panelNumber = 0;
        int slot = 0;
        
        // Run through all the visit locations and add them to the inventories with anv buttons
        for (UUID playerUUID: activeVisits) {
            ItemStack icon = cachedPlayers.get(playerUUID);
            if (icon != null) {
                playerPanel.get(panelNumber).setItem(slot++, icon);

                // Check if the panel is full
                if (slot == panelSize - 2) {
                    // Add navigation buttons
                    if (panelNumber > 0) {
                        playerPanel.get(panelNumber).setItem(slot++, new CPItem(Material.PAPER, Message.PLAYERS_PREVIOUS.toString(), "players " + (panelNumber - 1), "").getItem());
                    }
                    playerPanel.get(panelNumber).setItem(slot, new CPItem(Material.PAPER, Message.PLAYERS_NEXT.toString(), "players " + (panelNumber + 1), "").getItem());
                    // Move onto the next panel
                    panelNumber++;
                    slot = 0;
                }
            }
        }
        if (remainder != 0 && panelNumber > 0) {
            playerPanel.get(panelNumber).setItem(slot++, new CPItem(Material.PAPER, Message.PLAYERS_PREVIOUS.toString(), "players " + (panelNumber - 1), "").getItem());
        }
    }

    public Inventory getPanel(int panelNumber) {
        if (panelNumber < 0) {
            panelNumber = 0;
        } else if (panelNumber > playerPanel.size() - 1) {
            panelNumber = playerPanel.size() - 1;
        }
        return playerPanel.get(panelNumber);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled=true)
    public void onInventoryClick(InventoryClickEvent event) {
    	// Get the inventory being clicked
        InventoryView inventory = event.getView();
        if (inventory == null || inventory.getTitle() == null) {
            return;
        }
        // The player that clicked the item
        final Player player = (Player) event.getWhoClicked();
        String title = inventory.getTitle();
        if (!inventory.getTitle().startsWith(Message.PLAYERS_TITLE + " #")) {
            return;
        }
        event.setCancelled(true);
        if (event.getSlotType().equals(SlotType.OUTSIDE)) {
            player.closeInventory();
            return;
        }
        if (event.getClick().equals(ClickType.SHIFT_RIGHT)) {
            player.closeInventory();
            return;
        }
        
        // Get item clicked, return if useless
        ItemStack clicked = event.getCurrentItem();
        if (event.getRawSlot() >= event.getInventory().getSize() || clicked.getType() == Material.AIR) {
            return;
        }
        
        int panelNumber = 0;
        try {
            panelNumber = Integer.valueOf(title.substring(title.indexOf('#') + 1));
        } catch (Exception e) {
            panelNumber = 0;
        }
        
        if (clicked.getItemMeta().hasDisplayName()) {
            String command = ChatColor.stripColor(clicked.getItemMeta().getDisplayName());
            if (command != null) {
                if (command.equalsIgnoreCase(ChatColor.stripColor(Message.PLAYERS_NEXT.toString()))) {
                    player.closeInventory();
                    Util.runCommand(player, "players " + (panelNumber + 1));
                } else if (command.equalsIgnoreCase(ChatColor.stripColor(Message.PLAYERS_PREVIOUS.toString()))) {
                    player.closeInventory();
                    Util.runCommand(player, "players " + (panelNumber - 1));
                } else {
                    player.closeInventory();
                    Util.runCommand(player, "visit " + command);
                }
            }
        }
    }

    @Override
    public void setHead(HeadInfo headInfo) {
        ItemStack head = headInfo.getHead();
        cachedPlayers.put(headInfo.getUuid(), headInfo.getHead());
        for (Inventory panel: playerPanel) {
            for (int i = 0; i < panel.getSize(); i++) {
                ItemStack it = panel.getItem(i);
                if (it != null && it.getType().equals(Material.PLAYER_HEAD)) {
                    ItemMeta meta = it.getItemMeta();
                    if (headInfo.getName().equals(meta.getDisplayName())) {
                        panel.setItem(i, head);
                        return;
                    }
                }
            }
        }   
    }
}
