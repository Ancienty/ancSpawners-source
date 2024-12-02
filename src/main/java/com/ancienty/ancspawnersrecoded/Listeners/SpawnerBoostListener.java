package com.ancienty.ancspawnersrecoded.Listeners;

import com.ancienty.ancspawnersrecoded.Database.DatabaseTask;
import com.ancienty.ancspawnersrecoded.Main;
import com.ancienty.ancspawnersrecoded.SpawnerManager.ancSpawner;
import de.tr7zw.nbtapi.NBTItem;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class SpawnerBoostListener implements Listener {

    @EventHandler
    public void onPlayerUseBoost(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack itemInHand = player.getInventory().getItemInMainHand();
        if (itemInHand == null || itemInHand.getType() == Material.AIR) {
            return;
        }

        // Check if the item has the BoostType NBT tag
        NBTItem nbtItem = new NBTItem(itemInHand);
        if (nbtItem.hasKey("BoostType")) {
            Block clickedBlock = event.getClickedBlock();
            if (clickedBlock != null && clickedBlock.getType() == Material.SPAWNER) {
                // Cancel the event to prevent default behavior
                event.setCancelled(true);

                // Get the spawner at the clicked location
                ancSpawner spawner = Main.getPlugin().getSpawnerManager().getSpawner(clickedBlock.getWorld(), clickedBlock.getLocation());
                if (spawner == null) {
                    Main.getPlugin().sendMessage(player, "spawner_not_managed", new String[]{});
                    return;
                }

                // Get the boost type from the item, standardize it to lowercase
                String boostType = nbtItem.getString("BoostType").toLowerCase();
                double boostAmount = nbtItem.getDouble("BoostAmount");
                long boostDuration = nbtItem.getLong("BoostDuration");

                // Check if the same boost type is already active
                if (spawner.getBoosts().getActiveBoosts().containsKey(boostType)) {
                    // Send message to the player that the boost is already active
                    Main.getPlugin().sendMessage(player, "boost_already_active", new String[]{boostType});
                    return;
                }

                long endTime = System.currentTimeMillis() + boostDuration;

                // Create the database task to insert the boost into the database
                String insertBoostQuery = "INSERT INTO boosts (world, location, boost_type, end_date, boost_amount) VALUES (?, ?, ?, ?, ?)";
                DatabaseTask insertBoostTask = new DatabaseTask(insertBoostQuery, new Object[]{
                        spawner.getWorld().getName(),
                        Main.getPlugin().getSpawnerManager().getLocation(spawner),
                        boostType,
                        endTime,
                        boostAmount
                });

                // Add the task to the operations queue
                Main.getPlugin().addDatabaseTask(insertBoostTask);

                // Remove one boost item from the player's hand
                itemInHand.setAmount(itemInHand.getAmount() - 1);
                if (itemInHand.getAmount() > 0) {
                    player.getInventory().setItemInMainHand(itemInHand);
                } else {
                    player.getInventory().setItemInMainHand(null);
                }

                // Get the boost name from lang.yml
                String boostNameKey = "";
                String boostAmountFormatted = String.format("%.2f", boostAmount);
                if (boostType.equalsIgnoreCase("spawn_amount")) {
                    boostNameKey = "boost_spawn_amount";
                } else if (boostType.equalsIgnoreCase("spawn_time")) {
                    boostNameKey = "boost_spawn_time";
                } else if (boostType.equalsIgnoreCase("sell_multiplier")) {
                    boostNameKey = "boost_sell_multiplier";
                } else {
                    // Default boost name if not defined
                    boostNameKey = "boost_" + boostType;
                }

                String boostDisplayName = Main.getPlugin().lang.getString("lang." + boostNameKey);
                if (boostDisplayName == null) {
                    boostDisplayName = "&7" + boostType + " &8(&6{0}&8)";
                }

                boostDisplayName = boostDisplayName.replace("{0}", boostAmountFormatted).replace(" &8[&f{1}&8]", "");

                // Get the boost applied message from lang.yml
                Main.getPlugin().sendMessage(player, "boost_applied", new String[]{boostDisplayName});

                // Reload boosts for the spawner after a short delay to ensure the database operation has completed
                Main.getPlugin().getServer().getScheduler().runTaskLater(Main.getPlugin(), spawner::reloadBoosts, 2L);
            }
        }
    }
}
