package com.ancienty.ancspawnersrecoded.Listeners;

import com.ancienty.ancspawnersrecoded.Main;
import com.ancienty.ancspawnersrecoded.SpawnerManager.SpawnerManager;
import com.ancienty.ancspawnersrecoded.SpawnerManager.ancSpawner;
import com.cryptomorin.xseries.XMaterial;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.BlockState;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

public class SpawnerBreakListener implements Listener {

    @EventHandler
    public void onSpawnerBreak(BlockBreakEvent e) {
        if (e.getBlock().getType() != XMaterial.SPAWNER.parseMaterial()) {
            return;
        }

        Player player = e.getPlayer();
        SpawnerManager manager = Main.getPlugin().getSpawnerManager();
        ancSpawner spawner = manager.getSpawner(e.getBlock().getWorld(), e.getBlock().getLocation());
        BlockState blockState = e.getBlock().getState();

        // Ensure the block state is a CreatureSpawner to avoid casting exceptions
        if (!(blockState instanceof CreatureSpawner)) {
            Main.getPlugin().sendMessage(player, "errorWithSpawner", new String[]{});
            return;
        }

        CreatureSpawner creatureSpawner = (CreatureSpawner) blockState;
        Random random = new Random();

        if (spawner == null) {
            e.setCancelled(true);
            // This spawner is not registered in the database, so it must be a vanilla spawner.
            if (isVanillaSupportEnabled()) {
                if (playerSilkTouchCheck(player)) {
                    ItemStack spawner_item = Main.getPlugin().getSpawner(creatureSpawner.getSpawnedType().toString());

                    // Check for null spawner items to avoid NullPointerException
                    if (spawner_item == null) {
                        Main.getPlugin().sendMessage(player, "errorWithSpawner", new String[]{});
                        return;
                    }

                    double spawner_drop_chance = getSpawnerDropChance(player);

                    if (random.nextDouble() < spawner_drop_chance / 100) {
                        // Send message: spawner broken
                        Main.getPlugin().sendMessage(player, "vanillaSpawnerBroken", new String[]{});
                        e.getBlock().setType(XMaterial.AIR.parseMaterial());
                        if (isAutoPickupEnabledForPlayer(player)) {
                            // Handle adding items to inventory with stack limits and inventory space
                            Map<Integer, ItemStack> leftovers = player.getInventory().addItem(spawner_item);
                            if (!leftovers.isEmpty()) {
                                // Inventory full, drop leftovers
                                for (ItemStack item : leftovers.values()) {
                                    dropSpawners(item, item.getAmount(), e.getBlock().getLocation());
                                }
                            }
                        } else {
                            dropSpawners(spawner_item, 1, e.getBlock().getLocation());
                        }
                    } else {
                        // Send message: spawner lost
                        e.getBlock().setType(XMaterial.AIR.parseMaterial());
                        Main.getPlugin().sendMessage(player, "noSilkTouchSpawnerLost", new String[]{});
                    }
                } else {
                    // Send message: silk touch required
                    Main.getPlugin().sendMessage(player, "silkTouchRequired", new String[]{});
                    if (!Main.getPlugin().getOtherModule().getBoolean("silk-touch.block")) {
                        e.getBlock().setType(XMaterial.AIR.parseMaterial());
                    }
                }
            } else {
                // Send message: vanilla spawners not supported
                Main.getPlugin().sendMessage(player, "vanillaSpawnersNotSupported", new String[]{});
            }
        } else {
            e.setCancelled(true);
            if (spawner.getOwnerUUID().equals(player.getUniqueId().toString()) || hasBreakAllPermission(player)) {
                int spawner_current_level = spawner.getLevel();
                double spawner_drop_chance = getSpawnerDropChance(player);
                ItemStack spawner_item = Main.getPlugin().getSpawnerFromSpawnerBlock(spawner);

                // Check for null spawner items to avoid NullPointerException
                if (spawner_item == null) {
                    Main.getPlugin().sendMessage(player, "errorWithSpawner", new String[]{});
                    return;
                }

                if (playerSilkTouchCheck(player)) {
                    if (spawner_current_level > 1) {
                        if (player.isSneaking()) {
                            // Reduce the spawner to level 1
                            spawner.setLevel(1);
                            int totalSpawners = spawner_current_level - 1;
                            int spawnersDropped = 0;
                            int spawnersLost = 0;

                            // Loop through each spawner and apply drop chance
                            for (int i = 0; i < totalSpawners; i++) {
                                if (random.nextDouble() < spawner_drop_chance / 100) {
                                    spawnersDropped++;
                                } else {
                                    spawnersLost++;
                                }
                            }

                            // Handle spawnersDropped
                            if (spawnersDropped > 0) {
                                ItemStack spawner_item_to_give = spawner_item.clone();
                                spawner_item_to_give.setAmount(spawnersDropped);

                                if (isAutoPickupEnabledForPlayer(player)) {
                                    Map<Integer, ItemStack> leftovers = addItemsToInventory(player, spawner_item_to_give, spawnersDropped);
                                    if (!leftovers.isEmpty()) {
                                        for (ItemStack item : leftovers.values()) {
                                            dropSpawners(item, item.getAmount(), e.getBlock().getLocation());
                                        }
                                    }
                                } else {
                                    dropSpawners(spawner_item_to_give, spawnersDropped, e.getBlock().getLocation());
                                }
                            }

                            // If spawnersLost > 0, send message
                            if (spawnersLost > 0) {
                                Main.getPlugin().sendMessage(player, "chanceByCount", new String[]{String.valueOf(spawnersLost)});
                            }

                            // Send message: broke all levels
                            Main.getPlugin().sendMessage(player, "brokeAllLevels", new String[]{spawner.getType()});
                        } else {
                            spawner.setLevel(spawner_current_level - 1);

                            if (random.nextDouble() < spawner_drop_chance / 100) {
                                // Send message: broke with levels
                                Main.getPlugin().sendMessage(player, "brokeWithLevels", new String[]{spawner.getType()});

                                if (isAutoPickupEnabledForPlayer(player)) {
                                    Map<Integer, ItemStack> leftovers = addItemsToInventory(player, spawner_item, 1);
                                    if (!leftovers.isEmpty()) {
                                        for (ItemStack item : leftovers.values()) {
                                            dropSpawners(item, item.getAmount(), e.getBlock().getLocation());
                                        }
                                    }
                                } else {
                                    dropSpawners(spawner_item, 1, e.getBlock().getLocation());
                                }
                            } else {
                                // Send message: spawner lost
                                Main.getPlugin().sendMessage(player, "noSilkTouchSpawnerLost", new String[]{});
                            }
                        }
                    } else {
                        Main.getPlugin().getSpawnerManager().deleteSpawner(spawner);
                        e.getBlock().setType(XMaterial.AIR.parseMaterial());

                        if (random.nextDouble() < spawner_drop_chance / 100) {
                            // Send message: broke spawner
                            String spawner_name;
                            if (!spawner.getConfigName().equalsIgnoreCase("default")) {
                                spawner_name = Main.getPlugin().getConfig().getString("spawners." + spawner.getConfigName() + ".name");
                            } else {
                                spawner_name = Main.getPlugin().getConfig().getString("spawners.default.name");
                                spawner_name = spawner_name.replace("{entity_name}", spawner.getType().toUpperCase(Locale.ENGLISH));
                            }
                            spawner_name = ChatColor.translateAlternateColorCodes('&', spawner_name);
                            Main.getPlugin().sendMessage(player, "brokeSpawner", new String[]{spawner_name});

                            if (isAutoPickupEnabledForPlayer(player)) {
                                Map<Integer, ItemStack> leftovers = addItemsToInventory(player, spawner_item, 1);
                                if (!leftovers.isEmpty()) {
                                    for (ItemStack item : leftovers.values()) {
                                        dropSpawners(item, item.getAmount(), e.getBlock().getLocation());
                                    }
                                }
                            } else {
                                dropSpawners(spawner_item, 1, e.getBlock().getLocation());
                            }
                        } else {
                            // Send message: spawner lost
                            Main.getPlugin().sendMessage(player, "noSilkTouchSpawnerLost", new String[]{});
                        }
                    }
                } else {
                    if (Main.getPlugin().getOtherModule().getBoolean("silk-touch.block")) {
                        // Send message: silk touch required
                        Main.getPlugin().sendMessage(player, "silkTouchRequired", new String[]{});
                        return;
                    } else {
                        e.getBlock().setType(XMaterial.AIR.parseMaterial());
                        // Send message: spawner lost
                        Main.getPlugin().sendMessage(player, "noSilkTouchSpawnerLost", new String[]{});
                    }
                }
                Main.getPlugin().updateSpawnerHologram(player, e.getBlock(), true);
            } else {
                // Send message: you're not the owner
                Main.getPlugin().sendMessage(player, "notTheOwner", new String[]{});
            }
        }
    }

