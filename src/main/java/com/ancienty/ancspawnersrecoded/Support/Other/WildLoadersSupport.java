package com.ancienty.ancspawnersrecoded.Support.Other;

import com.ancienty.ancspawnersrecoded.Main;
import com.bgsoftware.wildloaders.api.WildLoadersAPI;
import org.bukkit.Chunk;

public class WildLoadersSupport {

    public static boolean chunkHasActiveWildLoader(Chunk chunk) {
        if (!isLoaded()) return false;
        if (WildLoadersAPI.getChunkLoader(chunk).isPresent()) {
            return true;
        }
        return false;
    }

    public static boolean isLoaded() {
        return Main.getPlugin().getServer().getPluginManager().getPlugin("WildLoaders") != null && Main.getPlugin().getServer().getPluginManager().isPluginEnabled("WildLoaders");
    }
}
