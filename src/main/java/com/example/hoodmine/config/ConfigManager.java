package com.example.hoodmine.config;

import com.example.hoodmine.HoodMinePlugin;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Класс для управления конфигурацией
public class ConfigManager {
    private final HoodMinePlugin plugin;
    private FileConfiguration config;
    private FileConfiguration messages;
    private final List<Phase> phases = new ArrayList<>();
    private final Map<String, SellerItem> sellerItems = new HashMap<>();
    private final List<Quest> quests = new ArrayList<>();
    private double rewardMultiplierBase;
    private double rewardMultiplierPerQuest;
    private double rewardMultiplierMax;
    private boolean showBossBar;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public ConfigManager(HoodMinePlugin plugin) {
        this.plugin = plugin;
        loadConfig();
        loadMessages();
        loadPhases();
        loadSellerItems();
        loadQuests();
        loadRewardMultiplier();
        plugin.getLogger().info("Конфигурация загружена. Фаз: " + phases.size() + ", Квестов: " + quests.size());
    }

    private void loadConfig() {
        plugin.saveDefaultConfig();
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            plugin.saveResource("config.yml", false);
            plugin.getLogger().info("Создан новый config.yml, так как он отсутствовал.");
        }
        config = YamlConfiguration.loadConfiguration(configFile);
        plugin.getLogger().info("Загружен config.yml из: " + configFile.getAbsolutePath());
        plugin.getLogger().info("Содержимое config.yml: " + config.saveToString());
        showBossBar = config.getBoolean("settings.show_bossbar", true);
    }

    private void loadMessages() {
        File messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        messages = YamlConfiguration.loadConfiguration(messagesFile);
    }

    private void loadPhases() {
        phases.clear();
        List<?> phaseList = config.getList("mine.phases");
        plugin.getLogger().info("Попытка загрузки списка mine.phases. Список найден: " + (phaseList != null) + ", размер: " + (phaseList != null ? phaseList.size() : 0));
        if (phaseList == null || phaseList.isEmpty()) {
            plugin.getLogger().warning("Список mine.phases отсутствует или пуст. Используется дефолтная фаза.");
            Map<String, Double> defaultSpawns = new HashMap<>();
            defaultSpawns.put("STONE", 1.0);
            phases.add(new Phase("<white>Дефолтная фаза", "<white>Дефолтная", defaultSpawns));
            return;
        }
        for (Object obj : phaseList) {
            if (!(obj instanceof Map)) {
                plugin.getLogger().warning("Элемент списка mine.phases не является мапой. Пропущен: " + obj);
                continue;
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> phaseMap = (Map<String, Object>) obj;
            String name = (String) phaseMap.get("name");
            String displayName = (String) phaseMap.get("display_name");
            @SuppressWarnings("unchecked")
            Map<String, Object> spawnsMap = (Map<String, Object>) phaseMap.get("spawns");
            plugin.getLogger().info("Обработка фазы. name=" + name + ", display_name=" + displayName + ", spawnsMap=" + (spawnsMap != null));
            if (name == null || displayName == null || spawnsMap == null) {
                plugin.getLogger().warning("Некорректные данные фазы. Пропущена.");
                continue;
            }
            Map<String, Double> spawns = new HashMap<>();
            for (Map.Entry<String, Object> entry : spawnsMap.entrySet()) {
                if (entry.getValue() instanceof Number) {
                    double weight = ((Number) entry.getValue()).doubleValue();
                    if (Material.getMaterial(entry.getKey()) != null) {
                        spawns.put(entry.getKey(), weight);
                        plugin.getLogger().info("Добавлен материал: " + entry.getKey() + " с весом " + weight);
                    } else {
                        plugin.getLogger().warning("Неверный материал: " + entry.getKey() + ". Пропущен.");
                    }
                } else {
                    plugin.getLogger().warning("Некорректный вес для материала " + entry.getKey() + ": " + entry.getValue() + ". Пропущен.");
                }
            }
            if (!spawns.isEmpty()) {
                phases.add(new Phase(name, displayName, spawns));
                plugin.getLogger().info("Фаза успешно загружена с " + spawns.size() + " материалами.");
            }
        }
        if (phases.isEmpty()) {
            plugin.getLogger().warning("Список фаз пуст. Добавлена дефолтная фаза.");
            Map<String, Double> defaultSpawns = new HashMap<>();
            defaultSpawns.put("STONE", 1.0);
            phases.add(new Phase("<white>Дефолтная фаза", "<white>Дефолтная", defaultSpawns));
        }
    }

    private void loadSellerItems() {
        sellerItems.clear();
        ConfigurationSection sellerSection = config.getConfigurationSection("seller.items");
        if (sellerSection == null) {
            plugin.getLogger().warning("Секция seller.items отсутствует в config.yml.");
            return;
        }
        for (String key : sellerSection.getKeys(false)) {
            String materialName = sellerSection.getString(key + ".material");
            Material material = Material.getMaterial(materialName);
            if (material == null) {
                plugin.getLogger().warning("Неверный материал для товара скупщика: " + materialName + " в " + key);
                continue;
            }
            double price = sellerSection.getDouble(key + ".price");
            String displayName = sellerSection.getString(key + ".display_name");
            List<String> lore = sellerSection.getStringList(key + ".lore");
            sellerItems.put(key, new SellerItem(material, price, displayName, lore));
            plugin.getLogger().info("Загружен товар скупщика: " + key + " (материал: " + materialName + ")");
        }
    }

    private void loadQuests() {
        quests.clear();
        List<?> questList = config.getList("quests");
        plugin.getLogger().info("Попытка загрузки списка quests. Список найден: " + (questList != null) + ", размер: " + (questList != null ? questList.size() : 0));
        if (questList == null || questList.isEmpty()) {
            plugin.getLogger().warning("Список quests отсутствует или пуст.");
            return;
        }
        for (Object obj : questList) {
            if (!(obj instanceof Map)) {
                plugin.getLogger().warning("Элемент списка quests не является мапой. Пропущен: " + obj);
                continue;
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> questMap = (Map<String, Object>) obj;
            String id = (String) questMap.get("id");
            String name = (String) questMap.get("name");
            String description = (String) questMap.get("description");
            String materialName = (String) questMap.get("material");
            Material material = Material.getMaterial(materialName);
            Integer amount = (questMap.get("amount") instanceof Number) ? ((Number) questMap.get("amount")).intValue() : null;
            plugin.getLogger().info("Обработка квеста. id=" + id + ", name=" + name + ", material=" + materialName + ", amount=" + amount);
            if (id == null || name == null || material == null || amount == null || amount <= 0) {
                plugin.getLogger().warning("Некорректные данные квеста. Пропущен.");
                continue;
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> rewardMap = (Map<String, Object>) questMap.get("reward");
            double money = rewardMap != null && rewardMap.get("money") instanceof Number ? ((Number) rewardMap.get("money")).doubleValue() : 0.0;
            int experience = rewardMap != null && rewardMap.get("experience") instanceof Number ? ((Number) rewardMap.get("experience")).intValue() : 0;
            @SuppressWarnings("unchecked")
            List<String> commands = rewardMap != null ? (List<String>) rewardMap.getOrDefault("commands", new ArrayList<>()) : new ArrayList<>();
            quests.add(new Quest(id, name, description, material, amount, money, experience, commands));
            plugin.getLogger().info("Загружен квест: " + id + " (материал: " + materialName + ", количество: " + amount + ")");
        }
        if (quests.isEmpty()) {
            plugin.getLogger().warning("Список квестов пуст.");
        }
    }

    private void loadRewardMultiplier() {
        ConfigurationSection multiplierSection = config.getConfigurationSection("reward_multiplier");
        if (multiplierSection != null) {
            rewardMultiplierBase = multiplierSection.getDouble("base", 1.0);
            rewardMultiplierPerQuest = multiplierSection.getDouble("per_quest", 0.1);
            rewardMultiplierMax = multiplierSection.getDouble("max", 2.0);
        } else {
            rewardMultiplierBase = 1.0;
            rewardMultiplierPerQuest = 0.1;
            rewardMultiplierMax = 2.0;
            plugin.getLogger().warning("Секция reward_multiplier отсутствует в config.yml. Используются значения по умолчанию.");
        }
    }

    public String getMineName() {
        return config.getString("mine.name", "Шахта");
    }

    public long getTimerInterval() {
        return config.getLong("mine.timer_interval", 300);
    }

    public List<Phase> getPhases() {
        return phases;
    }

    public Map<String, SellerItem> getSellerItems() {
        return sellerItems;
    }

    public List<Quest> getQuests() {
        return quests;
    }

    public String getRawMessage(String key) {
        String raw = messages.getString(key, "<red>Сообщение не найдено: " + key);
        return miniMessage.serialize(miniMessage.deserialize(raw));
    }

    public String getPhaseDisplayName(String phaseName) {
        for (Phase phase : phases) {
            if (phase.getName().equals(phaseName)) {
                return miniMessage.serialize(miniMessage.deserialize(phase.getDisplayName()));
            }
        }
        return phaseName;
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public boolean isShowBossBar() {
        return showBossBar;
    }

    // Добавление недостающих геттеров для множителей
    public double getRewardMultiplierBase() {
        return rewardMultiplierBase;
    }

    public double getRewardMultiplierPerQuest() {
        return rewardMultiplierPerQuest;
    }

    public double getRewardMultiplierMax() {
        return rewardMultiplierMax;
    }

    public static class Phase {
        private final String name;
        private final String displayName;
        private final Map<String, Double> spawns;

        public Phase(String name, String displayName, Map<String, Double> spawns) {
            this.name = name;
            this.displayName = displayName;
            this.spawns = spawns;
        }

        public String getName() {
            return name;
        }

        public String getDisplayName() {
            return displayName;
        }

        public Map<String, Double> getSpawns() {
            return spawns;
        }
    }

    public static class SellerItem {
        private final Material material;
        private final double price;
        private final String displayName;
        private final List<String> lore;

        public SellerItem(Material material, double price, String displayName, List<String> lore) {
            this.material = material;
            this.price = price;
            this.displayName = displayName;
            this.lore = lore;
        }

        public Material getMaterial() {
            return material;
        }

        public double getPrice() {
            return price;
        }

        public String getDisplayName() {
            return displayName;
        }

        public List<String> getLore() {
            return lore;
        }
    }

    public static class Quest {
        private final String id;
        private final String name;
        private final String description;
        private final Material material;
        private final int amount;
        private final double money;
        private final int experience;
        private final List<String> commands;

        public Quest(String id, String name, String description, Material material, int amount, double money, int experience, List<String> commands) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.material = material;
            this.amount = amount;
            this.money = money;
            this.experience = experience;
            this.commands = commands;
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        public Material getMaterial() {
            return material;
        }

        public int getAmount() {
            return amount;
        }

        public double getMoney() {
            return money;
        }

        public int getExperience() {
            return experience;
        }

        public List<String> getCommands() {
            return commands;
        }
    }
}