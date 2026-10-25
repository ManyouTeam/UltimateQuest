package cn.superiormc.ultimatequest.gui.action;

import cn.superiormc.ultimatequest.UltimateQuest;
import cn.superiormc.ultimatequest.commands.SubMenu;
import cn.superiormc.ultimatequest.gui.inv.CategoryMenuGUI;
import cn.superiormc.ultimatequest.gui.inv.ConfiguredMenuGUI;
import cn.superiormc.ultimatequest.objects.actions.AbstractRunAction;
import org.bukkit.entity.Player;

public final class ActionGUIPage extends AbstractRunAction<ObjectSingleGUIAction, GUIActionContext> {

    private final int pageOffset;

    public ActionGUIPage(String type, int pageOffset) {
        super(type);
        this.pageOffset = pageOffset;
        if ("command".equals(type)) {
            setRequiredArgs("command");
        }
    }

    @Override
    protected void onDoAction(ObjectSingleGUIAction singleAction,
                              Player player,
                              GUIActionContext context) {
        if (context == null) {
            return;
        }
        switch (getType()) {
            case "previous_page", "next_page", "refresh" -> {
                context.gui().openPage(context.gui().getCurrentPage() + pageOffset);
            }
            case "open_menu" -> {
                String targetId = singleAction.contains("menu")
                        ? singleAction.getString("menu", player, context) : "main";
                SubMenu.open(player, targetId, context.bypassPermission());
            }
            case "command" -> {
                String command = singleAction.getString("command", player, context);
                if (!command.isBlank()) {
                    if (singleAction.getBoolean("as-console", true)) {
                        UltimateQuest.methodUtil.dispatchCommand(command);
                    } else {
                        UltimateQuest.methodUtil.dispatchCommand(player, command);
                    }
                }
            }
            default -> {
                if (context.gui() instanceof ConfiguredMenuGUI menu) {
                    menu.executeMenuAction(singleAction, context);
                } else if (context.gui() instanceof CategoryMenuGUI categories) {
                    categories.executeMenuAction(singleAction, context);
                }
            }
        }
    }
}
