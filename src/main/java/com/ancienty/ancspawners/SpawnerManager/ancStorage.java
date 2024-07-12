package com.ancienty.ancspawners.SpawnerManager;

import com.ancienty.ancspawners.Main;
import com.cryptomorin.xseries.XMaterial;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;

import com.ancienty.ancspawners.Database.SQLite;

public class ancStorage {

    private HashMap<Material, Integer> storage = new HashMap<>();
    private Integer stored_xp;
    private ancSpawner spawner;

    public ancStorage(World world, Location location, ancSpawner spawner) {
        this.spawner = spawner;
        loadStorageFromDatabase(world, location);
    }

    private void loadStorageFromDatabase(World world, Location location) {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;

        try {
            connection = SQLite.dataSource.getConnection();
            String query = "SELECT * FROM storage WHERE world = ? AND location = ?";
            preparedStatement = connection.prepareStatement(query);
            preparedStatement.setString(1, world.getName());
            preparedStatement.setString(2, Main.getPlugin().getSpawnerManager().getLocationString(location));
            resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                ItemStack item = XMaterial.valueOf(resultSet.getString("item")).parseItem();
                int amount = resultSet.getInt("amount");
                storage.put(item.getType(), amount);
            }

            // Closing the first resultSet and preparedStatement
            resultSet.close();
            preparedStatement.close();

            query = "SELECT * FROM storage_xp WHERE world = ? AND location = ?";
            preparedStatement = connection.prepareStatement(query);
            preparedStatement.setString(1, world.getName());
            preparedStatement.setString(2, Main.getPlugin().getSpawnerManager().getLocationString(location));
            resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                stored_xp = resultSet.getInt("xp");
            } else {
                stored_xp = 0; // Initialize to 0 if no data found
            }
        } catch (SQLException exception) {
            exception.printStackTrace();
        } finally {
            try {
                if (resultSet != null) resultSet.close();
                if (preparedStatement != null) preparedStatement.close();
                if (connection != null) connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public HashMap<Material, Integer> getStorage() {
        return storage;
    }

    public Integer getStoredItem(Material item) {
        return storage.get(item) != null ? storage.get(item) : 0;
    }

    public void setStoredItem(Material item, int amount) {
        storage.put(item, amount);
        if (!Main.getPlugin().getSpawnerManager().updatedSpawnerList.contains(spawner)) {
            Main.getPlugin().getSpawnerManager().updatedSpawnerList.add(spawner);
        }
    }

    public Integer getStoredXp() {
        return stored_xp != null ? stored_xp : 0;
    }

    public void setStoredXp(Integer xp) {
        stored_xp = xp;
        if (!Main.getPlugin().getSpawnerManager().updatedSpawnerList.contains(spawner)) {
            Main.getPlugin().getSpawnerManager().updatedSpawnerList.add(spawner);
        }
    }

    public int getTotalStored() {
        return storage.values().stream().mapToInt(Integer::intValue).sum();
    }

    public double getMoney(Player player) {
        double totalMoney = Main.getPlugin().getPlayerMultiplier(player) * storage.entrySet().stream()
                .mapToDouble(entry -> Main.getPlugin().getPriceForItem(entry.getKey().name()) * entry.getValue())
                .sum();

        BigDecimal roundedMoney = new BigDecimal(totalMoney).setScale(2, RoundingMode.HALF_UP);
        return roundedMoney.doubleValue();
    }

    public void clearStorage() {
        getStorage().clear();
        if (!Main.getPlugin().getSpawnerManager().updatedSpawnerList.contains(spawner)) {
            Main.getPlugin().getSpawnerManager().updatedSpawnerList.add(spawner);
        }
    }

    public ancSpawner getSpawner() {
        return this.spawner;
    }
}
