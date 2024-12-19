package com.ancienty.ancspawnersrecoded.SpawnerManager;

import com.ancienty.ancspawnersrecoded.Database.DatabaseTask;
import com.ancienty.ancspawnersrecoded.Main;
import com.ancienty.ancspawnersrecoded.Support.Editors.CustomGetters.CustomGetters;
import com.ancienty.ancspawnersrecoded.Support.Editors.CustomGetters.DefaultGetter;
import com.ancienty.ancspawnersrecoded.Support.Editors.CustomGetters.ItemsAdderGetter;
import com.ancienty.ancspawnersrecoded.Support.Editors.HologramEditor;
import com.ancienty.ancspawnersrecoded.Support.Other.WildLoadersSupport;
import com.ancienty.ancspawnersrecoded.Utils.ItemStackUtils;
import com.cryptomorin.xseries.XMaterial;
import com.google.gson.Gson;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

public class SpawnerManager {

    private final List<ancSpawner> spawners_list = new CopyOnWriteArrayList<>();
    private final List<ancSpawner> updated_spawners_list = Collections.synchronizedList(new ArrayList<>());

    private final Map<ancSpawner, Long> last_spawn_times = new ConcurrentHashMap<>();
    public final Map<String, Integer> config_to_delay_cache = new ConcurrentHashMap<>();
    public final Map<String, ItemStack> itemStackCache = new HashMap<>();
    private final Map<String, String> entityTypeCache = new ConcurrentHashMap<>();
    private final Map<String, Double> rangeCache = new ConcurrentHashMap<>();

    public Wolf tamedWolf;
    public final Map<UUID, ancSpawner> entityLink = new ConcurrentHashMap<>();

    public SpawnerManager() {
        tameWolfForKills();
        loadSpawners();
        startAutoSaveTask();
    }

    public List<ancSpawner> getSpawners() {
        return spawners_list;
    }

    public List<ancSpawner> getUpdatedSpawners() {
        return updated_spawners_list;
    }

