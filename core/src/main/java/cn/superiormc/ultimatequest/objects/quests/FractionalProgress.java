package cn.superiormc.ultimatequest.objects.quests;

import cn.superiormc.ultimatequest.objects.counting.CountMode;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Fixed six-decimal carry while retaining the existing integer save/API fields. */
public final class FractionalProgress {
    private FractionalProgress() {
    }

    public record Result(long whole, BigDecimal remainder) { }

    public static Result add(long current, BigDecimal remainder, BigDecimal amount, long maximum) {
        if (current < 0L || maximum < current) throw new IllegalArgumentException("Invalid progress bounds");
        if (amount.signum() <= 0 || current == maximum) return new Result(current, remainder);
        BigDecimal sum = BigDecimal.valueOf(current).add(remainder).add(amount)
                .setScale(CountMode.SCALE, RoundingMode.HALF_UP).min(BigDecimal.valueOf(maximum));
        long whole = sum.longValue();
        return new Result(whole, sum.subtract(BigDecimal.valueOf(whole)).stripTrailingZeros());
    }

    public static BigDecimal readRemainder(String text) {
        BigDecimal result;
        try {
            result = new BigDecimal(text).setScale(CountMode.SCALE, RoundingMode.UNNECESSARY).stripTrailingZeros();
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException("Invalid saved progress precision: " + text, exception);
        }
        if (result.signum() < 0 || result.compareTo(BigDecimal.ONE) >= 0) {
            throw new IllegalArgumentException("Invalid saved progress remainder: " + text);
        }
        return result;
    }
}
