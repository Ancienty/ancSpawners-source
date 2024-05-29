package com.ancienty.ancspawners.GUIs;

import com.ancienty.ancspawners.Main;
import com.cryptomorin.xseries.XMaterial;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class StorageGUI {
    
    private Player player;
    private Block block;
    private Inventory inventory;
    
    
    public StorageGUI(Player player, Block block) {
        if (player != null) {
            this.player = player;
            this.block = block;
            int size = Main.getPlugin().lang.getInt("storageMenu.menuRows");
            String name = ChatColor.translateAlternateColorCodes('&', Main.getPlugin().lang.getString("storageMenu.menuName"));
            inventory = Bukkit.createInventory(null, size * 9, name);


            initializeItems().thenAccept(unused -> openInventory());
        }
    }

    public CompletableFuture<Boolean> initializeItems() {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        if (block != null) {
            Main.database.getSpawnerMoney(player, block).thenAccept(spawner_money -> {
                Main.database.getStoredItems(block).thenAccept(stored_items -> {
                    Main.database.getStorageLimit(block).thenAccept(storage_limit -> {
                        Bukkit.getScheduler().runTask(Main.getPlugin(), () -> {
                            Material sellMaterial = XMaterial.valueOf(Main.getPlugin().lang.getString("storageMenu.sellItem.material")).parseMaterial();
                            String sellName = Main.getPlugin().lang.getString("storageMenu.sellItem.name");
                            List<String> sellLore = Main.getPlugin().lang.getStringList("storageMenu.sellItem.lore");
                            List<String> sellLoreReal = new ArrayList<>();
                            int slot = Main.getPlugin().lang.getInt("storageMenu.sellItem.slot");

                            ItemStack sellItem = new ItemStack(sellMaterial);
                            ItemMeta sellItemMeta = sellItem.getItemMeta();

                            sellItemMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', sellName));
                            for (String text : sellLore) {
                                text = text.replace("{multiplier}", String.valueOf(Main.getPlugin().getPlayerMultiplier(player)));
                                text = text.replace("{money}", String.valueOf(spawner_money));
                                sellLoreReal.add(ChatColor.translateAlternateColorCodes('&', text));
                            }
                            sellItemMeta.setLore(sellLoreReal);
                            sellItemMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
                            sellItem.setItemMeta(sellItemMeta);
                            inventory.setItem(slot, sellItem);

                            int stored_item_slot = 0;

                            for (String item_name : stored_items) {
                                ItemStack stored_item = XMaterial.valueOf(item_name).parseItem();
                                ItemMeta item_meta = stored_item.getItemMeta();

                                List<String> storedItemLore = Main.getPlugin().lang.getStringList("storageMenu.itemLores");
                                List<String> storedItemLoreReal = new ArrayList<>();

                                Main.database.getSpawnerStoredByItem(block, stored_item.getType()).thenAccept(item_amount -> {
                                    for (String text : storedItemLore) {
                                        text = text.replace("{amount_type}", String.valueOf(item_amount));
                                        text = text.replace("{storage_limit}", String.valueOf(storage_limit));
                                        storedItemLoreReal.add(ChatColor.translateAlternateColorCodes('&', text));
                                    }
                                });

                                item_meta.setLore(storedItemLoreReal);
                                item_meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
                                stored_item.setItemMeta(item_meta);
                                inventory.setItem(stored_item_slot, stored_item);
                                stored_item_slot += 1;
                            }
                            fillerItems();
                            future.complete(true);
                        });

                    }).exceptionally(throwable -> {
                        Main.getPlugin().getLogger().severe("Error getting spawner storage limit." + throwable.getClass().getName() + ": " + throwable.getMessage());
                        return null;
                    });
                }).exceptionally(throwable -> {
                    Main.getPlugin().getLogger().severe("Error getting spawner stored items." + throwable.getClass().getName() + ": " + throwable.getMessage());
                    return null;
                });
            }).exceptionally(throwable -> {
                Main.getPlugin().getLogger().severe("Error getting spawner money." + throwable.getClass().getName() + ": " + throwable.getMessage());
                return null;
            });
        }
        return future;
    }

    public void fillerItems() {
        Bukkit.getScheduler().runTask(Main.getPlugin(), () -> {
            if (block != null) {

                if (Main.getPlugin().lang.get("storageMenu.fillItem.material") != null && !Main.getPlugin().lang.getString("storageMenu.fillItem.material").equalsIgnoreCase("AIR")) {

                    ItemStack fillerItem = XMaterial.valueOf(Main.getPlugin().lang.getString("storageMenu.fillItem.material")).parseItem();
                    ItemMeta meta = fillerItem.getItemMeta();
                    String item_name = Main.getPlugin().lang.getString("storageMenu.fillItem.name") != null ? ChatColor.translateAlternateColorCodes('&', Main.getPlugin().lang.getString("storageMenu.fillItem.name")) : "no name";
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
