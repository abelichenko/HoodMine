package com.example.hoodmine.quests;

import com.example.hoodmine.HoodMinePlugin;
import com.example.hoodmine.config.ConfigManager;
import com.example.hoodmine.database.DatabaseManager;
import com.example.hoodmine.utils.RegionManager;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Класс для управления квестами
public class QuestManager implements Listener {
    private final HoodMinePlugin plugin;
    private final ConfigManager configManager;
    private final DatabaseManager databaseManager;
    private final RegionManager regionManager;
    private final Map<UUID, Map<String, Integer>> playerProgress;
    private final Map<UUID, Integer> playerPages;
    private final Pattern hexPattern = Pattern.compile("^#([A-Fa-f0-9]{6})(.*)$");

    public QuestManager(HoodMinePlugin plugin, ConfigManager configManager, DatabaseManager databaseManager, RegionManager regionManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.databaseManager = databaseManager;
        this.regionManager = regionManager;
        this.playerProgress = new HashMap<>();
        this.playerPages = new HashMap<>();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    // Преобразование строки с HEX-кодом в формат MiniMessage
    private String formatTitleWithHex(String title) {
        Matcher matcher = hexPattern.matcher(title);
        if (matcher.matches()) {
            String hexCode = matcher.group(1);
            String text = matcher.group(2);
            return "<#" + hexCode + ">" + text;
        }
        return title; // Если нет HEX-кода, возвращаем исходную строку
    }

    // Открытие GUI квестов
    public void openQuestsGUI(Player player, int page) {
        ConfigManager.QuestsGUISettings guiSettings = configManager.getQuestsGUISettings();
        Inventory inventory = plugin.getServer().createInventory(null, guiSettings.getSize(), "Квесты");

        List<ConfigManager.Quest> quests = configManager.getQuests();
        int questsPerPage = guiSettings.getQuestsPerPage();
        int startIndex = page * questsPerPage;
        int endIndex = Math.min(startIndex + questsPerPage, quests.size());
        List<Integer> questSlots = guiSettings.getQuestSlots();

        // Заполнение слотов квестами
        for (int i = startIndex; i < endIndex && i < quests.size(); i++) {
            ConfigManager.Quest quest = quests.get(i);
            int slot = questSlots.get(i - startIndex);
            ItemStack item = new ItemStack(Material.valueOf(quest.getTargetBlock()));
            ItemMeta meta = item.getItemMeta();
            String formattedTitle = formatTitleWithHex(quest.getTitle());
            meta.setDisplayName(LegacyComponentSerializer.legacySection().serialize(
                    MiniMessage.miniMessage().deserialize(formattedTitle)
            ));
            List<String> lore = new ArrayList<>();
            lore.add(LegacyComponentSerializer.legacySection().serialize(
                    MiniMessage.miniMessage().deserialize("Требуется: " + quest.getAmount() + " " + quest.getDisplayName())
            ));
            lore.add(LegacyComponentSerializer.legacySection().serialize(
                    MiniMessage.miniMessage().deserialize("Награда: Выполнить команды")
            ));
            int progress = getPlayerProgress(player.getUniqueId(), quest.getId());
            lore.add(LegacyComponentSerializer.legacySection().serialize(
                    MiniMessage.miniMessage().deserialize("Прогресс: " + progress + "/" + quest.getAmount())
            ));
            meta.setLore(lore);
            item.setItemMeta(meta);
            inventory.setItem(slot, item);
        }

        // Кнопка "Следующая страница"
        if (endIndex < quests.size() && guiSettings.getNextPageSlot() >= 0) {
            ItemStack nextPageItem = new ItemStack(Material.valueOf(guiSettings.getNextPageMaterial()));
            ItemMeta nextMeta = nextPageItem.getItemMeta();
            nextMeta.setDisplayName(LegacyComponentSerializer.legacySection().serialize(
                    MiniMessage.miniMessage().deserialize(guiSettings.getNextPageName())
            ));
            nextPageItem.setItemMeta(nextMeta);
            inventory.setItem(guiSettings.getNextPageSlot(), nextPageItem);
        }

        // Кнопка "Предыдущая страница"
        if (page > 0 && guiSettings.getPrevPageSlot() >= 0) {
            ItemStack prevPageItem = new ItemStack(Material.valueOf(guiSettings.getPrevPageMaterial()));
            ItemMeta prevMeta = prevPageItem.getItemMeta();
            prevMeta.setDisplayName(LegacyComponentSerializer.legacySection().serialize(
                    MiniMessage.miniMessage().deserialize(guiSettings.getPrevPageName())
            ));
            prevPageItem.setItemMeta(prevMeta);
            inventory.setItem(guiSettings.getPrevPageSlot(), prevPageItem);
        }

        playerPages.put(player.getUniqueId(), page);
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
                        String formattedTitle = formatTitleWithHex(quest.getTitle());
                        player.sendMessage(LegacyComponentSerializer.legacySection().serialize(
                                MiniMessage.miniMessage().deserialize(
                                        configManager.getRawMessage("quest_completed"),
                                        Placeholder.unparsed("quest", LegacyComponentSerializer.legacySection().serialize(
                                                MiniMessage.miniMessage().deserialize(formattedTitle)
                                        ))
                                )
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
        if (!event.getView().getTitle().equals("Квесты")) return;
        event.setCancelled(true); // Блокируем взаимодействие
        if (!(event.getWhoClicked() instanceof Player player)) return;
        int slot = event.getRawSlot();
        ConfigManager.QuestsGUISettings guiSettings = configManager.getQuestsGUISettings();
        int currentPage = playerPages.getOrDefault(player.getUniqueId(), 0);

        if (slot == guiSettings.getNextPageSlot() && (currentPage + 1) * guiSettings.getQuestsPerPage() < configManager.getQuests().size()) {
            openQuestsGUI(player, currentPage + 1);
        } else if (slot == guiSettings.getPrevPageSlot() && currentPage > 0) {
            openQuestsGUI(player, currentPage - 1);
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