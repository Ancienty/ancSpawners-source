package com.ancienty.ancspawners.Listeners;

import com.ancienty.ancspawners.GUIs.MainGUI;
import com.ancienty.ancspawners.Main;
import com.ancienty.ancspawners.SpawnerManager.ancSpawner;
import com.cryptomorin.xseries.XMaterial;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public class SpawnerClickListener implements Listener {

    @EventHandler
    public void onSpawnerClickEvent(PlayerInteractEvent e)  {
        if (e.getClickedBlock() != null && e.getClickedBlock().getType() == XMaterial.SPAWNER.parseMaterial() && e.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
            Block clickedBlock = e.getClickedBlock();
            Player player = e.getPlayer();

            ancSpawner spawner = Main.getPlugin().getSpawnerManager().getSpawner(clickedBlock.getWorld(), clickedBlock.getLocation());

            if (spawner != null) {
                boolean isOwner = spawner.getOwnerUUID().equalsIgnoreCase(player.getUniqueId().toString());
                boolean isFriend = spawner.isFriend(player.getUniqueId().toString());

                if (isOwner || isFriend) {
                    Main.getPlugin().player_block_map.remove(player);
                    Main.getPlugin().player_block_map.put(player, clickedBlock);
                    new MainGUI(clickedBlock, player).openInventory();
                }
            }
        }
    }
}
