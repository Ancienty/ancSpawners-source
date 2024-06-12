package com.ancienty.ancspawners.Listeners;

import com.ancienty.ancspawners.Main;
import com.cryptomorin.xseries.XMaterial;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.SlimeSplitEvent;
import org.bukkit.event.entity.SpawnerSpawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class SpawnerSpawnListener implements Listener {

    private final HashMap<UUID, Block> entityLinkToSpawners = new HashMap<>();
    public static final HashMap<Block, Boolean> spawnerAutoKillCheck = new HashMap<>();

    public CompletableFuture<HashMap<Material, Integer>> getMobDrops(Block spawner, String entityName) {
        return Main.database.getSpawnerLevel(spawner).thenApply(spawnerLevel -> {
            HashMap<Material, Integer> drops = new HashMap<>();
            List<ItemStack> lootTable = Main.getPlugin().getLootTable(entityName);
            if (lootTable != null) {
                for (ItemStack item : lootTable) {
                    drops.put(item.getType(), item.getAmount() * spawnerLevel);
                }
            }
            return drops;
        });
    }

    public void dropItems(Block block, World world, Location location, String entityName) {
        getMobDrops(block, entityName).thenAccept(givenDrops -> {
            givenDrops.forEach((material, amount) -> {
                ItemStack itemStack = new ItemStack(material);
                while (amount >= 64) {
                    itemStack.setAmount(64);
                    amount -= 64;
                    world.dropItemNaturally(location, itemStack);
                }
                if (amount > 0) {
                    itemStack.setAmount(amount);
                    world.dropItemNaturally(location, itemStack);
                }
            });
        }).exceptionally(ex -> {
            Main.getPlugin().getLogger().log(Level.SEVERE, "Error dropping items", ex);
            return null;
        });
    }

    public void dropXP(Block block, World world, Location location, String entityName) {
        int dropXP = Main.getPlugin().getLootTableXP(entityName.toLowerCase(Locale.ENGLISH));
        Main.database.getSpawnerLevel(block).thenAccept(spawnerLevel -> {
            world.spawn(location, ExperienceOrb.class, x -> x.setExperience(dropXP * spawnerLevel));
        }).exceptionally(ex -> {
            Main.getPlugin().getLogger().log(Level.SEVERE, "Error dropping XP", ex);
            return null;
        });
    }

    public void addDropsToStorage(Block block, String entityName) {
        getMobDrops(block, entityName).thenAccept(mobDrops -> {
            CompletableFuture<Integer> storageLimitFuture = Main.database.getStorageLimit(block);
            CompletableFuture<Integer> spawnerLevelFuture = Main.database.getSpawnerLevel(block);
            CompletableFuture.allOf(storageLimitFuture, spawnerLevelFuture).thenRun(() -> {
                int storageLimit = storageLimitFuture.join();
                int spawnerLevel = spawnerLevelFuture.join();
                mobDrops.forEach((material, amount) -> {
                    Main.database.getSpawnerStoredByItem(block, material).thenAccept(previousStored -> {
                        int newAmount = previousStored + amount;
                        if (newAmount >= storageLimit) {
                            newAmount = storageLimit;
                        }
                        Main.database.updateStorage(block, material, newAmount);
                    });
                });
            });
        }).exceptionally(ex -> {
            Main.getPlugin().getLogger().log(Level.SEVERE, "Error adding drops to storage", ex);
            return null;
        });
    }

    public void addXPToStorage(Block block, int addAmount) {
        CompletableFuture<Integer> storedXPFuture = Main.database.getStoredXP(block);
        CompletableFuture<Integer> spawnerLevelFuture = Main.database.getSpawnerLevel(block);
        CompletableFuture.allOf(storedXPFuture, spawnerLevelFuture).thenRun(() -> {
            int previousXP = storedXPFuture.join();
            int spawnerLevel = spawnerLevelFuture.join();
            int addThis = previousXP + (addAmount * spawnerLevel);
            int xpLimit = Main.getPlugin().getConfig().getInt("config.modules.storage-limit.xpLimit");
            if (addThis > xpLimit) {
                addThis = xpLimit;
            }
            Main.database.updateXP(block, addThis);
        }).exceptionally(ex -> {
            Main.getPlugin().getLogger().log(Level.SEVERE, "Error adding XP to storage", ex);
            return null;
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onSpawnerSpawnEvent(SpawnerSpawnEvent e) {
        Block block = e.getSpawner().getBlock();
        Main.database.spawnerHasOwner(block).thenAccept(spawnerHasOwner -> {
            if (spawnerHasOwner) {
                CompletableFuture<Boolean> isAutoKillEnabledFuture = Main.database.isAutoKillEnabled(block);
                CompletableFuture<String> spawnerModeFuture = Main.database.getSpawnerMode(block);
                CompletableFuture<Integer> spawnerLevelFuture = Main.database.getSpawnerLevel(block);
                CompletableFuture<Integer> storageLimitFuture = Main.database.getStorageLimit(block);
                CompletableFuture<String> spawnerTypeFuture = Main.database.getSpawnerType(block);

                CompletableFuture.allOf(isAutoKillEnabledFuture, spawnerModeFuture, spawnerLevelFuture, storageLimitFuture, spawnerTypeFuture)
                        .thenRun(() -> {
                            boolean isAutoKillEnabled = isAutoKillEnabledFuture.join();
                            String spawnerMode = spawnerModeFuture.join();
                            int spawnerLevel = spawnerLevelFuture.join();
                            int storageLimit = storageLimitFuture.join();
                            String spawnerType = spawnerTypeFuture.join();

                            if (!spawnerAutoKillCheck.containsKey(block)) {
                                spawnerAutoKillCheck.put(block, isAutoKillEnabled);
                            }

                            boolean isAutoKillEnabledInConfig = Main.getPlugin().getConfig().getString("config.modules.auto-kill.enabled").equalsIgnoreCase("true");

                            if (Main.getPlugin().storageEnabled) {
                                handleStorageEnabled(e, block, spawnerMode, spawnerLevel, storageLimit, spawnerType, isAutoKillEnabledInConfig);
                            } else {
                                handleStorageDisabled(e, block, spawnerMode, spawnerLevel, spawnerType, isAutoKillEnabledInConfig);
                            }
                        }).exceptionally(ex -> {
                            Main.getPlugin().getLogger().log(Level.SEVERE, "Error processing spawner spawn event", ex);
                            return null;
                        });
            }
        });
    }

    private void handleStorageEnabled(SpawnerSpawnEvent e, Block block, String spawnerMode, int spawnerLevel, int storageLimit, String spawnerType, boolean isAutoKillEnabledInConfig) {
        if (spawnerMode.equalsIgnoreCase("ENTITY")) {
            handleEntityMode(e, block, spawnerLevel, storageLimit, isAutoKillEnabledInConfig);
        } else if (spawnerMode.equalsIgnoreCase("ITEM")) {
            handleItemMode(e, block, spawnerLevel, storageLimit, spawnerType);
        }
    }

    private void handleEntityMode(SpawnerSpawnEvent e, Block block, int spawnerLevel, int storageLimit, boolean isAutoKillEnabledInConfig) {
        if (isAutoKillEnabledInConfig && spawnerAutoKillCheck.get(block)) {
            e.getEntity().remove();
            String entityType = e.getEntity().getType().toString();
            if (Main.getPlugin().getLootTable(entityType) != null) {
                addDropsToStorage(block, entityType);
                addXPToStorage(block, Main.getPlugin().getLootTableXP(entityType));
            }
        } else {
            entityLinkToSpawners.put(e.getEntity().getUniqueId(), block);
        }
    }

    private void handleItemMode(SpawnerSpawnEvent e, Block block, int spawnerLevel, int storageLimit, String spawnerType) {
        e.getEntity().remove();
        String spawnedItem = Main.getPlugin().getConfig().getString("spawners." + spawnerType + ".spawnerInfo.material");
        ItemStack itemStack = new ItemStack(XMaterial.valueOf(spawnedItem.toUpperCase()).parseMaterial());
        Main.database.getSpawnerStoredByItem(block, itemStack.getType()).thenAccept(previous -> {
            int newAmount = previous + spawnerLevel;
            if (newAmount > storageLimit) {
                newAmount = storageLimit;
            }
            Main.database.updateStorage(block, itemStack.getType(), newAmount);
        }).exceptionally(ex -> {
            Main.getPlugin().getLogger().log(Level.SEVERE, "Error updating item storage", ex);
            return null;
        });
    }

    private void handleStorageDisabled(SpawnerSpawnEvent e, Block block, String spawnerMode, int spawnerLevel, String spawnerType, boolean isAutoKillEnabledInConfig) {
        if (spawnerMode.equalsIgnoreCase("ITEM")) {
            handleItemModeWithoutStorage(e, block, spawnerLevel, spawnerType);
        } else if (spawnerMode.equalsIgnoreCase("ENTITY")) {
            handleEntityModeWithoutStorage(e, block, spawnerLevel, isAutoKillEnabledInConfig);
        }
    }

    private void handleItemModeWithoutStorage(SpawnerSpawnEvent e, Block block, int spawnerLevel, String spawnerType) {
        e.getEntity().remove();
        String spawnedItem = Main.getPlugin().getConfig().getString("spawners." + spawnerType + ".spawnerInfo.material");
        ItemStack itemStack = new ItemStack(XMaterial.valueOf(spawnedItem.toUpperCase()).parseMaterial());

        ItemMeta meta = itemStack.getItemMeta();
        String itemName = Main.getPlugin().getConfig().getString("spawners." + itemStack.getType().toString().toLowerCase() + ".spawnerInfo.details.name");
        List<String> itemLore = Main.getPlugin().getConfig().getStringList("spawners." + itemStack.getType().toString().toLowerCase() + ".spawnerInfo.details.lore");

        if (itemName != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', itemName));
        }
        if (itemLore != null) {
            List<String> realLore = new ArrayList<>();
            for (String text : itemLore) {
                realLore.add(ChatColor.translateAlternateColorCodes('&', text));
            }
            meta.setLore(realLore);
        }

        itemStack.setItemMeta(meta);
        dropItemsNaturally(e, itemStack, spawnerLevel);
    }

    private void handleEntityModeWithoutStorage(SpawnerSpawnEvent e, Block block, int spawnerLevel, boolean isAutoKillEnabledInConfig) {
        if (isAutoKillEnabledInConfig && spawnerAutoKillCheck.get(block)) {
            Location location = e.getEntity().getLocation();
            e.getEntity().remove();
            dropItems(block, block.getWorld(), location, e.getEntity().getType().toString());
            if (!isXPStorageEnabled()) {
                dropXP(block, block.getWorld(), location, e.getEntity().getType().toString());
            } else {
                addXPToStorage(block, Main.plugin.getLootTableXP(e.getEntity().getType().toString()));
            }
        } else {
            entityLinkToSpawners.put(e.getEntity().getUniqueId(), block);
        }
    }

    private void dropItemsNaturally(SpawnerSpawnEvent e, ItemStack itemStack, int spawnerLevel) {
        Location dropLocation = e.getSpawner().getLocation().subtract(0, 1, 0);
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
    }

    public boolean isXPStorageEnabled() {
        return Main.getPlugin().lang.getBoolean("menu.exp.gui");
    }

    public boolean loottableOnlyAutokill() {
        return Main.getPlugin().getConfig().getBoolean("config.modules.auto-kill.loottable-only-autokill");
    }

    @EventHandler
    public void onSlimeSplit(SlimeSplitEvent e) {
        Entity entity = e.getEntity();
        UUID parentUUID = entity.getUniqueId();

        if (entityLinkToSpawners.containsKey(parentUUID)) {
            Block block = entityLinkToSpawners.get(parentUUID);
            for (Entity child : entity.getWorld().getNearbyEntities(entity.getLocation(), 1, 1, 1)) {
                if (child instanceof Slime && !entityLinkToSpawners.containsKey(child.getUniqueId())) {
                    entityLinkToSpawners.put(child.getUniqueId(), block);
                }
            }
        }
    }


    @EventHandler
    public void onSpawnerEntityDeathEvent(EntityDeathEvent e) {
        UUID entityUUID = e.getEntity().getUniqueId();
        if (entityLinkToSpawners.containsKey(entityUUID)) {
            Block block = entityLinkToSpawners.get(entityUUID);
            Entity entity = e.getEntity();
            Location entityLocation = entity.getLocation();
            World world = entity.getWorld();
            List<ItemStack> entityDrops = new ArrayList<>(e.getDrops());
            List<ItemStack> customLootTable = Main.getPlugin().getLootTable(entity.getName().toLowerCase());
            int[] droppedXP = {e.getDroppedExp()};
            e.setDroppedExp(0);
            e.getDrops().clear();

            CompletableFuture<Integer> spawnerLevelFuture = Main.database.getSpawnerLevel(block);
            CompletableFuture<Integer> storageLimitFuture = Main.database.getStorageLimit(block);
            CompletableFuture<Integer> storedXPFuture = Main.database.getStoredXP(block);

            CompletableFuture.allOf(spawnerLevelFuture, storageLimitFuture, storedXPFuture).thenRun(() -> {
                int spawnerLevel = spawnerLevelFuture.join();
                int storageLimit = storageLimitFuture.join();
                int spawnerXP = storedXPFuture.join();

                handleEntityDeath(e, block, entity, entityLocation, world, entityDrops, customLootTable, droppedXP, spawnerLevel, storageLimit, spawnerXP);
                entityLinkToSpawners.remove(entityUUID);
            }).exceptionally(ex -> {
                Main.getPlugin().getLogger().log(Level.SEVERE, "Error processing entity death event", ex);
                return null;
            });
        }
    }

    private void handleEntityDeath(EntityDeathEvent e, Block block, Entity entity, Location entityLocation, World world, List<ItemStack> entityDrops, List<ItemStack> customLootTable, int[] droppedXP, int spawnerLevel, int storageLimit, int spawnerXP) {
        if (Main.getPlugin().storageEnabled) {
            handleEntityDeathWithStorage(block, entity, entityLocation, world, entityDrops, customLootTable, droppedXP, spawnerLevel, storageLimit, spawnerXP);
        } else {
            handleEntityDeathWithoutStorage(e, block, entity, entityLocation, world, entityDrops, customLootTable, droppedXP, spawnerLevel);
        }
    }

    private void handleEntityDeathWithStorage(Block block, Entity entity, Location entityLocation, World world, List<ItemStack> entityDrops, List<ItemStack> customLootTable, int[] droppedXP, int spawnerLevel, int storageLimit, int spawnerXP) {
        if (customLootTable == null || loottableOnlyAutokill()) {
            handleDefaultLootWithStorage(block, entityDrops, droppedXP, spawnerLevel, storageLimit, spawnerXP);
        } else {
            handleCustomLootWithStorage(block, customLootTable, entity.getName().toLowerCase(), spawnerLevel, storageLimit, spawnerXP);
        }
    }

    private void handleDefaultLootWithStorage(Block block, List<ItemStack> entityDrops, int[] droppedXP, int spawnerLevel, int storageLimit, int spawnerXP) {
        Random random = new Random();
        if (droppedXP[0] == 0 && random.nextInt(100) <= 80) {
            droppedXP[0] = random.nextInt(8);
        }
        droppedXP[0] *= spawnerLevel;

        entityDrops.forEach(item -> {
            Main.database.getSpawnerStoredByItem(block, item.getType()).thenAccept(previous -> {
                int newAmount = previous + (item.getAmount() * spawnerLevel);
                if (newAmount > storageLimit) {
                    newAmount = storageLimit;
                }
                Main.database.updateStorage(block, item.getType(), newAmount);
            });
        });

        int newXP = spawnerXP + droppedXP[0];
        if (newXP > Main.getPlugin().getConfig().getInt("config.modules.storage-limit.xpLimit")) {
            newXP = Main.getPlugin().getConfig().getInt("config.modules.storage-limit.xpLimit");
        }
        Main.database.updateXP(block, newXP);
    }

    private void handleCustomLootWithStorage(Block block, List<ItemStack> customLootTable, String entityName, int spawnerLevel, int storageLimit, int spawnerXP) {
        customLootTable.forEach(itemStack -> {
            Main.database.getSpawnerStoredByItem(block, itemStack.getType()).thenAccept(previous -> {
                int newAmount = previous + (itemStack.getAmount() * spawnerLevel);
                if (newAmount > storageLimit) {
                    newAmount = storageLimit;
                }
                Main.database.updateStorage(block, itemStack.getType(), newAmount);
            });
        });

        Integer currentXP = Main.getPlugin().getLootTableXP(entityName.toLowerCase());
        if (currentXP != null) {
            int newXP = spawnerXP + currentXP;
            int xpLimit = Main.getPlugin().getConfig().getInt("config.modules.storage-limit.xpLimit");
            if (newXP > xpLimit) {
                newXP = xpLimit;
            }
            Main.database.updateXP(block, newXP);
        }
    }


    private void handleEntityDeathWithoutStorage(EntityDeathEvent e, Block block, Entity entity, Location entityLocation, World world, List<ItemStack> entityDrops, List<ItemStack> customLootTable, int[] droppedXP, int spawnerLevel) {
        Random random = new Random();
        if (droppedXP[        0] == 0 && random.nextInt(100) <= 80) {
            droppedXP[0] = random.nextInt(8);
        }

        if (customLootTable == null || loottableOnlyAutokill()) {
            handleDefaultLootWithoutStorage(e, entity, entityLocation, world, entityDrops, droppedXP, spawnerLevel);
        } else {
            handleCustomLootWithoutStorage(entityLocation, world, customLootTable, droppedXP, spawnerLevel);
        }
    }

    private void handleDefaultLootWithoutStorage(EntityDeathEvent e, Entity entity, Location entityLocation, World world, List<ItemStack> entityDrops, int[] droppedXP, int spawnerLevel) {
        if (!isXPStorageEnabled()) {
            world.spawn(entityLocation, ExperienceOrb.class, x -> x.setExperience(droppedXP[0] * spawnerLevel));
        } else {
            Main.database.updateXP(entityLinkToSpawners.get(entity.getUniqueId()), droppedXP[0] * spawnerLevel);
        }

        entityDrops.forEach(itemStack -> {
            int amount = itemStack.getAmount() * spawnerLevel;
            ItemStack singleItemStack = itemStack.clone();
            singleItemStack.setAmount(1);
            while (amount > 0) {
                ItemStack itemToDrop = singleItemStack.clone();
                if (amount > 64) {
                    itemToDrop.setAmount(64);
                    world.dropItemNaturally(entityLocation, itemToDrop);
                    amount -= 64;
                } else {
                    itemToDrop.setAmount(amount);
                    world.dropItemNaturally(entityLocation, itemToDrop);
                    amount = 0;
                }
            }
        });
    }

    private void handleCustomLootWithoutStorage(Location entityLocation, World world, List<ItemStack> customLootTable, int[] droppedXP, int spawnerLevel) {
        Integer currentXP = Main.getPlugin().getLootTableXP(entityLocation.getWorld().getName().toLowerCase());
        if (currentXP == null || currentXP == 0) {
            Random random = new Random();
            if (random.nextInt(100) <= 80) {
                currentXP = random.nextInt(8);
            }
        }
        if (!isXPStorageEnabled()) {
            Integer finalCurrentXP = currentXP;
            world.spawn(entityLocation, ExperienceOrb.class, x -> x.setExperience(finalCurrentXP * spawnerLevel));
        } else {
            Main.database.updateXP(entityLinkToSpawners.get(entityLocation), currentXP * spawnerLevel);
        }

        customLootTable.forEach(itemStack -> {
            int amount = itemStack.getAmount() * spawnerLevel;
            while (amount > 0) {
                ItemStack itemToDrop = itemStack.clone();
                if (amount > 64) {
                    itemToDrop.setAmount(64);
                    world.dropItemNaturally(entityLocation, itemToDrop);
                    amount -= 64;
                } else {
                    itemToDrop.setAmount(amount);
                    world.dropItemNaturally(entityLocation, itemToDrop);
                    amount = 0;
                }
            }
        });
    }
}
