package com.example.hoodmine;

import com.example.hoodmine.commands.HoodMineCommand;
import com.example.hoodmine.config.ConfigManager;
import com.example.hoodmine.database.DatabaseManager;
import com.example.hoodmine.quests.QuestManager;
import com.example.hoodmine.utils.RegionManager;
import org.bukkit.plugin.java.JavaPlugin;

// Основной класс плагина, инициализирующий все компоненты
public class HoodMinePlugin extends JavaPlugin {
    private ConfigManager configManager; // Менеджер конфигурации
    private DatabaseManager databaseManager; // Менеджер базы данных
    private RegionManager regionManager; // Менеджер региона шахты
    private QuestManager questManager; // Менеджер квестов

    @Override
    public void onEnable() {
        try {
            // Инициализация менеджеров
            configManager = new ConfigManager(this);
            databaseManager = new DatabaseManager(this);
            regionManager = new RegionManager(this, configManager);
            questManager = new QuestManager(this, configManager, databaseManager, regionManager);

            // Регистрация команды /hoodmine
            getCommand("hoodmine").setExecutor(new HoodMineCommand(this, configManager, regionManager, questManager));

            // Запуск задачи обновления фаз шахты
            regionManager.startPhaseUpdateTask();
        } catch (Exception e) {
            getLogger().severe("Ошибка при инициализации плагина: " + e.getMessage());
            e.printStackTrace();
            setEnabled(false); // Отключаем плагин при ошибке
        }
    }

    @Override
    public void onDisable() {
        // Закрытие соединения с базой данных при отключении плагина
        if (databaseManager != null) {
            databaseManager.closeConnection();
        }
    }

    // Геттеры для доступа к менеджерам
    public ConfigManager getConfigManager() {
        return configManager;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public RegionManager getRegionManager() {
        return regionManager;
    }

    public QuestManager getQuestManager() {
        return questManager;
    }
}