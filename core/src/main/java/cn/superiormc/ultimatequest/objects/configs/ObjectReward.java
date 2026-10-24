package cn.superiormc.ultimatequest.objects.configs;

import cn.superiormc.ultimatequest.managers.ActionManager;
import cn.superiormc.ultimatequest.managers.ConfigManager;
import cn.superiormc.ultimatequest.managers.ErrorManager;
import cn.superiormc.ultimatequest.managers.LanguageManager;
import cn.superiormc.ultimatequest.managers.QuestManager;
import cn.superiormc.ultimatequest.methods.BuildItem;
import cn.superiormc.ultimatequest.objects.ObjectSingleAction;
import cn.superiormc.ultimatequest.objects.QuestContext;
import cn.superiormc.ultimatequest.objects.quests.QuestDefinition;
import cn.superiormc.ultimatequest.utils.CommonUtil;
import cn.superiormc.ultimatequest.utils.SchedulerUtil;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BiConsumer;

public final class ObjectReward {

    private final ConfigurationSection config;

    private static final String RANDOM_TYPE = "random";

    private static final Set<String> TYPES = Set.of("item", "console_command", "message", "battlepass-experience", RANDOM_TYPE);

    private static final int MAX_RANDOM_DEPTH = 8;

    private final ActionManager<ObjectSingleAction, QuestContext> actions = new ActionManager<>();

    public ObjectReward(ConfigurationSection config) {
        this.config = config;
    }

    public ConfigurationSection getConfig() {
        return config;
    }

    public void validate() {
        validate(config);
    }

    public List<String> preview(Player player) {
        return preview(player, config);
    }

    public CompletableFuture<Void> grant(Player player, String questId) {
        return grant(player, config, questId);
    }

    public void resolveReferences(ConfigurationSection config) {
        resolveReferences(config, Map.of());
    }

    public Map<String, YamlConfiguration> loadPools() {
        return ConfigManager.configManager.getRewardPoolConfigurations();
    }

    public void resolveReferences(ConfigurationSection config, Map<String, YamlConfiguration> pools) {
        try {
            resolveReferences(config, pools, 0);
        } catch (RuntimeException exception) {
            ErrorManager.errorManager.sendErrorMessage("搂cError: Could not resolve reward references at " + config.getCurrentPath() + ": " + exception.getMessage());
            throw exception;
        }
    }

    private void resolveReferences(ConfigurationSection section, Map<String, YamlConfiguration> pools, int depth) {
        if (depth > MAX_RANDOM_DEPTH) {
            throw new IllegalArgumentException("Reward references are cyclic or nested too deeply at " + section.getCurrentPath());
        }
        if (section.isString("reference")) {
            String reference = section.getString("reference", "");
            ConfigurationSection reward = ConfigManager.configManager.getSection("reward-library." + reference);
            if (reward.getKeys(false).isEmpty()) {
                throw new IllegalArgumentException("Unknown reward reference: " + reference);
            }
            for (String key : List.copyOf(section.getKeys(false))) {
                section.set(key, null);
            }
            copy(reward, section);
            resolveReferences(section, pools, depth + 1);
            return;
        }
        if (section.contains("pool") && section.getString("type", "").equals(RANDOM_TYPE)) {
            String id = section.getString("pool", "");
            if (!section.isString("pool") || !id.matches("[a-z0-9_-]+") || !pools.containsKey(id)) {
                throw new IllegalArgumentException("Unknown reward pool at " + section.getCurrentPath() + ": " + id);
            }
            if (section.contains("random")) {
                throw new IllegalArgumentException("Use either pool or random at " + section.getCurrentPath());
            }
            ConfigurationSection choices = pools.get(id).getConfigurationSection("rewards");
            if (choices == null || choices.getKeys(false).isEmpty()) {
                throw new IllegalArgumentException("Missing rewards in reward pool: " + id);
            }
            section.set("type", RANDOM_TYPE);
            section.set("pool", null);
            copy(choices, section.createSection("random"));
            depth++;
        }
        for (String key : List.copyOf(section.getKeys(false))) {
            ConfigurationSection child = section.getConfigurationSection(key);
            if (child != null) {
                resolveReferences(child, pools, depth);
            }
        }
    }

    private void copy(ConfigurationSection source, ConfigurationSection target) {
        for (Map.Entry<String, Object> entry : source.getValues(true).entrySet()) {
            if (!(entry.getValue() instanceof ConfigurationSection)) {
                target.set(entry.getKey(), entry.getValue());
            }
        }
    }

