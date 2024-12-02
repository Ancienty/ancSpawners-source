package com.ancienty.ancspawnersrecoded.Support.Editors.CustomGetters;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;

public interface CustomGetters {

    ItemStack getMaterial(String material_name);
    Entity spawnEntity(String entity_name, Location location);
}
