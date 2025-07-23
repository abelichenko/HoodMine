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
        return title;
    }

    // Открытие GUI квестов
    public void openQuestsGUI(Player player, int page) {
        ConfigManager.QuestsGUISettings guiSettings = configManager.getQuestsGUISettings();
        String mineName = configManager.getMineName();
        String phaseName = regionManager.getCurrentPhase() != null ? regionManager.getCurrentPhase().getDisplayName() : "Не установлена";
        String timeToNext = String.valueOf(regionManager.getTimeToNextPhase());
        Inventory inventory = plugin.getServer().createInventory(null, guiSettings.getSize(),
                LegacyComponentSerializer.legacySection().serialize(MiniMessage.miniMessage().deserialize(
                        "Квесты",
                        Placeholder.unparsed("mine_name", mineName),
                        Placeholder.unparsed("mine_phase", phaseName),
                        Placeholder.unparsed("time_to_next", timeToNext)
                )));

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
                    MiniMessage.miniMessage().deserialize(formattedTitle,
                            Placeholder.unparsed("mine_name", mineName),
                            Placeholder.unparsed("mine_phase", phaseName),
                            Placeholder.unparsed("time_to_next", timeToNext))
            ));
            List<String> lore = new ArrayList<>();
            lore.add(LegacyComponentSerializer.legacySection().serialize(
                    MiniMessage.miniMessage().deserialize("Требуется: " + quest.getAmount() + " " + quest.getDisplayName(),
                            Placeholder.unparsed("mine_name", mineName),
                            Placeholder.unparsed("mine_phase", phaseName),
                            Placeholder.unparsed("time_to_next", timeToNext))
            ));
            lore.add(LegacyComponentSerializer.legacySection().serialize(
                    MiniMessage.miniMessage().deserialize("Награда: Выполнить команды",
                            Placeholder.unparsed("mine_name", mineName),
                            Placeholder.unparsed("mine_phase", phaseName),
                            Placeholder.unparsed("time_to_next", timeToNext))
            ));
            int progress = getPlayerProgress(player.getUniqueId(), quest.getId());
            lore.add(LegacyComponentSerializer.legacySection().serialize(
                    MiniMessage.miniMessage().deserialize("Прогресс: " + progress + "/" + quest.getAmount(),
                            Placeholder.unparsed("mine_name", mineName),
                            Placeholder.unparsed("mine_phase", phaseName),
                            Placeholder.unparsed("time_to_next", timeToNext))
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
                    MiniMessage.miniMessage().deserialize(guiSettings.getNextPageName(),
                            Placeholder.unparsed("mine_name", mineName),
                            Placeholder.unparsed("mine_phase", phaseName),
                            Placeholder.unparsed("time_to_next", timeToNext))
            ));
            nextPageItem.setItemMeta(nextMeta);
            inventory.setItem(guiSettings.getNextPageSlot(), nextPageItem);
        }

        // Кнопка "Предыдущая страница"
        if (page > 0 && guiSettings.getPrevPageSlot() >= 0) {
            ItemStack prevPageItem = new ItemStack(Material.valueOf(guiSettings.getPrevPageMaterial()));
            ItemMeta prevMeta = prevPageItem.getItemMeta();
            prevMeta.setDisplayName(LegacyComponentSerializer.legacySection().serialize(
                    MiniMessage.miniMessage().deserialize(guiSettings.getPrevPageName(),
                            Placeholder.unparsed("mine_name", mineName),
                            Placeholder.unparsed("mine_phase", phaseName),
                            Placeholder.unparsed("time_to_next", timeToNext))
            ));
            prevPageItem.setItemMeta(prevMeta);
            inventory.setItem(guiSettings.getPrevPageSlot(), prevPageItem);
        }

        playerPages.put(player.getUniqueId(), page);
        player.openInventory(inventory);
    }

    // Открытие GUI скупщика
    public void openSellerGUI(Player player, int page) {
        ConfigManager.SellerGUISettings guiSettings = configManager.getSellerGUISettings();
        String mineName = configManager.getMineName();
        String phaseName = regionManager.getCurrentPhase() != null ? regionManager.getCurrentPhase().getDisplayName() : "Не установлена";
        String timeToNext = String.valueOf(regionManager.getTimeToNextPhase());
        Inventory inventory = plugin.getServer().createInventory(null, guiSettings.getSize(),
                LegacyComponentSerializer.legacySection().serialize(MiniMessage.miniMessage().deserialize(
                        "Скупщик",
                        Placeholder.unparsed("mine_name", mineName),
                        Placeholder.unparsed("mine_phase", phaseName),
                        Placeholder.unparsed("time_to_next", timeToNext)
                )));

        List<ConfigManager.SellItem> sellItems = configManager.getSellItems();
        int itemsPerPage = guiSettings.getItemsPerPage();
        int startIndex = page * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, sellItems.size());
        List<Integer> itemSlots = guiSettings.getItemSlots();

        // Заполнение слотов предметами
        for (int i = startIndex; i < endIndex && i < sellItems.size(); i++) {
            ConfigManager.SellItem sellItem = sellItems.get(i);
            int slot = itemSlots.get(i - startIndex);
            ItemStack item = new ItemStack(Material.valueOf(sellItem.getMaterial()));
            ItemMeta meta = item.getItemMeta();
            double multiplier = calculateRewardMultiplier(player);
            double finalPrice = sellItem.getPrice() * sellItem.getAmount() * multiplier;
            meta.setDisplayName(LegacyComponentSerializer.legacySection().serialize(
                    MiniMessage.miniMessage().deserialize(sellItem.getDisplayName(),
                            Placeholder.unparsed("mine_name", mineName),
                            Placeholder.unparsed("mine_phase", phaseName),
                            Placeholder.unparsed("time_to_next", timeToNext))
            ));
            List<String> lore = new ArrayList<>();
            lore.add(LegacyComponentSerializer.legacySection().serialize(
                    MiniMessage.miniMessage().deserialize("Продажа: " + sellItem.getAmount() + " " + sellItem.getDisplayName(),
                            Placeholder.unparsed("mine_name", mineName),
                            Placeholder.unparsed("mine_phase", phaseName),
                            Placeholder.unparsed("time_to_next", timeToNext))
            ));
            lore.add(LegacyComponentSerializer.legacySection().serialize(
                    MiniMessage.miniMessage().deserialize("Цена: " + String.format("%.2f", finalPrice) + " (x" + String.format("%.2f", multiplier) + ")",
                            Placeholder.unparsed("mine_name", mineName),
                            Placeholder.unparsed("mine_phase", phaseName),
                            Placeholder.unparsed("time_to_next", timeToNext))
            ));
            meta.setLore(lore);
            item.setItemMeta(meta);
            inventory.setItem(slot, item);
        }

        // Кнопка "Следующая страница"
        if (endIndex < sellItems.size() && guiSettings.getNextPageSlot() >= 0) {
            ItemStack nextPageItem = new ItemStack(Material.valueOf(guiSettings.getNextPageMaterial()));
            ItemMeta nextMeta = nextPageItem.getItemMeta();
            nextMeta.setDisplayName(LegacyComponentSerializer.legacySection().serialize(
                    MiniMessage.miniMessage().deserialize(guiSettings.getNextPageName(),
                            Placeholder.unparsed("mine_name", mineName),
                            Placeholder.unparsed("mine_phase", phaseName),
                            Placeholder.unparsed("time_to_next", timeToNext))
            ));
            nextPageItem.setItemMeta(nextMeta);
            inventory.setItem(guiSettings.getNextPageSlot(), nextPageItem);
        }

        // Кнопка "Предыдущая страница"
        if (page > 0 && guiSettings.getPrevPageSlot() >= 0) {
            ItemStack prevPageItem = new ItemStack(Material.valueOf(guiSettings.getPrevPageMaterial()));
            ItemMeta prevMeta = prevPageItem.getItemMeta();
            prevMeta.setDisplayName(LegacyComponentSerializer.legacySection().serialize(
                    MiniMessage.miniMessage().deserialize(guiSettings.getPrevPageName(),
                            Placeholder.unparsed("mine_name", mineName),
                            Placeholder.unparsed("mine_phase", phaseName),
                            Placeholder.unparsed("time_to_next", timeToNext))
            ));
            prevPageItem.setItemMeta(prevMeta);
            inventory.setItem(guiSettings.getPrevPageSlot(), prevPageItem);
        }

        playerPages.put(player.getUniqueId(), page);
        player.openInventory(inventory);
    }

    // Обработка кликов по GUI скупщика
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        if (!title.equals("Квесты") && !title.equals("Скупщик")) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        int slot = event.getRawSlot();
        int currentPage = playerPages.getOrDefault(player.getUniqueId(), 0);

        if (title.equals("Квесты")) {
            ConfigManager.QuestsGUISettings guiSettings = configManager.getQuestsGUISettings();
            if (slot == guiSettings.getNextPageSlot() && (currentPage + 1) * guiSettings.getQuestsPerPage() < configManager.getQuests().size()) {
                openQuestsGUI(player, currentPage + 1);
            } else if (slot == guiSettings.getPrevPageSlot() && currentPage > 0) {
                openQuestsGUI(player, currentPage - 1);
            }
        } else if (title.equals("Скупщик")) {
            ConfigManager.SellerGUISettings guiSettings = configManager.getSellerGUISettings();
            List<ConfigManager.SellItem> sellItems = configManager.getSellItems();
            int itemsPerPage = guiSettings.getItemsPerPage();
            int startIndex = currentPage * itemsPerPage;

            if (slot == guiSettings.getNextPageSlot() && (currentPage + 1) * itemsPerPage < sellItems.size()) {
                openSellerGUI(player, currentPage + 1);
            } else if (slot == guiSettings.getPrevPageSlot() && currentPage > 0) {
                openSellerGUI(player, currentPage - 1);
            } else {
                // Проверка клика по предмету продажи
                int index = guiSettings.getItemSlots().indexOf(slot);
                if (index >= 0 && index < sellItems.size() - startIndex) {
                    ConfigManager.SellItem sellItem = sellItems.get(startIndex + index);
                    handleSellItem(player, sellItem);
                }
            }
        }
    }

    // Обработка продажи предмета
    private void handleSellItem(Player player, ConfigManager.SellItem sellItem) {
        Material material = Material.getMaterial(sellItem.getMaterial());
        if (material == null) {
            player.sendMessage(LegacyComponentSerializer.legacySection().serialize(
                    MiniMessage.miniMessage().deserialize(configManager.getRawMessage("sell_invalid_material"),
                            Placeholder.unparsed("mine_name", configManager.getMineName()),
                            Placeholder.unparsed("mine_phase", regionManager.getCurrentPhase() != null ? regionManager.getCurrentPhase().getDisplayName() : "Не установлена"),
                            Placeholder.unparsed("time_to_next", String.valueOf(regionManager.getTimeToNextPhase())))
            ));
            return;
        }

        // Проверка инвентаря игрока
        int totalAmount = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == material) {
                totalAmount += item.getAmount();
            }
        }

        if (totalAmount < sellItem.getAmount()) {
            player.sendMessage(LegacyComponentSerializer.legacySection().serialize(
                    MiniMessage.miniMessage().deserialize(configManager.getRawMessage("sell_not_enough"),
                            Placeholder.unparsed("material", sellItem.getDisplayName()),
                            Placeholder.unparsed("mine_name", configManager.getMineName()),
                            Placeholder.unparsed("mine_phase", regionManager.getCurrentPhase() != null ? regionManager.getCurrentPhase().getDisplayName() : "Не установлена"),
                            Placeholder.unparsed("time_to_next", String.valueOf(regionManager.getTimeToNextPhase())))
            ));
            return;
        }

        // Удаление предметов
        int remaining = sellItem.getAmount();
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == material) {
                int itemAmount = item.getAmount();
                if (itemAmount <= remaining) {
                    player.getInventory().remove(item);
                    remaining -= itemAmount;
                } else {
                    item.setAmount(itemAmount - remaining);
                    remaining = 0;
                }
                if (remaining == 0) break;
            }
        }

        // Выполнение команд и расчёт цены с множителем
        double multiplier = calculateRewardMultiplier(player);
        double finalPrice = sellItem.getPrice() * sellItem.getAmount() * multiplier;
        for (String command : sellItem.getCommands()) {
            String finalCommand = command.replace("%play%", player.getName())
                    .replace("%amount%", String.valueOf(sellItem.getAmount()))
                    .replace("%money%", String.format("%.2f", finalPrice));
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCommand);
        }

        player.sendMessage(LegacyComponentSerializer.legacySection().serialize(
                MiniMessage.miniMessage().deserialize(configManager.getRawMessage("sell_success"),
                        Placeholder.unparsed("amount", String.valueOf(sellItem.getAmount())),
                        Placeholder.unparsed("material", sellItem.getDisplayName()),
                        Placeholder.unparsed("money", String.format("%.2f", finalPrice)),
                        Placeholder.unparsed("mine_name", configManager.getMineName()),
                        Placeholder.unparsed("mine_phase", regionManager.getCurrentPhase() != null ? regionManager.getCurrentPhase().getDisplayName() : "Не установлена"),
                        Placeholder.unparsed("time_to_next", String.valueOf(regionManager.getTimeToNextPhase())))
        ));
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
                                        )),
                                        Placeholder.unparsed("mine_name", configManager.getMineName()),
                                        Placeholder.unparsed("mine_phase", regionManager.getCurrentPhase() != null ? regionManager.getCurrentPhase().getDisplayName() : "Не установлена"),
                                        Placeholder.unparsed("time_to_next", String.valueOf(regionManager.getTimeToNextPhase()))
                                )
                        ));
                        resetPlayerProgress(playerId, questId);
                    }
                }
            }
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