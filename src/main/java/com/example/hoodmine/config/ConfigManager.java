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
        ConfigurationSection phasesSection = config.getConfigurationSection("mine.phases");
        plugin.getLogger().info("Попытка загрузки секции mine.phases. Секция найдена: " + (phasesSection != null));
        if (phasesSection == null) {
            plugin.getLogger().warning("Секция mine.phases отсутствует в config.yml. Используется дефолтная фаза.");
            Map<String, Double> defaultSpawns = new HashMap<>();
            defaultSpawns.put("STONE", 1.0);
            phases.add(new Phase("<white>Дефолтная фаза", "<white>Дефолтная", defaultSpawns));
            return;
        }
        plugin.getLogger().info("Загружаю фазы из секции mine.phases. Количество ключей: " + phasesSection.getKeys(false).size());
        for (String key : phasesSection.getKeys(false)) {
            String name = phasesSection.getString(key + ".name");
            String displayName = phasesSection.getString(key + ".display_name");
            ConfigurationSection spawnsSection = phasesSection.getConfigurationSection(key + ".spawns");
            plugin.getLogger().info("Обработка фазы " + key + ". name=" + name + ", display_name=" + displayName + ", spawnsSection=" + (spawnsSection != null));
            if (name == null || displayName == null || spawnsSection == null) {
                plugin.getLogger().warning("Некорректные данные для фазы: " + key + ". Пропущена.");
                continue;
            }
            Map<String, Double> spawns = new HashMap<>();
            plugin.getLogger().info("Обрабатываю спавны для фазы " + key + ". Количество спавнов: " + spawnsSection.getKeys(false).size());
            for (String material : spawnsSection.getKeys(false)) {
                double weight = spawnsSection.getDouble(material);
                plugin.getLogger().info("Проверка материала " + material + " с весом " + weight);
                if (Material.getMaterial(material) != null) {
                    spawns.put(material, weight);
                    plugin.getLogger().info("Добавлен материал: " + material + " с весом " + weight + " для фазы " + key);
                } else {
                    plugin.getLogger().warning("Неверный материал в фазе " + key + ": " + material + ". Пропущен.");
                }
            }
            if (!spawns.isEmpty()) {
                phases.add(new Phase(name, displayName, spawns));
                plugin.getLogger().info("Фаза " + key + " успешно загружена с " + spawns.size() + " материалами.");
            } else {
                plugin.getLogger().warning("Список spawns для фазы " + key + " пуст. Фаза пропущена.");
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
        ConfigurationSection questsSection = config.getConfigurationSection("quests");
        if (questsSection == null) {
            plugin.getLogger().warning("Секция quests отсутствует в config.yml.");
            return;
        }
        for (String key : questsSection.getKeys(false)) {
            String id = questsSection.getString(key + ".id");
            String name = questsSection.getString(key + ".name");
            String description = questsSection.getString(key + ".description");
            String materialName = questsSection.getString(key + ".material");
            Material material = Material.getMaterial(materialName);
            int amount = questsSection.getInt(key + ".amount");
            if (id == null || name == null || material == null || amount <= 0) {
                plugin.getLogger().warning("Некорректные данные для квеста: " + key + ". Пропущен.");
                continue;
            }
            ConfigurationSection rewardSection = questsSection.getConfigurationSection(key + ".reward");
            double money = rewardSection != null ? rewardSection.getDouble("money", 0.0) : 0.0;
            int experience = rewardSection != null ? rewardSection.getInt("experience", 0) : 0;
            List<String> commands = rewardSection != null ? rewardSection.getStringList("commands") : new ArrayList<>();
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
        return messages.getString(key, "<red>Сообщение не найдено: " + key);
    }

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