package cn.superiormc.ultimatequest.objects;

import cn.superiormc.ultimatequest.methods.BuildItem;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public final class ObjectCustomItem {

    private final String id;

    private final YamlConfiguration config;

    private final ConfigurationSection baseItem;

    public ObjectCustomItem(String id, YamlConfiguration config) {
        this.id = id;
        this.config = config;
        baseItem = config.getConfigurationSection("base-item");
        if (baseItem == null) {
            throw new IllegalArgumentException("Missing base-item in items/" + id + ".yml");
        }
    }

    public String getId() {
        return id;
    }

    public boolean isEnabled() {
        return config.getBoolean("enabled", true);
    }

    public ItemStack build(Player player, String... args) {
        return BuildItem.buildItemStack(player, baseItem, args);
    }
}
