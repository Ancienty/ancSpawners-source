package com.ancienty.ancspawners.Versions.Holograms;

import com.ancienty.ancspawners.Main;
import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class SpawnerHologram_General implements SpawnerHologram {


    @Override
    public void createHologram(Player player, Block block) {
        if (Main.getPlugin().getConfig().getString("config.modules.hologram.enabled").equalsIgnoreCase("true")) {
            CompletableFuture<String> spawnerTypeFuture = Main.database.getSpawnerType(block);
            CompletableFuture<String> spawnerOwnerFuture = Main.database.getSpawnerOwner(block);
            CompletableFuture<Integer> spawnerLevelFuture = Main.database.getSpawnerLevel(block);

            // Combine futures
            CompletableFuture<Void> combinedFuture = CompletableFuture.allOf(spawnerTypeFuture, spawnerOwnerFuture, spawnerLevelFuture);

            combinedFuture.thenCompose(voidResult -> {
                String hologramName = Main.database.getHologramName(block);
                boolean createHologram = DHAPI.getHologram(hologramName) == null;

                // Location adding
                double x = block.getLocation().getX() + 0.5;
                double z = block.getLocation().getZ() + 0.5;
                double y = block.getLocation().getY() + Main.getPlugin().getConfig().getDouble("config.modules.hologram.hologram-height");

                Location location = new Location(block.getWorld(), x, y, z);
                List<String> spawnerHolograms = Main.getPlugin().lang.getStringList("lang.hologram");
                List<String> realHologram = new ArrayList<>();

                return Main.getPlugin().getStorageBar(block).thenAccept(storageBar -> {
                    spawnerHolograms.forEach(line -> {
                        line = line.replace("{spawner}", Main.getPlugin().getSpawner(spawnerTypeFuture.join()).getItemMeta().getDisplayName());
                        line = line.replace("{level}", String.valueOf(spawnerLevelFuture.join()));
                        line = line.replace("{owner}", spawnerOwnerFuture.join());
                        line = line.replace("{storage_bar}", storageBar);
                        line = ChatColor.translateAlternateColorCodes('&', line);
                        realHologram.add(line);
                    });

                    if (createHologram) {
                        Hologram hologram = DHAPI.createHologram(hologramName, location, realHologram);
                        Main.getPlugin().sendMessage(player, "enabledHologram", new String[]{});
                    } else {
                        DHAPI.removeHologram(hologramName);
                        Main.getPlugin().sendMessage(player, "disabledHologram", new String[]{});
                    }
                });
            });
        } else {
            Main.getPlugin().getLogger().warning("You have holograms disabled in your modules but a player tried using this module, please remove the item from the main menu as well.");
        }
    }

    @Override
    public void createHologramPlace(Player player, Block block) {
        if (Main.getPlugin().getConfig().getString("config.modules.hologram.enabled").equalsIgnoreCase("true")) {
            CompletableFuture<String> spawnerTypeFuture = Main.database.getSpawnerType(block);
            CompletableFuture<String> spawnerOwnerFuture = Main.database.getSpawnerOwner(block);
            CompletableFuture<Integer> spawnerLevelFuture = Main.database.getSpawnerLevel(block);

            // Combine futures
            CompletableFuture<Void> combinedFuture = CompletableFuture.allOf(spawnerTypeFuture, spawnerOwnerFuture, spawnerLevelFuture);

            combinedFuture.thenCompose(voidResult -> {
                String hologramName = Main.database.getHologramName(block);
                boolean createHologram = DHAPI.getHologram(hologramName) == null;

                // Location adding
                double x = block.getLocation().getX() + 0.5;
                double z = block.getLocation().getZ() + 0.5;
                double y = block.getLocation().getY() + Main.getPlugin().getConfig().getDouble("config.modules.hologram.hologram-height");

                Location location = new Location(block.getWorld(), x, y, z);
                List<String> spawnerHolograms = Main.getPlugin().lang.getStringList("lang.hologram");
                List<String> realHologram = new ArrayList<>();

                return Main.getPlugin().getStorageBar(block).thenAccept(storageBar -> {
                    spawnerHolograms.forEach(line -> {
                        line = line.replace("{spawner}", Main.getPlugin().getSpawner(spawnerTypeFuture.join()).getItemMeta().getDisplayName());
                        line = line.replace("{level}", String.valueOf(spawnerLevelFuture.join()));
                        line = line.replace("{owner}", spawnerOwnerFuture.join());
                        line = line.replace("{storage_bar}", storageBar);
                        line = ChatColor.translateAlternateColorCodes('&', line);
                        realHologram.add(line);
                    });

                    if (createHologram) {
                        Hologram hologram = DHAPI.createHologram(hologramName, location, realHologram);
                    }
                });
            });
        }
    }


    public static void updateSpawnerHologram(Block block, String hologramName) {
        if (Main.getPlugin().getConfig().getString("config.modules.hologram.enabled").equalsIgnoreCase("true") && Main.getPlugin().getServer().getPluginManager().getPlugin("DecentHolograms") != null) {
            if (DHAPI.getHologram(hologramName) != null) {
                Location hologramLocation = DHAPI.getHologram(hologramName).getLocation();
                DHAPI.removeHologram(hologramName);

                CompletableFuture<String> spawnerTypeFuture = Main.database.getSpawnerType(block);
                CompletableFuture<String> spawnerOwnerFuture = Main.database.getSpawnerOwner(block);
                CompletableFuture<Integer> spawnerLevelFuture = Main.database.getSpawnerLevel(block);

                // Combine futures
                CompletableFuture<Void> combinedFuture = CompletableFuture.allOf(spawnerTypeFuture, spawnerOwnerFuture, spawnerLevelFuture);

                combinedFuture.thenCompose(voidResult -> {
                    List<String> spawnerHolograms = Main.getPlugin().lang.getStringList("lang.hologram");
                    List<String> realHologram = new ArrayList<>();

                    return Main.getPlugin().getStorageBar(block).thenAccept(storageBar -> {
                        spawnerHolograms.forEach(line -> {
                            line = line.replace("{spawner}", Main.getPlugin().getSpawner(spawnerTypeFuture.join()).getItemMeta().getDisplayName());
                            line = line.replace("{level}", String.valueOf(spawnerLevelFuture.join()));
                            line = line.replace("{owner}", spawnerOwnerFuture.join());
                            line = line.replace("{storage_bar}", storageBar);
                            line = ChatColor.translateAlternateColorCodes('&', line);
                            realHologram.add(line);
                        });

                        Hologram hologram = DHAPI.createHologram(hologramName, hologramLocation, realHologram);
                    });
                });
            }
        }
    }

}
