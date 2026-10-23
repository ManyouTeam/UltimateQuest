package cn.superiormc.ultimatequest.api.trigger;

import java.util.Locale;

public enum ItemSelector {

    CONTEXT,
    TRIGGER_ITEM,
    COUNTED_ITEM,
    MAIN_HAND,
    OFF_HAND,
    HELMET,
    CHESTPLATE,
    LEGGINGS,
    BOOTS,
    ARMOR,
    ALL_EQUIPMENT;

    public static ItemSelector parse(String value, ItemSelector defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT).replace('-', '_');
        normalized = switch(normalized) {
            case "HAND" -> {
                    yield "MAIN_HAND";
                }
            case "OFFHAND" -> {
                    yield "OFF_HAND";
                }
            case "HEAD" -> {
                    yield "HELMET";
                }
            case "CHEST" -> {
                    yield "CHESTPLATE";
                }
            case "LEGS" -> {
                    yield "LEGGINGS";
                }
            case "FEET" -> {
                    yield "BOOTS";
                }
            default -> {
                    yield normalized;
                }
        };
        try {
            return valueOf(normalized);
        } catch (IllegalArgumentException ignored) {
            return defaultValue;
        }
    }
}
