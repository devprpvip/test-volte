package dev.minhhaudev.checktimedeploy;

import dev.minhhaudev.checktimedeploy.command.DeployCommand;
import dev.minhhaudev.checktimedeploy.config.ConfigManager;
import dev.minhhaudev.checktimedeploy.listener.PlayerListener;
import dev.minhhaudev.checktimedeploy.storage.DatabaseManager;
import dev.minhhaudev.checktimedeploy.util.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Instant;

public final class CheckTimeDeploy extends JavaPlugin {

    private ConfigManager configManager;
    private DatabaseManager databaseManager;
    private Instant startTime;

    @Override
    public void onLoad() {
        // config will be loaded in onEnable
    }

    @Override
    public void onEnable() {
        this.startTime = Instant.now();

        // Config
        this.configManager = new ConfigManager(this);
        configManager.load();

        // Database
        this.databaseManager = new DatabaseManager(this);
        databaseManager.init();

        // Listeners - tự ghi nhận số liệu thật theo thời gian thực
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);

        // Command
        DeployCommand cmd = new DeployCommand(this);
        if (getCommand("checktimedeploy") != null) {
            getCommand("checktimedeploy").setExecutor(cmd);
            getCommand("checktimedeploy").setTabCompleter(cmd);
        }

        Instant deploy = configManager.getDeployInstant();
        String deployStr = TimeUtil.formatInstant(deploy, configManager.getDateFormat(), configManager.getTimezone());
        String nowStr = TimeUtil.formatInstant(startTime, configManager.getDateFormat(), configManager.getTimezone());
        long days = TimeUtil.between(deploy, startTime).toDays();

        getLogger().info("========================================");
        getLogger().info(" CheckTimeDeploy v" + getDescription().getVersion() + " by minhhaudev");
        getLogger().info(" Java 21 | Support 1.21.x | SQLite");
        getLogger().info(" Deploy: " + deployStr + " (" + configManager.getTimezone() + ")");
        getLogger().info(" Start : " + nowStr);
        getLogger().info(" Tổng deploy: " + days + " ngày (" + TimeUtil.formatDuration(TimeUtil.between(deploy, startTime)) + ")");
        getLogger().info(" DB: " + configManager.getDatabaseFile() + " | Unique: " + databaseManager.getTotalUnique());
        getLogger().info("========================================");

        // PlaceholderAPI hook optional
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            try {
                new dev.minhhaudev.checktimedeploy.hook.PlaceholderHook(this).register();
                getLogger().info("Đã hook PlaceholderAPI");
            } catch (Exception ignored) {}
        }
    }

    @Override
    public void onDisable() {
        if (databaseManager != null) databaseManager.close();
        if (startTime != null) {
            getLogger().info("CheckTimeDeploy disabled. Uptime: " + TimeUtil.formatDuration(TimeUtil.between(startTime, Instant.now())));
        }
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public Instant getStartTime() {
        return startTime;
    }
}
