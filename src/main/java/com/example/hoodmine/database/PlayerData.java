package com.example.hoodmine.database;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.example.hoodmine.quests.Quest;

// Класс для хранения данных игрока
public class PlayerData {
    private final Map<String, Integer> progress; // Прогресс по квестам
    private final Map<String, Boolean> completed; // Завершённые квесты

    public PlayerData() {
        this.progress = new HashMap<>();
        this.completed = new HashMap<>();
    }

    // Получение прогресса по квестам
    public Map<String, Integer> getProgress() {
        return progress;
    }

    // Получение завершённых квестов
    public Map<String, Boolean> getCompleted() {
        return completed;
    }

    // Получение множителя награды на основе завершённых квестов
    public double getRewardMultiplier(int totalQuests) {
        long completedCount = completed.values().stream().filter(b -> b).count();
        return 1.0 + (completedCount * 0.1);
    }

    // Получение количества добытых блоков для квеста
    public int getQuestProgress(String questId, List<Quest> quests) {
        return progress.getOrDefault(questId, 0);
    }

    // Получение количества блоков, необходимых для завершения квеста
    public int getQuestAmount(String questId, List<Quest> quests) {
        return quests.stream()
                .filter(q -> q.getId().equals(questId))
                .findFirst()
                .map(Quest::getAmount)
                .orElse(0);
    }
}