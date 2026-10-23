package cn.superiormc.ultimatequest.utils;

import java.util.List;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

public interface SpecialMethodUtil {

    String methodID();

    void dispatchCommand(String command);

    void dispatchCommand(Player player, String command);

    void dispatchOpCommand(Player player, String command);

    ItemStack getItemObject(Object object);

    Object makeItemToObject(ItemStack item);

    void spawnEntity(Location location, EntityType entity);

    void playerTeleport(Player player, Location location);

    SkullMeta setSkullMeta(SkullMeta meta, String skull);

    String serializeSkull(SkullMeta meta);

    void setItemName(ItemMeta meta, String name, Player player);

    void setItemItemName(ItemMeta meta, String itemName, Player player);

    void setItemLore(ItemMeta meta, List<String> lore, Player player);

    default ItemMeta finalizeItemText(ItemMeta meta) {
        return meta;
    }

    void sendChat(Player player, String text);

    void sendTitle(Player player, String title, String subTitle, int fadeIn, int stay, int fadeOut);

    void sendActionBar(Player player, String message);

    void sendBossBar(Player player, String title, float progress, String color, String style);

    void sendBossBar(Player player, String title, float progress, String color, String style, String key);

    void clearBossBars(Player player);

    Inventory createNewInv(Player player, int size, String text, InventoryHolder holder);

    String legacyParse(String text);

    String getItemName(ItemMeta meta);

    String getItemItemName(ItemMeta meta);

    List<String> getItemLore(ItemMeta meta);

    String getEntityName(LivingEntity entity);

    ItemStack editItemStack(ItemStack item, Player player, ConfigurationSection section, int amount, String... args);

    ConfigurationSection serializeItemStack(ItemStack item);
}
