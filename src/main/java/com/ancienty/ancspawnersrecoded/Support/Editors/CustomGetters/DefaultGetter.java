package com.ancienty.ancspawnersrecoded.Support.Editors.CustomGetters;

import com.ancienty.ancspawnersrecoded.Main;
import com.cryptomorin.xseries.XEntity;
import com.cryptomorin.xseries.XEntityType;
import com.cryptomorin.xseries.XMaterial;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;

import java.util.Locale;

public class DefaultGetter implements CustomGetters {


    @Override
    public ItemStack getMaterial(String material_name) {
        try {
            return XMaterial.valueOf(material_name.toUpperCase(Locale.ENGLISH)).parseItem();
        } catch (IllegalArgumentException e) {
            Main.getPlugin().getLogger().warning("Cannot get Material: " + material_name);
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public Entity spawnEntity(String entity_name, Location location) {
        try {
            EntityType entityType = XEntityType.valueOf(entity_name.toUpperCase(Locale.ENGLISH)).get();
            return location.getWorld().spawnEntity(location, entityType);
        } catch (IllegalArgumentException e) {
            Main.getPlugin().getLogger().warning("Cannot get Entity: " + entity_name);
            e.printStackTrace();
            return null;
        }
    }
}
