package com.ancienty.ancspawnersrecoded.Utils;

import com.ancienty.ancspawnersrecoded.Main;
import com.cryptomorin.xseries.XMaterial;
import de.tr7zw.nbtapi.NBTItem;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class BoostUtils {

    public static ItemStack createBoostItem(String boostKey) {
        ConfigurationSection boostSection = Main.getPlugin().getBoostsModule().getConfigurationSection("boosts.list." + boostKey);
        if (boostSection == null) {
            return null; // Boost not found in the config
        }

        String name = ChatColor.translateAlternateColorCodes('&', boostSection.getString("name", "&fUnknown Boost"));
        List<String> loreConfig = boostSection.getStringList("lore");
        List<String> lore = new ArrayList<>();
        for (String line : loreConfig) {
            lore.add(ChatColor.translateAlternateColorCodes('&', line));
        }

        // Get the material from the boost configuration
        String materialName = boostSection.getString("material", "PAPER");
        Material material = XMaterial.matchXMaterial(materialName.toUpperCase()).orElse(XMaterial.PAPER).parseMaterial();

        ItemStack boostItem = new ItemStack(material != null ? material : Material.PAPER);
        ItemMeta meta = boostItem.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(lore);
            boostItem.setItemMeta(meta);

            // Add NBT data
            NBTItem nbtItem = new NBTItem(boostItem);

            String boostType = boostSection.getString("type", "").toLowerCase();
            double boostAmount = boostSection.getDouble("amount", 0.0);
            String durationStr = boostSection.getString("duration", "0h");
            long durationMillis = parseDuration(durationStr);

            nbtItem.setString("BoostKey", boostKey);
            nbtItem.setString("BoostType", boostType);
            nbtItem.setDouble("BoostAmount", boostAmount);
            nbtItem.setLong("BoostDuration", durationMillis); // Store the duration instead of end time

            boostItem = nbtItem.getItem();
        }

        return boostItem;
    }

    /**
     * Parses a duration string like "1d", "12h", or "30m" into milliseconds.
     */
    public static long parseDuration(String durationStr) {
        durationStr = durationStr.toLowerCase();
        long duration = 0L;
        try {
            if (durationStr.endsWith("d")) {
                int days = Integer.parseInt(durationStr.replace("d", ""));
                duration = days * 24L * 60L * 60L * 1000L;
            } else if (durationStr.endsWith("h")) {
                int hours = Integer.parseInt(durationStr.replace("h", ""));
                duration = hours * 60L * 60L * 1000L;
            } else if (durationStr.endsWith("m")) {
                int minutes = Integer.parseInt(durationStr.replace("m", ""));
                duration = minutes * 60L * 1000L;
            } else if (durationStr.endsWith("s")) {
                int seconds = Integer.parseInt(durationStr.replace("s", ""));
                duration = seconds * 1000L;
            }
        } catch (NumberFormatException e) {
            // Handle invalid format
            duration = 0L;
        }
        return duration;
    }
}
