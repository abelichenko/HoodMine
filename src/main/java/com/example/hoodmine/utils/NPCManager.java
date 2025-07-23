package com.example.hoodmine.utils;

import com.example.hoodmine.HoodMinePlugin;
import com.example.hoodmine.commands.CommandHandler;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

// Класс для управления NPC
public class NPCManager implements Listener {
    private final HoodMinePlugin plugin;
    private final Map<UUID, Consumer<Player>> npcActions;
    private final Map<UUID, Location> npcLocations;

    public NPCManager(HoodMinePlugin plugin) {
        this.plugin = plugin;
        this.npcActions = new HashMap<>();
        this.npcLocations = new HashMap<>();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        loadNPCs();
    }

    // Спавн NPC
    public void spawnNPC(Player player, Consumer<Player> action) {
        Location location = player.getLocation();
        Villager villager = location.getWorld().spawn(location, Villager.class, v -> {
            String name = LegacyComponentSerializer.legacySection().serialize(
                    MiniMessage.miniMessage().deserialize("<green>Шахтёр NPC",
                            Placeholder.unparsed("mine_name", plugin.getConfigManager().getMineName()),
                            Placeholder.unparsed("mine_phase", plugin.getRegionManager().getCurrentPhase() != null ? plugin.getRegionManager().getCurrentPhase().getDisplayName() : "Не установлена"),
                            Placeholder.unparsed("time_to_next", String.valueOf(plugin.getRegionManager().getTimeToNextPhase())))
            );
            v.setCustomName(name);
            v.setCustomNameVisible(true);
            v.setAI(false);
            v.setInvulnerable(true);
        });
        npcActions.put(villager.getUniqueId(), action);
        npcLocations.put(villager.getUniqueId(), location);
        saveNPCs();
        player.sendMessage(LegacyComponentSerializer.legacySection().serialize(
                MiniMessage.miniMessage().deserialize("<green>NPC успешно создан!",
                        Placeholder.unparsed("mine_name", plugin.getConfigManager().getMineName()),
                        Placeholder.unparsed("mine_phase", plugin.getRegionManager().getCurrentPhase() != null ? plugin.getRegionManager().getCurrentPhase().getDisplayName() : "Не установлена"),
                        Placeholder.unparsed("time_to_next", String.valueOf(plugin.getRegionManager().getTimeToNextPhase())))
        ));
    }

    // Удаление NPC
    public void removeNPC(Player player) {
        Location location = player.getLocation();
        player.getWorld().getEntitiesByClass(Villager.class).stream()
                .filter(v -> v.getCustomName() != null && v.getCustomName().contains("Шахтёр NPC"))
                .filter(v -> v.getLocation().distanceSquared(location) < 25)
                .findFirst()
                .ifPresent(v -> {
                    npcActions.remove(v.getUniqueId());
                    npcLocations.remove(v.getUniqueId());
                    v.remove();
                    saveNPCs();
                    player.sendMessage(LegacyComponentSerializer.legacySection().serialize(
                            MiniMessage.miniMessage().deserialize("<green>NPC успешно удалён!",
                                    Placeholder.unparsed("mine_name", plugin.getConfigManager().getMineName()),
                                    Placeholder.unparsed("mine_phase", plugin.getRegionManager().getCurrentPhase() != null ? plugin.getRegionManager().getCurrentPhase().getDisplayName() : "Не установлена"),
                                    Placeholder.unparsed("time_to_next", String.valueOf(plugin.getRegionManager().getTimeToNextPhase())))
                    ));
                });
    }

    // Сохранение NPC в instances.yml
    private void saveNPCs() {
        File file = new File(plugin.getDataFolder(), "instances.yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        config.set("npcs", null);
        int index = 0;
        for (Map.Entry<UUID, Location> entry : npcLocations.entrySet()) {
            Location loc = entry.getValue();
            config.set("npcs." + index + ".uuid", entry.getKey().toString());
            config.set("npcs." + index + ".world", loc.getWorld().getName());
            config.set("npcs." + index + ".x", loc.getX());
            config.set("npcs." + index + ".y", loc.getY());
            config.set("npcs." + index + ".z", loc.getZ());
            index++;
        }
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Ошибка сохранения NPC: " + e.getMessage());
        }
    }

    // Загрузка NPC из instances.yml
    private void loadNPCs() {
        File file = new File(plugin.getDataFolder(), "instances.yml");
        if (!file.exists()) return;
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection npcsSection = config.getConfigurationSection("npcs");
        if (npcsSection == null) return;
        CommandHandler handler = new CommandHandler(plugin, plugin.getConfigManager(), plugin.getRegionManager(), plugin.getQuestManager());
        for (String key : npcsSection.getKeys(false)) {
            String uuidStr = npcsSection.getString(key + ".uuid");
            String worldName = npcsSection.getString(key + ".world");
            double x = npcsSection.getDouble(key + ".x");
            double y = npcsSection.getDouble(key + ".y");
            double z = npcsSection.getDouble(key + ".z");
            World world = Bukkit.getWorld(worldName);
            if (world == null) {
                plugin.getLogger().warning("Мир " + worldName + " не найден для NPC.");
                continue;
            }
            UUID uuid = UUID.fromString(uuidStr);
            Location location = new Location(world, x, y, z);
            Villager villager = world.spawn(location, Villager.class, v -> {
                String name = LegacyComponentSerializer.legacySection().serialize(
                        MiniMessage.miniMessage().deserialize("<green>Шахтёр NPC",
                                Placeholder.unparsed("mine_name", plugin.getConfigManager().getMineName()),
                                Placeholder.unparsed("mine_phase", plugin.getRegionManager().getCurrentPhase() != null ? plugin.getRegionManager().getCurrentPhase().getDisplayName() : "Не установлена"),
                                Placeholder.unparsed("time_to_next", String.valueOf(plugin.getRegionManager().getTimeToNextPhase())))
                );
                v.setCustomName(name);
                v.setCustomNameVisible(true);
                v.setAI(false);
                v.setInvulnerable(true);
            });
            npcActions.put(villager.getUniqueId(), p -> handler.handleQuests(p));
            npcLocations.put(villager.getUniqueId(), location);
        }
    }

    // Обработка взаимодействия с NPC
    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (event.getRightClicked() instanceof Villager villager) {
            Consumer<Player> action = npcActions.get(villager.getUniqueId());
            if (action != null) {
                action.accept(event.getPlayer());
                event.setCancelled(true);
            }
        }
    }
}