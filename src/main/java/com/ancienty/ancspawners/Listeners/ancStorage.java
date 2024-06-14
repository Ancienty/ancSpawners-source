package com.ancienty.ancspawners.Listeners;

import com.ancienty.ancspawners.Main;
import com.cryptomorin.xseries.XMaterial;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

public class ancStorage {

    private final HashMap<ItemStack, Integer> storage = new HashMap<>();

    public ancStorage(World world, Location location) {
        Block block = world.getBlockAt(location);
        loadStoredItems(block).join();
    }

    private CompletableFuture<Void> loadStoredItems(Block block) {
        return Main.database.getStoredItems(block).thenCompose(storedItems -> {
            CompletableFuture<?>[] futures = storedItems.stream()
                    .map(itemName -> {
                        Material material = XMaterial.valueOf(itemName).parseMaterial();
                        if (material != null) {
                            return Main.database.getSpawnerStoredByItem(block, material).thenAccept(storedByItem -> {
                                storage.put(new ItemStack(material), storedByItem);
                            });
                        }
                        return null;
                    })
                    .filter(future -> future != null)
                    .toArray(CompletableFuture[]::new);
            return CompletableFuture.allOf(futures);
        });
    }

    public synchronized HashMap<ItemStack, Integer> getStorage() {
        return new HashMap<>(storage); // Return a copy to avoid external modification
    }

    public synchronized void addItem(ItemStack item, int amount) {
        storage.merge(item, amount, Integer::sum);
    }

    public synchronized void deleteItem(ItemStack item) {
        storage.remove(item);
    }

    public synchronized void removeFromStorage(ItemStack item, int amount) {
        storage.computeIfPresent(item, (key, oldAmount) -> {
            int newAmount = oldAmount - amount;
            return (newAmount > 0) ? newAmount : null;
        });
    }
}
