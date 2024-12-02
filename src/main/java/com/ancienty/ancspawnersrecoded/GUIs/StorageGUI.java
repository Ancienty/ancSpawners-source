package com.ancienty.ancspawnersrecoded.GUIs;

import com.ancienty.ancspawnersrecoded.Main;
import com.ancienty.ancspawnersrecoded.SpawnerManager.ancSpawner;
import com.ancienty.ancspawnersrecoded.SpawnerManager.ancStorage;
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

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
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
            Main.getPlugin().player_block_map.put(this.player, this.block);
            int size = Main.getPlugin().lang.getInt("storageMenu.menuRows");
            String name = ChatColor.translateAlternateColorCodes('&', Main.getPlugin().lang.getString("storageMenu.menuName"));
            inventory = Bukkit.createInventory(null, size * 9, name);

            initializeItems().thenAccept(unused -> openInventory());
        }
    }

    public CompletableFuture<Void> initializeItems() {
        ancSpawner spawner = Main.getPlugin().getSpawnerManager().getSpawner(block.getWorld(), block.getLocation());
        if (spawner != null) {
            ancStorage storage = spawner.getStorage();
            double spawnerMoney = storage.getMoney(player);
            Set<ItemStack> stored_items = storage.getStorage().keySet();
            int storageLimit = spawner.getStorageLimit();
            String spawnerType = spawner.getType();

            return CompletableFuture.runAsync(() -> {
                Bukkit.getScheduler().runTask(Main.getPlugin(), () -> {
                    setupSellItem(spawnerMoney, spawnerType);
                    setupStoredItems(stored_items, storageLimit, storage);
                    fillerItems();
                });
            });
        } else {
            CompletableFuture<Void> future = new CompletableFuture<>();
            future.completeExceptionally(new Exception("Spawner not found for block: " + block));
            return future;
        }
    }

    private void setupSellItem(Double spawnerMoney, String spawnerType) {
        ancSpawner spawner = Main.getPlugin().getSpawnerManager().getSpawner(block.getWorld(), block.getLocation());
        boolean sell_button;
        if (spawner.getConfigName() == null || spawner.getConfigName().equalsIgnoreCase("")) {
            sell_button = Main.getPlugin().getConfig().getBoolean("spawners.default.spawnerInfo.sell_button");
        } else {
            sell_button = Main.getPlugin().getConfig().getBoolean("spawners." + spawner.getConfigName() + ".spawnerInfo.sell_button");
        }
        if (sell_button) {
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

    private void setupStoredItems(Set<ItemStack> storedItems, int storageLimit, ancStorage storage) {
        AtomicInteger storedItemSlot = new AtomicInteger();
        DecimalFormat formatter = new DecimalFormat("#,###");

        String formattedStorageLimit = formatter.format(storageLimit);

        storedItems.forEach(storedItemm -> {
            ItemStack storedItem = storedItemm.clone();
            ItemMeta itemMeta = storedItem.getItemMeta();

            List<String> storedItemLoreReal = itemMeta.hasLore() ? new ArrayList<>(itemMeta.getLore()) : new ArrayList<>();
            List<String> storedItemLore = Main.getPlugin().lang.getStringList("storageMenu.itemLores");

            int itemAmount = storage.getStoredItem(storedItem);
            String formattedItemAmount = formatter.format(itemAmount);

            for (String text : storedItemLore) {
                text = text.replace("{amount_type}", formattedItemAmount);
                text = text.replace("{storage_limit}", formattedStorageLimit);
                text = text.replace("{storage_bar}", Main.getPlugin().getStorageBarOfItem(storedItem, storage.getSpawner()));
                storedItemLoreReal.add(ChatColor.translateAlternateColorCodes('&', text));
            }

            itemMeta.setLore(storedItemLoreReal);
            itemMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            storedItem.setItemMeta(itemMeta);
            inventory.setItem(storedItemSlot.getAndIncrement(), storedItem);
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
