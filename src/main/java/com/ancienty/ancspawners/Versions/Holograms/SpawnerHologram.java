package com.ancienty.ancspawners.Versions.Holograms;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

public interface SpawnerHologram {

    void createHologram(Player player, Block block);
    void createHologramPlace(Player player, Block block);
}
