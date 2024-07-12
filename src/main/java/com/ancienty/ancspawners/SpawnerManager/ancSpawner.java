package com.ancienty.ancspawners.SpawnerManager;

import com.ancienty.ancspawners.Main;
import com.cryptomorin.xseries.XMaterial;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

public class ancSpawner {

    private final World world;
    private final Location location;
    private int level;
    private final String ownerUUID;
    private final String type;
    private final String mode;
    private boolean autokill;
    private final ancStorage storage;
    private int storage_limit;
    private final List<String> friend_uuids;
    private boolean virtual_storage;
    private boolean xp_storage;

    public ancSpawner(World world, Location location, int level, String ownerUUID, String type, String mode, boolean autokill, int storage_limit, boolean virtual_storage, boolean xp_storage) {
        this.world = world;
        this.location = location;
        this.level = level;
        this.ownerUUID = ownerUUID;
        this.type = type;
        this.mode = mode;
        this.autokill = autokill;
        this.storage = new ancStorage(world, location, this);
        if (storage_limit == 0) {
            storage_limit = Main.getPlugin().getConfig().getInt("config.modules.storage-limit.default");
        }
        this.storage_limit = storage_limit;
        this.friend_uuids = new ArrayList<>();
        this.virtual_storage = virtual_storage;
        this.xp_storage = xp_storage;
    }

    public void loadFriendsUUID(Connection connection, String locationData) {
        String query = "SELECT uuid FROM friends WHERE world = ? AND location = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setString(1, world.getName());
            preparedStatement.setString(2, locationData);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    friend_uuids.add(resultSet.getString("uuid"));
                }
            }
        } catch (SQLException sqlEx) {
            Main.getPlugin().getLogger().warning("Error loading friends for spawner: " + sqlEx.getMessage());
            sqlEx.printStackTrace();
        }
    }

    public CompletableFuture<Boolean> isBlockSpawner() {
        CompletableFuture<Boolean> future = new CompletableFuture<>();

        Bukkit.getScheduler().runTask(Main.getPlugin(), () -> {
            boolean isSpawner = getWorld().getBlockAt(getLocation()).getType().equals(XMaterial.SPAWNER.parseMaterial());
            future.complete(isSpawner);
        });

        return future;
    }


    public World getWorld() {
        return world;
    }

    public Location getLocation() {
        return location;
    }

    public int getLevel() {
        return level;
    }

    public String getOwnerUUID() {
        return ownerUUID;
    }

    public String getType() {
        return type;
    }

    public ancStorage getStorage() {
        return storage;
    }

    public String getMode() {
        return mode;
    }

    public boolean getAutoKill() {
        return autokill;
    }

    public int getStorageLimit() {
        return storage_limit;
    }

    public boolean isVirtualStorageEnabled() {
        return virtual_storage;
    }

    public boolean isXPStorageEnabled() {
        return xp_storage;
    }

    public void setVirtualStorage(boolean bool) {
        virtual_storage = bool;
        if (!Main.getPlugin().getSpawnerManager().updatedSpawnerList.contains(this)) {
            Main.getPlugin().getSpawnerManager().updatedSpawnerList.add(this);
        }
    }

    public void setXPStorage(boolean bool) {
        xp_storage = bool;
        if (!Main.getPlugin().getSpawnerManager().updatedSpawnerList.contains(this)) {
            Main.getPlugin().getSpawnerManager().updatedSpawnerList.add(this);
        }
    }

    public List<String> getFriendUuids() {
        return friend_uuids;
    }

    public boolean isFriend(String uuid) {
        return friend_uuids.contains(uuid);
    }

    public void setLevel(int level) {
        this.level = level;
        if (!Main.getPlugin().getSpawnerManager().updatedSpawnerList.contains(this)) {
            Main.getPlugin().getSpawnerManager().updatedSpawnerList.add(this);
        }

    }

    public void setAutoKill(boolean bool) {
        this.autokill = bool;
        if (!Main.getPlugin().getSpawnerManager().updatedSpawnerList.contains(this)) {
            Main.getPlugin().getSpawnerManager().updatedSpawnerList.add(this);
        }

    }

    public void setStorageLimit(int storage_limit) {
        this.storage_limit = storage_limit;
        if (!Main.getPlugin().getSpawnerManager().updatedSpawnerList.contains(this)) {
            Main.getPlugin().getSpawnerManager().updatedSpawnerList.add(this);
        }

    }

    public void addFriend(String uuid) {
        friend_uuids.add(uuid);
        if (!Main.getPlugin().getSpawnerManager().updatedSpawnerList.contains(this)) {
            Main.getPlugin().getSpawnerManager().updatedSpawnerList.add(this);
        }

    }

    public void removeFriend(String uuid) {
        friend_uuids.remove(uuid);
        if (!Main.getPlugin().getSpawnerManager().updatedSpawnerList.contains(this)) {
            Main.getPlugin().getSpawnerManager().updatedSpawnerList.add(this);
        }

    }
}