    // Helper method to add items to player's inventory and handle stack limits
    private Map<Integer, ItemStack> addItemsToInventory(Player player, ItemStack itemStack, int totalAmount) {
        Map<Integer, ItemStack> leftovers = Collections.emptyMap(); // Initialize with an empty map
        while (totalAmount > 0) {
            ItemStack itemToAdd = itemStack.clone();
            int amountToAdd = Math.min(totalAmount, itemStack.getMaxStackSize());
            itemToAdd.setAmount(amountToAdd);
            totalAmount -= amountToAdd;
            leftovers = player.getInventory().addItem(itemToAdd);
            if (!leftovers.isEmpty()) {
                // If there are leftovers, break the loop and return them
                break;
            }
        }
        return leftovers;
    }

    public void dropSpawners(ItemStack itemStack, int amount, Location location) {
        while (amount > 0) {
            ItemStack item_to_drop = itemStack.clone();
            int drop_amount = Math.min(amount, itemStack.getMaxStackSize());
            item_to_drop.setAmount(drop_amount);
            amount -= drop_amount;
            location.getWorld().dropItemNaturally(location, item_to_drop);
        }
    }

    public boolean isVanillaSupportEnabled() {
        return Main.getPlugin().getOtherModule().getBoolean("vanilla-spawner-support.enabled");
    }

