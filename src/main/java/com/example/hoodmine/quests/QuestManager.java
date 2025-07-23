package com.example.hoodmine.quests;

import com.example.hoodmine.HoodMinePlugin;
import com.example.hoodmine.config.ConfigManager;
import com.example.hoodmine.database.DatabaseManager;
import com.example.hoodmine.utils.RegionManagerMine;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
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
    private final RegionManagerMine regionManager;
    private final Map<UUID, Map<String, Integer>> playerProgress;
    private final Map<UUID, Integer> completedQuests;

    public QuestManager(HoodMinePlugin plugin, ConfigManager configManager, DatabaseManager databaseManager, RegionManagerMine regionManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.databaseManager = databaseManager;
        this.regionManager = regionManager;
        this.playerProgress = new HashMap<>();
        this.completedQuests = new HashMap<>();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        loadCompletedQuests();
    }

    private void loadCompletedQuests() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            UUID playerId = player.getUniqueId();
            int completed = databaseManager.getCompletedQuests(playerId);
            completedQuests.put(playerId, completed);
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (!regionManager.isInMineRegion(event.getBlock().getLocation())) {
            return;
        }
        Material material = event.getBlock().getType();
        Map<String, Integer> progress = playerProgress.computeIfAbsent(player.getUniqueId(), k -> new HashMap<>());
        for (ConfigManager.Quest quest : configManager.getQuests()) {
            if (quest.getMaterial() == material) {
                String questId = quest.getId();
                int current = progress.getOrDefault(questId, 0) + 1;
                progress.put(questId, current);
                if (current >= quest.getAmount()) {
                    completeQuest(player, quest);
                    progress.remove(questId);
                }
            }
        }
    }

    public void completeQuest(Player player, ConfigManager.Quest quest) {
        UUID playerId = player.getUniqueId();
        if (quest.getMoney() > 0) {
            // Предполагается интеграция с Vault
            // Economy econ = VaultHook.getEconomy();
            // if (econ != null) econ.depositPlayer(player, quest.getMoney());
        }
        if (quest.getExperience() > 0) {
            player.giveExp(quest.getExperience());
        }
        for (String command : quest.getCommands()) {
            plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), command.replace("%player%", player.getName()));
        }
        int completed = completedQuests.getOrDefault(playerId, 0) + 1;
        completedQuests.put(playerId, completed);
        databaseManager.saveCompletedQuests(playerId, completed);
        String mineName = configManager.getMineName();
        String phaseName = regionManager.getCurrentPhase() != null ? regionManager.getCurrentPhase().getDisplayName() : "Не установлена";
        String timeToNext = String.valueOf(regionManager.getTimeToNextPhase());
        player.sendMessage(MiniMessage.miniMessage().deserialize(
                configManager.getRawMessage("quest_completed"),
                Placeholder.unparsed("quest_name", quest.getName()),
                Placeholder.unparsed("mine_name", mineName),
                Placeholder.unparsed("mine_phase", phaseName),
                Placeholder.unparsed("time_to_next", timeToNext),
                Placeholder.unparsed("multiplier", String.format("%.2f", getPlayerMultiplier(playerId)))
        ));
    }

    public ItemStack getQuestItem(ConfigManager.Quest quest, Player player) {
        ItemStack item = new ItemStack(quest.getMaterial());
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(MiniMessage.miniMessage().deserialize(quest.getName()));
            Map<String, Integer> progress = getPlayerProgress(player.getUniqueId());
            int current = progress.getOrDefault(quest.getId(), 0);
            List<Component> lore = new ArrayList<>();
            if (quest.getDescription() != null) {
                lore.add(MiniMessage.miniMessage().deserialize(quest.getDescription()));
            }
            lore.add(MiniMessage.miniMessage().deserialize("<white>Прогресс: <green>" + current + "/" + quest.getAmount()));
            lore.add(MiniMessage.miniMessage().deserialize("<white>Награда: <green>$" + quest.getMoney()));
            lore.add(MiniMessage.miniMessage().deserialize("<white>Опыт: <green>" + quest.getExperience()));
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    public Map<String, Integer> getPlayerProgress(UUID playerId) {
        return playerProgress.getOrDefault(playerId, new HashMap<>());
    }

    public double getPlayerMultiplier(UUID playerId) {
        int completed = completedQuests.getOrDefault(playerId, 0);
        double multiplier = configManager.getRewardMultiplierBase() + (completed * configManager.getRewardMultiplierPerQuest());
        return Math.min(multiplier, configManager.getRewardMultiplierMax());
    }
}