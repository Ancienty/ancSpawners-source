package com.ancienty.ancspawners;

import com.ancienty.ancspawners.Commands.SpawnerCommand;
import com.ancienty.ancspawners.Database.Database;
import com.ancienty.ancspawners.Database.DatabaseTask;
import com.ancienty.ancspawners.Database.SQLite;
import com.ancienty.ancspawners.GUIs.FriendsGUI;
import com.ancienty.ancspawners.Listeners.*;
import com.ancienty.ancspawners.Utils.LootTablesCreator;
import com.ancienty.ancspawners.Utils.Metrics;
import com.ancienty.ancspawners.Utils.UpdateChecker;
import com.ancienty.ancspawners.Utils.Utils;
import com.ancienty.ancspawners.Versions.Holograms.*;
import com.cryptomorin.xseries.XMaterial;
import net.brcdev.shopgui.ShopGuiPlusApi;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static com.ancienty.ancspawners.Database.Database.operations_queue;

public final class Main extends JavaPlugin implements Listener {

    public static Main plugin;

    public static Main getPlugin() {
        return plugin;
    }
    public YamlConfiguration lang;
    private static Economy econ = null;
    private static Permission perms = null;

    public boolean storageEnabled;
    public static Database database;
    public HashMap<Player, Block> player_block_map = new HashMap<>();

