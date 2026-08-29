package dev.minhhaudev.checktimedeploy.command;

import dev.minhhaudev.checktimedeploy.CheckTimeDeploy;
import dev.minhhaudev.checktimedeploy.storage.PlayerData;
import dev.minhhaudev.checktimedeploy.util.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DeployCommand implements CommandExecutor, TabCompleter {

    private final CheckTimeDeploy plugin;

    public DeployCommand(CheckTimeDeploy plugin) {
        this.plugin = plugin;
    }

    private String color(String s) {
        return s.replace("&", "§");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            showMain(sender);
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "player":
            case "p":
                if (args.length < 2) {
                    sender.sendMessage(color("&cDùng: /" + label + " player <tên>"));
                    return true;
                }
                showPlayer(sender, args[1]);
                return true;
            case "top":
                String type = args.length >= 2 ? args[1] : "joins";
                showTop(sender, type);
                return true;
            case "reload":
                if (!sender.hasPermission("checktimedeploy.admin")) {
                    sender.sendMessage(color(plugin.getConfigManager().getMessage("no-permission", "&cBạn không có quyền.")));
                    return true;
                }
                plugin.getConfigManager().reload();
                sender.sendMessage(color(plugin.getConfigManager().getMessage("reload", "&aĐã reload.")));
                return true;
            case "setdeploy":
            case "settime":
                if (!sender.hasPermission("checktimedeploy.admin")) {
                    sender.sendMessage(color("&cBạn không có quyền."));
                    return true;
                }
                if (args.length < 3) {
                    sender.sendMessage(color("&cDùng: /" + label + " setdeploy <dd/MM/yyyy> <HH:mm:ss>"));
                    sender.sendMessage(color("&7Ví dụ: /" + label + " setdeploy 12/11/2024 20:16:08"));
                    return true;
                }
                String timeStr = args[1] + " " + args[2];
                // support if user typed in one arg with space? we already combine
                if (args.length > 3) {
                    // join remaining?
                    StringBuilder sb = new StringBuilder(args[1]);
                    for (int i = 2; i < args.length; i++) sb.append(" ").append(args[i]);
                    timeStr = sb.toString();
                }
                boolean ok = plugin.getConfigManager().setDeployTime(timeStr);
                if (ok) {
                    String tz = plugin.getConfigManager().getTimezone();
                    sender.sendMessage(color(plugin.getConfigManager().getMessage("setdeploy-success", "&aĐã đổi deploy thành %time%")
                            .replace("%time%", timeStr).replace("%timezone%", tz)));
                } else {
                    sender.sendMessage(color(plugin.getConfigManager().getMessage("setdeploy-invalid", "&cSai định dạng!")));
                }
                return true;
            case "set":
                if (!sender.hasPermission("checktimedeploy.admin")) {
                    sender.sendMessage(color("&cBạn không có quyền."));
                    return true;
                }
                if (args.length < 4) {
                    sender.sendMessage(color("&cDùng: /" + label + " set <player> <joins|quits> <số>"));
                    return true;
                }
                handleSet(sender, args[1], args[2], args[3]);
                return true;
            case "add":
                if (!sender.hasPermission("checktimedeploy.admin")) {
                    sender.sendMessage(color("&cBạn không có quyền."));
                    return true;
                }
                if (args.length < 4) {
                    sender.sendMessage(color("&cDùng: /" + label + " add <player> <joins|quits> <số>"));
                    return true;
                }
                handleAdd(sender, args[1], args[2], args[3]);
                return true;
            case "reset":
                if (!sender.hasPermission("checktimedeploy.admin")) {
                    sender.sendMessage(color("&cBạn không có quyền."));
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(color("&cDùng: /" + label + " reset <player|all>"));
                    return true;
                }
                handleReset(sender, args[1]);
                return true;
            case "help":
                sendHelp(sender, label);
                return true;
            default:
                // unknown sub -> show help or treat as player name?
                if (args.length == 1) {
                    // try player lookup as shortcut: /ctd <player>
                    PlayerData pd = plugin.getDatabaseManager().getPlayerDataByName(sub);
                    if (pd != null) {
                        showPlayer(sender, sub);
                        return true;
                    }
                }
                sendHelp(sender, label);
                return true;
        }
    }

    private void showMain(CommandSender sender) {
        Instant deploy = plugin.getConfigManager().getDeployInstant();
        Instant now = Instant.now();
        String tz = plugin.getConfigManager().getTimezone();
        String dateFmt = plugin.getConfigManager().getDateFormat();
        String deployStr = TimeUtil.formatInstant(deploy, dateFmt, tz);
        String nowStr = TimeUtil.formatInstant(now, dateFmt, tz);
        Duration sinceDeploy = TimeUtil.between(deploy, now);
        long totalDays = sinceDeploy.toDays();
        double totalDaysDouble = sinceDeploy.toSeconds() / 86400.0;

        // time in real: running since deploy
        String durationStr = TimeUtil.formatDuration(sinceDeploy);

        // fetch stats async but we are sync; use sync getters (they are synchronized fast)
        int unique = plugin.getDatabaseManager().getTotalUnique();
        long totalJoins = plugin.getDatabaseManager().getTotalJoins();
        long totalQuits = plugin.getDatabaseManager().getTotalQuits();
        int online = Bukkit.getOnlinePlayers().size();
        int max = Bukkit.getMaxPlayers();

        // personal stats if player
        String personal = "";
        if (sender instanceof Player p) {
            PlayerData pd = plugin.getDatabaseManager().getPlayerData(p.getUniqueId().toString());
            if (pd != null) {
                personal = color("\n&7 Bạn: &f" + pd.name + " &7- Vào &a" + pd.joinCount + " &7lần | Ra &c" + pd.quitCount + " &7lần\n&7 Vào đầu: &f" + TimeUtil.formatInstant(pd.firstJoin, dateFmt, tz));
            } else {
                personal = color("\n&7 Bạn: &f" + p.getName() + " &7- Chưa có dữ liệu");
            }
        }

        sender.sendMessage(color("&7&m--------------------------------"));
        sender.sendMessage(color("&b&l CheckTimeDeploy &7- &fby &aminhhaudev"));
        sender.sendMessage(color("&7 Phiên bản: &f" + plugin.getDescription().getVersion() + " &7| &fJava 21 &7| &f1.21.x"));
        sender.sendMessage(color("&7 Deploy: &a" + deployStr + " &7(" + tz + ")"));
        sender.sendMessage(color("&7 Hiện tại: &e" + nowStr));
        sender.sendMessage(color("&7 Tổng deploy: &f" + durationStr + " &7(" + String.format("%.2f", totalDaysDouble) + " ngày)"));
        sender.sendMessage(color("&7 Tổng ngày: &f" + totalDays + " ngày &7| &f" + sinceDeploy.toHours() + " giờ"));
        sender.sendMessage(color("&7&m------------------------------"));
        sender.sendMessage(color("&7 Tổng người từng vào: &f" + unique));
        sender.sendMessage(color("&7 Tổng lượt vào: &a" + totalJoins + " &7| &cTổng lượt ra: &c" + totalQuits));
        sender.sendMessage(color("&7 Đang online: &f" + online + "/" + max));
        if (!personal.isEmpty()) sender.sendMessage(personal);
        sender.sendMessage(color("&7&m--------------------------------"));
        sender.sendMessage(color("&7 Lệnh: &f/ctd player <tên> &7| &f/ctd top &7| &f/ctd help"));
    }

    private void showPlayer(CommandSender sender, String name) {
        PlayerData pd = plugin.getDatabaseManager().getPlayerDataByName(name);
        if (pd == null) {
            // try uuid?
            pd = plugin.getDatabaseManager().getPlayerData(name);
        }
        if (pd == null) {
            sender.sendMessage(color(plugin.getConfigManager().getMessage("player-not-found", "&cKhông tìm thấy %player%").replace("%player%", name)));
            return;
        }
        String tz = plugin.getConfigManager().getTimezone();
        String fmt = plugin.getConfigManager().getDateFormat();
        sender.sendMessage(color("&7&m--------------------------------"));
        sender.sendMessage(color("&b " + pd.name + " &7[" + pd.uuid.substring(0, 8) + "...]"));
        sender.sendMessage(color("&7 UUID: &f" + pd.uuid));
        sender.sendMessage(color("&7 Vào đầu: &a" + TimeUtil.formatInstant(pd.firstJoin, fmt, tz)));
        sender.sendMessage(color("&7 Lần cuối vào: &a" + TimeUtil.formatInstant(pd.lastJoin, fmt, tz)));
        if (pd.lastQuit != null) sender.sendMessage(color("&7 Lần cuối ra: &c" + TimeUtil.formatInstant(pd.lastQuit, fmt, tz)));
        else sender.sendMessage(color("&7 Lần cuối ra: &7Chưa ra"));
        sender.sendMessage(color("&7 Số lần vào: &a" + pd.joinCount));
        sender.sendMessage(color("&7 Số lần ra: &c" + pd.quitCount));
        sender.sendMessage(color("&7&m--------------------------------"));
    }

    private void showTop(CommandSender sender, String type) {
        boolean joins = !type.equalsIgnoreCase("quits") && !type.equalsIgnoreCase("quit");
        List<PlayerData> top = plugin.getDatabaseManager().getTopJoins(10);
        if (!joins) {
            // need quits sort - reuse but sort by quit_count; our getTopJoins sorts by join_count, so for quits we query manually? For now reuse but show joins top if quits requested we sort again
            top.sort((a, b) -> Integer.compare(b.quitCount, a.quitCount));
        }
        sender.sendMessage(color("&7&m--------------------------------"));
        sender.sendMessage(color("&b&l Top 10 &7- " + (joins ? "Lượt vào" : "Lượt ra")));
        int i = 1;
        for (PlayerData pd : top) {
            int val = joins ? pd.joinCount : pd.quitCount;
            sender.sendMessage(color("&7 #" + i + " &f" + pd.name + " &7- &a" + val + " &7lần"));
            i++;
        }
        if (top.isEmpty()) sender.sendMessage(color("&7 Chưa có dữ liệu"));
        sender.sendMessage(color("&7&m--------------------------------"));
    }

    private void handleSet(CommandSender sender, String player, String field, String valStr) {
        try {
            int val = Integer.parseInt(valStr);
            boolean ok = plugin.getDatabaseManager().setPlayerField(player, field, val);
            if (ok) sender.sendMessage(color("&aĐã set &e" + field + " &acủa &f" + player + " &athành &e" + val));
            else sender.sendMessage(color("&cKhông tìm thấy người chơi hoặc field không hợp lệ (joins/quits)"));
        } catch (NumberFormatException e) {
            sender.sendMessage(color("&cSố không hợp lệ: " + valStr));
        }
    }

    private void handleAdd(CommandSender sender, String player, String field, String valStr) {
        try {
            int delta = Integer.parseInt(valStr);
            boolean ok = plugin.getDatabaseManager().addPlayerField(player, field, delta);
            if (ok) sender.sendMessage(color("&aĐã cộng &e" + delta + " &avào &e" + field + " &acủa &f" + player));
            else sender.sendMessage(color("&cKhông tìm thấy người chơi hoặc field không hợp lệ"));
        } catch (NumberFormatException e) {
            sender.sendMessage(color("&cSố không hợp lệ: " + valStr));
        }
    }

    private void handleReset(CommandSender sender, String target) {
        if (target.equalsIgnoreCase("all")) {
            plugin.getDatabaseManager().resetAll();
            sender.sendMessage(color("&aĐã reset toàn bộ dữ liệu người chơi!"));
        } else {
            boolean ok = plugin.getDatabaseManager().resetPlayer(target);
            if (ok) sender.sendMessage(color("&aĐã reset dữ liệu của &f" + target));
            else sender.sendMessage(color("&cKhông tìm thấy " + target));
        }
    }

    private void sendHelp(CommandSender sender, String label) {
        sender.sendMessage(color("&7&m--------------------------------"));
        sender.sendMessage(color("&b&l CheckTimeDeploy Help &7by minhhaudev"));
        sender.sendMessage(color("&f/" + label + " &7- Xem tổng quan deploy & stats"));
        sender.sendMessage(color("&f/" + label + " player <tên> &7- Xem chi tiết người chơi"));
        sender.sendMessage(color("&f/" + label + " top [joins|quits] &7- Top 10"));
        sender.sendMessage(color("&f/" + label + " reload &7- Reload config"));
        sender.sendMessage(color("&c Admin:"));
        sender.sendMessage(color("&f/" + label + " setdeploy <dd/MM/yyyy> <HH:mm:ss> &7- Đổi ngày deploy"));
        sender.sendMessage(color("&f/" + label + " set <player> <joins|quits> <số>"));
        sender.sendMessage(color("&f/" + label + " add <player> <joins|quits> <số>"));
        sender.sendMessage(color("&f/" + label + " reset <player|all>"));
        sender.sendMessage(color("&7&m--------------------------------"));
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        List<String> ret = new ArrayList<>();
        if (args.length == 1) {
            List<String> subs = Arrays.asList("player", "top", "help", "reload", "setdeploy", "set", "add", "reset");
            for (String s : subs) if (s.startsWith(args[0].toLowerCase())) ret.add(s);
            // also player names as shortcut
            if (sender.hasPermission("checktimedeploy.use")) {
                for (Player p : Bukkit.getOnlinePlayers()) if (p.getName().toLowerCase().startsWith(args[0].toLowerCase())) ret.add(p.getName());
            }
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("player") || args[0].equalsIgnoreCase("set") || args[0].equalsIgnoreCase("add") || args[0].equalsIgnoreCase("reset")) {
                for (Player p : Bukkit.getOnlinePlayers()) if (p.getName().toLowerCase().startsWith(args[1].toLowerCase())) ret.add(p.getName());
                if (args[0].equalsIgnoreCase("reset")) ret.add("all");
            } else if (args[0].equalsIgnoreCase("top")) {
                if ("joins".startsWith(args[1].toLowerCase())) ret.add("joins");
                if ("quits".startsWith(args[1].toLowerCase())) ret.add("quits");
            }
        } else if (args.length == 3) {
            if (args[0].equalsIgnoreCase("set") || args[0].equalsIgnoreCase("add")) {
                if ("joins".startsWith(args[2].toLowerCase())) ret.add("joins");
                if ("quits".startsWith(args[2].toLowerCase())) ret.add("quits");
            }
        }
        return ret;
    }
}
