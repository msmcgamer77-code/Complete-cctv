package com.cctv.commands;

import com.cctv.CCTVPlugin;
import com.cctv.models.Camera;
import com.cctv.util.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class CCTVCommand implements CommandExecutor {

    private final CCTVPlugin plugin;

    public CCTVCommand(CCTVPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "give": {
                if (!sender.hasPermission("cctv.admin")) {
                    sender.sendMessage("§cNo permission.");
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage("§cUsage: /cctv give <camera|monitor> [player]");
                    return true;
                }
                Player target;
                if (args.length >= 3) {
                    target = Bukkit.getPlayer(args[2]);
                    if (target == null) {
                        sender.sendMessage("§cPlayer not found.");
                        return true;
                    }
                } else if (sender instanceof Player) {
                    target = (Player) sender;
                } else {
                    sender.sendMessage("§cSpecify a player when running from console.");
                    return true;
                }

                if (args[1].equalsIgnoreCase("camera")) {
                    target.getInventory().addItem(ItemUtil.createCameraItem(plugin));
                    sender.sendMessage("§aGave a CCTV Camera item to " + target.getName());
                } else if (args[1].equalsIgnoreCase("monitor")) {
                    target.getInventory().addItem(ItemUtil.createMonitorItem(plugin));
                    sender.sendMessage("§aGave a CCTV Monitor item to " + target.getName());
                } else {
                    sender.sendMessage("§cUsage: /cctv give <camera|monitor> [player]");
                }
                return true;
            }

            case "list": {
                if (plugin.getCameraManager().getCameras().isEmpty()) {
                    sender.sendMessage("§7No cameras placed yet.");
                    return true;
                }
                sender.sendMessage("§b--- CCTV Cameras ---");
                for (Camera camera : plugin.getCameraManager().getCameras().values()) {
                    boolean rec = plugin.getRecordingManager().isRecording(camera.getName());
                    sender.sendMessage("§f- " + camera.getName() + " §7@ " + camera.getLocation().getWorld().getName()
                            + " " + camera.getLocation().getBlockX() + "," + camera.getLocation().getBlockY()
                            + "," + camera.getLocation().getBlockZ() + (rec ? " §c[RECORDING]" : ""));
                }
                return true;
            }

            case "remove": {
                if (!sender.hasPermission("cctv.admin")) {
                    sender.sendMessage("§cNo permission.");
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage("§cUsage: /cctv remove <name>");
                    return true;
                }
                boolean removed = plugin.getCameraManager().removeCamera(args[1]);
                sender.sendMessage(removed ? "§aCamera removed." : "§cCamera not found.");
                return true;
            }

            case "watch": {
                if (!(sender instanceof Player)) {
                    sender.sendMessage("§cOnly players can watch cameras.");
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage("§cUsage: /cctv watch <name>");
                    return true;
                }
                Camera camera = plugin.getCameraManager().getCamera(args[1]);
                if (camera == null) {
                    sender.sendMessage("§cCamera not found.");
                    return true;
                }
                plugin.getWatchManager().startWatch((Player) sender, camera.getLocation());
                return true;
            }

            case "stopwatch": {
                if (!(sender instanceof Player)) return true;
                plugin.getWatchManager().stopWatch((Player) sender);
                return true;
            }

            case "record": {
                if (!sender.hasPermission("cctv.admin")) {
                    sender.sendMessage("§cNo permission.");
                    return true;
                }
                if (args.length < 3) {
                    sender.sendMessage("§cUsage: /cctv record <start|stop> <camera>");
                    return true;
                }
                Camera camera = plugin.getCameraManager().getCamera(args[2]);
                if (camera == null) {
                    sender.sendMessage("§cCamera not found.");
                    return true;
                }
                if (args[1].equalsIgnoreCase("start")) {
                    if (plugin.getRecordingManager().isRecording(camera.getName())) {
                        sender.sendMessage("§cAlready recording that camera.");
                        return true;
                    }
                    plugin.getRecordingManager().startRecording(camera);
                    sender.sendMessage("§c● §fStarted recording on " + camera.getName());
                } else if (args[1].equalsIgnoreCase("stop")) {
                    String saved = plugin.getRecordingManager().stopRecording(camera.getName());
                    if (saved == null) {
                        sender.sendMessage("§cThat camera isn't recording.");
                    } else {
                        sender.sendMessage("§a■ Stopped recording. Saved as: §f" + saved);
                    }
                } else {
                    sender.sendMessage("§cUsage: /cctv record <start|stop> <camera>");
                }
                return true;
            }

            case "footage": {
                if (args.length < 2) {
                    sender.sendMessage("§cUsage: /cctv footage <camera>");
                    return true;
                }
                List<String> files = plugin.getRecordingManager().listFootage(args[1]);
                if (files.isEmpty()) {
                    sender.sendMessage("§7No saved footage for that camera.");
                    return true;
                }
                sender.sendMessage("§b--- Footage for " + args[1] + " ---");
                for (String f : files) sender.sendMessage("§f- " + f);
                return true;
            }

            case "playback": {
                if (!(sender instanceof Player)) {
                    sender.sendMessage("§cOnly players can view playback.");
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage("§cUsage: /cctv playback <filename>");
                    return true;
                }
                boolean ok = plugin.getRecordingManager().playback(args[1], sender);
                if (!ok) sender.sendMessage("§cCould not play back that footage file.");
                return true;
            }

            default:
                sendHelp(sender);
                return true;
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§b--- CCTV Plugin ---");
        sender.sendMessage("§e/cctv give camera|monitor [player] §7- get the camera/monitor item");
        sender.sendMessage("§e/cctv list §7- list all cameras");
        sender.sendMessage("§e/cctv remove <name> §7- remove a camera");
        sender.sendMessage("§e/cctv watch <name> §7- watch a camera live");
        sender.sendMessage("§e/cctv stopwatch §7- stop watching");
        sender.sendMessage("§e/cctv record start|stop <name> §7- record footage");
        sender.sendMessage("§e/cctv footage <name> §7- list saved footage files");
        sender.sendMessage("§e/cctv playback <file> §7- replay saved footage");
    }
                           }
