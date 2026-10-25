package cn.superiormc.ultimatequest.managers;

import cn.superiormc.ultimatequest.gui.action.ActionGUIPage;
import cn.superiormc.ultimatequest.gui.action.GUIActionContext;
import cn.superiormc.ultimatequest.gui.action.ObjectSingleGUIAction;
import cn.superiormc.ultimatequest.objects.ObjectAction;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

import java.util.List;

public final class GUIActionManager {

    public static final GUIActionManager INSTANCE = new GUIActionManager();

    private final ActionManager<ObjectSingleGUIAction, GUIActionContext> actionManager;

    private GUIActionManager() {
        actionManager = new ActionManager<>(
                (section, parent, player, context) -> createActions(section).runAllActions(player, context),
                (section, parent, player, context, amount) ->
                        createActions(section).runRandomEveryActions(player, context, amount),
                (section, parent, context) ->
                        player -> createActions(section).runAllActions(player, context),
                true);
        actionManager.registerNewAction("previous_page", new ActionGUIPage("previous_page", -1));
        actionManager.registerNewAction("next_page", new ActionGUIPage("next_page", 1));
        actionManager.registerNewAction("refresh", new ActionGUIPage("refresh", 0));
        for (String type : List.of("open_menu", "command", "open_categories", "select_category", "back", "cycle_view",
                "cycle_challenge_group", "cycle_period_group", "set_period_group", "cycle_scroll_group", "set_scroll_group",
                "quest", "claim_all", "open_challenges", "quest_info", "abandon_quest")) {
            actionManager.registerNewAction(type, new ActionGUIPage(type, 0));
        }
    }

    public ObjectAction<ObjectSingleGUIAction, GUIActionContext> createButtonActions(ConfigurationSection button) {
        return createButtonActions(button, null);
    }

    public ObjectAction<ObjectSingleGUIAction, GUIActionContext> createButtonActions(
            ConfigurationSection button, ClickType clickType) {
        if (button == null) {
            return createActions(null);
        }
        ConfigurationSection specific = clickType == null ? null
                : button.getConfigurationSection("click-actions." + clickType.name());
        return createActions(specific == null ? button.getConfigurationSection("actions") : specific);
    }

    public void executeButtonActions(ConfigurationSection button, Player player, GUIActionContext context) {
        execute(createButtonActions(button, context == null ? null : context.clickType()), player, context);
    }

    public void execute(ObjectAction<ObjectSingleGUIAction, GUIActionContext> actions,
                        Player player,
                        GUIActionContext context) {
        if (actions != null) {
            actions.runAllActions(player, context);
        }
    }

    public ActionManager<ObjectSingleGUIAction, GUIActionContext> getActionManager() {
        return actionManager;
    }

    private ObjectAction<ObjectSingleGUIAction, GUIActionContext> createActions(ConfigurationSection section) {
        return new ObjectAction<>(section, ObjectSingleGUIAction::new, actionManager::doAction);
    }
}
