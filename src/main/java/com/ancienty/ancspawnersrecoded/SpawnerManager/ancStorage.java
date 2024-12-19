package com.ancienty.ancspawnersrecoded.SpawnerManager;

import com.ancienty.ancspawnersrecoded.Database.DatabaseTask;
import com.ancienty.ancspawnersrecoded.Main;
import com.ancienty.ancspawnersrecoded.Utils.ItemStackUtils;
import com.cryptomorin.xseries.XMaterial;
import com.google.gson.Gson;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ancStorage {

    private final Map<ItemStack, Integer> storage = new ConcurrentHashMap<>();
    private Integer stored_xp = 0;
    private final ancSpawner spawner;
    private final Gson gson = new Gson();

    public ancStorage(World world, Location location, ancSpawner spawner) {
        this.spawner = spawner;
        loadStorageFromDatabase(world, location);
    }

    private void loadStorageFromDatabase(World world, Location location) {
        String query = "SELECT * FROM storage WHERE world = ? AND location = ?";
        DatabaseTask loadStorageTask = new DatabaseTask(query, new Object[]{
                world.getName(),
                Main.getPlugin().getSpawnerManager().getLocation(location)
        });

        Main.getPlugin().getSqlProcessing().executeDatabaseQuery(loadStorageTask, resultSet -> {
            try {
                while (resultSet.next()) {
                    String itemData = resultSet.getString("item");
                    Integer amount = resultSet.getInt("amount");

                    ItemStack itemStack;
                    try {
                        itemStack = ItemStackUtils.itemStackFromBase64(itemData);
                    } catch (IOException | ClassNotFoundException e) {
                        e.printStackTrace();
                        continue; // Skip this item if deserialization fails
                    }
                    storage.put(itemStack, amount);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        // Load stored XP
        String xpQuery = "SELECT * FROM storage_xp WHERE world = ? AND location = ?";
        DatabaseTask loadXpTask = new DatabaseTask(xpQuery, new Object[]{
                world.getName(),
                Main.getPlugin().getSpawnerManager().getLocation(location)
        });

        Main.getPlugin().getSqlProcessing().executeDatabaseQuery(loadXpTask, resultSet -> {
            try {
                if (resultSet.next()) {
                    stored_xp = resultSet.getInt("xp");
                } else {
                    stored_xp = 0;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public void addToUpdatedSpawnersList() {
        if (!Main.getPlugin().getSpawnerManager().getUpdatedSpawners().contains(spawner)) {
            Main.getPlugin().getSpawnerManager().getUpdatedSpawners().add(spawner);
        }
    }

    public Map<ItemStack, Integer> getStorage() {
        return storage;
    }

    public Integer getStoredItem(ItemStack item) {
        return storage.getOrDefault(item, 0);
    }

    public Map<ItemStack, Integer> getStoredItemByNameAndType(ItemStack itemGiven) {
        boolean itemGivenHasDisplayName = itemGiven.hasItemMeta() && itemGiven.getItemMeta().hasDisplayName();
        String itemGivenDisplayName = itemGivenHasDisplayName ? itemGiven.getItemMeta().getDisplayName() : null;

        // Get the XMaterial of itemGiven
        XMaterial itemGivenXMaterial = XMaterial.matchXMaterial(itemGiven);

        for (ItemStack itemStack : storage.keySet()) {
            boolean itemStackHasDisplayName = itemStack.hasItemMeta() && itemStack.getItemMeta().hasDisplayName();
            String itemStackDisplayName = itemStackHasDisplayName ? itemStack.getItemMeta().getDisplayName() : null;

            // Get the XMaterial of itemStack
            XMaterial itemStackXMaterial = XMaterial.matchXMaterial(itemStack);

            // Compare XMaterials first
            if (itemGivenXMaterial != itemStackXMaterial) {
                continue; // Materials do not match
            }

            if (itemGivenHasDisplayName) {
                // itemGiven has a display name; compare display names
                if (itemStackHasDisplayName && itemGivenDisplayName.equals(itemStackDisplayName)) {
                    // Both have display names and they match
                    Map<ItemStack, Integer> result = new HashMap<>();
                    result.put(itemStack, storage.get(itemStack));
                    return result;
                }
            } else {
                // itemGiven does not have a display name
                if (!itemStackHasDisplayName) {
                    // Both do not have display names
                    Map<ItemStack, Integer> result = new HashMap<>();
                    result.put(itemStack, storage.get(itemStack));
                    return result;
                }
            }
        }
        // No matching item found
        return null;
    }

    public void setStoredItem(ItemStack item, int amount) {
        if (amount > spawner.getStorageLimit()) {
            amount = spawner.getStorageLimit();
        }
        if (amount <= 0) {
            storage.remove(item);
        } else {
            storage.put(item, amount);
        }
        addToUpdatedSpawnersList();
    }

    public Integer getStoredXp() {
        return stored_xp != null ? stored_xp : 0;
    }

    public void setStoredXp(Integer xp) {
        stored_xp = xp;
        addToUpdatedSpawnersList();
    }

    public int getTotalStored() {
        return storage.values().stream().mapToInt(Integer::intValue).sum();
    }

    public double getMoney(Player player) {
        double totalMoney = Main.getPlugin().getPlayerMultiplier(player) * storage.entrySet().stream()
                .mapToDouble(entry -> Main.getPlugin().getPriceForItem(entry.getKey().getType().toString().toUpperCase(Locale.ENGLISH)) * entry.getValue())
                .sum();

        return BigDecimal.valueOf(totalMoney).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    public void clearStorage() {
        storage.clear();
        addToUpdatedSpawnersList();
    }

    public ancSpawner getSpawner() {
        return this.spawner;
    }
}
