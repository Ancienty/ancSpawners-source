package com.ancienty.ancspawnersrecoded.Utils;

import com.ancienty.ancspawnersrecoded.Main;
import org.bukkit.Bukkit;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Utils {

    private static final String API_URL = "http://141.11.109.225:5000/check_license?ip=";
    private static final String PLUGIN_NAME = "ancspawners";

    public boolean checkLicense() {
        try {
            String localIPAddress = getLocalIPAddress();
            if (localIPAddress == null) {
                Bukkit.getConsoleSender().sendMessage("Could not determine the local IP address.");
                return false;
            }

            String externalIPAddress = getExternalIPAddress();
            if (externalIPAddress == null) {
                Bukkit.getConsoleSender().sendMessage("Could not determine the external IP address.");
                return false;
            }

            String urlString = API_URL + externalIPAddress + "&plugin=" + PLUGIN_NAME;
            Logger logger = Main.getPlugin().getLogger();
            logger.info("(License) - License IP: " + externalIPAddress); // Log external IP

            try {
                JSONObject response = sendGET(urlString);

                String status = response.getString("status");
                String discordid = response.getString("discordid");

                if ("valid".equals(status)) {
                    logger.info("(License) - License verified, plugin is activated.");
                    logger.info("(License) - Discord ID: " + discordid);
                    return true;
                } else {
                    logger.severe("License not found for this IP address, shutting down the server.");
                    Bukkit.getScheduler().cancelTasks(Main.getPlugin());
                    Bukkit.shutdown();
                    return false;
                }
            } catch (IOException | JSONException e) {
                logger.log(Level.SEVERE, "Error checking license: ", e);
                Bukkit.getConsoleSender().sendMessage("An error occurred while checking the license. Please try again later.");
            }

        } catch (SocketException e) {
            Bukkit.getConsoleSender().sendMessage("Error obtaining network interfaces.");
        }
        return false;
    }

    private String getExternalIPAddress() {
        try {
            URL whatismyip = new URL("http://checkip.amazonaws.com");
            BufferedReader in = new BufferedReader(new InputStreamReader(
                    whatismyip.openStream()));

            String ip = in.readLine();
            return ip;
        } catch (IOException e) {
            Logger logger = Main.getPlugin().getLogger();
            logger.log(Level.SEVERE, "Error fetching external IP address: ", e);
            return null;
        }
    }

    private String getLocalIPAddress() throws SocketException {
        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
        while (interfaces.hasMoreElements()) {
            NetworkInterface iface = interfaces.nextElement();
            if (iface.isLoopback() || !iface.isUp()) {
                continue;
            }
            Enumeration<InetAddress> addresses = iface.getInetAddresses();
            while (addresses.hasMoreElements()) {
                InetAddress addr = addresses.nextElement();
                if (!addr.isLoopbackAddress() && !addr.isLinkLocalAddress() && !addr.isMulticastAddress()) {
                    return addr.getHostAddress();
                }
            }
        }
        return null;
    }

    private JSONObject sendGET(String url) throws IOException, JSONException {
        URL obj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();
        con.setRequestMethod("GET");
        con.setRequestProperty("User-Agent", "Mozilla/5.0");

        int responseCode = con.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuilder response = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            return new JSONObject(response.toString());
        } else {
            throw new IOException("Response code: " + responseCode);
        }
    }
}
