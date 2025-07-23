package com.example.hoodmine.commands;

import com.example.hoodmine.HoodMinePlugin;
import com.example.hoodmine.config.ConfigManager;
import com.example.hoodmine.quests.QuestManager;
import com.example.hoodmine.utils.RegionManager;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

// Класс для обработки подкоманд /hoodmine
public class CommandHandler {
    private final HoodMinePlugin plugin;
    private final ConfigManager configManager;
    private final RegionManager regionManager;
    private final QuestManager questManager;

    public CommandHandler(HoodMinePlugin plugin, ConfigManager configManager, RegionManager regionManager, QuestManager questManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.regionManager = regionManager;
        this.questManager = questManager;
    }

    // Обработка команды /hoodmine setregion
    public boolean handleSetRegion(Player player) {
        return regionManager.setRegion(player);
    }

    // Обработка команды /hoodmine setname <name>
    public void handleSetName(CommandSender sender, String name) {
        configManager.setMineName(name);
        sender.sendMessage(LegacyComponentSerializer.legacySection().serialize(
                MiniMessage.miniMessage().deserialize(
                        configManager.getRawMessage("setname_ok"),
                        Placeholder.unparsed("name", name),
                        Placeholder.unparsed("mine_name", configManager.getMineName()),
                        Placeholder.unparsed("mine_phase", regionManager.getCurrentPhase() != null ? regionManager.getCurrentPhase().getDisplayName() : "Не установлена"),
                        Placeholder.unparsed("time_to_next", String.valueOf(regionManager.getTimeToNextPhase()))
                )
        ));
    }

    // Обработка команды /hoodmine quests
    public void handleQuests(Player player) {
        questManager.openQuestsGUI(player, 0);
    }

