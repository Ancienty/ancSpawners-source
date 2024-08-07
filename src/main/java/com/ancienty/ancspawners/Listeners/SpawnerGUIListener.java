package com.ancienty.ancspawners.Listeners;

import com.ancienty.ancspawners.GUIs.FriendsGUI;
import com.ancienty.ancspawners.GUIs.SettingsGUI;
import com.ancienty.ancspawners.GUIs.StorageGUI;
import com.ancienty.ancspawners.Main;
import com.ancienty.ancspawners.SpawnerManager.ancSpawner;
import com.cryptomorin.xseries.XMaterial;
import eu.decentsoftware.holograms.api.DHAPI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

public class SpawnerGUIListener implements Listener {

    HashMap<Player, ancSpawner> friendsAddCheckList = new HashMap<>();

    @EventHandler
    public void onSpawnerMenuClickEvent(InventoryClickEvent e) throws ExecutionException, InterruptedException {
        Player player = (Player) e.getView().getPlayer();
        Block block = Main.getPlugin().player_block_map.getOrDefault((Player) e.getView().getPlayer(), null);

        if (block != null) {
            ancSpawner spawner = Main.getPlugin().getSpawnerManager().getSpawner(block.getWorld(), block.getLocation());
            if (spawner != null) {
                e.getView().close();
                // Spawner menu listener
                if (e.getView().getTitle().equalsIgnoreCase(ChatColor.translateAlternateColorCodes('&', Main.getPlugin().lang.getString("menu.menuName")))) {
                    if (spawner != null && player != null) {
                        int info_slot = -1;
                        int level_slot = -1;
                        int hologram_slot = -1;
                        int storage_slot = -1;
                        int xp_slot = -1;
                        int friends_slot = -1;
                        int autokill_slot = -1;
                        int settings_slot = -1;
                        if (Main.getPlugin().lang.getBoolean("menu.info.gui")) {
                            info_slot = Main.getPlugin().lang.getInt("menu.info.slot");
                        }
                        if (Main.getPlugin().lang.getBoolean("menu.level-up.gui")) {
                            level_slot = Main.getPlugin().lang.getInt("menu.level-up.slot");
                        }
                        if (Main.getPlugin().lang.getBoolean("menu.hologram.gui")) {
                            hologram_slot = Main.getPlugin().lang.getInt("menu.hologram.slot");
                        }
                        if (Main.getPlugin().lang.getBoolean("menu.storage.gui")) {
                            storage_slot = Main.getPlugin().lang.getInt("menu.storage.slot");
                        }
                        if (Main.getPlugin().lang.getBoolean("menu.exp.gui")) {
                            xp_slot = Main.getPlugin().lang.getInt("menu.exp.slot");
                        }
                        if (Main.getPlugin().lang.getBoolean("menu.friends.gui")) {
                            friends_slot = Main.getPlugin().lang.getInt("menu.friends.slot");
                        }
                        if (Main.getPlugin().lang.getBoolean("menu.auto-kill.gui")) {
                            autokill_slot = Main.getPlugin().lang.getInt("menu.auto-kill.slot");
                        }
                        if (Main.getPlugin().lang.getBoolean("menu.settings.gui")) {
                            settings_slot = Main.getPlugin().lang.getInt("menu.settings.slot");
                        }

                        if (e.getCurrentItem() != null) {
                            e.setCancelled(true);
                            if (e.getRawSlot() == info_slot) {
                                e.getView().getPlayer().closeInventory();
                            } else if (e.getRawSlot() == level_slot) {
                                if (e.getView().getPlayer().getInventory().getItemInOffHand() == null || !e.getView().getPlayer().getInventory().getItemInOffHand().getType().equals(XMaterial.SPAWNER.parseMaterial())) {
                                    e.getView().getPlayer().closeInventory();
                                    Main.getPlugin().spawnerLevelUp((Player) e.getView().getPlayer(), block);
                                } else {
                                    Main.getPlugin().sendMessage((Player) e.getView().getPlayer(), "offHandSpawner", new String[]{"null"});
                                }
                            } else if (e.getRawSlot() == hologram_slot) {
                                e.getView().getPlayer().closeInventory();
                                if (DHAPI.getHologram(Main.database.getHologramName(block)) != null) {
                                    DHAPI.removeHologram(Main.database.getHologramName(block));
                                    Main.getPlugin().sendMessage(player, "disabledHologram", new String[]{"null"});
                                } else {
                                    Main.getPlugin().createSpawnerHologram(player, block, block.getWorld(), block.getLocation());
                                }
                            } else if (e.getRawSlot() == storage_slot) {
                                player.closeInventory();
                                new StorageGUI(player, block);
                            } else if (e.getRawSlot() == xp_slot) {
                                Player player2 = (Player) e.getView().getPlayer();
                                if (spawner.getMode().equalsIgnoreCase("ENTITY")) {
                                    try {
                                        Main.getPlugin().spawnerGiveXP(player2, block);
                                    } catch (ExecutionException | InterruptedException ex) {
                                        ex.printStackTrace();
                                    }
                                    player2.closeInventory();
                                }
                            } else if (e.getRawSlot() == friends_slot) {
                                e.getView().getPlayer().closeInventory();
                                new FriendsGUI(player, block);
                            } else if (e.getRawSlot() == autokill_slot) {
                                e.getView().getPlayer().closeInventory();
                                if (spawner.getMode().equalsIgnoreCase("ENTITY")) {
                                    spawner.setAutoKill(!spawner.getAutoKill());
                                    Bukkit.getScheduler().runTask(Main.getPlugin(), () -> {
                                        if (spawner.getAutoKill()) {
                                            Main.getPlugin().sendMessage(player, "enabledAutoKill", new String[]{null});
                                        } else {
                                            Main.getPlugin().sendMessage(player, "disabledAutoKill", new String[]{null});
                                        }
                                    });
                                }
                            } else if (e.getRawSlot() == settings_slot) {
                                e.getView().getPlayer().closeInventory();
                                new SettingsGUI(player, block);
                            }
                        }
                    }

                    // Storage menu listener
                } else if (e.getView().getTitle().equalsIgnoreCase(ChatColor.translateAlternateColorCodes('&', Main.getPlugin().lang.getString("storageMenu.menuName")))) {
                    e.setCancelled(true);
                    Material fillerItem = XMaterial.valueOf(Main.getPlugin().lang.getString("storageMenu.fillItem.material")).parseMaterial();
                    if (e.getCurrentItem() != null && e.getCurrentItem().getType() != fillerItem) {
                        if (e.getSlot() == Main.getPlugin().lang.getInt("storageMenu.sellItem.slot")) {
                            Main.getEconomy().depositPlayer((Player) e.getView().getPlayer(), spawner.getStorage().getMoney((Player) e.getView().getPlayer()));
                            Main.getPlugin().sendMessage(player, "soldItems", new String[]{String.valueOf(spawner.getStorage().getTotalStored()), String.valueOf(spawner.getStorage().getMoney(player)), String.valueOf(Main.getPlugin().getPlayerMultiplier(player))});
                            if (Main.getPlugin().getConfig().getBoolean("config.modules.title-messages.money-title")) {
                                String title = ChatColor.translateAlternateColorCodes('&', Main.getPlugin().lang.getString("lang.sellItemsTitle"));
                                String sub_title = ChatColor.translateAlternateColorCodes('&', Main.getPlugin().lang.getString("lang.sellItemsSubtitle"));
                                title = title.replace("{money_gained}", String.valueOf(spawner.getStorage().getMoney(player)));
                                sub_title = sub_title.replace("{money_gained}", String.valueOf(spawner.getStorage().getMoney(player)));
                                player.sendTitle(title, sub_title, 5, 30, 5);
                            }
                            spawner.getStorage().clearStorage();
                            e.getView().getPlayer().closeInventory();
                        } else {
                            if (e.getClick().isLeftClick()) {
                                String clickedItem = String.valueOf(e.getCurrentItem().getType());
                                Main.getPlugin().spawnerGetItem((Player) e.getView().getPlayer(), spawner, clickedItem, 64);
                                e.getView().getPlayer().closeInventory();
                            } else if (e.getClick().isRightClick()) {
                                String clickedItem = String.valueOf(e.getCurrentItem().getType());
                                Main.getPlugin().spawnerGetItem((Player) e.getView().getPlayer(), spawner, clickedItem, 2304);
                                e.getView().getPlayer().closeInventory();
                            }
                        }
                    }

                    // Friends menu listener
                } else if (e.getView().getTitle().equalsIgnoreCase(ChatColor.translateAlternateColorCodes('&', Main.getPlugin().lang.getString("friendsMenu.menuName")))) {
                    e.setCancelled(true);

                    Material fillerItem = XMaterial.valueOf(Main.getPlugin().lang.getString("friendsMenu.fillItem.material")).parseMaterial();
                    if (e.getCurrentItem() != null && e.getCurrentItem().getType() != fillerItem) {
                        if (e.getSlot() == Main.getPlugin().lang.getInt("friendsMenu.addItem.slot")) {
                            if (!friendsAddCheckList.isEmpty() || !friendsAddCheckList.containsKey((Player) e.getView().getPlayer())) {
                                friendsAddCheckList.put((Player) e.getView().getPlayer(), spawner);
                                Main.getPlugin().sendMessage((Player) e.getView().getPlayer(), "addFriendMessage", new String[]{});
                                e.getView().getPlayer().closeInventory();
                            }
                        } else {
                            if (e.getCurrentItem().getType() == XMaterial.PLAYER_HEAD.parseMaterial()) {
                                List<String> lore = e.getCurrentItem().getItemMeta().getLore();
                                String friendUUID = lore.get(0);
                                friendUUID = ChatColor.stripColor(friendUUID);
                                friendUUID = friendUUID.replace("&8", "");
                                String finalFriendUUID = friendUUID;
                                spawner.removeFriend(finalFriendUUID);
                                Main.getPlugin().sendMessage((Player) e.getView().getPlayer(), "removedFriendFromSpawner", new String[]{});
                                e.getView().getPlayer().closeInventory();
                            }
                        }
                    }

                    // Settings menu listener
                } else if (e.getView().getTitle().equalsIgnoreCase(ChatColor.translateAlternateColorCodes('&', Main.getPlugin().lang.getString("settingsMenu.menuName")))) {
                    e.setCancelled(true);
                    if (e.getCurrentItem() != null) {
                        if (e.getSlot() == Main.getPlugin().lang.getInt("settingsMenu.virtual_storage.slot")) {
                            boolean virtual_force_default = Main.getPlugin().getConfig().getBoolean("config.modules.settings.virtual-storage.force-default");
                            boolean default_value = Main.getPlugin().getConfig().getBoolean("config.modules.settings.virtual-storage.default");
                            if (virtual_force_default) {
                                spawner.setVirtualStorage(default_value);
                                e.getView().getPlayer().closeInventory();
                                return;
                            }

                            String permission = Main.getPlugin().getConfig().getString("config.modules.settings.virtual-storage.permission");
                            if (Main.getPermissions().has(e.getView().getPlayer(), permission)) {
                                spawner.setVirtualStorage(!spawner.isVirtualStorageEnabled());
                                Main.getPlugin().sendMessage((Player) e.getView().getPlayer(), "toggledVirtualStorage", new String[]{});
                            } else {
                                Main.getPlugin().sendMessage((Player) e.getView().getPlayer(), "noPermission", new String[]{});
                            }
                        } else if (e.getSlot() == Main.getPlugin().lang.getInt("settingsMenu.xp_storage.slot")) {
                            boolean xp_force_default = Main.getPlugin().getConfig().getBoolean("config.modules.settings.xp-storage.force-default");
                            boolean default_value = Main.getPlugin().getConfig().getBoolean("config.modules.settings.xp-storage.default");
                            if (xp_force_default) {
                                spawner.setXPStorage(default_value);
                                e.getView().getPlayer().closeInventory();
                                return;
                            }

                            String permission = Main.getPlugin().getConfig().getString("config.modules.settings.xp-storage.permission");
                            if (Main.getPermissions().has(e.getView().getPlayer(), permission)) {
                                spawner.setXPStorage(!spawner.isXPStorageEnabled());
                                Main.getPlugin().sendMessage((Player) e.getView().getPlayer(), "toggledXpStorage", new String[]{});
                            } else {
                                Main.getPlugin().sendMessage((Player) e.getView().getPlayer(), "noPermission", new String[]{});
                            }
                        }
                        e.getView().getPlayer().closeInventory();
                    }
                }
            }
        }
    }



