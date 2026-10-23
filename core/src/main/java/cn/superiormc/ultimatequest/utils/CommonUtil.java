package cn.superiormc.ultimatequest.utils;

import cn.superiormc.ultimatequest.UltimateQuest;
import cn.superiormc.ultimatequest.managers.LanguageManager;
import io.lumine.mythic.bukkit.BukkitAdapter;
import io.lumine.mythic.bukkit.MythicBukkit;
import java.io.File;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

public class CommonUtil {

    public static LocalDateTime getNowTime() {
        return LocalDateTime.now();
    }

    public static LocalDateTime stringToTime(String time) {
        return time == null ? null : LocalDateTime.parse(time, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    public static String timeToString(LocalDateTime time) {
        return time == null ? null : time.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    public static Set<String> normalized(List<String> values) {
        Set<String> result = new HashSet<>();
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                result.add(value.trim().toUpperCase(Locale.ROOT));
            }
        }
        return result;
    }

    public static Set<String> values(ConfigurationSection section, String pluralPath, String singularPath) {
        Set<String> result = normalized(section.getStringList(pluralPath));
        String value = section.getString(singularPath, "");
        if (!value.isBlank()) {
            result.add(value.trim().toUpperCase(Locale.ROOT));
        }
        return result;
    }

    public static double getMaxHealth(Entity entity) {
        if (!(entity instanceof LivingEntity living)) {
            return 0.0D;
        }
        AttributeInstance attribute = living.getAttribute(Attribute.MAX_HEALTH);
        return attribute == null ? living.getHealth() : attribute.getValue();
    }

    public static double getMaxAbsorption(Entity entity) {
        if (!(entity instanceof LivingEntity living)) {
            return 0.0D;
        }
        AttributeInstance attribute = living.getAttribute(Attribute.MAX_ABSORPTION);
        return attribute == null ? living.getHealth() : attribute.getValue();
    }

    public static ItemStack getSlotItem(EntityEquipment equipment, String slot) {
        return switch(slot.toUpperCase(Locale.ROOT).replace('-', '_')) {
            case "MAIN_HAND" -> {
                    yield equipment.getItemInMainHand();
                }
            case "OFF_HAND" -> {
                    yield equipment.getItemInOffHand();
                }
            case "HELMET" -> {
                    yield equipment.getHelmet();
                }
            case "CHESTPLATE" -> {
                    yield equipment.getChestplate();
                }
            case "LEGGINGS" -> {
                    yield equipment.getLeggings();
                }
            case "BOOTS" -> {
                    yield equipment.getBoots();
                }
            default -> {
                    yield null;
                }
        };
    }

    public static Map<String, Boolean> loadedPlugins = new HashMap<>();

    public static boolean checkPluginLoad(String pluginName) {
        if (loadedPlugins.containsKey(pluginName)) {
            return loadedPlugins.get(pluginName);
        }
        loadedPlugins.put(pluginName, UltimateQuest.instance.getServer().getPluginManager().isPluginEnabled(pluginName));
        return UltimateQuest.instance.getServer().getPluginManager().isPluginEnabled(pluginName);
    }

    public static boolean getClass(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public static boolean checkClass(String className, String methodName) {
        try {
            Class<?> targetClass = Class.forName(className);
            Method[] methods = targetClass.getDeclaredMethods();
            for (Method method : methods) {
                if (method.getName().equals(methodName)) {
                    return true;
                }
            }
            return false;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public static boolean getYearVersion(int year, int majorVersion, int minorVersion) {
        return UltimateQuest.yearVersion > year || (UltimateQuest.yearVersion == year && UltimateQuest.majorVersion >= majorVersion && UltimateQuest.minorVersion >= minorVersion);
    }

    public static boolean getMajorVersion(int version) {
        return UltimateQuest.yearVersion > 1 || UltimateQuest.majorVersion >= version;
    }

    public static boolean getMinorVersion(int majorVersion, int minorVersion) {
        return UltimateQuest.yearVersion > 1 || UltimateQuest.majorVersion > majorVersion || (UltimateQuest.majorVersion == majorVersion && UltimateQuest.minorVersion >= minorVersion);
    }

    public static String modifyString(Player player, String text, String... args) {
        if (text == null) {
            return "";
        }
        text = CommonUtil.parseLang(player, text);
        for (int i = 0; i < args.length; i += 2) {
            if (i + 1 >= args.length) {
                break;
            }
            String var = "{" + args[i] + "}";
            if (args[i + 1] == null) {
                text = text.replace(var, "");
            } else {
                text = text.replace(var, args[i + 1]);
            }
        }
        return text;
    }

    public static List<String> modifyList(Player player, List<String> config, String... args) {
        List<String> resultList = new ArrayList<>();
        for (String s : config) {
            s = CommonUtil.parseLang(player, s);
            for (int i = 0; i < args.length; i += 2) {
                String var = "{" + args[i] + "}";
                if (args[i + 1] == null) {
                    s = s.replace(var, "");
                } else {
                    s = s.replace(var, args[i + 1]);
                }
            }
            resultList.add(TextUtil.withPAPI(s, player));
        }
        return resultList;
    }

    public static Pattern pattern8 = Pattern.compile("\\{lang:(.*?)}");

    public static String parseLang(Player player, String text) {
        Matcher matcher8 = pattern8.matcher(text);
        while (matcher8.find()) {
            String placeholder = matcher8.group(1);
            text = text.replace("{lang:" + placeholder + "}", LanguageManager.languageManager.getStringText(player, "override-lang." + placeholder));
        }
        return text;
    }

    public static void summonMythicMobs(Location location, String mobID, int level) {
        MythicBukkit.inst().getMobManager().getMythicMob(mobID).ifPresent(mob -> mob.spawn(BukkitAdapter.adapt(location), level));
    }

    public static void mkDir(File dir) {
        if (!dir.exists()) {
            File parentFile = dir.getParentFile();
            if (parentFile == null) {
                return;
            }
            String parentPath = parentFile.getPath();
            mkDir(new File(parentPath));
            dir.mkdir();
        }
    }

    public static NamespacedKey parseNamespacedKey(String key) {
        String[] keySplit = key.split(":");
        if (keySplit.length == 1) {
            return NamespacedKey.minecraft(key.toLowerCase());
        }
        return NamespacedKey.fromString(key);
    }

    public static Color parseColor(String color) {
        if (color == null || color.isEmpty()) {
            return Color.fromRGB(0, 0, 0);
        }
        color = color.trim();
        //  #RRGGBB
        if (color.startsWith("#")) {
            return Color.fromRGB(Integer.parseInt(color.substring(1), 16));
        }
        //  R,G,B
        String[] keySplit = color.replace(" ", "").split(",");
        if (keySplit.length == 3) {
            return Color.fromRGB(Integer.parseInt(keySplit[0]), Integer.parseInt(keySplit[1]), Integer.parseInt(keySplit[2]));
        }
        // ?RGB int
        return Color.fromRGB(Integer.parseInt(color));
    }

    public static String colorToString(Color color) {
        if (color == null) {
            return "0,0,0";
        }
        return color.getRed() + "," + color.getGreen() + "," + color.getBlue();
    }

    public static List<Color> parseColorList(List<String> rawList) {
        List<Color> colors = new ArrayList<>();
        for (String value : rawList) {
            try {
                colors.add(parseColor(value));
            } catch (Exception e) {
                return colors;
            }
        }
        return colors;
    }

    public static void giveOrDrop(Player player, ItemStack... item) {
        if (item == null) {
            return;
        }
        for (ItemStack tempVal1 : item) {
            if (tempVal1 == null) {
                return;
            }
        }
        HashMap<Integer, ItemStack> result = player.getInventory().addItem(item);
        if (!result.isEmpty()) {
            for (int id : result.keySet()) {
                player.getWorld().dropItem(player.getLocation(), result.get(id));
            }
        }
    }

    public static boolean hasEnoughDurability(ItemStack itemStack, int requiredDurability) {
        if (requiredDurability <= 0 || itemStack == null || itemStack.getType().isAir()) {
            return true;
        }
        ItemMeta itemMeta = itemStack.getItemMeta();
        if (itemMeta == null || itemMeta.isUnbreakable() || !(itemMeta instanceof Damageable damageable)) {
            return true;
        }
        int maxDurability = damageable.hasMaxDamage() ? damageable.getMaxDamage() : itemStack.getType().getMaxDurability();
        return maxDurability <= 0 || maxDurability - damageable.getDamage() >= requiredDurability;
    }
}
