package com.ancienty.ancspawnersrecoded.SpawnerManager;

import com.ancienty.ancspawnersrecoded.Database.DatabaseTask;
import com.ancienty.ancspawnersrecoded.Main;
import com.ancienty.ancspawnersrecoded.Utils.BoostInfo;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ancBoosts {

    private final World world;
    private final Location location;
    private final ancSpawner spawner;
    private final Map<String, BoostInfo> activeBoosts = new ConcurrentHashMap<>();

    public ancBoosts(World world, Location location, ancSpawner spawner) {
        this.world = world;
        this.location = location;
        this.spawner = spawner;
        Bukkit.getScheduler().runTaskTimer(Main.getPlugin(), this::loadActiveBoosts, 0, 6000);
    }

    /**
     * Loads active boosts from the database, removes expired boosts.
     */
    public void loadActiveBoosts() {
        activeBoosts.clear();
        String query = "SELECT * FROM boosts WHERE world = ? AND location = ?";
        DatabaseTask selectBoostsTask = new DatabaseTask(query, new Object[]{
                world.getName(),
                Main.getPlugin().getSpawnerManager().getLocation(location),
        });

        Main.getPlugin().executeDatabaseQuery(selectBoostsTask, resultSet -> {
            List<String> expiredBoostTypes = new ArrayList<>();
            try {
                while (resultSet.next()) {
                    long endDate = resultSet.getLong("end_date");
                    long currentDate = System.currentTimeMillis();
                    String boostType = resultSet.getString("boost_type").toLowerCase();
                    double boostAmount = resultSet.getDouble("boost_amount");
                    if (currentDate > endDate) {
                        expiredBoostTypes.add(boostType);
                    } else {
                        activeBoosts.put(boostType, new BoostInfo(boostAmount, endDate));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            // Remove expired boosts from the database
            for (String boostType : expiredBoostTypes) {
                removeExpiredBoost(boostType);
            }
        });
    }

    /**
     * Removes an expired boost from the database.
     */
    private void removeExpiredBoost(String boostType) {
        String deleteQuery = "DELETE FROM boosts WHERE world = ? AND location = ? AND boost_type = ?";
        DatabaseTask deleteBoostTask = new DatabaseTask(deleteQuery, new Object[]{
                world.getName(),
                Main.getPlugin().getSpawnerManager().getLocation(location),
                boostType
        });
        Main.getPlugin().addDatabaseTask(deleteBoostTask);
    }

    /**
     * Gets the current active boosts.
     */
    public Map<String, BoostInfo> getActiveBoosts() {
        return activeBoosts;
    }

    /**
     * Gets the spawn amount boost value.
     */
    public double getSpawnAmountBoost() {
        BoostInfo info = activeBoosts.get("spawn_amount");
        return info != null ? info.getAmount() : 0.0;
    }

    /**
     * Gets the spawn time boost value.
     */
    public double getSpawnTimeBoost() {
        BoostInfo info = activeBoosts.get("spawn_time");
        return info != null ? info.getAmount() : 1.0;
    }

    /**
     * Gets the sell multiplier boost value.
     */
    public double getSellMultiplierBoost() {
        BoostInfo info = activeBoosts.get("sell_multiplier");
        return info != null ? info.getAmount() : 1.0;
    }
}
