package com.ancienty.ancspawners.Listeners;

import com.ancienty.ancspawners.Main;
import com.cryptomorin.xseries.XMaterial;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class SpawnerSpawnListener implements Listener {

    HashMap<UUID, Block> entityLinkToSpawners = new HashMap<>();
    public static HashMap<Block, Boolean> spawner_auto_kill_check = new HashMap<>();

    public CompletableFuture<HashMap<Material, Integer>> getMobDrops(Block spawner, String entity_name) {
        CompletableFuture<HashMap<Material, Integer>> return_this = new CompletableFuture<>();
        Main.database.getSpawnerLevel(spawner).thenAccept(spawner_level -> {

            HashMap<Material, Integer> drops = new HashMap<>();
            if (Main.getPlugin().getLootTable(entity_name) != null) {
                List<ItemStack> loottable = Main.getPlugin().getLootTable(entity_name);
                assert loottable != null;
                for (ItemStack item : loottable) {
                    drops.put(item.getType(), item.getAmount() * spawner_level);
                }
            }
            return_this.complete(drops);

        });
        return return_this;
    }

    public void dropItems(Block block, World world, Location location, String entity_name) {
        getMobDrops(block, entity_name).thenAccept(given_drops -> {
            given_drops.forEach((material, integer) -> {
                ItemStack itemStack = new ItemStack(material);
                while (integer >= 64) {
                    itemStack.setAmount(64);
                    integer -= 64;
                    world.dropItemNaturally(location, itemStack);
                }

                if (integer > 0) {
                    itemStack.setAmount(integer);
                    world.dropItemNaturally(location, itemStack);
                }
            });
        });
    }

    public void dropXP(Block block, World world, Location location, String entity_name) {
        int drop_xp = Main.getPlugin().getLootTableXP(entity_name);
        Main.database.getSpawnerLevel(block).thenAccept(spawner_level -> {
            world.spawn(location, ExperienceOrb.class, x -> x.setExperience(drop_xp * spawner_level));
        });
    }

    public void addDropsToStorage(Block block, String entity_name) {
        getMobDrops(block, entity_name).thenAccept(mob_drops -> {
            Main.database.getStorageLimit(block).thenAccept(storage_limit -> {
                Main.database.getSpawnerLevel(block).thenAccept(spawner_level -> {
                    mob_drops.forEach((material, amount) -> {
                       Main.database.getSpawnerStoredByItem(block, material).thenAccept(previous_stored -> {
                          int new_amount = previous_stored + amount;
                          if (new_amount >= storage_limit) {
                              new_amount = storage_limit;
                          }
                          Main.database.updateStorage(block, material, new_amount);
                      });
                  });
              });
           });
        });
    }

    public void addXPToStorage(Block block, int add_amount) {
        Main.database.getStoredXP(block).thenAccept(previous_xp -> {
            Main.database.getSpawnerLevel(block).thenAccept(spawner_level -> {
                int add_this = previous_xp + (add_amount * spawner_level);
                if (add_this > Main.getPlugin().getConfig().getInt("config.modules.storage-limit.xpLimit")) {
                    add_this = Main.getPlugin().getConfig().getInt("config.modules.storage-limit.xpLimit");
                }
                Main.database.updateXP(block, add_this);
            });
        });
    }


    @EventHandler(
            priority = EventPriority.HIGHEST
    )
    public void onSpawnerSpawnEvent(SpawnerSpawnEvent e) {
        Block block = e.getSpawner().getBlock();
        Main.database.spawnerHasOwner(block).thenAccept(spawner_has_owner -> Main.database.isAutoKillEnabled(block).thenAccept(is_autokill_enabled -> Main.database.getSpawnerMode(block).thenAccept(spawner_mode -> Main.database.getSpawnerLevel(block).thenAccept(spawner_level -> Main.database.getStorageLimit(block).thenAccept(storage_limit -> Main.database.getSpawnerType(block).thenAccept(spawner_type -> {
            if (spawner_has_owner) {

                // Check auto kill feature.
                if (!spawner_auto_kill_check.containsKey(e.getSpawner().getBlock())) {
                    if (is_autokill_enabled) {
                        spawner_auto_kill_check.put(e.getSpawner().getBlock(), true);
                    } else {
                        spawner_auto_kill_check.put(e.getSpawner().getBlock(), false);
                    }
                }

                // Check if the virtual storage module is enabled.
                if (Main.getPlugin().storageEnabled) {

                    // If storage module is enabled, and spawner mode is entity.
                    if (spawner_mode.equalsIgnoreCase("ENTITY")) {
                        // If auto-kill is enabled in config.
                        if (Main.getPlugin().getConfig().getString("config.modules.auto-kill.enabled").equalsIgnoreCase("true")) {
                            // If auto-kill is enabled for this specific spawner.
                            if (spawner_auto_kill_check.get(e.getSpawner().getBlock())) {
                                e.getEntity().remove();
                                if (Main.getPlugin().getLootTable(e.getEntity().getType().toString()) != null) {

                                    addDropsToStorage(block, e.getEntity().getType().toString());
                                    addXPToStorage(block, Main.getPlugin().getLootTableXP(e.getEntity().getType().toString()));

                                }
                            } else {
                                entityLinkToSpawners.put(e.getEntity().getUniqueId(), e.getSpawner().getBlock());
                            }
                        } else {
                            entityLinkToSpawners.put(e.getEntity().getUniqueId(), e.getSpawner().getBlock());
                        }

                        // If storage is enabled, and spawner mode is item.
                    } else if (spawner_mode.equalsIgnoreCase("ITEM")) {
                        e.getEntity().remove();
                        int spawnerLevel = spawner_level;
                        String spawned_item = Main.getPlugin().getConfig().getString("spawners." + spawner_type + ".spawnerInfo.material");
                        ItemStack itemStack = new ItemStack(XMaterial.valueOf(spawned_item.toUpperCase()).parseMaterial());
                        int storage = storage_limit;
                        Main.database.getSpawnerStoredByItem(block, itemStack.getType()).thenAccept(previous -> {
                            int newAmount = previous + spawnerLevel;

                            if (newAmount > storage) {
                                newAmount = storage;
                            }

                            Main.database.updateStorage(e.getSpawner().getBlock(), itemStack.getType(), newAmount);
                        });
                    }

                    // If the storage module is disabled.
                } else {

                    // If the storage module is disabled, and the spawner mode is item.
                    if (spawner_mode.equalsIgnoreCase("ITEM")) {
                        e.getEntity().remove();
                        String spawned_item = Main.getPlugin().getConfig().getString("spawners." + spawner_type + ".spawnerInfo.material");
                        ItemStack itemStack = new ItemStack(XMaterial.valueOf(spawned_item.toUpperCase()).parseMaterial());

                        ItemMeta meta = itemStack.getItemMeta();
                        String itemName = Main.getPlugin().getConfig().get("spawners." + itemStack.getType().toString().toLowerCase() + ".spawnerInfo.details.name") == null ? null : Main.getPlugin().getConfig().getString("spawners." + itemStack.getType().toString().toLowerCase() + ".spawnerInfo.details.name");
                        List<String> itemLore = Main.getPlugin().getConfig().getList("spawners." + itemStack.getType().toString().toLowerCase() + ".spawnerInfo.details.lore") == null ? null : Main.getPlugin().getConfig().getStringList("spawners." + itemStack.getType().toString().toLowerCase() + ".spawnerInfo.details.lore");
                        List<String> realLore = new ArrayList<>();

                        if (itemName != null) {
                            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', itemName));
                        }
                        if (itemLore != null) {
                            for (String text : itemLore) {
                                realLore.add(ChatColor.translateAlternateColorCodes('&', text));
                            }
                            meta.setLore(realLore);
                        }

                        itemStack.setItemMeta(meta);

                        itemStack.setAmount(1);
                        Location dropLocation = e.getSpawner().getLocation();
                        dropLocation = dropLocation.subtract(0, 1, 0);
                        int spawnerLevel = spawner_level;

                        while (spawnerLevel > 0) {
                            ItemStack itemToDrop = itemStack.clone();
                            if (spawnerLevel > 64) {
                                itemToDrop.setAmount(64);
                                e.getSpawner().getWorld().dropItemNaturally(dropLocation, itemToDrop);
                                spawnerLevel -= 64;
                            } else {
                                itemToDrop.setAmount(spawnerLevel);
                                e.getSpawner().getWorld().dropItemNaturally(dropLocation, itemToDrop);
                                spawnerLevel -= spawnerLevel;
                            }
                        }
                    } else {
                        if (Main.getPlugin().getConfig().getString("config.modules.auto-kill.enabled").equalsIgnoreCase("true")) {
                            if (spawner_auto_kill_check.get(e.getSpawner().getBlock())) {
                                Location location = e.getEntity().getLocation();
                                e.getEntity().remove();
                                dropItems(block, block.getWorld(), location, e.getEntity().getType().toString());
                                if (!Main.getPlugin().lang.getBoolean("menu.exp.gui")) {
                                    dropXP(block, block.getWorld(), location, e.getEntity().getType().toString());
                                } else {
                                    addXPToStorage(block, Main.plugin.getLootTableXP(e.getEntity().getType().toString()));
                                }
                            } else {
                                entityLinkToSpawners.put(e.getEntity().getUniqueId(), e.getSpawner().getBlock());
                            }
                        } else {
                            entityLinkToSpawners.put(e.getEntity().getUniqueId(), e.getSpawner().getBlock());
                        }
                    }
                }
            }
        }))))));
    }

    public boolean isXPStorageEnabled() {
        return Main.getPlugin().lang.getBoolean("menu.exp.gui");
    }

    public boolean loottableOnlyAutokill() {
        return Main.getPlugin().getConfig().getBoolean("config.modules.auto-kill.loottable-only-autokill");
    }


    @EventHandler
    public void onSpawnerEntityDeathEvent(EntityDeathEvent e) {
        if (entityLinkToSpawners.containsKey(e.getEntity().getUniqueId())) {
            Block block = entityLinkToSpawners.get(e.getEntity().getUniqueId());
            Entity entity = e.getEntity();
            Location entity_location = e.getEntity().getLocation();
            World world = e.getEntity().getWorld();
            UUID entity_uuid = e.getEntity().getUniqueId();
            List<ItemStack> entity_drops = new ArrayList<>(e.getDrops());
            List<ItemStack> custom_loottable = Main.getPlugin().getLootTable(entity.getName().toLowerCase());
            final int[] dropped_xp = {e.getDroppedExp()};
            e.setDroppedExp(0);
            e.getDrops().clear();
            Main.database.getSpawnerLevel(block).thenAccept(spawner_level -> {
               Main.database.getStorageLimit(block).thenAccept(storage_limit -> {
                   Main.database.getStoredXP(block).thenAccept(spawner_xp -> {
                       // If virtual storage is enabled.
                      if (Main.getPlugin().storageEnabled) {
                          // If virtual storage is enabled and custom loottable for this mob does not exist.
                          if (custom_loottable == null || loottableOnlyAutokill()) {
                              int spawnerLevel = spawner_level;
                              int droppedXP = dropped_xp[0];
                              if (droppedXP == 0) {
                                  Random random = new Random();
                                  if (random.nextInt(100) <= 80) {
                                      droppedXP = random.nextInt(8);
                                  }
                              }

                              droppedXP *= spawnerLevel;
                              int maxStorage = storage_limit;
                              if (entity_drops.isEmpty()) {
                              }
                              for (ItemStack item : entity_drops) {
                                  Main.database.getSpawnerStoredByItem(block, item.getType()).thenAccept(previous -> {
                                      int newAmount = previous + (item.getAmount() * spawnerLevel);
                                      if (newAmount > maxStorage) {
                                          newAmount = maxStorage;
                                      }
                                      Main.database.updateStorage(block, item.getType(), newAmount);
                                  });
                              }

                              int previousXP = spawner_xp;
                              int newXP = previousXP + droppedXP;
                              if (newXP > Main.getPlugin().getConfig().getInt("config.modules.storage-limit.xpLimit")) {
                                  newXP = Main.getPlugin().getConfig().getInt("config.modules.storage-limit.xpLimit");
                              }
                              Main.database.updateXP(block, newXP);
                              entityLinkToSpawners.remove(entity_uuid);

                          // If virtual storage is enabled and custom loottable exists for this mob.
                          // OR!! if virtual storage is enabled and CUSTOM LOOTTABLE only exists on AUTO-KILL!
                          } else {
                              int spawnerLevel = spawner_level;
                              int maxStorage = storage_limit;
                              int previousXP = spawner_xp;
                              Integer currentXP = Main.getPlugin().getLootTableXP(entity.getName().toLowerCase());
                              custom_loottable.forEach(itemStack -> Main.database.getSpawnerStoredByItem(block, itemStack.getType()).thenAccept(previous -> {
                                  int newAmount = previous + (itemStack.getAmount() * spawnerLevel);
                                  if (newAmount > maxStorage) {
                                      newAmount = maxStorage;
                                  }
                                  Main.database.updateStorage(block, itemStack.getType(), newAmount);
                              }));
                              if (currentXP != null) {
                                  int newXP = previousXP + currentXP;
                                  if (newXP > Main.getPlugin().getConfig().getInt("config.modules.storage-limit.xpLimit")) {
                                      newXP = Main.getPlugin().getConfig().getInt("config.modules.storage-limit.xpLimit");
                                  }
                                  Main.database.updateXP(block, newXP);
                              }
                          }
                      // If virtual storage is disabled.
                      } else {
                          // If custom loottable does not exist for this mob.
                          if (custom_loottable == null || loottableOnlyAutokill()) {
                              int spawnerLevel = spawner_level;
                              if (dropped_xp[0] == 0) {
                                  Random random = new Random();
                                  if (random.nextInt(100) <= 80) {
                                      dropped_xp[0] = (random.nextInt(8));
                                  }
                              }
                              if (!isXPStorageEnabled()) {
                                  world.spawn(entity_location, ExperienceOrb.class, x -> x.setExperience(dropped_xp[0] * spawnerLevel));
                              } else {
                                  Main.database.updateXP(block, dropped_xp[0] * spawnerLevel);
                              }
                              entity_drops.forEach(itemStack -> {
                                  int amount = itemStack.getAmount() * spawnerLevel;
                                  ItemStack itemStack1 = itemStack.clone();
                                  itemStack1.setAmount(1);
                                  while (amount > 0) {
                                      ItemStack itemToDrop = itemStack1.clone();
                                      if (amount > 64) {
                                          itemToDrop.setAmount(64);
                                          world.dropItemNaturally(entity_location, itemToDrop);
                                          amount -= 64;
                                      } else {
                                          itemToDrop.setAmount(amount);
                                          world.dropItemNaturally(entity_location, itemToDrop);
                                          amount -= amount;
                                      }
                                  }
                              });
                          } else {
                              // If virtual storage is disabled but custom loottables exist.
                              // OR!!! virtual storage is disabled BUT CUSTOM LOOTTABLE only works for auto-kill.
                              int spawnerLevel = spawner_level;
                              Integer dropped_xp_loottable = Main.getPlugin().getLootTableXP(entity.getName().toLowerCase());
                              assert dropped_xp_loottable != null;
                              if (dropped_xp_loottable == 0) {
                                  Random random = new Random();
                                  if (random.nextInt(100) <= 80) {
                                      dropped_xp_loottable = (random.nextInt(8));
                                  }
                              }
                              if (!isXPStorageEnabled()) {
                                  Integer finalDropped_xp_loottable = dropped_xp_loottable;
                                  world.spawn(entity_location, ExperienceOrb.class, x -> x.setExperience(finalDropped_xp_loottable * spawnerLevel));
                              } else {
                                  Main.database.updateXP(block, dropped_xp_loottable * spawnerLevel);
                              }
                              custom_loottable.forEach(itemStack -> {
                                  int amount = (itemStack.getAmount() * spawnerLevel);
                                  while (amount > 0) {
                                      ItemStack itemToDrop = itemStack.clone();
                                      if (amount > 64) {
                                          itemToDrop.setAmount(64);
                                          world.dropItemNaturally(entity_location, itemToDrop);
                                          amount -= 64;
                                      } else {
                                          itemToDrop.setAmount(amount);
                                          world.dropItemNaturally(entity_location, itemToDrop);
                                          amount -= amount;
                                      }
                                  }
                              });
                          }
                      }
                   });
               });
            });
        }
    }

    /*@EventHandler
    public void onSpawnerEntityDeathEvent(EntityDeathEvent e) {
        if (entityLinkToSpawners.containsKey(e.getEntity().getUniqueId())) {
            Block block = entityLinkToSpawners.get(e.getEntity().getUniqueId());
            Main.database.getSpawnerLevel(block).thenAccept(spawner_level -> Main.database.getStorageLimit(block).thenAccept(storage_limit -> Main.database.getStoredXP(block).thenAccept(stored_xp -> {
                if (e.getEntity() instanceof Blaze) {
                    if (e.getDrops().isEmpty()) {
                        ItemStack itemStack = XMaterial.BLAZE_ROD.parseItem();
                        Random random = new Random();
                        if (random.nextInt(100) >= 80) {
                            int amount = random.nextInt(2);
                            amount += 1;
                            assert itemStack != null;
                            itemStack.setAmount(amount);
                            e.getDrops().add(itemStack);
                        }
                    }
                }
                List<ItemStack> customLoottable = Main.getPlugin().getLootTable(e.getEntity().getName().toLowerCase());
                if (Main.plugin.storageEnabled) {
                    if (customLoottable == null) {
                        int spawnerLevel = spawner_level;
                        int droppedXP = e.getDroppedExp();
                        if (droppedXP == 0) {
                            Random random = new Random();
                            if (random.nextInt(100) <= 80) {
                                droppedXP = random.nextInt(8);
                            }
                        }

                        droppedXP *= spawnerLevel;
                        int maxStorage = storage_limit;
                        e.getDrops().forEach(item -> Main.database.getSpawnerStoredByItem(block, item.getType()).thenAccept(previous -> {
                            int newAmount = previous + (item.getAmount() * spawnerLevel);
                            if (newAmount > maxStorage) {
                                newAmount = maxStorage;
                            }
                            Main.database.updateStorage(block, item.getType(), newAmount);
                        }));

                        int previousXP = stored_xp;
                        int newXP = previousXP + droppedXP;
                        if (newXP > Main.getPlugin().getConfig().getInt("config.modules.storage-limit.xpLimit")) {
                            newXP = Main.getPlugin().getConfig().getInt("config.modules.storage-limit.xpLimit");
                        }
                        Main.database.updateXP(block, newXP);
                        e.getDrops().clear();
                        e.setDroppedExp(0);
                        entityLinkToSpawners.remove(e.getEntity().getUniqueId());

                        // If custom loottable does exist & is enabled.
                    } else {
                        e.getDrops().clear();
                        e.setDroppedExp(0);
                        int spawnerLevel = spawner_level;
                        int maxStorage = storage_limit;
                        int previousXP = stored_xp;
                        Integer currentXP = Main.getPlugin().getLootTableXP(e.getEntity().getName().toLowerCase());
                        customLoottable.forEach(itemStack -> Main.database.getSpawnerStoredByItem(block, itemStack.getType()).thenAccept(previous -> {
                            int newAmount = previous + (itemStack.getAmount() * spawnerLevel);
                            if (newAmount > maxStorage) {
                                newAmount = maxStorage;
                            }
                            Main.database.updateStorage(block, itemStack.getType(), newAmount);
                        }));
                        if (currentXP != null) {
                            int newXP = previousXP + currentXP;
                            if (newXP > Main.getPlugin().getConfig().getInt("config.modules.storage-limit.xpLimit")) {
                                newXP = Main.getPlugin().getConfig().getInt("config.modules.storage-limit.xpLimit");
                            }
                            Main.database.updateXP(block, newXP);
                        }
                    }

                    // If the virtual storage is disabled.
                } else {
                    if (customLoottable == null) {
                        int spawnerLevel = spawner_level;
                        int dropped_xp = e.getDroppedExp();
                        if (dropped_xp == 0) {
                            Random random = new Random();
                            if (random.nextInt(100) <= 80) {
                                dropped_xp = (random.nextInt(8));
                            }
                        }
                        if (!isXPStorageEnabled()) {
                            e.setDroppedExp(dropped_xp * spawnerLevel);
                        } else {
                            Main.database.updateXP(block, dropped_xp * spawnerLevel);
                            e.setDroppedExp(0);
                        }
                        e.getDrops().forEach(itemStack -> {
                            int amount = itemStack.getAmount() * spawnerLevel;
                            ItemStack itemStack1 = itemStack.clone();
                            itemStack1.setAmount(1);
                            while (amount > 0) {
                                ItemStack itemToDrop = itemStack1.clone();
                                if (amount > 64) {
                                    itemToDrop.setAmount(64);
                                    e.getEntity().getWorld().dropItemNaturally(e.getEntity().getLocation(), itemToDrop);
                                    amount -= 64;
                                } else {
                                    itemToDrop.setAmount(amount);
                                    e.getEntity().getWorld().dropItemNaturally(e.getEntity().getLocation(), itemToDrop);
                                    amount -= amount;
                                }
                            }
                        });
                    } else {
                        // If virtual storage is disabled but custom loottables exist.
                        e.getDrops().clear();
                        int spawnerLevel = spawner_level;
                        Integer dropped_xp = Main.getPlugin().getLootTableXP(e.getEntity().getName().toLowerCase());
                        assert dropped_xp != null;
                        if (dropped_xp == 0) {
                            Random random = new Random();
                            if (random.nextInt(100) <= 80) {
                                dropped_xp = (random.nextInt(8));
                            }
                        }
                        if (!isXPStorageEnabled()) {
                            e.setDroppedExp(dropped_xp * spawnerLevel);
                        } else {
                            Main.database.updateXP(block, dropped_xp * spawnerLevel);
                        }
                        e.setDroppedExp(0);
                        customLoottable.forEach(itemStack -> {
                            int amount = (itemStack.getAmount() * spawnerLevel);
                            while (amount > 0) {
                                ItemStack itemToDrop = itemStack.clone();
                                if (amount > 64) {
                                    itemToDrop.setAmount(64);
                                    e.getEntity().getWorld().dropItemNaturally(e.getEntity().getLocation(), itemToDrop);
                                    amount -= 64;
                                } else {
                                    itemToDrop.setAmount(amount);
                                    e.getEntity().getWorld().dropItemNaturally(e.getEntity().getLocation(), itemToDrop);
                                    amount -= amount;
                                }
                            }
                        });
                    }
                }
            })));
        }
    }*/
}
