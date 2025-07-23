package com.example.hoodmine.config;

import com.example.hoodmine.HoodMinePlugin;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
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
    }

    private void loadConfig() {
        plugin.saveDefaultConfig();
        config = plugin.getConfig();
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
        if (phasesSection == null) {
            plugin.getLogger().warning("Секция mine.phases отсутствует в config.yml. Используется дефолтная фаза.");
            // Добавляем дефолтную фазу, чтобы избежать пустого списка
            Map<String, Double> defaultSpawns = new HashMap<>();
            defaultSpawns.put("STONE", 1.0);
            phases.add(new Phase("<white>Дефолтная фаза", "<white>Дефолтная", defaultSpawns));
            return;
        }
        for (String key : phasesSection.getKeys(false)) {
            String name = phasesSection.getString(key + ".name");
            String displayName = phasesSection.getString(key + ".display_name");
            Map<String, Double> spawns = new HashMap<>();
            ConfigurationSection spawnsSection = phasesSection.getConfigurationSection(key + ".spawns");
            if (spawnsSection != null) {
                for (String material : spawnsSection.getKeys(false)) {
                    spawns.put(material, spawnsSection.getDouble(material));
                }
            } else {
                plugin.getLogger().warning("Секция spawns отсутствует для фазы: " + key);
            }
            if (name != null && displayName != null) {
                phases.add(new Phase(name, displayName, spawns));
            } else {
                plugin.getLogger().warning("Некорректные данные для фазы: " + key);
            }
        }
        if (phases.isEmpty()) {
            plugin.getLogger().warning("Список фаз пуст. Добавляется дефолтная фаза.");
            Map<String, Double> defaultSpawns = new HashMap<>();
            defaultSpawns.put("STONE", 1.0);
            phases.add(new Phase("<white>Дефолтная фаза", "<white>Дефолтная", defaultSpawns));
        }
    }

    private void loadSellerItems() {
        sellerItems.clear();
        ConfigurationSection sellerSection = config.getConfigurationSection("seller.items");
        if (sellerSection != null) {
            for (String key : sellerSection.getKeys(false)) {
                String materialName = sellerSection.getString(key + ".material");
                Material material = Material.getMaterial(materialName);
                if (material == null) {
                    plugin.getLogger().warning("Неверный материал для товара скупщика: " + materialName);
                    continue;
                }
                double price = sellerSection.getDouble(key + ".price");
                String displayName = sellerSection.getString(key + ".display_name");
                List<String> lore = sellerSection.getStringList(key + ".lore");
                sellerItems.put(key, new SellerItem(material, price, displayName, lore));
            }
        }
    }

    private void loadQuests() {
        quests.clear();
        ConfigurationSection questsSection = config.getConfigurationSection("quests");
        if (questsSection != null) {
            for (String key : questsSection.getKeys(false)) {
                String id = questsSection.getString(key + ".id");
                String name = questsSection.getString(key + ".name");
                String description = questsSection.getString(key + ".description");
                String materialName = questsSection.getString(key + ".material");
                Material material = Material.getMaterial(materialName);
                if (material == null) {
                    plugin.getLogger().warning("Неверный материал для квеста: " + materialName);
                    continue;
                }
                int amount = questsSection.getInt(key + ".amount");
                ConfigurationSection rewardSection = questsSection.getConfigurationSection(key + ".reward");
                double money = rewardSection != null ? rewardSection.getDouble("money") : 0.0;
                int experience = rewardSection != null ? rewardSection.getInt("experience") : 0;
                List<String> commands = rewardSection != null ? rewardSection.getStringList("commands") : new ArrayList<>();
                quests.add(new Quest(id, name, description, material, amount, money, experience, commands));
            }
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