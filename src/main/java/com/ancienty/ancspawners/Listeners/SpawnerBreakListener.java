package com.ancienty.ancspawners.Listeners;

import com.ancienty.ancspawners.Main;
import com.ancienty.ancspawners.Versions.Holograms.SpawnerHologram_General;
import com.cryptomorin.xseries.XMaterial;
import de.tr7zw.nbtapi.NBT;
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

    public int getDropChance(Player player) {
        int drop_chance;
        boolean enabled = Main.getPlugin().getConfig().get("config.modules.spawner-drop-chance.default") != null;
        if (enabled) {
            drop_chance = Main.getPlugin().getConfig().getInt("config.modules.spawner-drop-chance.default");
            for (String permission : Main.getPlugin().getConfig().getConfigurationSection("config.modules.spawner-drop-chance.permissions").getKeys(false)) {
                if (Main.getPermissions().has(player, permission)) {
                    drop_chance = Main.getPlugin().getConfig().getInt("config.modules.spawner-drop-chance.permissions." + permission);
                }
            }
        } else {
            drop_chance = 100;
        }
        return drop_chance;
    }

    public boolean playerSilkTouch(Player player) {
        // Returns true if the player can break it, false otherwise.
        boolean can_break;
        FileConfiguration config = Main.getPlugin().getConfig();
        if (config.getBoolean("config.modules.silk-touch.enabled")) {
            if (player.getInventory().getItemInMainHand().hasItemMeta() && player.getInventory().getItemInMainHand().getItemMeta().hasEnchant(Enchantment.SILK_TOUCH)) {
                can_break = true;
            } else {
                can_break = Main.getPermissions().has(player, config.getString("config.modules.silk-touch.exempt"));
            }
        } else {
            can_break = true;
        }
        return can_break;
    }

    public boolean playerHasBreakAllPermission(Player player) {
        // Returns true if a player has bypass permission to break all spawners regardless of ownership, false otherwise.
        return Main.getPermissions().has(player, Main.getPlugin().getConfig().getString("config.modules.break-spawners.permission"));
    }


    @EventHandler
    public void onSpawnerBreakEvent(BlockBreakEvent e) {
        if (!e.isCancelled()) {
            int spawner_drop_chance = getDropChance(e.getPlayer());
            if (e.getBlock().getType() == XMaterial.SPAWNER.parseMaterial()) {
                Block clicked_block = e.getBlock();
                Player player = e.getPlayer();
                boolean holograms_enabled = Main.getPlugin().getConfig().getBoolean("config.modules.hologram.enabled");
                Main.database.spawnerHasOwner(e.getBlock()).thenAccept(boolean1 -> {
                   if (boolean1) { // If spawner has an owner.
                       try {
                           Main.database.getSpawnerOwnerUuid(clicked_block).thenAccept(owneruuid -> {
                             if (owneruuid.equalsIgnoreCase(player.getUniqueId().toString()) || playerHasBreakAllPermission(player)) {
                                 e.setCancelled(true);
                                 if (!playerSilkTouch(player)) {
                                     if (Main.getPlugin().getConfig().getBoolean("config.modules.silk-touch.block")) {
                                         Main.getPlugin().sendMessage(player, "silkTouchRequired", new String[]{});
                                     } else {
                                         e.getBlock().setType(Material.AIR);
                                         Main.database.breakSpawner(player, clicked_block);
                                         Main.getPlugin().sendMessage(player, "noSilkTouchSpawnerLost", new String[]{});
                                     }
                                     return;
                                 }

                                 Main.database.getSpawnerLevel(clicked_block).thenAccept(spawner_level -> {
                                     if (spawner_level == 1) {
                                         Main.database.getSpawnerType(clicked_block).thenAccept(spawner_type -> {
                                             ItemStack spawner = Main.getPlugin().getSpawner(spawner_type);
                                             String hologram_name = Main.database.getHologramName(e.getBlock());
                                             if (holograms_enabled) {
                                                 if (DHAPI.getHologram(hologram_name) != null) {
                                                     DHAPI.removeHologram(hologram_name);
                                                 }
                                             }

                                             clicked_block.setType(Material.AIR);
                                             Bukkit.getScheduler().runTaskLater(Main.getPlugin(), () -> {
                                                 Main.database.breakSpawner(player, clicked_block);
                                             }, 1);
                                             clicked_block.getDrops().clear();
                                             boolean autopickup = Main.getPlugin().getConfig().getBoolean("config.modules.auto-pickup.enabled");
                                             boolean hasautopickupperm = Main.getPermissions().has(player, Main.getPlugin().getConfig().getString("config.modules.auto-pickup.permission"));
                                             if (autopickup && hasautopickupperm) {
                                                 if (player.getInventory().firstEmpty() != -1) {
                                                     Random random = new Random();
                                                     if (spawner_drop_chance >= random.nextInt(101)) {
                                                         player.getInventory().addItem(spawner);
                                                     }
                                                 } else {
                                                     Random random = new Random();
                                                     if (spawner_drop_chance >= random.nextInt(101)) {
                                                         clicked_block.getLocation().getWorld().dropItemNaturally(e.getBlock().getLocation(), spawner);
                                                     }
                                                 }
                                             } else {
                                                 Random random = new Random();
                                                 if (spawner_drop_chance >= random.nextInt(101)) {
                                                     clicked_block.getLocation().getWorld().dropItemNaturally(e.getBlock().getLocation(), spawner);
                                                 }
                                             }

                                             Main.getPlugin().sendMessage(player, "brokeSpawner", new String[]{Main.getPlugin().getSpawner(spawner_type).getItemMeta().getDisplayName()});

                                         });
                                     } else if (spawner_level > 1) {
                                         if (player.isSneaking()) {

                                             final int[] totalLevelToDrop = {spawner_level - 1};
                                             Main.database.getSpawnerType(clicked_block).thenAccept(spawner_type -> {

                                                 ItemStack spawner = Main.getPlugin().getSpawner(spawner_type);
                                                 assert spawner != null;
                                                 spawner.setAmount(1);

                                                 Main.database.setSpawnerLevelTo1(player, clicked_block);
                                                 clicked_block.getDrops().clear();

                                                 while (totalLevelToDrop[0] > 0) {
                                                     ItemStack spawnerToDrop = spawner.clone();
                                                     if (totalLevelToDrop[0] > 64) {
                                                         spawnerToDrop.setAmount(64);
                                                         totalLevelToDrop[0] -= 64;
                                                     } else {
                                                         spawnerToDrop.setAmount(totalLevelToDrop[0]);
                                                         totalLevelToDrop[0] -= totalLevelToDrop[0];
                                                     }
                                                     boolean autopickup = Main.getPlugin().getConfig().getBoolean("config.modules.auto-pickup.enabled");
                                                     boolean hasautopickupperm = Main.getPermissions().has(e.getPlayer(), Main.getPlugin().getConfig().getString("config.modules.auto-pickup.permission"));
                                                     if (autopickup && hasautopickupperm) {
                                                         if (player.getInventory().firstEmpty() != -1) {
                                                             Random random = new Random();
                                                             if (spawner_drop_chance >= random.nextInt(101)) {
                                                                 player.getInventory().addItem(spawnerToDrop);
                                                             }
                                                         } else {
                                                             Random random = new Random();
                                                             if (spawner_drop_chance >= random.nextInt(101)) {
                                                                 clicked_block.getLocation().getWorld().dropItemNaturally(e.getBlock().getLocation(), spawnerToDrop);
                                                             }
                                                         }
                                                     } else {
                                                         Random random = new Random();
                                                         if (spawner_drop_chance >= random.nextInt(101)) {
                                                             clicked_block.getLocation().getWorld().dropItemNaturally(e.getBlock().getLocation(), spawnerToDrop);
                                                         }
                                                     }
                                                 }

                                                 Main.getPlugin().sendMessage(player, "brokeAllLevels", new String[]{Main.getPlugin().getSpawner(spawner_type).getItemMeta().getDisplayName()});
                                                 SpawnerHologram_General.updateSpawnerHologram(clicked_block, Main.database.getHologramName(clicked_block));
                                             });
                                         } else {
                                             Main.database.getSpawnerType(e.getBlock()).thenAccept(spawner_type -> {
                                                 ItemStack spawner = Main.getPlugin().getSpawner(spawner_type);
                                                 Main.database.reduceLevelBy1(e.getPlayer(), e.getBlock());
                                                 clicked_block.getDrops().clear();

                                                 boolean autopickup = Main.getPlugin().getConfig().getBoolean("config.modules.auto-pickup.enabled");
                                                 boolean hasautopickupperm = Main.getPermissions().has(e.getPlayer(), Main.getPlugin().getConfig().getString("config.modules.auto-pickup.permission"));
                                                 if (autopickup && hasautopickupperm) {
                                                     if (player.getInventory().firstEmpty() != -1) {
                                                         Random random = new Random();
                                                         if (spawner_drop_chance >= random.nextInt(101)) {
                                                             player.getInventory().addItem(spawner);
                                                         }
                                                     } else {
                                                         Random random = new Random();
                                                         if (spawner_drop_chance >= random.nextInt(101)) {
                                                             clicked_block.getLocation().getWorld().dropItemNaturally(clicked_block.getLocation(), spawner);
                                                         }
                                                     }
                                                 } else {
                                                     Random random = new Random();
                                                     if (spawner_drop_chance >= random.nextInt(101)) {
                                                         clicked_block.getLocation().getWorld().dropItemNaturally(clicked_block.getLocation(), spawner);
                                                     }
                                                 }

                                                 Main.getPlugin().sendMessage(player, "brokeWithLevels", new String[]{Main.getPlugin().getSpawner(spawner_type).getItemMeta().getDisplayName()});
                                                 SpawnerHologram_General.updateSpawnerHologram(clicked_block, Main.database.getHologramName(clicked_block));
                                             });
                                         }
                                     }
                                 });
                             } else {
                                 boolean hasBypassPermission = Main.getPermissions().has(e.getPlayer(), "ancspawners.bypass");
                                 e.setCancelled(true);
                                 if (hasBypassPermission) {
                                     Main.database.getSpawnerLevel(e.getBlock()).thenAccept(spawnerLevel -> {
                                         Main.database.getSpawnerType(e.getBlock()).thenAccept(spawnerTypeInData -> {
                                             String spawnerDisplayName = Main.getPlugin().getSpawnerByEntity(spawnerTypeInData).getItemMeta().getDisplayName();
                                             try {
                                                 Main.database.getSpawnerOwnerUuid(e.getBlock()).thenAccept(owner -> {

                                                     String hologramName = Main.database.getHologramName(e.getBlock());

                                                     if (holograms_enabled) {
                                                         if (DHAPI.getHologram(hologramName) != null) {
                                                             DHAPI.removeHologram(hologramName);
                                                         }
                                                     }

                                                     e.getBlock().setType(Material.AIR);

                                                     Bukkit.getScheduler().runTaskLater(Main.getPlugin(), () -> {
                                                         Main.database.breakSpawner(e.getPlayer(), e.getBlock());
                                                     }, 1);

                                                     Main.getPlugin().sendMessage(e.getPlayer(), "bypassBrokeSpawner", new String[]{String.valueOf(spawnerLevel), spawnerDisplayName, owner});
                                                 });
                                             } catch (ExecutionException | InterruptedException ex) {
                                                 throw new RuntimeException(ex);
                                             }
                                         });
                                     });
                                 } else {
                                     // NOT THE OWNER IS BELOW.
                                     Main.database.getSpawnerType(e.getBlock()).thenAccept(spawner_type -> {
                                         Main.getPlugin().sendMessage(e.getPlayer(), "notTheOwner", new String[]{Main.getPlugin().getSpawner(spawner_type).getItemMeta().getDisplayName()});
                                     });
                                 }
                             }
                           });
                       } catch (ExecutionException | InterruptedException ex) {
                           throw new RuntimeException(ex);
                       }
                   } else {
                       if (Main.getPlugin().getConfig().getString("config.modules.vanilla-spawner-support.enabled").equalsIgnoreCase("true")) {
                           e.setCancelled(true);
                           // This means the spawner was not placed after ancSpawners was installed, most likely a vanilla spawner.

                           CreatureSpawner spawner = (CreatureSpawner) e.getBlock().getState();
                           String entity;
                           if (MinecraftVersion.isAtLeastVersion(MinecraftVersion.MC1_12_R1)) {
                               entity = spawner.getSpawnedType().toString();
                           } else {
                               final String[] entityFromNbt = new String[1];
                               NBT.get(spawner, nbt -> {
                                   entityFromNbt[0] = nbt.getString("EntityId");
                               });
                               entity = entityFromNbt[0];
                           }

                           // Search the config to find the equivalent spawner.

                           if (entity != null) {
                               entity = entity.toUpperCase();
                               ItemStack itemStack = Main.getPlugin().getSpawnerByEntity(entity);
                               if (itemStack != null) {
                                   e.getBlock().setType(Material.AIR);
                                   Random random = new Random();
                                   if (spawner_drop_chance >= random.nextInt(101)) {
                                       e.getBlock().getWorld().dropItemNaturally(e.getBlock().getLocation(), itemStack);
                                   }
                                   // SEND MESSAGE - Vanilla spawner has been broken.
                                   Main.getPlugin().sendMessage(e.getPlayer(), "vanillaSpawnerBroken", new String[]{});

                               } else {
                                   // SEND MESSAGE - This vanilla spawner has not been configured yet.
                                   Main.getPlugin().sendMessage(e.getPlayer(), "vanillaSpawnerNotConfigured", new String[]{});

                               }
                           } else {
                               // SEND MESSAGE - This spawner seems to be broken.
                               Main.getPlugin().sendMessage(e.getPlayer(), "vanillaSpawnerEntityNotFound", new String[]{});

                           }
                       }
                   }
                });
            }
        }
    }
}
