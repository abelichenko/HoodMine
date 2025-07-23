package com.example.hoodmine.quests;

import java.util.List;

// Класс для представления квеста
public class Quest {
    private final String id; // Уникальный идентификатор квеста
    private final String title; // Название квеста
    private final String targetBlock; // Целевой блок для квеста
    private final int amount; // Необходимое количество блоков
    private final List<String> rewards; // Список наград

    public Quest(String id, String title, String targetBlock, int amount, List<String> rewards) {
        this.id = id;
        this.title = title;
        this.targetBlock = targetBlock;
        this.amount = amount;
        this.rewards = rewards;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getTargetBlock() {
        return targetBlock;
    }

    public int getAmount() {
        return amount;
    }

    public List<String> getRewards() {
        return rewards;
    }
}