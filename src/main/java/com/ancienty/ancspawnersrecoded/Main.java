package com.ancienty.ancspawnersrecoded;

import com.ancienty.ancspawnersrecoded.Commands.SpawnerCommand;
import com.ancienty.ancspawnersrecoded.Database.SQLProcessing;
import com.ancienty.ancspawnersrecoded.Database.SQLite;
import com.ancienty.ancspawnersrecoded.GUIs.Listeners.SpawnerClickListener;
import com.ancienty.ancspawnersrecoded.GUIs.Listeners.SpawnerGUIListener;
import com.ancienty.ancspawnersrecoded.Listeners.SpawnerBoostListener;
import com.ancienty.ancspawnersrecoded.Listeners.SpawnerBreakListener;
import com.ancienty.ancspawnersrecoded.Listeners.SpawnerPlaceListener;
import com.ancienty.ancspawnersrecoded.Listeners.SpawnerSpawnListener;
import com.ancienty.ancspawnersrecoded.SpawnerManager.DeathListener;
import com.ancienty.ancspawnersrecoded.SpawnerManager.SpawnerManager;
import com.ancienty.ancspawnersrecoded.SpawnerManager.ancSpawner;
import com.ancienty.ancspawnersrecoded.SpawnerManager.ancStorage;
import com.ancienty.ancspawnersrecoded.Support.Editors.HologramEditor;
import com.ancienty.ancspawnersrecoded.Utils.*;
import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.profiles.builder.XSkull;
import com.cryptomorin.xseries.profiles.objects.Profileable;
import de.tr7zw.nbtapi.NBT;
import de.tr7zw.nbtapi.iface.ReadableNBT;
import net.brcdev.shopgui.ShopGuiPlusApi;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

public final class Main extends JavaPlugin {

    private static Main plugin;
    private static SQLite sqLite;
    private static HologramEditor hologramEditor;
    private SpawnerManager spawnerManager;

    public YamlConfiguration modules_autokill;
    public YamlConfiguration modules_friends;
    public YamlConfiguration modules_hologram;
    public YamlConfiguration modules_multipliers;
    public YamlConfiguration modules_settings;
    public YamlConfiguration modules_other;
    public YamlConfiguration modules_storage_limits;
    public YamlConfiguration modules_boost;

    private static Economy econ = null;
    private static Permission perms = null;
    private static SQLProcessing sqlProcessing;

    private ancLogger ancLogger;
    public YamlConfiguration lang;
    private boolean license_invalid;
    public HashMap<Player, Block> player_block_map = new HashMap<>();
    private static final Map<String, ItemStack> HEAD_CACHE = new ConcurrentHashMap<>();

