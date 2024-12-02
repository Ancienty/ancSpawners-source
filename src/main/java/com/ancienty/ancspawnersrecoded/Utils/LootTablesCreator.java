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

        /*loottables_creator = new File(Main.getPlugin().getDataFolder(), File.separator + "loottables" + File.separator + "blaze.yml");
        if (!loottables_creator.exists()) {
            plugin.saveResource("loottables" + File.separator + "blaze.yml", true);
        }

        loottables_creator = new File(Main.getPlugin().getDataFolder(), File.separator + "loottables" + File.separator + "chicken.yml");
        if (!loottables_creator.exists()) {
            plugin.saveResource("loottables" + File.separator + "chicken.yml", true);
        }

        loottables_creator = new File(Main.getPlugin().getDataFolder(), File.separator + "loottables" + File.separator + "cow.yml");
        if (!loottables_creator.exists()) {
            plugin.saveResource("loottables" + File.separator + "cow.yml", true);
        }

        loottables_creator = new File(Main.getPlugin().getDataFolder(), File.separator + "loottables" + File.separator + "creeper.yml");
        if (!loottables_creator.exists()) {
            plugin.saveResource("loottables" + File.separator + "creeper.yml", true);
        }

        loottables_creator = new File(Main.getPlugin().getDataFolder(), File.separator + "loottables" + File.separator + "enderman.yml");
        if (!loottables_creator.exists()) {
            plugin.saveResource("loottables" + File.separator + "enderman.yml", true);
        }

        loottables_creator = new File(Main.getPlugin().getDataFolder(), File.separator + "loottables" + File.separator + "iron_golem.yml");
        if (!loottables_creator.exists()) {
            plugin.saveResource("loottables" + File.separator + "iron_golem.yml", true);
        }

        loottables_creator = new File(Main.getPlugin().getDataFolder(), File.separator + "loottables" + File.separator + "magma_cube.yml");
        if (!loottables_creator.exists()) {
            plugin.saveResource("loottables" + File.separator + "magma_cube.yml", true);
        }

        loottables_creator = new File(Main.getPlugin().getDataFolder(), File.separator + "loottables" + File.separator + "pig.yml");
        if (!loottables_creator.exists()) {
            plugin.saveResource("loottables" + File.separator + "pig.yml", true);
        }

        loottables_creator = new File(Main.getPlugin().getDataFolder(), File.separator + "loottables" + File.separator + "rabbit.yml");
        if (!loottables_creator.exists()) {
            plugin.saveResource("loottables" + File.separator + "rabbit.yml", true);
        }

        loottables_creator = new File(Main.getPlugin().getDataFolder(), File.separator + "loottables" + File.separator + "sheep.yml");
        if (!loottables_creator.exists()) {
            plugin.saveResource("loottables" + File.separator + "sheep.yml", true);
        }

        loottables_creator = new File(Main.getPlugin().getDataFolder(), File.separator + "loottables" + File.separator + "skeleton.yml");
        if (!loottables_creator.exists()) {
            plugin.saveResource("loottables" + File.separator + "skeleton.yml", true);
        }

        loottables_creator = new File(Main.getPlugin().getDataFolder(), File.separator + "loottables" + File.separator + "slime.yml");
        if (!loottables_creator.exists()) {
            plugin.saveResource("loottables" + File.separator + "slime.yml", true);
        }

        loottables_creator = new File(Main.getPlugin().getDataFolder(), File.separator + "loottables" + File.separator + "spider.yml");
        if (!loottables_creator.exists()) {
            plugin.saveResource("loottables" + File.separator + "spider.yml", true);
        }

        loottables_creator = new File(Main.getPlugin().getDataFolder(), File.separator + "loottables" + File.separator + "zombie.yml");
        if (!loottables_creator.exists()) {
            plugin.saveResource("loottables" + File.separator + "zombie.yml", true);
        }*/

    }
}
