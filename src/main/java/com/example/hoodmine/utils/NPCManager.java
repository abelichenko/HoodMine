package com.example.hoodmine.utils;

import com.example.hoodmine.HoodMinePlugin;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;

import java.util.function.Consumer;

// Класс для управления NPC
public class NPCManager {
    private final HoodMinePlugin plugin; // Ссылка на плагин

    public NPCManager(HoodMinePlugin plugin) {
        this.plugin = plugin;
    }

    // Спавн NPC с заданным именем и действием при взаимодействии
    public void spawnNPC(Location location, String name, Consumer<Player> onInteract) {
        ArmorStand npc = (ArmorStand) location.getWorld().spawnEntity(location, EntityType.ARMOR_STAND);
        npc.setCustomName(ColorUtils.colorize("#55FF55", name));
        npc.setCustomNameVisible(true);
        npc.setInvisible(true);
        npc.setInvulnerable(true);
        npc.setGravity(false);

        // Регистрация события для взаимодействия с NPC
        plugin.getServer().getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
                if (event.getRightClicked().equals(npc)) {
                    onInteract.accept(event.getPlayer());
                }
            }
        }, plugin);
    }
}