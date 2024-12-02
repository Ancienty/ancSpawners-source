package com.ancienty.ancspawnersrecoded.Database;

import com.ancienty.ancspawnersrecoded.Main;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import de.tr7zw.nbtapi.utils.MinecraftVersion;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;

import java.io.File;
import java.sql.*;
import java.util.logging.Level;

public class SQLite {

    public static HikariDataSource dataSource;

    public static void initializeHikariCP() {
        if (dataSource == null) {
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl("jdbc:sqlite:" + Main.getPlugin().getDataFolder() + File.separator + "database.db");

            // Connection Pool Settings
            config.setMaximumPoolSize(15);
            config.setMinimumIdle(4);
            config.setIdleTimeout(300000);
            config.setConnectionTimeout(20000);
            config.setMaxLifetime(1800000);

            // Leak Detection
            config.setLeakDetectionThreshold(5000);

            // Initialization Fail Timeout
            config.setInitializationFailTimeout(30000);

            if (!MinecraftVersion.isAtLeastVersion(MinecraftVersion.MC1_12_R1)) {
                config.setConnectionTestQuery("SELECT 1");
            }

            dataSource = new HikariDataSource(config);
        }
    }

    public SQLite() {
        initializeHikariCP();
        try (Connection connection = dataSource.getConnection()) {
            connection.prepareStatement("PRAGMA journal_mode=WAL").execute();
            // Check if the database uses the old structure
            if (isOldDatabaseStructure(connection)) {
                Bukkit.getLogger().log(Level.INFO, "[ancSpawners] Old database structure detected. Starting data conversion...");
                convertOldDataToNewStructure(connection);
                Bukkit.getLogger().log(Level.INFO, "[ancSpawners] Data conversion completed successfully.");
            } else {
                // Create tables with the new structure
                createAllTables(connection);
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    private void createAllTables(Connection connection) throws SQLException {
        createSpawnersTable(connection);
        createRemainingTables(connection);
    }

    private void createSpawnersTable(Connection connection) throws SQLException {
        connection.prepareStatement(
                "CREATE TABLE IF NOT EXISTS spawners (" +
                        "world VARCHAR(255), " +
                        "location VARCHAR(255), " +
                        "owner_uuid VARCHAR(255), " +
                        "auto_kill BOOLEAN, " +
                        "config_name TEXT, " +
                        "spawner_material TEXT, " +
                        "spawner_mode TEXT, " +
                        "storage_limit INT, " +
                        "level INT, " +
                        "virtual_storage BOOLEAN," +
                        "xp_storage BOOLEAN," +
                        "CONSTRAINT spawners_unique_combo UNIQUE (world, location))"
        ).execute();
    }

    private void createRemainingTables(Connection connection) throws SQLException {
        connection.prepareStatement(
                "CREATE TABLE IF NOT EXISTS friends (" +
                        "world VARCHAR(255), " +
                        "location VARCHAR(255), " +
                        "uuid VARCHAR(255))"
        ).execute();

        connection.prepareStatement(
                "CREATE TABLE IF NOT EXISTS storage (" +
                        "world VARCHAR(255), " +
                        "location VARCHAR(255), " +
                        "item TEXT, " +
                        "amount INT)"
        ).execute();

        connection.prepareStatement(
                "CREATE TABLE IF NOT EXISTS storage_xp (" +
                        "world VARCHAR(255), " +
                        "location VARCHAR(255), " +
                        "xp INT, " +
                        "CONSTRAINT storage_xp_unique_combo UNIQUE (world, location))"
        ).execute();

        connection.prepareStatement("CREATE TABLE IF NOT EXISTS boosts (" +
                "world VARCHAR(255), " +
                "location VARCHAR(255), " +
                "boost_type VARCHAR(255), " +
                "end_date BIGINT, " +
                "boost_amount DOUBLE)"
        ).execute();
    }

    private boolean isOldDatabaseStructure(Connection connection) throws SQLException {
        DatabaseMetaData meta = connection.getMetaData();

        // Check if the 'spawners' table exists
        ResultSet rs = meta.getTables(null, null, "spawners", null);
        boolean spawnersTableExists = rs.next();
        rs.close();

        if (!spawnersTableExists) {
            // The 'spawners' table does not exist, so the database is new
            return false;
        }

        // Check if the 'boosts' table exists
        rs = meta.getTables(null, null, "boosts", null);
        boolean boostsTableExists = rs.next();
        rs.close();

        // If the 'spawners' table exists but the 'boosts' table does not, it's an old database structure
        return !boostsTableExists;
    }

    private void convertOldDataToNewStructure(Connection connection) throws SQLException {
        // Begin transaction
        connection.setAutoCommit(false);
        try {
            // Rename old 'spawners' table
            connection.prepareStatement("ALTER TABLE spawners RENAME TO spawners_old").execute();

            // Create new 'spawners' table
            createSpawnersTable(connection);

            // Migrate data from 'spawners_old' to new 'spawners' table
            migrateSpawnersData(connection);

            // Migrate 'storage' data if necessary
            migrateStorageData(connection);

            // Drop old tables
            connection.prepareStatement("DROP TABLE IF EXISTS spawners_old").execute();
            connection.prepareStatement("DROP TABLE IF EXISTS storage_old").execute();

            // Create remaining tables
            createRemainingTables(connection);

            // Commit transaction
            connection.commit();
        } catch (SQLException e) {
            connection.rollback();
            e.printStackTrace();
            throw e;
        } finally {
            connection.setAutoCommit(true);
        }
    }

    private boolean columnExists(Connection connection, String tableName, String columnName) throws SQLException {
        DatabaseMetaData meta = connection.getMetaData();
        ResultSet rs = meta.getColumns(null, null, tableName, columnName);
        boolean exists = rs.next();
        rs.close();
        return exists;
    }

    private void migrateSpawnersData(Connection connection) throws SQLException {
        String selectOldSpawners = "SELECT world, location, uuid, autokill, type, level, mode, storage_limit";
        // Check for the existence of xp_storage and virtual_storage columns
        boolean hasXpStorage = columnExists(connection, "spawners_old", "xp_storage");
        boolean hasVirtualStorage = columnExists(connection, "spawners_old", "virtual_storage");

        // Adjust the SELECT statement accordingly
        if (hasXpStorage) {
            selectOldSpawners += ", xp_storage";
        }
        if (hasVirtualStorage) {
            selectOldSpawners += ", virtual_storage";
        }
        selectOldSpawners += " FROM spawners_old";

        try (PreparedStatement selectStmt = connection.prepareStatement(selectOldSpawners);
             ResultSet rs = selectStmt.executeQuery()) {

            String insertNewSpawner = "INSERT INTO spawners (world, location, owner_uuid, auto_kill, config_name, spawner_material, spawner_mode, storage_limit, level, virtual_storage, xp_storage) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement insertStmt = connection.prepareStatement(insertNewSpawner)) {

                while (rs.next()) {
                    String world = rs.getString("world");
                    String location = rs.getString("location");
                    String uuid = rs.getString("uuid");
                    String autokill = rs.getString("autokill");
                    String type = rs.getString("type");
                    String levelStr = rs.getString("level");
                    String mode = rs.getString("mode");
                    String storageLimitStr = rs.getString("storage_limit");

                    // Default values for xp_storage and virtual_storage
                    boolean actual_xp_storage = false;
                    boolean actual_virtual_storage = false;

                    // Read xp_storage if the column exists
                    if (hasXpStorage) {
                        String xp_storage = rs.getString("xp_storage");
                        actual_xp_storage = parseBooleanValue(xp_storage);
                    }

                    // Read virtual_storage if the column exists
                    if (hasVirtualStorage) {
                        String virtual_storage = rs.getString("virtual_storage");
                        actual_virtual_storage = parseBooleanValue(virtual_storage);
                    }

                    // Convert data types
                    boolean autoKill = Boolean.parseBoolean(autokill);
                    int level = Integer.parseInt(levelStr);
                    int storageLimit = Integer.parseInt(storageLimitStr);

                    // Insert into new table
                    insertStmt.setString(1, world);
                    insertStmt.setString(2, location);
                    insertStmt.setString(3, uuid);
                    insertStmt.setBoolean(4, autoKill);
                    String config_name = "default";
                    ConfigurationSection spawnersSection = Main.getPlugin().getConfig().getConfigurationSection("spawners");
                    if (spawnersSection != null) {
                        for (String spawner_config_name : spawnersSection.getKeys(false)) {
                            if (Main.getPlugin().getConfig().getString("spawners." + spawner_config_name + ".spawnerInfo.material").equalsIgnoreCase(type)) {
                                config_name = spawner_config_name;
                                break;
                            }
                        }
                    }
                    insertStmt.setString(5, type);
                    insertStmt.setString(6, type);
                    insertStmt.setString(7, mode);
                    insertStmt.setInt(8, storageLimit);
                    insertStmt.setInt(9, level);
                    insertStmt.setBoolean(10, actual_virtual_storage);
                    insertStmt.setBoolean(11, actual_xp_storage);

                    insertStmt.addBatch();
                }
                insertStmt.executeBatch();
            }
        }
    }

    private boolean parseBooleanValue(String value) {
        if (value == null) {
            return false;
        }
        value = value.trim().toLowerCase();
        return value.equals("1") || value.equals("true");
    }

    private void migrateStorageData(Connection connection) throws SQLException {
        // Rename old 'storage' table
        connection.prepareStatement("ALTER TABLE storage RENAME TO storage_old").execute();

        // Create new 'storage' table
        connection.prepareStatement(
                "CREATE TABLE IF NOT EXISTS storage (" +
                        "world VARCHAR(255), " +
                        "location VARCHAR(255), " +
                        "item TEXT, " +
                        "amount INT)"
        ).execute();

        // Migrate data
        String selectOldStorage = "SELECT world, location, item, amount FROM storage_old";
        try (PreparedStatement selectStmt = connection.prepareStatement(selectOldStorage);
             ResultSet rs = selectStmt.executeQuery()) {

            String insertNewStorage = "INSERT INTO storage (world, location, item, amount) VALUES (?, ?, ?, ?)";
            try (PreparedStatement insertStmt = connection.prepareStatement(insertNewStorage)) {

                while (rs.next()) {
                    String world = rs.getString("world");
                    String location = rs.getString("location");
                    String item = rs.getString("item");
                    int amount = rs.getInt("amount");

                    insertStmt.setString(1, world);
                    insertStmt.setString(2, location);
                    insertStmt.setString(3, item);
                    insertStmt.setInt(4, amount);

                    insertStmt.addBatch();
                }
                insertStmt.executeBatch();
            }
        }
    }

    public HikariDataSource getDataSource() {
        return dataSource;
    }

    public String getHologramName(Block block) {
        return ("x-" + block.getLocation().getX() + "y-" + block.getLocation().getY() + "z-" + block.getLocation().getZ()).replace(".0", "");
    }
}