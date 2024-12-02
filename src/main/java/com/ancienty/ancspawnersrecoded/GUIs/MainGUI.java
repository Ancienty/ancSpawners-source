package com.ancienty.ancspawnersrecoded.GUIs;

import com.ancienty.ancspawnersrecoded.Main;
import com.ancienty.ancspawnersrecoded.SpawnerManager.ancSpawner;
import com.ancienty.ancspawnersrecoded.Utils.BoostInfo;
import com.cryptomorin.xseries.XMaterial;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class MainGUI {

    private Inventory inventory = null;
    private Player player = null;
    private Block block = null;

    public MainGUI(Block block, Player player) {
        if (block != null) {
            int size = Main.getPlugin().lang.getInt("menu.menuRows");
            String name = ChatColor.translateAlternateColorCodes('&', Main.getPlugin().lang.getString("menu.menuName"));
            inventory = Bukkit.createInventory(null, size * 9, name);
            this.player = player;
            this.block = block;
            Main.getPlugin().player_block_map.put(this.player, this.block);

            // Place items.
            initializeItems();
        }
    }

    public void initializeItems() {
        if (block != null) {
            ancSpawner spawner = Main.getPlugin().getSpawnerManager().getSpawner(block.getWorld(), block.getLocation());
            if (spawner != null) {
                Bukkit.getScheduler().runTask(Main.getPlugin(), () -> {
                    String owner = spawner.getOwnerUUID();
                    String type = spawner.getType();
                    String mode = spawner.getMode();
                    int total_stored = spawner.getStorage().getTotalStored();
                    int total_xp = spawner.getStorage().getStoredXp();
                    int level = spawner.getLevel();
                    double money = spawner.getStorage().getMoney(player);

                    for (String configKey : Main.getPlugin().lang.getConfigurationSection("menu").getKeys(false)) {
                        if (!configKey.equalsIgnoreCase("menuName") && !configKey.equalsIgnoreCase("menuRows") && !configKey.equalsIgnoreCase("fillItem")) {
                            if (Main.getPlugin().lang.getBoolean("menu." + configKey + ".gui")) {
                                int slot = Main.getPlugin().lang.getInt("menu." + configKey + ".slot");
                                ItemStack material;
                                if (!Main.getPlugin().lang.getString("menu." + configKey + ".material").startsWith("head-")) {
                                    material = XMaterial.valueOf(Main.getPlugin().lang.getString("menu." + configKey + ".material")).parseItem();
                                } else {
                                    material = Main.getHead(Main.getPlugin().lang.getString("menu." + configKey + ".material").split("head-")[1]);
                                }

                                if (!((configKey.equalsIgnoreCase("auto-kill") || configKey.equalsIgnoreCase("exp")) && mode.equalsIgnoreCase("item"))) {
                                    String name = ChatColor.translateAlternateColorCodes('&', Main.getPlugin().lang.getString("menu." + configKey + ".name"));
                                    List<String> lore = Main.getPlugin().lang.getStringList("menu." + configKey + ".lore");
                                    List<String> real_lore = new ArrayList<>();

                                    for (String text : lore) {
                                        if (text.contains("{active_boosts}")) {
                                            // Get active boosts
                                            Map<String, BoostInfo> activeBoosts = spawner.getBoosts().getActiveBoosts();
                                            if (activeBoosts.isEmpty()) {
                                                // No active boosts
                                                String noBoostsText = ChatColor.translateAlternateColorCodes('&', Main.getPlugin().lang.getString("lang.no_active_boosts"));
                                                real_lore.add(noBoostsText);
                                            } else {
                                                // Add each boost to the lore
                                                for (Map.Entry<String, BoostInfo> entry : activeBoosts.entrySet()) {
                                                    String boostType = entry.getKey();
                                                    BoostInfo boostInfo = entry.getValue();
                                                    double boostAmount = boostInfo.getAmount();
                                                    long endTime = boostInfo.getEndTime();
                                                    long timeLeftMillis = endTime - System.currentTimeMillis();

                                                    // Format time left
                                                    String timeLeftFormatted = formatTimeLeft(timeLeftMillis);

                                                    // Get the boost name from lang.yml
                                                    String boostNameKey = "";
                                                    String boostAmountFormatted = "";
                                                    if (boostType.equalsIgnoreCase("spawn_amount")) {
                                                        boostNameKey = "boost_spawn_amount";
                                                        boostAmountFormatted = String.format("%.2f", boostAmount);
                                                    } else if (boostType.equalsIgnoreCase("spawn_time")) {
                                                        boostNameKey = "boost_spawn_time";
                                                        boostAmountFormatted = String.format("%.2f", boostAmount);
                                                    } else if (boostType.equalsIgnoreCase("sell_multiplier")) {
                                                        boostNameKey = "boost_sell_multiplier";
                                                        boostAmountFormatted = String.format("%.2f", boostAmount);
                                                    } else {
                                                        // Default boost name if not defined
                                                        boostNameKey = "boost_" + boostType;
                                                    }

                                                    String boostDisplayName = Main.getPlugin().lang.getString("lang." + boostNameKey);
                                                    if (boostDisplayName == null) {
                                                        boostDisplayName = "&7" + boostType + " &8(&6{0}&8) &8[&f{1}&8]";
                                                    }

                                                    boostDisplayName = boostDisplayName.replace("{0}", boostAmountFormatted).replace("{1}", timeLeftFormatted);
                                                    boostDisplayName = ChatColor.translateAlternateColorCodes('&', boostDisplayName);

                                                    real_lore.add(boostDisplayName);
                                                }
                                            }
                                        } else {
                                            // Replace other placeholders
                                            text = text.replace("{type}", type);
                                            text = text.replace("{level}", String.valueOf(level));
                                            text = text.replace("{owner}", Bukkit.getOfflinePlayer(UUID.fromString(owner)).getName());
                                            text = text.replace("{amount}", String.valueOf(total_stored));
                                            text = text.replace("{multiplier}", String.valueOf(Main.getPlugin().getPlayerMultiplier(player)));
                                            text = text.replace("{experience}", String.valueOf(total_xp));
                                            text = text.replace("{money}", String.valueOf(money));
                                            text = ChatColor.translateAlternateColorCodes('&', text);
                                            real_lore.add(text);
                                        }
                                    }

                                    addItem(slot, material, name, real_lore);
                                }
                            }
                        }
                    }

                    fillerItems();
                });
            }
        }
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
                if (Main.getPlugin().lang.get("menu.fillItem.material") != null && !Main.getPlugin().lang.getString("menu.fillItem.material").equalsIgnoreCase("AIR")) {
                    ItemStack fillerItem = XMaterial.valueOf(Main.getPlugin().lang.getString("menu.fillItem.material")).parseItem();
                    ItemMeta meta = fillerItem.getItemMeta();
                    String item_name = Main.getPlugin().lang.getString("menu.fillItem.name") != null ? ChatColor.translateAlternateColorCodes('&', Main.getPlugin().lang.getString("menu.fillItem.name")) : "no name";
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
        player.openInventory(inventory);
    }

    /**
     * Formats the time left in a human-readable format.
     */
    private String formatTimeLeft(long millis) {
        if (millis <= 0) {
            return "0s";
        }

        long seconds = millis / 1000;

        long days = seconds / (24 * 3600);
        seconds %= (24 * 3600);

        long hours = seconds / 3600;
        seconds %= 3600;

        long minutes = seconds / 60;
        seconds %= 60;

        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("d ");
        if (hours > 0) sb.append(hours).append("h ");
        if (minutes > 0) sb.append(minutes).append("m ");
        if (seconds > 0 || sb.length() == 0) sb.append(seconds).append("s");

        return sb.toString().trim();
    }
}
