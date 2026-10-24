package cn.superiormc.ultimatequest.objects.menus;

import cn.superiormc.ultimatequest.objects.buttons.ObjectButton;

import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ObjectMenu {

    private final String id;

    private final ConfigurationSection config;

    private final Map<String, ObjectButton> buttonItems = new LinkedHashMap<>();

    private final Map<String, ObjectMenuContent> contents = new LinkedHashMap<>();

    private final Map<String, Map<String, List<Integer>>> layouts = new LinkedHashMap<>();

    public ObjectMenu(String id, ConfigurationSection config) {
        this.id = id;
        this.config = config;
        initMenu();
    }

    public void initMenu() {
        layouts.clear();
        initLayouts();
        initButtons();
        initContents();
    }

    public void initButtons() {
        buttonItems.clear();
        ConfigurationSection section = config.getConfigurationSection("items");
        if (section == null) {
            return;
        }
        for (String key : section.getKeys(false)) {
            ConfigurationSection button = section.getConfigurationSection(key);
            if (button != null) {
                buttonItems.put(key, new ObjectButton(key, button));
            }
        }
    }

    public void initContents() {
        contents.clear();
        ConfigurationSection section = config.getConfigurationSection("contents");
        if (section == null) {
            return;
        }
        for (String key : section.getKeys(false)) {
            ConfigurationSection content = section.getConfigurationSection(key);
            if (content != null) {
                contents.put(key, new ObjectMenuContent(key, content));
            }
        }
    }

    public Map<String, List<Integer>> layoutSlots(List<String> layout) {
        Map<String, List<Integer>> result = new LinkedHashMap<>();
        for (int row = 0; row < layout.size(); row++) {
            for (int column = 0; column < 9; column++) {
                String symbol = String.valueOf(layout.get(row).charAt(column));
                result.computeIfAbsent(symbol, ignored -> new ArrayList<>()).add(row * 9 + column);
            }
        }
        return result;
    }

    public List<Integer> symbolSlots(String id, Map<String, List<Integer>> layoutSlots) {
        if (id.length() != 1) {
            throw new IllegalArgumentException("Menu '" + this.id + "' layout entry '" + id + "' must be one character");
        }
        return layoutSlots.getOrDefault(id, List.of());
    }

    public String getId() {
        return id;
    }

    public ConfigurationSection getConfig() {
        return config;
    }

    public Map<String, ObjectButton> getButtons() {
        return Collections.unmodifiableMap(buttonItems);
    }

    public Map<String, ObjectMenuContent> getContents() {
        return Collections.unmodifiableMap(contents);
    }

    public List<String> getLayout(boolean selected, boolean challenges) {
        List<String> rows = config.getStringList(challenges ? "challenge-layout" : selected ? "detail-layout"
                                                                                            : "catalog-layout");
        return rows.isEmpty() ? config.getStringList("layout") : rows;
    }

    public ObjectButton getContentButton(ConfigurationSection section) {
        for (ObjectMenuContent content : contents.values()) {
            if (content.getConfig() == section) {
                return content.getButton();
            }
        }
        return null;
    }

    private void initLayouts() {
        for (String path : List.of("layout", "catalog-layout", "detail-layout", "challenge-layout", "category-filter.layout")) {
            if (!config.contains(path)) {
                continue;
            }
            List<String> rows = config.getStringList(path);
            if (rows.isEmpty() || rows.size() > 6 || rows.stream().anyMatch(row -> row.length() != 9)) {
                throw new IllegalArgumentException("Invalid menu layout: " + id + "." + path);
            }
            if (path.equals("category-filter.layout") && rows.stream().noneMatch(row -> row.contains("Q"))) {
                throw new IllegalArgumentException("Category layout requires Q slots: " + id);
            }
            layouts.put(path, layoutSlots(rows));
        }
    }

}
