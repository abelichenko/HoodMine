package com.example.hoodmine.config;

import com.example.hoodmine.HoodMinePlugin;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Класс для управления конфигурацией плагина
public class ConfigManager {
    private final HoodMinePlugin plugin;
    private FileConfiguration config;
    private FileConfiguration messages;
    private File configFile;
    private File messagesFile;

    public ConfigManager(HoodMinePlugin plugin) {
        this.plugin = plugin;
        loadConfig();
        loadMessages();
    }

    // Загрузка config.yml
    private void loadConfig() {
        configFile = new File(plugin.getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            plugin.saveResource("config.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(configFile);
    }

    // Загрузка messages.yml
    private void loadMessages() {
        messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        messages = YamlConfiguration.loadConfiguration(messagesFile);
    }

    // Получение названия шахты
    public String getMineName() {
        return config.getString("mine_name", "Шахта");
    }

    // Установка названия шахты
    public void setMineName(String name) {
        config.set("mine_name", name);
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Ошибка сохранения config.yml: " + e.getMessage());
        }
    }

    // Получение списка фаз
    public List<Phase> getPhases() {
        List<Phase> phases = new ArrayList<>();
        ConfigurationSection phasesSection = config.getConfigurationSection("phases");
        if (phasesSection == null) return phases;
        for (String key : phasesSection.getKeys(false)) {
            String displayName = phasesSection.getString(key + ".display_name");
            ConfigurationSection spawnsSection = phasesSection.getConfigurationSection(key + ".spawns");
            Map<String, Double> spawns = new HashMap<>();
            if (spawnsSection != null) {
                for (String material : spawnsSection.getKeys(false)) {
                    spawns.put(material, spawnsSection.getDouble(material));
                }
            }
            phases.add(new Phase(key, displayName, spawns));
        }
        return phases;
    }

    // Получение интервала таймера
    public long getTimerInterval() {
        return config.getLong("timer_interval", 600);
    }

    // Получение множителя наград
    public RewardMultiplier getRewardMultiplier() {
        ConfigurationSection section = config.getConfigurationSection("reward_multiplier");
        if (section == null) {
            return new RewardMultiplier(1.0, 0.1, 2.0);
        }
        return new RewardMultiplier(
                section.getDouble("base", 1.0),
                section.getDouble("per_quest", 0.1),
                section.getDouble("max", 2.0)
        );
    }

    // Получение цен продажи
    public Map<String, Double> getSellPrices() {
        Map<String, Double> sellPrices = new HashMap<>();
        ConfigurationSection section = config.getConfigurationSection("sell_prices");
        if (section != null) {
            for (String material : section.getKeys(false)) {
                sellPrices.put(material, section.getDouble(material));
            }
        }
        return sellPrices;
    }

    // Получение списка квестов
    public List<Quest> getQuests() {
        List<Quest> quests = new ArrayList<>();
        ConfigurationSection questsSection = config.getConfigurationSection("quests");
        if (questsSection == null) return quests;
        for (String key : questsSection.getKeys(false)) {
            String id = questsSection.getString(key + ".id");
            String title = questsSection.getString(key + ".title");
            String targetBlock = questsSection.getString(key + ".target_block");
            String displayName = questsSection.getString(key + ".display_name");
            int amount = questsSection.getInt(key + ".amount");
            List<String> reward = questsSection.getStringList(key + ".reward");
            quests.add(new Quest(id, title, targetBlock, displayName, amount, reward));
        }
        return quests;
    }

    // Получение настроек GUI квестов
    public QuestsGUISettings getQuestsGUISettings() {
        ConfigurationSection section = config.getConfigurationSection("quests_gui");
        if (section == null) {
            return new QuestsGUISettings(27, new ArrayList<>(), -1, "ARROW", "<green>Следующая страница", -1, "ARROW", "<green>Предыдущая страница");
        }
        List<Integer> questSlots = section.getIntegerList("quest_slots");
        int size = section.getInt("size", 27);
        int nextPageSlot = section.getInt("next_page_slot", -1);
        String nextPageMaterial = section.getString("next_page_material", "ARROW");
        String nextPageName = section.getString("next_page_name", "<green>Следующая страница");
        int prevPageSlot = section.getInt("prev_page_slot", -1);
        String prevPageMaterial = section.getString("prev_page_material", "ARROW");
        String prevPageName = section.getString("prev_page_name", "<green>Предыдущая страница");
        return new QuestsGUISettings(size, questSlots, nextPageSlot, nextPageMaterial, nextPageName, prevPageSlot, prevPageMaterial, prevPageName);
    }

    // Получение настроек GUI скупщика
    public SellerGUISettings getSellerGUISettings() {
        ConfigurationSection section = config.getConfigurationSection("seller_gui");
        if (section == null) {
            return new SellerGUISettings(27, new ArrayList<>(), -1, "ARROW", "<green>Следующая страница", -1, "ARROW", "<green>Предыдущая страница");
        }
        List<Integer> itemSlots = section.getIntegerList("item_slots");
        int size = section.getInt("size", 27);
        int nextPageSlot = section.getInt("next_page_slot", -1);
        String nextPageMaterial = section.getString("next_page_material", "ARROW");
        String nextPageName = section.getString("next_page_name", "<green>Следующая страница");
        int prevPageSlot = section.getInt("prev_page_slot", -1);
        String prevPageMaterial = section.getString("prev_page_material", "ARROW");
        String prevPageName = section.getString("prev_page_name", "<green>Предыдущая страница");
        return new SellerGUISettings(size, itemSlots, nextPageSlot, nextPageMaterial, nextPageName, prevPageSlot, prevPageMaterial, prevPageName);
    }

    // Получение списка предметов для продажи
    public List<SellItem> getSellItems() {
        List<SellItem> sellItems = new ArrayList<>();
        ConfigurationSection itemsSection = config.getConfigurationSection("sell_items");
        if (itemsSection == null) return sellItems;
        for (String key : itemsSection.getKeys(false)) {
            String material = itemsSection.getString(key + ".material");
            String displayName = itemsSection.getString(key + ".display_name");
            double price = itemsSection.getDouble(key + ".price");
            int amount = itemsSection.getInt(key + ".amount");
            List<String> commands = itemsSection.getStringList(key + ".commands");
            sellItems.add(new SellItem(material, displayName, price, amount, commands));
        }
        return sellItems;
    }

    // Получение сообщения из messages.yml
    public String getRawMessage(String key) {
        return messages.getString(key, "Сообщение не найдено: " + key);
    }

    // Структура для фаз
    public static class Phase {
        private final String id;
        private final String displayName;
        private final Map<String, Double> spawns;

        public Phase(String id, String displayName, Map<String, Double> spawns) {
            this.id = id;
            this.displayName = displayName;
            this.spawns = spawns;
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
    }

    // Структура для множителя наград
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

    // Структура для квестов
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

    // Структура для настроек GUI квестов
    public static class QuestsGUISettings {
        private final int size;
        private final List<Integer> questSlots;
        private final int nextPageSlot;
        private final String nextPageMaterial;
        private final String nextPageName;
        private final int prevPageSlot;
        private final String prevPageMaterial;
        private final String prevPageName;

        public QuestsGUISettings(int size, List<Integer> questSlots, int nextPageSlot, String nextPageMaterial, String nextPageName,
                                 int prevPageSlot, String prevPageMaterial, String prevPageName) {
            this.size = size;
            this.questSlots = questSlots;
            this.nextPageSlot = nextPageSlot;
            this.nextPageMaterial = nextPageMaterial;
            this.nextPageName = nextPageName;
            this.prevPageSlot = prevPageSlot;
            this.prevPageMaterial = prevPageMaterial;
            this.prevPageName = prevPageName;
        }

        public int getSize() {
            return size;
        }

        public List<Integer> getQuestSlots() {
            return questSlots;
        }

        public int getQuestsPerPage() {
            return questSlots.size();
        }

        public int getNextPageSlot() {
            return nextPageSlot;
        }

        public String getNextPageMaterial() {
            return nextPageMaterial;
        }

        public String getNextPageName() {
            return nextPageName;
        }

        public int getPrevPageSlot() {
            return prevPageSlot;
        }

        public String getPrevPageMaterial() {
            return prevPageMaterial;
        }

        public String getPrevPageName() {
            return prevPageName;
        }
    }

    // Структура для настроек GUI скупщика
    public static class SellerGUISettings {
        private final int size;
        private final List<Integer> itemSlots;
        private final int nextPageSlot;
        private final String nextPageMaterial;
        private final String nextPageName;
        private final int prevPageSlot;
        private final String prevPageMaterial;
        private final String prevPageName;

        public SellerGUISettings(int size, List<Integer> itemSlots, int nextPageSlot, String nextPageMaterial, String nextPageName,
                                 int prevPageSlot, String prevPageMaterial, String prevPageName) {
            this.size = size;
            this.itemSlots = itemSlots;
            this.nextPageSlot = nextPageSlot;
            this.nextPageMaterial = nextPageMaterial;
            this.nextPageName = nextPageName;
            this.prevPageSlot = prevPageSlot;
            this.prevPageMaterial = prevPageMaterial;
            this.prevPageName = prevPageName;
        }

        public int getSize() {
            return size;
        }

        public List<Integer> getItemSlots() {
            return itemSlots;
        }

        public int getItemsPerPage() {
            return itemSlots.size();
        }

        public int getNextPageSlot() {
            return nextPageSlot;
        }

        public String getNextPageMaterial() {
            return nextPageMaterial;
        }

        public String getNextPageName() {
            return nextPageName;
        }

        public int getPrevPageSlot() {
            return prevPageSlot;
        }

        public String getPrevPageMaterial() {
            return prevPageMaterial;
        }

        public String getPrevPageName() {
            return prevPageName;
        }
    }

    // Структура для предметов продажи
    public static class SellItem {
        private final String material;
        private final String displayName;
        private final double price;
        private final int amount;
        private final List<String> commands;

        public SellItem(String material, String displayName, double price, int amount, List<String> commands) {
            this.material = material;
            this.displayName = displayName;
            this.price = price;
            this.amount = amount;
            this.commands = commands;
        }

        public String getMaterial() {
            return material;
        }

        public String getDisplayName() {
            return displayName;
        }

        public double getPrice() {
            return price;
        }

        public int getAmount() {
            return amount;
        }

        public List<String> getCommands() {
            return commands;
        }
    }
}