    public boolean playerSilkTouchCheck(Player player) {
        if (Main.getPlugin().getOtherModule().getBoolean("silk-touch.enabled")) {
            ItemStack itemStack = player.getInventory().getItemInMainHand();
            if (itemStack != null && itemStack.containsEnchantment(Enchantment.SILK_TOUCH)) {
                return true;
            } else {
                return Main.getPermissions().has(player, Main.getPlugin().getOtherModule().getString("silk-touch.exempt"));
            }
        } else {
            return true;
        }
    }

    public boolean hasBreakAllPermission(Player player) {
        return Main.getPermissions().has(player, Main.getPlugin().getOtherModule().getString("break-spawners.permission"));
    }

    public boolean isAutoPickupEnabledForPlayer(Player player) {
        return Main.getPlugin().getOtherModule().getBoolean("auto-pickup.enabled") &&
                Main.getPermissions().has(player, Main.getPlugin().getOtherModule().getString("auto-pickup.permission"));
    }

    public double getSpawnerDropChance(Player player) {
        double max_chance = Main.getPlugin().getOtherModule().getDouble("spawner-drop-chance.default");
        if (Main.getPlugin().getOtherModule().getConfigurationSection("spawner-drop-chance.permissions") != null) {
            for (String permission : Main.getPlugin().getOtherModule().getConfigurationSection("spawner-drop-chance.permissions").getKeys(false)) {
                if (Main.getPermissions().has(player, permission)) {
                    max_chance = Math.max(max_chance, Main.getPlugin().getOtherModule().getDouble("spawner-drop-chance.permissions." + permission));
                }
            }
        }
        return max_chance;
    }
}