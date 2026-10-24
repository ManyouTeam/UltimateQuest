package cn.superiormc.ultimatequest.managers;

import cn.superiormc.ultimatequest.UltimateQuest;
import cn.superiormc.ultimatequest.utils.ConfigurationFileUtil;
import cn.superiormc.ultimatequest.utils.ResetTimeUtil;
import cn.superiormc.ultimatequest.utils.TextUtil;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import cn.superiormc.ultimatequest.objects.configs.ObjectSeason;

public final class SeasonManager {

    private final File stateFile = new File(UltimateQuest.instance.getDataFolder(), "season-cycles.yml");

    private YamlConfiguration cycles = new YamlConfiguration();

    private YamlConfiguration working;

    private Instant candidateNext;

    private Instant nextCheck = Instant.MAX;

    private boolean loaded;

    public synchronized void editSeason(QuestManager coordinator, CommandSender sender, String type, String pass,
            String operation, List<String> values) {
        if (!pass.matches("[a-z0-9_-]+") || !Set.of("battlepass", "milestone", "normal", "period").contains(type)) {
            LanguageManager.languageManager.sendStringText(sender, "battlepass.admin-failed");
            return;
        }
        try {
            String folder = switch (type) {
                case "battlepass" -> {
                    yield "battlepasses";
                }
                case "milestone" -> {
                    yield "milestones";
                }
                case "normal" -> {
                    yield "tasks";
                }
                case "period" -> {
                    yield "periods";
                }
                default -> {
                    throw new IllegalArgumentException("Unsupported season type");
                }
            };
            Path root = UltimateQuest.instance.getDataFolder().toPath().toRealPath();
            Path directory = root.resolve(folder).toRealPath();
            Path file = directory.resolve(pass + ".yml");
            if (!directory.startsWith(root) || Files.isSymbolicLink(file) || !Files.isRegularFile(file)
                    || !file.toRealPath().startsWith(directory)) {
                throw new IllegalArgumentException("Definition file is unavailable");
            }
            byte[] original = Files.readAllBytes(file);
            YamlConfiguration config = new YamlConfiguration();
            config.loadFromString(new String(original, StandardCharsets.UTF_8));
            switch (operation) {
                case "pause" -> {
                    config.set("paused", true);
                }
                case "resume" -> {
                    config.set("paused", false);
                }
                case "end" -> {
                    config.set("ends-at", Instant.now().toString());
                    config.set("rotation.enabled", false);
                }
                case "start" -> {
                    if (values.size() != 2 || !values.get(0).matches("[a-z0-9_-]+")) {
                        throw new IllegalArgumentException("start <season> <duration-seconds>");
                    }
                    if (coordinator.growthDefinitions(type).stream().anyMatch(definition -> definition.id().equals(pass)
                            && definition.season().equals(values.get(0)))) {
                        throw new IllegalArgumentException("Use a new season ID");
                    }
                    long seconds = Long.parseLong(values.get(1));
                    if (seconds <= 0L) {
                        throw new IllegalArgumentException("Season duration must be positive");
                    }
                    config.set("season", values.get(0));
                    config.set("starts-at", Instant.now().toString());
                    config.set("ends-at", Instant.now().plusSeconds(seconds).toString());
                    config.set("rotation.enabled", false);
                    config.set("paused", false);
                }
                case "schedule" -> {
                    if (values.size() != 3 || !values.get(0).matches("[a-z0-9_-]+")) {
                        throw new IllegalArgumentException("schedule <season> <UTC-start> <UTC-end>");
                    }
                    config.set("season", values.get(0));
                    config.set("starts-at", Instant.parse(values.get(1)).toString());
                    config.set("ends-at", Instant.parse(values.get(2)).toString());
                    config.set("rotation.enabled", false);
                    config.set("paused", false);
                }
                case "globalboost" -> {
                    if (values.size() != 2) {
                        throw new IllegalArgumentException("globalboost <multiplier> <seconds>");
                    }
                    double multiplier = Double.parseDouble(values.get(0));
                    long seconds = Long.parseLong(values.get(1));
                    if (!Double.isFinite(multiplier) || multiplier < 1D || seconds <= 0L) {
                        throw new IllegalArgumentException("Invalid booster");
                    }
                    config.set("global-booster.multiplier", multiplier);
                    config.set("global-booster.expires-at", Instant.now().plusSeconds(seconds).toEpochMilli());
                }
                default -> {
                    throw new IllegalArgumentException("Unknown season operation");
                }
            }
            if (!Arrays.equals(Files.readAllBytes(file), original)) {
                throw new IllegalStateException("The definition file changed during the operation. Try again.");
            }
            ConfigurationFileUtil.save(file, config, true);
            try {
                coordinator.reloadDefinitions();
            } catch (Exception exception) {
                ConfigurationFileUtil.write(file, original, false);
                try {
                    coordinator.reloadDefinitions();
                } catch (Exception rollbackFailure) {
                    exception.addSuppressed(rollbackFailure);
                }
                throw exception;
            }
            LanguageManager.languageManager.sendStringText(sender, "battlepass.admin-saved");
            TextUtil.sendMessage(null, "§e[UltimateQuest] §f" + sender.getName() + " updated season " + pass + ": " + operation);
        } catch (Exception exception) {
            ErrorManager.errorManager.sendErrorMessage("§cError: Could not edit season " + pass + ": " + exception.getMessage());
            LanguageManager.languageManager.sendStringText(sender, "battlepass.admin-failed");
        }
    }

    public void beginReload() throws Exception {
        if (!loaded && stateFile.isFile()) {
            cycles.load(stateFile);
        }
        loaded = true;
        working = new YamlConfiguration();
        working.loadFromString(cycles.saveToString());
        candidateNext = Instant.MAX;
    }

    public void prepare(String type, String id, YamlConfiguration config, Instant now) {
        ObjectSeason season = new ObjectSeason(config);
        if (!season.isRotating()) {
            return;
        }
        Instant start = season.getPrevious(now);
        long saved = working.getLong(type + "/" + id, Long.MIN_VALUE);
        if (saved > start.getEpochSecond()) {
            start = Instant.ofEpochSecond(saved);
        }
        Instant end = season.getNext(start);
        season.apply(start, end);
        working.set(type + "/" + id, start.getEpochSecond());
        if (end.isBefore(candidateNext)) {
            candidateNext = end;
        }
    }

    public void commitReload() throws Exception {
        if (!working.getValues(false).equals(cycles.getValues(false))) {
            ConfigurationFileUtil.save(stateFile.toPath(), working, false);
        }
        cycles = working;
        nextCheck = candidateNext;
    }

    public void refresh(QuestManager coordinator) {
        if (Instant.now().isBefore(nextCheck)) {
            return;
        }
        try {
            coordinator.reloadDefinitions();
        } catch (Exception exception) {
            nextCheck = Instant.now().plusSeconds(30L);
            ErrorManager.errorManager.sendErrorMessage("§cError: Could not rotate seasons: " + exception.getMessage());
        }
    }
}
