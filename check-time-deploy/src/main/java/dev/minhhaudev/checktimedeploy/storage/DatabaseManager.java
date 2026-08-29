package dev.minhhaudev.checktimedeploy.storage;

import dev.minhhaudev.checktimedeploy.CheckTimeDeploy;
import org.bukkit.entity.Player;

import java.io.File;
import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

public class DatabaseManager {

    private final CheckTimeDeploy plugin;
    private File dbFile;
    private Connection connection;

    public DatabaseManager(CheckTimeDeploy plugin) {
        this.plugin = plugin;
    }

    public synchronized void init() {
        try {
            String fileName = plugin.getConfigManager().getDatabaseFile();
            dbFile = new File(plugin.getDataFolder(), fileName);
            if (!dbFile.getParentFile().exists()) dbFile.getParentFile().mkdirs();

            Class.forName("org.sqlite.JDBC");
            String url = "jdbc:sqlite:" + dbFile.getAbsolutePath();
            connection = DriverManager.getConnection(url);
            try (Statement st = connection.createStatement()) {
                st.execute("PRAGMA journal_mode=WAL;");
                st.execute("PRAGMA synchronous=NORMAL;");
            }
            createTables();
            plugin.getLogger().info("Đã kết nối SQLite: " + dbFile.getName());
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Lỗi khởi tạo SQLite", e);
        }
    }

    private void createTables() throws SQLException {
        try (Statement st = connection.createStatement()) {
            st.executeUpdate("CREATE TABLE IF NOT EXISTS player_stats (" +
                    "uuid TEXT PRIMARY KEY," +
                    "name TEXT NOT NULL," +
                    "first_join INTEGER NOT NULL," +
                    "last_join INTEGER NOT NULL," +
                    "last_quit INTEGER," +
                    "join_count INTEGER NOT NULL DEFAULT 0," +
                    "quit_count INTEGER NOT NULL DEFAULT 0" +
                    ");");
        }
    }

