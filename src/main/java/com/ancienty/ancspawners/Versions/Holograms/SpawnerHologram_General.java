package com.ancienty.ancspawners.Versions.Holograms;

import com.ancienty.ancspawners.Main;
import com.ancienty.ancspawners.SpawnerManager.ancSpawner;
import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SpawnerHologram_General implements SpawnerHologram {

    @Override
    public void createHologram(Player player, Block block) {
        if (Main.getPlugin().getConfig().getString("config.modules.hologram.enabled").equalsIgnoreCase("true")) {
            ancSpawner spawner = Main.getPlugin().getSpawnerManager().getSpawner(block.getWorld(), block.getLocation());
            if (spawner != null) {
                String spawnerType = spawner.getType();
                String spawner_uuid = spawner.getOwnerUUID();
                String spawnerOwner = Bukkit.getOfflinePlayer(UUID.fromString(spawner_uuid)).getName();
                int spawnerLevel = spawner.getLevel();

                String hologramName = Main.database.getHologramName(block);
                boolean createHologram = DHAPI.getHologram(hologramName) == null;

                double x = block.getLocation().getX() + 0.5;
                double z = block.getLocation().getZ() + 0.5;
                double y = block.getLocation().getY() + Main.getPlugin().getConfig().getDouble("config.modules.hologram.hologram-height");

                Location location = new Location(block.getWorld(), x, y, z);
                List<String> spawnerHolograms = Main.getPlugin().lang.getStringList("lang.hologram");
                List<String> realHologram = new ArrayList<>();

                spawnerHolograms.forEach(line -> {
                    line = line.replace("{spawner}", Main.getPlugin().getSpawner(spawnerType).getItemMeta().getDisplayName());
                    line = line.replace("{level}", String.valueOf(spawnerLevel));
                    line = line.replace("{owner}", spawnerOwner);
                    line = line.replace("{storage_bar}", Main.getPlugin().getStorageBar(block));
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
            }
        } else {
            Main.getPlugin().getLogger().warning("You have holograms disabled in your modules but a player tried using this module, please remove the item from the main menu as well.");
        }
    }

    @Override
    public void createHologramPlace(Player player, Block block) {
        if (Main.getPlugin().getConfig().getString("config.modules.hologram.enabled").equalsIgnoreCase("true")) {
            ancSpawner spawner = Main.getPlugin().getSpawnerManager().getSpawner(block.getWorld(), block.getLocation());
            if (spawner != null) {
                String spawnerType = spawner.getType();
                String spawner_uuid = spawner.getOwnerUUID();
                String spawnerOwner = Bukkit.getOfflinePlayer(UUID.fromString(spawner_uuid)).getName();
                int spawnerLevel = spawner.getLevel();

                String hologramName = Main.database.getHologramName(block);
                boolean createHologram = DHAPI.getHologram(hologramName) == null;

                double x = block.getLocation().getX() + 0.5;
                double z = block.getLocation().getZ() + 0.5;
                double y = block.getLocation().getY() + Main.getPlugin().getConfig().getDouble("config.modules.hologram.hologram-height");

                Location location = new Location(block.getWorld(), x, y, z);
                List<String> spawnerHolograms = Main.getPlugin().lang.getStringList("lang.hologram");
                List<String> realHologram = new ArrayList<>();

                spawnerHolograms.forEach(line -> {
                    line = line.replace("{spawner}", Main.getPlugin().getSpawner(spawnerType).getItemMeta().getDisplayName());
                    line = line.replace("{level}", String.valueOf(spawnerLevel));
                    line = line.replace("{owner}", spawnerOwner);
                    line = line.replace("{storage_bar}", Main.getPlugin().getStorageBar(block));
                    line = ChatColor.translateAlternateColorCodes('&', line);
                    realHologram.add(line);
                });

                if (createHologram) {
                    Hologram hologram = DHAPI.createHologram(hologramName, location, realHologram);
                }
            }
        }
    }

    public static void updateSpawnerHologram(Block block, String hologramName) {
        if (Main.getPlugin().getConfig().getString("config.modules.hologram.enabled").equalsIgnoreCase("true") && Main.getPlugin().getServer().getPluginManager().getPlugin("DecentHolograms") != null) {
            if (DHAPI.getHologram(hologramName) != null) {
                Location hologramLocation = DHAPI.getHologram(hologramName).getLocation();
                DHAPI.removeHologram(hologramName);

                ancSpawner spawner = Main.getPlugin().getSpawnerManager().getSpawner(block.getWorld(), block.getLocation());
                if (spawner != null) {
                    String spawnerType = spawner.getType();
                    String spawner_uuid = spawner.getOwnerUUID();
                    String spawnerOwner = Bukkit.getOfflinePlayer(UUID.fromString(spawner_uuid)).getName();
                    int spawnerLevel = spawner.getLevel();

                    List<String> spawnerHolograms = Main.getPlugin().lang.getStringList("lang.hologram");
                    List<String> realHologram = new ArrayList<>();

                    spawnerHolograms.forEach(line -> {
                        line = line.replace("{spawner}", Main.getPlugin().getSpawner(spawnerType).getItemMeta().getDisplayName());
                        line = line.replace("{level}", String.valueOf(spawnerLevel));
                        line = line.replace("{owner}", spawnerOwner);
                        line = line.replace("{storage_bar}", Main.getPlugin().getStorageBar(block));
                        line = ChatColor.translateAlternateColorCodes('&', line);
                        realHologram.add(line);
                    });

                    Hologram hologram = DHAPI.createHologram(hologramName, hologramLocation, realHologram);
                }
            }
        }
    }

    public static void updateSpawnerHologram(ancSpawner block, String hologramName) {
        Bukkit.getScheduler().runTask(Main.getPlugin(), () -> {
            if (Main.getPlugin().getConfig().getString("config.modules.hologram.enabled").equalsIgnoreCase("true") && Main.getPlugin().getServer().getPluginManager().getPlugin("DecentHolograms") != null) {
                if (DHAPI.getHologram(hologramName) != null) {
                    Location hologramLocation = DHAPI.getHologram(hologramName).getLocation();
                    DHAPI.removeHologram(hologramName);

                    ancSpawner spawner = Main.getPlugin().getSpawnerManager().getSpawner(block.getWorld(), block.getLocation());
                    if (spawner != null) {
                        String spawnerType = spawner.getType();
                        String spawner_uuid = spawner.getOwnerUUID();
                        String spawnerOwner = Bukkit.getOfflinePlayer(UUID.fromString(spawner_uuid)).getName();
                        int spawnerLevel = spawner.getLevel();

                        List<String> spawnerHolograms = Main.getPlugin().lang.getStringList("lang.hologram");
                        List<String> realHologram = new ArrayList<>();

                        Block blockk = spawner.getWorld().getBlockAt(spawner.getLocation());

                        spawnerHolograms.forEach(line -> {
                            line = line.replace("{spawner}", Main.getPlugin().getSpawner(spawnerType).getItemMeta().getDisplayName());
                            line = line.replace("{level}", String.valueOf(spawnerLevel));
                            line = line.replace("{owner}", spawnerOwner);
                            line = line.replace("{storage_bar}", Main.getPlugin().getStorageBar(blockk));
                            line = ChatColor.translateAlternateColorCodes('&', line);
                            realHologram.add(line);
                        });

                        Hologram hologram = DHAPI.createHologram(hologramName, hologramLocation, realHologram);
                    }
                }
            }
        });
    }
}
