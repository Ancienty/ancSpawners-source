package com.ancienty.ancspawnersrecoded.GUIs;

import com.ancienty.ancspawnersrecoded.Main;
import com.ancienty.ancspawnersrecoded.SpawnerManager.ancSpawner;
import com.cryptomorin.xseries.XMaterial;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class FriendsGUI {

    private Player player = null;
    private Block block = null;
    private Inventory inventory = null;

    public FriendsGUI(Player player, Block block) {
        if (player != null) {
            this.player = player;
            this.block = block;
            Main.getPlugin().player_block_map.put(this.player, this.block);

            int size = 6;
            String name = ChatColor.translateAlternateColorCodes('&', Main.getPlugin().lang.getString("friendsMenu.menuName"));
            inventory = Bukkit.createInventory(null, size * 9, name);

            // Initialize the items.
            if (initializeItems()) {
                openInventory();
            }
        }
    }

    public boolean initializeItems() {
        final int[] friendSlot = {0};
        FileConfiguration config = Main.getPlugin().lang;
        ancSpawner spawner = Main.getPlugin().getSpawnerManager().getSpawner(block.getWorld(), block.getLocation());
        List<String> friendUUIDs = spawner.getFriendUuids();

        for (String friendUUID : friendUUIDs) {
            OfflinePlayer friend = Bukkit.getOfflinePlayer(UUID.fromString(friendUUID));
            ItemStack friendHead = new ItemStack(XMaterial.PLAYER_HEAD.parseMaterial());
            SkullMeta friendHeadMeta = (SkullMeta) friendHead.getItemMeta();
            List<String> lore = config.getStringList("friendsMenu.friends.lore");
            List<String> loreReal = new ArrayList<>();
            String friendHeadName = "&e" + friend.getName();
            friendHeadMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', friendHeadName));
            loreReal.add(ChatColor.translateAlternateColorCodes('&', "&8" + friendUUID));

            for (String text : lore) {
                loreReal.add(ChatColor.translateAlternateColorCodes('&', text));
            }

            friendHeadMeta.setOwningPlayer(friend);
            friendHeadMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', friendHeadName));
            friendHeadMeta.setLore(loreReal);
            friendHead.setItemMeta(friendHeadMeta);

            inventory.setItem(friendSlot[0], friendHead);
            friendSlot[0]++;  // Increment the friendSlot for each friend added
        }

        Material addFriendMaterial;
        try {
            addFriendMaterial = XMaterial.valueOf(config.getString("friendsMenu.addItem.material")).parseMaterial();
        } catch (IllegalArgumentException e) {
            Main.getPlugin().getLogger().severe("Invalid material in config for addFriendItem: " + config.getString("friendsMenu.addItem.material"));
            return false;
        }
        String friendName = config.getString("friendsMenu.addItem.name");
        List<String> friendLore = config.getStringList("friendsMenu.addItem.lore");
        List<String> realLore = new ArrayList<>();
        int addFriendSlot = config.getInt("friendsMenu.addItem.slot");
        ItemStack addFriendItem = new ItemStack(addFriendMaterial);
        ItemMeta addFriendMeta = addFriendItem.getItemMeta();
        addFriendMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', friendName));
        for (String t : friendLore) {
            realLore.add(ChatColor.translateAlternateColorCodes('&', t));
        }
        addFriendMeta.setLore(realLore);
        addFriendItem.setItemMeta(addFriendMeta);
        inventory.setItem(addFriendSlot, addFriendItem);

        if (!config.getString("friendsMenu.fillItem.material").equalsIgnoreCase("AIR")) {
            Material fillMaterial = XMaterial.valueOf(config.getString("friendsMenu.fillItem.material")).parseMaterial();
            String name = null;
            ItemStack fillItem = new ItemStack(fillMaterial);
            if (config.getString("friendsMenu.fillItem.name") != null) {
                name = config.getString("friendsMenu.fillItem.name");
            }
            if (name != null) {
                ItemMeta fillItemMeta = fillItem.getItemMeta();
                fillItemMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
                fillItem.setItemMeta(fillItemMeta);
            }
            for (int i = 0; i <= 53; i++) {
                if (inventory.getItem(i) == null) {
                    inventory.setItem(i, fillItem);
                }
            }
        }
        return true;
    }

    public void openInventory() {
        ancSpawner spawner = Main.getPlugin().getSpawnerManager().getSpawner(block.getWorld(), block.getLocation());
        String owner_uuid = spawner.getOwnerUUID();
        if (owner_uuid == null) {
            return;
        }
        Bukkit.getScheduler().runTask(Main.getPlugin(), () -> {
            if (owner_uuid.equalsIgnoreCase(player.getUniqueId().toString())) {
                player.openInventory(inventory);
            } else {
                Main.getPlugin().sendMessage(player, "notTheOwner", new String[]{null});
            }
        });
    }
}
