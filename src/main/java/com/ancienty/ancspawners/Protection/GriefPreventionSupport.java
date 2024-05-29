package com.ancienty.ancspawners.Protection;

import com.ancienty.ancspawners.Main;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.ClaimPermission;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.PlayerData;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class GriefPreventionSupport {

    public boolean canPlace(Player player, Block block) {
        if (isLoaded()) {
            // Get the GriefPrevention instance
            GriefPrevention gp = GriefPrevention.instance;

            // Get player data
            PlayerData playerData = gp.dataStore.getPlayerData(player.getUniqueId());

            // Get the claim at the block's location
            Claim claim = gp.dataStore.getClaimAt(block.getLocation(), false, playerData.lastClaim);

            // If there is no claim, allow placing the block
            if (claim == null) {
                return true;
            }

            // Check if the player has build permission in the claim
            String noBuildReason = claim.allowBuild(player, block.getType());

            // If the player does not have permission, return false
            if (noBuildReason != null) {
                return false;
            }

            // The player has permission, allow placing the block
        }
        return true;
    }

    public boolean isLoaded() {
        return Main.getPlugin().getServer().getPluginManager().getPlugin("GriefPrevention") != null && Main.getPlugin().getServer().getPluginManager().isPluginEnabled("GriefPrevention");
    }
}
