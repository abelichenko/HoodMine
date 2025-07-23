package com.example.hoodmine.commands;

import com.example.hoodmine.HoodMinePlugin;
import com.example.hoodmine.config.ConfigManager;
import com.example.hoodmine.quests.QuestManager;
import com.example.hoodmine.utils.RegionManagerMine;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

// Класс для обработки команды /hoodmine
public class HoodMineCommand implements CommandExecutor {
    private final HoodMinePlugin plugin;
    private final ConfigManager configManager;
    private final RegionManagerMine regionManager;
    private final QuestManager questManager;
    private final CommandHandler handler;

    public HoodMineCommand(HoodMinePlugin plugin, ConfigManager configManager, RegionManagerMine regionManager, QuestManager questManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.regionManager = regionManager;
        this.questManager = questManager;
        this.handler = new CommandHandler(plugin, configManager, regionManager, questManager);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            return handler.handleHelp(sender);
        }
        String subCommand = args[0].toLowerCase();
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Эта команда только для игроков!");
            return true;
        }
        switch (subCommand) {
            case "setregion":
                return handler.handleSetRegion(player);
            case "reset":
                return handler.handleReset(player);
            case "seller":
                return handler.handleSeller(player);
            case "quests":
                return handler.handleQuests(player);
            case "help":
                return handler.handleHelp(sender);
            default:
                return handler.handleHelp(sender);
        }
    }
}