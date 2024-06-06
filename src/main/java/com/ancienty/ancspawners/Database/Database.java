package com.ancienty.ancspawners.Database;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;

public interface Database {

    // Database information:
    // TABLES: spawners - friends - storage
    // spawners:
    // world
    // location
    // uuid
    // mode
    // type
    // level
    // auto-kill
    //
    // friends:
    // world
    // location
    // uuid
    //
    // storage:
    // world
    // location
    // item_name: amount
    // xp: amount

    ConcurrentLinkedQueue<DatabaseTask> operations_queue = new ConcurrentLinkedQueue<>();

    // Getters
    CompletableFuture<String> getSpawnerOwnerUuid(Block block) throws ExecutionException, InterruptedException;

    String getLocation(Block block);

    CompletableFuture<String> getSpawnerOwner(Block block);
    CompletableFuture<String> getSpawnerMode(Block block);
    CompletableFuture<String> getSpawnerType(Block block);
    CompletableFuture<Integer> getSpawnerLevel(Block block);
    String getHologramName(Block block);
    CompletableFuture<Integer> getSpawnerStoredByItem(Block block, Material material);
    CompletableFuture<Integer> getSpawnerTotalStored(Block block);
    CompletableFuture<Integer> getStoredXP(Block block);
    CompletableFuture<List<String>> getFriendsByUuid(Block block);
    CompletableFuture<Double> getSpawnerMoney(Player player, Block block);
    CompletableFuture<List<String>> getStoredItems(Block block);


    // Methods
    void placeSpawner(Player player, Block block, String mode, String type);

    CompletableFuture<Boolean> doesLocationHaveSpawner(World world, Location location);

    void deleteSpawner(World world, Location location);

    void breakSpawner(Player player, Block block);
    void enableAutoKill(Player player, Block block);
    void closeHikariCP();
    void updateStorage(Block block, Material material, int amount);
    void updateXP(Block block, int amount);
    void updateLevel(Block block, int level);
    // This is the old "updateStorage" from Main class.
    void checkStorage(Block block) throws ExecutionException, InterruptedException;

    void updateStorageLimit(Block block, int storage_limit);

    void toggleAutoKill(Block block);
    void addFriend(Block block, String uuid);
    void removeFriend(Block block, String uuid);
    CompletableFuture<Double> spawnerSellAllItems(Player player, Block block);


    // Checkers
    CompletableFuture<Boolean> isAutoKillEnabled(Block block);
    CompletableFuture<Boolean> spawnerHasOwner(Block block);
    CompletableFuture<Boolean> checkIfFriend(Block block, Player player);
    void checkSpawnerXPPermission(Block block, Player player);


    CompletableFuture<Integer> getStorageLimit(Block block);

    // Setters
    void setSpawnerLevelTo1(Player player, Block block);
    void reduceLevelBy1(Player player, Block block);

}
