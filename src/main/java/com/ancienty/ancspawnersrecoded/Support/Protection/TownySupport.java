package com.ancienty.ancspawnersrecoded.Support.Protection;

import com.ancienty.ancspawnersrecoded.Main;
import com.palmergames.bukkit.towny.object.TownyPermission;
import com.palmergames.bukkit.towny.utils.PlayerCacheUtil;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

public class TownySupport {

    public boolean canPlace(Player player, Block block) {
        if (isLoaded()) {
            return PlayerCacheUtil.getCachePermission(player, block.getLocation(), block.getType(), TownyPermission.ActionType.BUILD);
        } else {
            return true;
        }
    }


    public boolean isLoaded() {
        return Main.getPlugin().getServer().getPluginManager().getPlugin("Towny") != null && Main.getPlugin().getServer().getPluginManager().isPluginEnabled("Towny");
    }
}
