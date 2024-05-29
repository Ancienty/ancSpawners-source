package com.ancienty.ancspawners.Listeners;

import com.ancienty.ancspawners.Main;
import com.ancienty.ancspawners.Protection.SSB2Support;
import com.ancienty.ancspawners.Protection.TownySupport;
import com.ancienty.ancspawners.Protection.WorldGuardSupport;
import com.ancienty.ancspawners.Versions.Holograms.SpawnerHologram;
import com.ancienty.ancspawners.Versions.Holograms.SpawnerHologram_General;
import com.ancienty.ancspawners.Versions.SpawnerNBT.SpawnerUpdate;
import com.ancienty.ancspawners.Versions.SpawnerNBT.SpawnerUpdate1_16;
import com.ancienty.ancspawners.Versions.SpawnerNBT.SpawnerUpdate1_20;
import com.ancienty.ancspawners.Versions.SpawnerNBT.SpawnerUpdate1_8;
import com.cryptomorin.xseries.XMaterial;
import de.tr7zw.nbtapi.utils.MinecraftVersion;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;

public class SpawnerPlaceListener implements Listener {


    @EventHandler(
            priority = EventPriority.LOWEST
    )
    public void onSpawnerPlaceEvent(BlockPlaceEvent e) {
        if (!e.isCancelled()) {
            if (e.getBlockPlaced().getType() == XMaterial.SPAWNER.parseMaterial()) {
                // Protection elements.

                if (Main.getPlugin().getServer().getPluginManager().isPluginEnabled("Towny")) {
                    if (!new TownySupport().canPlace(e.getPlayer(), e.getBlockPlaced())) {
                        e.setCancelled(true);
                        return;
                    }
                }

                if (Main.getPlugin().getServer().getPluginManager().isPluginEnabled("WorldGuard")) {
                    if (!new WorldGuardSupport().canPlace(e.getPlayer(), e.getBlockPlaced())) {
                        e.setCancelled(true);
                        return;
                    }
                }

                if (Main.getPlugin().getServer().getPluginManager().isPluginEnabled("SuperiorSkyblock2")) {
                    if (!new SSB2Support().canPlace(e.getPlayer(), e.getBlockPlaced())) {
                        e.setCancelled(true);
                        return;
                    }
                }

                // Get spawner type from config.yml (ItemName check)
                String spawner = null;
                for (String t : Main.getPlugin().getConfig().getConfigurationSection("spawners").getKeys(false)) {
                    if (e.getItemInHand().hasItemMeta() && e.getItemInHand().getItemMeta().getDisplayName().equalsIgnoreCase(ChatColor.translateAlternateColorCodes('&', Main.getPlugin().getConfig().getString("spawners." + t + ".name")))) {
                        spawner = t;
                        break;
                    }
                }
                final String finalspawner = spawner;
                if (spawner != null) {
                    CreatureSpawner block = (CreatureSpawner) e.getBlockPlaced().getState();
                    String spawnerType = Main.getPlugin().getConfig().getString("spawners." + spawner + ".spawnerInfo.material");
                    int spawnerDelay = Main.getPlugin().getConfig().getInt("spawners." + spawner + ".spawnerInfo.delay");
                    int spawnerRange = Main.getPlugin().getConfig().getInt("spawners." + spawner + ".spawnerInfo.range");

                    SpawnerUpdate spawnerUpdate;
                    if (MinecraftVersion.isAtLeastVersion(MinecraftVersion.MC1_17_R1)) {
                        spawnerUpdate = new SpawnerUpdate1_20();
                    } else if (MinecraftVersion.isAtLeastVersion(MinecraftVersion.MC1_12_R1)) {
                        spawnerUpdate = new SpawnerUpdate1_16();
                    } else {
                        spawnerUpdate = new SpawnerUpdate1_8();
                    }

                    spawnerUpdate.spawnerUpdate(e.getBlockPlaced(), e.getBlockPlaced().getLocation(), spawnerType, spawner, Main.getPlugin().getConfig().getString("spawners." + spawner + ".spawnerInfo.mode"), spawnerDelay, spawnerRange);

                    boolean auto_kill = Main.getPlugin().getConfig().getBoolean("config.modules.auto-kill.enabled");
                    boolean force_auto_kill = Main.getPlugin().getConfig().getBoolean("config.modules.auto-kill.force");

                    Bukkit.getScheduler().runTaskLater(Main.getPlugin(), () -> {
                        if (block.getWorld().getBlockAt(block.getLocation()).getType().equals(block.getType())) {
                            Main.database.placeSpawner(e.getPlayer(), e.getBlockPlaced(), Main.getPlugin().getConfig().getString("spawners." + finalspawner + ".spawnerInfo.mode"), finalspawner);
                            if (auto_kill && force_auto_kill) {
                                Main.database.enableAutoKill(e.getPlayer(), e.getBlockPlaced());
                            }

                            SpawnerHologram hologram = new SpawnerHologram_General();
                            hologram.createHologramPlace(e.getPlayer(), e.getBlockPlaced());
                        }
                    }, 1);


                    Main.getPlugin().sendMessage(e.getPlayer(), "placedSpawner", new String[]{Main.getPlugin().getSpawner(spawner).getItemMeta().getDisplayName()});

                } else {
                    Main.getPlugin().sendMessage(e.getPlayer(), "errorWithSpawner", new String[]{});
                    e.setCancelled(true);
                }
            }
        }
    }
}