    public synchronized void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                plugin.getLogger().info("Đã đóng SQLite.");
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Lỗi đóng SQLite", e);
        }
    }

    private synchronized Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            String url = "jdbc:sqlite:" + dbFile.getAbsolutePath();
            connection = DriverManager.getConnection(url);
        }
        return connection;
    }

    public synchronized void handleJoin(Player player) {
        try {
            String uuid = player.getUniqueId().toString();
            String name = player.getName();
            long now = Instant.now().toEpochMilli();

            PlayerData existing = getPlayerData(uuid);
            if (existing == null) {
                try (PreparedStatement ps = getConnection().prepareStatement(
                        "INSERT INTO player_stats(uuid,name,first_join,last_join,join_count,quit_count) VALUES(?,?,?,?,?,?)")) {
                    ps.setString(1, uuid);
                    ps.setString(2, name);
                    ps.setLong(3, now);
                    ps.setLong(4, now);
                    ps.setInt(5, 1);
                    ps.setInt(6, 0);
                    ps.executeUpdate();
                }
            } else {
                try (PreparedStatement ps = getConnection().prepareStatement(
                        "UPDATE player_stats SET name=?, last_join=?, join_count=join_count+1 WHERE uuid=?")) {
                    ps.setString(1, name);
                    ps.setLong(2, now);
                    ps.setString(3, uuid);
                    ps.executeUpdate();
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Lỗi handleJoin " + player.getName(), e);
        }
    }

    public synchronized void handleQuit(Player player) {
        try {
            String uuid = player.getUniqueId().toString();
            long now = Instant.now().toEpochMilli();
            // ensure exists
            PlayerData existing = getPlayerData(uuid);
            if (existing == null) {
                // create if not exists (edge)
                try (PreparedStatement ps = getConnection().prepareStatement(
                        "INSERT INTO player_stats(uuid,name,first_join,last_join,last_quit,join_count,quit_count) VALUES(?,?,?,?,?,?,?)")) {
                    ps.setString(1, uuid);
                    ps.setString(2, player.getName());
                    ps.setLong(3, now);
                    ps.setLong(4, now);
                    ps.setLong(5, now);
                    ps.setInt(6, 1);
                    ps.setInt(7, 1);
                    ps.executeUpdate();
                    return;
                }
            }
            try (PreparedStatement ps = getConnection().prepareStatement(
                    "UPDATE player_stats SET last_quit=?, quit_count=quit_count+1 WHERE uuid=?")) {
                ps.setLong(1, now);
                ps.setString(2, uuid);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Lỗi handleQuit " + player.getName(), e);
        }
    }

    public synchronized PlayerData getPlayerData(String uuid) {
        try (PreparedStatement ps = getConnection().prepareStatement("SELECT * FROM player_stats WHERE uuid=?")) {
            ps.setString(1, uuid);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Lỗi getPlayerData", e);
        }
        return null;
    }

    public synchronized PlayerData getPlayerDataByName(String name) {
        try (PreparedStatement ps = getConnection().prepareStatement("SELECT * FROM player_stats WHERE lower(name)=lower(?) ORDER BY last_join DESC LIMIT 1")) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Lỗi getPlayerDataByName", e);
        }
        return null;
    }

    private PlayerData mapRow(ResultSet rs) throws SQLException {
        String uuid = rs.getString("uuid");
        String name = rs.getString("name");
        long first = rs.getLong("first_join");
        long lastJoin = rs.getLong("last_join");
        long lastQuit = rs.getLong("last_quit");
        boolean wasNull = rs.wasNull();
        int joins = rs.getInt("join_count");
        int quits = rs.getInt("quit_count");
        Instant firstI = Instant.ofEpochMilli(first);
        Instant lastJ = Instant.ofEpochMilli(lastJoin);
        Instant lastQ = wasNull ? null : Instant.ofEpochMilli(lastQuit);
        return new PlayerData(uuid, name, firstI, lastJ, lastQ, joins, quits);
    }

    public synchronized int getTotalUnique() {
        try (Statement st = getConnection().createStatement(); ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM player_stats")) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Lỗi getTotalUnique", e);
        }
        return 0;
    }

    public synchronized long getTotalJoins() {
        try (Statement st = getConnection().createStatement(); ResultSet rs = st.executeQuery("SELECT COALESCE(SUM(join_count),0) FROM player_stats")) {
            if (rs.next()) return rs.getLong(1);
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Lỗi getTotalJoins", e);
        }
        return 0;
    }

    public synchronized long getTotalQuits() {
        try (Statement st = getConnection().createStatement(); ResultSet rs = st.executeQuery("SELECT COALESCE(SUM(quit_count),0) FROM player_stats")) {
            if (rs.next()) return rs.getLong(1);
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Lỗi getTotalQuits", e);
        }
        return 0;
    }

    public synchronized List<PlayerData> getTopJoins(int limit) {
        List<PlayerData> list = new ArrayList<>();
        try (PreparedStatement ps = getConnection().prepareStatement("SELECT * FROM player_stats ORDER BY join_count DESC, last_join DESC LIMIT ?")) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Lỗi getTopJoins", e);
        }
        return list;
    }

    public synchronized boolean setPlayerField(String uuidOrName, String field, int value) {
        String uuid = null;
        PlayerData pd = getPlayerData(uuidOrName);
        if (pd != null) uuid = pd.uuid;
        else {
            pd = getPlayerDataByName(uuidOrName);
            if (pd != null) uuid = pd.uuid;
        }
        if (uuid == null) return false;
        String col;
        if (field.equalsIgnoreCase("joins") || field.equalsIgnoreCase("join_count") || field.equalsIgnoreCase("join")) col = "join_count";
        else if (field.equalsIgnoreCase("quits") || field.equalsIgnoreCase("quit_count") || field.equalsIgnoreCase("quit")) col = "quit_count";
        else return false;
        try (PreparedStatement ps = getConnection().prepareStatement("UPDATE player_stats SET " + col + "=? WHERE uuid=?")) {
            ps.setInt(1, value);
            ps.setString(2, uuid);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Lỗi setPlayerField", e);
            return false;
        }
    }

    public synchronized boolean addPlayerField(String uuidOrName, String field, int delta) {
        String uuid = null;
        PlayerData pd = getPlayerData(uuidOrName);
        if (pd != null) uuid = pd.uuid;
        else {
            pd = getPlayerDataByName(uuidOrName);
            if (pd != null) uuid = pd.uuid;
        }
        if (uuid == null) return false;
        String col;
        if (field.equalsIgnoreCase("joins") || field.equalsIgnoreCase("join_count")) col = "join_count";
        else if (field.equalsIgnoreCase("quits") || field.equalsIgnoreCase("quit_count")) col = "quit_count";
        else return false;
        try (PreparedStatement ps = getConnection().prepareStatement("UPDATE player_stats SET " + col + "=" + col + "+? WHERE uuid=?")) {
            ps.setInt(1, delta);
            ps.setString(2, uuid);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Lỗi addPlayerField", e);
            return false;
        }
    }

    public synchronized boolean resetPlayer(String uuidOrName) {
        String uuid = null;
        PlayerData pd = getPlayerData(uuidOrName);
        if (pd != null) uuid = pd.uuid;
        else {
            pd = getPlayerDataByName(uuidOrName);
            if (pd != null) uuid = pd.uuid;
        }
        if (uuid == null) return false;
        try (PreparedStatement ps = getConnection().prepareStatement("DELETE FROM player_stats WHERE uuid=?")) {
            ps.setString(1, uuid);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Lỗi resetPlayer", e);
            return false;
        }
    }

    public synchronized void resetAll() {
        try (Statement st = getConnection().createStatement()) {
            st.executeUpdate("DELETE FROM player_stats");
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Lỗi resetAll", e);
        }
    }
}
