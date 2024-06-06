package com.ancienty.ancspawners.Listeners;

import com.ancienty.ancspawners.Main;
import com.ancienty.ancspawners.Versions.Holograms.SpawnerHologram_General;
import com.cryptomorin.xseries.XMaterial;
import de.tr7zw.nbtapi.NBT;
import de.tr7zw.nbtapi.iface.ReadableNBT;
import de.tr7zw.nbtapi.utils.MinecraftVersion;
import eu.decentsoftware.holograms.api.DHAPI;
import org.bukkit.Bukkit;
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
import java.util.concurrent.ExecutionException;

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
        Main.database.spawnerHasOwner(clickedBlock).thenAccept(spawnerHasOwner -> {
            if (spawnerHasOwner) {
                try {
                    handleOwnedSpawnerBreak(e, clickedBlock, player, hologramsEnabled, spawnerDropChance);
                } catch (ExecutionException | InterruptedException ex) {
                    throw new RuntimeException(ex);
                }
            } else {
                handleVanillaSpawnerBreak(e, clickedBlock, spawnerDropChance);
            }
        }).exceptionally(ex -> {
            Main.getPlugin().getLogger().severe("Error handling spawner break: " + ex.getMessage());
            e.setCancelled(true);
            return null;
        });
    }

    private void handleOwnedSpawnerBreak(BlockBreakEvent e, Block clickedBlock, Player player, boolean hologramsEnabled, int spawnerDropChance) throws ExecutionException, InterruptedException {
        Main.database.getSpawnerOwnerUuid(clickedBlock).thenAccept(ownerUuid -> {
            if (ownerUuid.equalsIgnoreCase(player.getUniqueId().toString()) || playerHasBreakAllPermission(player)) {
                processSpawnerBreak(e, clickedBlock, player, hologramsEnabled, spawnerDropChance);
            } else {
                handleNotOwner(e, clickedBlock, player, hologramsEnabled);
            }
        }).exceptionally(ex -> {
            Main.getPlugin().getLogger().severe("Error retrieving spawner owner UUID: " + ex.getMessage());
            e.setCancelled(true);
            return null;
        }).handle((result, exception) -> {
            if (exception != null) {
                if (exception instanceof ExecutionException || exception instanceof InterruptedException) {
                    Main.getPlugin().getLogger().severe("Error handling spawner break: " + exception.getMessage());
                    e.setCancelled(true);
                }
            }
            return result;
        });
    }


    private void processSpawnerBreak(BlockBreakEvent e, Block clickedBlock, Player player, boolean hologramsEnabled, int spawnerDropChance) {
        e.setCancelled(true);

        if (!playerSilkTouch(player)) {
            handleNoSilkTouch(e, clickedBlock, player);
            return;
        }

        Main.database.getSpawnerLevel(clickedBlock).thenAccept(spawnerLevel -> {
            if (spawnerLevel == 1) {
                handleLevel1SpawnerBreak(e, clickedBlock, player, hologramsEnabled, spawnerDropChance);
            } else if (spawnerLevel > 1) {
                handleHigherLevelSpawnerBreak(e, clickedBlock, player, spawnerLevel, hologramsEnabled, spawnerDropChance);
            }
        }).exceptionally(ex -> {
            Main.getPlugin().getLogger().severe("Error retrieving spawner level: " + ex.getMessage());
            e.setCancelled(true);
            return null;
        });
    }

    private void handleNoSilkTouch(BlockBreakEvent e, Block clickedBlock, Player player) {
        if (Main.getPlugin().getConfig().getBoolean("config.modules.silk-touch.block")) {
            Main.getPlugin().sendMessage(player, "silkTouchRequired", new String[]{});
        } else {
            e.getBlock().setType(Material.AIR);
            Main.database.breakSpawner(player, clickedBlock);
            Main.getPlugin().sendMessage(player, "noSilkTouchSpawnerLost", new String[]{});
        }
    }

    private void handleLevel1SpawnerBreak(BlockBreakEvent e, Block clickedBlock, Player player, boolean hologramsEnabled, int spawnerDropChance) {
        Main.database.getSpawnerType(clickedBlock).thenAccept(spawnerType -> {
            ItemStack spawner = Main.getPlugin().getSpawner(spawnerType);
            removeHologramIfExists(hologramsEnabled, clickedBlock);
            clickedBlock.setType(Material.AIR);
            Bukkit.getScheduler().runTaskLater(Main.getPlugin(), () -> Main.database.breakSpawner(player, clickedBlock), 1);
            dropSpawnerOrAddToInventory(clickedBlock, player, spawner, spawnerDropChance);
            Main.getPlugin().sendMessage(player, "brokeSpawner", new String[]{spawner.getItemMeta().getDisplayName()});
        }).exceptionally(ex -> {
            Main.getPlugin().getLogger().severe("Error handling level 1 spawner break: " + ex.getMessage());
            e.setCancelled(true);
            return null;
        });
    }

    private void handleHigherLevelSpawnerBreak(BlockBreakEvent e, Block clickedBlock, Player player, int spawnerLevel, boolean hologramsEnabled, int spawnerDropChance) {
        if (player.isSneaking()) {
            handleSneakingHigherLevelSpawnerBreak(e, clickedBlock, player, spawnerLevel, hologramsEnabled, spawnerDropChance);
        } else {
            handleNormalHigherLevelSpawnerBreak(e, clickedBlock, player, hologramsEnabled, spawnerDropChance);
        }
    }

    private void handleSneakingHigherLevelSpawnerBreak(BlockBreakEvent e, Block clickedBlock, Player player, int spawnerLevel, boolean hologramsEnabled, int spawnerDropChance) {
        Main.database.getSpawnerType(clickedBlock).thenAccept(spawnerType -> {
            ItemStack spawner = Main.getPlugin().getSpawner(spawnerType);
            assert spawner != null;
            spawner.setAmount(1);
            Main.database.setSpawnerLevelTo1(player, clickedBlock);
            clickedBlock.getDrops().clear();

            dropMultipleSpawners(clickedBlock, player, spawner, spawnerLevel - 1, spawnerDropChance);
            Main.getPlugin().sendMessage(player, "brokeAllLevels", new String[]{spawner.getItemMeta().getDisplayName()});
            SpawnerHologram_General.updateSpawnerHologram(clickedBlock, Main.database.getHologramName(clickedBlock));
        }).exceptionally(ex -> {
            Main.getPlugin().getLogger().severe("Error handling sneaking higher level spawner break: " + ex.getMessage());
            e.setCancelled(true);
            return null;
        });
    }

    private void handleNormalHigherLevelSpawnerBreak(BlockBreakEvent e, Block clickedBlock, Player player, boolean hologramsEnabled, int spawnerDropChance) {
        Main.database.getSpawnerType(clickedBlock).thenAccept(spawnerType -> {
            ItemStack spawner = Main.getPlugin().getSpawner(spawnerType);
            Main.database.reduceLevelBy1(player, clickedBlock);
            clickedBlock.getDrops().clear();

            dropSpawnerOrAddToInventory(clickedBlock, player, spawner, spawnerDropChance);
            Main.getPlugin().sendMessage(player, "brokeWithLevels", new String[]{spawner.getItemMeta().getDisplayName()});
            SpawnerHologram_General.updateSpawnerHologram(clickedBlock, Main.database.getHologramName(clickedBlock));
        }).exceptionally(ex -> {
            Main.getPlugin().getLogger().severe("Error handling normal higher level spawner break: " + ex.getMessage());
            e.setCancelled(true);
            return null;
        });
    }

    private void handleNotOwner(BlockBreakEvent e, Block clickedBlock, Player player, boolean hologramsEnabled) {
        Main.database.getSpawnerType(clickedBlock).thenAccept(spawnerType -> {
            Main.getPlugin().sendMessage(player, "notTheOwner", new String[]{Main.getPlugin().getSpawner(spawnerType).getItemMeta().getDisplayName()});
        }).exceptionally(ex -> {
            Main.getPlugin().getLogger().severe("Error handling not owner: " + ex.getMessage());
            e.setCancelled(true);
            return null;
        });
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
