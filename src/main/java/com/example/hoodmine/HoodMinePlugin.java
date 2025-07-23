package com.example.hoodmine;

import com.example.hoodmine.config.ConfigManager;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class HoodMinePlugin extends JavaPlugin implements Listener {
    private ConfigManager configManager;
    private final Map<UUID, Map<String, Integer>> playerQuestProgress = new HashMap<>();
    private final Map<UUID, BossBar> playerBossBars = new HashMap<>();
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private Connection dbConnection;
    private Map<String, ArmorStand> npcs = new HashMap<>();
    private WorldGuard worldGuard;

    @Override
    public void onEnable() {
        configManager = new ConfigManager(this);
        getServer().getPluginManager().registerEvents(this, this);
        setupDatabase();
        getCommand("hoodmine").setExecutor(new HoodMineCommand(this, configManager));

        // Инициализация WorldGuard
        if (Bukkit.getPluginManager().getPlugin("WorldGuard") != null) {
            worldGuard = WorldGuard.getInstance();
            getLogger().info("WorldGuard обнаружен и инициализирован.");
        } else {
            getLogger().warning("WorldGuard не найден. Функциональность регионов отключена.");
        }

        getLogger().info("HoodMine v1.0 включён!");
    }

    @Override
    public void onDisable() {
        saveAllProgress();
        for (ArmorStand npc : npcs.values()) {
            if (npc != null && !npc.isDead()) npc.remove();
        }
    }

    private void setupDatabase() {
        try {
            dbConnection = DriverManager.getConnection("jdbc:sqlite:plugins/HoodMine/quests.db");
            Statement stmt = dbConnection.createStatement();
            stmt.execute("CREATE TABLE IF NOT EXISTS quest_progress (player_uuid TEXT, quest_id TEXT, progress INTEGER, PRIMARY KEY (player_uuid, quest_id))");
            stmt.close();
        } catch (SQLException e) {
            getLogger().severe("Ошибка при настройке базы данных: " + e.getMessage());
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        UUID playerUuid = event.getPlayer().getUniqueId();
        loadPlayerProgress(playerUuid);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerUuid = event.getPlayer().getUniqueId();
        savePlayerProgress(playerUuid);
        BossBar bossBar = playerBossBars.remove(playerUuid);
        if (bossBar != null) {
            event.getPlayer().hideBossBar(bossBar);
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        String materialName = event.getBlock().getType().toString();
        UUID playerUuid = player.getUniqueId();
        Location loc = event.getBlock().getLocation();

        // Проверка региона (если WorldGuard активен)
        if (worldGuard != null) {
            RegionManager regionManager = getRegionManager(loc.getWorld().getName());
            if (regionManager != null) {
                ApplicableRegionSet regions = regionManager.getApplicableRegions(BukkitAdapter.asBlockVector(loc));
                boolean inMineRegion = false;
                for (ProtectedRegion region : regions) {
                    if (region.getId().equals("mine_region")) { // Замените на нужный ID региона
                        inMineRegion = true;
                        break;
                    }
                }
                if (!inMineRegion) {
                    return; // Блок не засчитывается, если вне региона
                }
            }
        }

        if (!configManager.isShowBossBar()) return;

        for (ConfigManager.Quest quest : configManager.getQuests()) {
            if (quest.getMaterial().toString().equals(materialName)) {
                Map<String, Integer> progressMap = playerQuestProgress.computeIfAbsent(playerUuid, k -> new HashMap<>());
                int currentProgress = progressMap.getOrDefault(quest.getId(), 0);
                currentProgress++;
                progressMap.put(quest.getId(), currentProgress);

                // Отображение +1 в ActionBar
                player.sendActionBar(miniMessage.deserialize("<green>+1"));

                // Создание или обновление bossbar
                BossBar bossBar = playerBossBars.getOrDefault(playerUuid, BossBar.bossBar(
                        Component.text(quest.getName() + " 0/0"),
                        0.0f,
                        BossBar.Color.GREEN,
                        BossBar.Overlay.PROGRESS
                ));
                float progress = (float) currentProgress / quest.getAmount();
                bossBar.name(Component.text(quest.getName() + " " + currentProgress + "/" + quest.getAmount()));
                bossBar.progress(Math.min(progress, 1.0f));
                player.showBossBar(bossBar);
                playerBossBars.put(playerUuid, bossBar);

                // Таймер на 5 секунд с удалением, если перебивается
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (playerBossBars.get(playerUuid) == bossBar) {
                            player.hideBossBar(bossBar);
                            playerBossBars.remove(playerUuid);
                        }
                    }
                }.runTaskLater(this, 100L); // 5 секунд

                if (currentProgress >= quest.getAmount()) {
                    player.sendMessage(miniMessage.deserialize("<green>Квест " + quest.getName() + " выполнен!"));
                    player.giveExp(quest.getExperience());
                    // Расчёт множителя наград
                    int completedQuests = (int) playerQuestProgress.get(playerUuid).entrySet().stream()
                            .filter(e -> e.getValue() >= configManager.getQuests().stream()
                                    .filter(q -> q.getId().equals(e.getKey())).findFirst().map(ConfigManager.Quest::getAmount).orElse(0)).count();
                    double multiplier = configManager.getRewardMultiplierBase() + (completedQuests * configManager.getRewardMultiplierPerQuest());
                    double finalReward = Math.min(multiplier, configManager.getRewardMultiplierMax()) * quest.getMoney();
                    player.sendMessage(miniMessage.deserialize("<gold>Награда увеличена на множитель " + multiplier + "x. Получено: " + finalReward + " монет."));
                    // Логика для money и commands
                    progressMap.remove(quest.getId()); // Сброс квеста после выполнения
                }
            }
        }
    }

    private void loadPlayerProgress(UUID playerUuid) {
        try (PreparedStatement stmt = dbConnection.prepareStatement("SELECT quest_id, progress FROM quest_progress WHERE player_uuid = ?")) {
            stmt.setString(1, playerUuid.toString());
            ResultSet rs = stmt.executeQuery();
            Map<String, Integer> progressMap = new HashMap<>();
            while (rs.next()) {
                progressMap.put(rs.getString("quest_id"), rs.getInt("progress"));
            }
            playerQuestProgress.put(playerUuid, progressMap);
        } catch (SQLException e) {
            getLogger().warning("Ошибка при загрузке прогресса: " + e.getMessage());
        }
    }

    private void savePlayerProgress(UUID playerUuid) {
        Map<String, Integer> progressMap = playerQuestProgress.get(playerUuid);
        if (progressMap == null) return;
        try (PreparedStatement stmt = dbConnection.prepareStatement(
                "INSERT OR REPLACE INTO quest_progress (player_uuid, quest_id, progress) VALUES (?, ?, ?)")) {
            for (Map.Entry<String, Integer> entry : progressMap.entrySet()) {
                stmt.setString(1, playerUuid.toString());
                stmt.setString(2, entry.getKey());
                stmt.setInt(3, entry.getValue());
                stmt.addBatch();
            }
            stmt.executeBatch();
        } catch (SQLException e) {
            getLogger().warning("Ошибка при сохранении прогресса: " + e.getMessage());
        }
    }

    private void saveAllProgress() {
        for (UUID playerUuid : playerQuestProgress.keySet()) {
            savePlayerProgress(playerUuid);
        }
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public Map<String, ArmorStand> getNpcs() {
        return npcs;
    }

    public void addNpc(String type, ArmorStand npc) {
        npcs.put(type, npc);
    }

    public void removeNpc(String type) {
        npcs.remove(type);
    }

    // Метод для получения RegionManager
    public RegionManager getRegionManager(String worldName) {
        if (worldGuard == null) return null;
        return worldGuard.getPlatform().getRegionContainer().get(BukkitAdapter.adapt(Bukkit.getWorld(worldName)));
    }
}