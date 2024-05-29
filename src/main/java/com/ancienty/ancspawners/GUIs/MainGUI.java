package com.ancienty.ancspawners.GUIs;

import com.ancienty.ancspawners.Main;
import com.cryptomorin.xseries.XMaterial;
import eu.decentsoftware.holograms.api.DHAPI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

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

            // Place items.
            initializeItems();
        }
    }

    public void initializeItems() {
        if (block != null) {
            Main.database.getSpawnerOwner(block).thenAccept(owner -> {
                Main.database.getSpawnerType(block).thenAccept(type -> {
                    Main.database.getSpawnerMode(block).thenAccept(mode -> {
                        Main.database.getSpawnerTotalStored(block).thenAccept(total_stored -> {
                            Main.database.getStoredXP(block).thenAccept(total_xp -> {
                                Main.database.getSpawnerLevel(block).thenAccept(level -> {
                                    Main.database.getSpawnerMoney(player, block).thenAccept(money -> {
                                        Bukkit.getScheduler().runTask(Main.getPlugin(), () -> {
                                            for (String configKey : Main.getPlugin().lang.getConfigurationSection("menu").getKeys(false)) {
                                                if (!configKey.equalsIgnoreCase("menuName") || !configKey.equalsIgnoreCase("menuRows") || !configKey.equalsIgnoreCase("fillItem")) {
                                                    if (Main.getPlugin().lang.getBoolean("menu." + configKey + ".gui")) {
                                                        int slot = Main.getPlugin().lang.getInt("menu." + configKey + ".slot");
                                                        ItemStack material;
                                                        if (!Main.getPlugin().lang.getString("menu." + configKey + ".material").startsWith("head-")) {
                                                            material = XMaterial.valueOf(Main.getPlugin().lang.getString("menu." + configKey + ".material")).parseItem();
                                                        } else {
                                                            material = Main.getHead(Main.getPlugin().lang.getString("menu." + configKey + ".material").split("head-")[1]);
                                                        }
                                                        String name = ChatColor.translateAlternateColorCodes('&', Main.getPlugin().lang.getString("menu." + configKey + ".name"));
                                                        List<String> lore = Main.getPlugin().lang.getStringList("menu." + configKey + ".lore");
                                                        List<String> real_lore = new ArrayList<>();
                                                        lore.forEach(text -> {
                                                            text = text.replace("{type}", type);
                                                            text = text.replace("{level}", String.valueOf(level));
                                                            text = text.replace("{owner}", owner);
                                                            text = text.replace("{amount}", String.valueOf(total_stored));
                                                            text = text.replace("{multiplier}", String.valueOf(Main.getPlugin().getPlayerMultiplier(player)));
                                                            text = text.replace("{experience}", String.valueOf(total_xp));
                                                            text = text.replace("{money}", String.valueOf(money));
                                                            text = ChatColor.translateAlternateColorCodes('&', text);
                                                            real_lore.add(text);
                                                        });

                                                        addItem(slot, material, name, real_lore);
                                                    }
                                                }
                                            }
                                        });

                                        fillerItems();
                                    }).exceptionally(throwable -> {
                                        Main.getPlugin().getLogger().severe("Error getting spawner money." + throwable.getClass().getName() + ": " + throwable.getMessage());
                                        return null;
                                    });
                                }).exceptionally(throwable -> {
                                    Main.getPlugin().getLogger().severe("Error getting spawner level." + throwable.getClass().getName() + ": " + throwable.getMessage());
                                    return null;
                                });
                            }).exceptionally(throwable -> {
                                Main.getPlugin().getLogger().severe("Error getting spawner stored xp." + throwable.getClass().getName() + ": " + throwable.getMessage());
                                return null;
                            });
                        }).exceptionally(throwable -> {
                            Main.getPlugin().getLogger().severe("Error getting spawner total stored items." + throwable.getClass().getName() + ": " + throwable.getMessage());
                            return null;
                        });
                    }).exceptionally(throwable -> {
                        Main.getPlugin().getLogger().severe("Error getting spawner mode." + throwable.getClass().getName() + ": " + throwable.getMessage());
                        return null;
                    });
                }).exceptionally(throwable -> {
                    Main.getPlugin().getLogger().severe("Error getting spawner type." + throwable.getClass().getName() + ": " + throwable.getMessage());
                    return null;
                });
            }).exceptionally(throwable -> {
                Main.getPlugin().getLogger().severe("Error getting spawner owner." + throwable.getClass().getName() + ": " + throwable.getMessage());
                return null;
            });
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

}
