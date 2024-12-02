package com.ancienty.ancspawnersrecoded.Listeners;

import com.ancienty.ancspawnersrecoded.Main;
import com.ancienty.ancspawnersrecoded.SpawnerManager.ancSpawner;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.SpawnerSpawnEvent;

public class SpawnerSpawnListener implements Listener {

    @EventHandler
    public void onSpawnerSpawn(SpawnerSpawnEvent e) {
        Block block = e.getSpawner().getBlock();
        ancSpawner spawner = Main.getPlugin().getSpawnerManager().getSpawner(block.getWorld(), block.getLocation());
        if (spawner != null) {
            e.getEntity().remove();
        }
    }
}
