package com.ancienty.ancspawners.Database;

import com.ancienty.ancspawners.Main;
import com.ancienty.ancspawners.SpawnerManager.ancSpawner;
import com.ancienty.ancspawners.Versions.Holograms.SpawnerHologram_General;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import de.tr7zw.nbtapi.utils.MinecraftVersion;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.io.File;
import java.sql.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import static com.ancienty.ancspawners.Main.getPermissions;

public class SQLite implements Database {

    public static HikariDataSource dataSource;

    public static void initializeHikariCP() {
        if (dataSource == null) {
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl("jdbc:sqlite:" + Main.getPlugin().getDataFolder() + File.separator + "database.db");

            // Connection Pool Settings
            config.setMaximumPoolSize(15);    // Increase if you expect higher load
            config.setMinimumIdle(4);         // Maintain a few idle connections
            config.setIdleTimeout(300000);    // 5 minutes - Close idle connections a bit quicker
            config.setConnectionTimeout(20000); // Lower connection wait time to 20 seconds
            config.setMaxLifetime(1800000);   // 30 minutes - Standard

            // Leak Detection
            config.setLeakDetectionThreshold(3000); // 3 seconds - Detect connections not closed within this time

            // Initialization Fail Timeout
            config.setInitializationFailTimeout(30000); // 30 seconds - Fail fast if database is unreachable

            if (!MinecraftVersion.isAtLeastVersion(MinecraftVersion.MC1_12_R1)) {
                config.setConnectionTestQuery("SELECT 1");
            }

            dataSource = new HikariDataSource(config);
        }
    }


