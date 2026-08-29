package dev.minhhaudev.checktimedeploy.hook;

import dev.minhhaudev.checktimedeploy.CheckTimeDeploy;
import dev.minhhaudev.checktimedeploy.util.TimeUtil;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import java.time.Instant;

public class PlaceholderHook extends PlaceholderExpansion {

    private final CheckTimeDeploy plugin;

    public PlaceholderHook(CheckTimeDeploy plugin) {
        this.plugin = plugin;
    }

    @Override public @NotNull String getIdentifier() { return "checktimedeploy"; }
    @Override public @NotNull String getAuthor() { return "minhhaudev"; }
    @Override public @NotNull String getVersion() { return plugin.getDescription().getVersion(); }
    @Override public boolean persist() { return true; }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String params) {
        Instant deploy = plugin.getConfigManager().getDeployInstant();
        Instant now = Instant.now();
        String tz = plugin.getConfigManager().getTimezone();
        String fmt = plugin.getConfigManager().getDateFormat();
        switch (params.toLowerCase()) {
            case "deploy": return TimeUtil.formatInstant(deploy, fmt, tz);
            case "days": return String.valueOf(TimeUtil.between(deploy, now).toDays());
            case "hours": return String.valueOf(TimeUtil.between(deploy, now).toHours());
            case "duration": return TimeUtil.formatDuration(TimeUtil.between(deploy, now));
            case "unique": return String.valueOf(plugin.getDatabaseManager().getTotalUnique());
            case "joins": return String.valueOf(plugin.getDatabaseManager().getTotalJoins());
            case "quits": return String.valueOf(plugin.getDatabaseManager().getTotalQuits());
            default: return null;
        }
    }
}
