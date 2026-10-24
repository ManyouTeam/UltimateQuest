package cn.superiormc.ultimatequest.objects.configs;

import cn.superiormc.ultimatequest.gui.action.GUIActionContext;
import cn.superiormc.ultimatequest.gui.action.ObjectSingleGUIAction;
import cn.superiormc.ultimatequest.managers.GUIActionManager;
import cn.superiormc.ultimatequest.objects.ObjectAction;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ObjectButton extends AbstractButton {

    private final Map<ClickType, ObjectAction<ObjectSingleGUIAction, GUIActionContext>> actions = new EnumMap<>(ClickType.class);

    private final String id;

    private final Map<String, ObjectDisplayItem> displayItems = new LinkedHashMap<>();

    public ObjectButton(String id, ConfigurationSection config) {
        super(config);
        this.id = id;
        initDisplayItems();
        for (ClickType type : ClickType.values()) {
            actions.put(type, GUIActionManager.INSTANCE.createButtonActions(config, type));
        }
    }

    private void initDisplayItems() {
        for (String path : config.getKeys(true)) {
            ConfigurationSection section = config.getConfigurationSection(path);
            if (section != null) {
                displayItems.put(path, new ObjectDisplayItem(section));
            }
        }
    }

    public String getId() {
        return id;
    }

    public ConfigurationSection getConfig() {
        return config;
    }

    public ObjectDisplayItem getDisplayItem(String path) {
        return displayItems.get(path);
    }

    public void clickEvent(Player player, GUIActionContext context) {
        GUIActionManager.INSTANCE.execute(actions.get(context.clickType()), player, context);
    }

    public boolean isHidden(int page, int maximumPage) {
        return config.getBoolean("hide-on-first-page", false) && page == 0 || config.getBoolean("hide-on-last-page", false) && page == maximumPage;
    }

    public ConfigurationSection getDisplayConfig(boolean hidden, String periodGroup, String scrollGroup, String view) {
        if (hidden) {
            return config.getConfigurationSection("hidden-item");
        }
        for (String path : List.of("period-items." + periodGroup, "scroll-items." + scrollGroup, "view-items." + view, "item")) {
            ObjectDisplayItem item = displayItems.get(path);
            if (item != null) {
                return item.getConfig();
            }
        }
        return config;
    }
}
