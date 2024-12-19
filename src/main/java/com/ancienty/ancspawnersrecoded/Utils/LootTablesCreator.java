package com.ancienty.ancspawnersrecoded.Utils;

import com.ancienty.ancspawnersrecoded.Main;

import java.io.File;

public class LootTablesCreator {

    public void createLootTables() {
        Main plugin = Main.getPlugin();
        File loottables_creator = new File(Main.getPlugin().getDataFolder(), File.separator + "loottables" + File.separator + "example_zombie.yml");
        if (!loottables_creator.exists()) {
            plugin.saveResource("loottables" + File.separator + "example_zombie.yml", true);
        }
    }
}
