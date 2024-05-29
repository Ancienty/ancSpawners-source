package com.ancienty.ancspawners.Versions.Holograms;

import com.ancienty.ancspawners.Main;
import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class SpawnerHologram_General implements SpawnerHologram {


    @Override
    public void createHologram(Player player, Block block) {
        if (Main.getPlugin().getConfig().getString("config.modules.hologram.enabled").equalsIgnoreCase("true")) {
            Main.database.getSpawnerType(block).thenAccept(spawner_type -> {
                Main.database.getSpawnerOwner(block).thenAccept(spawner_owner -> {
                    Main.database.getSpawnerLevel(block).thenAccept(spawner_level -> {
                        String hologramName = Main.database.getHologramName(block);
                        boolean createHologram = false;
                        if (DHAPI.getHologram(hologramName) == null) {
                            createHologram = true;
                        }

                        // Location adding
                        Location location = block.getLocation();
                        double x = location.getX() > 0 ? Main.getPlugin().getConfig().getDouble("config.modules.hologram.positive-hologram-x") : Main.getPlugin().getConfig().getDouble("config.modules.hologram.negative-hologram-x");
                        double z = location.getZ() > 0 ? Main.getPlugin().getConfig().getDouble("config.modules.hologram.positive-hologram-z") : Main.getPlugin().getConfig().getDouble("config.modules.hologram.negative-hologram-z");
                        double y = Main.getPlugin().getConfig().getDouble("config.modules.hologram.hologram-height");

                        location.add(x, y, z);
                        if (createHologram) {

                            List<String> spawnerHolograms = Main.getPlugin().lang.getStringList("lang.hologram");
                            List<String> realHologram = new ArrayList<>();
                            Main.getPlugin().getStorageBar(block).thenAccept(storage_bar -> {
                                spawnerHolograms.forEach(line -> {
                                    line = line.replace("{spawner}", Main.getPlugin().getSpawner(spawner_type).getItemMeta().getDisplayName());
                                    line = line.replace("{level}", String.valueOf(spawner_level));
                                    line = line.replace("{owner}", spawner_owner);
                                    line = line.replace("{storage_bar}", storage_bar);
                                    line = ChatColor.translateAlternateColorCodes('&', line);
                                    realHologram.add(line);
                                });
                            });

                            Hologram hologram = DHAPI.createHologram(hologramName, location, realHologram);
                            Main.getPlugin().sendMessage(player, "enabledHologram", new String[]{});

                        } else {
                            DHAPI.removeHologram(hologramName);
                            Main.getPlugin().sendMessage(player, "disabledHologram", new String[]{});
                        }
                    });
                });
            });
        } else {
            Main.getPlugin().getLogger().warning("You have holograms disabled in your modules but a player tried using this module, please remove the item from the main menu as well.");
        }
    }

    @Override
    public void createHologramPlace(Player player, Block block) {
        if (Main.getPlugin().getConfig().getString("config.modules.hologram.enabled").equalsIgnoreCase("true")) {
            Main.database.getSpawnerType(block).thenAccept(spawner_type -> {
                Main.database.getSpawnerOwner(block).thenAccept(spawner_owner -> {
                    Main.database.getSpawnerLevel(block).thenAccept(spawner_level -> {
                        String hologramName = Main.database.getHologramName(block);
                        boolean createHologram = false;
                        if (DHAPI.getHologram(hologramName) == null) {
                            createHologram = true;
                        }

                        // Location adding
                        Location location = block.getLocation();
                        double x = location.getX() > 0 ? Main.getPlugin().getConfig().getDouble("config.modules.hologram.positive-hologram-x") : Main.getPlugin().getConfig().getDouble("config.modules.hologram.negative-hologram-x");
                        double z = location.getZ() > 0 ? Main.getPlugin().getConfig().getDouble("config.modules.hologram.positive-hologram-z") : Main.getPlugin().getConfig().getDouble("config.modules.hologram.negative-hologram-z");
                        double y = Main.getPlugin().getConfig().getDouble("config.modules.hologram.hologram-height");

                        location.add(x, y, z);
                        if (createHologram) {

                            List<String> spawnerHolograms = Main.getPlugin().lang.getStringList("lang.hologram");
                            List<String> realHologram = new ArrayList<>();
                            Main.getPlugin().getStorageBar(block).thenAccept(storage_bar -> {
                                spawnerHolograms.forEach(line -> {
                                    line = line.replace("{spawner}", Main.getPlugin().getSpawner(spawner_type).getItemMeta().getDisplayName());
                                    line = line.replace("{level}", String.valueOf(spawner_level));
                                    line = line.replace("{owner}", spawner_owner);
                                    line = line.replace("{storage_bar}", storage_bar);
                                    line = ChatColor.translateAlternateColorCodes('&', line);
                                    realHologram.add(line);
                                });
                            });

                            Hologram hologram = DHAPI.createHologram(hologramName, location, realHologram);

                        }
                    });
                });
            });
        }
    }
}