    @EventHandler
    public void onPlayerMessageEvent(AsyncPlayerChatEvent e) {
        if (!friendsAddCheckList.isEmpty()) {
            if (friendsAddCheckList.containsKey(e.getPlayer())) {
                ancSpawner block = friendsAddCheckList.get(e.getPlayer());
                Player playerToAdd = Bukkit.getPlayer(e.getMessage());
                Player currentPlayer = e.getPlayer();
                String message = e.getMessage();
                e.setCancelled(true);
                if (message.contains("cancel")) {
                    friendsAddCheckList.remove(currentPlayer);
                    Main.getPlugin().sendMessage(currentPlayer, "cancelledProcess", new String[]{});
                } else {
                    if (playerToAdd != null) {
                        if (playerToAdd == currentPlayer) {
                            friendsAddCheckList.remove(currentPlayer);
                            return;
                        }
                        if (block.isFriend(playerToAdd.getUniqueId().toString())) {
                            block.addFriend(playerToAdd.getUniqueId().toString());
                            Main.getPlugin().sendMessage(currentPlayer, "successfullyAddedFriend", new String[]{});
                        }
                        friendsAddCheckList.remove(currentPlayer);
                    } else {
                        Main.getPlugin().sendMessage(currentPlayer, "playerNotFound", new String[]{});
                        friendsAddCheckList.remove(currentPlayer);
                    }
                }
            }
        }
    }
}