    public void validate(ConfigurationSection section) {
        try {
            validate(section, 0);
        } catch (RuntimeException exception) {
            ErrorManager.errorManager.sendErrorMessage("搂cError: Invalid reward configuration at " + (section == null ? "rewards" : section.getCurrentPath()) + ": " + exception.getMessage());
            throw exception;
        }
    }

    private void validate(ConfigurationSection section, int depth) {
        if (section == null) {
            return;
        }
        if (depth > MAX_RANDOM_DEPTH) {
            throw new IllegalArgumentException("Random rewards are nested too deeply");
        }
        for (String key : section.getKeys(false)) {
            ConfigurationSection reward = section.getConfigurationSection(key);
            if (reward == null) {
                throw new IllegalArgumentException("Reward must be a section: " + key);
            }
            String type = reward.getString("type", "");
            if (!TYPES.contains(type)) {
                throw new IllegalArgumentException("Unknown reward type: " + type);
            }
            if (type.equals("item") && !reward.isConfigurationSection("item")) {
                throw new IllegalArgumentException("Missing reward item: " + key);
            }
            if (type.equals("console_command") && reward.getString("command", "").isBlank()) {
                throw new IllegalArgumentException("Missing reward command: " + key);
            }
            if (type.equals("message") && !reward.isString("message")) {
                throw new IllegalArgumentException("Missing reward message: " + key);
            }
            if (type.equals("battlepass-experience")) {
                if (!reward.getString("battlepass", "").matches("[a-z0-9_-]+")) {
                    throw new IllegalArgumentException("Missing battle pass reward target: " + key);
                }
                QuestDefinition.positiveLong(reward, "amount", 0L);
            }
            if (type.equals(RANDOM_TYPE)) {
                validateRandom(reward, key, depth);
            }
        }
    }

    private void validateRandom(ConfigurationSection reward, String key, int depth) {
        if ((reward.contains("count") && !reward.isInt("count")) || reward.getInt("count", 1) <= 0) {
            throw new IllegalArgumentException("Random reward count must be a positive integer: " + key);
        }
        if (reward.contains("repeat") && !reward.isBoolean("repeat")) {
            throw new IllegalArgumentException("Random reward repeat must be a boolean: " + key);
        }
        ConfigurationSection choices = reward.getConfigurationSection("random");
        if (choices == null || choices.getKeys(false).isEmpty()) {
            throw new IllegalArgumentException("Missing random reward choices: " + key);
        }
        double total = 0.0D;
        int available = 0;
        for (String choice : choices.getKeys(false)) {
            ConfigurationSection entry = choices.getConfigurationSection(choice);
            if (entry == null) {
                throw new IllegalArgumentException("Random reward choice must be a section: " + key + "/" + choice);
            }
            double rate = entry.getDouble("rate", 0);
            if ((entry.contains("rate") && !(entry.get("rate") instanceof Number)) || !Double.isFinite(rate) || rate < 0.0D) {
                throw new IllegalArgumentException("Random reward rate must be a nonnegative number: " + key + "/" + choice);
            }
            total += rate;
            if (rate > 0.0D) {
                available++;
            }
            ConfigurationSection nested = entry.getConfigurationSection("rewards");
            if (nested == null || nested.getKeys(false).isEmpty()) {
                throw new IllegalArgumentException("Missing rewards in random choice: " + key + "/" + choice);
            }
            validate(nested, depth + 1);
        }
        if (!Double.isFinite(total) || total <= 0.0D) {
            throw new IllegalArgumentException("Random reward rates must add up to a finite positive value: " + key);
        }
        if (!reward.getBoolean("repeat", false) && reward.getInt("count", 1) > available) {
            throw new IllegalArgumentException("Random reward count exceeds the number of positive-weight choices: " + key);
        }
    }

    public void forEachReward(ConfigurationSection section, BiConsumer<String, ConfigurationSection> consumer) {
        forEachReward(section, consumer, 0);
    }

    private void forEachReward(ConfigurationSection section, BiConsumer<String, ConfigurationSection> consumer, int depth) {
        if (section == null || depth > MAX_RANDOM_DEPTH) {
            return;
        }
        for (String key : section.getKeys(false)) {
            ConfigurationSection reward = section.getConfigurationSection(key);
            if (reward == null) {
                continue;
            }
            consumer.accept(key, reward);
            if (reward.getString("type", "").equals(RANDOM_TYPE)) {
                ConfigurationSection choices = reward.getConfigurationSection("random");
                if (choices != null) {
                    for (String choice : choices.getKeys(false)) {
                        ConfigurationSection entry = choices.getConfigurationSection(choice);
                        if (entry != null) {
                            forEachReward(entry.getConfigurationSection("rewards"), consumer, depth + 1);
                        }
                    }
                }
            }
        }
    }

