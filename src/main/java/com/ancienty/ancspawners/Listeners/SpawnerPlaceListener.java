package com.ancienty.ancspawners.Listeners;

import com.ancienty.ancspawners.Main;
import com.ancienty.ancspawners.Protection.GriefPreventionSupport;
import com.ancienty.ancspawners.Protection.SSB2Support;
import com.ancienty.ancspawners.Protection.TownySupport;
import com.ancienty.ancspawners.Protection.WorldGuardSupport;
import com.ancienty.ancspawners.SpawnerManager.SpawnerManager;
import com.ancienty.ancspawners.SpawnerManager.ancSpawner;
import com.ancienty.ancspawners.SpawnerManager.ancStorage;
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

import java.util.ArrayList;

public class SpawnerPlaceListener implements Listener {

    @EventHandler(priority = EventPriority.LOWEST)
    public void onSpawnerPlaceEvent(BlockPlaceEvent e) {
        if (e.isCancelled() || e.getBlockPlaced().getType() != XMaterial.SPAWNER.parseMaterial()) {
            return;
        }

        // Check if the spawner already exists in the plugin's spawner manager
        boolean doesSpawnerExist = Main.getPlugin().getSpawnerManager().getSpawner(e.getBlockPlaced().getWorld(), e.getBlockPlaced().getLocation()) != null;
        if (doesSpawnerExist) {
            Main.getPlugin().getSpawnerManager().removeSpawner(Main.getPlugin().getSpawnerManager().getSpawner(e.getBlockPlaced().getWorld(), e.getBlockPlaced().getLocation()));
        }

        if (!canPlaceSpawner(e)) {
            e.setCancelled(true);
            return;
        }

        String spawnerType = getSpawnerType(e);
        if (spawnerType == null) {
            Main.getPlugin().sendMessage(e.getPlayer(), "errorWithSpawner", new String[]{});
            e.setCancelled(true);
            return;
        }

        CreatureSpawner block = (CreatureSpawner) e.getBlockPlaced().getState();
        configureSpawner(e, block, spawnerType);
    }

    private boolean canPlaceSpawner(BlockPlaceEvent e) {
        if (Main.getPlugin().getServer().getPluginManager().isPluginEnabled("GriefPrevention") &&
                !new GriefPreventionSupport().canPlace(e.getPlayer(), e.getBlockPlaced())) {
            return false;
        }

        if (Main.getPlugin().getServer().getPluginManager().isPluginEnabled("Towny") &&
                !new TownySupport().canPlace(e.getPlayer(), e.getBlockPlaced())) {
            return false;
        }

        if (Main.getPlugin().getServer().getPluginManager().isPluginEnabled("WorldGuard") &&
                !new WorldGuardSupport().canPlace(e.getPlayer(), e.getBlockPlaced())) {
            return false;
        }

        if (Main.getPlugin().getServer().getPluginManager().isPluginEnabled("SuperiorSkyblock2") &&
                !new SSB2Support().canPlace(e.getPlayer(), e.getBlockPlaced())) {
            return false;
        }

        return true;
    }

    private String getSpawnerType(BlockPlaceEvent e) {
        for (String t : Main.getPlugin().getConfig().getConfigurationSection("spawners").getKeys(false)) {
            if (e.getItemInHand().hasItemMeta() &&
                    e.getItemInHand().getItemMeta().getDisplayName().equalsIgnoreCase(
                            ChatColor.translateAlternateColorCodes('&', Main.getPlugin().getConfig().getString("spawners." + t + ".name")))) {
                return t;
            }
        }
        return null;
    }

    private void configureSpawner(BlockPlaceEvent e, CreatureSpawner block, String spawnerType) {
        int spawnerDelay = Main.getPlugin().getConfig().getInt("spawners." + spawnerType + ".spawnerInfo.delay");
        int spawnerRange = Main.getPlugin().getConfig().getInt("spawners." + spawnerType + ".spawnerInfo.range");

        SpawnerUpdate spawnerUpdate;
        if (MinecraftVersion.isAtLeastVersion(MinecraftVersion.MC1_17_R1)) {
            spawnerUpdate = new SpawnerUpdate1_20();
        } else if (MinecraftVersion.isAtLeastVersion(MinecraftVersion.MC1_12_R1)) {
            spawnerUpdate = new SpawnerUpdate1_16();
        } else {
            spawnerUpdate = new SpawnerUpdate1_8();
        }

        String spawnerMaterial = Main.getPlugin().getConfig().getString("spawners." + spawnerType + ".spawnerInfo.material");
        String spawnerMode = Main.getPlugin().getConfig().getString("spawners." + spawnerType + ".spawnerInfo.mode");

        spawnerUpdate.spawnerUpdate(e.getBlockPlaced(), e.getBlockPlaced().getLocation(), spawnerMaterial, spawnerType, spawnerMode, spawnerDelay, spawnerRange);

        boolean autoKill = Main.getPlugin().getConfig().getBoolean("config.modules.auto-kill.enabled");
        boolean forceAutoKill = Main.getPlugin().getConfig().getBoolean("config.modules.auto-kill.force");

        if (block.getWorld().getBlockAt(block.getLocation()).getType().equals(block.getType())) {
            ancSpawner spawner = new ancSpawner(e.getBlockPlaced().getWorld(), e.getBlockPlaced().getLocation(), 1, e.getPlayer().getUniqueId().toString(), spawnerType, spawnerMode, autoKill && forceAutoKill, 0);
            Main.getPlugin().getSpawnerManager().addSpawner(spawner);
            Main.getPlugin().getSpawnerManager().updatedSpawnerList.add(spawner);

            SpawnerHologram hologram = new SpawnerHologram_General();
            hologram.createHologramPlace(e.getPlayer(), e.getBlockPlaced());
        }

        Main.getPlugin().sendMessage(e.getPlayer(), "placedSpawner", new String[]{Main.getPlugin().getSpawner(spawnerType).getItemMeta().getDisplayName()});
    }
}