    private void loadSpawners() {
        // Initialize counts
        AtomicInteger loadedSpawners = new AtomicInteger(0);
        AtomicInteger skippedSpawners = new AtomicInteger(0);

        // Load spawners from the database asynchronously
        String query = "SELECT * FROM spawners";
        DatabaseTask loadSpawnersTask = new DatabaseTask(query, new Object[]{});

        Main.getPlugin().getSqlProcessing().executeDatabaseQuery(loadSpawnersTask, resultSet -> {
            try {
                while (resultSet.next()) {
                    try {
                        World world = Bukkit.getWorld(resultSet.getString("world"));
                        if (world == null) {
                            skippedSpawners.incrementAndGet();
                            Main.getPlugin().getAncLogger().writeError("Skipped spawner at location " + resultSet.getString("location") + " due to missing world.");
                            continue;
                        }
                        Location location = parseLocation(resultSet.getString("location"), world);
                        String ownerUUID = resultSet.getString("owner_uuid");
                        String spawnerMode = resultSet.getString("spawner_mode");
                        String spawnerMaterial = resultSet.getString("spawner_material");
                        int level = resultSet.getInt("level");
                        boolean autoKill = resultSet.getBoolean("auto_kill");
                        int storageLimit = resultSet.getInt("storage_limit");
                        String configName = resultSet.getString("config_name");

                        boolean virtualStorage = resultSet.getBoolean("virtual_storage");
                        boolean xpStorage = resultSet.getBoolean("xp_storage");

                        createSpawner(world, location, configName, level, ownerUUID, spawnerMaterial, spawnerMode, autoKill, storageLimit, virtualStorage, xpStorage);

                        loadedSpawners.incrementAndGet();
                    } catch (Exception e) {
                        skippedSpawners.incrementAndGet();
                        Main.getPlugin().getAncLogger().writeError("Error loading spawner at location " + resultSet.getString("location") + ": " + e.getMessage());
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                // After loading is complete, print the messages
                Main.getPlugin().getLogger().info(loadedSpawners.get() + " spawners have been loaded successfully.");
                Main.getPlugin().getLogger().info(skippedSpawners.get() + " spawners skipped due to errors, errors can be found in the logs folder of the plugin.");

                // Run spawners
                runSpawners();
            }
        });
    }


    private Location parseLocation(String locationString, World world) {
        String[] parts = locationString.split(" ");
        double x = Double.parseDouble(parts[0].split(":")[1]);
        double y = Double.parseDouble(parts[1].split(":")[1]);
        double z = Double.parseDouble(parts[2].split(":")[1]);
        return new Location(world, x, y, z);
    }

    public Map<UUID, ancSpawner> getEntityLink() {
        return entityLink;
    }

    public int getDelay(@Nullable String config_name) {
        String key = config_name != null ? config_name : "default";
        return config_to_delay_cache.computeIfAbsent(key, k -> {
            if (k.equals("default")) {
                // Fetch the default delay directly
                return Main.getPlugin().getConfig().getInt("spawners.default.spawnerInfo.delay");
            } else {
                // Fetch the default delay without using computeIfAbsent to avoid recursion
                int defaultDelay = Main.getPlugin().getConfig().getInt("spawners.default.spawnerInfo.delay");
                // Get the delay for the specific config_name, or use the default delay if not found
                return Main.getPlugin().getConfig().getInt("spawners." + k + ".spawnerInfo.delay", defaultDelay);
            }
        });
    }

    public void runSpawners() {
        Random random = new Random();
        Bukkit.getScheduler().runTaskTimerAsynchronously(Main.getPlugin(), () -> {
            long currentTime = System.currentTimeMillis();

            if (spawners_list.isEmpty()) return;

            List<ancSpawner> spawnersToProcess;

            // Synchronize only to copy the list
            synchronized (spawners_list) {
                spawnersToProcess = new ArrayList<>(spawners_list);
            }

            List<ancSpawner> spawnersToSpawn = new ArrayList<>();

            // Process spawners asynchronously without holding the lock
            for (ancSpawner spawner : spawnersToProcess) {
                // Skip spawners in unloaded chunks
                if (!spawner.getLocation().getChunk().isLoaded()) continue;

                // Optimize loop conditions: check if it's time to spawn first
                long lastSpawnTime = last_spawn_times.getOrDefault(spawner, 0L);
                if (lastSpawnTime == 0) {
                    lastSpawnTime = currentTime;
                    last_spawn_times.put(spawner, currentTime);
                }

                int baseDelay = getDelay(spawner.getConfigName()) * 1000;

                ancBoosts boosts = spawner.getBoosts();

                double spawnTimeBoost = boosts.getSpawnTimeBoost();

                long adjustedDelay = (long) (baseDelay / spawnTimeBoost);

                if (currentTime - lastSpawnTime < adjustedDelay) continue;

                // Now check if the spawner block still exists
                if (spawner.getWorld().getBlockAt(spawner.getLocation()).getType() != XMaterial.SPAWNER.parseMaterial()) {
                    deleteSpawner(spawner);
                    continue;
                }

                // Check spawn conditions (e.g., player proximity)
                if (!checkSpawnConditions(spawner)) continue;

                spawnersToSpawn.add(spawner);
                // Update the last spawn time
                last_spawn_times.put(spawner, currentTime);
            }

            if (spawnersToSpawn.isEmpty()) return;

            // Schedule the spawning tasks on the main thread
            Bukkit.getScheduler().runTask(Main.getPlugin(), () -> {
                for (ancSpawner spawner : spawnersToSpawn) {
                    // Determine spawn amount
                    int spawnAmount = 1 + (int) spawner.getBoosts().getSpawnAmountBoost(); // Base amount is 1
                    if (random.nextDouble() * 100 < 5) {HologramEditor.updateHologram(spawner.getLocation().getBlock());}

                    if (spawner.getMode().equalsIgnoreCase("entity")) {
                        for (int i = 0; i < spawnAmount; i++) {
                            String type = !spawner.getConfigName().equalsIgnoreCase("default") && spawner.getConfigName() != null ? getEntityCache(spawner.getConfigName()) : spawner.getType();
                            LivingEntity entity = (LivingEntity) spawnEntity(type, Main.getPlugin().generateNewLocation(spawner.getLocation(), 0));
                            if (entity == null) continue;
                            if (spawner.getAutoKill()) {
                                entityLink.put(entity.getUniqueId(), spawner);
                                if (tamedWolf != null && tamedWolf.isValid()) {
                                    entity.damage(entity.getHealth() + 20, tamedWolf);
                                } else {
                                    tameWolfForKills();
                                    entity.damage(entity.getHealth() + 20, tamedWolf);
                                }
                            }
                        }
                    } else if (spawner.getMode().equalsIgnoreCase("item") || spawner.getMode().equalsIgnoreCase("ITEM")) {
                        int dropAmount = spawner.getLevel() * spawnAmount; // Apply spawnAmount boost

                        if (spawner.isVirtualStorageEnabled()) {
                            // Virtual storage can be updated asynchronously
                            ItemStack itemStack = getCachedItemStack(spawner.getConfigName());
                            if (itemStack != null) {
                                int currentAmount = spawner.getStorage().getStoredItem(itemStack);
                                spawner.getStorage().setStoredItem(itemStack, currentAmount + dropAmount);
                            }
                        } else {
                            // Dropping items must be done synchronously
                            ItemStack itemStack = getCachedItemStack(spawner.getConfigName());
                            if (itemStack != null) {
                                dropItemsNaturally(spawner.getWorld(), spawner.getLocation(), itemStack, dropAmount);
                            }
                        }
                    }
                }
            });
        }, 0L, 1L); // Adjust the period as needed
    }

    private String getEntityCache(String configName) {
        return entityTypeCache.computeIfAbsent(configName, key -> {
           if (configName.equalsIgnoreCase("default")) return null;
           return Main.getPlugin().getConfig().getString("spawners." + configName + ".spawnerInfo.material");
        });
    }

    private ItemStack getCachedItemStack(String configName) {
        return itemStackCache.computeIfAbsent(configName, key -> {
            String material_name = Main.getPlugin().getConfig().getString("spawners." + configName + ".spawnerInfo.material");
            ItemStack itemStack = getMaterial(material_name);
            if (itemStack == null) {
                return null;
            }

            // Handle custom item meta
            ItemMeta itemMeta = itemStack.getItemMeta();
            ConfigurationSection section = Main.getPlugin().getConfig().getConfigurationSection("spawners." + configName);

            if (section != null) {
                String customName = ChatColor.translateAlternateColorCodes('&', section.getString("spawnerInfo.details.name", ""));
                List<String> customLore = section.getStringList("spawnerInfo.details.lore");
                List<String> translatedLore = new ArrayList<>();
                for (String line : customLore) {
                    String translatedLine = ChatColor.translateAlternateColorCodes('&', line);
                    translatedLore.add(translatedLine);
                }

                itemMeta.setDisplayName(customName);
                itemMeta.setLore(translatedLore);
                itemStack.setItemMeta(itemMeta);
            }
            return itemStack;
        });
    }


    private void dropItemsNaturally(World world, Location location, ItemStack itemStack, int totalAmount) {
        int maxStackSize = itemStack.getMaxStackSize();
        while (totalAmount > 0) {
            int dropAmount = Math.min(totalAmount, maxStackSize);
            ItemStack dropStack = itemStack.clone();
            dropStack.setAmount(dropAmount);
            world.dropItemNaturally(Main.getPlugin().generateNewLocation(location, 0), dropStack);
            totalAmount -= dropAmount;
        }
    }

    public Double getRangeForSpawner(ancSpawner spawner) {
        String config_name = spawner.getConfigName();
        if (!rangeCache.isEmpty() && rangeCache.containsKey(config_name)) {
            return rangeCache.get(config_name);
        } else {
            if (config_name == null || config_name.equalsIgnoreCase("default")) {
                rangeCache.put("default", Main.getPlugin().getConfig().getDouble("spawners.default.spawnerInfo.range"));
            } else {
                rangeCache.put("default", Main.getPlugin().getConfig().getDouble("spawners." + config_name + ".spawnerInfo.range"));
            }
            return getRangeForSpawner(spawner);
        }
    }

    public boolean checkSpawnConditions(ancSpawner ancSpawner) {
        Location location = ancSpawner.getLocation();
        double range = getRangeForSpawner(ancSpawner);

        if (WildLoadersSupport.chunkHasActiveWildLoader(ancSpawner.getLocation().getChunk())) return true;

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getWorld().equals(location.getWorld())) {
                if (location.distance(player.getLocation()) <= range) {
                    return true;
                }
            }
        }
        return false;
    }

