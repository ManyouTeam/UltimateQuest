package cn.superiormc.ultimatequest.objects.quests;

import cn.superiormc.ultimatequest.managers.AntiAbuseManager;
import cn.superiormc.ultimatequest.objects.counting.CountMode;
import cn.superiormc.ultimatequest.objects.counting.TriggerCounter;
import cn.superiormc.ultimatequest.managers.EventConditionsManager;
import cn.superiormc.ultimatequest.managers.ConfigManager;
import cn.superiormc.ultimatequest.managers.LanguageManager;
import cn.superiormc.ultimatequest.managers.TriggerManager;
import cn.superiormc.ultimatequest.objects.AbstractConfiguredSection;
import cn.superiormc.ultimatequest.objects.QuestContext;
import cn.superiormc.ultimatequest.utils.CommonUtil;
import java.util.ArrayList;
import java.util.Map;
import java.util.List;
import java.util.Locale;
import java.math.BigDecimal;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

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
        required = source ? 1L : QuestDefinition.positiveLong(section, "amount", 0L);
        this.value = source ? QuestDefinition.positiveLong(section, "value", 1L) : 1L;
        if (required <= 0L || TriggerManager.triggerManager.get(trigger).isEmpty()) {
            throw new IllegalArgumentException("Invalid amount or trigger for objective " + id);
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
        String format = section.getString("display", ConfigManager.configManager.getString(
                "objective-display.formats." + trigger, ConfigManager.configManager.getString(
                        "objective-display.formats." + key, "{lang:objective-action-" + key.replace('_', '-') + "}{target}")));
        if (!List.of("kill", "block_break", "fish", "consume").contains(key) && !section.contains("display")
                && !ConfigManager.configManager.config.contains("objective-display.formats." + trigger)
                && !ConfigManager.configManager.config.contains("objective-display.formats." + key)) {
            format = trigger;
        }
        return CommonUtil.modifyString(player, format, "target", targetName(player), "trigger", trigger).strip();
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
                if (key.equals("kill") && type.equals("match_entity")
                        && condition.getString("target", "TARGET").equalsIgnoreCase("TARGET")) {
                    targets = translatedTargets(rules.getStringList("entity-types"), "entity");
                } else if (key.equals("block_break") && type.equals("block_type")) {
                    targets = translatedTargets(new ArrayList<>(CommonUtil.values(condition, "types", "type-name")), "block");
                } else if ((key.equals("consume") && type.equals("match_item")
                        && condition.getString("item.selector", condition.getString("item", "CONTEXT")).equalsIgnoreCase("CONTEXT"))
                        || (key.equals("fish") && type.equals("catch_match_item"))) {
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
                || context.event() instanceof org.bukkit.event.Cancellable cancelled && cancelled.isCancelled()) {
            return BigDecimal.ZERO;
        }
        BigDecimal amount = counter.measure(context, counted -> EventConditionsManager.eventConditions.matches(
                section.getConfigurationSection("conditions"), counted));
        amount = amount.multiply(BigDecimal.valueOf(value)).min(CountMode.MAXIMUM);
        if (amount.signum() <= 0 || !AntiAbuseManager.antiAbuseManager.allows(
                section.getConfigurationSection("anti-abuse"), context, scope, true)) return BigDecimal.ZERO;
        return amount;
    }



}
