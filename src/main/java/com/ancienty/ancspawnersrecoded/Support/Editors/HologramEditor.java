package com.ancienty.ancspawnersrecoded.Support.Editors;

import com.ancienty.ancspawnersrecoded.Main;
import com.ancienty.ancspawnersrecoded.SpawnerManager.ancSpawner;
import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class HologramEditor {

    public static void updateHologram(Block block) {
        if (Main.getPlugin().getServer().getPluginManager().getPlugin("DecentHolograms") != null && Main.getPlugin().getServer().getPluginManager().getPlugin("DecentHolograms").isEnabled()) {
            if (Main.getPlugin().getHologramModule().getBoolean("hologram.enabled")) {
                if (Main.getPlugin().getHologramModule().getString("hologram.enabled").equalsIgnoreCase("true")) {
                    ancSpawner spawner = Main.getPlugin().getSpawnerManager().getSpawner(block.getWorld(), block.getLocation());
                    if (spawner != null) {
                        String spawnerType = spawner.getType();
                        String spawner_uuid = spawner.getOwnerUUID();
                        String spawnerOwner = Bukkit.getOfflinePlayer(UUID.fromString(spawner_uuid)).getName();
                        int spawnerLevel = spawner.getLevel();
                        String hologramName = Main.getDatabase().getHologramName(block);
                        Hologram hologram = DHAPI.getHologram(hologramName);
                        if (hologram != null) {
                            List<String> spawnerHolograms = Main.getPlugin().lang.getStringList("lang.hologram");
                            List<String> realHologram = new ArrayList<>();

                            spawnerHolograms.forEach(line -> {
                                line = line.replace("{spawner}", Main.getPlugin().getSpawnerFromSpawnerBlock(spawner).getItemMeta().getDisplayName());
                                line = line.replace("{level}", String.valueOf(spawnerLevel));
                                line = line.replace("{owner}", spawnerOwner);
                                line = line.replace("{storage_bar}", Main.getPlugin().getStorageBar(block));
                                line = ChatColor.translateAlternateColorCodes('&', line);
                                realHologram.add(line);
                            });
                            DHAPI.setHologramLines(hologram, realHologram);
                        }
                    } else {
                        removeHologram(block);
                    }
                }
            }
        }
    }

    public static void removeHologram(Block block) {
        if (Main.getPlugin().getServer().getPluginManager().getPlugin("DecentHolograms") != null && Main.getPlugin().getServer().getPluginManager().getPlugin("DecentHolograms").isEnabled()) {
            if (Main.getPlugin().getHologramModule().getBoolean("hologram.enabled")) {
                if (Main.getPlugin().getHologramModule().getString("hologram.enabled").equalsIgnoreCase("true")) {
                    String hologram_name = Main.getDatabase().getHologramName(block);
                    if (DHAPI.getHologram(hologram_name) != null) {
                        DHAPI.removeHologram(hologram_name);
                    }
                }
            }
        }
    }

    public static void toggleHologram(Player player, Block block, @Nullable Boolean is_silent) {
        if (Main.getPlugin().getServer().getPluginManager().getPlugin("DecentHolograms") != null && Main.getPlugin().getServer().getPluginManager().getPlugin("DecentHolograms").isEnabled()) {
            if (Main.getPlugin().getHologramModule().getBoolean("hologram.enabled")) {
                if (Main.getPlugin().getHologramModule().getString("hologram.enabled").equalsIgnoreCase("true")) {
                    ancSpawner spawner = Main.getPlugin().getSpawnerManager().getSpawner(block.getWorld(), block.getLocation());
                    if (spawner != null) {
                        String spawnerType = spawner.getType();
                        String spawner_uuid = spawner.getOwnerUUID();
                        String spawnerOwner = Bukkit.getOfflinePlayer(UUID.fromString(spawner_uuid)).getName();
                        int spawnerLevel = spawner.getLevel();

                        String hologramName = Main.getDatabase().getHologramName(block);
                        boolean createHologram = DHAPI.getHologram(hologramName) == null;
                        if (!createHologram) {
                            DHAPI.removeHologram(hologramName);
                            if (is_silent != null && !is_silent) {
                                Main.getPlugin().sendMessage(player, "disabledHologram", new String[]{});
                            }
                            return;
                        }

                        double x = block.getLocation().getX() + 0.5;
                        double z = block.getLocation().getZ() + 0.5;
                        double y = block.getLocation().getY() + Main.getPlugin().getHologramModule().getDouble("hologram.hologram-height");

                        Location location = new Location(block.getWorld(), x, y, z);
                        List<String> spawnerHolograms = Main.getPlugin().lang.getStringList("lang.hologram");
                        List<String> realHologram = new ArrayList<>();

                        spawnerHolograms.forEach(line -> {
                            line = line.replace("{spawner}", Main.getPlugin().getSpawnerFromSpawnerBlock(spawner).getItemMeta().getDisplayName());
                            line = line.replace("{level}", String.valueOf(spawnerLevel));
                            line = line.replace("{owner}", spawnerOwner);
                            line = line.replace("{storage_bar}", Main.getPlugin().getStorageBar(block));
                            line = ChatColor.translateAlternateColorCodes('&', line);
                            realHologram.add(line);
                        });

                        Hologram hologram = DHAPI.createHologram(hologramName, location, realHologram);
                        if (is_silent != null && !is_silent) {
                            Main.getPlugin().sendMessage(player, "enabledHologram", new String[]{});
                        }
                    }
                }
            }
        }
    }
}
