package com.example.hoodmine.commands;

import com.example.hoodmine.HoodMinePlugin;
import com.example.hoodmine.config.ConfigManager;
import com.example.hoodmine.quests.QuestManager;
import com.example.hoodmine.utils.RegionManagerMine;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

// Класс для обработки подкоманд и событий GUI
public class CommandHandler implements Listener {
    private final HoodMinePlugin plugin;
    private final ConfigManager configManager;
    private final RegionManagerMine regionManager;
    private final QuestManager questManager;

    public CommandHandler(HoodMinePlugin plugin, ConfigManager configManager, RegionManagerMine regionManager, QuestManager questManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.regionManager = regionManager;
        this.questManager = questManager;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public boolean handleSetRegion(Player player) {
        return regionManager.setRegion(player);
    }

    public boolean handleReset(Player player) {
        regionManager.resetMine();
        String mineName = configManager.getMineName();
        String phaseName = regionManager.getCurrentPhase() != null ? regionManager.getCurrentPhase().getDisplayName() : "Не установлена";
        String timeToNext = String.valueOf(regionManager.getTimeToNextPhase());
        player.sendMessage(MiniMessage.miniMessage().deserialize(
                configManager.getRawMessage("mine_reset"),
                Placeholder.unparsed("mine_name", mineName),
                Placeholder.unparsed("mine_phase", phaseName),
                Placeholder.unparsed("time_to_next", timeToNext)
        ));
        return true;
    }

    public boolean handleSeller(Player player) {
        Inventory inventory = Bukkit.createInventory(player, 27, MiniMessage.miniMessage().deserialize(
                configManager.getRawMessage("seller_title")
        ));
        double multiplier = questManager.getPlayerMultiplier(player.getUniqueId());
        for (ConfigManager.SellerItem item : configManager.getSellerItems().values()) {
            ItemStack itemStack = new ItemStack(item.getMaterial());
            ItemMeta meta = itemStack.getItemMeta();
            if (meta != null) {
                meta.displayName(MiniMessage.miniMessage().deserialize(item.getDisplayName()));
                List<Component> lore = new ArrayList<>();
                for (String line : item.getLore()) {
                    lore.add(MiniMessage.miniMessage().deserialize(
                            line,
                            Placeholder.unparsed("price", String.format("%.2f", item.getPrice() * multiplier)),
                            Placeholder.unparsed("multiplier", String.format("%.2f", multiplier))
                    ));
                }
                meta.lore(lore);
                itemStack.setItemMeta(meta);
            }
            inventory.addItem(itemStack);
        }
        player.openInventory(inventory);
        return true;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        String title = MiniMessage.miniMessage().serialize(event.getView().title());
        String expectedTitle = MiniMessage.miniMessage().serialize(MiniMessage.miniMessage().deserialize(
                configManager.getRawMessage("seller_title")
        ));
        if (!title.equals(expectedTitle)) {
            return;
        }
        event.setCancelled(true);
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) {
            return;
        }
        ConfigManager.SellerItem sellerItem = null;
        for (ConfigManager.SellerItem item : configManager.getSellerItems().values()) {
            if (item.getMaterial() == clickedItem.getType()) {
                sellerItem = item;
                break;
            }
        }
        if (sellerItem == null) {
            return;
        }
        double multiplier = questManager.getPlayerMultiplier(player.getUniqueId());
        double price = sellerItem.getPrice() * multiplier;
        int amount = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == sellerItem.getMaterial()) {
                amount += item.getAmount();
            }
        }
        if (amount == 0) {
            String mineName = configManager.getMineName();
            String phaseName = regionManager.getCurrentPhase() != null ? regionManager.getCurrentPhase().getDisplayName() : "Не установлена";
            String timeToNext = String.valueOf(regionManager.getTimeToNextPhase());
            player.sendMessage(MiniMessage.miniMessage().deserialize(
                    configManager.getRawMessage("no_items_to_sell"),
                    Placeholder.unparsed("mine_name", mineName),
                    Placeholder.unparsed("mine_phase", phaseName),
                    Placeholder.unparsed("time_to_next", timeToNext)
            ));
            return;
        }
        double totalMoney = price * amount;
        // Предполагается интеграция с Vault
        // Economy econ = VaultHook.getEconomy();
        // if (econ != null) econ.depositPlayer(player, totalMoney);
        player.getInventory().removeItem(new ItemStack(sellerItem.getMaterial(), amount));
        String mineName = configManager.getMineName();
        String phaseName = regionManager.getCurrentPhase() != null ? regionManager.getCurrentPhase().getDisplayName() : "Не установлена";
        String timeToNext = String.valueOf(regionManager.getTimeToNextPhase());
        player.sendMessage(MiniMessage.miniMessage().deserialize(
                configManager.getRawMessage("items_sold"),
                Placeholder.unparsed("amount", String.valueOf(amount)),
                Placeholder.unparsed("material", sellerItem.getMaterial().name()),
                Placeholder.unparsed("money", String.format("%.2f", totalMoney)),
                Placeholder.unparsed("multiplier", String.format("%.2f", multiplier)),
                Placeholder.unparsed("mine_name", mineName),
                Placeholder.unparsed("mine_phase", phaseName),
                Placeholder.unparsed("time_to_next", timeToNext)
        ));
    }

    public boolean handleQuests(Player player) {
        List<ConfigManager.Quest> quests = configManager.getQuests();
        if (quests.isEmpty()) {
            plugin.getLogger().warning("Список квестов пуст при открытии GUI для " + player.getName());
            String mineName = configManager.getMineName();
            String phaseName = regionManager.getCurrentPhase() != null ? regionManager.getCurrentPhase().getDisplayName() : "Не установлена";
            String timeToNext = String.valueOf(regionManager.getTimeToNextPhase());
            player.sendMessage(MiniMessage.miniMessage().deserialize(
                    "<red>Квесты не найдены.",
                    Placeholder.unparsed("mine_name", mineName),
                    Placeholder.unparsed("mine_phase", phaseName),
                    Placeholder.unparsed("time_to_next", timeToNext)
            ));
            return true;
        }
        Inventory inventory = Bukkit.createInventory(player, 27, MiniMessage.miniMessage().deserialize(
                configManager.getRawMessage("quests_title")
        ));
        for (ConfigManager.Quest quest : quests) {
            ItemStack item = questManager.getQuestItem(quest, player);
            if (item != null) {
                inventory.addItem(item);
                plugin.getLogger().info("Добавлен квест " + quest.getId() + " в GUI для " + player.getName());
            } else {
                plugin.getLogger().warning("Не удалось создать предмет для квеста " + quest.getId() + " для " + player.getName());
            }
        }
        player.openInventory(inventory);
        return true;
    }

    public boolean handleHelp(CommandSender sender) {
        String mineName = configManager.getMineName();
        String phaseName = regionManager.getCurrentPhase() != null ? regionManager.getCurrentPhase().getDisplayName() : "Не установлена";
        String timeToNext = String.valueOf(regionManager.getTimeToNextPhase());
        for (String message : configManager.getRawMessage("help").split("\n")) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize(
                    message,
                    Placeholder.unparsed("mine_name", mineName),
                    Placeholder.unparsed("mine_phase", phaseName),
                    Placeholder.unparsed("time_to_next", timeToNext)
            ));
        }
        return true;
    }
}