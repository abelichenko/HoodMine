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
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Random;

// Класс для управления регионом шахты
public class RegionManager {
    private final HoodMinePlugin plugin;
    private final ConfigManager configManager;
    private CuboidRegion mineRegion;
    private int currentPhaseIndex;
    private final Random random;
    private long timeToNextPhase;

    public RegionManager(HoodMinePlugin plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.currentPhaseIndex = 0;
        this.random = new Random();
        this.timeToNextPhase = configManager.getTimerInterval();
        loadRegion(); // Загрузка региона при инициализации
    }

    // Установка региона шахты
    public boolean setRegion(Player player) {
        WorldEditPlugin worldEdit = (WorldEditPlugin) Bukkit.getPluginManager().getPlugin("WorldEdit");
        if (worldEdit == null) {
            player.sendMessage(LegacyComponentSerializer.legacySection().serialize(
                    MiniMessage.miniMessage().deserialize(configManager.getRawMessage("no_worldedit"))
            ));
            return false;
        }

        try {
            World world = player.getWorld();
            com.sk89q.worldedit.world.World weWorld = BukkitAdapter.adapt(world);
            com.sk89q.worldedit.LocalSession session = worldEdit.getSession(player);
            Region region = session.getSelection(weWorld);

            if (region == null || !(region instanceof CuboidRegion)) {
                player.sendMessage(LegacyComponentSerializer.legacySection().serialize(
                        MiniMessage.miniMessage().deserialize(configManager.getRawMessage("no_region"))
                ));
                return false;
            }

            mineRegion = (CuboidRegion) region;
            saveRegion(); // Сохраняем регион
            player.sendMessage(LegacyComponentSerializer.legacySection().serialize(
                    MiniMessage.miniMessage().deserialize(configManager.getRawMessage("setregion_ok"))
            ));
            resetMine(); // Сброс региона после установки
            return true;
        } catch (com.sk89q.worldedit.IncompleteRegionException e) {
            player.sendMessage(LegacyComponentSerializer.legacySection().serialize(
                    MiniMessage.miniMessage().deserialize(configManager.getRawMessage("no_region"))
            ));
            return false;
        } catch (Exception e) {
            player.sendMessage(LegacyComponentSerializer.legacySection().serialize(
                    MiniMessage.miniMessage().deserialize(
                            configManager.getRawMessage("setregion_error"),
                            Placeholder.unparsed("error", e.getMessage())
                    )
            ));
            plugin.getLogger().severe("Ошибка установки региона: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // Сохранение региона в instances.yml
    private void saveRegion() {
        if (mineRegion == null) return;
        File file = new File(plugin.getDataFolder(), "instances.yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        BlockVector3 min = mineRegion.getMinimumPoint();
        BlockVector3 max = mineRegion.getMaximumPoint();
        config.set("mine_region.world", mineRegion.getWorld().getName());
        config.set("mine_region.min.x", min.getX());
        config.set("mine_region.min.y", min.getY());
        config.set("mine_region.min.z", min.getZ());
        config.set("mine_region.max.x", max.getX());
        config.set("mine_region.max.y", max.getY());
        config.set("mine_region.max.z", max.getZ());
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Ошибка сохранения региона: " + e.getMessage());
        }
    }

    // Загрузка региона из instances.yml
    private void loadRegion() {
        File file = new File(plugin.getDataFolder(), "instances.yml");
        if (!file.exists()) return;
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        String worldName = config.getString("mine_region.world");
        if (worldName == null) return;
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            plugin.getLogger().warning("Мир " + worldName + " не найден для региона шахты.");
            return;
        }
        int minX = config.getInt("mine_region.min.x");
        int minY = config.getInt("mine_region.min.y");
        int minZ = config.getInt("mine_region.min.z");
        int maxX = config.getInt("mine_region.max.x");
        int maxY = config.getInt("mine_region.max.y");
        int maxZ = config.getInt("mine_region.max.z");
        mineRegion = new CuboidRegion(
                BukkitAdapter.adapt(world),
                BlockVector3.at(minX, minY, minZ),
                BlockVector3.at(maxX, maxY, maxZ)
        );
    }

    // Сброс шахты
    public void resetMine() {
        if (mineRegion == null || configManager.getPhases().isEmpty()) {
            plugin.getLogger().warning("Невозможно сбросить шахту: регион или фазы не определены.");
            return;
        }

        ConfigManager.Phase currentPhase = configManager.getPhases().get(currentPhaseIndex);
        Map<String, Double> spawns = currentPhase.getSpawns();
        if (spawns.isEmpty()) {
            plugin.getLogger().warning("Для фазы нет спавнов: " + currentPhase.getId());
            return;
        }

        World world = Bukkit.getWorld(mineRegion.getWorld().getName());
        if (world == null) {
            plugin.getLogger().severe("Мир не найден для региона шахты.");
            return;
        }

        // Заполнение региона блоками
        for (BlockVector3 pos : mineRegion) {
            double rand = random.nextDouble();
            double cumulative = 0.0;
            boolean set = false;
            for (Map.Entry<String, Double> entry : spawns.entrySet()) {
                cumulative += entry.getValue();
                if (rand <= cumulative) {
                    BlockType blockType = BlockTypes.get(entry.getKey().toLowerCase());
                    if (blockType != null) {
                        Material material = Material.getMaterial(entry.getKey().toUpperCase());
                        if (material != null) {
                            world.getBlockAt(pos.getX(), pos.getY(), pos.getZ()).setType(material);
                            set = true;
                        }
                    }
                    break;
                }
            }
            // Если блок не установлен, ставим STONE
            if (!set) {
                world.getBlockAt(pos.getX(), pos.getY(), pos.getZ()).setType(Material.STONE);
            }
        }

        currentPhaseIndex = (currentPhaseIndex + 1) % configManager.getPhases().size();
        timeToNextPhase = configManager.getTimerInterval(); // Сбрасываем таймер
    }

    // Запуск задачи обновления фаз
    public void startPhaseUpdateTask() {
        if (configManager.getPhases().isEmpty()) {
            plugin.getLogger().warning("Невозможно запустить задачу обновления фаз: фазы не определены.");
            return;
        }

        new BukkitRunnable() {
            long ticksRemaining = configManager.getTimerInterval() * 20L;

            @Override
            public void run() {
                ticksRemaining -= 20L; // Уменьшаем на 1 секунду (20 тиков)
                timeToNextPhase = ticksRemaining / 20L;
                if (ticksRemaining <= 0) {
                    resetMine();
                    ticksRemaining = configManager.getTimerInterval() * 20L; // Сбрасываем таймер
                }
            }
        }.runTaskTimer(plugin, 0L, 20L); // Запускаем каждую секунду
    }

    // Получение текущей фазы
    public ConfigManager.Phase getCurrentPhase() {
        if (configManager.getPhases().isEmpty()) {
            plugin.getLogger().warning("Фазы отсутствуют, возвращается null.");
            return null;
        }
        return configManager.getPhases().get(currentPhaseIndex);
    }

    // Получение времени до следующей фазы
    public long getTimeToNextPhase() {
        return timeToNextPhase;
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