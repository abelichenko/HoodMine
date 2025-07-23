package com.example.hoodmine.utils;

import com.example.hoodmine.HoodMinePlugin;
import com.example.hoodmine.config.ConfigManager;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

// Класс для управления голограммами
public class HologramManager {
    private final HoodMinePlugin plugin;
    private final ConfigManager configManager;
    private final RegionManagerMine regionManager;
    private final List<Hologram> holograms;

    public HologramManager(HoodMinePlugin plugin, ConfigManager configManager, RegionManagerMine regionManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.regionManager = regionManager;
        this.holograms = new ArrayList<>();
        loadHolograms();
        startHologramUpdater();
    }

    // Загрузка голограмм из instances.yml
    private void loadHolograms() {
        File file = new File(plugin.getDataFolder(), "instances.yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection hologramSection = config.getConfigurationSection("holograms");
        if (hologramSection == null) return;
        for (String key : hologramSection.getKeys(false)) {
            Location location = (Location) hologramSection.get(key + ".location");
            List<String> lines = hologramSection.getStringList(key + ".lines");
            holograms.add(new Hologram(location, lines));
        }
    }

    // Сохранение голограмм в instances.yml
    public void saveHolograms() {
        File file = new File(plugin.getDataFolder(), "instances.yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        config.set("holograms", null);
        for (int i = 0; i < holograms.size(); i++) {
            Hologram hologram = holograms.get(i);
            config.set("holograms." + i + ".location", hologram.location);
            config.set("holograms." + i + ".lines", hologram.lines);
        }
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Ошибка сохранения голограмм: " + e.getMessage());
        }
    }

    // Создание новой голограммы
    public void createHologram(Location location, List<String> lines) {
        holograms.add(new Hologram(location, lines));
        saveHolograms();
        updateHolograms();
    }

    // Удаление голограммы
    public void removeHologram(Location location) {
        holograms.removeIf(hologram -> hologram.location.distanceSquared(location) < 1);
        saveHolograms();
        updateHolograms();
    }

    // Обновление голограмм
    private void startHologramUpdater() {
        new BukkitRunnable() {
            @Override
            public void run() {
                updateHolograms();
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    // Обновление содержимого голограмм
    private void updateHolograms() {
        String mineName = configManager.getMineName();
        String phaseName = regionManager.getCurrentPhase() != null ? regionManager.getCurrentPhase().getDisplayName() : "Не установлена";
        String timeToNext = String.valueOf(regionManager.getTimeToNextPhase());
        for (Hologram hologram : holograms) {
            hologram.update(mineName, phaseName, timeToNext);
        }
    }

    // Структура для голограммы
    private static class Hologram {
        private final Location location;
        private final List<String> lines;
        private Object hologramInstance; // Заглушка для API голограмм (например, HolographicDisplays)

        public Hologram(Location location, List<String> lines) {
            this.location = location;
            this.lines = new ArrayList<>(lines);
            // Инициализация голограммы (зависит от API)
        }

        public void update(String mineName, String phaseName, String timeToNext) {
            // Обновление голограммы с использованием плейсхолдеров
            List<String> updatedLines = new ArrayList<>();
            for (String line : lines) {
                updatedLines.add(LegacyComponentSerializer.legacySection().serialize(
                        MiniMessage.miniMessage().deserialize(
                                line,
                                Placeholder.unparsed("mine_name", mineName),
                                Placeholder.unparsed("mine_phase", phaseName),
                                Placeholder.unparsed("time_to_next", timeToNext)
                        )
                ));
            }
            // Здесь должен быть код для обновления голограммы через API (например, HolographicDisplays)
            // Пример: hologramInstance.clearLines(); updatedLines.forEach(hologramInstance::appendTextLine);
        }
    }
}