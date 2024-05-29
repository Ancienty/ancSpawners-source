package com.ancienty.ancspawners.Utils;

import com.ancienty.ancspawners.Main;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

public class UpdateChecker {

    public void checkForUpdates() {
        try {

            String pluginVersion = Main.getPlugin().getDescription().getVersion();

            // Define your plugin's update URL
            URL url = new URL("https://raw.githubusercontent.com/Ancienty/ancSpawners/main/version");
            BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
            String latestVersion = reader.readLine();
            reader.close();

            String licensedVersion = latestVersion + "-licensed";

            // Compare versions
            if (!latestVersion.equals(pluginVersion) || !licensedVersion.equals(pluginVersion)) {
                Main.getPlugin().getLogger().warning("A newer version of ancSpawners (v" + latestVersion + ") is available!");
                // Notify admins or players about the update
                // You can use Bukkit's messaging system to notify players
            }
        } catch (IOException ignored) {
        }
    }
}
