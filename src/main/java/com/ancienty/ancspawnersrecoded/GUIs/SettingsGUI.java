package com.ancienty.ancspawnersrecoded.GUIs;

import com.ancienty.ancspawnersrecoded.Main;
import com.ancienty.ancspawnersrecoded.SpawnerManager.ancSpawner;
import com.cryptomorin.xseries.XMaterial;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class SettingsGUI {

    private Player player = null;
    private Block block = null;
    private Inventory inventory = null;

    public SettingsGUI(Player player, Block block) {
        if (player != null) {
            this.player = player;
            this.block = block;
            Main.getPlugin().player_block_map.put(this.player, this.block);

            int size = Main.getPlugin().lang.getInt("settingsMenu.menuRows");
            String name = ChatColor.translateAlternateColorCodes('&', Main.getPlugin().lang.getString("settingsMenu.menuName"));
            inventory = Bukkit.createInventory(null, size * 9, name);

            // Initialize the items.
            if (initializeItems()) {
                openInventory();
            }
        }
    }

    public boolean initializeItems() {
        if (block != null) {
            ancSpawner spawner = Main.getPlugin().getSpawnerManager().getSpawner(block.getWorld(), block.getLocation());
            if (spawner != null) {
                Bukkit.getScheduler().runTask(Main.getPlugin(), () -> {
                    boolean virtualStorageEnabled = spawner.isVirtualStorageEnabled();
                    boolean xpStorageEnabled = spawner.isXPStorageEnabled();
                    String enabled = "&a✔";
                    String disabled = "&c✖";
                    String virtualMessage;
                    String xpMessage;
                    if (virtualStorageEnabled) virtualMessage = enabled;
                    else { virtualMessage = disabled; }
                    if (xpStorageEnabled) xpMessage = enabled;
                    else { xpMessage = disabled; }

                    for (String configKey : Main.getPlugin().lang.getConfigurationSection("settingsMenu").getKeys(false)) {
                        if (!configKey.equalsIgnoreCase("menuName") && !configKey.equalsIgnoreCase("menuRows") && !configKey.equalsIgnoreCase("fillItem")) {
                            int slot = Main.getPlugin().lang.getInt("settingsMenu." + configKey + ".slot");
                            ItemStack material;
                            if (!Main.getPlugin().lang.getString("settingsMenu." + configKey + ".material").startsWith("head-")) {
                                material = XMaterial.valueOf(Main.getPlugin().lang.getString("settingsMenu." + configKey + ".material")).parseItem();
                            } else {
                                material = Main.getHead(Main.getPlugin().lang.getString("settingsMenu." + configKey + ".material").split("head-")[1]);
                            }

                            String name = ChatColor.translateAlternateColorCodes('&', Main.getPlugin().lang.getString("settingsMenu." + configKey + ".name"));
                            List<String> lore = Main.getPlugin().lang.getStringList("settingsMenu." + configKey + ".lore");
                            List<String> real_lore = new ArrayList<>();
                            lore.forEach(text -> {
                                text = text.replace("{virtual_storage}", virtualMessage);
                                text = text.replace("{xp_storage}", xpMessage);
                                text = ChatColor.translateAlternateColorCodes('&', text);
                                real_lore.add(text);
                            });

                            addItem(slot, material, name, real_lore);
                        }
                    }
                    fillerItems();
                });
            }
        }
        return true;
    }

    public void addItem(int slot, ItemStack itemStack, String name, List<String> lore) {
        Bukkit.getScheduler().runTask(Main.getPlugin(), () -> {
            if (block != null) {
                ItemStack item = itemStack;
                ItemMeta meta = item.getItemMeta();
                meta.setDisplayName(name);
                meta.setLore(lore);
                meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
                item.setItemMeta(meta);
                inventory.setItem(slot, item);
            }
        });
    }

    public void fillerItems() {
        Bukkit.getScheduler().runTask(Main.getPlugin(), () -> {
            if (block != null) {
                if (Main.getPlugin().lang.get("settingsMenu.fillItem.material") != null && !Main.getPlugin().lang.getString("settingsMenu.fillItem.material").equalsIgnoreCase("AIR")) {
                    ItemStack fillerItem = XMaterial.valueOf(Main.getPlugin().lang.getString("settingsMenu.fillItem.material")).parseItem();
                    ItemMeta meta = fillerItem.getItemMeta();
                    String item_name = Main.getPlugin().lang.getString("settingsMenu.fillItem.name") != null ? ChatColor.translateAlternateColorCodes('&', Main.getPlugin().lang.getString("settingsMenu.fillItem.name")) : "no name";
                    if (!item_name.equalsIgnoreCase("no name")) {
                        meta.setDisplayName(item_name);
                    }
                    fillerItem.setItemMeta(meta);

                    for (int i = 0; i < inventory.getSize(); i++) {
                        if (inventory.getItem(i) == null || inventory.getItem(i).getType() == XMaterial.AIR.parseMaterial()) {
                            inventory.setItem(i, fillerItem);
                        }
                    }
                }
            }
        });
    }



    public void openInventory() {
        Bukkit.getScheduler().runTask(Main.getPlugin(), () -> {
            player.openInventory(inventory);
        });
    }
}