    @Override
    public void onEnable() {
        // Plugin startup logic
        getLogger().info("ancSpawners is being enabled.");
        if (!setupEconomy() ) {
            getLogger().severe("Dependency: Vault could not be found, disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        getLogger().info("Hooked into Vault.");
        setupPermissions();

        plugin = this;
        saveDefaultConfig();

        int pluginId = 20678; // <-- Replace with the id of your plugin!
        Metrics metrics = new Metrics(this, pluginId);

        // Licensing (DISABLED FOR SPIGOTMC)
        Utils utils = new Utils();
        utils.checkLicense();

        getLogger().info("Creating/reading data files.");
        // Creation of database:
        if (getConfig().getString("database.type").equalsIgnoreCase("SQLITE")) {
            database = new SQLite();
        }

        try {createLangFiles();} catch (IOException e) {throw new RuntimeException(e);}
        getLogger().info("Registering events.");
        getServer().getPluginManager().registerEvents(new SpawnerPlaceListener(), this);
        getServer().getPluginManager().registerEvents(new SpawnerBreakListener(), this);
        getServer().getPluginManager().registerEvents(new SpawnerSpawnListener(), this);
        getServer().getPluginManager().registerEvents(new SpawnerClickListener(), this);
        getServer().getPluginManager().registerEvents(new SpawnerGUIListener(), this);
        try {getServer().getPluginManager().registerEvents(new FriendsGUI(null, null), this);} catch (ExecutionException | InterruptedException e) {throw new RuntimeException(e);}
        getLogger().info("ancSpawners has been enabled.");

        // Spawner saving part.
        new SpawnerCommand();

        storageEnabled = lang.getString("menu.storage.gui").equalsIgnoreCase("true");

        // Update checker.
        new UpdateChecker().checkForUpdates();


        new Thread(() -> {
            while (true) {
                synchronized (operations_queue) {
                    while (operations_queue.isEmpty()) {
                        try {
                            operations_queue.wait();
                        } catch (InterruptedException ex) {
                            ex.printStackTrace();
                        }
                    }

                    DatabaseTask task = operations_queue.poll();
                    assert task != null;
                    String query = task.getQuery();
                    Object[] parameters = task.getParameters();
                    String select_parameters = null;
                    if (query.startsWith("SELECT")) {
                        select_parameters = task.getSelectParameter();
                    }

                    if (select_parameters == null) { // Meaning: The query is not a select operation.
                        try (Connection connection = SQLite.dataSource.getConnection();
                             PreparedStatement statement = connection.prepareStatement(query)) {

                            for (int i = 0; i < parameters.length; i++) {
                                statement.setObject(i + 1, parameters[i]);
                            }

                            int rowsAffected = statement.executeUpdate(); // Use executeUpdate

                        } catch (SQLException ex) {
                            ex.printStackTrace();
                        }
                    } else { // Meaning: The query is a select operation.
                        CompletableFuture<Object> result = new CompletableFuture<>();
                        if (select_parameters.startsWith("LIST")) { // Meaning: The return element needs to be a List<String>
                            select_parameters = select_parameters.replace("LIST", "");
                            List<String> return_this = new ArrayList<>();
                            try (Connection connection = SQLite.dataSource.getConnection();
                                 PreparedStatement statement = connection.prepareStatement(query)) {

                                for (int i = 0; i < parameters.length; i++) {
                                    statement.setObject(i + 1, parameters[i]);
                                }

                                try (ResultSet set = statement.executeQuery()) {
                                    while (set.next()) {
                                        return_this.add(set.getString(select_parameters));
                                    }
                                    if (return_this.isEmpty()) {
                                        result.complete(Collections.emptyList());
                                    } else {
                                        result.complete(return_this);
                                    }
                                    task.setReturnElement(result);
                                }

                            } catch (SQLException ex) {
                                ex.printStackTrace();
                            }
                        } else if (select_parameters.startsWith("TOTAL")) { // Meaning: You should use while set.next() and get all the values.
                            select_parameters = select_parameters.replace("TOTAL", "");
                            int return_this = 0;
                            try (Connection connection = SQLite.dataSource.getConnection();
                                 PreparedStatement statement = connection.prepareStatement("SELECT * FROM storage WHERE world = ? AND location = ?")) {

                                for (int i = 0; i < parameters.length; i++) {
                                    statement.setObject(i + 1, parameters[i]);
                                }

                                try (ResultSet set = statement.executeQuery()) {
                                    while (set.next()) {
                                        return_this += set.getInt("amount");
                                    }
                                    result.complete(return_this);
                                    if (!result.isDone()) {
                                        result.complete(0);
                                    }
                                    task.setReturnElement(result);
                                }

                            } catch (SQLException ex) {
                                ex.printStackTrace();
                            }
                        } else if (select_parameters.startsWith("STORAGE_LIMIT")) {
                            select_parameters = select_parameters.replace("STORAGE_LIMIT", "");
                            try (Connection connection = SQLite.dataSource.getConnection();
                                 PreparedStatement statement = connection.prepareStatement(query)) {

                                for (int i = 0; i < parameters.length; i++) {
                                    statement.setObject(i + 1, parameters[i]);
                                }

                                try (ResultSet set = statement.executeQuery()) {
                                    if (set.next()) {
                                        result.complete(Main.getPlugin().getConfig().getString("config.modules.storage-limit.enabled").equalsIgnoreCase("true") ? set.getInt("storage_limit") : 0);
                                    } else {
                                        result.complete(0);
                                    }
                                    if (!result.isDone()) {
                                        result.complete(null);
                                    }
                                    task.setReturnElement(result);
                                }


                            } catch (SQLException ex) {
                                ex.printStackTrace();
                            }
                        } else if (select_parameters.startsWith("SPAWNER_MONEY")) {
                            select_parameters = select_parameters.replace("SPAWNER_MONEY", "");
                            try (Connection connection = SQLite.dataSource.getConnection();
                                 PreparedStatement statement = connection.prepareStatement(query)) {

                                for (int i = 0; i < parameters.length; i++) {
                                    statement.setObject(i + 1, parameters[i]);
                                }

                                try (ResultSet set = statement.executeQuery()) {
                                    HashMap<String, Integer> items_list = new HashMap<>();
                                    while (set.next()) {
                                        items_list.put(set.getString("item"), set.getInt("amount"));
                                    }

                                    AtomicReference<BigDecimal> total = new AtomicReference<>(BigDecimal.ZERO); // Initialize with BigDecimal
                                    String finalSelect_parameters1 = select_parameters;
                                    items_list.forEach((item, amount) -> {
                                        double price = Main.getPlugin().getPriceForItem(Bukkit.getPlayer(UUID.fromString(finalSelect_parameters1.split("--")[1])), item);;
                                        BigDecimal itemTotal = BigDecimal.valueOf(price * amount);
                                        total.updateAndGet(v -> v.add(itemTotal)); // Update with BigDecimal
                                    });

                                    BigDecimal playerMultiplier = BigDecimal.valueOf(Main.getPlugin().getPlayerMultiplier(Bukkit.getPlayer(UUID.fromString(select_parameters.split("--")[1]))));
                                    BigDecimal roundedTotal = total.get().multiply(playerMultiplier).setScale(2, RoundingMode.HALF_UP);

                                    result.complete(roundedTotal.doubleValue()); // Return as double after rounding
                                    task.setReturnElement(result);
                                }

                            } catch (SQLException ex) {
                                ex.printStackTrace();
                            }
                        } else if (select_parameters.startsWith("SPAWNER_SELL_ITEMS")) {
                            select_parameters = select_parameters.replace("SPAWNER_SELL_ITEMS", "");
                            try (Connection connection = SQLite.dataSource.getConnection();
                                 PreparedStatement statement = connection.prepareStatement(query)) {

                                for (int i = 0; i < parameters.length; i++) {
                                    statement.setObject(i + 1, parameters[i]);
                                }

                                try (ResultSet set = statement.executeQuery()) {
                                    HashMap<String, Integer> itemsList = new HashMap<>();
                                    while (set.next()) {
                                        itemsList.put(set.getString("item"), set.getInt("amount"));
                                    }

                                    AtomicReference<BigDecimal> total = new AtomicReference<>(BigDecimal.ZERO);
                                    AtomicInteger totalSold = new AtomicInteger();

                                    String finalSelect_parameters = select_parameters;
                                    itemsList.forEach((item, amount) -> {
                                        double price = Main.getPlugin().getPriceForItem(Bukkit.getPlayer(UUID.fromString(finalSelect_parameters.split("---")[1])), item);
                                        BigDecimal itemTotal = BigDecimal.valueOf(price * amount);
                                        total.updateAndGet(v -> v.add(itemTotal));
                                        totalSold.addAndGet(amount);
                                    });

                                    BigDecimal playerMultiplier = BigDecimal.valueOf(Main.getPlugin().getPlayerMultiplier(Bukkit.getPlayer(UUID.fromString(select_parameters.split("---")[1]))));
                                    BigDecimal roundedTotal = total.get().multiply(playerMultiplier).setScale(2, RoundingMode.HALF_UP);

                                    Main.getPlugin().sendMessage(Bukkit.getPlayer(UUID.fromString(select_parameters.split("---")[1])), "soldItems", new String[]{String.valueOf(totalSold), String.valueOf(roundedTotal), String.valueOf(playerMultiplier)}); // Original functionality
                                    Main.getEconomy().depositPlayer(Bukkit.getPlayer(UUID.fromString(select_parameters.split("---")[1])), roundedTotal.doubleValue()); // Using roundedTotal for accuracy

                                    double double_value = roundedTotal.doubleValue();
                                    CompletableFuture<Double> return_this = new CompletableFuture<>();
                                    return_this.complete(double_value);
                                    if (!result.isDone()) {
                                        result.complete(null);
                                    }
                                    if (getConfig().getBoolean("config.modules.title-messages.money-title")) {
                                        Player player = Bukkit.getPlayer(UUID.fromString(select_parameters.split("---")[1]));
                                        if (player.isOnline()) {
                                            String title = ChatColor.translateAlternateColorCodes('&', Main.getPlugin().lang.getString("lang.sellItemsTitle"));
                                            String sub_title = ChatColor.translateAlternateColorCodes('&', Main.getPlugin().lang.getString("lang.sellItemsSubtitle"));
                                            title = title.replace("{money_gained}", String.valueOf(double_value));
                                            sub_title = sub_title.replace("{money_gained}", String.valueOf(double_value));
                                            player.sendTitle(title, sub_title, 5, 30, 5);
                                        }
                                    }
                                    task.setReturnElement(result);
                                }

                            } catch (SQLException ex) {
                                ex.printStackTrace();
                            }
                        } else {
                            try (Connection connection = SQLite.dataSource.getConnection();
                                 PreparedStatement statement = connection.prepareStatement(query)) {

                                for (int i = 0; i < parameters.length; i++) {
                                    statement.setObject(i + 1, parameters[i]);
                                }

                                try (ResultSet set = statement.executeQuery()) {
                                    if (set.next()) {
                                        if (task.is_boolean()) {
                                            if (select_parameters.equalsIgnoreCase("autokill")) {
                                                String boolean_in_database = set.getString("autokill");
                                                if (boolean_in_database.equalsIgnoreCase("true") || boolean_in_database.equalsIgnoreCase(String.valueOf(1))) {
                                                    result.complete(true);
                                                } else {
                                                    result.complete(false);
                                                }
                                            } else {
                                                result.complete(true);
                                            }
                                        } else {
                                            result.complete(set.getObject(select_parameters));
                                        }
                                    } else {
                                        if (task.is_boolean()) {
                                            result.complete(false);
                                        } else {
                                            result.complete(null);
                                        }
                                    }
                                    if (!result.isDone()) {
                                        result.complete(null);
                                    }
                                    task.setReturnElement(result);
                                }

                            } catch (SQLException ex) {
                                ex.printStackTrace();
                            }
                        }
                    }
                }
            }
        }).start();

    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        getLogger().warning("Shutdown detected, initializing force-save.");
        database.closeHikariCP();
    }

    public void createLangFiles() throws IOException {
        File langEN = new File(getDataFolder(), "/lang/lang_en.yml");
        if (!langEN.exists()) {
            plugin.saveResource("lang/lang_en.yml", true);
        }
        File langTR = new File(getDataFolder(), "/lang/lang_tr.yml");
        if (!langTR.exists()) {
            plugin.saveResource("lang/lang_tr.yml", true);
        }

        String configLang = getConfig().getString("config.lang");
        if (configLang != null) {
            if (configLang.equalsIgnoreCase("tr")) {
                lang = YamlConfiguration.loadConfiguration(langTR);
            } else if (configLang.equalsIgnoreCase("en")) {
                lang = YamlConfiguration.loadConfiguration(langEN);
            }
        }

        new LootTablesCreator().createLootTables();
    }

    public void sendMessage(Player player, String langKey, String[] variables) {
        String prefix = lang.getString("lang.prefix");
        String message = lang.getString("lang." + langKey);

        if (variables.length > 0) {
            for (int i = 0; i < 5; i++) {
                if (message.contains("{" + i + "}")) {
                    message = message.replace("{" + i + "}", variables[i]);
                }
            }
        }

        message = ChatColor.translateAlternateColorCodes('&', prefix + message);
        player.sendMessage(message);
    }

    public List<ItemStack> getLootTable(String entityName) {
        File loottable = new File(getDataFolder(), File.separator + "loottables" + File.separator + entityName.toLowerCase(Locale.ENGLISH) + ".yml");
        if (loottable.exists()) {
            List<ItemStack> returnList = new ArrayList<>();
            Random random = new Random();
            YamlConfiguration yamlConfiguration = YamlConfiguration.loadConfiguration(loottable);
            yamlConfiguration.getStringList("drops.itemDrops").forEach(listItem -> {
                String[] splitted = listItem.split(":");
                splitted[1] = splitted[1].replace(":", "");
                int chance = Integer.parseInt(splitted[1]);
                if (random.nextInt(100) <= chance) {
                    splitted[0] = splitted[0].replace(":", "");
                    ItemStack itemStack = XMaterial.valueOf(splitted[0]).parseItem();
                    splitted[2] = splitted[2].replace(":", "");
                    String[] amountSplitted = splitted[2].split("-");
                    amountSplitted[0] = amountSplitted[0].replace("-", "");
                    amountSplitted[1] = amountSplitted[1].replace("-", "");

                    int amountDiff = Integer.parseInt(amountSplitted[1]) - Integer.parseInt(amountSplitted[0]);
                    int amountRandom = random.nextInt(amountDiff + 1);
                    int amountToGive = amountRandom + Integer.parseInt(amountSplitted[0]);
                    assert itemStack != null;
                    itemStack.setAmount(amountToGive);
                    returnList.add(itemStack);

                }
            });
            return returnList;
        }
        return null;
    }

    public Integer getLootTableXP(String entityName) {
        File loottable = new File(getDataFolder(), File.separator + "loottables" + File.separator + entityName.toLowerCase(Locale.ENGLISH) + ".yml");
        if (loottable.exists()) {
            YamlConfiguration yamlConfiguration = YamlConfiguration.loadConfiguration(loottable);
            Integer returnInt = null;
            Random random = new Random();
            int xpChance = yamlConfiguration.getInt("drops.xpDropChance");
            String xpAmountRange = yamlConfiguration.getString("drops.xpDrops");
            if (xpAmountRange.equalsIgnoreCase("0-0")) {
                return 0;
            }
            String[] splitted = xpAmountRange.split("-");
            splitted[0] = splitted[0].replace("-", "");
            splitted[1] = splitted[1].replace("-", "");
            if (random.nextInt(100) <= xpChance) {
                int xpDiff = Integer.parseInt(splitted[1]) - Integer.parseInt(splitted[0]);
                int minXp = random.nextInt(xpDiff + 1);
                returnInt = minXp + Integer.parseInt(splitted[0]);
            }
            return returnInt;
        }
        return null;
    }


    public void spawnerLevelUp(Player player, Block block) throws ExecutionException, InterruptedException {
        String playerUUID = player.getUniqueId().toString();
        database.getSpawnerOwnerUuid(block).thenAccept(ownerUUID -> {
            database.getSpawnerLevel(block).thenAccept(currentLevel -> {
                if (ownerUUID.equalsIgnoreCase(playerUUID)) {
                    database.getSpawnerType(block).thenAccept(type -> {
                        ItemStack spawner = getSpawner(type);
                        assert spawner != null;
                        spawner.setAmount(1);
                        int levelToAdd;
                        ItemStack[] itemList = player.getInventory().getContents();
                        // itemStack.setAmount(0);
                        levelToAdd = Arrays.stream(itemList).filter(itemStack -> itemStack != null && itemStack.hasItemMeta()).filter(itemStack -> itemStack.getItemMeta().equals(spawner.getItemMeta())).mapToInt(ItemStack::getAmount).sum();

                        ItemStack spawnerToGiveBack = spawner.clone();
                        int maximumSpawnerLevel = getConfig().getInt("config.modules.spawner-level-limit.level-limit");

                        // 253 - 10 - 3
                        int maximumToLevelUp = maximumSpawnerLevel - currentLevel;
                        int removeAmount;
                        if (maximumToLevelUp > 0) {
                            removeAmount = Math.min(levelToAdd, maximumToLevelUp);
                            ItemStack spawnerToRemove = spawner.clone();
                            spawnerToRemove.setAmount(removeAmount);

                            try {
                                player.getInventory().removeItem(spawnerToRemove);
                            } catch (IllegalArgumentException ignored) {
                            }
                            player.updateInventory();

                            if (levelToAdd > 0) {
                                int new_level = currentLevel + removeAmount;
                                database.updateLevel(block, new_level);
                                sendMessage(player, "levelUpSuccessful", new String[]{String.valueOf(removeAmount)});
                                SpawnerHologram_General.updateSpawnerHologram(block, database.getHologramName(block));
                            } else {
                                sendMessage(player, "noSpawnersFound", new String[]{});
                            }

                        } else {
                            sendMessage(player, "spawnerMaxLevel", new String[]{});
                        }
                    });
                } else {
                    sendMessage(player, "notTheOwner", new String[]{});
                }
            });
        });
    }

    public ItemStack getSpawnerByEntity(String entityName) {
        String spawnerNameInConfig = null;
        for (String spawnerNameForLoop : getConfig().getConfigurationSection("spawners").getKeys(false)) {
            if (getConfig().getString("spawners." + spawnerNameForLoop + ".spawnerInfo.material").equalsIgnoreCase(entityName)) {
                spawnerNameInConfig = spawnerNameForLoop;
            }
        }

        if (spawnerNameInConfig != null) {
            return getSpawner(spawnerNameInConfig);
        } else {
            return null;
        }
    }

    public ItemStack getSpawner(String type) {
        String spawnerConfig = null;
        ItemStack spawner = new ItemStack(XMaterial.SPAWNER.parseMaterial());
        ItemMeta spawnerMeta = spawner.getItemMeta();

        for (String spawnerNameInConfig : getConfig().getConfigurationSection("spawners").getKeys(false)) {
            if (spawnerNameInConfig.equalsIgnoreCase(type)) {
                spawnerConfig = spawnerNameInConfig;
            }
        }

        if (spawnerConfig != null) {

            String spawnerName = getConfig().getString("spawners." + spawnerConfig + ".name");
            List<String> spawnerLore = getConfig().getStringList("spawners." + spawnerConfig + ".lore");
            List<String> spawnerLoreReal = new ArrayList<>();

            for (String text : spawnerLore) {
                spawnerLoreReal.add(ChatColor.translateAlternateColorCodes('&', text));
            }

            spawnerMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', spawnerName));
            spawnerMeta.setLore(spawnerLoreReal);
            spawnerMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);

            spawner.setItemMeta(spawnerMeta);
            return spawner;
        } else {
            return null;
        }
    }

    public static ItemStack getHead(String value) {
        ItemStack skull = new ItemStack(XMaterial.PLAYER_HEAD.parseMaterial());
        UUID hashAsId = new UUID(value.hashCode(), value.hashCode());
        return Bukkit.getUnsafe().modifyItemStack(skull,
                "{SkullOwner:{Id:\"" + hashAsId + "\",Properties:{textures:[{Value:\"" + value + "\"}]}}}"
        );
    }

    public void spawnerGiveXP(Player player, Block block) throws ExecutionException, InterruptedException {
        if (getConfig().getString("config.modules.permission-based-xp.enabled").equalsIgnoreCase("true")) {
            database.getSpawnerOwnerUuid(block).thenAccept(owneruuid -> {
               if (owneruuid.equalsIgnoreCase(player.getUniqueId().toString())) {
                   database.getStoredXP(block).thenAccept(xp -> {
                      if (xp != 0) {
                          player.giveExp(xp);
                          database.updateXP(block, 0);
                          sendMessage(player, "tookXP", new String[]{String.valueOf(xp)});
                          if (getConfig().getBoolean("config.modules.title-messages.xp-title")) {
                              if (player.isOnline()) {
                                  String title = ChatColor.translateAlternateColorCodes('&', Main.getPlugin().lang.getString("lang.takeXpTitle"));
                                  String sub_title = ChatColor.translateAlternateColorCodes('&', Main.getPlugin().lang.getString("lang.takeXpSubtitle"));
                                  title = title.replace("{xp_gained}", String.valueOf(xp));
                                  sub_title = sub_title.replace("{xp_gained}", String.valueOf(xp));
                                  player.sendTitle(title, sub_title, 5, 30, 5);
                              }
                          }
                      } else {
                          sendMessage(player, "noXP", new String[]{});
                      }
                   });
               }
            });
        } else {
            database.getStoredXP(block).thenAccept(xp -> {
                if (xp != 0) {
                    player.giveExp(xp);
                    database.updateXP(block, 0);
                    sendMessage(player, "tookXP", new String[]{String.valueOf(xp)});
                    if (getConfig().getBoolean("config.modules.title-messages.xp-title")) {
                        if (player.isOnline()) {
                            String title = ChatColor.translateAlternateColorCodes('&', Main.getPlugin().lang.getString("lang.takeXpTitle"));
                            String sub_title = ChatColor.translateAlternateColorCodes('&', Main.getPlugin().lang.getString("lang.takeXpSubtitle"));
                            title = title.replace("{xp_gained}", String.valueOf(xp));
                            sub_title = sub_title.replace("{xp_gained}", String.valueOf(xp));
                            player.sendTitle(title, sub_title, 5, 30, 5);
                        }
                    }
                } else {
                    sendMessage(player, "noXP", new String[]{});
                }
            });
        }
    }

    public void spawnerGetItem(Player player, Block block, String material, int amount) {
        database.getSpawnerStoredByItem(block, XMaterial.valueOf(material).parseMaterial()).thenAccept(itemAmountInData -> {
           int amountToGive;
            if (itemAmountInData >= amount) {
                amountToGive = amount;
            } else {
                amountToGive = itemAmountInData;
            }

            if (amountToGive > 0) {

                ItemStack itemToGive = new ItemStack(Material.valueOf(material));
                itemToGive.setAmount(amountToGive);
                HashMap<Integer, ItemStack> itemsFailedToAdd = player.getInventory().addItem(itemToGive);
                if (!itemsFailedToAdd.isEmpty()) {
                    ItemStack item1 = itemsFailedToAdd.get(0);
                    int item1amount = item1.getAmount();

                    amountToGive -= item1amount;
                }

                itemAmountInData -= amountToGive;
                sendMessage(player, "tookItems", new String[]{String.valueOf(amountToGive), String.valueOf(itemToGive.getType())});

                database.updateStorage(block, XMaterial.valueOf(material).parseMaterial(), itemAmountInData);
            } else {
                sendMessage(player, "noItems", new String[]{});
            }
        });
    }


    public double getPriceForItem(Player player, String item) {
        String priceProvider = getConfig().getString("itemPrices.priceProvider");
        double price = 0;
        if (priceProvider.equalsIgnoreCase("custom")) {
            price = Main.getPlugin().getConfig().getDouble("itemPrices." + item);
        } else if (priceProvider.equalsIgnoreCase("shopguiplus")) {
            ItemStack itemStack = XMaterial.valueOf(item).parseItem();
            price = ShopGuiPlusApi.getItemStackPriceSell(itemStack);
        }
        return price;
    }

    public double getPlayerMultiplier(Player player) {
        double multiplier = 1.0;

        for (String permission : this.getConfig().getConfigurationSection("config.modules.multipliers.permissions").getKeys(false)) {
            if (getPermissions().has(player, permission)) {
                multiplier = this.getConfig().getDouble("config.modules.multipliers.permissions." + permission);
            }
        }
        return multiplier;
    }

    public CompletableFuture<String> getStorageBar(Block block) {
        CompletableFuture<String> returnThis = new CompletableFuture<>();

        // Retrieve storage limit and stored items in parallel
        CompletableFuture<Integer> storageLimitFuture = Main.database.getStorageLimit(block);
        CompletableFuture<List<String>> storedItemsFuture = Main.database.getStoredItems(block);

        // Combine the results of the parallel operations
        CompletableFuture<Void> combinedFuture = CompletableFuture.allOf(storageLimitFuture, storedItemsFuture);

        combinedFuture.thenAcceptAsync(voidResult -> {
            try {
                int storageLimit = storageLimitFuture.get();
                List<String> storedItems = storedItemsFuture.get();

                if (storedItems.isEmpty()) {
                    // If no items are stored, return an empty storage bar
                    String returnText = "&f☰☰☰☰☰☰☰☰☰☰";
                    returnText = ChatColor.translateAlternateColorCodes('&', returnText);
                    returnThis.complete(returnText);
                    return;
                }

                int totalStorageLimit = storageLimit * storedItems.size();
                AtomicInteger totalStoredItems = new AtomicInteger();

                // Use another CompletableFuture to handle the nested database calls for each item
                List<CompletableFuture<Void>> itemFutures = new ArrayList<>();
                for (String item : storedItems) {
                    CompletableFuture<Void> itemFuture = Main.database.getSpawnerStoredByItem(block, XMaterial.valueOf(item).parseMaterial())
                            .thenAccept(totalStoredItems::addAndGet);
                    itemFutures.add(itemFuture);
                }

                // Wait for all item futures to complete
                CompletableFuture<Void> allItemsFuture = CompletableFuture.allOf(itemFutures.toArray(new CompletableFuture[0]));
                allItemsFuture.thenAcceptAsync(itemVoidResult -> {
                    double division = (double) totalStoredItems.get() / totalStorageLimit;
                    int amountOfBars = (int) Math.floor((division * 10));
                    int remainderBars = 10 - amountOfBars;
                    StringBuilder builder = new StringBuilder();

                    for (int i = 0; i < amountOfBars; i++) {
                        builder.append("&a☰");
                    }
                    for (int i = 0; i < remainderBars; i++) {
                        builder.append("&f☰");
                    }
                    String returnText = builder.toString();

                    if (amountOfBars == 10) {
                        returnText = returnText.replace("&a", "&4");
                    } else if (amountOfBars > 8) {
                        returnText = returnText.replace("&a", "&c");
                    } else if (amountOfBars > 6) {
                        returnText = returnText.replace("&a", "&6");
                    } else if (amountOfBars > 4) {
                        returnText = returnText.replace("&a", "&e");
                    }

                    returnText = ChatColor.translateAlternateColorCodes('&', returnText);
                    returnThis.complete(returnText);
                });
            } catch (Exception e) {
                returnThis.completeExceptionally(e);
            }
        });

        return returnThis;
    }



    public void createSpawnerHologram(Player player, Block block, World world, Location location) {
        SpawnerHologram spawnerHologram;

        spawnerHologram = new SpawnerHologram_General();
        spawnerHologram.createHologram(player, block);
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        econ = rsp.getProvider();
        return econ != null;
    }

    private boolean setupPermissions() {
        RegisteredServiceProvider<Permission> rsp = getServer().getServicesManager().getRegistration(Permission.class);
        perms = rsp.getProvider();
        return perms != null;
    }

    public static Economy getEconomy() {
        return econ;
    }

    public static Permission getPermissions() {
        return perms;
    }
}
