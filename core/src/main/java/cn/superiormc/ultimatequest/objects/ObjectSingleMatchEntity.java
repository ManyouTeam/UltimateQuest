package cn.superiormc.ultimatequest.objects;

import cn.superiormc.ultimatequest.utils.CommonUtil;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public final class ObjectSingleMatchEntity {

    public final ConfigurationSection section;

    private final LivingEntity entity;

    private final Player player;

    private final QuestContext context;

    public ObjectSingleMatchEntity(ConfigurationSection section, LivingEntity entity, Player player, QuestContext context) {
        this.section = section;
        this.entity = entity;
        this.player = player;
        this.context = context;
    }

    public ObjectSingleMatchEntity withSection(ConfigurationSection section) {
        return new ObjectSingleMatchEntity(section, entity, player, context);
    }

    public String parsePlaceholder(String value) {
        if (value == null) {
            return null;
        }
        int level = context == null ? 1 : context.level();
        return CommonUtil.modifyString(player, value, "level", String.valueOf(level));
    }

    public List<String> parsePlaceholder(List<String> values) {
        List<String> result = new ArrayList<>();
        for (String value : values) {
            result.add(parsePlaceholder(value));
        }
        return result;
    }

    public ConfigurationSection getSection() {
        return section;
    }

    public LivingEntity getEntity() {
        return entity;
    }

    public Player getPlayer() {
        return player;
    }

    public QuestContext getContext() {
        return context;
    }
}
