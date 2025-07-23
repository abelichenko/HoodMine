package com.example.hoodmine.config;

import com.example.hoodmine.HoodMinePlugin;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// Класс для управления конфигурацией
public class ConfigManager {
    private final HoodMinePlugin plugin;
    private FileConfiguration config;
    private FileConfiguration messages;
    private List<Phase> phases;
    private List<Quest> quests;
    private Map<String, Double> sellPrices;
    private RewardMultiplier rewardMultiplier;
    private QuestsGUISettings questsGUISettings;

    public ConfigManager(HoodMinePlugin plugin) {
        this.plugin = plugin;
        loadConfig();
        loadMessages();
    }

    // Загрузка конфигурации
    private void loadConfig() {
        plugin.saveDefaultConfig();
        config = plugin.getConfig();
        loadPhases();
        loadQuests();
        loadSellPrices();
        loadRewardMultiplier();
        loadQuestsGUISettings();
    }

    // Загрузка messages.yml
    private void loadMessages() {
        File messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        messages = YamlConfiguration.loadConfiguration(messagesFile);
    }

    // Получение сообщения
    public String getMessage(String key) {
        return messages.getString(key, "<red>Сообщение не найдено: " + key);
    }

    // Получение сырого сообщения
    public String getRawMessage(String key) {
        return messages.getString(key, "<red>Сообщение не найдено: " + key);
    }

    // Установка имени шахты
    public void setMineName(String name) {
        config.set("mine_name", name);
        plugin.saveConfig();
    }

    // Получение имени шахты
    public String getMineName() {
        return config.getString("mine_name", "HoodMine");
    }

    // Получение интервала таймера
    public long getTimerInterval() {
        return config.getLong("timer_interval", 600);
    }

    // Загрузка фаз
    private void loadPhases() {
        phases = new ArrayList<>();
        ConfigurationSection phasesSection = config.getConfigurationSection("phases");
        if (phasesSection != null) {
            for (String phaseId : phasesSection.getKeys(false)) {
                ConfigurationSection phaseSection = phasesSection.getConfigurationSection(phaseId);
                if (phaseSection != null) {
                    String displayName = phaseSection.getString("display_name", phaseId);
                    long duration = phaseSection.getLong("duration", 600);
                    Map<String, Double> spawns = new HashMap<>();
                    ConfigurationSection spawnsSection = phaseSection.getConfigurationSection("spawns");
                    if (spawnsSection != null) {
                        for (String material : spawnsSection.getKeys(false)) {
                            spawns.put(material, spawnsSection.getDouble(material));
                        }
                    }
                    phases.add(new Phase(phaseId, displayName, duration, spawns));
                }
            }
        }
    }

    // Загрузка квестов
    private void loadQuests() {
        quests = new ArrayList<>();
        ConfigurationSection questsSection = config.getConfigurationSection("quests");
        if (questsSection != null) {
            for (String questId : questsSection.getKeys(false)) {
                ConfigurationSection questSection = questsSection.getConfigurationSection(questId);
                if (questSection != null) {
                    String title = questSection.getString("title", questId);
                    String targetBlock = questSection.getString("target_block", "STONE");
                    String displayName = questSection.getString("display_name", targetBlock);
                    int amount = questSection.getInt("amount", 1);
                    List<String> reward = questSection.getStringList("reward");
                    quests.add(new Quest(questId, title, targetBlock, displayName, amount, reward));
                }
            }
        }
    }

    // Загрузка цен продажи
    private void loadSellPrices() {
        sellPrices = new HashMap<>();
        ConfigurationSection pricesSection = config.getConfigurationSection("sell_prices");
        if (pricesSection != null) {
            for (String material : pricesSection.getKeys(false)) {
                sellPrices.put(material, pricesSection.getDouble(material));
            }
        }
    }

    // Загрузка множителя наград
    private void loadRewardMultiplier() {
        ConfigurationSection multiplierSection = config.getConfigurationSection("reward_multiplier");
        if (multiplierSection != null) {
            double base = multiplierSection.getDouble("base", 1.0);
            double perQuest = multiplierSection.getDouble("per_quest", 0.1);
            double max = multiplierSection.getDouble("max", 2.0);
            rewardMultiplier = new RewardMultiplier(base, perQuest, max);
        } else {
            rewardMultiplier = new RewardMultiplier(1.0, 0.1, 2.0);
        }
    }