    public void tameWolfForKills() {
        Bukkit.getScheduler().runTask(Main.getPlugin(), () -> {
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
                tamedWolf.setTamed(true);
                tamedWolf.setSitting(true);
                tamedWolf.setAI(false);
            }
        });
    }

    public ancSpawner getSpawner(World world, Location location) {
        return spawners_list.stream()
                .filter(spawner -> spawner.getWorld().equals(world) &&
                        spawner.getLocation().equals(location))
                .findFirst()
                .orElse(null);
    }

    public String getLocation(ancSpawner spawner) {
        return "x:" + spawner.getLocation().getX() + " y:" + spawner.getLocation().getY() + " z:" + spawner.getLocation().getZ();
    }

    public String getLocation(Location location) {
        return "x:" + location.getX() + " y:" + location.getY() + " z:" + location.getZ();
    }

    public void saveNow(Logger logger) {
        save(logger);
    }

    private void startAutoSaveTask() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(Main.getPlugin(), () -> {
            Logger logger = Main.getPlugin().getLogger();
            save(logger); // Call the save method every 5 minutes
        }, 0L, 6000L); // 6000 ticks = 5 minutes
    }

    private void save(Logger logger) {
        // Log the start of the auto-save process
        Main.getPlugin().getLogger().info("Starting auto-save of " + updated_spawners_list.size() + " spawner(s).");
        List<ancSpawner> spawners_done = new ArrayList<>();
        for (ancSpawner spawner : updated_spawners_list) {
            if (spawners_done.contains(spawner)) continue;
            spawners_done.add(spawner);

            String spawnerQuery = "INSERT OR REPLACE INTO spawners (world, location, owner_uuid, spawner_mode, spawner_material, level, auto_kill, storage_limit, config_name, virtual_storage, xp_storage) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            String deleteFriendsQuery = "DELETE FROM friends WHERE world = ? AND location = ?";
            String friendQuery = "INSERT INTO friends (world, location, uuid) VALUES (?, ?, ?)";
            String deleteStorageQuery = "DELETE FROM storage WHERE world = ? AND location = ?";
            String storageQuery = "INSERT INTO storage (world, location, item, amount) VALUES (?, ?, ?, ?)";
            String storageXpQuery = "INSERT OR REPLACE INTO storage_xp (world, location, xp) VALUES (?, ?, ?)";

            // Save spawner details
            DatabaseTask spawnerTask = new DatabaseTask(spawnerQuery, new Object[]{
                    spawner.getWorld().getName(),
                    getLocation(spawner),
                    spawner.getOwnerUUID(),
                    spawner.getMode(),
                    spawner.getType(),
                    spawner.getLevel(),
                    spawner.getAutoKill(),
                    spawner.getStorageLimit(),
                    spawner.getConfigName(),
                    spawner.isVirtualStorageEnabled(),
                    spawner.isXPStorageEnabled()
            });
            Main.getPlugin().getSqlProcessing().addDatabaseTask(spawnerTask);

            // Delete and save friends
            DatabaseTask deleteFriendsTask = new DatabaseTask(deleteFriendsQuery, new Object[]{
                    spawner.getWorld().getName(),
                    getLocation(spawner)
            });
            Main.getPlugin().getSqlProcessing().addDatabaseTask(deleteFriendsTask);

            for (String uuid : spawner.getFriendUuids()) {
                DatabaseTask friendTask = new DatabaseTask(friendQuery, new Object[]{
                        spawner.getWorld().getName(),
                        getLocation(spawner),
                        uuid
                });
                Main.getPlugin().getSqlProcessing().addDatabaseTask(friendTask);
            }

            // Delete old storage data before inserting new data
            DatabaseTask deleteStorageTask = new DatabaseTask(deleteStorageQuery, new Object[]{
                    spawner.getWorld().getName(),
                    getLocation(spawner)
            });
            Main.getPlugin().getSqlProcessing().addDatabaseTask(deleteStorageTask);

            // Insert new storage items into the storage table
            for (Map.Entry<ItemStack, Integer> entry : spawner.getStorage().getStorage().entrySet()) {
                String itemData;
                try {
                    itemData = ItemStackUtils.itemStackToBase64(entry.getKey());
                } catch (IOException e) {
                    // Log the serialization error
                    Main.getPlugin().getAncLogger().writeError("Failed to serialize item stack for spawner at location " + getLocation(spawner) + ": " + e.getMessage());
                    continue; // Skip this item if serialization fails
                }
                int amount = entry.getValue();

                DatabaseTask storageTask = new DatabaseTask(storageQuery, new Object[]{
                        spawner.getWorld().getName(),
                        getLocation(spawner),
                        itemData,
                        amount
                });
                Main.getPlugin().getSqlProcessing().addDatabaseTask(storageTask);
            }

            // Save XP storage in the storage_xp table
            DatabaseTask storageXpTask = new DatabaseTask(storageXpQuery, new Object[]{
                    spawner.getWorld().getName(),
                    getLocation(spawner),
                    spawner.getStorage().getStoredXp()
            });
            Main.getPlugin().getSqlProcessing().addDatabaseTask(storageXpTask);
        }
        updated_spawners_list.removeAll(spawners_done);
        // Log the completion of the auto-save process
        Main.getPlugin().getLogger().info("Spawner auto-save completed.");
    }


    private ItemStack getMaterial(String material_name) {
        CustomGetters getter;
        if (material_name.startsWith("IA-")) {
            getter = new ItemsAdderGetter();
        } else {
            getter = new DefaultGetter();
        }

        return getter.getMaterial(material_name);
    }

    private Entity spawnEntity(String entity_name, Location location) {
        CustomGetters getter;
        if (entity_name.startsWith("IA-")) {
            getter = new ItemsAdderGetter();
        } else {
            getter = new DefaultGetter();
        }

        return getter.spawnEntity(entity_name, location);
    }

    // Get the required info, create the ancSpawner class here, then add it to both the lists.
    public ancSpawner createSpawner(World world, Location location, @Nullable String config_name, int level, String ownerUUID, String type, String mode, boolean autokill, int storage_limit, boolean virtual_storage, boolean xp_storage) {
        ancSpawner spawner = new ancSpawner(world, location, config_name, level, ownerUUID, type, mode, autokill, storage_limit, virtual_storage, xp_storage);
        updated_spawners_list.add(spawner);
        spawners_list.add(spawner);
        return spawner;
    }

    // Remove the spawner from both lists, also from the SQL database.
    public void deleteSpawner(ancSpawner spawner) {
        World world = spawner.getWorld();
        Location location = spawner.getLocation();
        spawners_list.remove(spawner);
        updated_spawners_list.remove(spawner);

        String deleteSpawnerQuery = "DELETE FROM spawners WHERE world = ? AND location = ?";
        String deleteSpawnerStorageQuery = "DELETE FROM storage WHERE world = ? AND location = ?";
        String deleteSpawnerStorageXPQuery = "DELETE FROM storage_xp WHERE world = ? AND location = ?";
        String deleteSpawnerFriendsQuery = "DELETE FROM friends WHERE world = ? AND location = ?";

        DatabaseTask deleteSpawnerTask = new DatabaseTask(deleteSpawnerQuery, new Object[]{
                world.getName(),
                getLocation(location)
        });

        DatabaseTask deleteStorageTask = new DatabaseTask(deleteSpawnerStorageQuery, new Object[]{
                world.getName(),
                getLocation(location)
        });

        DatabaseTask deleteStorageXpTask = new DatabaseTask(deleteSpawnerStorageXPQuery, new Object[]{
                world.getName(),
                getLocation(location)
        });

        DatabaseTask deleteFriendsTask = new DatabaseTask(deleteSpawnerFriendsQuery, new Object[]{
                world.getName(),
                getLocation(location)
        });

        Main.getPlugin().getSqlProcessing().addDatabaseTask(deleteSpawnerTask);
        Main.getPlugin().getSqlProcessing().addDatabaseTask(deleteStorageTask);
        Main.getPlugin().getSqlProcessing().addDatabaseTask(deleteStorageXpTask);
        Main.getPlugin().getSqlProcessing().addDatabaseTask(deleteFriendsTask);
    }
}
