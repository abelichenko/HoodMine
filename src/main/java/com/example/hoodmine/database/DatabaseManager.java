package com.example.hoodmine.database;

import com.example.hoodmine.HoodMinePlugin;
import com.example.hoodmine.config.ConfigManager;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

// Класс для управления базой данных SQLite
public class DatabaseManager {
    private final HoodMinePlugin plugin;
    private Connection connection;

    public DatabaseManager(HoodMinePlugin plugin) {
        this.plugin = plugin;
        initializeDatabase();
    }

    // Инициализация базы данных
    private void initializeDatabase() {
        try {
            // Подключение к SQLite
            String path = plugin.getDataFolder() + "/data.db";
            connection = DriverManager.getConnection("jdbc:sqlite:" + path);
            // Создание таблицы для прогресса квестов
            String createTableSQL = "CREATE TABLE IF NOT EXISTS player_quests (" +
                    "player_uuid TEXT NOT NULL, " +
                    "quest_id TEXT NOT NULL, " +
                    "progress INTEGER NOT NULL, " +
                    "completed INTEGER NOT NULL DEFAULT 0, " +
                    "PRIMARY KEY (player_uuid, quest_id))";
            try (PreparedStatement stmt = connection.prepareStatement(createTableSQL)) {
                stmt.execute();
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Ошибка инициализации базы данных: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Обновление прогресса игрока
    public void updatePlayerProgress(UUID playerId, String questId, int progress) {
        try {
            String sql = "INSERT OR REPLACE INTO player_quests (player_uuid, quest_id, progress, completed) " +
                    "VALUES (?, ?, ?, (SELECT completed FROM player_quests WHERE player_uuid = ? AND quest_id = ?))";
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, playerId.toString());
                stmt.setString(2, questId);
                stmt.setInt(3, progress);
                stmt.setString(4, playerId.toString());
                stmt.setString(5, questId);
                stmt.executeUpdate();
            }
            // Проверка достижения максимального прогресса для квеста
            int questAmount = plugin.getConfigManager().getQuests().stream()
                    .filter(q -> q.getId().equals(questId))
                    .findFirst()
                    .map(ConfigManager.Quest::getAmount)
                    .orElse(0);
            if (progress >= questAmount) {
                String updateCompletedSQL = "UPDATE player_quests SET completed = completed + 1 " +
                        "WHERE player_uuid = ? AND quest_id = ?";
                try (PreparedStatement stmt = connection.prepareStatement(updateCompletedSQL)) {
                    stmt.setString(1, playerId.toString());
                    stmt.setString(2, questId);
                    stmt.executeUpdate();
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Ошибка обновления прогресса: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Получение количества завершённых квестов
    public int getCompletedQuestsCount(UUID playerId) {
        try {
            String sql = "SELECT SUM(completed) AS total FROM player_quests WHERE player_uuid = ?";
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, playerId.toString());
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    return rs.getInt("total");
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Ошибка получения количества завершённых квестов: " + e.getMessage());
            e.printStackTrace();
        }
        return 0;
    }

    // Закрытие соединения с базой данных
    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Ошибка закрытия базы данных: " + e.getMessage());
            e.printStackTrace();
        }
    }
}