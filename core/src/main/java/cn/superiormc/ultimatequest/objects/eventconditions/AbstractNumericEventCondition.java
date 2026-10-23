package cn.superiormc.ultimatequest.objects.eventconditions;

import cn.superiormc.ultimatequest.objects.ObjectSingleCondition;
import cn.superiormc.ultimatequest.objects.QuestContext;
import java.util.Locale;

abstract class AbstractNumericEventCondition extends AbstractEventCondition {

    protected AbstractNumericEventCondition(String type) {
        super(type);
    }

    @Override
    protected final boolean onMatch(ObjectSingleCondition condition) {
        QuestContext context = condition.getContext();
        if (context == null) {
            return false;
        }
        Double current = currentValue(context, condition);
        if (current == null) {
            return false;
        }
        if (condition.contains("value") || condition.contains("compare")) {
            double target = condition.getDouble("value", current, context, "original", String.valueOf(current), "current", String.valueOf(current));
            if (!compare(current, target, condition.getString("compare", ">="))) {
                return false;
            }
        }
        double minimum = condition.getDouble("min", -Double.MAX_VALUE, context, "original", String.valueOf(current), "current", String.valueOf(current));
        double maximum = condition.getDouble("max", Double.MAX_VALUE, context, "original", String.valueOf(current), "current", String.valueOf(current));
        return current >= minimum && current <= maximum;
    }

    protected abstract Double currentValue(QuestContext context, ObjectSingleCondition condition);

    private boolean compare(double current, double target, String rawOperator) {
        String operator = rawOperator == null ? ">=" : rawOperator.trim().toLowerCase(Locale.ROOT);
        return switch(operator) {
            case ">", "gt" ->
                current > target;
            case ">=", "=>", "gte" ->
                current >= target;
            case "<", "lt" ->
                current < target;
            case "<=", "=<", "lte" ->
                current <= target;
            case "!=", "<>", "ne" ->
                current != target;
            case "=", "==", "eq" ->
                current == target;
            default ->
                current >= target;
        };
    }
}