    // Загрузка настроек GUI квестов
    private void loadQuestsGUISettings() {
        ConfigurationSection guiSection = config.getConfigurationSection("quests_gui");
        if (guiSection != null) {
            int size = guiSection.getInt("size", 27);
            List<Integer> questSlots = guiSection.getIntegerList("quest_slots");
            int questsPerPage = guiSection.getInt("quests_per_page", 9);
            int nextPageSlot = guiSection.getInt("next_page_slot", -1);
            int prevPageSlot = guiSection.getInt("prev_page_slot", -1);
            String nextPageMaterial = guiSection.getString("next_page_material", "ARROW");
            String prevPageMaterial = guiSection.getString("prev_page_material", "ARROW");
            String nextPageName = guiSection.getString("next_page_name", "<green>Next Page");
            String prevPageName = guiSection.getString("prev_page_name", "<green>Previous Page");
            questsGUISettings = new QuestsGUISettings(size, questSlots, questsPerPage, nextPageSlot, prevPageSlot,
                    nextPageMaterial, prevPageMaterial, nextPageName, prevPageName);
        } else {
            questsGUISettings = new QuestsGUISettings(27, List.of(10, 11, 12, 13, 14, 15, 16, 19, 20), 9, 26, 18,
                    "ARROW", "ARROW", "<green>Next Page", "<green>Previous Page");
        }
    }

    // Получение фаз
    public List<Phase> getPhases() {
        return phases;
    }

    // Получение квестов
    public List<Quest> getQuests() {
        return quests;
    }

    // Получение цен продажи
    public Map<String, Double> getSellPrices() {
        return sellPrices;
    }

    // Получение множителя наград
    public RewardMultiplier getRewardMultiplier() {
        return rewardMultiplier;
    }

    // Получение настроек GUI квестов
    public QuestsGUISettings getQuestsGUISettings() {
        return questsGUISettings;
    }

    // Класс для хранения данных о фазе
    public static class Phase {
        private final String id;
        private final String displayName;
        private final long duration;
        private final Map<String, Double> spawns;

        public Phase(String id, String displayName, long duration, Map<String, Double> spawns) {
            this.id = id;
            this.displayName = displayName;
            this.duration = duration;
            this.spawns = spawns;
        }

        public String getId() {
            return id;
        }

        public String getDisplayName() {
            return displayName;
        }

        public long getDuration() {
            return duration;
        }

        public Map<String, Double> getSpawns() {
            return spawns;
        }
    }

    // Класс для хранения данных о квесте
    public static class Quest {
        private final String id;
        private final String title;
        private final String targetBlock;
        private final String displayName;
        private final int amount;
        private final List<String> reward;

        public Quest(String id, String title, String targetBlock, String displayName, int amount, List<String> reward) {
            this.id = id;
            this.title = title;
            this.targetBlock = targetBlock;
            this.displayName = displayName;
            this.amount = amount;
            this.reward = reward;
        }

        public String getId() {
            return id;
        }

        public String getTitle() {
            return title;
        }

        public String getTargetBlock() {
            return targetBlock;
        }

        public String getDisplayName() {
            return displayName;
        }

        public int getAmount() {
            return amount;
        }

        public List<String> getReward() {
            return reward;
        }
    }

    // Класс для хранения множителя наград
    public static class RewardMultiplier {
        private final double base;
        private final double perQuest;
        private final double max;

        public RewardMultiplier(double base, double perQuest, double max) {
            this.base = base;
            this.perQuest = perQuest;
            this.max = max;
        }

        public double getBase() {
            return base;
        }

        public double getPerQuest() {
            return perQuest;
        }

        public double getMax() {
            return max;
        }
    }

    // Класс для хранения настроек GUI квестов
    public static class QuestsGUISettings {
        private final int size;
        private final List<Integer> questSlots;
        private final int questsPerPage;
        private final int nextPageSlot;
        private final int prevPageSlot;
        private final String nextPageMaterial;
        private final String prevPageMaterial;
        private final String nextPageName;
        private final String prevPageName;

        public QuestsGUISettings(int size, List<Integer> questSlots, int questsPerPage, int nextPageSlot, int prevPageSlot,
                                 String nextPageMaterial, String prevPageMaterial, String nextPageName, String prevPageName) {
            this.size = size;
            this.questSlots = questSlots;
            this.questsPerPage = questsPerPage;
            this.nextPageSlot = nextPageSlot;
            this.prevPageSlot = prevPageSlot;
            this.nextPageMaterial = nextPageMaterial;
            this.prevPageMaterial = prevPageMaterial;
            this.nextPageName = nextPageName;
            this.prevPageName = prevPageName;
        }

        public int getSize() {
            return size;
        }

        public List<Integer> getQuestSlots() {
            return questSlots;
        }

        public int getQuestsPerPage() {
            return questsPerPage;
        }

        public int getNextPageSlot() {
            return nextPageSlot;
        }

        public int getPrevPageSlot() {
            return prevPageSlot;
        }

        public String getNextPageMaterial() {
            return nextPageMaterial;
        }

        public String getPrevPageMaterial() {
            return prevPageMaterial;
        }

        public String getNextPageName() {
            return nextPageName;
        }

        public String getPrevPageName() {
            return prevPageName;
        }
    }
}