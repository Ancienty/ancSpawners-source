package com.ancienty.ancspawnersrecoded.Utils;

import com.ancienty.ancspawnersrecoded.Main;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

public class UpdateChecker implements Listener {

    public boolean checkForUpdates() {
        try {

            String pluginVersion = Main.getPlugin().getDescription().getVersion();

            URL url = new URL("https://raw.githubusercontent.com/Ancienty/ancSpawners/main/version");
            BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
            String latestVersion = reader.readLine();
            reader.close();

            String licensedVersion = latestVersion + "-licensed";

            // Compare versions
            if (!latestVersion.equals(pluginVersion) && !licensedVersion.equals(pluginVersion)) {
                Main.getPlugin().getLogger().warning("A newer version of ancSpawners (v" + latestVersion + ") is available!");
                return true;
            }
        } catch (IOException ignored) {
        }
        return false;
    }

    public String getLatestVersion() {
        try {

            String pluginVersion = Main.getPlugin().getDescription().getVersion();

            URL url = new URL("https://raw.githubusercontent.com/Ancienty/ancSpawners/main/version");
            BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
            String latestVersion = reader.readLine();
            reader.close();
            return latestVersion;

        } catch (IOException ignored) {
        }
        return "Version could not be found.";
    }

    @EventHandler
    public void onPlayerJoinEvent(PlayerJoinEvent e) {
        if (e.getPlayer().isOp()) {
            if (checkForUpdates()) {
                String message = "&8[&6ancSpawners&8] &7A newer version of &eancSpawners &7is out! Latest version: &ev" + getLatestVersion() + "\n&8[&6ancSpawners&8] &7Please update &cas soon as possible!";
                Bukkit.getScheduler().runTaskLater(Main.getPlugin(), () -> {
                    e.getPlayer().sendMessage(ChatColor.translateAlternateColorCodes('&', message));
                }, 100);
            }
        }
    }
}
