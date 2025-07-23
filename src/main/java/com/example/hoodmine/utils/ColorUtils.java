package com.example.hoodmine.utils;

import net.md_5.bungee.api.ChatColor;

// Класс для обработки цветных сообщений с использованием hex-кодов
public class ColorUtils {
    public static String colorize(String hex, String message) {
        return ChatColor.of(hex) + message;
    }
}