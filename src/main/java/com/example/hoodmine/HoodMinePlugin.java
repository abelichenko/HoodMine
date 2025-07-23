package com.example.hoodmine;

import com.example.hoodmine.commands.HoodMineCommand;
import com.example.hoodmine.config.ConfigManager;
import com.example.hoodmine.database.DatabaseManager;
import com.example.hoodmine.quests.QuestManager;
import com.example.hoodmine.utils.HologramManager;
import com.example.hoodmine.utils.PlaceholderHook;
import com.example.hoodmine.utils.RegionManager;
import org.bukkit.plugin.java.JavaPlugin;

// Основной класс плагина
public class HoodMinePlugin extends JavaPlugin {
    private ConfigManager configManager;
    private RegionManager regionManager;
    private DatabaseManager databaseManager;
    private QuestManager questManager;
    private HologramManager hologramManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        configManager = new ConfigManager(this);
        databaseManager = new DatabaseManager(this);
        regionManager = new RegionManager(this, configManager, databaseManager);
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

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public RegionManager getRegionManager() {
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
}