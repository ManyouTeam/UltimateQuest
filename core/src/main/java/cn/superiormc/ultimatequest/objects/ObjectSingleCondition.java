package cn.superiormc.ultimatequest.objects;

import cn.superiormc.ultimatequest.utils.CommonUtil;
import cn.superiormc.ultimatequest.utils.TextUtil;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public final class ObjectSingleCondition extends AbstractConfiguredSection<QuestContext> {

    private final QuestContext context;

    private final Player player;

    public ObjectSingleCondition(ConfigurationSection section, QuestContext context) {
        this(section, context, context == null ? null : context.player());
    }

    public ObjectSingleCondition(ConfigurationSection section, QuestContext context, Player player) {
        super(section);
        this.context = context;
        this.player = player;
    }

    public ObjectSingleCondition withSection(ConfigurationSection section) {
        return new ObjectSingleCondition(section, context, player);
    }

    public String parsePlaceholder(String value) {
        return replacePlaceholder(value, player(), context);
    }

    public List<String> parsePlaceholder(List<String> values) {
        List<String> result = new ArrayList<>();
        for (String value : values) {
            result.add(parsePlaceholder(value));
        }
        return result;
    }

    public Player sourcePlayer() {
        return context == null ? null : context.player();
    }

    public Player player() {
        return context == null ? player : context.player();
    }

    public QuestContext getContext() {
        return context;
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
