package com.example.hoodmine;

import com.example.hoodmine.commands.HoodMineCommand;
import com.example.hoodmine.config.ConfigManager;
import com.example.hoodmine.database.DatabaseManager;
import com.example.hoodmine.quests.QuestManager;
import com.example.hoodmine.utils.HologramManager;
import com.example.hoodmine.utils.PlaceholderHook;
import com.example.hoodmine.utils.RegionManagerMine;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.plugin.java.JavaPlugin;

// Основной класс плагина
public class HoodMinePlugin extends JavaPlugin implements Listener {
    private ConfigManager configManager;
    private RegionManagerMine regionManager;
    private DatabaseManager databaseManager;
    private QuestManager questManager;
    private HologramManager hologramManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        configManager = new ConfigManager(this);
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("HoodMine v1.0 включён!");
        databaseManager = new DatabaseManager(this);
        regionManager = new RegionManagerMine(this, configManager, databaseManager);
        questManager = new QuestManager(this, configManager, databaseManager, regionManager);
        hologramManager = new HologramManager(this, configManager, regionManager);
        getCommand("hoodmine").setExecutor(new HoodMineCommand(this, configManager, regionManager, questManager));

        // Регистрация плейсхолдеров в PlaceholderAPI
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new PlaceholderHook(this, configManager, regionManager).register();
            getLogger().info("PlaceholderAPI подключен, плейсхолдеры зарегистрированы.");
        } else {
            getLogger().warning("PlaceholderAPI не найден, плейсхолдеры не будут работать в других плагинах.");
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        String playerName = event.getPlayer().getName();
        String materialName = event.getBlock().getType().toString();
        for (ConfigManager.Quest quest : configManager.getQuests()) {
            if (quest.getMaterial().toString().equals(materialName)) {
                // Здесь нужно обновить прогресс игрока
                getLogger().info(playerName + " добыл " + materialName + ", квест: " + quest.getName());
                // Пример: увеличиваем прогресс (нужно реализовать хранение прогресса)
                // int currentProgress = getPlayerProgress(playerName, quest.getId());
                // setPlayerProgress(playerName, quest.getId(), currentProgress + 1);
            }
        }
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public RegionManagerMine getRegionManager() {
        return regionManager;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public QuestManager getQuestManager() {
        return questManager;
    }

    public HologramManager getHologramManager() {
        return hologramManager;
    }

    // Методы для хранения прогресса (пример, нужно реализовать)
    private int getPlayerProgress(String playerName, String questId) {
        // Логика получения прогресса (например, из файла или базы данных)
        return 0; // Заменить на реальную логику
    }

    private void setPlayerProgress(String playerName, String questId, int progress) {
        // Логика сохранения прогресса
        getLogger().info(playerName + " обновлён прогресс квеста " + questId + " до " + progress);
    }
}