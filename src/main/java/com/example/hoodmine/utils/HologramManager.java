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
    private final RegionManager regionManager;
    private final List<Hologram> holograms;

    public HologramManager(HoodMinePlugin plugin, ConfigManager configManager, RegionManager regionManager) {
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
    private void saveHolograms() {
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

        public Hologram(Location location, List<String> lines) {
            this.location = location;
            this.lines = lines;
        }

        public void update(String mineName, String phaseName, String timeToNext) {
            // Пример обновления голограммы (зависит от используемого плагина, например, HolographicDisplays)
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
            // Здесь должен быть код для обновления голограммы в зависимости от используемого API
        }
    }
}