    public boolean hasRandom(ConfigurationSection section) {
        if (section == null) {
            return false;
        }
        for (String key : section.getKeys(false)) {
            ConfigurationSection reward = section.getConfigurationSection(key);
            if (reward != null && reward.getString("type", "").equals(RANDOM_TYPE)) {
                return true;
            }
        }
        return false;
    }

    public YamlConfiguration resolveSnapshot(ConfigurationSection section) {
        return resolveKeys(section, drawKeys(section));
    }

    public List<String> drawKeys(ConfigurationSection section) {
        return List.copyOf(resolveRandom(section).keySet());
    }

    public YamlConfiguration resolveKeys(ConfigurationSection section, List<String> keys) {
        YamlConfiguration snapshot = new YamlConfiguration();
        ConfigurationSection root = snapshot.createSection("rewards");
        for (String key : keys) {
            ConfigurationSection reward = findReward(section, key, 0);
            if (reward == null) {
                throw new IllegalArgumentException("Saved reward key no longer exists: " + key);
            }
            ConfigurationSection target = root.createSection(key);
            for (Map.Entry<String, Object> value : reward.getValues(true).entrySet()) {
                if (!(value.getValue() instanceof ConfigurationSection)) {
                    target.set(value.getKey(), value.getValue());
                }
            }
        }
        validate(root);
        return snapshot;
    }

    private ConfigurationSection findReward(ConfigurationSection section, String path, int depth) {
        if (section == null || depth > MAX_RANDOM_DEPTH) {
            return null;
        }
        for (String key : section.getKeys(false)) {
            ConfigurationSection reward = section.getConfigurationSection(key);
            if (reward == null) {
                continue;
            }
            if (!reward.getString("type", "").equals(RANDOM_TYPE)) {
                if (path.equals(key)) {
                    return reward;
                }
                continue;
            }
            if (!path.startsWith(key + "/")) {
                continue;
            }
            String remaining = path.substring(key.length() + 1);
            int separator = remaining.indexOf('/');
            if (separator < 1 || !remaining.substring(0, separator).matches("[1-9][0-9]*")) {
                continue;
            }
            remaining = remaining.substring(separator + 1);
            ConfigurationSection choices = reward.getConfigurationSection("random");
            if (choices == null) {
                continue;
            }
            for (String choice : choices.getKeys(false)) {
                if (remaining.startsWith(choice + "/")) {
                    ConfigurationSection resolved = findReward(choices.getConfigurationSection(choice + ".rewards"), remaining.substring(choice.length() + 1), depth + 1);
                    if (resolved != null) {
                        return resolved;
                    }
                }
            }
        }
        return null;
    }

    public List<String> preview(Player player, ConfigurationSection section) {
        List<String> lines = new ArrayList<>();
        if (section != null) {
            for (String key : section.getKeys(false)) {
                ConfigurationSection reward = section.getConfigurationSection(key);
                if (reward == null) {
                    continue;
                }
                previewReward(player, reward, key, lines);
            }
        }
        if (lines.isEmpty()) {
            lines.add(LanguageManager.languageManager.getStringText(player, "battlepass.reward-empty"));
        }
        return lines;
    }

    private void previewReward(Player player, ConfigurationSection reward, String key, List<String> lines) {
        if (reward.getString("type", "").equals(RANDOM_TYPE)) {
            previewRandom(player, reward, key, lines);
            return;
        }
        if (reward.isList("display.lore")) {
            lines.addAll(CommonUtil.modifyList(player, reward.getStringList("display.lore")));
        } else if (reward.isString("display.name")) {
            lines.add(CommonUtil.modifyString(player, reward.getString("display.name")));
        } else if (reward.getString("type", "").equals("item")) {
            ConfigurationSection item = reward.getConfigurationSection("item");
            lines.add(LanguageManager.languageManager.getStringText(player, "battlepass.reward-item", "", "name", CommonUtil.modifyString(player, item.getString("name", item.getString("material", key))), "amount", String.valueOf(item.getInt("amount", 1))));
        } else if (reward.getString("type", "").equals("battlepass-experience")) {
            lines.add(LanguageManager.languageManager.getStringText(player, "battlepass.reward-xp", "", "amount", String.valueOf(reward.getLong("amount")), "pass", reward.getString("battlepass", "")));
        } else if (!reward.getString("type", "").equals("message")) {
            lines.add(LanguageManager.languageManager.getStringText(player, "battlepass.reward-custom", "", "name", key));
        }
    }

