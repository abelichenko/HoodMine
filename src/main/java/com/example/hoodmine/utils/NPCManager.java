package com.example.hoodmine.utils;

import com.example.hoodmine.HoodMinePlugin;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

// Класс для управления NPC
public class NPCManager implements Listener {
    private final HoodMinePlugin plugin;
    private final Map<UUID, Consumer<Player>> npcActions;

    public NPCManager(HoodMinePlugin plugin) {
        this.plugin = plugin;
        this.npcActions = new HashMap<>();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    // Спавн NPC
    public void spawnNPC(Player player, Consumer<Player> action) {
        Location location = player.getLocation();
        Villager villager = location.getWorld().spawn(location, Villager.class, v -> {
            // Преобразуем Component в строку с использованием LegacyComponentSerializer
            String name = LegacyComponentSerializer.legacySection().serialize(
                    MiniMessage.miniMessage().deserialize("<green>Шахтёр NPC")
            );
            v.setCustomName(name);
            v.setCustomNameVisible(true);
            v.setAI(false);
            v.setInvulnerable(true);
        });
        npcActions.put(villager.getUniqueId(), action);
        player.sendMessage(MiniMessage.miniMessage().deserialize("<green>NPC успешно создан!"));
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
                    v.remove();
                    player.sendMessage(MiniMessage.miniMessage().deserialize("<green>NPC успешно удалён!"));
                });
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