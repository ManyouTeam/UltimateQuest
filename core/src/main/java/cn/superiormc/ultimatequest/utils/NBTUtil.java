package cn.superiormc.ultimatequest.utils;

import de.tr7zw.nbtapi.NBTItem;
import de.tr7zw.nbtapi.NBTType;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.inventory.ItemStack;

public class NBTUtil {

    public static String getObjectType(Object object) {
        if (object instanceof Byte) {
            return "byte";
        } else if (object instanceof Short) {
            return "short";
        } else if (object instanceof Integer) {
            return "int";
        } else if (object instanceof Long) {
            return "long";
        } else if (object instanceof Float) {
            return "float";
        } else if (object instanceof Double) {
            return "double";
        } else if (object instanceof String) {
            return "string";
        }
        return null;
    }

    public static ItemStack addNBT(ItemStack item, String type, String key, Object object) {
        if (!CommonUtil.checkPluginLoad("NBTAPI")) {
            //  NBTAPI ?
            return item;
        }
        NBTItem nbtItem = new NBTItem(item);
        switch(//  type ?NBT ?
        type.toLowerCase()) {
            case "byte":
                nbtItem.setByte(key, ((Number) object).byteValue());
                break;
            case "short":
                nbtItem.setShort(key, ((Number) object).shortValue());
                break;
            case "int":
                nbtItem.setInteger(key, ((Number) object).intValue());
                break;
            case "long":
                nbtItem.setLong(key, ((Number) object).longValue());
                break;
            case "float":
                nbtItem.setFloat(key, ((Number) object).floatValue());
                break;
            case "double":
                nbtItem.setDouble(key, ((Number) object).doubleValue());
                break;
            case "string":
                nbtItem.setString(key, (String) object);
                break;
        }
        //  ItemStack
        return nbtItem.getItem();
    }

    public static Map<String, Object> getAllNBT(ItemStack item) {
        Map<String, Object> nbtData = new HashMap<>();
        if (!CommonUtil.checkPluginLoad("NBTAPI")) {
            //  NBTAPI ?Map
            return nbtData;
        }
        NBTItem nbtItem = new NBTItem(item);
        for (String key : nbtItem.getKeys()) {
            //  NBT
            Object value = null;
            // ?
            if (nbtItem.hasTag(key)) {
                if (nbtItem.getType(key) == NBTType.NBTTagString) {
                    value = nbtItem.getString(key);
                } else if (nbtItem.getType(key) == NBTType.NBTTagInt) {
                    value = nbtItem.getInteger(key);
                } else if (nbtItem.getType(key) == NBTType.NBTTagByte) {
                    value = nbtItem.getByte(key);
                } else if (nbtItem.getType(key) == NBTType.NBTTagDouble) {
                    value = nbtItem.getDouble(key);
                } else if (nbtItem.getType(key) == NBTType.NBTTagFloat) {
                    value = nbtItem.getFloat(key);
                } else if (nbtItem.getType(key) == NBTType.NBTTagLong) {
                    value = nbtItem.getLong(key);
                } else if (nbtItem.getType(key) == NBTType.NBTTagShort) {
                    value = nbtItem.getShort(key);
                }
                //  Map
                nbtData.put(key, value);
            }
        }
        return nbtData;
    }
}
