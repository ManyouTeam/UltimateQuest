package cn.superiormc.ultimatequest.services;

import cn.superiormc.ultimatequest.managers.ErrorManager;
import cn.superiormc.ultimatequest.managers.LanguageManager;
import cn.superiormc.ultimatequest.managers.QuestManager;
import cn.superiormc.ultimatequest.editor.EditorScope;
import cn.superiormc.ultimatequest.editor.EditorTarget;
import cn.superiormc.ultimatequest.managers.ConfigManager;
import cn.superiormc.ultimatequest.utils.TextUtil;
import cn.superiormc.ultimatequest.utils.ConfigurationFileUtil;
import java.io.File;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.bukkit.command.CommandSender;

public final class DefinitionEditor {

    private final QuestManager coordinator;

    public DefinitionEditor(QuestManager coordinator) {
        this.coordinator = coordinator;
    }

    public synchronized void editSeason(CommandSender sender, String type, String pass, String operation, List<String> values) {
        if (!pass.matches("[a-z0-9_-]+") || !Set.of("battlepass", "milestone", "normal", "period").contains(type)) {
            LanguageManager.languageManager.sendStringText(sender, "battlepass.admin-failed");
            return;
        }
        try {
            EditorTarget target = EditorTarget.load(EditorScope.ofName(type), pass);
            if (target == null) {
                throw new IllegalArgumentException("Definition file is unavailable");
            }
            var config = target.getConfig();
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
            saveTarget(target);
            LanguageManager.languageManager.sendStringText(sender, "battlepass.admin-saved");
            TextUtil.sendMessage(null, "§e[UltimateQuest] §f" + sender.getName() + " updated season " + pass + ": " + operation);
        } catch (Exception exception) {
            ErrorManager.errorManager.sendErrorMessage("§cError: Could not edit battle pass " + pass + ": " + exception.getMessage());
            LanguageManager.languageManager.sendStringText(sender, "battlepass.admin-failed");
        }
    }

    public synchronized void saveTarget(EditorTarget target) throws Exception {
        File expectedFile = target.getScope().getFile(target.getId());
        if (!expectedFile.equals(target.getFile())) {
            throw new IllegalArgumentException("Invalid editor target");
        }
        byte[] original = target.getOriginal();
        byte[] serialized = target.getConfig().saveToString().getBytes(StandardCharsets.UTF_8);
        if (!Arrays.equals(Files.readAllBytes(expectedFile.toPath()), original)) {
            throw new IllegalStateException("The file changed after it was opened. Reopen it before editing.");
        }
        boolean menu = target.getScope() == EditorScope.MENU;
        ConfigurationFileUtil.write(expectedFile.toPath(), serialized, true);
        try {
            if (menu) {
                ConfigManager.configManager.reloadMenus();
            } else {
                coordinator.reloadDefinitions();
            }
        } catch (Exception exception) {
            ConfigurationFileUtil.write(expectedFile.toPath(), original, false);
            try {
                if (menu) {
                    ConfigManager.configManager.reloadMenus();
                } else {
                    coordinator.reloadDefinitions();
                }
            } catch (Exception rollbackFailure) {
                exception.addSuppressed(rollbackFailure);
            }
            throw exception;
        }
        target.markSaved(serialized);
    }
}
