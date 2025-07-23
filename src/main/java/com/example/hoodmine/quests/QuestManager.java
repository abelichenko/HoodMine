package com.example.hoodmine.quests;

import com.example.hoodmine.HoodMinePlugin;
import com.example.hoodmine.config.ConfigManager;
import com.example.hoodmine.database.DatabaseManager;
import com.example.hoodmine.utils.RegionManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// Класс для управления квестами
public class QuestManager implements Listener {
    private final HoodMinePlugin plugin;
    private final ConfigManager configManager;
    private final DatabaseManager databaseManager;
    private final RegionManager regionManager;
    private final Map<UUID, Map<String, Integer>> playerProgress; // Прогресс игроков по квестам

    public QuestManager(HoodMinePlugin plugin, ConfigManager configManager, DatabaseManager databaseManager, RegionManager regionManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.databaseManager = databaseManager;
        this.regionManager = regionManager;
        this.playerProgress = new HashMap<>();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    // Открытие GUI квестов
    public void openQuestsGUI(Player player) {
        Inventory inventory = plugin.getServer().createInventory(null, 27, "Quests");
        for (ConfigManager.Quest quest : configManager.getQuests()) {
            ItemStack item = new ItemStack(Material.valueOf(quest.getTargetBlock()));
            ItemMeta meta = item.getItemMeta();
            // Преобразуем HEX-цвета в Legacy-формат для корректного отображения
            meta.setDisplayName(LegacyComponentSerializer.legacySection().serialize(
                    MiniMessage.miniMessage().deserialize(quest.getTitle())
            ));
            List<String> lore = new ArrayList<>();
            lore.add(LegacyComponentSerializer.legacySection().serialize(
                    MiniMessage.miniMessage().deserialize("Required: " + quest.getAmount() + " " + quest.getTargetBlock())
            ));
            lore.add(LegacyComponentSerializer.legacySection().serialize(
                    MiniMessage.miniMessage().deserialize("Reward: Execute commands")
            ));
            int progress = getPlayerProgress(player.getUniqueId(), quest.getId());
            lore.add(LegacyComponentSerializer.legacySection().serialize(
                    MiniMessage.miniMessage().deserialize("Progress: " + progress + "/" + quest.getAmount())
            ));
            meta.setLore(lore);
            item.setItemMeta(meta);
            inventory.addItem(item);
        }
        player.openInventory(inventory);
    }

    // Обработка добычи блока
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (regionManager.isInMineRegion(event.getBlock().getLocation())) {
            String material = event.getBlock().getType().name();
            for (ConfigManager.Quest quest : configManager.getQuests()) {
                if (quest.getTargetBlock().equals(material)) {
                    UUID playerId = player.getUniqueId();
                    String questId = quest.getId();
                    int progress = getPlayerProgress(playerId, questId) + 1;
                    updatePlayerProgress(playerId, questId, progress);
                    if (progress >= quest.getAmount()) {
                        double multiplier = calculateRewardMultiplier(player);
                        for (String command : quest.getReward()) {
                            String finalCommand = command.replace("%play%", player.getName());
                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCommand);
                        }
                        player.sendMessage(MiniMessage.miniMessage().deserialize(
                                configManager.getRawMessage("quest_completed"),
                                Placeholder.unparsed("quest", quest.getTitle())
                        ));
                        resetPlayerProgress(playerId, questId);
                    }
                }
            }
        }
    }

    // Обработка кликов по инвентарю
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTitle().equals("Quests")) {
            event.setCancelled(true); // Блокируем взаимодействие с предметами
        }
    }

    // Расчёт множителя наград
    public double calculateRewardMultiplier(Player player) {
        ConfigManager.RewardMultiplier multiplier = configManager.getRewardMultiplier();
        int completedQuests = databaseManager.getCompletedQuestsCount(player.getUniqueId());
        double calculated = multiplier.getBase() + (completedQuests * multiplier.getPerQuest());
        return Math.min(calculated, multiplier.getMax());
    }

    // Получение прогресса игрока
    private int getPlayerProgress(UUID playerId, String questId) {
        return playerProgress.computeIfAbsent(playerId, k -> new HashMap<>()).getOrDefault(questId, 0);
    }

    // Обновление прогресса игрока
    private void updatePlayerProgress(UUID playerId, String questId, int progress) {
        playerProgress.computeIfAbsent(playerId, k -> new HashMap<>()).put(questId, progress);
        databaseManager.updatePlayerProgress(playerId, questId, progress);
    }

    // Сброс прогресса игрока
    private void resetPlayerProgress(UUID playerId, String questId) {
        playerProgress.computeIfAbsent(playerId, k -> new HashMap<>()).put(questId, 0);
        databaseManager.updatePlayerProgress(playerId, questId, 0);
    }
}