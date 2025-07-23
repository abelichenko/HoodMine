package com.example.hoodmine;

import com.example.hoodmine.commands.HoodMineCommand;
import com.example.hoodmine.config.ConfigManager;
import com.example.hoodmine.database.DatabaseManager;
import com.example.hoodmine.quests.QuestManager;
import com.example.hoodmine.utils.HologramManager;
import com.example.hoodmine.utils.NPCManager;
import com.example.hoodmine.utils.RegionManager;
import org.bukkit.plugin.java.JavaPlugin;

// Основной класс плагина
public class HoodMinePlugin extends JavaPlugin {
    private ConfigManager configManager;
    private RegionManager regionManager;
    private QuestManager questManager;
    private DatabaseManager databaseManager;
    private HologramManager hologramManager;
    private NPCManager npcManager;

    @Override
    public void onEnable() {
        configManager = new ConfigManager(this);
        databaseManager = new DatabaseManager(this);
        regionManager = new RegionManager(this, configManager);
        questManager = new QuestManager(this, configManager, databaseManager, regionManager);
        hologramManager = new HologramManager(this, configManager);
        npcManager = new NPCManager(this);
        getCommand("hoodmine").setExecutor(new HoodMineCommand(this, configManager, regionManager, questManager));
        regionManager.startPhaseUpdateTask();
        getLogger().info("Плагин HoodMine включён!");
    }

    @Override
    public void onDisable() {
        databaseManager.closeConnection();
        getLogger().info("Плагин HoodMine выключен!");
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public RegionManager getRegionManager() {
        return regionManager;
    }

    public QuestManager getQuestManager() {
        return questManager;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public HologramManager getHologramManager() {
        return hologramManager;
    }

    public NPCManager getNPCManager() {
        return npcManager;
    }
}