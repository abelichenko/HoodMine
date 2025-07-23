package com.example.hoodmine.utils;

import com.example.hoodmine.HoodMinePlugin;
import com.example.hoodmine.config.ConfigManager;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;
import java.util.Random;

// Класс для управления регионом шахты
public class RegionManager {
    private final HoodMinePlugin plugin; // Ссылка на плагин
    private final ConfigManager configManager; // Менеджер конфигурации
    private CuboidRegion mineRegion; // Регион шахты
    private int currentPhaseIndex; // Индекс текущей фазы
    private final Random random; // Генератор случайных чисел

    public RegionManager(HoodMinePlugin plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.currentPhaseIndex = 0;
        this.random = new Random();
    }

    // Установка региона шахты
    public boolean setRegion(Player player) {
        WorldEditPlugin worldEdit = (WorldEditPlugin) Bukkit.getPluginManager().getPlugin("WorldEdit");
        if (worldEdit == null) {
            player.sendMessage(configManager.getMessage("no_worldedit"));
            return false;
        }

        try {
            World world = player.getWorld();
            com.sk89q.worldedit.world.World weWorld = BukkitAdapter.adapt(world);
            com.sk89q.worldedit.LocalSession session = worldEdit.getSession(player);
            Region region = session.getSelection(weWorld);

            if (region == null || !(region instanceof CuboidRegion)) {
                player.sendMessage(configManager.getMessage("no_region"));
                return false;
            }

            mineRegion = (CuboidRegion) region;
            player.sendMessage(configManager.getMessage("setregion_ok"));
            resetMine(); // Сброс региона после установки
            return true;
        } catch (com.sk89q.worldedit.IncompleteRegionException e) {
            player.sendMessage(configManager.getMessage("no_region"));
            return false;
        } catch (Exception e) {
            player.sendMessage(MiniMessage.miniMessage().deserialize(
                    configManager.getRawMessage("setregion_error"),
                    Placeholder.unparsed("error", e.getMessage())
            ));
            plugin.getLogger().severe("Ошибка установки региона: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // Сброс шахты
    public void resetMine() {
        if (mineRegion == null || configManager.getPhases().isEmpty()) {
            plugin.getLogger().warning("Cannot reset mine: Region or phases not defined.");
            return;
        }

        ConfigManager.Phase currentPhase = configManager.getPhases().get(currentPhaseIndex);
        Map<String, Double> spawns = currentPhase.getSpawns();
        if (spawns.isEmpty()) {
            plugin.getLogger().warning("No spawns defined for phase: " + currentPhase.getId());
            return;
        }

        World world = Bukkit.getWorld(mineRegion.getWorld().getName());
        if (world == null) {
            plugin.getLogger().severe("World not found for mine region.");
            return;
        }

        // Заполнение региона блоками на основе вероятностей
        for (BlockVector3 pos : mineRegion) {
            double rand = random.nextDouble();
            double cumulative = 0.0;
            for (Map.Entry<String, Double> entry : spawns.entrySet()) {
                cumulative += entry.getValue();
                if (rand <= cumulative) {
                    BlockType blockType = BlockTypes.get(entry.getKey().toLowerCase());
                    if (blockType != null) {
                        world.getBlockAt(pos.getX(), pos.getY(), pos.getZ()).setType(BukkitAdapter.adapt(blockType).getMaterial());
                    }
                    break;
                }
            }
        }

        currentPhaseIndex = (currentPhaseIndex + 1) % configManager.getPhases().size();
    }

    // Запуск задачи обновления фаз
    public void startPhaseUpdateTask() {
        if (configManager.getPhases().isEmpty()) {
            plugin.getLogger().warning("Cannot start phase update task: No phases defined.");
            return;
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                resetMine();
            }
        }.runTaskTimer(plugin, 0L, configManager.getTimerInterval() * 20L);
    }

    // Получение текущей фазы
    public ConfigManager.Phase getCurrentPhase() {
        if (configManager.getPhases().isEmpty()) {
            plugin.getLogger().warning("No phases available, returning null.");
            return null;
        }
        return configManager.getPhases().get(currentPhaseIndex);
    }

    // Получение времени до следующей фазы
    public long getTimeToNextPhase() {
        if (configManager.getPhases().isEmpty()) {
            return 0;
        }
        return configManager.getTimerInterval();
    }

    // Проверка, находится ли локация в регионе шахты
    public boolean isInMineRegion(Location location) {
        if (mineRegion == null) return false;
        BlockVector3 pos = BukkitAdapter.asBlockVector(location);
        return mineRegion.contains(pos);
    }

    // Получение региона шахты
    public CuboidRegion getMineRegion() {
        return mineRegion;
    }
}