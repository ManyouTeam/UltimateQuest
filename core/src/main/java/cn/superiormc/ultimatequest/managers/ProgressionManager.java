package cn.superiormc.ultimatequest.managers;

import cn.superiormc.ultimatequest.api.progression.ProgressionEvent;
import cn.superiormc.ultimatequest.api.progression.ProgressionService;
import cn.superiormc.ultimatequest.api.progression.ProgressionView;
import cn.superiormc.ultimatequest.objects.caches.ObjectCache;
import cn.superiormc.ultimatequest.objects.counting.CountMode;
import cn.superiormc.ultimatequest.objects.quests.QuestDefinition;
import cn.superiormc.ultimatequest.objects.quests.QuestInstance;
import cn.superiormc.ultimatequest.utils.CommonUtil;
import cn.superiormc.ultimatequest.utils.SchedulerUtil;
import cn.superiormc.ultimatequest.utils.TextUtil;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public final class ProgressionManager implements ProgressionService {

    private final QuestManager coordinator;

    private final List<Consumer<ProgressionEvent>> listeners = new CopyOnWriteArrayList<>();

    public ProgressionManager(QuestManager coordinator) {
        this.coordinator = coordinator;
    }

    @Override
    public Optional<ProgressionView> progress(UUID player, String type, String id) {
        QuestInstance quest = coordinator.growthInstance(player, type, id);
        return quest == null ? Optional.empty() : Optional.of(new ProgressionView(type, quest.definition().id(),
                quest.season(), quest.total(), quest.level(), quest.stageStates()));
    }

    @Override
    public AutoCloseable subscribe(Consumer<ProgressionEvent> listener) {
        listeners.add(listener);
        return () -> listeners.remove(listener);
    }

    public void publish(String action, UUID player, QuestInstance quest, String stage) {
        ProgressionEvent event = new ProgressionEvent(action, player, quest.definition().type(), quest.definition().id(),
                quest.season(), quest.total(), quest.level(), stage);
        for (Consumer<ProgressionEvent> listener : listeners) {
            try {
                listener.accept(event);
            } catch (RuntimeException exception) {
                ErrorManager.errorManager.sendErrorMessage("§cError: Progression listener failed: " + exception.getMessage());
            }
        }
    }

    @Override
    public void addExperience(Player player, String pass, long amount) {
        coordinator.addExperienceAndSave(player, pass, amount);
    }

    @Override
    public void claimAll(Player player, String type, String id) {
        coordinator.claimAll(player, type, id);
    }

    public void close() {
        listeners.clear();
    }

    public void applyChange(QuestInstance quest, String operation, List<String> values) {
        switch (operation) {
            case "setxp" -> {
                quest.setTotal(Long.parseLong(values.get(0)));
            }
            case "addxp" -> {
                quest.addTotal(QuestDefinition.positiveLongValue(values.get(0)));
            }
            case "removexp" -> {
                long amount = QuestDefinition.positiveLongValue(values.get(0));
                quest.setTotal(quest.totalAmount().subtract(BigDecimal.valueOf(amount)).max(BigDecimal.ZERO));
            }
            case "setlevel" -> {
                int level = Integer.parseInt(values.get(0));
                if (level != 0 && !quest.definition().thresholds().containsKey(level)) {
                    throw new IllegalArgumentException("Unknown level");
                }
                quest.setTotal(level == 0 ? 0L : quest.definition().thresholds().get(level));
            }
            case "premium" -> {
                if (!Set.of("true", "false").contains(values.get(0))) {
                    throw new IllegalArgumentException("Expected true or false");
                }
                quest.premium(Boolean.parseBoolean(values.get(0)));
            }
            case "boost" -> {
                double multiplier = Double.parseDouble(values.get(0));
                long seconds = QuestDefinition.positiveLongValue(values.get(1));
                quest.booster(multiplier, Instant.now().plusSeconds(seconds).toEpochMilli());
            }
            case "resolve" -> {
                String stage = values.get(0);
                if (!Set.of("CLAIMING", "REVIEW").contains(quest.stageState(stage))
                        || !Set.of("AVAILABLE", "CLAIMED").contains(values.get(1))) {
                    throw new IllegalArgumentException("Only unresolved claims may be reviewed");
                }
                quest.stageState(stage, values.get(1));
            }
            default -> {
                throw new IllegalArgumentException("Unknown progression operation");
            }
        }
    }

    public long boostedAmount(Player player, QuestInstance quest, long amount) {
        return boostedAmount(player, quest, BigDecimal.valueOf(amount)).longValue();
    }

    public BigDecimal boostedAmount(Player player, QuestInstance quest, BigDecimal amount) {
        if (amount.signum() <= 0 || !quest.definition().type().equals("battlepass")) {
            return amount;
        }
        double multiplier = quest.booster(Instant.now());
        ConfigurationSection settings = quest.definition().settings();
        if (SeasonManager.seasonManager != null) {
            multiplier = Math.max(multiplier, SeasonManager.seasonManager.globalBooster(quest.definition(), Instant.now()));
        }
        ConfigurationSection permissionBoosters = settings.getConfigurationSection("permission-boosters");
        if (permissionBoosters != null) {
            for (String permission : permissionBoosters.getKeys(false)) {
                ConfigurationSection booster = permissionBoosters.getConfigurationSection(permission);
                if (booster != null && player.hasPermission(booster.getString("permission", "ultimatequest.booster." + permission))) {
                    multiplier = Math.max(multiplier, booster.getDouble("multiplier", 1D));
                }
            }
        }
        if (!Double.isFinite(multiplier)) {
            return CountMode.MAXIMUM;
        }
        return amount.multiply(BigDecimal.valueOf(Math.max(1D, multiplier)))
                .min(CountMode.MAXIMUM);
    }

    public void levelFeedback(Player player, QuestInstance quest) {
        QuestInstance snapshot = new QuestInstance(quest);
        ObjectCache cache = CacheManager.cacheManager.getObjectCache(player.getUniqueId());
        if (cache == null) {
            return;
        }
        SchedulerUtil.runSync(player, () -> {
            if (!CacheManager.cacheManager.isSession(cache, player)) {
                return;
            }
            publish("level-up", player.getUniqueId(), snapshot, "");
            coordinator.sendQuestMessage(player, snapshot, "battlepass.level-up");
            ConfigurationSection feedback = snapshot.definition().settings().getConfigurationSection("level-up");
            if (feedback != null) {
                TextUtil.sendMessage(player, CommonUtil.modifyString(player,
                        feedback.getString("message", ""), coordinator.questPlaceholders(player, snapshot)));
            }
        });
    }

}
