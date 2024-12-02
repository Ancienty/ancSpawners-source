package com.ancienty.ancspawnersrecoded.Listeners;

import com.ancienty.ancspawnersrecoded.Main;
import com.ancienty.ancspawnersrecoded.Support.Editors.HologramEditor;
import com.ancienty.ancspawnersrecoded.Support.Editors.SpawnerConfiguration.DefaultConfiguration;
import com.ancienty.ancspawnersrecoded.Support.Protection.GriefPreventionSupport;
import com.ancienty.ancspawnersrecoded.Support.Protection.SSB2Support;
import com.ancienty.ancspawnersrecoded.Support.Protection.TownySupport;
import com.ancienty.ancspawnersrecoded.Support.Protection.WorldGuardSupport;
import com.cryptomorin.xseries.XMaterial;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;

import java.util.HashMap;
import java.util.Locale;

public class SpawnerPlaceListener implements Listener {

    @EventHandler(priority = EventPriority.LOWEST)
    public void onSpawnerPlace(BlockPlaceEvent e) {
        // Check if the block placed is a spawner
        if (e.getBlockPlaced().getType() != XMaterial.SPAWNER.parseMaterial()) {
            return;
        }

        // Check if the event is cancelled
        if (e.isCancelled()) {
            return;
        }

        // Check if the protection elements allow block place
        if (!canPlaceSpawner(e)) {
            return;
        }

        HashMap<String, String> spawner_hash = Main.getPlugin().getSpawnerFromItemStack(e.getItemInHand());

        if (spawner_hash == null) {
            Main.getPlugin().sendMessage(e.getPlayer(), "errorWithSpawner", new String[]{});
            e.setCancelled(true);
            return;
        }

        String spawner_config_name = spawner_hash.get("SpawnerConfigName");
        String spawner_type = spawner_hash.get("SpawnerType");
        String spawner_mode = spawner_hash.get("SpawnerMode");

        if (spawner_type == null || spawner_mode == null) {
            Main.getPlugin().sendMessage(e.getPlayer(), "errorWithSpawner", new String[]{});
            e.setCancelled(true);
            return;
        }

        new DefaultConfiguration().configureSpawner(e, spawner_config_name, spawner_type, spawner_mode);

        boolean autoKill = Main.getPlugin().getAutoKillModule().getBoolean("auto-kill.enabled");
        boolean forceAutoKill = Main.getPlugin().getAutoKillModule().getBoolean("auto-kill.force");
        boolean virtual_storage = Main.getPlugin().getSettingsModule().getBoolean("virtual-storage.default");
        boolean xp_storage = Main.getPlugin().getSettingsModule().getBoolean("xp-storage.default");

        String spawner_name;
        if (!spawner_config_name.equalsIgnoreCase("default")) {
            spawner_name = Main.getPlugin().getConfig().getString("spawners." + spawner_config_name + ".name");
        } else {
            spawner_name = Main.getPlugin().getConfig().getString("spawners.default.name");
            spawner_name = spawner_name.replace("{entity_name}", spawner_type.toUpperCase(Locale.ENGLISH));
        }
        spawner_name = ChatColor.translateAlternateColorCodes('&', spawner_name);
        String finalSpawner_name = spawner_name;
        Location location = e.getBlockPlaced().getLocation();

        Bukkit.getScheduler().runTaskLater(Main.getPlugin(), () -> {
            if (e.getBlockPlaced().getWorld().getBlockAt(location).getType().equals(XMaterial.SPAWNER.parseMaterial())) {
                Main.getPlugin().getSpawnerManager().createSpawner(e.getBlockPlaced().getWorld(), e.getBlockPlaced().getLocation(), spawner_config_name, 1, e.getPlayer().getUniqueId().toString(), spawner_type, spawner_mode, autoKill && forceAutoKill, 0, virtual_storage, xp_storage);
                HologramEditor.toggleHologram(e.getPlayer(), e.getBlockPlaced(), true);
                Main.getPlugin().sendMessage(e.getPlayer(), "placedSpawner", new String[]{finalSpawner_name});
            }
        }, 2);
    }

    public boolean canPlaceSpawner(BlockPlaceEvent e) {
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
}
