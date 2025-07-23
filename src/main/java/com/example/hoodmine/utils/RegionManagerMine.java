package com.example.hoodmine.utils;

import com.example.hoodmine.HoodMinePlugin;
import com.example.hoodmine.config.ConfigManager;
import com.example.hoodmine.database.DatabaseManager;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
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

    private void loadRegion() {
        FileConfiguration config = plugin.getConfig();
        String regionId = config.getString("mine_region");
        if (regionId != null) {
            World world = plugin.getServer().getWorld(config.getString("world"));
            if (world != null) {
                RegionManager regionManager = WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(world));
                if (regionManager != null) {
                    mineRegion = regionManager.getRegion(regionId);
                }
            }
        }
        List<ConfigManager.Phase> phases = configManager.getPhases();
        if (!phases.isEmpty()) {
            currentPhase = phases.get(0);
            plugin.getLogger().info("Установлена начальная фаза: " + currentPhase.getName() + " с " + currentPhase.getSpawns().size() + " материалами.");
        } else {
            plugin.getLogger().warning("Список фаз пуст. Устанавливается currentPhase = null.");
            currentPhase = null;
        }
        timeToNextPhase = configManager.getTimerInterval();
    }

    public boolean setRegion(Player player) {
        com.sk89q.worldedit.regions.Region selection = null;
        try {
            Plugin worldEdit = plugin.getServer().getPluginManager().getPlugin("WorldEdit");
            if (worldEdit instanceof WorldEditPlugin) {
                selection = ((WorldEditPlugin) worldEdit).getSession(player).getSelection(BukkitAdapter.adapt(player.getWorld()));
            } else {
                String mineName = configManager.getMineName();
                String phaseName = currentPhase != null ? currentPhase.getDisplayName() : "Не установлена";
                String timeToNext = String.valueOf(timeToNextPhase);
                player.sendMessage(MiniMessage.miniMessage().deserialize(
                        configManager.getRawMessage("worldedit_not_found"),
                        Placeholder.unparsed("mine_name", mineName),
                        Placeholder.unparsed("mine_phase", phaseName),
                        Placeholder.unparsed("time_to_next", timeToNext)
                ));
                return false;
            }
        } catch (Exception e) {
            String mineName = configManager.getMineName();
            String phaseName = currentPhase != null ? currentPhase.getDisplayName() : "Не установлена";
            String timeToNext = String.valueOf(timeToNextPhase);
            player.sendMessage(MiniMessage.miniMessage().deserialize(
                    configManager.getRawMessage("region_select_error"),
                    Placeholder.unparsed("mine_name", mineName),
                    Placeholder.unparsed("mine_phase", phaseName),
                    Placeholder.unparsed("time_to_next", timeToNext)
            ));
            return false;
        }
        World world = player.getWorld();
        RegionManager regionManager = WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(world));
        if (regionManager == null) {
            String mineName = configManager.getMineName();
            String phaseName = currentPhase != null ? currentPhase.getDisplayName() : "Не установлена";
            String timeToNext = String.valueOf(timeToNextPhase);
            player.sendMessage(MiniMessage.miniMessage().deserialize(
                    configManager.getRawMessage("worldguard_error"),
                    Placeholder.unparsed("mine_name", mineName),
                    Placeholder.unparsed("mine_phase", phaseName),
                    Placeholder.unparsed("time_to_next", timeToNext)
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
        player.sendMessage(MiniMessage.miniMessage().deserialize(
                configManager.getRawMessage("region_set_success"),
                Placeholder.unparsed("mine_name", mineName),
                Placeholder.unparsed("mine_phase", phaseName),
                Placeholder.unparsed("time_to_next", timeToNext)
        ));
        resetMine();
        return true;
    }

    public void resetMine() {
        if (mineRegion == null) {
            plugin.getLogger().warning("Регион шахты не установлен. Сброс невозможен.");
            return;
        }
        if (currentPhase == null) {
            plugin.getLogger().warning("Текущая фаза не установлена. Сброс невозможен.");
            return;
        }
        World world = plugin.getServer().getWorld(plugin.getConfig().getString("world"));
        if (world == null) {
            plugin.getLogger().warning("Мир шахты не найден. Сброс невозможен.");
            return;
        }
        BlockVector3 min = mineRegion.getMinimumPoint();
        BlockVector3 max = mineRegion.getMaximumPoint();
        for (int x = min.getBlockX(); x <= max.getBlockX(); x++) {
            for (int y = min.getBlockY(); y <= max.getBlockY(); y++) {
                for (int z = min.getBlockZ(); z <= max.getBlockZ(); z++) {
                    Location loc = new Location(world, x, y, z);
                    if (mineRegion.contains(x, y, z)) {
                        String materialName = getRandomMaterial(currentPhase.getSpawns());
                        Material material = Material.getMaterial(materialName);
                        if (material != null) {
                            loc.getBlock().setType(material);
                            plugin.getLogger().info("Установлен блок: " + materialName + " в " + x + "," + y + "," + z);
                        } else {
                            plugin.getLogger().warning("Неверный материал: " + materialName + ". Установлен STONE.");
                            loc.getBlock().setType(Material.STONE);
                        }
                    }
                }
            }
        }
    }

    private String getRandomMaterial(Map<String, Double> spawns) {
        if (spawns == null || spawns.isEmpty()) {
            plugin.getLogger().warning("Список spawns пуст или null. Содержимое: " + (spawns != null ? spawns.toString() : "null"));
            return "STONE";
        }
        double totalWeight = 0.0;
        for (Double weight : spawns.values()) {
            if (weight != null && weight > 0) {
                totalWeight += weight;
            } else {
                plugin.getLogger().warning("Некорректный вес (null или <= 0) в spawns. Игнорируется. Список: " + spawns.toString());
            }
        }
        if (totalWeight <= 0.0) {
            plugin.getLogger().warning("Сумма весов spawns <= 0. Список: " + spawns.toString());
            return "STONE";
        }
        plugin.getLogger().info("Общая сумма весов: " + totalWeight + ". Список материалов: " + spawns.toString());
        double randomValue = random.nextDouble() * totalWeight;
        double currentWeight = 0.0;
        for (Map.Entry<String, Double> entry : spawns.entrySet()) {
            if (entry.getValue() != null && entry.getValue() > 0) {
                currentWeight += entry.getValue();
                if (randomValue <= currentWeight) {
                    plugin.getLogger().info("Выбран материал: " + entry.getKey() + " с весом " + entry.getValue() + ". Случайное значение: " + randomValue);
                    return entry.getKey();
                }
            }
        }
        plugin.getLogger().warning("Не удалось выбрать материал. Возвращен STONE. Список: " + spawns.toString());
        return "STONE";
    }

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

    private void switchPhase() {
        List<ConfigManager.Phase> phases = configManager.getPhases();
        if (phases.isEmpty()) {
            plugin.getLogger().warning("Список фаз пуст. Смена фазы невозможна.");
            currentPhase = null;
            return;
        }
        int currentIndex = currentPhase != null ? phases.indexOf(currentPhase) : -1;
        currentPhase = phases.get((currentIndex + 1) % phases.size());
        resetMine();
        String mineName = configManager.getMineName();
        String phaseName = currentPhase != null ? currentPhase.getDisplayName() : "Не установлена";
        String timeToNext = String.valueOf(timeToNextPhase);
        plugin.getServer().broadcast(MiniMessage.miniMessage().deserialize(
                configManager.getRawMessage("phase_changed"),
                Placeholder.unparsed("mine_name", mineName),
                Placeholder.unparsed("mine_phase", phaseName),
                Placeholder.unparsed("time_to_next", timeToNext)
        ));
    }

    public boolean isInMineRegion(Location location) {
        if (mineRegion == null) return false;
        return mineRegion.contains(
                location.getBlockX(),
                location.getBlockY(),
                location.getBlockZ()
        );
    }

    public ConfigManager.Phase getCurrentPhase() {
        return currentPhase;
    }

    public long getTimeToNextPhase() {
        return timeToNextPhase;
    }

    public ProtectedRegion getMineRegion() {
        return mineRegion;
    }
}