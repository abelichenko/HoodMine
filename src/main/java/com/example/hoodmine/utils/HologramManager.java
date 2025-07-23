package com.example.hoodmine.utils;

import com.example.hoodmine.HoodMinePlugin;
import com.example.hoodmine.config.ConfigManager;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;

import java.util.ArrayList;
import java.util.List;

// Класс для управления голограммами
public class HologramManager {
    private final HoodMinePlugin plugin;
    private final ConfigManager configManager;
    private final List<ArmorStand> holograms;

    public HologramManager(HoodMinePlugin plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.holograms = new ArrayList<>();
    }

    // Создание голограммы
    public void createHologram(Location location, String text) {
        ArmorStand hologram = (ArmorStand) location.getWorld().spawnEntity(location, EntityType.ARMOR_STAND);
        hologram.setGravity(false);
        hologram.setCanPickupItems(false);
        hologram.setCustomName(MiniMessage.miniMessage().deserialize(text).toString());
        hologram.setCustomNameVisible(true);
        hologram.setVisible(false);
        hologram.setInvulnerable(true);
        holograms.add(hologram);
    }

    // Удаление всех голограмм
    public void removeAllHolograms() {
        for (ArmorStand hologram : holograms) {
            hologram.remove();
        }
        holograms.clear();
    }
}