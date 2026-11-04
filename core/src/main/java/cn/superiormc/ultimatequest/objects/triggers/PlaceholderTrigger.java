package cn.superiormc.ultimatequest.objects.triggers;

import cn.superiormc.ultimatequest.utils.TextUtil;

import java.math.BigDecimal;
import java.util.Set;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public final class PlaceholderTrigger extends EventTrigger<TriggerRuntime.PlayerTick> {

    private static final Set<String> RULES = Set.of(">=", ">", "<=", "<", "=", "==", "!=", "*=", "=*", "!*=", "!=*");

    public PlaceholderTrigger() {
        super("placeholder", "on-placeholder", TriggerRuntime.PlayerTick.class, Set.of());
    }

    @Override
    protected void handle(TriggerRuntime.PlayerTick tick, TriggerRuntime runtime) {
        if (tick.player().isOnline() && runtime.isObserved("placeholder", tick.player())) {
            runtime.fire(this, runtime.base(tick.player(), null).tick(tick.tick()));
        }
    }

    public static void validate(ConfigurationSection section) {
        if (section.getString("placeholder", "").isBlank() || !section.contains("value")
                || !RULES.contains(section.getString("rule", section.getString("operator", ">=")))) {
            throw new IllegalArgumentException("Placeholder objectives require placeholder, value, and a valid comparison rule");
        }
    }

    public static boolean matches(ConfigurationSection section, Player player) {
        String expression = section.getString("placeholder", "");
        String expectedExpression = section.getString("value", "");
        try {
            String actual = TextUtil.withPAPI(expression, player).trim();
            String expected = TextUtil.withPAPI(expectedExpression, player).trim();
            if (expression.contains("%") && actual.equals(expression)
                    || expectedExpression.contains("%") && expected.equals(expectedExpression)) {
                return false;
            }
            return compare(actual, section.getString("rule", section.getString("operator", ">=")), expected);
        } catch (RuntimeException exception) {
            return false;
        }
    }

    public static boolean compare(String actual, String rule, String expected) {
        return switch (rule) {
            case "==" -> actual.equals(expected);
            case "!=" -> !actual.equals(expected);
            case "*=" -> actual.contains(expected);
            case "=*" -> expected.contains(actual);
            case "!*=" -> !actual.contains(expected);
            case "!=*" -> !expected.contains(actual);
            case ">=", ">", "<=", "<", "=" -> compareNumbers(actual, rule, expected);
            default -> false;
        };
    }

    private static boolean compareNumbers(String actual, String rule, String expected) {
        try {
            int result = new BigDecimal(actual).compareTo(new BigDecimal(expected));
            return switch (rule) {
                case ">=" -> result >= 0;
                case ">" -> result > 0;
                case "<=" -> result <= 0;
                case "<" -> result < 0;
                case "=" -> result == 0;
                default -> false;
            };
        } catch (NumberFormatException exception) {
            return false;
        }
    }
}
