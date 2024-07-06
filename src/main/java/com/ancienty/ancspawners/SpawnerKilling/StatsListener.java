package com.ancienty.ancspawners.SpawnerKilling;

import com.ancienty.ancspawners.Main;
import org.bukkit.Statistic;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerStatisticIncrementEvent;

public class StatsListener implements Listener {

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
}
