package com.ancienty.ancspawnersrecoded.SpawnerManager;

import com.ancienty.ancspawnersrecoded.Database.DatabaseTask;
import com.ancienty.ancspawnersrecoded.Main;
import org.bukkit.Location;
import org.bukkit.World;

import javax.annotation.Nullable;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ancSpawner {

    private final World world;
    private final Location location;
    private int level;
    private final String ownerUUID;
    private final String type;
    private final String mode;
    private final String config_name;
    private boolean autokill;
    private final ancStorage storage;
    private int storage_limit;
    private final CopyOnWriteArrayList<String> friend_uuids;
    private boolean virtual_storage;
    private boolean xp_storage;
    private ancBoosts boosts;

    public ancSpawner(World world, Location location, @Nullable String config_name, int level, String ownerUUID, String type, String mode, boolean autokill, int storage_limit, boolean virtual_storage, boolean xp_storage) {
        this.world = world;
        this.location = location;
        this.level = level;
        this.ownerUUID = ownerUUID;
        this.type = type;
        this.mode = mode;
        this.autokill = autokill;
        this.storage = new ancStorage(world, location, this);
        this.config_name = config_name;
        if (storage_limit == 0) {
            storage_limit = Main.getPlugin().getStorageModule().getInt("storage-limit.default");
        }
        this.storage_limit = storage_limit;
        this.friend_uuids = new CopyOnWriteArrayList<>();
        this.virtual_storage = virtual_storage;
        this.xp_storage = xp_storage;
        this.boosts = new ancBoosts(world, location, this);

        loadFriendsUUID();
    }

    public void loadFriendsUUID() {
        String query = "SELECT uuid FROM friends WHERE world = ? AND location = ?";
        DatabaseTask loadFriendsTask = new DatabaseTask(query, new Object[]{
                world.getName(),
                Main.getPlugin().getSpawnerManager().getLocation(this)
        });

        Main.getPlugin().executeDatabaseQuery(loadFriendsTask, resultSet -> {
            try {
                while (resultSet.next()) {
                    friend_uuids.add(resultSet.getString("uuid"));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
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

    public String getConfigName() {
        return config_name;
    }

    /**
     * Gets the boosts associated with this spawner.
     */
    public ancBoosts getBoosts() {
        if (boosts == null) {
            boosts = new ancBoosts(this.world, this.location, this);
        }
        return boosts;
    }

    /**
     * Reloads the boosts for this spawner.
     */
    public void reloadBoosts() {
        if (boosts == null) {
            boosts = new ancBoosts(this.world, this.location, this);
        } else {
            boosts.loadActiveBoosts();
        }
    }

    public boolean isVirtualStorageEnabled() {
        return virtual_storage;
    }

    public boolean isXPStorageEnabled() {
        return xp_storage;
    }

    public void addToUpdatedSpawnersList() {
        if (!Main.getPlugin().getSpawnerManager().getUpdatedSpawners().contains(this)) {
            Main.getPlugin().getSpawnerManager().getUpdatedSpawners().add(this);
        }
    }

    public void setVirtualStorage(boolean bool) {
        virtual_storage = bool;
        addToUpdatedSpawnersList();
    }

    public void setXPStorage(boolean bool) {
        xp_storage = bool;
        addToUpdatedSpawnersList();
    }

    public List<String> getFriendUuids() {
        return friend_uuids;
    }

    public boolean isFriend(String uuid) {
        return friend_uuids.contains(uuid);
    }

    public void setLevel(int level) {
        this.level = level;
        addToUpdatedSpawnersList();
    }

    public void setAutoKill(boolean bool) {
        this.autokill = bool;
        addToUpdatedSpawnersList();
    }

    public void setStorageLimit(int storage_limit) {
        this.storage_limit = storage_limit;
        addToUpdatedSpawnersList();
    }

    public void addFriend(String uuid) {
        friend_uuids.add(uuid);
        addToUpdatedSpawnersList();
    }

    public void removeFriend(String uuid) {
        friend_uuids.remove(uuid);
        addToUpdatedSpawnersList();
    }
}

