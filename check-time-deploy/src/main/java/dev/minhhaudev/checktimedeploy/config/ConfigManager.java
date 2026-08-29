package dev.minhhaudev.checktimedeploy.config;

import dev.minhhaudev.checktimedeploy.CheckTimeDeploy;
import dev.minhhaudev.checktimedeploy.util.TimeUtil;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.time.Instant;

public class ConfigManager {

    private final CheckTimeDeploy plugin;
    private File file;
    private FileConfiguration config;
    private Instant deployInstant;

    public ConfigManager(CheckTimeDeploy plugin) {
        this.plugin = plugin;
    }

    public void load() {
        file = new File(plugin.getDataFolder(), "config.yml");
        if (!file.exists()) {
            plugin.saveResource("config.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(file);
        // auto update missing keys
        boolean needSave = false;
        if (!config.contains("deploy.time")) { config.set("deploy.time", "2024-11-12 20:16:08"); needSave = true; }
        if (!config.contains("deploy.timezone")) { config.set("deploy.timezone", "Asia/Ho_Chi_Minh"); needSave = true; }
        if (!config.contains("deploy.format-date")) { config.set("deploy.format-date", "dd/MM/yyyy HH:mm:ss"); needSave = true; }
        if (!config.contains("storage.file")) { config.set("storage.file", "database.db"); needSave = true; }
        if (needSave) save();

        reloadDeployInstant();
    }

    public void reload() {
        config = YamlConfiguration.loadConfiguration(file);
        reloadDeployInstant();
    }

    private void reloadDeployInstant() {
        String timeStr = config.getString("deploy.time", "2024-11-12 20:16:08");
        String tz = config.getString("deploy.timezone", "Asia/Ho_Chi_Minh");
        deployInstant = TimeUtil.parseDeployTime(timeStr, tz);
    }

    public void save() {
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Không thể lưu config.yml: " + e.getMessage());
        }
    }

    public boolean setDeployTime(String timeStr) {
        // validate format dd/MM/yyyy HH:mm:ss
        try {
            String tz = getTimezone();
            Instant test = TimeUtil.parseDeployTime(timeStr, tz);
            // also try to ensure parsing is correct by formatting back
            if (test == null) return false;
            config.set("deploy.time", timeStr);
            save();
            deployInstant = test;
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public Instant getDeployInstant() {
        return deployInstant;
    }

    public String getDeployTimeString() {
        return config.getString("deploy.time", "2024-11-12 20:16:08");
    }

    public String getTimezone() {
        return config.getString("deploy.timezone", "Asia/Ho_Chi_Minh");
    }

    public String getDateFormat() {
        return config.getString("deploy.format-date", "dd/MM/yyyy HH:mm:ss");
    }

    public String getDatabaseFile() {
        return config.getString("storage.file", "database.db");
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public String getMessage(String path, String def) {
        return config.getString("messages." + path, def);
    }
}
