package com.ancienty.ancspawnersrecoded.Support.Editors.CustomGetters;

import dev.lone.itemsadder.api.CustomEntity;
import dev.lone.itemsadder.api.CustomStack;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;

public class ItemsAdderGetter implements CustomGetters {


    @Override
    public ItemStack getMaterial(String material_name) {
        String[] parts = material_name.substring(3).split(":");
        if (parts.length == 2) {
            String namespace = parts[0];
            String name = parts[1];
            CustomStack customStack = CustomStack.getInstance(namespace + ":" + name);
            return customStack.getItemStack();
        } else {
            // Send message: spawner misconfigured.
            return null;
        }
    }

    @Override
    public Entity spawnEntity(String entity_name, Location location) {
        String[] parts = entity_name.substring(3).split(":");
        if (parts.length == 2) {
            String namespace = parts[0];
            String name = parts[1];
            CustomEntity entity = CustomEntity.spawn(namespace + ":" + name, location);
            return entity.getEntity();
        } else {
            // Send message: spawner misconfigured.
            return null;
        }
    }
}
