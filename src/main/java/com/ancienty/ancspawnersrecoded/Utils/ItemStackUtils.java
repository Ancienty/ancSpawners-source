package com.ancienty.ancspawnersrecoded.Utils;

import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.*;
import java.util.Base64;

public class ItemStackUtils {

    public static String itemStackToBase64(ItemStack item) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream)) {
            dataOutput.writeObject(item);
        }
        return Base64.getEncoder().encodeToString(outputStream.toByteArray());
    }

    public static ItemStack itemStackFromBase64(String data) throws IOException, ClassNotFoundException {
        byte[] serializedObject = Base64.getDecoder().decode(data);
        try (BukkitObjectInputStream dataInput = new BukkitObjectInputStream(new ByteArrayInputStream(serializedObject))) {
            return (ItemStack) dataInput.readObject();
        }
    }
}