    private void previewRandom(Player player, ConfigurationSection reward, String key, List<String> lines) {
        List<String> header = new ArrayList<>();
        if (reward.isList("display.lore")) {
            header.addAll(CommonUtil.modifyList(player, reward.getStringList("display.lore")));
        } else if (reward.isString("display.name")) {
            header.add(CommonUtil.modifyString(player, reward.getString("display.name")));
        }
        ConfigurationSection choices = reward.getConfigurationSection("random");
        if (choices == null || choices.getKeys(false).isEmpty()) {
            lines.addAll(header);
            lines.add(LanguageManager.languageManager.getStringText(player, "battlepass.reward-custom", "", "name", key));
            return;
        }
        lines.addAll(header);
        int count = reward.getInt("count", 1);
        String mode = LanguageManager.languageManager.getStringText(player, reward.getBoolean("repeat", false) ? "battlepass.reward-random-repeat" : "battlepass.reward-random-unique");
        lines.add(LanguageManager.languageManager.getStringText(player, "battlepass.reward-random-count", "", "count", String.valueOf(count), "mode", mode));
        double total = totalRate(choices);
        for (String choice : choices.getKeys(false)) {
            ConfigurationSection entry = choices.getConfigurationSection(choice);
            if (entry == null) {
                continue;
            }
            List<String> nested = new ArrayList<>();
            ConfigurationSection nestedRewards = entry.getConfigurationSection("rewards");
            if (nestedRewards != null) {
                for (String nestedKey : nestedRewards.getKeys(false)) {
                    ConfigurationSection nestedReward = nestedRewards.getConfigurationSection(nestedKey);
                    if (nestedReward != null) {
                        previewReward(player, nestedReward, nestedKey, nested);
                    }
                }
            }
            if (nested.isEmpty()) {
                continue;
            }
            String detail;
            if (count == 1) {
                String rate = total <= 0.0D ? "0" : String.valueOf(Math.round(entry.getDouble("rate", 0) / total * 100.0D));
                detail = LanguageManager.languageManager.getStringText(player, "battlepass.reward-random-rate", "", "rate", rate);
            } else {
                detail = LanguageManager.languageManager.getStringText(player, "battlepass.reward-random-weight", "", "weight", String.valueOf(entry.getDouble("rate", 0)));
            }
            for (int i = 0; i < nested.size(); i++) {
                lines.add(i == 0 ? nested.get(i) + detail : "&8    " + nested.get(i));
            }
        }
    }

    private static double totalRate(ConfigurationSection choices) {
        double total = 0.0D;
        for (String choice : choices.getKeys(false)) {
            ConfigurationSection entry = choices.getConfigurationSection(choice);
            if (entry != null) {
                double rate = entry.getDouble("rate", 0);
                if (Double.isFinite(rate) && rate > 0.0D) {
                    total += rate;
                }
            }
        }
        return total;
    }

    public CompletableFuture<Void> grant(Player player, QuestDefinition definition) {
        return grant(player, definition.rewards(), definition.id());
    }

