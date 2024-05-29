package com.ancienty.ancspawners.Versions.SpawnerNBT;

import com.ancienty.ancspawners.Main;
import com.cryptomorin.xseries.XMaterial;
import de.tr7zw.nbtapi.NBT;
import de.tr7zw.nbtapi.iface.ReadWriteNBT;
import org.bukkit.Location;
import org.bukkit.block.Block;

public class SpawnerUpdate1_8 implements SpawnerUpdate {

    @Override
    public void spawnerUpdate(Block block, Location location, String entityType, String spawnerName, String mode, int delay, int playerRange) {

        if (mode.equalsIgnoreCase("entity")) {

            String finalEntityType1 = entityType;
            finalEntityType1 = finalEntityType1.toLowerCase();
            finalEntityType1 = finalEntityType1.substring(0, 1).toUpperCase() + finalEntityType1.substring(1);
            String finalEntityType = finalEntityType1;
            NBT.modify(block.getState(), nbt -> {
                    nbt.removeKey("SpawnPotentials");
                    nbt.setString("EntityId", finalEntityType);
                    nbt.removeKey("SpawnPotentials");
                    nbt.setInteger("SpawnCount", 1);
                    nbt.setInteger("SpawnRange", 2);
                    nbt.setInteger("RequiredPlayerRange", playerRange);
                    nbt.setInteger("Delay", delay * 20);
                    nbt.setInteger("MinSpawnDelay", delay * 20);
                    nbt.setInteger("MaxSpawnDelay", delay * 20);
                    nbt.removeKey("SpawnPotentials");

                });
        } else {
            try {

                entityType = entityType.toUpperCase();


                String finalEntityType = entityType;
                NBT.modify(block.getState(), nbt -> {
                    nbt.removeKey("SpawnPotentials");
                    ReadWriteNBT spawnData = nbt.getOrCreateCompound("SpawnData");

                    spawnData.setItemStack("Item", XMaterial.valueOf(finalEntityType).parseItem());
                    spawnData.setString("id", "minecraft:item");
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


            } catch (Exception ignored) {}
        }
    }
}
