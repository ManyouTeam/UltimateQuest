package cn.superiormc.ultimatequest.gui.action;

import cn.superiormc.ultimatequest.objects.quests.QuestDefinition;
import cn.superiormc.ultimatequest.objects.quests.QuestInstance;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;

public record GUIActionContext(GUIPageController gui,
                               Inventory inventory,
                               ClickType clickType,
                               int slot,
                               String menuId,
                               QuestDefinition definition,
                               QuestInstance instance,
                               String kind,
                               String stage,
                               boolean bypassPermission) {
}
