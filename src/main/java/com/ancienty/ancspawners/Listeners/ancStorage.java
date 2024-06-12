package com.ancienty.ancspawners.Listeners;

import com.ancienty.ancspawners.Main;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;

public class ancStorage {

    HashMap<ItemStack, Integer> storage = new HashMap<>();

    public ancStorage(World world, Location location) {
        Block block = world.getBlockAt(location);
        Main.database.getStoredItems(block).thenAccept(stored_items -> {

        });
    }
}
