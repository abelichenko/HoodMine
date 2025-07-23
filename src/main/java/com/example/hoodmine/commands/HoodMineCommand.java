package com.example.hoodmine.commands;

import com.example.hoodmine.HoodMinePlugin;
import com.example.hoodmine.config.ConfigManager;
import com.example.hoodmine.quests.QuestManager;
import com.example.hoodmine.utils.NPCManager;
import com.example.hoodmine.utils.RegionManagerMine;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
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
    private final NPCManager npcManager;
    private final CommandHandler handler;

    public HoodMineCommand(HoodMinePlugin plugin, ConfigManager configManager, RegionManagerMine regionManager, QuestManager questManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.regionManager = regionManager;
        this.questManager = questManager;
        this.npcManager = new NPCManager(plugin);
        this.handler = new CommandHandler(plugin, configManager, regionManager, questManager);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String mineName = configManager.getMineName();
        String phaseName = regionManager.getCurrentPhase() != null ? regionManager.getCurrentPhase().getDisplayName() : "Не установлена";
        String timeToNext = String.valueOf(regionManager.getTimeToNextPhase());

        if (args.length == 0) {
            sender.sendMessage(LegacyComponentSerializer.legacySection().serialize(
                    MiniMessage.miniMessage().deserialize(configManager.getRawMessage("usage"),
                            Placeholder.unparsed("mine_name", mineName),
                            Placeholder.unparsed("mine_phase", phaseName),
                            Placeholder.unparsed("time_to_next", timeToNext))
            ));
            return true;
        }

        String subCommand = args[0].toLowerCase();
        switch (subCommand) {
            case "setregion":
                if (!(sender instanceof Player)) {
                    sender.sendMessage(LegacyComponentSerializer.legacySection().serialize(
                            MiniMessage.miniMessage().deserialize(configManager.getRawMessage("player_only"),
                                    Placeholder.unparsed("mine_name", mineName),
                                    Placeholder.unparsed("mine_phase", phaseName),
                                    Placeholder.unparsed("time_to_next", timeToNext))
                    ));
                    return true;
                }
                if (!sender.hasPermission("hoodmine.admin")) {
                    sender.sendMessage(LegacyComponentSerializer.legacySection().serialize(
                            MiniMessage.miniMessage().deserialize(configManager.getRawMessage("no_permission"),
                                    Placeholder.unparsed("mine_name", mineName),
                                    Placeholder.unparsed("mine_phase", phaseName),
                                    Placeholder.unparsed("time_to_next", timeToNext))
                    ));
                    return true;
                }
                return handler.handleSetRegion((Player) sender);

            case "setname":
                if (!sender.hasPermission("hoodmine.admin")) {
                    sender.sendMessage(LegacyComponentSerializer.legacySection().serialize(
                            MiniMessage.miniMessage().deserialize(configManager.getRawMessage("no_permission"),
                                    Placeholder.unparsed("mine_name", mineName),
                                    Placeholder.unparsed("mine_phase", phaseName),
                                    Placeholder.unparsed("time_to_next", timeToNext))
                    ));
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(LegacyComponentSerializer.legacySection().serialize(
                            MiniMessage.miniMessage().deserialize(configManager.getRawMessage("setname_usage"),
                                    Placeholder.unparsed("mine_name", mineName),
                                    Placeholder.unparsed("mine_phase", phaseName),
                                    Placeholder.unparsed("time_to_next", timeToNext))
                    ));
                    return true;
                }
                String name = String.join(" ", args).substring(args[0].length() + 1);
                handler.handleSetName(sender, name);
                return true;

            case "quests":
                if (!(sender instanceof Player)) {
                    sender.sendMessage(LegacyComponentSerializer.legacySection().serialize(
                            MiniMessage.miniMessage().deserialize(configManager.getRawMessage("player_only"),
                                    Placeholder.unparsed("mine_name", mineName),
                                    Placeholder.unparsed("mine_phase", phaseName),
                                    Placeholder.unparsed("time_to_next", timeToNext))
                    ));
                    return true;
                }
                if (!sender.hasPermission("hoodmine.quests")) {
                    sender.sendMessage(LegacyComponentSerializer.legacySection().serialize(
                            MiniMessage.miniMessage().deserialize(configManager.getRawMessage("no_permission"),
                                    Placeholder.unparsed("mine_name", mineName),
                                    Placeholder.unparsed("mine_phase", phaseName),
                                    Placeholder.unparsed("time_to_next", timeToNext))
                    ));
                    return true;
                }
                handler.handleQuests((Player) sender);
                return true;

            case "sell":
                if (!(sender instanceof Player)) {
                    sender.sendMessage(LegacyComponentSerializer.legacySection().serialize(
                            MiniMessage.miniMessage().deserialize(configManager.getRawMessage("player_only"),
                                    Placeholder.unparsed("mine_name", mineName),
                                    Placeholder.unparsed("mine_phase", phaseName),
                                    Placeholder.unparsed("time_to_next", timeToNext))
                    ));
                    return true;
                }
                if (!sender.hasPermission("hoodmine.sell")) {
                    sender.sendMessage(LegacyComponentSerializer.legacySection().serialize(
                            MiniMessage.miniMessage().deserialize(configManager.getRawMessage("no_permission"),
                                    Placeholder.unparsed("mine_name", mineName),
                                    Placeholder.unparsed("mine_phase", phaseName),
                                    Placeholder.unparsed("time_to_next", timeToNext))
                    ));
                    return true;
                }
                handler.handleSell((Player) sender, args.length > 1 ? new String[]{args[1], args.length > 2 ? args[2] : "1"} : new String[]{"", ""});
                return true;

            case "seller":
                if (!(sender instanceof Player)) {
                    sender.sendMessage(LegacyComponentSerializer.legacySection().serialize(
                            MiniMessage.miniMessage().deserialize(configManager.getRawMessage("player_only"),
                                    Placeholder.unparsed("mine_name", mineName),
                                    Placeholder.unparsed("mine_phase", phaseName),
                                    Placeholder.unparsed("time_to_next", timeToNext))
                    ));
                    return true;
                }
                if (!sender.hasPermission("hoodmine.sell")) {
                    sender.sendMessage(LegacyComponentSerializer.legacySection().serialize(
                            MiniMessage.miniMessage().deserialize(configManager.getRawMessage("no_permission"),
                                    Placeholder.unparsed("mine_name", mineName),
                                    Placeholder.unparsed("mine_phase", phaseName),
                                    Placeholder.unparsed("time_to_next", timeToNext))
                    ));
                    return true;
                }
                handler.handleSeller((Player) sender);
                return true;

            case "reset":
                if (!(sender instanceof Player)) {
                    sender.sendMessage(LegacyComponentSerializer.legacySection().serialize(
                            MiniMessage.miniMessage().deserialize(configManager.getRawMessage("player_only"),
                                    Placeholder.unparsed("mine_name", mineName),
                                    Placeholder.unparsed("mine_phase", phaseName),
                                    Placeholder.unparsed("time_to_next", timeToNext))
                    ));
                    return true;
                }
                if (!sender.hasPermission("hoodmine.admin")) {
                    sender.sendMessage(LegacyComponentSerializer.legacySection().serialize(
                            MiniMessage.miniMessage().deserialize(configManager.getRawMessage("no_permission"),
                                    Placeholder.unparsed("mine_name", mineName),
                                    Placeholder.unparsed("mine_phase", phaseName),
                                    Placeholder.unparsed("time_to_next", timeToNext))
                    ));
                    return true;
                }
                handler.handleReset((Player) sender);
                return true;

            case "info":
                if (!(sender instanceof Player)) {
                    sender.sendMessage(LegacyComponentSerializer.legacySection().serialize(
                            MiniMessage.miniMessage().deserialize(configManager.getRawMessage("player_only"),
                                    Placeholder.unparsed("mine_name", mineName),
                                    Placeholder.unparsed("mine_phase", phaseName),
                                    Placeholder.unparsed("time_to_next", timeToNext))
                    ));
                    return true;
                }
                if (!sender.hasPermission("hoodmine.info")) {
                    sender.sendMessage(LegacyComponentSerializer.legacySection().serialize(
                            MiniMessage.miniMessage().deserialize(configManager.getRawMessage("no_permission"),
                                    Placeholder.unparsed("mine_name", mineName),
                                    Placeholder.unparsed("mine_phase", phaseName),
                                    Placeholder.unparsed("time_to_next", timeToNext))
                    ));
                    return true;
                }
                handler.handleInfo((Player) sender);
                return true;

            case "npc":
                if (!(sender instanceof Player)) {
                    sender.sendMessage(LegacyComponentSerializer.legacySection().serialize(
                            MiniMessage.miniMessage().deserialize(configManager.getRawMessage("player_only"),
                                    Placeholder.unparsed("mine_name", mineName),
                                    Placeholder.unparsed("mine_phase", phaseName),
                                    Placeholder.unparsed("time_to_next", timeToNext))
                    ));
                    return true;
                }
                if (!sender.hasPermission("hoodmine.admin")) {
                    sender.sendMessage(LegacyComponentSerializer.legacySection().serialize(
                            MiniMessage.miniMessage().deserialize(configManager.getRawMessage("no_permission"),
                                    Placeholder.unparsed("mine_name", mineName),
                                    Placeholder.unparsed("mine_phase", phaseName),
                                    Placeholder.unparsed("time_to_next", timeToNext))
                    ));
                    return true;
                }
                if (args.length < 2 || (!args[1].equalsIgnoreCase("spawn") && !args[1].equalsIgnoreCase("remove"))) {
                    sender.sendMessage(LegacyComponentSerializer.legacySection().serialize(
                            MiniMessage.miniMessage().deserialize(configManager.getRawMessage("npc_usage"),
                                    Placeholder.unparsed("mine_name", mineName),
                                    Placeholder.unparsed("mine_phase", phaseName),
                                    Placeholder.unparsed("time_to_next", timeToNext))
                    ));
                    return true;
                }
                Player player = (Player) sender;
                if (args[1].equalsIgnoreCase("spawn")) {
                    npcManager.spawnNPC(player, p -> handler.handleQuests(p));
                } else {
                    npcManager.removeNPC(player);
                }
                return true;

            default:
                sender.sendMessage(LegacyComponentSerializer.legacySection().serialize(
                        MiniMessage.miniMessage().deserialize(configManager.getRawMessage("unknown_command"),
                                Placeholder.unparsed("mine_name", mineName),
                                Placeholder.unparsed("mine_phase", phaseName),
                                Placeholder.unparsed("time_to_next", timeToNext))
                ));
                return true;
        }
    }
}