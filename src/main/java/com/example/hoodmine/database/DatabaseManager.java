package com.example.hoodmine.database;

import com.example.hoodmine.HoodMinePlugin;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

// Класс для управления базой данных
public class DatabaseManager {
    private final HoodMinePlugin plugin;
    private Connection connection;

    public DatabaseManager(HoodMinePlugin plugin) {
        this.plugin = plugin;
        initializeDatabase();
    }

    private void initializeDatabase() {
        try {
            File dataFolder = plugin.getDataFolder();
            if (!dataFolder.exists()) {
                dataFolder.mkdirs();
            }
            String url = "jdbc:sqlite:" + new File(dataFolder, "database.db").getPath();
            connection = DriverManager.getConnection(url);
            try (PreparedStatement stmt = connection.prepareStatement(
                    "CREATE TABLE IF NOT EXISTS player_quests (player_uuid TEXT PRIMARY KEY, completed_quests INTEGER)")) {
                stmt.execute();
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Ошибка инициализации базы данных: " + e.getMessage());
        }
    }

    public void saveCompletedQuests(UUID playerId, int completedQuests) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "INSERT OR REPLACE INTO player_quests (player_uuid, completed_quests) VALUES (?, ?)")) {
            stmt.setString(1, playerId.toString());
            stmt.setInt(2, completedQuests);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Ошибка сохранения завершённых квестов: " + e.getMessage());
        }
    }

    public int getCompletedQuests(UUID playerId) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT completed_quests FROM player_quests WHERE player_uuid = ?")) {
            stmt.setString(1, playerId.toString());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("completed_quests");
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Ошибка загрузки завершённых квестов: " + e.getMessage());
        }
        return 0;
    }
}