    public SQLite() {
        initializeHikariCP();
        try (Connection connection = dataSource.getConnection()) {
            connection.prepareStatement("CREATE TABLE IF NOT EXISTS spawners (world varchar(255), location varchar(255), uuid varchar(255), mode varchar(255), type varchar(255), level varchar(255), autokill varchar(255), storage_limit varchar(255), CONSTRAINT spawners_unique_combo UNIQUE (world, location))").execute();
            connection.prepareStatement("CREATE TABLE IF NOT EXISTS friends (world varchar(255), location varchar(255), uuid varchar(255))").execute();
            connection.prepareStatement("CREATE TABLE IF NOT EXISTS storage (world varchar(255), location varchar(255), item varchar(255), amount INT, CONSTRAINT storage_unique_combo UNIQUE (world, location, item))").execute();
            connection.prepareStatement("CREATE TABLE IF NOT EXISTS storage_xp (world varchar(255), location varchar(255), xp INT, CONSTRAINT storage_xp_unique_combo UNIQUE (world, location))").execute();
            if (!indexExists(connection, "spawners", "idx_spawners_world_location")) {
                connection.prepareStatement("CREATE INDEX idx_spawners_world_location ON spawners(world, location)").execute();
            }

            if (!indexExists(connection, "friends", "idx_friends_world_location")) {
                connection.prepareStatement("CREATE INDEX idx_friends_world_location ON friends(world, location)").execute();
            }

            if (!indexExists(connection, "storage", "idx_storage_world_location")) {
                connection.prepareStatement("CREATE INDEX idx_storage_world_location ON storage(world, location)").execute();
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    // Helper method to check if an index exists
    private boolean indexExists(Connection connection, String tableName, String indexName) throws SQLException {
        String query = "SELECT count(*) FROM sqlite_master WHERE type = 'index' AND name = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, indexName);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }

    public void notifyTask() {
        synchronized (operations_queue) {
            operations_queue.notify();
        }
    }

    public void closeHikariCP() {
        dataSource.close();
    }

    @Override
    public void updateStorage(Block block, Material material, int new_amount) {
        if (new Random().nextInt(100) >= 90) {
            SpawnerHologram_General.updateSpawnerHologram(block, getHologramName(block));
        }
        String query = "INSERT OR REPLACE INTO storage (world, location, item, amount) VALUES (?, ?, ?, ?)";
        DatabaseTask task = new DatabaseTask(query, new Object[]{block.getWorld().getName(), getLocation(block), material.toString(), new_amount}, null);
        operations_queue.add(task);
        notifyTask();
    }


    @Override
    public void updateXP(Block block, int amount) {
        if (new Random().nextInt(100) >= 90) {
            SpawnerHologram_General.updateSpawnerHologram(block, getHologramName(block));
        }
        String query = "INSERT OR REPLACE INTO storage_xp (world, location, xp) VALUES (?, ?, ?)";
        DatabaseTask task = new DatabaseTask(query, new Object[]{block.getWorld().getName(), getLocation(block), amount}, null);
        operations_queue.add(task);
        notifyTask();
    }

    @Override
    public void updateLevel(Block block, int level) {
        String query = "UPDATE spawners SET level = ? WHERE world = ? AND location = ?";
        DatabaseTask task = new DatabaseTask(query, new Object[]{level, block.getWorld().getName(), getLocation(block)}, null);
        operations_queue.add(task);
        notifyTask();
    }

    @Override
    public void checkStorage(Block block) {
        if (Main.getPlugin().getConfig().getString("config.modules.storage-limit.enabled").equalsIgnoreCase("true")) {
            getSpawnerOwnerUuid(block).thenAccept(uuid_from_function -> {
                UUID uuid = UUID.fromString(uuid_from_function);
                OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
                if (offlinePlayer.isOnline()) {
                    int storage = 0;
                    String perm = null;
                    Iterator var6 = Main.getPlugin().getConfig().getConfigurationSection("config.modules.storage-limit.permissions").getKeys(false).iterator();

                    while(var6.hasNext()) {
                        String permission = (String)var6.next();
                        if (getPermissions().has((Player)offlinePlayer, permission)) {
                            storage = Main.getPlugin().getConfig().getInt("config.modules.storage-limit.permissions." + permission);
                            perm = permission;
                        }
                    }

                    if (perm == null) {
                        storage = Main.getPlugin().getConfig().getInt("config.modules.storage-limit.default");
                    }

                    int finalStorage = storage;
                    getStorageLimit(block).thenAccept(storageInData -> {
                        if (finalStorage > storageInData) {
                            updateStorageLimit(block, finalStorage);
                        }
                    });
                }
            });
        }
    }

    @Override
    public void updateStorageLimit(Block block, int storage_limit) {
        String query = "UPDATE spawners SET storage_limit = ? WHERE world = ? AND location = ?";
        DatabaseTask task = new DatabaseTask(query, new Object[]{storage_limit, block.getWorld().getName(), getLocation(block)}, null);
        operations_queue.add(task);
        notifyTask();
    }

    @Override
    public void toggleAutoKill(Block block) {
        String query = "UPDATE spawners SET autokill = CASE WHEN autokill = 'true' THEN 'false' ELSE 'true' END WHERE world = ? AND location = ?";
        DatabaseTask task = new DatabaseTask(query, new Object[]{block.getWorld().getName(), getLocation(block)}, null);
        operations_queue.add(task);
        notifyTask();
    }

    @Override
    public void addFriend(Block block, String uuid) {
        String query = "INSERT INTO friends (world, location, uuid) VALUES (?, ?, ?)";
        DatabaseTask task = new DatabaseTask(query, new Object[]{block.getWorld().getName(), getLocation(block), uuid}, null);
        operations_queue.add(task);
        notifyTask();
    }

    @Override
    public void removeFriend(Block block, String uuid) {
        String query = "DELETE FROM friends WHERE world = ? AND location = ? AND uuid = ?";
        DatabaseTask task = new DatabaseTask(query, new Object[]{block.getWorld().getName(), getLocation(block), uuid}, null);
        operations_queue.add(task);
        notifyTask();
    }

    @Override
    public CompletableFuture<Double> spawnerSellAllItems(Player player, Block block) {
        String query = "SELECT * FROM storage WHERE world = ? AND location = ?";
        DatabaseTask task = new DatabaseTask(query, new Object[]{block.getWorld().getName(), getLocation(block)}, "SPAWNER_SELL_ITEMS*---" + player.getUniqueId().toString());
        operations_queue.add(task);
        notifyTask();
        DatabaseTask task2 = new DatabaseTask("DELETE FROM storage WHERE world = ? AND location = ?", new Object[]{block.getWorld().getName(), getLocation(block)}, null);
        operations_queue.add(task2);
        notifyTask();
        return task.getReturnDouble();
    }


    @Override
    public CompletableFuture<String> getSpawnerOwnerUuid(Block block) {
        String query = "SELECT uuid FROM spawners WHERE world = ? AND location = ?";
        DatabaseTask task = new DatabaseTask(query, new Object[]{block.getWorld().getName(), getLocation(block)}, "uuid");
        operations_queue.add(task);
        notifyTask();
        return task.getReturnString();
    }

    @Override
    public String getLocation(Block block) {
        String location_x = String.valueOf(block.getLocation().getBlockX());
        String location_y = String.valueOf(block.getLocation().getBlockY());
        String location_z = String.valueOf(block.getLocation().getBlockZ());
        return "x:" + location_x + " y:" + location_y + " z:" + location_z;
    }

    @Override
    public CompletableFuture<String> getSpawnerOwner(Block block) {
        CompletableFuture<String> return_value = new CompletableFuture<>();
        getSpawnerOwnerUuid(block).thenAccept(uuid -> {
            Bukkit.getScheduler().runTask(Main.getPlugin(), () -> {
                UUID uuid_real = UUID.fromString(uuid);
                OfflinePlayer player = Bukkit.getOfflinePlayer(uuid_real);
                return_value.complete(player.getName());
            });
        });
        return return_value;
    }

    @Override
    public CompletableFuture<String> getSpawnerMode(Block block) {
        String query = "SELECT mode FROM spawners WHERE world = ? AND location = ?";
        DatabaseTask task = new DatabaseTask(query, new Object[]{block.getWorld().getName(), getLocation(block)}, "mode");
        operations_queue.add(task);
        notifyTask();
        return task.getReturnString();
    }


    @Override
    public CompletableFuture<String> getSpawnerType(Block block) {
        String query = "SELECT type FROM spawners WHERE world = ? AND location = ?";
        DatabaseTask task = new DatabaseTask(query, new Object[]{block.getWorld().getName(), getLocation(block)}, "type");
        operations_queue.add(task);
        notifyTask();
        return task.getReturnString();
    }

    @Override
    public CompletableFuture<Integer> getSpawnerLevel(Block block) {
        String query = "SELECT level FROM spawners WHERE world = ? AND location = ?";
        DatabaseTask task = new DatabaseTask(query, new Object[]{block.getWorld().getName(), getLocation(block)}, "level");
        operations_queue.add(task);
        notifyTask();
        return task.getReturnInteger();
    }

    @Override
    public CompletableFuture<List<String>> getStoredItems(Block block) {
        String query = "SELECT item FROM storage WHERE world = ? AND location = ?";
        DatabaseTask task = new DatabaseTask(query, new Object[]{block.getWorld().getName(), getLocation(block)}, "LISTitem");
        operations_queue.add(task);
        notifyTask();
        return task.getReturnList();
    }

    @Override
    public String getHologramName(Block block) {
        String world = block.getWorld().getName();
        String location = getLocation(block);
        String return_value = world + location;
        return_value = return_value.replace(" ", "");
        return_value = return_value.replace(":", "");
        return return_value;
    }

    @Override
    public CompletableFuture<Integer> getSpawnerStoredByItem(Block block, Material material) {
        String query = "SELECT * FROM storage WHERE world = ? AND location = ? AND item = ?";
        DatabaseTask task = new DatabaseTask(query, new Object[]{block.getWorld().getName(), getLocation(block), material.toString()}, "amount");
        operations_queue.add(task);
        notifyTask();
        return task.getReturnInteger();
    }

    @Override
    public CompletableFuture<Integer> getSpawnerTotalStored(Block block) {
        String query = "SELECT * FROM storage WHERE world = ? AND location = ?";
        DatabaseTask task = new DatabaseTask(query, new Object[]{block.getWorld().getName(), getLocation(block)}, "TOTALamount");
        operations_queue.add(task);
        notifyTask();
        return task.getReturnInteger();
    }

    @Override
    public CompletableFuture<Integer> getStoredXP(Block block) {
        String query = "SELECT xp FROM storage_xp WHERE world = ? AND location = ?";
        DatabaseTask task = new DatabaseTask(query, new Object[]{block.getWorld().getName(), getLocation(block)}, "xp");
        operations_queue.add(task);
        notifyTask();
        return task.getReturnInteger();
    }

    @Override
    public CompletableFuture<List<String>> getFriendsByUuid(Block block) {
        String query = "SELECT * FROM friends WHERE world = ? AND location = ?";
        DatabaseTask task = new DatabaseTask(query, new Object[]{block.getWorld().getName(), getLocation(block)}, "LISTuuid");
        operations_queue.add(task);
        notifyTask();
        return task.getReturnList();
    }

    @Override
    public CompletableFuture<Double> getSpawnerMoney(Player player, Block block) {
        String query = "SELECT * FROM storage WHERE world = ? AND location = ?";
        DatabaseTask task = new DatabaseTask(query, new Object[]{block.getWorld().getName(), getLocation(block)}, "SPAWNER_MONEY*--" + player.getUniqueId().toString());
        operations_queue.add(task);
        notifyTask();
        return task.getReturnDouble();
    }


    @Override
    public void placeSpawner(Player player, Block block, String mode, String type) {
        String query = "INSERT OR REPLACE INTO spawners (world, location, uuid, mode, type, level, autokill) VALUES (?, ?, ?, ?, ?, ?, ?)";
        DatabaseTask task = new DatabaseTask(query, new Object[]{block.getWorld().getName(), getLocation(block), player.getUniqueId().toString(), mode, type, 1, false}, null);
        operations_queue.add(task);
        notifyTask();
    }

    @Override
    public CompletableFuture<Boolean> doesLocationHaveSpawner(World world, Location location) {
        String query = "SELECT * FROM spawners WHERE world = ? AND location = ?";
        DatabaseTask task = new DatabaseTask(query, new Object[]{world, location}, "owner", true);
        operations_queue.add(task);
        notifyTask();
        return task.getReturnBoolean();
    }

    @Override
    public void deleteSpawner(World world, Location location) {
        String query = "DELETE FROM spawners WHERE world = ? AND location = ?";
        DatabaseTask task = new DatabaseTask(query, new Object[]{world, location}, null);
        operations_queue.add(task);
        notifyTask();
    }


    @Override
    public void breakSpawner(Player player, Block block) {
        String query = "DELETE FROM spawners WHERE world = ? AND location = ? AND uuid = ?";
        DatabaseTask task = new DatabaseTask(query, new Object[]{block.getWorld().getName(), getLocation(block), player.getUniqueId().toString()}, null);
        operations_queue.add(task);
        notifyTask();
    }

    @Override
    public void enableAutoKill(Player player, Block block) {
        String query = "UPDATE spawners SET autokill = true WHERE world = ? AND location = ?";
        DatabaseTask task = new DatabaseTask(query, new Object[]{block.getWorld().getName(), getLocation(block)}, null);
        operations_queue.add(task);
        notifyTask();
    }

    @Override
    public CompletableFuture<Boolean> isAutoKillEnabled(Block block) {
        String query = "SELECT autokill FROM spawners WHERE world = ? AND location = ?";
        DatabaseTask task = new DatabaseTask(query, new Object[]{block.getWorld().getName(), getLocation(block)}, "autokill", true);
        operations_queue.add(task);
        notifyTask();
        return task.getReturnBoolean();
    }


    @Override
    public CompletableFuture<Boolean> spawnerHasOwner(Block block) {
        String query = "SELECT uuid FROM spawners WHERE world = ? AND location = ?";
        DatabaseTask task = new DatabaseTask(query, new Object[]{block.getWorld().getName(), getLocation(block)}, "uuid", true);
        operations_queue.add(task);
        notifyTask();
        return task.getReturnBoolean();
    }

    @Override
    public CompletableFuture<Boolean> checkIfFriend(Block block, Player player) {
        String query = "SELECT uuid FROM friends WHERE world = ? AND location = ? AND uuid = ?";
        DatabaseTask task = new DatabaseTask(query, new Object[]{block.getWorld().getName(), getLocation(block), player.getUniqueId().toString()}, "uuid", true);
        operations_queue.add(task);
        notifyTask();
        return task.getReturnBoolean();
    }

    @Override
    public void checkSpawnerXPPermission(Block block, Player player) {
        if (!getPermissions().has(player, Main.getPlugin().getConfig().getString("config.modules.permission-based-xp.permission"))) {
            updateXP(block, 0);
        }
    }

    @Override
    public CompletableFuture<Integer> getStorageLimit(Block block) {
        String query = "SELECT storage_limit FROM spawners WHERE world = ? AND location = ?";
        DatabaseTask task = new DatabaseTask(query, new Object[]{block.getWorld().getName(), getLocation(block)}, "STORAGE_LIMITstorage_limit");
        operations_queue.add(task);
        notifyTask();
        return task.getReturnInteger();
    }

    @Override
    public void setSpawnerLevelTo1(Player player, Block block) {
        String query = "UPDATE spawners SET level = 1 WHERE world = ? AND location = ?";
        DatabaseTask task = new DatabaseTask(query, new Object[]{block.getWorld().getName(), getLocation(block)}, null);
        operations_queue.add(task);
        notifyTask();
    }

    @Override
    public void reduceLevelBy1(Player player, Block block) {
        String query = "UPDATE spawners SET level = level - 1 WHERE world = ? AND location = ?";
        DatabaseTask task = new DatabaseTask(query, new Object[]{block.getWorld().getName(), getLocation(block)}, null);
        operations_queue.add(task);
        notifyTask();
    }
}
