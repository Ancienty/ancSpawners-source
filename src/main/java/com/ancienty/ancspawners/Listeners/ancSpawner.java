package com.ancienty.ancspawners.Listeners;

import com.ancienty.ancspawners.Main;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.concurrent.CompletableFuture;

public class ancSpawner {

    private final World world;
    private final Location location;
    private final String ownerUuid;
    private final String spawnerType;
    private final String spawnerMode;
    private int level;
    private boolean auto_kill;
    private ancStorage spawnerStorage;

    public ancSpawner(World world, Location location, String ownerUuid, String type, String mode, int level) {
        this.world = world;
        this.location = location;
        this.ownerUuid = ownerUuid;
        this.spawnerType = type;
        this.spawnerMode = mode;
        this.level = level;
        this.auto_kill = false;
    }

    public World getWorld() {
        return world;
    }

    public Location getLocation() {
        return location;
    }

    public String getOwnerUuid() {
        return ownerUuid;
    }

    public String getSpawnerType() {
        return spawnerType;
    }

    public String getSpawnerMode() {
        return spawnerMode;
    }

    public int getLevel() {
        return level;
    }

    public boolean getAutoKill() {
        return auto_kill;
    }

    public void setAutoKill(boolean bool) {
        this.auto_kill = bool;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public CompletableFuture<ancStorage> getSpawnerStorage() {
        if (spawnerStorage == null) {
            // Lazy initialization of spawner storage
            return CompletableFuture.runAsync(() -> {
                spawnerStorage = new ancStorage(world, location);
            }).thenApply(v -> spawnerStorage).exceptionally(ex -> {
                // Handle potential errors
                Main.getPlugin().getLogger().severe("Error loading spawner storage: " + ex.getMessage());
                return null;
            });
        } else {
            return CompletableFuture.completedFuture(spawnerStorage);
        }
    }
}
