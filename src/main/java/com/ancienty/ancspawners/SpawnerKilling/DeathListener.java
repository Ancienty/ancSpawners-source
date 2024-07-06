package com.ancienty.ancspawners.SpawnerKilling;

import com.ancienty.ancspawners.Listeners.SpawnerSpawnListener;
import com.ancienty.ancspawners.Main;
import com.ancienty.ancspawners.SpawnerManager.SpawnerManager;
import com.ancienty.ancspawners.SpawnerManager.ancSpawner;
import com.ancienty.ancspawners.SpawnerManager.ancStorage;
import com.cryptomorin.xseries.XMaterial;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Slime;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class DeathListener implements Listener {

    private final SpawnerSpawnListener spawnerSpawnListener = new SpawnerSpawnListener();

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
            List<ItemStack> item_drops = Main.getPlugin().getLootTable(e.getEntity().getName()) != null ? Main.getPlugin().getLootTable(e.getEntity().getName()) : new ArrayList<>(e.getDrops());
            Integer xp_drop = Main.getPlugin().getLootTableXP(e.getEntity().getName());
            if (xp_drop == null || xp_drop == 0) {
                xp_drop = e.getDroppedExp();
            }

            if (Main.getPlugin().storageEnabled) {
                ancStorage storage = spawner.getStorage();
                if (item_drops != null) {
                    for (ItemStack item_drop : item_drops) {
                        int new_storage_amount = storage.getStoredItem(item_drop.getType()) + item_drop.getAmount() * level;
                        if (new_storage_amount >= spawner.getStorageLimit()) {
                            new_storage_amount = spawner.getStorageLimit();
                        }
                        storage.setStoredItem(item_drop.getType(), new_storage_amount);
                    }
                }
                if (xp_drop > 0) {
                    if (spawnerSpawnListener.isXPStorageEnabled()) {
                        int new_xp_amount = storage.getStoredXp() + xp_drop * level;
                        if (new_xp_amount >= Main.getPlugin().getConfig().getInt("config.modules.storage-limit.xpLimit")) {
                            new_xp_amount = Main.getPlugin().getConfig().getInt("config.modules.storage-limit.xpLimit");
                        }
                        storage.setStoredXp(new_xp_amount);
                    } else {
                        dropXP(e.getEntity().getLocation(), xp_drop);
                    }
                }

            } else {
                if (item_drops != null) {
                    for (ItemStack item_drop : item_drops) {
                        dropItems(item_drop.getType().toString(), item_drop.getAmount() * level, e.getEntity().getLocation());
                    }
                } if (xp_drop > 0) {
                    dropXP(e.getEntity().getLocation(), xp_drop);
                }
            }

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
        } if (amount > 0) {
            itemStack.setAmount(amount);
            location.getWorld().dropItemNaturally(location, itemStack);
        }
    }

    public void dropXP(Location location, int amount) {
        location.getWorld().spawn(location, ExperienceOrb.class, x -> x.setExperience(amount));
    }
}
