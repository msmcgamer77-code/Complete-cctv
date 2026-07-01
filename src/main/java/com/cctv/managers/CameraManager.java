package com.cctv.managers;

import com.cctv.models.Camera;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public class CameraManager {

    private final JavaPlugin plugin;
    private final File file;
    private final Map<String, Camera> cameras = new LinkedHashMap<>();
    private int counter = 0;

    public CameraManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "cameras.yml");
    }

    public Map<String, Camera> getCameras() {
        return cameras;
    }

    public Camera getCamera(String name) {
        return cameras.get(name.toLowerCase());
    }

    public Camera placeCamera(Location location) {
        counter++;
        String name = "cam" + counter;
        while (cameras.containsKey(name)) {
            counter++;
            name = "cam" + counter;
        }

        ArmorStand stand = (ArmorStand) location.getWorld().spawnEntity(location, EntityType.ARMOR_STAND);
        stand.setSmall(true);
        stand.setInvulnerable(true);
        stand.setGravity(false);
        stand.setBasePlate(false);
        stand.setArms(false);
        stand.setCustomNameVisible(true);
        stand.setCustomName("§b[CCTV] " + name);
        stand.getEquipment().setHelmet(new ItemStack(Material.PLAYER_HEAD));

        Camera camera = new Camera(name, location);
        camera.setStandEntity(stand);
        cameras.put(name, camera);
        saveCameras();
        return camera;
    }

    public boolean removeCamera(String name) {
        Camera camera = cameras.remove(name.toLowerCase());
        if (camera == null) return false;
        if (camera.getStandEntity() != null && !camera.getStandEntity().isDead()) {
            camera.getStandEntity().remove();
        }
        saveCameras();
        return true;
    }

    public void saveCameras() {
        FileConfiguration config = new YamlConfiguration();
        for (Camera camera : cameras.values()) {
            String path = "cameras." + camera.getName();
            Location loc = camera.getLocation();
            config.set(path + ".world", loc.getWorld().getName());
            config.set(path + ".x", loc.getX());
            config.set(path + ".y", loc.getY());
            config.set(path + ".z", loc.getZ());
            config.set(path + ".yaw", loc.getYaw());
            config.set(path + ".pitch", loc.getPitch());
            if (camera.getStandEntity() != null) {
                config.set(path + ".entity-uuid", camera.getStandEntity().getUniqueId().toString());
            }
        }
        try {
            if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save cameras.yml: " + e.getMessage());
        }
    }

    public void loadCameras() {
        cameras.clear();
        if (!file.exists()) return;
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        if (config.getConfigurationSection("cameras") == null) return;

        for (String name : config.getConfigurationSection("cameras").getKeys(false)) {
            String path = "cameras." + name;
            String worldName = config.getString(path + ".world");
            World world = Bukkit.getWorld(worldName);
            if (world == null) continue;

            double x = config.getDouble(path + ".x");
            double y = config.getDouble(path + ".y");
            double z = config.getDouble(path + ".z");
            float yaw = (float) config.getDouble(path + ".yaw");
            float pitch = (float) config.getDouble(path + ".pitch");

            Location loc = new Location(world, x, y, z, yaw, pitch);
            Camera camera = new Camera(name, loc);

            String uuidStr = config.getString(path + ".entity-uuid");
            ArmorStand stand = null;
            if (uuidStr != null) {
                try {
                    org.bukkit.entity.Entity e = Bukkit.getEntity(java.util.UUID.fromString(uuidStr));
                    if (e instanceof ArmorStand) stand = (ArmorStand) e;
                } catch (IllegalArgumentException ignored) {}
            }
            if (stand == null || stand.isDead()) {
                stand = (ArmorStand) world.spawnEntity(loc, EntityType.ARMOR_STAND);
                stand.setSmall(true);
                stand.setInvulnerable(true);
                stand.setGravity(false);
                stand.setBasePlate(false);
                stand.setArms(false);
                stand.setCustomNameVisible(true);
                stand.setCustomName("§b[CCTV] " + name);
                stand.getEquipment().setHelmet(new ItemStack(Material.PLAYER_HEAD));
            }
            camera.setStandEntity(stand);
            cameras.put(name, camera);

            if (name.matches("cam\\d+")) {
                int n = Integer.parseInt(name.substring(3));
                if (n > counter) counter = n;
            }
        }
    }
  }
