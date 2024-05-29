package com.ancienty.ancspawners.Versions.SpawnerNBT;

import com.ancienty.ancspawners.Main;
import com.cryptomorin.xseries.XEntity;
import com.cryptomorin.xseries.XMaterial;
import de.tr7zw.nbtapi.NBT;
import de.tr7zw.nbtapi.iface.ReadWriteNBT;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.EntityType;

public class SpawnerUpdate1_20 implements SpawnerUpdate {


    @Override
    public void spawnerUpdate (Block blocks, Location location, String entityType, String spawnerName, String mode, int delay, int playerRange){
        if (mode.equalsIgnoreCase("entity")) {
            CreatureSpawner block = (CreatureSpawner) blocks.getState();
            EntityType spawnerType = EntityType.valueOf(Main.getPlugin().getConfig().getString("spawners." + spawnerName + ".spawnerInfo.material"));
            int spawnerDelay = Main.getPlugin().getConfig().getInt("spawners." + spawnerName + ".spawnerInfo.delay");
            int spawnerRange = Main.getPlugin().getConfig().getInt("spawners." + spawnerName + ".spawnerInfo.range");
            block.setSpawnedType(spawnerType);
            block.setDelay(spawnerDelay * 20);
            block.setSpawnCount(1);
            block.setSpawnRange(2);
            block.setMinSpawnDelay(spawnerDelay * 20);
            block.setMaxSpawnDelay(spawnerDelay * 20);
            block.setRequiredPlayerRange(spawnerRange);
            block.update();
        } else {

            entityType = entityType.toUpperCase();


            String finalEntityType = entityType;
            NBT.modify(blocks.getState(), nbt -> {
                nbt.removeKey("SpawnPotentials");
                ReadWriteNBT spawnData = nbt.getOrCreateCompound("SpawnData");
                ReadWriteNBT entityData = spawnData.getOrCreateCompound("entity");
                entityData.setItemStack("Item", XMaterial.valueOf(finalEntityType).parseItem());
                entityData.setString("id", "minecraft:item");
                nbt.removeKey("SpawnPotentials");
                nbt.setString("EntityId", "Item");
                nbt.setInteger("SpawnCount", 1);
                nbt.setInteger("SpawnRange", 2);
                nbt.setInteger("RequiredPlayerRange", playerRange);
                nbt.setInteger("Delay", delay * 20);
                nbt.setInteger("MinSpawnDelay", delay * 20);
                nbt.setInteger("MaxSpawnDelay", delay * 20);
                nbt.removeKey("SpawnPotentials");
            });

        }
    }
}
