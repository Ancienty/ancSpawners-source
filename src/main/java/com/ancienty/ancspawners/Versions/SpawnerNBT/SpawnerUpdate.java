package com.ancienty.ancspawners.Versions.SpawnerNBT;

import org.bukkit.Location;
import org.bukkit.block.Block;

public interface SpawnerUpdate {

    void spawnerUpdate(Block block, Location location, String entityType, String spawnerName, String mode, int delay, int playerRange);
}
