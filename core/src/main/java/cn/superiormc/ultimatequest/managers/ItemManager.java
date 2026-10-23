package cn.superiormc.ultimatequest.managers;

import cn.superiormc.ultimatequest.UltimateQuest;
import cn.superiormc.ultimatequest.methods.DebuildItem;
import cn.superiormc.ultimatequest.objects.ObjectCustomItem;
import cn.superiormc.ultimatequest.utils.SchedulerUtil;
import cn.superiormc.ultimatequest.utils.TextUtil;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class ItemManager extends AbstractManager {

    private final Map<String, ObjectCustomItem> customItems = new LinkedHashMap<>();

    public static ItemManager itemManager;

    public ItemManager() {
        itemManager = this;
        initSavedItems();
    }

    public void initSavedItems() {
        customItems.clear();
        File directory = new File(UltimateQuest.instance.getDataFolder(), "items");
        if (!directory.exists() && !directory.mkdirs()) {
            ErrorManager.errorManager.sendErrorMessage("§cError: Could not create custom item directory: " + directory);
            return;
        }
        File[] files = directory.listFiles((ignored, name) -> name.endsWith(".yml"));
        if (files == null) {
            return;
        }
        for (File file : files) {
            String id = file.getName().substring(0, file.getName().length() - 4).toLowerCase(Locale.ROOT);
            try {
                YamlConfiguration config = new YamlConfiguration();
                config.load(file);
                ObjectCustomItem customItem = new ObjectCustomItem(id, config);
                if (!customItem.isEnabled()) {
                    continue;
                }
                if (customItems.putIfAbsent(id, customItem) != null) {
                    throw new IllegalArgumentException("Duplicate custom item id: " + id);
                }
                TextUtil.sendMessage(null, TextUtil.pluginPrefix() + " §fLoaded custom item: " + id + ".yml!");
            } catch (Throwable throwable) {
                ErrorManager.errorManager.sendErrorMessage("§cError: Failed to load custom item " + file.getName() + ": " + throwable.getMessage());
            }
        }
    }

    public boolean saveMainHandItem(Player player, String key) {
        YamlConfiguration config = newDefinition();
        ItemStack item = player.getInventory().getItemInMainHand();
        config.getConfigurationSection("base-item").set("item", UltimateQuest.methodUtil.makeItemToObject(item));
        config.getConfigurationSection("base-item").set("amount", item.getAmount());
        return saveDefinition(key, config);
    }

    public boolean saveMainHandItemFormat(Player player, String key) {
        YamlConfiguration config = newDefinition();
        DebuildItem.debuildItem(player.getInventory().getItemInMainHand(), config.getConfigurationSection("base-item"));
        return saveDefinition(key, config);
    }

    public ItemStack getItemByKey(Player player, String key) {
        ObjectCustomItem customItem = customItems.get(key.toLowerCase(Locale.ROOT));
        if (customItem == null) {
            return null;
        }
        try {
            return customItem.build(player);
        } catch (Throwable throwable) {
            ErrorManager.errorManager.sendErrorMessage("§cError: Failed to build custom item " + customItem.getId() + ": " + throwable.getMessage());
            return null;
        }
    }

    public Collection<String> getItemKeys() {
        return List.copyOf(customItems.keySet());
    }

    public Map<String, ObjectCustomItem> getCustomItems() {
        return Map.copyOf(customItems);
    }

    /**
     * Collects non-persistent custom-attribute modifiers from equipped custom items.
     */
    private YamlConfiguration newDefinition() {
        YamlConfiguration config = new YamlConfiguration();
        config.createSection("base-item");
        return config;
    }

    private boolean saveDefinition(String key, YamlConfiguration config) {
        final String id;
        final ObjectCustomItem customItem;
        try {
            id = normalizeNewId(key);
            customItem = new ObjectCustomItem(id, config);
        } catch (Throwable throwable) {
            ErrorManager.errorManager.sendErrorMessage("§cError: Failed to save custom item " + key + ": " + throwable.getMessage());
            return false;
        }
        File directory = new File(UltimateQuest.instance.getDataFolder(), "items");
        if (!directory.exists() && !directory.mkdirs()) {
            ErrorManager.errorManager.sendErrorMessage("§cError: Could not create custom item directory: " + directory);
            return false;
        }
        customItems.put(id, customItem);
        String yaml = config.saveToString();
        Path path = new File(directory, id + ".yml").toPath();
        SchedulerUtil.runTaskAsynchronously(() -> {
            try {
                Files.writeString(path, yaml, StandardCharsets.UTF_8);
            } catch (IOException exception) {
                SchedulerUtil.runSync(() -> ErrorManager.errorManager.sendErrorMessage("§cError: Failed to save custom item " + id + ".yml: " + exception.getMessage()));
            }
        });
        return true;
    }

    private String normalizeNewId(String key) {
        if (key == null) {
            throw new IllegalArgumentException("Custom item id cannot be null");
        }
        String normalized = key.toLowerCase(Locale.ROOT);
        if (!normalized.matches("[a-z0-9_-]+")) {
            throw new IllegalArgumentException("Custom item id may only contain lowercase letters, numbers, '_' and '-'");
        }
        return normalized;
    }
}
