package com.example.hoodmine.utils;

import com.example.hoodmine.HoodMinePlugin;
import com.example.hoodmine.config.ConfigManager;
import com.example.hoodmine.utils.RegionManagerMine;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

// Класс для регистрации плейсхолдеров в PlaceholderAPI
public class PlaceholderHook extends PlaceholderExpansion {
    private final HoodMinePlugin plugin;
    private final ConfigManager configManager;
    private final RegionManagerMine regionManager;

    public PlaceholderHook(HoodMinePlugin plugin, ConfigManager configManager, RegionManagerMine regionManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.regionManager = regionManager;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "hoodmine";
    }

    @Override
    public @NotNull String getAuthor() {
        return "abelichenko";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String identifier) {
        if (identifier.equals("mine_name")) {
            return configManager.getMineName();
        } else if (identifier.equals("time_to_next")) {
            return String.valueOf(regionManager.getTimeToNextPhase());
        } else if (identifier.equals("mine_phase")) {
            ConfigManager.Phase currentPhase = regionManager.getCurrentPhase();
            String phaseName = currentPhase != null ? currentPhase.getDisplayName() : "Не установлена";
            return LegacyComponentSerializer.legacySection().serialize(
                    MiniMessage.miniMessage().deserialize(phaseName)
            );
        }
        return null;
    }
}