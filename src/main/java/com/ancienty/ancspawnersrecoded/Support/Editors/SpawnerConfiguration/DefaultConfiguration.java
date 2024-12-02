package com.ancienty.ancspawnersrecoded.Support.Editors.SpawnerConfiguration;

import com.ancienty.ancspawnersrecoded.Main;
import com.cryptomorin.xseries.XEntityType;
import com.cryptomorin.xseries.XMaterial;
import de.tr7zw.nbtapi.NBT;
import de.tr7zw.nbtapi.iface.ReadWriteNBT;
import de.tr7zw.nbtapi.utils.MinecraftVersion;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.event.block.BlockPlaceEvent;

import javax.annotation.Nullable;
import java.util.Locale;

public class DefaultConfiguration {

    public void configureSpawner(BlockPlaceEvent e, @Nullable String spawner_config_name, String spawner_type, String spawner_mode) {
        CreatureSpawner block = (CreatureSpawner)e.getBlockPlaced().getState();
        if (MinecraftVersion.isAtLeastVersion(MinecraftVersion.MC1_12_R1) && spawner_mode.equalsIgnoreCase("entity")) {
            block.setDelay(70);
            block.setMinSpawnDelay(60);
            block.setMaxSpawnDelay(80);
            block.setRequiredPlayerRange(1);
            block.setSpawnedType(XEntityType.valueOf(spawner_type.toUpperCase(Locale.ENGLISH)).get());
            block.update();
        } else {
            if (MinecraftVersion.isAtLeastVersion(MinecraftVersion.MC1_20_R1)) {
                if (spawner_type.startsWith("IA-")) {
                    NBT.modify(block, (nbt) -> {
                        nbt.removeKey("SpawnPotentials");
                        ReadWriteNBT spawnData = nbt.getOrCreateCompound("SpawnData");
                        ReadWriteNBT entityData = spawnData.getOrCreateCompound("entity");
                        entityData.setItemStack("Item", XMaterial.TORCH.parseItem());
                        entityData.setString("id", "minecraft:item");
                        nbt.removeKey("SpawnPotentials");
                        nbt.setString("EntityId", "Item");
                        nbt.setInteger("SpawnCount", 1);
                        nbt.setInteger("SpawnRange", 2);
                        nbt.setInteger("RequiredPlayerRange", 1);
                        nbt.setInteger("Delay", 999);
                        nbt.setInteger("MinSpawnDelay", 999);
                        nbt.setInteger("MaxSpawnDelay", 999);
                        nbt.removeKey("SpawnPotentials");
                    });
                } else {
                    NBT.modify(block, (nbt) -> {
                        nbt.removeKey("SpawnPotentials");
                        ReadWriteNBT spawnData = nbt.getOrCreateCompound("SpawnData");
                        ReadWriteNBT entityData = spawnData.getOrCreateCompound("entity");
                        entityData.setItemStack("Item", XMaterial.valueOf(spawner_type.toUpperCase(Locale.ENGLISH)).parseItem());
                        entityData.setString("id", "minecraft:item");
                        nbt.removeKey("SpawnPotentials");
                        nbt.setString("EntityId", "Item");
                        nbt.setInteger("SpawnCount", 1);
                        nbt.setInteger("SpawnRange", 2);
                        nbt.setInteger("RequiredPlayerRange", 1);
                        nbt.setInteger("Delay", 999);
                        nbt.setInteger("MinSpawnDelay", 999);
                        nbt.setInteger("MaxSpawnDelay", 999);
                        nbt.removeKey("SpawnPotentials");
                    });
                }
            } else if (spawner_type.startsWith("IA-")) {
                NBT.modify(block, (nbt) -> {
                    nbt.removeKey("SpawnPotentials");
                    ReadWriteNBT spawnData = nbt.getOrCreateCompound("SpawnData");
                    spawnData.setItemStack("Item", XMaterial.TORCH.parseItem());
                    spawnData.setString("id", "minecraft:item");
                    nbt.removeKey("SpawnPotentials");
                    nbt.setString("EntityId", "Item");
                    nbt.setInteger("SpawnCount", 1);
                    nbt.setInteger("SpawnRange", 2);
                    nbt.setInteger("RequiredPlayerRange", 1);
                    nbt.setInteger("Delay", 999);
                    nbt.setInteger("MinSpawnDelay", 999);
                    nbt.setInteger("MaxSpawnDelay", 999);
                    nbt.removeKey("SpawnPotentials");
                });
            } else {
                NBT.modify(block, (nbt) -> {
                    nbt.removeKey("SpawnPotentials");
                    ReadWriteNBT spawnData = nbt.getOrCreateCompound("SpawnData");
                    spawnData.setItemStack("Item", XMaterial.valueOf(spawner_type.toUpperCase(Locale.ENGLISH)).parseItem());
                    spawnData.setString("id", "minecraft:item");
                    nbt.removeKey("SpawnPotentials");
                    nbt.setString("EntityId", "Item");
                    nbt.setInteger("SpawnCount", 1);
                    nbt.setInteger("SpawnRange", 2);
                    nbt.setInteger("RequiredPlayerRange", 1);
                    nbt.setInteger("Delay", 999);
                    nbt.setInteger("MinSpawnDelay", 999);
                    nbt.setInteger("MaxSpawnDelay", 999);
                    nbt.removeKey("SpawnPotentials");
                });
            }
        }
    }
}
