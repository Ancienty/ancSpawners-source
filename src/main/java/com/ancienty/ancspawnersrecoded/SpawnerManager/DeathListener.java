package com.ancienty.ancspawnersrecoded.SpawnerManager;

import com.ancienty.ancspawnersrecoded.Main;
import com.cryptomorin.xseries.XMaterial;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Statistic;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.SlimeSplitEvent;
import org.bukkit.event.player.PlayerStatisticIncrementEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable; // ADDED
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class DeathListener implements Listener {

    private static final Map<String, List<ItemStack>> lootTableCache = new HashMap<>();
    private static final Map<String, Integer> lootTableXPCache = new HashMap<>();
    private static Integer xpLimitCache = null;

    @EventHandler
    public void onSpawnerAutoKillEvent(EntityDeathEvent e) {
        SpawnerManager manager = Main.getPlugin().getSpawnerManager();
        if (manager.getEntityLink().containsKey(e.getEntity().getUniqueId())) {
            ancSpawner spawner = manager.getEntityLink().get(e.getEntity().getUniqueId());
            int level = spawner.getLevel();
            if (e.getEntity() instanceof Slime) {
                Bukkit.getScheduler().runTaskLater(Main.getPlugin(), () -> manager.getEntityLink().remove(e.getEntity().getUniqueId()), 40);
            } else {
                manager.getEntityLink().remove(e.getEntity().getUniqueId());
            }

            if (e.getEntity() instanceof Blaze) {
                e.getDrops().clear();
                e.getDrops().add(XMaterial.BLAZE_ROD.parseItem());
                e.setDroppedExp(5);
            }

            // Use cache for loot tables
            List<ItemStack> item_drops = lootTableCache.get(e.getEntity().getName());
            if (item_drops == null) {
                item_drops = Main.getPlugin().getLootTable(e.getEntity().getName());
                if (item_drops != null) {
                    lootTableCache.put(e.getEntity().getName(), item_drops);
                } else {
                    item_drops = new ArrayList<>(e.getDrops());
                }
            }

            // Use cache for XP drops
            Integer xp_drop = lootTableXPCache.get(e.getEntity().getName());
            if (xp_drop == null) {
                xp_drop = Main.getPlugin().getLootTableXP(e.getEntity().getName());
                if (xp_drop != null) {
                    lootTableXPCache.put(e.getEntity().getName(), xp_drop);
                } else {
                    xp_drop = e.getDroppedExp();
                }
            }

            ancStorage storage = spawner.getStorage();

            int finalXp_drop = xp_drop;
            List<ItemStack> finalItem_drops = item_drops;

            // ADDED: Normalize item durability before storing
            for (ItemStack item : finalItem_drops) {
                ItemMeta meta = item.getItemMeta();
                if (meta instanceof Damageable) {
                    Damageable dmg = (Damageable) meta;
                    dmg.setDamage(0); // Set damage to 0 to represent full durability
                    item.setItemMeta(meta);
                }
            }

            Bukkit.getScheduler().runTaskAsynchronously(Main.getPlugin(), () -> {
                if (spawner.isVirtualStorageEnabled()) {
                    if (!finalItem_drops.isEmpty()) {
                        for (ItemStack item_drop : finalItem_drops) {
                            int amount = item_drop.getAmount();
                            ItemStack check_this = item_drop.clone();
                            check_this.setAmount(1);
                            int new_storage_amount = storage.getStoredItem(check_this) + amount * level;
                            if (new_storage_amount >= spawner.getStorageLimit()) {
                                new_storage_amount = spawner.getStorageLimit();
                            }
                            storage.setStoredItem(check_this, new_storage_amount);
                        }
                    }
                } else {
                    if (!finalItem_drops.isEmpty()) {
                        for (ItemStack item_drop : finalItem_drops) {
                            int finalAmount = item_drop.getAmount() * level;
                            Bukkit.getScheduler().runTask(Main.getPlugin(), () -> dropItems(item_drop.getType().toString(), finalAmount, e.getEntity().getLocation()));
                        }
                    }
                }

                if (spawner.isXPStorageEnabled()) {
                    if (finalXp_drop > 0) {
                        int new_xp_amount = storage.getStoredXp() + finalXp_drop * level;
                        if (xpLimitCache == null) {
                            xpLimitCache = Main.getPlugin().getStorageModule().getInt("storage-limit.xpLimit");
                        }
                        if (new_xp_amount >= xpLimitCache) {
                            new_xp_amount = xpLimitCache;
                        }
                        storage.setStoredXp(new_xp_amount);
                    }
                } else {
                    if (finalXp_drop > 0) {
                        Bukkit.getScheduler().runTask(Main.getPlugin(), () -> dropXP(e.getEntity().getLocation(), finalXp_drop));
                    }
                }
            });

            e.getDrops().clear();
            e.setDroppedExp(0);
        }
    }

    public void dropItems(String material, int amount, Location location) {
        ItemStack itemStack = XMaterial.valueOf(material).parseItem();
        assert itemStack != null;
        while (amount >= 64) {
            amount -= 64;
            itemStack.setAmount(64);
            location.getWorld().dropItemNaturally(location, itemStack);
        }
        if (amount > 0) {
            itemStack.setAmount(amount);
            location.getWorld().dropItemNaturally(location, itemStack);
        }
    }

    public void dropXP(Location location, int amount) {
        location.getWorld().spawn(location, ExperienceOrb.class, x -> x.setExperience(amount));
    }

    @EventHandler
    public void onStatisticIncrement(PlayerStatisticIncrementEvent event) {
        Statistic statistic = event.getStatistic();
        if (statistic == Statistic.MOB_KILLS) {
            if (Main.getPlugin().getSpawnerManager().tamedWolf == null) {
                Main.getPlugin().getSpawnerManager().tameWolfForKills();
                return;
            }
            if (event.getPlayer().equals(Main.getPlugin().getSpawnerManager().tamedWolf.getOwner())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onSlimeSplit(SlimeSplitEvent e) {
        Entity entity = e.getEntity();
        UUID parentUUID = entity.getUniqueId();
        Location location = e.getEntity().getLocation();
        SpawnerManager manager = Main.getPlugin().getSpawnerManager();

        Runnable task = () -> {
            for (Entity child : entity.getWorld().getNearbyEntities(location, 5, 5, 5)) {
                if (child instanceof Slime) {

                    UUID childUUID = child.getUniqueId();
                    ancSpawner spawner;

                    if (manager.getEntityLink().containsKey(parentUUID)) {
                        spawner = manager.getEntityLink().get(parentUUID);
                        if (!manager.getEntityLink().containsKey(childUUID)) {
                            manager.getEntityLink().put(childUUID, spawner);
                        }
                    }

                    if (Main.getPlugin().getSpawnerManager().entityLink.containsKey(parentUUID)) {
                        spawner = Main.getPlugin().getSpawnerManager().entityLink.get(parentUUID);
                        if (!Main.getPlugin().getSpawnerManager().entityLink.containsKey(childUUID)) {
                            Main.getPlugin().getSpawnerManager().entityLink.put(childUUID, spawner);
                        }
                        ((LivingEntity) child).damage(((LivingEntity) child).getHealth() + 1, Main.getPlugin().getSpawnerManager().tamedWolf);
                    }
                }
            }
        };
        Bukkit.getScheduler().runTaskLater(Main.getPlugin(), task, 2);
    }
}
