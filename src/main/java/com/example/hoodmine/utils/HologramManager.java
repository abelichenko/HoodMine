package com.example.hoodmine.utils;

import com.example.hoodmine.HoodMinePlugin;
import com.example.hoodmine.config.ConfigManager;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;

// Класс для управления голограммами
public class HologramManager {
    private final HoodMinePlugin plugin; // Ссылка на плагин
    private final ConfigManager configManager; // Менеджер конфигурации
    private ArmorStand hologramLine1; // Первая строка голограммы
    private ArmorStand hologramLine2; // Вторая строка голограммы

    public HologramManager(HoodMinePlugin plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    // Создание голограммы над центром региона
    public void createHologram(Location center, String mineName, String displayName, long timeToNext) {
        if (hologramLine1 != null) {
            hologramLine1.remove();
        }
        if (hologramLine2 != null) {
            hologramLine2.remove();
        }

        hologramLine1 = (ArmorStand) center.getWorld().spawnEntity(center.clone().add(0, 2, 0), EntityType.ARMOR_STAND);
        hologramLine1.setInvisible(true);
        hologramLine1.setGravity(false);
        hologramLine1.setCustomName(ColorUtils.colorize("#FFFF55", displayName + " " + mineName));
        hologramLine1.setCustomNameVisible(true);

        hologramLine2 = (ArmorStand) center.getWorld().spawnEntity(center.clone().add(0, 1.75, 0), EntityType.ARMOR_STAND);
        hologramLine2.setInvisible(true);
        hologramLine2.setGravity(false);
        hologramLine2.setCustomName(ColorUtils.colorize("#AAAAAA", "До смены фазы: " + timeToNext));
        hologramLine2.setCustomNameVisible(true);
    }

    // Обновление времени на голограмме
    public void updateHologramTime(long timeToNext) {
        if (hologramLine2 != null) {
            hologramLine2.setCustomName(ColorUtils.colorize("#AAAAAA", "До смены фазы: " + timeToNext));
        }
    }
}