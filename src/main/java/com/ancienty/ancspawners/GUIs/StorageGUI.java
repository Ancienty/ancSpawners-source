package com.ancienty.ancspawners.GUIs;

import com.ancienty.ancspawners.Main;
import com.cryptomorin.xseries.XMaterial;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

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

    public CompletableFuture<Void> initializeItems() {
        CompletableFuture<Double> spawnerMoneyFuture = Main.database.getSpawnerMoney(player, block);
        CompletableFuture<List<String>> storedItemsFuture = Main.database.getStoredItems(block);
        CompletableFuture<Integer> storageLimitFuture = Main.database.getStorageLimit(block);
        CompletableFuture<String> spawnerTypeFuture = Main.database.getSpawnerType(block);

        return CompletableFuture.allOf(spawnerMoneyFuture, storedItemsFuture, storageLimitFuture, spawnerTypeFuture)
                .thenAcceptAsync(voidResult -> {
                    try {
                        Double spawnerMoney = spawnerMoneyFuture.get();
                        List<String> storedItems = storedItemsFuture.get();
                        int storageLimit = storageLimitFuture.get();
                        String spawnerType = spawnerTypeFuture.get();

                        Bukkit.getScheduler().runTask(Main.getPlugin(), () -> {
                            setupSellItem(spawnerMoney, spawnerType);
                            setupStoredItems(storedItems, storageLimit);
                            fillerItems();
                        });

                    } catch (Exception e) {
                        Main.getPlugin().getLogger().severe("Error initializing storage GUI: " + e.getMessage());
                    }
                });
    }

    private void setupSellItem(Double spawnerMoney, String spawnerType) {
        if (Main.getPlugin().getConfig().get("spawners." + spawnerType + ".spawnerInfo.sell_button") == null || Main.getPlugin().getConfig().getBoolean("spawners." + spawnerType + ".spawnerInfo.sell_button")) {
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
                text = text.replace("{money}", String.valueOf(spawnerMoney));
                sellLoreReal.add(ChatColor.translateAlternateColorCodes('&', text));
            }
            sellItemMeta.setLore(sellLoreReal);
            sellItemMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            sellItem.setItemMeta(sellItemMeta);
            inventory.setItem(slot, sellItem);
        }
    }

    private void setupStoredItems(List<String> storedItems, int storageLimit) {
        AtomicInteger storedItemSlot = new AtomicInteger();

        storedItems.forEach(itemName -> {
            ItemStack storedItem = XMaterial.valueOf(itemName).parseItem();
            ItemMeta itemMeta = storedItem.getItemMeta();

            List<String> storedItemLore = Main.getPlugin().lang.getStringList("storageMenu.itemLores");
            List<String> storedItemLoreReal = new ArrayList<>();

            Main.database.getSpawnerStoredByItem(block, storedItem.getType()).thenAccept(itemAmount -> {
                for (String text : storedItemLore) {
                    text = text.replace("{amount_type}", String.valueOf(itemAmount));
                    text = text.replace("{storage_limit}", String.valueOf(storageLimit));
                    storedItemLoreReal.add(ChatColor.translateAlternateColorCodes('&', text));
                }

                itemMeta.setLore(storedItemLoreReal);
                itemMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
                storedItem.setItemMeta(itemMeta);
                inventory.setItem(storedItemSlot.getAndIncrement(), storedItem);
            });
        });
    }

    public void fillerItems() {
        Bukkit.getScheduler().runTask(Main.getPlugin(), () -> {
            if (block != null) {
                if (Main.getPlugin().lang.get("storageMenu.fillItem.material") != null && !Main.getPlugin().lang.getString("storageMenu.fillItem.material").equalsIgnoreCase("AIR")) {

                    ItemStack fillerItem = XMaterial.valueOf(Main.getPlugin().lang.getString("storageMenu.fillItem.material")).parseItem();
                    ItemMeta meta = fillerItem.getItemMeta();
                    String itemName = Main.getPlugin().lang.getString("storageMenu.fillItem.name") != null ? ChatColor.translateAlternateColorCodes('&', Main.getPlugin().lang.getString("storageMenu.fillItem.name")) : "no name";
                    if (!itemName.equalsIgnoreCase("no name")) {
                        meta.setDisplayName(itemName);
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
        Bukkit.getScheduler().runTask(Main.getPlugin(), () -> player.openInventory(inventory));
    }
}
