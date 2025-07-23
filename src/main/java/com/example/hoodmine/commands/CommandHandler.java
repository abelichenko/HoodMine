package com.example.hoodmine.commands;

import com.example.hoodmine.HoodMinePlugin;
import com.example.hoodmine.config.ConfigManager;
import com.example.hoodmine.quests.QuestManager;
import com.example.hoodmine.utils.RegionManager;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
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

    // Обработка команды /hoodmine установить_регион
    public boolean handleSetRegion(Player player) {
        return regionManager.setRegion(player);
    }

    // Обработка команды /hoodmine название <имя>
    public void handleSetName(CommandSender sender, String name) {
        configManager.setMineName(name);
        sender.sendMessage(MiniMessage.miniMessage().deserialize(
                configManager.getRawMessage("setname_ok"),
                Placeholder.unparsed("name", name)
        ));
    }

    // Обработка команды /hoodmine квесты
    public void handleQuests(Player player) {
        questManager.openQuestsGUI(player);
    }

    // Обработка команды /hoodmine продать [материал] [количество]
    public void handleSell(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(configManager.getMessage("sell_usage"));
            return;
        }

        String materialName = args[0].toUpperCase();
        Map<String, Double> sellPrices = configManager.getSellPrices();
        if (!sellPrices.containsKey(materialName)) {
            player.sendMessage(configManager.getMessage("sell_invalid_material"));
            return;
        }

        int amount;
        try {
            amount = Integer.parseInt(args[1]);
            if (amount <= 0) {
                player.sendMessage(configManager.getMessage("sell_invalid_amount"));
                return;
            }
        } catch (NumberFormatException e) {
            player.sendMessage(configManager.getMessage("sell_invalid_amount"));
            return;
        }

        Material material = Material.getMaterial(materialName);
        if (material == null) {
            player.sendMessage(configManager.getMessage("sell_invalid_material"));
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
            player.sendMessage(MiniMessage.miniMessage().deserialize(
                    configManager.getRawMessage("sell_not_enough"),
                    Placeholder.unparsed("material", materialName)
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
        player.sendMessage(MiniMessage.miniMessage().deserialize(
                configManager.getRawMessage("sell_success"),
                Placeholder.unparsed("amount", String.valueOf(amount)),
                Placeholder.unparsed("material", materialName),
                Placeholder.unparsed("money", String.format("%.2f", finalPrice))
        ));
    }

    // Обработка команды /hoodmine сброс
    public void handleReset(Player player) {
        if (regionManager.getMineRegion() == null) {
            player.sendMessage(configManager.getMessage("no_region"));
            return;
        }
        regionManager.resetMine();
        player.sendMessage(MiniMessage.miniMessage().deserialize(
                configManager.getRawMessage("reset_success")
        ));
    }

    // Обработка команды /hoodmine инфо
    public void handleInfo(Player player) {
        ConfigManager.Phase currentPhase = regionManager.getCurrentPhase();
        String phaseName = currentPhase != null ? currentPhase.getDisplayName() : "Не установлена";
        long timeToNext = regionManager.getTimeToNextPhase();
        player.sendMessage(MiniMessage.miniMessage().deserialize(
                configManager.getRawMessage("info_message"),
                Placeholder.unparsed("mine_name", configManager.getMineName()),
                Placeholder.unparsed("phase", phaseName),
                Placeholder.unparsed("time", String.valueOf(timeToNext))
        ));
    }
}