package cn.superiormc.ultimatequest.objects.quests;

import cn.superiormc.ultimatequest.managers.AntiAbuseManager;
import cn.superiormc.ultimatequest.managers.ConfigManager;
import cn.superiormc.ultimatequest.managers.EventConditionsManager;
import cn.superiormc.ultimatequest.managers.LanguageManager;
import cn.superiormc.ultimatequest.managers.TriggerManager;
import cn.superiormc.ultimatequest.objects.AbstractConfiguredSection;
import cn.superiormc.ultimatequest.objects.QuestContext;
import cn.superiormc.ultimatequest.objects.counting.CountMode;
import cn.superiormc.ultimatequest.objects.counting.TriggerCounter;
import cn.superiormc.ultimatequest.objects.triggers.PlaceholderTrigger;
import cn.superiormc.ultimatequest.utils.CommonUtil;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;

public final class QuestObjective extends AbstractConfiguredSection<QuestContext> {

    private final String trigger;

    private final long required;

    private final long value;

    private final TriggerCounter counter;

    public QuestObjective(String id, ConfigurationSection section) {
        this(id, section, false);
    }

    public QuestObjective(String id, ConfigurationSection section, boolean source) {
        super(id, section);
        String value = section.getString("trigger", "").toLowerCase(Locale.ROOT).replace('-', '_');
        trigger = value.contains(":") ? value : "ultimatequest:" + value;
        required = source ? 1L : QuestDefinition.positiveLong(section, "amount", trigger.equals("ultimatequest:placeholder") ? 1L : 0L);
        this.value = source ? QuestDefinition.positiveLong(section, "value", 1L) : 1L;
        if (required <= 0L || TriggerManager.triggerManager.get(trigger).isEmpty()) {
            throw new IllegalArgumentException("Invalid amount or trigger for objective " + id);
        }
        if (trigger.equals("ultimatequest:placeholder")) {
            PlaceholderTrigger.validate(section);
            if (source) {
                throw new IllegalArgumentException("Placeholder checks are task objectives, not progression XP sources");
            }
        }
        counter = new TriggerCounter(section, trigger);
        EventConditionsManager.eventConditions.validate(section.getConfigurationSection("conditions"));
    }

    public static YamlConfiguration resolve(String id, ConfigurationSection configured,
            Map<String, YamlConfiguration> library) {
        if (configured == null) {
            throw new IllegalArgumentException("Missing source for objective " + id);
        }
        ConfigurationSection source = configured.getConfigurationSection("source");
        if (source == null) {
            source = configured;
        }
        YamlConfiguration result = new YamlConfiguration();
        String reference = source.getString("quest", "");
        if (!reference.isEmpty()) {
            YamlConfiguration template = library.get(reference);
            if (template == null) {
                throw new IllegalArgumentException("Unknown quest objective: " + reference);
            }
            copy(template, result, "");
        }
        copy(source, result, "quest");
        if (configured != source) {
            copy(configured, result, "source");
        }
        if (!result.contains("trigger")) {
            throw new IllegalArgumentException("Objective " + id + " requires source.quest or source.trigger");
        }
        return result;
    }

    private static void copy(ConfigurationSection source, YamlConfiguration target, String excluded) {
        for (Map.Entry<String, Object> entry : source.getValues(true).entrySet()) {
            if (entry.getKey().equals(excluded) || entry.getKey().startsWith(excluded + ".")
                    || entry.getValue() instanceof ConfigurationSection) {
                continue;
            }
            target.set(entry.getKey(), entry.getValue());
        }
    }

    public String trigger() {
        return trigger;
    }

    public long required() {
        return required;
    }

    public String name() {
        return name(null);
    }

    public String name(Player player) {
        String configured = section.getString("name", "");
        if (!configured.isBlank()) {
            return CommonUtil.parseLang(player, configured);
        }
        String key = trigger.substring(trigger.indexOf(':') + 1);
        String action = trigger.startsWith("ultimatequest:")
                ? LanguageManager.languageManager.getStringText(player, "override-lang.objective-action-" + key.replace('_', '-'), "")
                : "";
        String defaults = action.isBlank() ? trigger : action + "{target}";
        String format = section.getString("display", ConfigManager.configManager.getString(
                "objective-display.formats." + trigger, ConfigManager.configManager.getString(
                        "objective-display.formats." + key, defaults)));
        String target = targetName(player);
        format = CommonUtil.parseLang(player, format);
        if (target.isBlank()) {
            format = format.replaceAll("[\\h:：]*\\{target\\}", "");
        }
        return CommonUtil.modifyString(player, format, "target", target, "trigger", trigger).strip();
    }

