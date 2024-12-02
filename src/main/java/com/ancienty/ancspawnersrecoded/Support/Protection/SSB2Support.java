package com.ancienty.ancspawnersrecoded.Support.Protection;

import com.ancienty.ancspawnersrecoded.Main;
import com.bgsoftware.superiorskyblock.api.SuperiorSkyblockAPI;
import com.bgsoftware.superiorskyblock.api.island.Island;
import com.bgsoftware.superiorskyblock.api.wrappers.SuperiorPlayer;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

public class SSB2Support {

    public boolean canPlace(Player player, Block block) {
        if (isLoaded()) {
            SuperiorPlayer superiorPlayer = SuperiorSkyblockAPI.getPlayer(player);
            boolean canPlace = true;

            Island island = SuperiorSkyblockAPI.getIslandAt(block.getLocation());
            if (island != null) {
                boolean isMember = island.getIslandMembers(true).contains(superiorPlayer);
                boolean isOwner = island.getOwner().equals(superiorPlayer);
                boolean isCoop = island.getCoopPlayers().contains(superiorPlayer);

                if (!isMember && !isOwner && !isCoop) {
                    if (!player.isOp()) {
                        canPlace = false;
                    }
                }
                return canPlace;
            }
        }
        return true;
    }

    public boolean isLoaded() {
        return Main.getPlugin().getServer().getPluginManager().getPlugin("SuperiorSkyblock2") != null && Main.getPlugin().getServer().getPluginManager().isPluginEnabled("SuperiorSkyblock2");
    }
}
