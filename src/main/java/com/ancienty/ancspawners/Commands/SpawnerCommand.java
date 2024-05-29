package com.ancienty.ancspawners.Commands;

import com.ancienty.ancspawners.Main;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.IOException;

public class SpawnerCommand extends Command {


    public SpawnerCommand() {
        super(
                "ancspawners",
                new String[]{"spawners", "ancspawner"},
                "The main command for ancSpawners!",
                "ancspawners.admin");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (args.length > 0 && args[0].equalsIgnoreCase("give")) {
                if (Main.getPermissions().has(player, "ancspawners.admin")) {
                    if (args.length > 2) {
                        String playerName = args[1];
                        String spawnerType = args[2];
                        int spawnerAmount = 1;
                        if (args.length > 3) {
                            spawnerAmount = Integer.parseInt(args[3]);
                        }

                        ItemStack spawnerToGive = Main.getPlugin().getSpawner(spawnerType);
                        if (spawnerToGive != null) {
                            spawnerToGive.setAmount(spawnerAmount);

                            Player playerToReceive = Bukkit.getPlayer(playerName);

                            if (playerToReceive != null) {
                                playerToReceive.getInventory().addItem(spawnerToGive);
                                Main.getPlugin().sendMessage(player, "givenSpawners", new String[]{String.valueOf(spawnerAmount), Main.getPlugin().getSpawner(spawnerType).getItemMeta().getDisplayName(), playerToReceive.getName()});
                                Main.getPlugin().sendMessage(playerToReceive, "receivedSpawners", new String[]{String.valueOf(spawnerAmount), Main.getPlugin().getSpawner(spawnerType).getItemMeta().getDisplayName()});
                            } else {
                                Main.getPlugin().sendMessage(player, "playerNotFound", new String[]{});
                            }
                        } else {
                            Main.getPlugin().sendMessage(player, "spawnerNotFound", new String[]{});
                        }
                    }
                } else {
                    Main.getPlugin().sendMessage(player, "noPermission", new String[]{});
                }
            } else if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
                if (Main.getPermissions().has(player, "ancspawners.admin")) {
                    Main.getPlugin().reloadConfig();
                    try {Main.getPlugin().createLangFiles();} catch (IOException e) {throw new RuntimeException(e);}
                    Main.getPlugin().storageEnabled = Main.getPlugin().lang.getString("menu.storage.gui").equalsIgnoreCase("true");
                    Main.getPlugin().sendMessage(player, "reloadedConfig", new String[]{});
                } else {
                    Main.getPlugin().sendMessage(player, "noPermission", new String[]{});
                }
            } else {
                Main.getPlugin().sendMessage(player, "commandUsage", new String[]{});
            }
        }

        else {
            // If sender is console
            ConsoleCommandSender console = (ConsoleCommandSender) sender;
            if (args.length > 0 && args[0].equalsIgnoreCase("give")) {
                if (args.length > 2) {
                    String playerName = args[1];
                    String spawnerType = args[2];
                    int spawnerAmount = 1;
                    if (args.length > 3) {
                        spawnerAmount = Integer.parseInt(args[3]);
                    }

                    ItemStack spawnerToGive = Main.getPlugin().getSpawner(spawnerType);
                    if (spawnerToGive != null) {
                        spawnerToGive.setAmount(spawnerAmount);

                        Player playerToReceive = Bukkit.getPlayer(playerName);

                        if (playerToReceive != null) {
                            playerToReceive.getInventory().addItem(spawnerToGive);
                            console.sendMessage("You've successfully given " + spawnerAmount + " of " + spawnerType + " to " + playerToReceive + ".");
                            Main.getPlugin().sendMessage(playerToReceive, "receivedSpawners", new String[]{String.valueOf(spawnerAmount), Main.getPlugin().getSpawner(spawnerType).getItemMeta().getDisplayName()});
                        } else {
                            Main.getPlugin().getLogger().warning("Unable to give " + spawnerAmount + " of " + spawnerType + " to " + playerToReceive + ", player not found.");
                        }
                    } else {
                        Main.getPlugin().getLogger().warning("Unable to give spawner! Does not exist: " + args[2]);
                    }
                }
            }
        }
    }
}
