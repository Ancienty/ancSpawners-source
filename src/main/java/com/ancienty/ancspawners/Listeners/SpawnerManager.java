package com.ancienty.ancspawners.Listeners;

import com.ancienty.ancspawners.Database.SQLite;
import com.ancienty.ancspawners.Main;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;

public class SpawnerManager implements Listener {

    private final List<ancSpawner> spawnerList = new CopyOnWriteArrayList<>();
    private static boolean dataLoaded = false;

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (dataLoaded) {
            event.getPlayer().kickPlayer("Spawner data has not yet been loaded\nPlease try again later.");
        }
    }

    public CompletableFuture<Void> loadSpawners() {
        Main.getPlugin().getLogger().info("Loading spawner data.");
        return CompletableFuture.runAsync(() -> {
            String query = "SELECT * FROM spawners";
            try (Connection connection = SQLite.dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement(query);
                 ResultSet resultSet = statement.executeQuery()) {

                List<CompletableFuture<Void>> futures = new ArrayList<>();
                while (resultSet.next()) {
                    futures.add(processSpawnerRow(resultSet));
                }

                CompletableFuture<Void> allFutures = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
                allFutures.join(); // Wait for all futures to complete

                if (allFutures.isDone()) {
                    dataLoaded = true;
                    Main.getPlugin().getLogger().info("Spawner data has been loaded.");
                }

            } catch (SQLException ex) {
                Main.getPlugin().getLogger().log(Level.SEVERE, "Error loading spawners", ex);
            }
        });
    }

    private CompletableFuture<Void> processSpawnerRow(ResultSet resultSet) {
        return CompletableFuture.runAsync(() -> {
            try {
                World world = Bukkit.getWorld(resultSet.getString("world"));
                if (world == null) {
                    Main.getPlugin().getLogger().warning("World not found: " + resultSet.getString("world"));
                    return;
                }

                Location location = parseLocation(resultSet.getString("location"), world);
                if (location == null) {
                    Main.getPlugin().getLogger().warning("Invalid location format: " + resultSet.getString("location"));
                    return;
                }

                String ownerUuid = resultSet.getString("uuid");
                String type = resultSet.getString("type");
                String mode = resultSet.getString("mode");
                int level = resultSet.getInt("level");

                ancSpawner spawner = new ancSpawner(world, location, ownerUuid, type, mode, level);
                spawnerList.add(spawner);

            } catch (SQLException ex) {
                Main.getPlugin().getLogger().log(Level.SEVERE, "Error processing spawner row", ex);
            }
        });
    }

    private Location parseLocation(String locationString, World world) {
        try {
            String[] parts = locationString.split(" ");
            double x = Double.parseDouble(parts[0].split(":")[1]);
            double y = Double.parseDouble(parts[1].split(":")[1]);
            double z = Double.parseDouble(parts[2].split(":")[1]);
            return new Location(world, x, y, z);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public List<ancSpawner> getSpawners() {
        return spawnerList;
    }

    public Optional<ancSpawner> getSpawner(Location location) {
        return spawnerList.stream()
                .filter(spawner -> spawner.getLocation().equals(location) && spawner.getWorld().equals(location.getWorld()))
                .findFirst();
    }

}
