package com.ancienty.ancspawnersrecoded.Commands;

import com.ancienty.ancspawnersrecoded.Main;
import com.ancienty.ancspawnersrecoded.Utils.BoostUtils;
import com.cryptomorin.xseries.XEntityType;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.StringUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class SpawnerCommand extends Command {

    public SpawnerCommand() {
        super(
                "ancspawners",
                new String[]{"spawners", "ancspawner", "asp"},
                "The main command for ancSpawners!",
                "ancspawners.admin");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sendUsageMessage(sender);
            return;
        }

        if (args[0].equalsIgnoreCase("give")) {
            handleGiveCommand(sender, args);
        } else if (args[0].equalsIgnoreCase("reload")) {
            handleReloadCommand(sender);
        } else if (args[0].equalsIgnoreCase("giveboost")) {
            handleGiveBoostCommand(sender, args);
        } else {
            sendUsageMessage(sender);
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return StringUtil.copyPartialMatches(args[0], Arrays.asList("give", "giveboost", "reload"), new ArrayList<>());
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("reload")) {
                return new ArrayList<>();
            }
            List<String> player_names = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                player_names.add(p.getName());
            }
            return StringUtil.copyPartialMatches(args[1], player_names, new ArrayList<>());
        } else if (args.length == 3) {
            if (args[0].equalsIgnoreCase("give")) {
                List<String> spawner_names = new ArrayList<>(Main.getPlugin().getConfig().getConfigurationSection("spawners").getKeys(false));
                for (XEntityType type : XEntityType.values()) {
                    String name = type.name().toLowerCase(Locale.ENGLISH);
                    name = name.substring(0, 1).toUpperCase(Locale.ENGLISH) + name.substring(1);
                    spawner_names.add(name);
                }
                return StringUtil.copyPartialMatches(args[2], spawner_names, new ArrayList<>());
            } if (args[0].equalsIgnoreCase("giveboost")) {
                List<String> boost_names = new ArrayList<>(Main.getPlugin().getBoostsModule().getConfigurationSection("boosts.list").getKeys(false));
                return StringUtil.copyPartialMatches(args[2], boost_names, new ArrayList<>());
            }
        }

        return new ArrayList<>();
    }

    private void handleGiveCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ancspawners.admin")) {
            if (sender instanceof Player) {
                Main.getPlugin().sendMessage((Player) sender, "noPermission", new String[]{});
            }
            return;
        }

        if (args.length < 3) {
            if (sender instanceof Player) {
                Main.getPlugin().sendMessage((Player) sender, "commandUsageGive", new String[]{});
            }
            return;
        }

        String playerName = args[1];
        String spawnerType = args[2];
        int spawnerAmount = 1;
        if (args.length > 3) {
            try {
                spawnerAmount = Integer.parseInt(args[3]);
            } catch (NumberFormatException e) {
                spawnerAmount = 1;
            }
        }

        ItemStack spawnerToGive = Main.getPlugin().getSpawner(spawnerType);
        if (spawnerToGive == null) {
            if (sender instanceof Player) {
                Main.getPlugin().sendMessage((Player) sender, "spawnerNotFound", new String[]{spawnerType});
            }
            return;
        }
        spawnerToGive.setAmount(spawnerAmount);

        Player playerToReceive = Bukkit.getPlayer(playerName);

        if (playerToReceive != null) {
            playerToReceive.getInventory().addItem(spawnerToGive);
            if (sender instanceof Player) {
                Main.getPlugin().sendMessage((Player) sender, "givenSpawners", new String[]{String.valueOf(spawnerAmount), spawnerToGive.getItemMeta().getDisplayName(), playerToReceive.getName()});
            } else {
                sender.sendMessage("You've successfully given " + spawnerAmount + " of " + spawnerType + " to " + playerToReceive.getName() + ".");
            }
            Main.getPlugin().sendMessage(playerToReceive, "receivedSpawners", new String[]{String.valueOf(spawnerAmount), spawnerToGive.getItemMeta().getDisplayName()});
        } else {
            if (sender instanceof Player) {
                Main.getPlugin().sendMessage((Player) sender, "playerNotFound", new String[]{});
            } else {
                sender.sendMessage("Player not found: " + playerName);
            }
        }
    }

    private void handleReloadCommand(CommandSender sender) {
        if (!sender.hasPermission("ancspawners.admin")) {
            if (sender instanceof Player) {
                Main.getPlugin().sendMessage((Player) sender, "noPermission", new String[]{});
            }
            return;
        }

        Main.getPlugin().reloadConfig();
        Main.getPlugin().getSpawnerManager().config_to_delay_cache.clear();
        Main.getPlugin().getSpawnerManager().itemStackCache.clear();
        try {
            Main.getPlugin().createLangFiles();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Main.getPlugin().loadConfig("autokill");
        Main.getPlugin().loadConfig("friends");
        Main.getPlugin().loadConfig("hologram");
        Main.getPlugin().loadConfig("multipliers");
        Main.getPlugin().loadConfig("other");
        Main.getPlugin().loadConfig("settings");
        Main.getPlugin().loadConfig("storage_limits");
        if (sender instanceof Player) {
            Main.getPlugin().sendMessage((Player) sender, "reloadedConfig", new String[]{});
        } else {
            sender.sendMessage("Config has been reloaded successfully.");
        }
    }

    private void handleGiveBoostCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ancspawners.admin")) {
            if (sender instanceof Player) {
                Main.getPlugin().sendMessage((Player) sender, "noPermission", new String[]{});
            }
            return;
        }

        if (args.length < 3) {
            if (sender instanceof Player) {
                Main.getPlugin().sendMessage((Player) sender, "commandUsageGiveBoost", new String[]{});
            }
            return;
        }

        String playerName = args[1];
        String boostKey = args[2];

        Player targetPlayer = Bukkit.getPlayer(playerName);
        if (targetPlayer == null) {
            if (sender instanceof Player) {
                Main.getPlugin().sendMessage((Player) sender, "playerNotFound", new String[]{});
            } else {
                sender.sendMessage("Player not found: " + playerName);
            }
            return;
        }

        ItemStack boostItem = BoostUtils.createBoostItem(boostKey);
        if (boostItem == null) {
            if (sender instanceof Player) {
                Main.getPlugin().sendMessage((Player) sender, "boostNotFound", new String[]{boostKey});
            } else {
                sender.sendMessage("Boost not found: " + boostKey);
            }
            return;
        }

        targetPlayer.getInventory().addItem(boostItem);

        // Send messages to the sender and the target player
        if (sender instanceof Player) {
            Main.getPlugin().sendMessage((Player) sender, "givenBoost", new String[]{boostKey, targetPlayer.getName()});
        } else {
            sender.sendMessage("You've given the boost " + boostKey + " to " + targetPlayer.getName() + ".");
        }
        Main.getPlugin().sendMessage(targetPlayer, "receivedBoost", new String[]{boostKey});
    }

    private void sendUsageMessage(CommandSender sender) {
        if (sender instanceof Player) {
            Main.getPlugin().sendMessage((Player) sender, "commandUsage", new String[]{});
        } else {
            sender.sendMessage("Correct usage: /ancspawners <give/giveboost/reload> [arguments]");
        }
    }
}
