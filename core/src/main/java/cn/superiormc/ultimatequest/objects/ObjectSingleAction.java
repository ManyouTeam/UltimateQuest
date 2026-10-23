package cn.superiormc.ultimatequest.objects;

import cn.superiormc.ultimatequest.utils.CommonUtil;
import cn.superiormc.ultimatequest.utils.TextUtil;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public final class ObjectSingleAction extends AbstractConfiguredSection<QuestContext> {

    public ObjectSingleAction(ConfigurationSection section) {
        super(section);
    }

    @Override
    protected String replacePlaceholder(String content, Player player, QuestContext context) {
        if (content == null) {
            return "";
        }
        int level = context == null ? 1 : context.level();
        return TextUtil.withPAPI(CommonUtil.modifyString(player, content, "level", String.valueOf(level)), player);
    }
}
