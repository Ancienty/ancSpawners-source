package com.ancienty.ancspawners.Listeners;

import com.ancienty.ancspawners.Main;
import com.ancienty.ancspawners.SpawnerKilling.DeathListener;
import com.ancienty.ancspawners.SpawnerManager.ancSpawner;
import com.ancienty.ancspawners.SpawnerManager.ancStorage;
import com.cryptomorin.xseries.XMaterial;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.SlimeSplitEvent;
import org.bukkit.event.entity.SpawnerSpawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import javax.annotation.Nullable;
import java.util.*;

public class SpawnerSpawnListener implements Listener {

    private static final int MAX_STACK_SIZE = 64;
    public static final Map<UUID, ancSpawner> entityLinkToSpawners = Collections.synchronizedMap(new HashMap<>());
    private DeathListener deathListener = null;

    @EventHandler
    public void onSpawnerSpawnEvent(SpawnerSpawnEvent e) {
        if (deathListener == null) {
            deathListener = new DeathListener();
        }
        Block block = e.getSpawner().getBlock();
        ancSpawner spawner = Main.getPlugin().getSpawnerManager().getSpawner(block.getWorld(), block.getLocation());
        if (spawner != null) {
            if (spawner.getMode().equalsIgnoreCase("ITEM")) {
                ancStorage storage = spawner.getStorage();
                String material_name = Main.getPlugin().getConfig().getString("spawners." + spawner.getType() + ".spawnerInfo.material");
                if (material_name != null) {

                    if (spawner.isVirtualStorageEnabled()) {
                        Material material = XMaterial.valueOf(material_name).parseMaterial();
                        if (material != null) {
                            storage.setStoredItem(material, storage.getStoredItem(material) + spawner.getLevel());
                        }
                    } else {
                        String item_name = Main.getPlugin().getConfig().getString("spawners." + spawner.getType() + ".spawnerInfo.details.name") != null ? Main.getPlugin().getConfig().getString("spawners." + spawner.getType() + ".spawnerInfo.details.name") : null;
                        List<String> item_lore = Main.getPlugin().getConfig().getStringList("spawners." + spawner.getType() + ".spawnerInfo.details.lore") != null ? Main.getPlugin().getConfig().getStringList("spawners." + spawner.getType() + ".spawnerInfo.details.lore") : null;
                        dropCustomItems(material_name, Main.getPlugin().getSpawnerManager().getNewLocation(e.getSpawner().getLocation(), 10), spawner.getLevel(), item_name, item_lore);
                    }
                }
            }
            e.getEntity().remove();
        }
    }

    public void dropCustomItems(String material_name, Location location, Integer amount, @Nullable String item_name, @Nullable List<String> item_lore) {
        ItemStack itemStack;
        try {
            itemStack = XMaterial.valueOf(material_name).parseItem();
        } catch (IllegalArgumentException ex) {
            return;
        }
        if (itemStack == null) return;

        ItemMeta itemMeta = itemStack.getItemMeta();
        if (itemMeta != null) {
            if (item_name != null) {
                itemMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', item_name));
            }

            if (item_lore != null) {
                List<String> real_lore = new ArrayList<>();
                for (String lore : item_lore) {
                    real_lore.add(ChatColor.translateAlternateColorCodes('&', lore));
                }
                itemMeta.setLore(real_lore);
            }
            itemStack.setItemMeta(itemMeta);
        }

        while (amount >= MAX_STACK_SIZE) {
            amount -= MAX_STACK_SIZE;
            itemStack.setAmount(MAX_STACK_SIZE);
            location.getWorld().dropItemNaturally(location, itemStack);
        }
        if (amount > 0) {
            itemStack.setAmount(amount);
            location.getWorld().dropItemNaturally(location, itemStack);
        }
    }

    public boolean loottableOnlyAutokill() {
        return Main.getPlugin().getConfig().getBoolean("config.modules.auto-kill.loottable-only-autokill");
    }

    @EventHandler
    public void onEntityDeathEvent(EntityDeathEvent e) {
        if (deathListener == null) {
            deathListener = new DeathListener();
        }
        UUID entityUUID = e.getEntity().getUniqueId();
        ancSpawner spawner = entityLinkToSpawners.get(entityUUID);
        if (spawner != null) {
            int level = spawner.getLevel();
            if (e.getEntity() instanceof Slime) {
                Bukkit.getScheduler().runTaskLater(Main.getPlugin(), () -> entityLinkToSpawners.remove(entityUUID), 40);
            } else {
                entityLinkToSpawners.remove(entityUUID);
            }

            List<ItemStack> item_drops;
            Integer xp_drop;
            if (loottableOnlyAutokill()) {
                item_drops = Main.getPlugin().getLootTable(e.getEntity().getName()) != null ? Main.getPlugin().getLootTable(e.getEntity().getName()) : new ArrayList<>(e.getDrops());
                xp_drop = Main.getPlugin().getLootTableXP(e.getEntity().getName());
            } else {
                item_drops = new ArrayList<>(e.getDrops());
                xp_drop = e.getDroppedExp();
            }

            if (xp_drop == null) {
                xp_drop = 0;
            }

            ancStorage storage = spawner.getStorage();
            if (item_drops != null) {
                for (ItemStack item_drop : item_drops) {
                    if (spawner.isVirtualStorageEnabled()) {
                        int new_storage_amount = storage.getStoredItem(item_drop.getType()) + item_drop.getAmount() * level;
                        new_storage_amount = Math.min(new_storage_amount, spawner.getStorageLimit());
                        storage.setStoredItem(item_drop.getType(), new_storage_amount);
                    } else {
                        deathListener.dropItems(item_drop.getType().toString(), item_drop.getAmount() * level, e.getEntity().getLocation());
                    }
                }
            }

            if (spawner.isXPStorageEnabled()) {
                if (xp_drop > 0) {
                        int new_xp_amount = storage.getStoredXp() + xp_drop * level;
                        new_xp_amount = Math.min(new_xp_amount, Main.getPlugin().getConfig().getInt("config.modules.storage-limit.xpLimit"));
                        storage.setStoredXp(new_xp_amount);
                        deathListener.dropXP(e.getEntity().getLocation(), xp_drop);
                }
            } else {
                deathListener.dropXP(e.getEntity().getLocation(), xp_drop);
            }

            e.getDrops().clear();
            e.setDroppedExp(0);
        }
    }

    @EventHandler
    public void onSlimeSplit(SlimeSplitEvent e) {
        Entity entity = e.getEntity();
        UUID parentUUID = entity.getUniqueId();
        Location location = e.getEntity().getLocation();

        Runnable task = () -> {
            for (Entity child : entity.getWorld().getNearbyEntities(location, 5, 5, 5)) {
                if (child instanceof Slime) {

                    UUID childUUID = child.getUniqueId();
                    ancSpawner spawner;

                    if (entityLinkToSpawners.containsKey(parentUUID)) {
                        spawner = entityLinkToSpawners.get(parentUUID);
                        if (!entityLinkToSpawners.containsKey(childUUID)) {
                            entityLinkToSpawners.put(childUUID, spawner);
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
