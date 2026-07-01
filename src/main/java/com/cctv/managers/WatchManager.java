package com.cctv.managers;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;

public class WatchManager {

    private final JavaPlugin plugin;

    private final Map<java.util.UUID, PreState> watching = new HashMap<>();

    public WatchManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    private static class PreState {
        final Location location;
        final GameMode gameMode;
        PreState(Location location, GameMode gameMode) {
            this.location = location;
            this.gameMode = gameMode;
        }
    }

    public boolean isWatching(Player player) {
        return watching.containsKey(player.getUniqueId());
    }

    public void startWatch(Player player, Location cameraLocation) {
        if (isWatching(player)) {
            player.teleport(cameraLocation);
            return;
        }
        watching.put(player.getUniqueId(), new PreState(player.getLocation(), player.getGameMode()));
        player.setGameMode(GameMode.SPECTATOR);
        player.teleport(cameraLocation);
        player.sendMessage("§b[CCTV] §fYou are now watching live. Type §e/cctv stopwatch §fto return.");
    }

    public void stopWatch(Player player) {
        PreState state = watching.remove(player.getUniqueId());
        if (state == null) return;
        player.setGameMode(state.gameMode);
        player.teleport(state.location);
        player.sendMessage("§b[CCTV] §fStopped watching.");
    }
}
