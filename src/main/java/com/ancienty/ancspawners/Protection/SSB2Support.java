package com.ancienty.ancspawners.Protection;

import com.ancienty.ancspawners.Main;
import com.bgsoftware.superiorskyblock.api.SuperiorSkyblockAPI;
import com.bgsoftware.superiorskyblock.api.wrappers.SuperiorPlayer;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

public class SSB2Support {

    public boolean canPlace(Player player, Block block) {
        if (isLoaded()) {
            SuperiorPlayer superiorPlayer = SuperiorSkyblockAPI.getPlayer(player);
            boolean can_place = true;
            boolean is_member = false;
            boolean is_owner = false;
            boolean is_coop = false;
            if (SuperiorSkyblockAPI.getIslandAt(block.getLocation()) != null) {
                is_member = SuperiorSkyblockAPI.getIslandAt(block.getLocation()) != null && SuperiorSkyblockAPI.getIslandAt(block.getLocation()).getIslandMembers().contains(superiorPlayer);
                is_owner = SuperiorSkyblockAPI.getIslandAt(block.getLocation()) != null && SuperiorSkyblockAPI.getIslandAt(block.getLocation()).getOwner().equals(superiorPlayer);
                is_coop = SuperiorSkyblockAPI.getIslandAt(block.getLocation()) != null && SuperiorSkyblockAPI.getIslandAt(block.getLocation()).getCoopPlayers().contains(superiorPlayer);
            } else {
                is_member = true;
            }
            if (!is_member && !is_owner && !is_coop) {
                if (!player.isOp()) {
                    can_place = false;
                }
            }
            return can_place;
        }
        return true;
    }

    public boolean isLoaded() {
        return Main.getPlugin().getServer().getPluginManager().getPlugin("SuperiorSkyblock2") != null && Main.getPlugin().getServer().getPluginManager().isPluginEnabled("SuperiorSkyblock2");
    }
}
