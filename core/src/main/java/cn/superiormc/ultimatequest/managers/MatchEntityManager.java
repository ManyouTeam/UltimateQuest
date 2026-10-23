package cn.superiormc.ultimatequest.managers;

import cn.superiormc.ultimatequest.objects.ObjectSingleMatchEntity;
import cn.superiormc.ultimatequest.objects.QuestContext;
import cn.superiormc.ultimatequest.objects.matchentity.AbstractMatchEntityRule;
import cn.superiormc.ultimatequest.objects.matchentity.Animal;
import cn.superiormc.ultimatequest.objects.matchentity.Any;
import cn.superiormc.ultimatequest.objects.matchentity.EntityContainsName;
import cn.superiormc.ultimatequest.objects.matchentity.EntityHealth;
import cn.superiormc.ultimatequest.objects.matchentity.EntityNone;
import cn.superiormc.ultimatequest.objects.matchentity.EntityPDC;
import cn.superiormc.ultimatequest.objects.matchentity.EntityTag;
import cn.superiormc.ultimatequest.objects.matchentity.EntityType;
import cn.superiormc.ultimatequest.objects.matchentity.Equip;
import cn.superiormc.ultimatequest.objects.matchentity.LevelledMobs;
import cn.superiormc.ultimatequest.objects.matchentity.Monster;
import cn.superiormc.ultimatequest.objects.matchentity.MythicMobs;
import cn.superiormc.ultimatequest.objects.matchentity.None;
import cn.superiormc.ultimatequest.objects.matchentity.Not;
import cn.superiormc.ultimatequest.objects.matchentity.Ranged;
import cn.superiormc.ultimatequest.utils.CommonUtil;
import cn.superiormc.ultimatequest.utils.TextUtil;
import java.util.Collection;
import java.util.HashSet;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class MatchEntityManager extends AbstractManager {

    public static MatchEntityManager matchEntityManager;

    private final Collection<AbstractMatchEntityRule> rules = new HashSet<>();

    public MatchEntityManager() {
        matchEntityManager = this;
        registerBuiltIns();
    }

    private void registerBuiltIns() {
        registerNewRule(new EntityType());
        registerNewRule(new None());
        registerNewRule(new EntityContainsName());
        registerNewRule(new EntityNone());
        registerNewRule(new EntityHealth());
        registerNewRule(new EntityTag());
        registerNewRule(new EntityPDC());
        registerNewRule(new Any());
        registerNewRule(new Not());
        registerNewRule(new Equip());
        registerNewRule(new Ranged());
        registerNewRule(new Monster());
        registerNewRule(new Animal());
        if (CommonUtil.checkPluginLoad("MythicMobs")) {
            registerNewRule(new MythicMobs());
        }
        if (CommonUtil.checkPluginLoad("LevelledMobs")) {
            registerNewRule(new LevelledMobs());
        }
    }

    public void registerNewRule(AbstractMatchEntityRule rule) {
        rules.add(rule);
    }

    public boolean getMatch(ConfigurationSection section, LivingEntity entity) {
        return getMatch(section, entity, null, null);
    }

    public boolean getMatch(ConfigurationSection section, LivingEntity entity, Player player, QuestContext context) {
        if (section == null) {
            return true;
        }
        if (entity == null) {
            return false;
        }
        return getMatch(new ObjectSingleMatchEntity(section, entity, player, context));
    }

    public boolean getMatch(ObjectSingleMatchEntity match) {
        for (AbstractMatchEntityRule rule : rules) {
            if (ConfigManager.configManager.getBoolean("debug")) {
                TextUtil.sendMessage(null, TextUtil.pluginPrefix() + " §fChecking rule: " + rule.getClass().getSimpleName() + "!");
            }
            if (rule.configNotContains(match.section)) {
                continue;
            }
            if (!rule.getMatch(match)) {
                return false;
            }
        }
        return true;
    }

    public Collection<AbstractMatchEntityRule> getRules() {
        return rules;
    }
}