    public long value() {
        return value;
    }

    public boolean countsItems() {
        return counter.mode() == CountMode.ITEMS;
    }

    public CountMode countMode() {
        return counter.mode();
    }

    private String targetName(Player player) {
        ConfigurationSection conditions = section.getConfigurationSection("conditions");
        String key = trigger.substring(trigger.indexOf(':') + 1);
        if (conditions != null) {
            for (String id : conditions.getKeys(false)) {
                ConfigurationSection condition = conditions.getConfigurationSection(id);
                if (condition == null) {
                    continue;
                }
                String type = condition.getString("type", "");
                ConfigurationSection rules = condition.getConfigurationSection("match");
                rules = rules == null ? condition : rules;
                List<String> targets = new ArrayList<>();
                if (type.equals("match_entity")
                        && condition.getString("target", "TARGET").equalsIgnoreCase("TARGET")) {
                    targets = translatedTargets(rules.getStringList("entity-types"), "entity");
                } else if (type.equals("block_type")) {
                    targets = translatedTargets(new ArrayList<>(CommonUtil.values(condition, "types", "type-name")), "block");
                } else if (type.equals("match_item") && List.of("COUNTED_ITEM", "CONTEXT").contains(
                        condition.getString("item.selector", condition.getString("item", "CONTEXT")).toUpperCase(Locale.ROOT))
                        || key.equals("fish") && type.equals("catch_match_item")) {
                    targets = translatedTargets(rules.getStringList("material"), "item");
                }
                if (!targets.isEmpty()) {
                    return String.join(LanguageManager.languageManager.getStringText(player,
                            "objective-display.target-separator", " / "), targets);
                }
            }
        }
        return LanguageManager.languageManager.getStringText(player, "objective-display.targets." + key, "");
    }

    private List<String> translatedTargets(List<String> configured, String translationType) {
        List<String> targets = new ArrayList<>();
        for (String value : configured) {
            String translation;
            try {
                if (translationType.equals("entity")) {
                    EntityType entityType = EntityType.valueOf(value.toUpperCase(Locale.ROOT));
                    if (entityType == EntityType.UNKNOWN) {
                        return List.of();
                    }
                    translation = "entity." + entityType.getKey().getNamespace() + "." + entityType.getKey().getKey();
                } else {
                    Material material = Material.matchMaterial(value);
                    if (material == null || material.isLegacy()) {
                        return List.of();
                    }
                    if (Bukkit.getServer() == null) {
                        translation = translationType + "." + material.getKey().getNamespace() + "." + material.getKey().getKey();
                    } else {
                        translation = material.getTranslationKey();
                    }
                }
            } catch (IllegalArgumentException exception) {
                return List.of();
            }
            if (translation == null || !translation.matches("[a-z0-9_.-]+")) {
                return List.of();
            }
            String component = "<lang:" + translation + ">";
            if (!targets.contains(component)) {
                targets.add(component);
            }
        }
        return targets;
    }

    /** Integer compatibility API. Internal progress uses contribution to preserve fractions. */
    public long increment(QuestContext context, String scope) {
        return contribution(context, scope).longValue();
    }

    public BigDecimal contribution(QuestContext context, String scope) {
        if (context == null || context.trigger() == null || !trigger.equals(context.trigger().key().toString())
                || context.event() instanceof Cancellable cancelled && cancelled.isCancelled()) {
            return BigDecimal.ZERO;
        }
        BigDecimal amount;
        if (trigger.equals("ultimatequest:placeholder")) {
            if (!PlaceholderTrigger.matches(section, context.player())
                    || !EventConditionsManager.eventConditions.matches(section.getConfigurationSection("conditions"), context)) {
                return BigDecimal.ZERO;
            }
            amount = BigDecimal.valueOf(required);
        } else {
            amount = counter.measure(context, counted -> EventConditionsManager.eventConditions.matches(
                    section.getConfigurationSection("conditions"), counted));
        }
        amount = amount.multiply(BigDecimal.valueOf(value)).min(CountMode.MAXIMUM);
        if (amount.signum() <= 0 || !AntiAbuseManager.antiAbuseManager.allows(
                section.getConfigurationSection("anti-abuse"), context, scope, true)) {
            return BigDecimal.ZERO;
        }
        return amount;
    }



}
