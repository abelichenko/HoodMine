package com.example.hoodmine;

import com.example.hoodmine.config.ConfigManager;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class HoodMineCommand implements CommandExecutor {
    private final HoodMinePlugin plugin;
    private final ConfigManager configManager;

    public HoodMineCommand(HoodMinePlugin plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Эта команда только для игроков!");
            return true;
        }
        Player player = (Player) sender;

        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Использование: /hoodmine npc <spawn|delete> <seller|quests>");
            return true;
        }

        String action = args[0].toLowerCase();
        String type = args[1].toLowerCase();

        if (action.equals("spawn")) {
            if (type.equals("seller") && player.hasPermission("hoodmine.npc.spawn.seller")) {
                spawnNPC(player, "seller");
                return true;
            } else if (type.equals("quests") && player.hasPermission("hoodmine.npc.spawn.quests")) {
                spawnNPC(player, "quests");
                return true;
            }
        } else if (action.equals("delete")) {
            if (type.equals("seller") && player.hasPermission("hoodmine.npc.delete.seller")) {
                deleteNPC(player, "seller");
                return true;
            } else if (type.equals("quests") && player.hasPermission("hoodmine.npc.delete.quests")) {
                deleteNPC(player, "quests");
                return true;
            }
        }

        player.sendMessage(ChatColor.RED + "Недостаточно прав или неверная команда!");
        return true;
    }

    private void spawnNPC(Player player, String type) {
        Location loc = player.getLocation();
        String npcName = configManager.getConfig().getString("npcs." + type + ".name", type.equals("seller") ? "Скупщик" : "Квестодатель");
        EntityType entityType = EntityType.valueOf(configManager.getConfig().getString("npcs." + type + ".entity_type", "VILLAGER"));
        ArmorStand npc = (ArmorStand) loc.getWorld().spawnEntity(loc, entityType == EntityType.PLAYER ? EntityType.ARMOR_STAND : entityType);
        npc.setCustomName(npcName);
        npc.setCustomNameVisible(true);
        if (entityType == EntityType.PLAYER) {
            npc.setGravity(false);
            npc.setSmall(true);
            // Дополнительная кастомизация модели игрока (например, через плагин или текстуры)
        }
        plugin.addNpc(type, npc);
        player.sendMessage(ChatColor.GREEN + "NPC " + type + " создан!");
    }

    private void deleteNPC(Player player, String type) {
        ArmorStand npc = plugin.getNpcs().get(type);
        if (npc != null && !npc.isDead()) {
            npc.remove();
            plugin.removeNpc(type);
            player.sendMessage(ChatColor.GREEN + "NPC " + type + " удалён!");
        } else {
            player.sendMessage(ChatColor.RED + "NPC " + type + " не найден!");
        }
    }
}