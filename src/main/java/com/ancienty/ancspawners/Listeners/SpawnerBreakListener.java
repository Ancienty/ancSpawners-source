package com.ancienty.ancspawners.Listeners;

import com.ancienty.ancspawners.Main;
import com.ancienty.ancspawners.SpawnerManager.ancSpawner;
import com.ancienty.ancspawners.Versions.Holograms.SpawnerHologram_General;
import com.cryptomorin.xseries.XMaterial;
import de.tr7zw.nbtapi.NBT;
import de.tr7zw.nbtapi.utils.MinecraftVersion;
import eu.decentsoftware.holograms.api.DHAPI;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Random;

public class SpawnerBreakListener implements Listener {

    private int getDropChance(Player player) {
        FileConfiguration config = Main.getPlugin().getConfig();
        int dropChance = config.getInt("config.modules.spawner-drop-chance.default", 100);

        if (config.isConfigurationSection("config.modules.spawner-drop-chance.permissions")) {
            for (String permission : config.getConfigurationSection("config.modules.spawner-drop-chance.permissions").getKeys(false)) {
                if (Main.getPermissions().has(player, permission)) {
                    dropChance = config.getInt("config.modules.spawner-drop-chance.permissions." + permission, dropChance);
                }
            }
        }
        return dropChance;
    }

    private boolean playerSilkTouch(Player player) {
        FileConfiguration config = Main.getPlugin().getConfig();
        if (config.getBoolean("config.modules.silk-touch.enabled")) {
            ItemStack item = player.getInventory().getItemInMainHand();
            if (item.hasItemMeta() && item.getItemMeta().hasEnchant(Enchantment.SILK_TOUCH)) {
                return true;
            }
            return Main.getPermissions().has(player, config.getString("config.modules.silk-touch.exempt"));
        }
        return true;
    }

    private boolean playerHasBreakAllPermission(Player player) {
        return Main.getPermissions().has(player, Main.getPlugin().getConfig().getString("config.modules.break-spawners.permission"));
    }

    private void handleSpawnerBreak(BlockBreakEvent e, Block clickedBlock, Player player, boolean hologramsEnabled, int spawnerDropChance) {
        ancSpawner spawner = Main.getPlugin().getSpawnerManager().getSpawner(clickedBlock.getWorld(), clickedBlock.getLocation());
        if (spawner != null) {
            if (spawner.getOwnerUUID().equalsIgnoreCase(player.getUniqueId().toString()) || playerHasBreakAllPermission(player)) {
                processSpawnerBreak(e, clickedBlock, player, hologramsEnabled, spawnerDropChance, spawner);
            } else {
                handleNotOwner(e, clickedBlock, player, spawner, hologramsEnabled);
            }
        } else {
            handleVanillaSpawnerBreak(e, clickedBlock, spawnerDropChance);
        }
    }

    private void processSpawnerBreak(BlockBreakEvent e, Block clickedBlock, Player player, boolean hologramsEnabled, int spawnerDropChance, ancSpawner spawner) {
        e.setCancelled(true);

        if (!playerSilkTouch(player)) {
            handleNoSilkTouch(e, clickedBlock, player, spawner);
            return;
        }

        int spawnerLevel = spawner.getLevel();
        if (spawnerLevel == 1) {
            handleLevel1SpawnerBreak(e, clickedBlock, player, hologramsEnabled, spawnerDropChance, spawner);
        } else if (spawnerLevel > 1) {
            handleHigherLevelSpawnerBreak(e, clickedBlock, player, spawnerLevel, hologramsEnabled, spawnerDropChance, spawner);
        }
    }

    private void handleNoSilkTouch(BlockBreakEvent e, Block clickedBlock, Player player, ancSpawner spawner) {
        if (Main.getPlugin().getConfig().getBoolean("config.modules.silk-touch.block")) {
            Main.getPlugin().sendMessage(player, "silkTouchRequired", new String[]{});
        } else {
            e.getBlock().setType(Material.AIR);
            Main.getPlugin().getSpawnerManager().removeSpawner(spawner);
            Main.getPlugin().sendMessage(player, "noSilkTouchSpawnerLost", new String[]{});
        }
    }