    @Override
    public void onEnable() {

        getLogger().info("Enabling ancSpawners-recoded.");
        plugin = this;
        saveDefaultConfig();

        if (!setupEconomy() ) {
            getLogger().severe("Dependency: Vault could not be found, disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        } if (getServer().getPluginManager().getPlugin("NBTAPI") == null || !getServer().getPluginManager().getPlugin("NBTAPI").isEnabled()) {
            getLogger().severe("Dependency: NBT-API could not be found, disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        setupPermissions();
        getLogger().info("Found dependencies: Vault & NBTAPI");

        int pluginId = 20678; // <-- Replace with the id of your plugin!
        Metrics metrics = new Metrics(this, pluginId);

        getLogger().info("Reading database & setting up spawnerManager.");

        sqlProcessing = new SQLProcessing();
        sqLite = new SQLite();
        spawnerManager = new SpawnerManager();

        // Licensing (Disabled for SPIGOTMC)
        /*Utils utils = new Utils();
        if (!utils.checkLicense()) {
            license_invalid = true;
            return;
        }*/

        getLogger().info("Reading lang files & modules.");

        try {createLangFiles();} catch (IOException e) {throw new RuntimeException(e);}
        ancLogger = new ancLogger();
        ancLogger.createLogFile();


        loadConfig("autokill");
        loadConfig("friends");
        loadConfig("hologram");
        loadConfig("multipliers");
        loadConfig("other");
        loadConfig("settings");
        loadConfig("storage_limits");
        loadConfig("boosts");

        if (getServer().getPluginManager().getPlugin("DecentHolograms") != null && getServer().getPluginManager().getPlugin("DecentHolograms").isEnabled()) {
            if (getHologramModule().getBoolean("hologram.enabled")) {
                hologramEditor = new HologramEditor();
                getLogger().info("Found dependency: DecentHolograms");
            }
        }

        getLogger().info("Registering events.");

        getServer().getPluginManager().registerEvents(new SpawnerClickListener(), this);
        getServer().getPluginManager().registerEvents(new SpawnerGUIListener(), this);
        getServer().getPluginManager().registerEvents(new SpawnerBreakListener(), this);
        getServer().getPluginManager().registerEvents(new SpawnerPlaceListener(), this);
        getServer().getPluginManager().registerEvents(new SpawnerSpawnListener(), this);
        getServer().getPluginManager().registerEvents(new SpawnerBoostListener(), this);
        getServer().getPluginManager().registerEvents(new DeathListener(), this);
        getServer().getPluginManager().registerEvents(new UpdateChecker(), this);

        getLogger().info("Creating commands.");

        new SpawnerCommand();

        getLogger().info("ancSpawners-recoded is now enabled!");
    }

    @Override
    public void onDisable() {
        if (!license_invalid) {
            getLogger().warning("Shutdown detected, initializing force-save.");
            getSpawnerManager().saveNow(getLogger());
            sqlProcessing.processQueueSynchronously();
        }
        Bukkit.getScheduler().cancelTasks(this);
    }

    public SQLProcessing getSqlProcessing() {
        return sqlProcessing;
    }

    public ancLogger getAncLogger() {
        return ancLogger;
    }

    public static Main getPlugin() {
        return plugin;
    }

    public static SQLite getDatabase() {
        return sqLite;
    }

    public SpawnerManager getSpawnerManager() {
        return spawnerManager;
    }

    public void spawnerLevelUp(Player player, Block block) {
        String playerUUID = player.getUniqueId().toString();
        ancSpawner ancSpawner = spawnerManager.getSpawner(block.getWorld(), block.getLocation());

        String ownerUUID = ancSpawner.getOwnerUUID();
        int currentLevel = ancSpawner.getLevel();
        if (ownerUUID.equalsIgnoreCase(playerUUID)) {
            ItemStack spawner = getSpawnerFromSpawnerBlock(ancSpawner);
            assert spawner != null;
            spawner.setAmount(1);
            int levelToAdd;
            ItemStack[] itemList = player.getInventory().getContents();
            // itemStack.setAmount(0);
            levelToAdd = Arrays.stream(itemList).filter(itemStack -> itemStack != null && itemStack.hasItemMeta()).filter(itemStack -> itemStack.getItemMeta().equals(spawner.getItemMeta())).mapToInt(ItemStack::getAmount).sum();

            ItemStack spawnerToGiveBack = spawner.clone();
            int maximumSpawnerLevel = getOtherModule().getInt("spawner-level-limit.level-limit");

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
                    ancSpawner.setLevel(new_level);
                    sendMessage(player, "levelUpSuccessful", new String[]{String.valueOf(removeAmount)});
                    updateSpawnerHologram(player, block, true);
                } else {
                    sendMessage(player, "noSpawnersFound", new String[]{});
                }

            } else {
                sendMessage(player, "spawnerMaxLevel", new String[]{});
            }
        } else {
            sendMessage(player, "notTheOwner", new String[]{});
        }
    }

    public String getStorageBarOfItem(ItemStack item, ancSpawner spawner) {
        int storageLimit = spawner.getStorageLimit();
        int storedItemAmount = spawner.getStorage().getStoredItem(item);
        StringBuilder returnText = new StringBuilder();

        if (storedItemAmount == 0) {
            returnText.append("&f☰☰☰☰☰☰☰☰☰☰");
            return ChatColor.translateAlternateColorCodes('&', returnText.toString());
        } else {
            double division = (double) storedItemAmount / storageLimit;
            int amountOfBars = (int) Math.floor((division * 10));
            int remainderBars = 10 - amountOfBars;
            for (int i = 0; i < amountOfBars; i++) {
                returnText.append("&a☰");
            }
            for (int i = 0; i < remainderBars; i++) {
                returnText.append("&f☰");
            }
            String returnValue = returnText.toString();

            if (amountOfBars == 10) {
                returnValue = returnValue.replace("&a", "&4");
            } else if (amountOfBars > 8) {
                returnValue = returnValue.replace("&a", "&c");
            } else if (amountOfBars > 6) {
                returnValue = returnValue.replace("&a", "&6");
            } else if (amountOfBars > 4) {
                returnValue = returnValue.replace("&a", "&e");
            }
            return ChatColor.translateAlternateColorCodes('&', returnValue);
        }
    }

    public String getStorageBar(Block block) {
        ancSpawner spawner = getSpawnerManager().getSpawner(block.getWorld(), block.getLocation());
        int storageLimit = spawner.getStorageLimit();
        int storedItemAmount = spawner.getStorage().getTotalStored();
        Map<ItemStack, Integer> storedItems = spawner.getStorage().getStorage();
        StringBuilder returnText = new StringBuilder();

        if (storedItemAmount == 0) {
            returnText.append("&f☰☰☰☰☰☰☰☰☰☰");
            return ChatColor.translateAlternateColorCodes('&', returnText.toString());
        } else {
            int totalStorageLimit = storageLimit * storedItems.size();
            double division = (double) storedItemAmount / totalStorageLimit;
            int amountOfBars = (int) Math.floor((division * 10));
            int remainderBars = 10 - amountOfBars;
            for (int i = 0; i < amountOfBars; i++) {
                returnText.append("&a☰");
            }
            for (int i = 0; i < remainderBars; i++) {
                returnText.append("&f☰");
            }
            String returnValue = returnText.toString();

            if (amountOfBars == 10) {
                returnValue = returnValue.replace("&a", "&4");
            } else if (amountOfBars > 8) {
                returnValue = returnValue.replace("&a", "&c");
            } else if (amountOfBars > 6) {
                returnValue = returnValue.replace("&a", "&6");
            } else if (amountOfBars > 4) {
                returnValue = returnValue.replace("&a", "&e");
            }
            return ChatColor.translateAlternateColorCodes('&', returnValue);
        }
    }

    public void spawnerGiveXP(Player player, Block block) throws ExecutionException, InterruptedException {
        ancSpawner ancSpawner = spawnerManager.getSpawner(block.getWorld(), block.getLocation());
        int xp = ancSpawner.getStorage().getStoredXp();
        if (xp != 0) {
            player.giveExp(xp);
            ancSpawner.getStorage().setStoredXp(0);
            sendMessage(player, "tookXP", new String[]{String.valueOf(xp)});
            if (getOtherModule().getBoolean("title-messages.xp-title")) {
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
    }

    public void spawnerGetItem(Player player, ancSpawner spawner, ItemStack itemGiven, int amount) {
        ancStorage storage = spawner.getStorage();
        Map<ItemStack, Integer> item_in_data = storage.getStoredItemByNameAndType(itemGiven);

        if (item_in_data == null) {
            sendMessage(player, "noItems", new String[]{});
            return;
        }

        ItemStack item = item_in_data.keySet().iterator().next();
        int itemAmountInData = item_in_data.get(item);

        int maxStackSize = item.getMaxStackSize();
        int totalGivenAmount = 0;

        // Loop to give the items in stacks
        while (amount > 0 && itemAmountInData > 0) {
            int amountToGive = Math.min(amount, Math.min(maxStackSize, itemAmountInData));
            ItemStack itemToGive = item.clone();
            itemToGive.setAmount(amountToGive);

            // Add the item stack to the player's inventory
            HashMap<Integer, ItemStack> itemsFailedToAdd = player.getInventory().addItem(itemToGive);
            int givenAmount = amountToGive;

            // Check if some items couldn't be added due to a full inventory
            if (!itemsFailedToAdd.isEmpty()) {
                int failedAmount = itemsFailedToAdd.values().stream().mapToInt(ItemStack::getAmount).sum();
                givenAmount -= failedAmount;
            }

            // Update the amount given and reduce the amount in data
            totalGivenAmount += givenAmount;
            itemAmountInData -= givenAmount;
            amount -= givenAmount;

            // If not all items were added, stop the process
            if (givenAmount < amountToGive) {
                break;
            }
        }

        // Inform the player about the successfully taken items
        if (totalGivenAmount > 0) {
            sendMessage(player, "tookItems", new String[]{String.valueOf(totalGivenAmount), item.getType().toString()});
            storage.setStoredItem(item, itemAmountInData);
        } else {
            sendMessage(player, "noItems", new String[]{});
        }
    }


    public void updateSpawnerHologram(Player player, Block block, @Nullable Boolean is_silent) {
        if (getServer().getPluginManager().getPlugin("DecentHolograms") != null && getServer().getPluginManager().getPlugin("DecentHolograms").isEnabled()) {
            if (getHologramModule().getBoolean("hologram.enabled")) {
                HologramEditor.updateHologram(block);
            }
        }
    }

    public void calculateSpawnerStorage(Player player, ancSpawner spawner) {
        int new_limit = getStorageModule().getInt("storage-limit.default");
        if (getStorageModule().getBoolean("storage-limit.enabled")) {
            for (String permission : getStorageModule().getConfigurationSection("storage-limit.permissions").getKeys(false)) {
                if (getPermissions().has(player, permission)) {
                    new_limit = Math.max(new_limit, getStorageModule().getInt("storage-limit.permissions." + permission));
                }
            }
        }
        spawner.setStorageLimit(new_limit);
    }

    /**
     * @return Returns a HashMap<String, String>
     *     where first String is SpawnerType, SpawnerMode
     *     or SpawnerConfigName, and the second String is
     *     the corresponding value.
     */
    public HashMap<String, String> getSpawnerFromItemStack(ItemStack itemStack) {
        ReadableNBT nbt = NBT.readNbt(itemStack);
        String spawner_config_name = nbt.getString("SpawnerConfigName");
        String spawner_type = nbt.getString("SpawnerType");
        String spawner_mode = nbt.getString("SpawnerMode");
        HashMap<String, String> return_hash = new HashMap<>();

        if (spawner_type == null || spawner_mode == null) {
            return null;
        }

        return_hash.put("SpawnerConfigName", spawner_config_name);
        return_hash.put("SpawnerType", spawner_type);
        return_hash.put("SpawnerMode", spawner_mode);
        return return_hash;
    }

    public ItemStack getSpawnerFromSpawnerBlock(ancSpawner spawner) {
        if (spawner.getConfigName() == null || (spawner.getConfigName() != null && spawner.getConfigName().equalsIgnoreCase("default"))) {
            return getSpawner(spawner.getType());
        } else {
            return getSpawner(spawner.getConfigName());
        }
    }

    public ItemStack getSpawner(String spawned_type) {
        boolean is_mob = false;
        boolean is_custom = false;

        // Check if the spawned_type is a custom spawner defined in the config
        if (getConfig().get("spawners." + spawned_type) != null) {
            is_custom = true;
        }

        // If not custom, check if it corresponds to a valid entity type
        if (!is_custom && isValidEntityType(spawned_type)) {
            is_mob = true;
        }

        // If neither a mob nor custom, return null
        if (!is_mob && !is_custom) {
            return null;
        }

        // Proceed based on whether it's a custom spawner or a mob spawner
        ItemStack spawner = XMaterial.SPAWNER.parseItem();
        ItemMeta spawner_meta = spawner.getItemMeta();
        ConfigurationSection section;

        if (is_custom) {
            // Handle custom spawner
            section = getConfig().getConfigurationSection("spawners." + spawned_type);
            String spawner_name_in_config = ChatColor.translateAlternateColorCodes('&', section.getString("name"));
            List<String> spawner_lore_in_config = section.getStringList("lore");
            List<String> colored_lore = new ArrayList<>();
            String spawner_type_in_config = section.getString("spawnerInfo.material").toUpperCase(Locale.ENGLISH);
            String spawner_mode_in_config = section.getString("spawnerInfo.mode").toUpperCase(Locale.ENGLISH);
            for (String text : spawner_lore_in_config) {
                text = ChatColor.translateAlternateColorCodes('&', text);
                colored_lore.add(text);
            }
            spawner_meta.setDisplayName(spawner_name_in_config);
            spawner_meta.setLore(colored_lore);
            spawner.setItemMeta(spawner_meta);

            NBT.modify(spawner, nbt -> {
                nbt.setString("SpawnerConfigName", spawned_type);
                nbt.setString("SpawnerType", spawner_type_in_config);
                nbt.setString("SpawnerMode", spawner_mode_in_config);
            });
            return spawner;
        }

        if (is_mob) {
            // Handle mob spawner
            section = getConfig().getConfigurationSection("spawners.default");
            String spawner_name_in_config = ChatColor.translateAlternateColorCodes('&', section.getString("name"));
            spawner_name_in_config = spawner_name_in_config.replace("{entity_name}", spawned_type.toUpperCase(Locale.ENGLISH));
            List<String> spawner_lore_in_config = section.getStringList("lore");
            List<String> colored_lore = new ArrayList<>();
            for (String text : spawner_lore_in_config) {
                text = text.replace("{entity_name}", capitalizeFirstLetter(spawned_type));
                text = ChatColor.translateAlternateColorCodes('&', text);
                colored_lore.add(text);
            }
            spawner_meta.setDisplayName(spawner_name_in_config);
            spawner_meta.setLore(colored_lore);
            spawner.setItemMeta(spawner_meta);

            NBT.modify(spawner, nbt -> {
                nbt.setString("SpawnerConfigName", "default");
                nbt.setString("SpawnerType", spawned_type.toUpperCase(Locale.ENGLISH));
                nbt.setString("SpawnerMode", "ENTITY");
            });
            return spawner;
        }

        return null;
    }

    // Helper method to validate entity type
    private boolean isValidEntityType(String entityTypeName) {
        try {
            EntityType.valueOf(entityTypeName.toUpperCase(Locale.ENGLISH));
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    // Helper method to capitalize the first letter
    private String capitalizeFirstLetter(String input) {
        if (input == null || input.isEmpty()) return input;
        return input.substring(0, 1).toUpperCase(Locale.ENGLISH) + input.substring(1).toLowerCase(Locale.ENGLISH);
    }


    public Location generateNewLocation(Location originalLocation, int recursionDepth) {
        if (recursionDepth > 10) {
            return originalLocation;
        }

        World world = originalLocation.getWorld();
        int originalX = originalLocation.getBlockX();
        int originalY = originalLocation.getBlockY();
        int originalZ = originalLocation.getBlockZ();

        int offsetX = new Random().nextInt(2 * 2 + 1) - 2;
        int offsetY = new Random().nextInt(2 * 2 + 1) - 2;
        int offsetZ = new Random().nextInt(2 * 2 + 1) - 2;

        Location newLocation = new Location(world, originalX + offsetX, originalY + offsetY, originalZ + offsetZ);
        Block block = world.getBlockAt(newLocation);

        if (block.getType() == Material.AIR) {
            return newLocation;
        } else {
            return generateNewLocation(originalLocation, recursionDepth + 1);
        }
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
        File langPL = new File(getDataFolder(), "/lang/lang_pl.yml");
        if (!langPL.exists()) {
            plugin.saveResource("lang/lang_pl.yml", true);
        }
        File langDE = new File(getDataFolder(), "/lang/lang_de.yml");
        if (!langDE.exists()) {
            plugin.saveResource("lang/lang_de.yml", true);
        }
        File langFR = new File(getDataFolder(), "/lang/lang_fr.yml");
        if (!langFR.exists()) {
            plugin.saveResource("lang/lang_fr.yml", true);
        }

        String configLang = getConfig().getString("config.lang");
        if (configLang != null) {
            if (configLang.equalsIgnoreCase("tr")) {
                lang = YamlConfiguration.loadConfiguration(langTR);
            } else if (configLang.equalsIgnoreCase("en")) {
                lang = YamlConfiguration.loadConfiguration(langEN);
            } else if (configLang.equalsIgnoreCase("pl")) {
                lang = YamlConfiguration.loadConfiguration(langPL);
            } else if (configLang.equalsIgnoreCase("de")) {
                lang = YamlConfiguration.loadConfiguration(langDE);
            } else if (configLang.equalsIgnoreCase("fr")) {
                lang = YamlConfiguration.loadConfiguration(langFR);
            }
        }

        new LootTablesCreator().createLootTables();
    }

    public void loadConfig(String fileName) {
        File file = new File(getDataFolder(), "/modules/" + fileName + ".yml");
        if (!file.exists()) {
            plugin.saveResource("modules/" + fileName + ".yml", true);
        }
        switch (fileName) {
            case "autokill":
                modules_autokill = YamlConfiguration.loadConfiguration(file);
                break;
            case "friends":
                modules_friends = YamlConfiguration.loadConfiguration(file);
                break;
            case "hologram":
                modules_hologram = YamlConfiguration.loadConfiguration(file);
                break;
            case "multipliers":
                modules_multipliers = YamlConfiguration.loadConfiguration(file);
                break;
            case "other":
                modules_other = YamlConfiguration.loadConfiguration(file);
                break;
            case "settings":
                modules_settings = YamlConfiguration.loadConfiguration(file);
                break;
            case "storage_limits":
                modules_storage_limits = YamlConfiguration.loadConfiguration(file);
                break;
            case "boosts":
                modules_boost = YamlConfiguration.loadConfiguration(file);
                break;
            default:
                throw new IllegalArgumentException("Unknown module: " + fileName);
        }
    }

    public List<ItemStack> getLootTable(String entityName) {
        entityName = entityName.replace(" ", "_");
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
        entityName = entityName.replace(" ", "_");
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

    public YamlConfiguration getAutoKillModule() {
        return modules_autokill;
    }

    public YamlConfiguration getStorageModule() {
        return modules_storage_limits;
    }

    public YamlConfiguration getFriendsModule() {
        return modules_friends;
    }

    public YamlConfiguration getHologramModule() {
        return modules_hologram;
    }

    public YamlConfiguration getMultipliersModule() {
        return modules_multipliers;
    }

    public YamlConfiguration getSettingsModule() {
        return modules_settings;
    }

    public YamlConfiguration getOtherModule() {
        return modules_other;
    }

    public YamlConfiguration getBoostsModule() {
        return modules_boost;
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

    public double getPriceForItem(String item) {
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
        double multiplier = 1;
        if (getMultipliersModule().getBoolean("multipliers.enabled")) {
            for (String permission : getMultipliersModule().getConfigurationSection("multipliers.permissions").getKeys(false)) {
                if (getPermissions().has(player, permission)) {
                    multiplier = Math.max(multiplier, getMultipliersModule().getDouble("multipliers.permissions." + permission));
                }
            }
        }
        return multiplier;
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

    public static ItemStack getHead(String value) {
        // Check if the head is already cached
        ItemStack cachedHead = HEAD_CACHE.get(value);
        if (cachedHead != null) {
            // Return a clone to ensure no accidental mutations on the cached instance
            return cachedHead.clone();
        }

        // If not cached, create the item
        ItemStack generatedHead = XSkull.createItem().profile(Profileable.detect(value)).apply();

        // Store a clone in cache to avoid direct mutations affecting the stored reference
        HEAD_CACHE.put(value, generatedHead.clone());

        return generatedHead;
    }
}
