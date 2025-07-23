package com.example.hoodmine.config;

import com.example.hoodmine.HoodMinePlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Класс для управления конфигурацией плагина
public class ConfigManager {
    private final HoodMinePlugin plugin; // Ссылка на плагин
    private FileConfiguration config; // Конфигурация config.yml
    private FileConfiguration messages; // Конфигурация messages.yml
    private final File configFile; // Файл config.yml
    private final File messagesFile; // Файл messages.yml
    private final List<Phase> phases; // Список фаз шахты
    private final List<Quest> quests; // Список квестов
    private String mineName; // Название шахты
    private int timerInterval; // Интервал таймера (в секундах)
    private final Map<String, Double> sellPrices; // Цены продажи руд
    private final RewardMultiplier rewardMultiplier; // Множитель наград

    public ConfigManager(HoodMinePlugin plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), "config.yml");
        this.messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        this.phases = new ArrayList<>();
        this.quests = new ArrayList<>();
        this.sellPrices = new HashMap<>();
        this.mineName = "HoodMine";
        this.timerInterval = 600;
        this.rewardMultiplier = new RewardMultiplier(1.0, 0.1, 2.0);

        // Загрузка конфигураций
        loadConfig();
        loadMessages();
        loadPhases();
        loadQuests();
        loadSellPrices();
    }

    // Загрузка config.yml
    private void loadConfig() {
        if (!configFile.exists()) {
            plugin.saveResource("config.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(configFile);
        mineName = config.getString("mine_name", "HoodMine");
        timerInterval = config.getInt("timer_interval", 600);

        // Загрузка множителя наград
        ConfigurationSection multiplierSection = config.getConfigurationSection("reward_multiplier");
        if (multiplierSection != null) {
            double base = multiplierSection.getDouble("base", 1.0);
            double perQuest = multiplierSection.getDouble("per_quest", 0.1);
            double max = multiplierSection.getDouble("max", 2.0);
            this.rewardMultiplier.setBase(base);
            this.rewardMultiplier.setPerQuest(perQuest);
            this.rewardMultiplier.setMax(max);
        }

        // Проверяем и создаём секции, если они отсутствуют
        if (!config.isConfigurationSection("phases")) {
            createDefaultPhases();
        }
        if (!config.isConfigurationSection("quests")) {
            createDefaultQuests();
        }
        if (!config.isConfigurationSection("sell_prices")) {
            createDefaultSellPrices();
        }
        saveConfig();
    }

    // Загрузка messages.yml
    private void loadMessages() {
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        messages = YamlConfiguration.loadConfiguration(messagesFile);
    }

    // Загрузка фаз шахты
    private void loadPhases() {
        phases.clear();
        ConfigurationSection phasesSection = config.getConfigurationSection("phases");
        if (phasesSection == null) {
            plugin.getLogger().warning("No phases found in config.yml! Using default phases.");
            createDefaultPhases();
            phasesSection = config.getConfigurationSection("phases");
        }

        for (String key : phasesSection.getKeys(false)) {
            ConfigurationSection phaseSection = phasesSection.getConfigurationSection(key);
            if (phaseSection != null) {
                String displayName = phaseSection.getString("display_name", "Фаза " + key);
                int duration = phaseSection.getInt("duration", 3600);
                ConfigurationSection spawnsSection = phaseSection.getConfigurationSection("spawns");
                Map<String, Double> spawns = new HashMap<>();
                if (spawnsSection != null) {
                    for (String material : spawnsSection.getKeys(false)) {
                        spawns.put(material, spawnsSection.getDouble(material, 0.0));
                    }
                }
                phases.add(new Phase(key, displayName, spawns, duration));
            }
        }
    }

    // Загрузка квестов
    private void loadQuests() {
        quests.clear();
        ConfigurationSection questsSection = config.getConfigurationSection("quests");
        if (questsSection == null) {
            plugin.getLogger().warning("No quests found in config.yml! Using default quests.");
            createDefaultQuests();
            questsSection = config.getConfigurationSection("quests");
        }

        for (String key : questsSection.getKeys(false)) {
            ConfigurationSection questSection = questsSection.getConfigurationSection(key);
            if (questSection != null) {
                String title = questSection.getString("title", "Квест " + key);
                String targetBlock = questSection.getString("target_block", "STONE");
                int amount = questSection.getInt("amount", 100);
                List<String> reward = questSection.getStringList("reward");
                quests.add(new Quest(key, title, targetBlock, amount, reward));
            }
        }
    }

    // Загрузка цен продажи
    private void loadSellPrices() {
        sellPrices.clear();
        ConfigurationSection pricesSection = config.getConfigurationSection("sell_prices");
        if (pricesSection == null) {
            plugin.getLogger().warning("No sell prices found in config.yml! Using default prices.");
            createDefaultSellPrices();
            pricesSection = config.getConfigurationSection("sell_prices");
        }

        for (String material : pricesSection.getKeys(false)) {
            sellPrices.put(material, pricesSection.getDouble(material, 0.0));
        }
    }

    // Создание стандартных фаз
    private void createDefaultPhases() {
        ConfigurationSection phasesSection = config.createSection("phases");
        ConfigurationSection common = phasesSection.createSection("common");
        common.set("display_name", "#AAAAAA[Обычная Фаза]");
        common.set("duration", 1800);
        ConfigurationSection commonSpawns = common.createSection("spawns");
        commonSpawns.set("COAL_ORE", 0.30);
        commonSpawns.set("IRON_ORE", 0.15);
        commonSpawns.set("GOLD_ORE", 0.05);

        ConfigurationSection rare = phasesSection.createSection("rare");
        rare.set("display_name", "#55FFFF[Редкая Фаза]");
        rare.set("duration", 1200);
        ConfigurationSection rareSpawns = rare.createSection("spawns");
        rareSpawns.set("COAL_ORE", 0.10);
        rareSpawns.set("IRON_ORE", 0.20);
        rareSpawns.set("GOLD_ORE", 0.10);
        rareSpawns.set("DIAMOND_ORE", 0.02);

        ConfigurationSection epic = phasesSection.createSection("epic");
        epic.set("display_name", "#AA00AA[Эпическая Фаза]");
        epic.set("duration", 600);
        ConfigurationSection epicSpawns = epic.createSection("spawns");
        epicSpawns.set("IRON_ORE", 0.10);
        epicSpawns.set("GOLD_ORE", 0.10);
        epicSpawns.set("DIAMOND_ORE", 0.05);
        epicSpawns.set("EMERALD_ORE", 0.01);

        saveConfig();
    }

    // Создание стандартных квестов
    private void createDefaultQuests() {
        ConfigurationSection questsSection = config.createSection("quests");
        ConfigurationSection quest1 = questsSection.createSection("quest1");
        quest1.set("title", "#FFFF55Сломать 1000 алмазной руды");
        quest1.set("target_block", "DIAMOND_ORE");
        quest1.set("amount", 1000);
        quest1.set("reward", Arrays.asList("eco give %play% 10101010"));
        saveConfig();
    }

    // Создание стандартных цен продажи
    private void createDefaultSellPrices() {
        ConfigurationSection pricesSection = config.createSection("sell_prices");
        pricesSection.set("DIAMOND_ORE", 50);
        pricesSection.set("IRON_ORE", 10);
        pricesSection.set("GOLD_ORE", 30);
        saveConfig();
    }

    // Сохранение config.yml
    public void saveConfig() {
        try {
            config.save(configFile);
        } catch (Exception e) {
            plugin.getLogger().severe("Ошибка при сохранении config.yml: " + e.getMessage());
        }
    }

    // Получение сообщения с поддержкой HEX-цветов
    public Component getMessage(String key) {
        String message = messages.getString(key, "Сообщение не найдено: " + key);
        return MiniMessage.miniMessage().deserialize(message);
    }

    // Получение сырого текста сообщения для внешней обработки
    public String getRawMessage(String key) {
        return messages.getString(key, "Сообщение не найдено: " + key);
    }

    // Получение списка фаз
    public List<Phase> getPhases() {
        return phases;
    }

    // Получение списка квестов
    public List<Quest> getQuests() {
        return quests;
    }

    // Получение интервала таймера
    public int getTimerInterval() {
        return timerInterval;
    }

    // Получение цен продажи
    public Map<String, Double> getSellPrices() {
        return sellPrices;
    }

    // Получение множителя наград
    public RewardMultiplier getRewardMultiplier() {
        return rewardMultiplier;
    }

    // Установка названия шахты
    public void setMineName(String name) {
        this.mineName = name;
        config.set("mine_name", name);
        saveConfig();
    }

    // Получение названия шахты
    public String getMineName() {
        return mineName;
    }

    // Внутренний класс для представления фазы шахты
    public static class Phase {
        private final String id;
        private final String displayName;
        private final Map<String, Double> spawns;
        private final int duration;

        public Phase(String id, String displayName, Map<String, Double> spawns, int duration) {
            this.id = id;
            this.displayName = displayName;
            this.spawns = spawns;
            this.duration = duration;
        }

        public String getId() {
            return id;
        }

        public String getDisplayName() {
            return displayName;
        }

        public Map<String, Double> getSpawns() {
            return spawns;
        }

        public int getDuration() {
            return duration;
        }
    }

    // Внутренний класс для представления квеста
    public static class Quest {
        private final String id;
        private final String title;
        private final String targetBlock;
        private final int amount;
        private final List<String> reward;

        public Quest(String id, String title, String targetBlock, int amount, List<String> reward) {
            this.id = id;
            this.title = title;
            this.targetBlock = targetBlock;
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

        public int getAmount() {
            return amount;
        }

        public List<String> getReward() {
            return reward;
        }
    }

    // Внутренний класс для множителя наград
    public static class RewardMultiplier {
        private double base;
        private double perQuest;
        private double max;

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

        public void setBase(double base) {
            this.base = base;
        }

        public void setPerQuest(double perQuest) {
            this.perQuest = perQuest;
        }

        public void setMax(double max) {
            this.max = max;
        }
    }
}