    // Обработка команды /hoodmine sell [material] [amount]
    public void handleSell(Player player, String[] args) {
        String mineName = configManager.getMineName();
        String phaseName = regionManager.getCurrentPhase() != null ? regionManager.getCurrentPhase().getDisplayName() : "Не установлена";
        String timeToNext = String.valueOf(regionManager.getTimeToNextPhase());

        if (args.length < 2) {
            player.sendMessage(LegacyComponentSerializer.legacySection().serialize(
                    MiniMessage.miniMessage().deserialize(configManager.getRawMessage("sell_usage"),
                            Placeholder.unparsed("mine_name", mineName),
                            Placeholder.unparsed("mine_phase", phaseName),
                            Placeholder.unparsed("time_to_next", timeToNext))
            ));
            return;
        }

        String materialName = args[0].toUpperCase();
        Map<String, Double> sellPrices = configManager.getSellPrices();
        if (!sellPrices.containsKey(materialName)) {
            player.sendMessage(LegacyComponentSerializer.legacySection().serialize(
                    MiniMessage.miniMessage().deserialize(configManager.getRawMessage("sell_invalid_material"),
                            Placeholder.unparsed("mine_name", mineName),
                            Placeholder.unparsed("mine_phase", phaseName),
                            Placeholder.unparsed("time_to_next", timeToNext))
            ));
            return;
        }

        int amount;
        try {
            amount = Integer.parseInt(args[1]);
            if (amount <= 0) {
                player.sendMessage(LegacyComponentSerializer.legacySection().serialize(
                        MiniMessage.miniMessage().deserialize(configManager.getRawMessage("sell_invalid_amount"),
                                Placeholder.unparsed("mine_name", mineName),
                                Placeholder.unparsed("mine_phase", phaseName),
                                Placeholder.unparsed("time_to_next", timeToNext))
                ));
                return;
            }
        } catch (NumberFormatException e) {
            player.sendMessage(LegacyComponentSerializer.legacySection().serialize(
                    MiniMessage.miniMessage().deserialize(configManager.getRawMessage("sell_invalid_amount"),
                            Placeholder.unparsed("mine_name", mineName),
                            Placeholder.unparsed("mine_phase", phaseName),
                            Placeholder.unparsed("time_to_next", timeToNext))
            ));
            return;
        }

        Material material = Material.getMaterial(materialName);
        if (material == null) {
            player.sendMessage(LegacyComponentSerializer.legacySection().serialize(
                    MiniMessage.miniMessage().deserialize(configManager.getRawMessage("sell_invalid_material"),
                            Placeholder.unparsed("mine_name", mineName),
                            Placeholder.unparsed("mine_phase", phaseName),
                            Placeholder.unparsed("time_to_next", timeToNext))
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

        if (totalAmount < amount) {
            String displayName = configManager.getQuests().stream()
                    .filter(q -> q.getTargetBlock().equals(materialName))
                    .findFirst()
                    .map(ConfigManager.Quest::getDisplayName)
                    .orElse(materialName);
            player.sendMessage(LegacyComponentSerializer.legacySection().serialize(
                    MiniMessage.miniMessage().deserialize(
                            configManager.getRawMessage("sell_not_enough"),
                            Placeholder.unparsed("material", displayName),
                            Placeholder.unparsed("mine_name", mineName),
                            Placeholder.unparsed("mine_phase", phaseName),
                            Placeholder.unparsed("time_to_next", timeToNext)
                    )
            ));
            return;
        }

        // Удаление предметов и выдача денег
        int remaining = amount;
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

        double price = sellPrices.get(materialName) * amount;
        double multiplier = questManager.calculateRewardMultiplier(player);
        double finalPrice = price * multiplier;
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "eco give " + player.getName() + " " + finalPrice);
        String displayName = configManager.getQuests().stream()
                .filter(q -> q.getTargetBlock().equals(materialName))
                .findFirst()
                .map(ConfigManager.Quest::getDisplayName)
                .orElse(materialName);
        player.sendMessage(LegacyComponentSerializer.legacySection().serialize(
                MiniMessage.miniMessage().deserialize(
                        configManager.getRawMessage("sell_success"),
                        Placeholder.unparsed("amount", String.valueOf(amount)),
                        Placeholder.unparsed("material", displayName),
                        Placeholder.unparsed("money", String.format("%.2f", finalPrice)),
                        Placeholder.unparsed("mine_name", mineName),
                        Placeholder.unparsed("mine_phase", phaseName),
                        Placeholder.unparsed("time_to_next", timeToNext)
                )
        ));
    }

    // Обработка команды /hoodmine seller
    public void handleSeller(Player player) {
        questManager.openSellerGUI(player, 0);
    }

    // Обработка команды /hoodmine reset
    public void handleReset(Player player) {
        String mineName = configManager.getMineName();
        String phaseName = regionManager.getCurrentPhase() != null ? regionManager.getCurrentPhase().getDisplayName() : "Не установлена";
        String timeToNext = String.valueOf(regionManager.getTimeToNextPhase());

        if (regionManager.getMineRegion() == null) {
            player.sendMessage(LegacyComponentSerializer.legacySection().serialize(
                    MiniMessage.miniMessage().deserialize(configManager.getRawMessage("no_region"),
                            Placeholder.unparsed("mine_name", mineName),
                            Placeholder.unparsed("mine_phase", phaseName),
                            Placeholder.unparsed("time_to_next", timeToNext))
            ));
            return;
        }
        regionManager.resetMine();
        player.sendMessage(LegacyComponentSerializer.legacySection().serialize(
                MiniMessage.miniMessage().deserialize(configManager.getRawMessage("reset_success"),
                        Placeholder.unparsed("mine_name", mineName),
                        Placeholder.unparsed("mine_phase", phaseName),
                        Placeholder.unparsed("time_to_next", timeToNext))
        ));
    }

    // Обработка команды /hoodmine info
    public void handleInfo(Player player) {
        String mineName = configManager.getMineName();
        String phaseName = regionManager.getCurrentPhase() != null ? regionManager.getCurrentPhase().getDisplayName() : "Не установлена";
        String timeToNext = String.valueOf(regionManager.getTimeToNextPhase());
        player.sendMessage(LegacyComponentSerializer.legacySection().serialize(
                MiniMessage.miniMessage().deserialize(
                        configManager.getRawMessage("info_message"),
                        Placeholder.unparsed("mine_name", mineName),
                        Placeholder.unparsed("mine_phase", phaseName),
                        Placeholder.unparsed("time_to_next", timeToNext)
                )
        ));
    }
}