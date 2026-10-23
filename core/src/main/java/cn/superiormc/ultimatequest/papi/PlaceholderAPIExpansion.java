package cn.superiormc.ultimatequest.papi;

import java.math.BigDecimal;
import cn.superiormc.ultimatequest.UltimateQuest;
import cn.superiormc.ultimatequest.managers.QuestManager;
import cn.superiormc.ultimatequest.objects.quests.QuestInstance;
import java.util.List;
import java.time.Instant;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

public final class PlaceholderAPIExpansion extends PlaceholderExpansion {

    private final UltimateQuest plugin;

    public PlaceholderAPIExpansion(UltimateQuest plugin) {
        this.plugin = plugin;
    }

    @Override
    @NotNull
    public String getAuthor() {
        return "PQguanfang";
    }

    @Override
    @NotNull
    public String getIdentifier() {
        return "ultimatequest";
    }

    @Override
    @NotNull
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        if (player == null || params == null || QuestManager.questManager == null) {
            return null;
        }
        if (!QuestManager.questManager.ready(player.getUniqueId())) {
            return "LOADING";
        }
        for (String type : List.of("battlepass", "milestone")) {
            for (String field : List.of("total", "level", "season", "remaining", "progress", "percent", "premium", "claimable", "timeleft")) {
                String prefix = type + "_" + field + "_";
                if (params.startsWith(prefix)) {
                    String id = params.substring(prefix.length());
                    QuestInstance growth = QuestManager.questManager.growthInstance(player.getUniqueId(), type, id);
                    if (field.equals("season")) {
                        var definition = QuestManager.questManager.growthDefinition(type, id);
                        return definition == null ? "" : definition.season();
                    }
                    if (growth != null) {
                        long next = growth.definition().thresholds().values().stream()
                                .filter(value -> value > growth.total()).findFirst().orElse(growth.total());
                        long previous = growth.definition().thresholds().values().stream()
                                .filter(value -> value <= growth.total()).reduce((first, second) -> second).orElse(0L);
                        switch (field) {
                            case "remaining" -> {
                                return growth.totalAmount().negate().add(BigDecimal.valueOf(next)).max(BigDecimal.ZERO).stripTrailingZeros().toPlainString();
                            }
                            case "progress" -> {
                                return growth.totalAmount().subtract(BigDecimal.valueOf(previous)).stripTrailingZeros().toPlainString();
                            }
                            case "percent" -> {
                                return String.valueOf(next == previous ? 100
                                        : (int) Math.min(100D, (growth.totalAmount().doubleValue() - previous) * 100D / (next - previous)));
                            }
                            case "premium" -> {
                                return String.valueOf(player.getPlayer() != null && growth.premium(player.getPlayer()));
                            }
                            case "claimable" -> {
                                return String.valueOf(growth.stageStates().entrySet().stream()
                                        .filter(entry -> entry.getValue().equals("AVAILABLE")
                                                && (!entry.getKey().startsWith("premium/")
                                                || player.getPlayer() != null && growth.premium(player.getPlayer()))).count());
                            }
                            case "timeleft" -> {
                                return growth.definition().endsAt() == null ? "-1" : String.valueOf(Math.max(0L,
                                        growth.definition().endsAt().getEpochSecond() - Instant.now().getEpochSecond()));
                            }
                            default -> {
                            }
                        }
                    }
                    return growth == null ? "0" : field.equals("total")
                            ? growth.totalAmount().stripTrailingZeros().toPlainString() : String.valueOf(growth.level());
                }
            }
        }
        if (!params.startsWith("state_")) {
            return null;
        }
        QuestInstance instance = QuestManager.questManager.instance(player.getUniqueId(), params.substring(6));
        return instance == null ? "AVAILABLE" : instance.state();
    }
}
