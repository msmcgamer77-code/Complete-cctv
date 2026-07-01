package com.cctv.listeners;

import com.cctv.CCTVPlugin;
import com.cctv.models.Camera;
import com.cctv.util.ItemUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.Bukkit;

import java.util.Map;

public class CameraListener implements Listener {

    private final CCTVPlugin plugin;

    public CameraListener(CCTVPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && ItemUtil.isCameraItem(plugin, item)) {
            event.setCancelled(true);
            if (!player.hasPermission("cctv.admin")) {
                player.sendMessage("§cYou don't have permission to place cameras.");
                return;
            }
            var block = event.getClickedBlock();
            var loc = block.getLocation().add(0.5, 1.0, 0.5);
            loc.setYaw(player.getLocation().getYaw());
            Camera camera = plugin.getCameraManager().placeCamera(loc);
            player.sendMessage("§a[CCTV] Camera placed: §f" + camera.getName());
            return;
        }

        if ((event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK)
                && ItemUtil.isMonitorItem(plugin, item)) {
            event.setCancelled(true);
            if (!player.hasPermission("cctv.use")) {
                player.sendMessage("§cYou don't have permission to use the monitor.");
                return;
            }
            openMonitorGUI(player);
        }
    }

    private void openMonitorGUI(Player player) {
        Map<String, Camera> cameras = plugin.getCameraManager().getCameras();
        int size = Math.max(9, ((cameras.size() / 9) + 1) * 9);
        if (size > 54) size = 54;
        Inventory inv = Bukkit.createInventory(null, size, "§1CCTV Monitor - Select Camera");

        for (Camera camera : cameras.values()) {
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            ItemMeta meta = head.getItemMeta();
            meta.setDisplayName("§b" + camera.getName());
            boolean recording = plugin.getRecordingManager().isRecording(camera.getName());
            meta.setLore(java.util.List.of(
                    "§7World: §f" + camera.getLocation().getWorld().getName(),
                    "§7Pos: §f" + camera.getLocation().getBlockX() + ", "
                            + camera.getLocation().getBlockY() + ", " + camera.getLocation().getBlockZ(),
                    recording ? "§c● Recording" : "§7○ Not recording",
                    "",
                    "§eClick to watch live"
            ));
            head.setItemMeta(meta);
            inv.addItem(head);
        }

        if (cameras.isEmpty()) {
            player.sendMessage("§7[CCTV] No cameras placed yet. Use §e/cctv give camera §7to place one.");
            return;
        }

        player.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTitle().equals("§1CCTV Monitor - Select Camera")) {
            event.setCancelled(true);
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType() != Material.PLAYER_HEAD || !clicked.hasItemMeta()) return;
            String camName = org.bukkit.ChatColor.stripColor(clicked.getItemMeta().getDisplayName());
            Camera camera = plugin.getCameraManager().getCamera(camName);
            if (camera == null) {
                event.getWhoClicked().sendMessage("§cThat camera no longer exists.");
                return;
            }
            Player player = (Player) event.getWhoClicked();
            player.closeInventory();
            plugin.getWatchManager().startWatch(player, camera.getLocation());
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        if (plugin.getWatchManager().isWatching(event.getPlayer())) {
            plugin.getWatchManager().stopWatch(event.getPlayer());
        }
    }
                }
