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
package com.reddevtrails.visit.utils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import com.reddevtrails.visit.Visit;

public class HeadGetter  {
    private final Map<UUID, HeadInfo> cachedHeads = new HashMap<>();
    private final Map<UUID, String> names = new ConcurrentHashMap<>();
    private final Map<UUID, Set<Requester>> headRequesters = new HashMap<>();
    private final Visit plugin;
    /**
     * @param plugin
     */
    public HeadGetter(Visit plugin) {
        super();
        this.plugin = plugin;
        runPlayerHeadGetter();
    }

    private void runPlayerHeadGetter() {
        plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            synchronized(names) {
                Iterator<Entry<UUID, String>> it = names.entrySet().iterator();
                if (it.hasNext()) {
                    Entry<UUID, String> en = it.next();
                    ItemStack playerSkull = new ItemStack(Material.PLAYER_HEAD, 1);
                    SkullMeta meta = (SkullMeta) playerSkull.getItemMeta();
                    meta.setOwningPlayer(Bukkit.getOfflinePlayer(en.getKey()));
                    meta.setDisplayName(ChatColor.WHITE + en.getValue());
                    playerSkull.setItemMeta(meta);
                    
                    // Save in cache
                    cachedHeads.put(en.getKey(), new HeadInfo(en.getValue(), en.getKey(), playerSkull));
                    // Tell requesters the head came in
                    if (headRequesters.containsKey(en.getKey())) {
                        for (Requester req : headRequesters.get(en.getKey())) {
                            plugin.getServer().getScheduler().runTask(plugin, () -> req.setHead(new HeadInfo(en.getValue(), en.getKey(), playerSkull)));
                        }
                    }
                    it.remove();
                }
            }
        }, 0L, 20L);
    }

    public void getHead(UUID playerUUID, Requester requester) {
        if (playerUUID == null) {
            return;
        }
        String name = Bukkit.getOfflinePlayer(playerUUID).getName();
        if (name == null || name.isEmpty()) {
            return;
        }
        // Check if in cache
        if (cachedHeads.containsKey(playerUUID)) {
            requester.setHead(cachedHeads.get(playerUUID));
        } else {
            // Get the name
            headRequesters.putIfAbsent(playerUUID, new HashSet<>());
            Set<Requester> requesters = headRequesters.get(playerUUID);
            requesters.add(requester);
            headRequesters.put(playerUUID, requesters);
            names.put(playerUUID, name);
        }
    }
    
    public class HeadInfo {
        String name = "";
        UUID uuid;
        ItemStack head;
        /**
         * @param name
         * @param uuid
         * @param head
         */
        public HeadInfo(String name, UUID uuid, ItemStack head) {
            this.name = name;
            this.uuid = uuid;
            this.head = head;
        }
        /**
         * @return the name
         */
        public String getName() {
            return name;
        }
        /**
         * @return the uuid
         */
        public UUID getUuid() {
            return uuid;
        }
        /**
         * @return the head
         */
        public ItemStack getHead() {
            return head.clone();
        }

    }
}
