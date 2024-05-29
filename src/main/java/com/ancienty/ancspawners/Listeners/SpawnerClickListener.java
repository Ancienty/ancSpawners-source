package com.ancienty.ancspawners.Listeners;

import com.ancienty.ancspawners.GUIs.MainGUI;
import com.ancienty.ancspawners.Main;
import com.cryptomorin.xseries.XMaterial;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.HashMap;
import java.util.concurrent.ExecutionException;

public class SpawnerClickListener implements Listener {

    @EventHandler
    public void onSpawnerClickEvent(PlayerInteractEvent e) throws ExecutionException, InterruptedException {
        if (e.getClickedBlock() != null && e.getClickedBlock().getType() == XMaterial.SPAWNER.parseMaterial() && e.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
            Block clicked_block = e.getClickedBlock();
            Player player = e.getPlayer();
            Main.database.getSpawnerOwnerUuid(clicked_block).thenAccept(owner_uuid -> {
                Main.database.checkIfFriend(clicked_block, player).thenAccept(friend -> {
                    try {
                        Main.database.checkStorage(clicked_block);
                    } catch (ExecutionException | InterruptedException ex) {
                        throw new RuntimeException(ex);
                    }
                    Bukkit.getScheduler().runTask(Main.getPlugin(), () -> {
                        if (owner_uuid != null) {
                            boolean is_owner = false;
                            boolean is_friend = friend;
                            if (owner_uuid.equalsIgnoreCase(player.getUniqueId().toString())) {
                                is_owner = true;
                            }

                            if (is_owner || is_friend) {
                                Main.getPlugin().player_block_map.remove(player);
                                Main.getPlugin().player_block_map.put(player, clicked_block);
                                new MainGUI(clicked_block, player).openInventory();
                            }
                        }
                    });
                });
            });
        }
    }

}
