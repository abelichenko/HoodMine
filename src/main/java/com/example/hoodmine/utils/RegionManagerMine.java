package com.example.hoodmine.utils;

import com.example.hoodmine.HoodMinePlugin;
import com.example.hoodmine.config.ConfigManager;
import com.example.hoodmine.database.DatabaseManager;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;
import java.util.Random;

// Класс для управления регионом шахты и фазами
public class RegionManagerMine {
    private final HoodMinePlugin plugin;
    private final ConfigManager configManager;
    private final DatabaseManager databaseManager;
    private ProtectedRegion mineRegion;
    private ConfigManager.Phase currentPhase;
    private long timeToNextPhase;
    private final Random random = new Random();

    public RegionManagerMine(HoodMinePlugin plugin, ConfigManager configManager, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.databaseManager = databaseManager;
        loadRegion();
        startPhaseTimer();
    }

    // Загрузка региона шахты
    private void loadRegion() {
        FileConfiguration config = plugin.getConfig();
        String regionId = config.getString("mine_region");
        if (regionId != null) {
            World world = plugin.getServer().getWorld(config.getString("world"));
            if (world != null) {
                RegionManagerMine regionManager = WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(world));
                if (regionManager != null) {
                    mineRegion = regionManager.getRegion(regionId);
                }
            }
        }
        currentPhase = configManager.getPhases().get(0);
        timeToNextPhase = configManager.getTimerInterval();
    }

    // Установка региона шахты
    public boolean setRegion(Player player) {
        com.sk89q.worldedit.regions.Region selection = null;
        try {
            selection = WorldEditPlugin.getInstance().getSession(player).getSelection(BukkitAdapter.adapt(player.getWorld()));
        } catch (Exception e) {
            String mineName = configManager.getMineName();
            String phaseName = currentPhase != null ? currentPhase.getDisplayName() : "Не установлена";
            String timeToNext = String.valueOf(timeToNextPhase);
            player.sendMessage(LegacyComponentSerializer.legacySection().serialize(
                    MiniMessage.miniMessage().deserialize(
                            configManager.getRawMessage("region_select_error"),
                            Placeholder.unparsed("mine_name", mineName),
                            Placeholder.unparsed("mine_phase", phaseName),
                            Placeholder.unparsed("time_to_next", timeToNext)
                    )
            ));
            return false;
        }
        World world = player.getWorld();
        RegionManagerMine regionManager = WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(world));
        if (regionManager == null) {
            String mineName = configManager.getMineName();
            String phaseName = currentPhase != null ? currentPhase.getDisplayName() : "Не установлена";
            String timeToNext = String.valueOf(timeToNextPhase);
            player.sendMessage(LegacyComponentSerializer.legacySection().serialize(
                    MiniMessage.miniMessage().deserialize(
                            configManager.getRawMessage("worldguard_error"),
                            Placeholder.unparsed("mine_name", mineName),
                            Placeholder.unparsed("mine_phase", phaseName),
                            Placeholder.unparsed("time_to_next", timeToNext)
                    )
            ));
            return false;
        }
        String regionId = "hoodmine_" + System.currentTimeMillis();
        ProtectedRegion region = new com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion(
                regionId,
                BlockVector3.at(selection.getMinimumPoint().getX(), selection.getMinimumPoint().getY(), selection.getMinimumPoint().getZ()),
                BlockVector3.at(selection.getMaximumPoint().getX(), selection.getMaximumPoint().getY(), selection.getMaximumPoint().getZ())
        );
        regionManager.addRegion(region);
        mineRegion = region;
        FileConfiguration config = plugin.getConfig();
        config.set("mine_region", regionId);
        config.set("world", world.getName());
        plugin.saveConfig();
        String mineName = configManager.getMineName();
        String phaseName = currentPhase != null ? currentPhase.getDisplayName() : "Не установлена";
        String timeToNext = String.valueOf(timeToNextPhase);
        player.sendMessage(LegacyComponentSerializer.legacySection().serialize(
                MiniMessage.miniMessage().deserialize(
                        configManager.getRawMessage("region_set_success"),
                        Placeholder.unparsed("mine_name", mineName),
                        Placeholder.unparsed("mine_phase", phaseName),
                        Placeholder.unparsed("time_to_next", timeToNext)
                )
        ));
        resetMine();
        return true;
    }

    // Сброс шахты
    public void resetMine() {
        if (mineRegion == null) return;
        World world = plugin.getServer().getWorld(plugin.getConfig().getString("world"));
        if (world == null) return;
        BlockVector3 min = mineRegion.getMinimumPoint();
        BlockVector3 max = mineRegion.getMaximumPoint();
        for (int x = min.getBlockX(); x <= max.getBlockX(); x++) {
            for (int y = min.getBlockY(); y <= max.getBlockY(); y++) {
                for (int z = min.getBlockZ(); z <= max.getBlockZ(); z++) {
                    Location loc = new Location(world, x, y, z);
                    if (mineRegion.contains(x, y, z)) {
                        if (random.nextDouble() < getCurrentPhase().getSpawns().getOrDefault("STONE", 1.0)) {
                            loc.getBlock().setType(Material.STONE);
                        } else {
                            String material = getRandomMaterial(getCurrentPhase().getSpawns());
                            loc.getBlock().setType(Material.valueOf(material));
                        }
                    }
                }
            }
        }
    }

    // Получение случайного материала на основе весов
    private String getRandomMaterial(Map<String, Double> spawns) {
        double totalWeight = spawns.values().stream().mapToDouble(Double::doubleValue).sum();
        double randomValue = random.nextDouble() * totalWeight;
        double currentWeight = 0;
        for (Map.Entry<String, Double> entry : spawns.entrySet()) {
            currentWeight += entry.getValue();
            if (randomValue <= currentWeight) {
                return entry.getKey();
            }
        }
        return "STONE";
    }

    // Запуск таймера смены фаз
    private void startPhaseTimer() {
        new BukkitRunnable() {
            @Override
            public void run() {
                timeToNextPhase--;
                if (timeToNextPhase <= 0) {
                    switchPhase();
                    timeToNextPhase = configManager.getTimerInterval();
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    // Смена фазы
    private void switchPhase() {
        List<ConfigManager.Phase> phases = configManager.getPhases();
        int currentIndex = phases.indexOf(currentPhase);
        currentPhase = phases.get((currentIndex + 1) % phases.size());
        resetMine();
        String mineName = configManager.getMineName();
        String phaseName = currentPhase != null ? currentPhase.getDisplayName() : "Не установлена";
        String timeToNext = String.valueOf(timeToNextPhase);
        plugin.getServer().broadcastMessage(LegacyComponentSerializer.legacySection().serialize(
                MiniMessage.miniMessage().deserialize(
                        configManager.getRawMessage("phase_changed"),
                        Placeholder.unparsed("mine_name", mineName),
                        Placeholder.unparsed("mine_phase", phaseName),
                        Placeholder.unparsed("time_to_next", timeToNext)
                )
        ));
    }

    // Проверка, находится ли локация в регионе шахты
    public boolean isInMineRegion(Location location) {
        if (mineRegion == null) return false;
        return mineRegion.contains(
                location.getBlockX(),
                location.getBlockY(),
                location.getBlockZ()
        );
    }

    // Получение текущей фазы
    public ConfigManager.Phase getCurrentPhase() {
        return currentPhase;
    }

    // Получение времени до следующей фазы
    public long getTimeToNextPhase() {
        return timeToNextPhase;
    }

    // Получение региона шахты
    public ProtectedRegion getMineRegion() {
        return mineRegion;
    }
}