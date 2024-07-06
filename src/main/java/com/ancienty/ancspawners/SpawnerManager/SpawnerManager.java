package com.ancienty.ancspawners.SpawnerManager;

import com.ancienty.ancspawners.Listeners.SpawnerSpawnListener;
import com.ancienty.ancspawners.Main;
import com.ancienty.ancspawners.Versions.Holograms.SpawnerHologram_General;
import eu.decentsoftware.holograms.api.DHAPI;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

import static com.ancienty.ancspawners.Database.SQLite.dataSource;

public class SpawnerManager implements Listener {

    private static final int NUM_BATCHES = 5;
    private static final int MAX_RECURSION_DEPTH = 10;
    private static final int DEFAULT_SPAWN_DELAY = 20;

    private final List<ancSpawner> spawnerList = new CopyOnWriteArrayList<>();
    public final List<ancSpawner> updatedSpawnerList = new CopyOnWriteArrayList<>();
    private final Map<ancSpawner, Long> lastSpawnTime = new ConcurrentHashMap<>();
    private volatile boolean areSpawnersLoaded = false;
    public Wolf tamedWolf;
    public final Map<UUID, ancSpawner> entityLink = new ConcurrentHashMap<>();

    public void loadSpawners() {
        final int BATCH_SIZE = 20; // Number of spawners to load per batch

        Bukkit.getScheduler().runTaskAsynchronously(Main.getPlugin(), () -> {
            int currentBatch = 0;

            try (Connection connection = dataSource.getConnection()) {
                Main.getPlugin().getLogger().info("Spawner data are being loaded asynchronously.");

                while (true) {
                    String query = "SELECT * FROM spawners LIMIT ? OFFSET ?";
                    try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
                        preparedStatement.setInt(1, BATCH_SIZE);
                        preparedStatement.setInt(2, currentBatch * BATCH_SIZE);

                        try (ResultSet resultSet = preparedStatement.executeQuery()) {
                            if (!resultSet.next()) {
                                break; // No more spawners to load
                            }

                            do {
                                World world = Bukkit.getWorld(resultSet.getString("world"));
                                if (world == null) continue;

                                String locationData = resultSet.getString("location");
                                String[] locationParts = locationData.split(" ");
                                double x = Double.parseDouble(locationParts[0].split(":")[1]);
                                double y = Double.parseDouble(locationParts[1].split(":")[1]);
                                double z = Double.parseDouble(locationParts[2].split(":")[1]);
                                Location location = new Location(world, x, y, z);

                                int level = resultSet.getInt("level");
                                String ownerUUID = resultSet.getString("uuid");
                                String type = resultSet.getString("type");
                                String mode = resultSet.getString("mode");
                                boolean autokill = resultSet.getBoolean("autokill");
                                int storageLimit = resultSet.getInt("storage_limit");

                                ancSpawner spawner = new ancSpawner(world, location, level, ownerUUID, type, mode, autokill, storageLimit);
                                spawner.loadFriendsUUID(connection, locationData);
                                spawnerList.add(spawner);
                            } while (resultSet.next());
                        }
                    }

                    currentBatch++;
                }

                // Switch back to main thread for post-loading tasks:
                Bukkit.getScheduler().runTask(Main.getPlugin(), () -> {
                    areSpawnersLoaded = true;
                    Main.getPlugin().getLogger().info("Spawner data has been loaded.");
                    runSpawners();
                    tameWolfForKills();
                    autoSaveSpawners();
                });

            } catch (SQLException ex) {
                Main.getPlugin().getLogger().severe("Async spawner loading error: " + ex.getMessage());
                ex.printStackTrace();
            }
        });
    }

    public String getLocationString(Location location) {
        return "x:" + location.getBlockX() + " y:" + location.getBlockY() + " z:" + location.getBlockZ();
    }


    public void saveSpawnersNow() {
        Logger logger = Main.getPlugin().getLogger();
        save(logger);
    }

    public String getLocation(ancSpawner spawner) {
        String location_x = String.valueOf(spawner.getLocation().getBlockX());
        String location_y = String.valueOf(spawner.getLocation().getBlockY());
        String location_z = String.valueOf(spawner.getLocation().getBlockZ());
        return "x:" + location_x + " y:" + location_y + " z:" + location_z;
    }

    private void save(Logger logger) {
        logger.info("Starting auto-save of " + updatedSpawnerList.size() + " new spawner information.");
        List<ancSpawner> spawners_done = new ArrayList<>();
        for (ancSpawner spawner : updatedSpawnerList) {
            if (spawners_done.contains(spawner)) return;
            spawners_done.add(spawner);
            String spawnerQuery = "INSERT OR REPLACE INTO spawners (world, location, uuid, mode, type, level, autokill, storage_limit) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
            String deleteFriendsQuery = "DELETE FROM friends WHERE world = ? AND location = ?";
            String friendQuery = "INSERT INTO friends (world, location, uuid) VALUES (?, ?, ?)";
            String deleteStorageQuery = "DELETE FROM storage WHERE world = ? AND location = ?";
            String storageQuery = "INSERT INTO storage (world, location, item, amount) VALUES (?, ?, ?, ?)";
            String deleteStorageXpQuery = "DELETE FROM storage_xp WHERE world = ? AND location = ?";
            String storageXpQuery = "INSERT INTO storage_xp (world, location, xp) VALUES (?, ?, ?)";

            try (Connection connection = dataSource.getConnection()) {
                // Save spawner details
                try (PreparedStatement statement = connection.prepareStatement(spawnerQuery)) {
                    statement.setString(1, spawner.getWorld().getName());
                    statement.setString(2, getLocation(spawner));
                    statement.setString(3, spawner.getOwnerUUID());
                    statement.setString(4, spawner.getMode());
                    statement.setString(5, spawner.getType());
                    statement.setInt(6, spawner.getLevel());
                    statement.setBoolean(7, spawner.getAutoKill());
                    statement.setInt(8, spawner.getStorageLimit());
                    statement.executeUpdate();
                }

                // Delete and save friends
                try (PreparedStatement deleteStatement = connection.prepareStatement(deleteFriendsQuery)) {
                    deleteStatement.setString(1, spawner.getWorld().getName());
                    deleteStatement.setString(2, getLocation(spawner));
                    deleteStatement.executeUpdate();
                }
                try (PreparedStatement statement = connection.prepareStatement(friendQuery)) {
                    for (String uuid : spawner.getFriendUuids()) {
                        statement.setString(1, spawner.getWorld().getName());
                        statement.setString(2, getLocation(spawner));
                        statement.setString(3, uuid);
                        statement.addBatch();
                    }
                    statement.executeBatch();
                }

                // Delete and save storage
                try (PreparedStatement deleteStatement = connection.prepareStatement(deleteStorageQuery)) {
                    deleteStatement.setString(1, spawner.getWorld().getName());
                    deleteStatement.setString(2, getLocation(spawner));
                    deleteStatement.executeUpdate();
                }
                try (PreparedStatement statement = connection.prepareStatement(storageQuery)) {
                    for (Map.Entry<Material, Integer> entry : spawner.getStorage().getStorage().entrySet()) {
                        statement.setString(1, spawner.getWorld().getName());
                        statement.setString(2, getLocation(spawner));
                        statement.setString(3, entry.getKey().toString());
                        statement.setInt(4, entry.getValue());
                        statement.addBatch();
                    }
                    statement.executeBatch();
                }

                // Delete and save storage XP
                try (PreparedStatement deleteStatement = connection.prepareStatement(deleteStorageXpQuery)) {
                    deleteStatement.setString(1, spawner.getWorld().getName());
                    deleteStatement.setString(2, getLocation(spawner));
                    deleteStatement.executeUpdate();
                }
                try (PreparedStatement statement = connection.prepareStatement(storageXpQuery)) {
                    statement.setString(1, spawner.getWorld().getName());
                    statement.setString(2, getLocation(spawner));
                    statement.setInt(3, spawner.getStorage().getStoredXp());
                    statement.executeUpdate();
                }

            } catch (SQLException ex) {
                ex.printStackTrace();
            }

        }
        logger.info("Spawner auto save has been successful.");
    }

    private void autoSaveSpawners() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(Main.getPlugin(), () -> {
            Logger logger = Main.getPlugin().getLogger();
            save(logger);
        }, 6000, 6000);
    }


    public void tameWolfForKills() {
        List<Player> onlinePlayers = new ArrayList<>(Bukkit.getOnlinePlayers());
        if (!onlinePlayers.isEmpty()) {
            Player owner = onlinePlayers.get(new Random().nextInt(onlinePlayers.size()));
            World world = owner.getWorld();
            Location spawnLocation = new Location(world, 0, 1, 0);

            // Set the surrounding blocks to air to prevent suffocation
            for (int x = -1; x <= 1; x++) {
                for (int y = -1; y <= 1; y++) {
                    for (int z = -1; z <= 1; z++) {
                        Location loc = spawnLocation.clone().add(x, y, z);
                        if (world.getBlockAt(loc).getType() != Material.AIR) {
                            world.getBlockAt(loc).setType(Material.AIR);
                        }
                    }
                }
            }

            tamedWolf = (Wolf) world.spawnEntity(spawnLocation, EntityType.WOLF);
            //tamedWolf.setOwner(owner);
            tamedWolf.setTamed(true);
            tamedWolf.setSitting(true);
            tamedWolf.setAI(false);
        }
    }

    @EventHandler
    public void onPlayerJoinEvent(PlayerJoinEvent e) {
        // Optionally handle player join event
    }

    public boolean checkSpawnConditions(ancSpawner ancSpawner) {
        Location location = ancSpawner.getLocation();
        double range = 16;

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getWorld().equals(location.getWorld())) {
                if (location.distance(player.getLocation()) <= range) {
                    return true;
                }
            }
        }
        return false;
    }


    public Location getNewLocation(Location originalLocation, int recursionDepth) {
        if (recursionDepth > MAX_RECURSION_DEPTH) {
            return originalLocation;
        }

        World world = originalLocation.getWorld();
        int originalX = originalLocation.getBlockX();
        int originalY = originalLocation.getBlockY();
        int originalZ = originalLocation.getBlockZ();

        int offsetX = new Random().nextInt(2 * 2 + 1) - 2;
        int offsetY = new Random().nextInt(2 * 2 + 1) - 2;
        int offsetZ = new Random().nextInt(2 * 2 + 1) - 2;

        Location newLocation = new Location(world, originalX + offsetX, originalY + offsetY, originalZ + offsetZ);
        Block block = world.getBlockAt(newLocation);

        if (block.getType() == Material.AIR) {
            return newLocation;
        } else {
            return getNewLocation(originalLocation, recursionDepth + 1);
        }
    }

    private List<List<ancSpawner>> splitSpawnerListIntoBatches(List<ancSpawner> spawners, int numBatches) {
        List<List<ancSpawner>> batches = new ArrayList<>();
        int batchSize = spawners.size() / numBatches;
        int remainder = spawners.size() % numBatches;

        int start = 0;
        for (int i = 0; i < numBatches; i++) {
            int end = start + batchSize;
            if (i < remainder) {
                end++;
            }
            batches.add(new ArrayList<>(spawners.subList(start, end)));
            start = end;
        }
        return batches;
    }

    public int getSpawnInterval(String spawnerType) {
        return Main.getPlugin().getConfig().getInt("spawners." + spawnerType + ".spawnerInfo.delay", DEFAULT_SPAWN_DELAY);
    }

    public String getHologramName(ancSpawner spawner) {
        String world = spawner.getWorld().getName();
        String location = getLocation(spawner);
        String return_value = world + location;
        return_value = return_value.replace(" ", "");
        return_value = return_value.replace(":", "");
        return return_value;
    }

    public void runSpawners() {
        Random random = new Random();
        Bukkit.getScheduler().runTaskTimerAsynchronously(Main.getPlugin(), () -> {
            long currentTime = System.currentTimeMillis();
            List<List<ancSpawner>> batches = splitSpawnerListIntoBatches(new ArrayList<>(spawnerList), NUM_BATCHES);

            for (List<ancSpawner> batch : batches) {
                Bukkit.getScheduler().runTaskAsynchronously(Main.getPlugin(), () -> {
                    for (ancSpawner spawner : batch) {
                        if (random.nextInt(101) <= 10) {
                            if (DHAPI.getHologram(getHologramName(spawner)) != null) {
                                SpawnerHologram_General.updateSpawnerHologram(spawner, getHologramName(spawner));
                            }
                        }
                        if (spawner.getLocation().getChunk().isLoaded() && checkSpawnConditions(spawner) && spawner.getMode().equalsIgnoreCase("entity")) {
                            long lastSpawn = lastSpawnTime.getOrDefault(spawner, 0L);
                            int spawnInterval = getSpawnInterval(spawner.getType());
                            if (currentTime - lastSpawn >= spawnInterval * 1000L) {
                                Location spawnLocation = getNewLocation(spawner.getLocation(), 10);
                                EntityType entityType = getEntityType(spawner.getType());
                                if (entityType != null) {
                                    Bukkit.getScheduler().runTask(Main.getPlugin(), () -> {
                                        LivingEntity spawnedEntity = (LivingEntity) spawner.getWorld().spawnEntity(spawnLocation, entityType);
                                        if (spawner.getAutoKill()) {
                                            entityLink.put(spawnedEntity.getUniqueId(), spawner);
                                            if (tamedWolf != null && tamedWolf.isValid()) {
                                                spawnedEntity.damage(spawnedEntity.getHealth() + 20, tamedWolf);
                                            } else {
                                                tameWolfForKills();
                                                spawnedEntity.damage(spawnedEntity.getHealth() + 20, tamedWolf);
                                            }
                                        } else {
                                            SpawnerSpawnListener.entityLinkToSpawners.put(spawnedEntity.getUniqueId(), spawner);
                                        }
                                    });
                                    lastSpawnTime.put(spawner, currentTime);
                                }
                            }
                        }
                    }
                });
            }
        }, 20, 20);
    }


    public Map<UUID, ancSpawner> getEntityLink() {
        return entityLink;
    }

    public EntityType getEntityType(String type) {
        try {
            String material = Main.getPlugin().getConfig().getString("spawners." + type + ".spawnerInfo.material");
            if (material == null) {
                Main.getPlugin().getLogger().severe("Error getting spawner material of the spawner: " + type);
                return null;
            }
            return EntityType.valueOf(material);
        } catch (IllegalArgumentException ex) {
            Main.getPlugin().getLogger().severe("Error getting spawner material of the spawner: " + type + " - Complete error message: " + ex.getMessage());
            return null;
        }
    }

    public ancSpawner getSpawner(World world, Location location) {
        return spawnerList.stream()
                .filter(spawner -> spawner.getWorld().equals(world) &&
                        spawner.getLocation().equals(location))
                .findFirst()
                .orElse(null);
    }

    public void removeSpawner(ancSpawner ancSpawner) {
        Main.database.deleteSpawner(ancSpawner.getWorld(), ancSpawner.getLocation());
        spawnerList.removeIf(spawner -> spawner.equals(ancSpawner));
        updatedSpawnerList.removeIf(spawner -> spawner.equals(ancSpawner));
    }

    public void addSpawner(ancSpawner ancSpawner) {
        spawnerList.add(ancSpawner);
    }
}
