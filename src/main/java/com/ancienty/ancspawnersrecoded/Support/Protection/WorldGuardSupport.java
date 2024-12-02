package com.ancienty.ancspawnersrecoded.Support.Protection;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Set;

import static org.bukkit.Bukkit.getServer;

public class WorldGuardSupport {


    public boolean canPlace(Player player, Block block) {
        if (isLoaded()) {
            RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
            RegionManager regions = container.get(BukkitAdapter.adapt(player.getWorld()));
            LocalPlayer localPlayer = WorldGuardPlugin.inst().wrapPlayer(player);
            assert regions != null;
            ApplicableRegionSet set = regions.getApplicableRegions(BlockVector3.at(block.getX(), block.getY(), block.getZ()));
            RegionQuery query = container.createQuery();

            Set<ProtectedRegion> regionSet = set.getRegions();
            boolean can_build = true;
            for (ProtectedRegion region : regionSet) {
                if (region.isMember(localPlayer) || region.isOwner(localPlayer)) {
                    can_build = true;
                } else if (!query.testState(BukkitAdapter.adapt(block.getLocation()), localPlayer, Flags.BUILD)) {
                    can_build = false;
                }
            }

            if (player.isOp()) {
                can_build = true;
            }

            return can_build;
        }
        return true;
    }

    public boolean isLoaded() {
        Plugin plugin = getServer().getPluginManager().getPlugin("WorldGuard");
        return plugin != null && plugin.isEnabled();
    }
}