    private void handleLevel1SpawnerBreak(BlockBreakEvent e, Block clickedBlock, Player player, boolean hologramsEnabled, int spawnerDropChance, ancSpawner spawner) {
        String spawnerType = spawner.getType();
        ItemStack spawnerItem = Main.getPlugin().getSpawner(spawnerType);
        removeHologramIfExists(hologramsEnabled, clickedBlock);
        clickedBlock.setType(Material.AIR);
        Main.getPlugin().getSpawnerManager().removeSpawner(spawner);
        dropSpawnerOrAddToInventory(clickedBlock, player, spawnerItem, spawnerDropChance);
        Main.getPlugin().sendMessage(player, "brokeSpawner", new String[]{spawnerItem.getItemMeta().getDisplayName()});
    }

    private void handleHigherLevelSpawnerBreak(BlockBreakEvent e, Block clickedBlock, Player player, int spawnerLevel, boolean hologramsEnabled, int spawnerDropChance, ancSpawner spawner) {
        if (player.isSneaking()) {
            handleSneakingHigherLevelSpawnerBreak(e, clickedBlock, player, spawnerLevel, hologramsEnabled, spawnerDropChance, spawner);
        } else {
            handleNormalHigherLevelSpawnerBreak(e, clickedBlock, player, hologramsEnabled, spawnerDropChance, spawner);
        }
    }

    private void handleSneakingHigherLevelSpawnerBreak(BlockBreakEvent e, Block clickedBlock, Player player, int spawnerLevel, boolean hologramsEnabled, int spawnerDropChance, ancSpawner spawner) {
        String spawnerType = spawner.getType();
        ItemStack spawnerItem = Main.getPlugin().getSpawner(spawnerType);
        spawnerItem.setAmount(1);
        spawner.setLevel(1);
        clickedBlock.getDrops().clear();

        dropMultipleSpawners(clickedBlock, player, spawnerItem, spawnerLevel - 1, spawnerDropChance);
        Main.getPlugin().sendMessage(player, "brokeAllLevels", new String[]{spawnerItem.getItemMeta().getDisplayName()});
        SpawnerHologram_General.updateSpawnerHologram(clickedBlock, Main.database.getHologramName(clickedBlock));
    }

    private void handleNormalHigherLevelSpawnerBreak(BlockBreakEvent e, Block clickedBlock, Player player, boolean hologramsEnabled, int spawnerDropChance, ancSpawner spawner) {
        String spawnerType = spawner.getType();
        ItemStack spawnerItem = Main.getPlugin().getSpawner(spawnerType);
        spawner.setLevel(spawner.getLevel() - 1);
        clickedBlock.getDrops().clear();

        dropSpawnerOrAddToInventory(clickedBlock, player, spawnerItem, spawnerDropChance);
        Main.getPlugin().sendMessage(player, "brokeWithLevels", new String[]{spawnerItem.getItemMeta().getDisplayName()});
        SpawnerHologram_General.updateSpawnerHologram(clickedBlock, Main.database.getHologramName(clickedBlock));
    }

    private void handleNotOwner(BlockBreakEvent e, Block clickedBlock, Player player, ancSpawner spawner, boolean hologramsEnabled) {
        String spawnerType = spawner.getType();
        e.setCancelled(true);
        Main.getPlugin().sendMessage(player, "notTheOwner", new String[]{Main.getPlugin().getSpawner(spawnerType).getItemMeta().getDisplayName()});
    }

    private void handleVanillaSpawnerBreak(BlockBreakEvent e, Block clickedBlock, int spawnerDropChance) {
        if (Main.getPlugin().getConfig().getString("config.modules.vanilla-spawner-support.enabled").equalsIgnoreCase("true")) {
            e.setCancelled(true);
            CreatureSpawner spawner = (CreatureSpawner) e.getBlock().getState();
            String entity = getSpawnerEntity(spawner);

            if (entity != null) {
                entity = entity.toUpperCase();
                ItemStack itemStack = Main.getPlugin().getSpawnerByEntity(entity);
                if (itemStack != null) {
                    clickedBlock.setType(Material.AIR);
                    dropSpawnerOrAddToInventory(clickedBlock, e.getPlayer(), itemStack, spawnerDropChance);
                    Main.getPlugin().sendMessage(e.getPlayer(), "vanillaSpawnerBroken", new String[]{});
                } else {
                    Main.getPlugin().sendMessage(e.getPlayer(), "vanillaSpawnerNotConfigured", new String[]{});
                }
            } else {
                Main.getPlugin().sendMessage(e.getPlayer(), "vanillaSpawnerEntityNotFound", new String[]{});
            }
        }
    }