    public CompletableFuture<Void> grant(Player player, ConfigurationSection section, String questId) {
        List<ItemStack> items = new ArrayList<>();
        List<String> commands = new ArrayList<>();
        List<ConfigurationSection> feedback = new ArrayList<>();
        List<ConfigurationSection> experience = new ArrayList<>();
        if (section == null) {
            return CompletableFuture.completedFuture(null);
        }
        try {
            Map<String, ConfigurationSection> resolved = resolveRandom(section);
            for (Map.Entry<String, ConfigurationSection> entry : resolved.entrySet()) {
                String key = entry.getKey();
                ConfigurationSection reward = entry.getValue();
                String type = reward.getString("type");
                if (type.equals("item")) {
                    ItemStack item = BuildItem.buildItemStack(player, reward.getConfigurationSection("item"));
                    if (item == null || item.getType().isAir() || item.getAmount() <= 0) {
                        throw new IllegalArgumentException("Invalid reward item: " + key);
                    }
                    items.add(item);
                } else if (type.equals("console_command")) {
                    commands.add(CommonUtil.modifyString(player, reward.getString("command"), "player", player.getName(), "uuid", player.getUniqueId().toString(), "quest", questId));
                } else if (type.equals("battlepass-experience")) {
                    if (!QuestManager.questManager.canAddExperience(player, reward.getString("battlepass", ""))) {
                        throw new IllegalArgumentException("Battle pass experience reward target is unavailable");
                    }
                    experience.add(reward);
                } else {
                    feedback.add(reward);
                }
            }
            for (ConfigurationSection reward : experience) {
                QuestManager.questManager.addExperience(player, reward.getString("battlepass"), reward.getLong("amount"));
            }
            for (ItemStack item : items) {
                CommonUtil.giveOrDrop(player, item);
            }
            for (ConfigurationSection message : feedback) {
                actions.doAction(new ObjectSingleAction(message), player, new QuestContext(null, null, player));
            }
        } catch (RuntimeException exception) {
            ErrorManager.errorManager.sendErrorMessage("搂cError: Could not grant rewards for " + questId + ": " + exception.getMessage());
            return CompletableFuture.failedFuture(exception);
        }
        CompletableFuture<Void> result = new CompletableFuture<>();
        if (commands.isEmpty()) {
            result.complete(null);
        } else {
            try {
                SchedulerUtil.runSync(() -> {
                    try {
                        for (String command : commands) {
                            if (!Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command)) {
                                throw new IllegalStateException("Reward command was not accepted");
                            }
                        }
                        result.complete(null);
                    } catch (RuntimeException exception) {
                        ErrorManager.errorManager.sendErrorMessage("搂cError: Could not execute reward commands for " + questId + ": " + exception.getMessage());
                        result.completeExceptionally(exception);
                    }
                });
            } catch (RuntimeException exception) {
                ErrorManager.errorManager.sendErrorMessage("搂cError: Could not schedule reward commands for " + questId + ": " + exception.getMessage());
                result.completeExceptionally(exception);
            }
        }
        return result;
    }

    private Map<String, ConfigurationSection> resolveRandom(ConfigurationSection section) {
        validate(section);
        Map<String, ConfigurationSection> resolved = new LinkedHashMap<>();
        collectResolved(section, resolved, "", 0);
        return resolved;
    }

    private void collectResolved(ConfigurationSection section, Map<String, ConfigurationSection> resolved, String prefix, int depth) {
        if (section == null) {
            return;
        }
        for (String key : section.getKeys(false)) {
            ConfigurationSection reward = section.getConfigurationSection(key);
            if (reward == null) {
                continue;
            }
            String path = prefix.isEmpty() ? key : prefix + "/" + key;
            if (!reward.getString("type", "").equals(RANDOM_TYPE)) {
                resolved.put(path, reward);
                continue;
            }
            if (depth >= MAX_RANDOM_DEPTH) {
                throw new IllegalArgumentException("Random rewards are nested too deeply: " + path);
            }
            List<ConfigurationSection> choices = roll(reward);
            for (int draw = 0; draw < choices.size(); draw++) {
                ConfigurationSection choice = choices.get(draw);
                collectResolved(choice.getConfigurationSection("rewards"), resolved, path + "/" + (draw + 1) + "/" + choice.getName(), depth + 1);
            }
        }
    }

    private List<ConfigurationSection> roll(ConfigurationSection reward) {
        ConfigurationSection choices = reward.getConfigurationSection("random");
        List<ConfigurationSection> candidates = new ArrayList<>();
        for (String choice : choices.getKeys(false)) {
            ConfigurationSection entry = choices.getConfigurationSection(choice);
            if (entry == null) {
                continue;
            }
            double rate = entry.getDouble("rate", 0);
            if (!Double.isFinite(rate) || rate <= 0.0D) {
                continue;
            }
            candidates.add(entry);
        }
        List<ConfigurationSection> selected = new ArrayList<>();
        int count = reward.getInt("count", 1);
        boolean repeat = reward.getBoolean("repeat", false);
        for (int draw = 0; draw < count; draw++) {
            double total = 0.0D;
            for (ConfigurationSection candidate : candidates) {
                total += candidate.getDouble("rate", 0);
            }
            double picked = ThreadLocalRandom.current().nextDouble(total);
            ConfigurationSection chosen = candidates.get(candidates.size() - 1);
            for (ConfigurationSection candidate : candidates) {
                picked -= candidate.getDouble("rate", 0);
                if (picked < 0.0D) {
                    chosen = candidate;
                    break;
                }
            }
            selected.add(chosen);
            if (!repeat) {
                candidates.remove(chosen);
            }
        }
        return selected;
    }
}