    private String getSpawnerEntity(CreatureSpawner spawner) {
        if (MinecraftVersion.isAtLeastVersion(MinecraftVersion.MC1_12_R1)) {
            return spawner.getSpawnedType().toString();
        } else {
            final String[] entityFromNbt = new String[1];
            NBT.get(spawner.getBlock().getState(), nbt -> {
                entityFromNbt[0] = nbt.getString("EntityId");
                return entityFromNbt[0];
            });
            return entityFromNbt[0];
        }
    }

    private void removeHologramIfExists(boolean hologramsEnabled, Block block) {
        if (hologramsEnabled) {
            String hologramName = Main.database.getHologramName(block);
            if (DHAPI.getHologram(hologramName) != null) {
                DHAPI.removeHologram(hologramName);
            }
        }
    }

    private void dropSpawnerOrAddToInventory(Block clickedBlock, Player player, ItemStack spawner, int spawnerDropChance) {
        Random random = new Random();
        boolean autopickup = Main.getPlugin().getConfig().getBoolean("config.modules.auto-pickup.enabled");
        boolean hasAutoPickupPerm = Main.getPermissions().has(player, Main.getPlugin().getConfig().getString("config.modules.auto-pickup.permission"));

        if (spawnerDropChance >= random.nextInt(101)) {
            if (autopickup && hasAutoPickupPerm) {
                if (player.getInventory().firstEmpty() != -1) {
                    player.getInventory().addItem(spawner);
                } else {
                    clickedBlock.getLocation().getWorld().dropItemNaturally(clickedBlock.getLocation(), spawner);
                }
            } else {
                clickedBlock.getLocation().getWorld().dropItemNaturally(clickedBlock.getLocation(), spawner);
            }
        }
    }

    private void dropMultipleSpawners(Block clickedBlock, Player player, ItemStack spawner, int amountToDrop, int spawnerDropChance) {
        Random random = new Random();
        boolean autopickup = Main.getPlugin().getConfig().getBoolean("config.modules.auto-pickup.enabled");
        boolean hasAutoPickupPerm = Main.getPermissions().has(player, Main.getPlugin().getConfig().getString("config.modules.auto-pickup.permission"));

        while (amountToDrop > 0) {
            ItemStack spawnerToDrop = spawner.clone();
            if (amountToDrop > 64) {
                spawnerToDrop.setAmount(64);
                amountToDrop -= 64;
            } else {
                spawnerToDrop.setAmount(amountToDrop);
                amountToDrop = 0;
            }

            if (spawnerDropChance >= random.nextInt(101)) {
                if (autopickup && hasAutoPickupPerm) {
                    if (player.getInventory().firstEmpty() != -1) {
                        player.getInventory().addItem(spawnerToDrop);
                    } else {
                        clickedBlock.getLocation().getWorld().dropItemNaturally(clickedBlock.getLocation(), spawnerToDrop);
                    }
                } else {
                    clickedBlock.getLocation().getWorld().dropItemNaturally(clickedBlock.getLocation(), spawnerToDrop);
                }
            }
        }
    }

    @EventHandler
    public void onSpawnerBreakEvent(BlockBreakEvent e) {
        if (e.isCancelled() || e.getBlock().getType() != XMaterial.SPAWNER.parseMaterial()) {
            return;
        }

        Block clickedBlock = e.getBlock();
        Player player = e.getPlayer();
        boolean hologramsEnabled = Main.getPlugin().getConfig().getBoolean("config.modules.hologram.enabled");
        int spawnerDropChance = getDropChance(player);

        handleSpawnerBreak(e, clickedBlock, player, hologramsEnabled, spawnerDropChance);
    }
}