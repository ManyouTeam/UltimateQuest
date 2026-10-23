package cn.superiormc.ultimatequest.managers;

import cn.superiormc.ultimatequest.editor.EditorContext;
import cn.superiormc.ultimatequest.editor.EditorLang;
import cn.superiormc.ultimatequest.editor.EditorScope;
import cn.superiormc.ultimatequest.editor.EditorTarget;
import cn.superiormc.ultimatequest.editor.EditorTypeResolver;
import cn.superiormc.ultimatequest.gui.Prompt;
import cn.superiormc.ultimatequest.gui.inv.editor.EditorFileListGUI;
import cn.superiormc.ultimatequest.gui.inv.editor.EditorRootGUI;
import cn.superiormc.ultimatequest.gui.inv.editor.EditorSectionGUI;
import cn.superiormc.ultimatequest.methods.DebuildItem;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public final class MenuStatusManager extends AbstractManager {

    public static MenuStatusManager menuStatusManager;

    private final Map<UUID, EditorContext> contexts = new ConcurrentHashMap<>();
    private final Map<UUID, Prompt> prompts = new ConcurrentHashMap<>();

    public MenuStatusManager() {
        menuStatusManager = this;
    }

    public boolean allowed(Player player) {
        if (!player.hasPermission("ultimatequest.admin")) {
            LanguageManager.languageManager.sendStringText(player, "error.miss-permission");
            return false;
        }
        if (!ConfigManager.configManager.getBoolean("editor.enabled", true)) {
            EditorLang.send(player, "editor.message.disabled", "&cThe configuration editor is disabled.");
            return false;
        }
        return true;
    }

    public EditorContext getContext(Player player) {
        return contexts.computeIfAbsent(player.getUniqueId(), ignored -> new EditorContext());
    }

    public void openRoot(Player player) {
        if (!allowed(player)) {
            return;
        }
        EditorContext context = getContext(player);
        context.setScope(null);
        context.setTarget(null);
        context.setPath("");
        context.setPage(0);
        new EditorRootGUI(player).openGUI();
    }

    public void openScope(Player player, EditorScope scope, int page) {
        if (!allowed(player) || scope == null) {
            return;
        }
        EditorContext context = getContext(player);
        context.setScope(scope);
        context.setTarget(null);
        context.setPath("");
        context.setPage(page);
        new EditorFileListGUI(player, scope, context.getPage()).openGUI();
    }

    public void openFile(Player player, EditorScope scope, String id) {
        if (!allowed(player)) {
            return;
        }
        EditorTarget draft = getContext(player).draft(scope, id);
        openTarget(player, draft == null ? EditorTarget.load(scope, id) : draft, "", 0);
    }

    public void openTarget(Player player, EditorTarget target, String path, int page) {
        if (!allowed(player)) {
            return;
        }
        if (target == null) {
            EditorLang.send(player, "editor.message.target-not-found", "&cTarget file not found.");
            return;
        }
        EditorContext context = getContext(player);
        context.remember(target);
        context.setScope(target.getScope());
        context.setTarget(target);
        context.setPath(path);
        context.setPage(page);
        new EditorSectionGUI(player, target, context.getPath(), context.getPage()).openGUI();
    }

    public ConfigurationSection getSection(EditorTarget target, String path) {
        return path == null || path.isEmpty() ? target.getConfig() : target.getConfig().getConfigurationSection(path);
    }

    public void setValue(Player player, EditorTarget target, String path, Object value) {
        if (!allowed(player) || path == null || path.isBlank()) {
            return;
        }
        if (value instanceof Double number && !Double.isFinite(number)) {
            EditorLang.send(player, "editor.message.invalid-number", "&cThat is not a valid finite number.");
            return;
        }
        target.getConfig().set(path, value);
    }

    public void removeValue(Player player, EditorTarget target, String path) {
        setValue(player, target, path, null);
    }

    public void replaceItemSection(Player player, EditorTarget target, String path, ItemStack itemStack) {
        if (!allowed(player)) {
            return;
        }
        target.getConfig().set(path, null);
        DebuildItem.debuildItem(itemStack.clone(), target.getConfig().createSection(path));
    }

    public void apply(Player player, EditorTarget target) {
        if (!allowed(player)) {
            return;
        }
        try {
            QuestManager.questManager.editor().saveTarget(target);
            EditorLang.send(player, "editor.message.saved", "&aValidated and saved &f{value}",
                    "value", target.getScope().getFolderName() + "/" + target.getId() + ".yml");
        } catch (Exception exception) {
            ErrorManager.errorManager.sendErrorMessage("§cError: Could not apply editor target "
                    + target.getId() + ": " + exception.getMessage());
            EditorLang.send(player, "editor.message.save-failed", "&cNot applied: {value}", "value", exception.getMessage());
        }
    }

    public boolean hasPrompt(Player player) {
        return prompts.containsKey(player.getUniqueId());
    }

    public void startPrompt(Player player, Prompt prompt) {
        if (!allowed(player)) {
            return;
        }
        prompts.put(player.getUniqueId(), prompt);
        player.closeInventory();
        EditorLang.send(player, "editor.prompt.description", "&f{value}", "value", prompt.getDescription());
        EditorLang.send(player, "editor.prompt.cancel-tip", "&7Type &fcancel &7to abort this edit.");
    }

    public void submitPrompt(Player player, String input) {
        Prompt prompt = prompts.remove(player.getUniqueId());
        if (prompt == null || !allowed(player)) {
            return;
        }
        if (input.equalsIgnoreCase("cancel")) {
            prompt.cancel(player);
        } else {
            prompt.handle(player, input);
        }
    }

    public void clear(Player player) {
        prompts.remove(player.getUniqueId());
        contexts.remove(player.getUniqueId());
    }

    public void promptString(Player player, EditorTarget target, String path, Runnable reopenAction) {
        startPrompt(player, new Prompt(
                EditorLang.text(player, "editor.prompt.string", "Input the new string for &f{path}", "path", path),
                (p, input) -> {
                    setValue(p, target, path, input);
                    reopenAction.run();
                },
                p -> reopenAction.run()));
    }

    public void promptNumber(Player player, EditorTarget target, String path, boolean integer, Runnable reopenAction) {
        startPrompt(player, new Prompt(
                EditorLang.text(player, integer ? "editor.prompt.integer" : "editor.prompt.double",
                        "Input the new number for &f{path}", "path", path),
                (p, input) -> {
                    try {
                        if (integer) {
                            setValue(p, target, path, Long.parseLong(input.trim()));
                        } else {
                            double value = Double.parseDouble(input.trim());
                            if (!Double.isFinite(value)) {
                                throw new NumberFormatException("Non-finite number");
                            }
                            setValue(p, target, path, value);
                        }
                        reopenAction.run();
                    } catch (NumberFormatException exception) {
                        EditorLang.send(p, "editor.message.invalid-number", "&cThat is not a valid finite number.");
                        promptNumber(p, target, path, integer, reopenAction);
                    }
                },
                p -> reopenAction.run()));
    }

    public void promptNewChild(Player player, EditorTarget target, String parentPath, Runnable reopenAction) {
        startPrompt(player, new Prompt(
                EditorLang.text(player, "editor.prompt.new-child", "Input the new child key"),
                (p, input) -> {
                    String key = input.trim();
                    String path = EditorTypeResolver.buildPath(parentPath, key);
                    if (!key.matches("[a-zA-Z0-9_-]+") || target.getConfig().contains(path)) {
                        EditorLang.send(p, "editor.message.invalid-key", "&cUse a new key containing letters, numbers, _ or -.");
                        promptNewChild(p, target, parentPath, reopenAction);
                        return;
                    }
                    promptNewChildValue(p, target, path, reopenAction);
                },
                p -> reopenAction.run()));
    }

    private void promptNewChildValue(Player player, EditorTarget target, String path, Runnable reopenAction) {
        startPrompt(player, new Prompt(
                EditorLang.text(player, "editor.prompt.new-child-value",
                        "Input a value for &f{path}&7. Use section/{}, boolean, number, ;; list, [] or string:text.", "path", path),
                (p, input) -> {
                    try {
                        if (input.equalsIgnoreCase("section") || input.equals("{}")) {
                            target.getConfig().createSection(path);
                        } else {
                            setValue(p, target, path, parseNewChildValue(input));
                        }
                        reopenAction.run();
                    } catch (IllegalArgumentException exception) {
                        EditorLang.send(p, "editor.message.invalid-new-child-value", "&cInvalid value.");
                        promptNewChildValue(p, target, path, reopenAction);
                    }
                },
                p -> reopenAction.run()));
    }

    public static Object parseNewChildValue(String input) {
        String trimmed = input.trim();
        if (input.startsWith("string:")) {
            return input.substring(7);
        }
        if (trimmed.equals("[]")) {
            return List.of();
        }
        if (trimmed.equalsIgnoreCase("true") || trimmed.equalsIgnoreCase("false")) {
            return Boolean.parseBoolean(trimmed);
        }
        if (trimmed.contains(";;")) {
            List<String> values = Arrays.stream(input.split(";;", -1)).map(String::trim).toList();
            if (values.stream().allMatch(value -> value.matches("-?\\d+"))) {
                return values.stream().map(Long::parseLong).toList();
            }
            return values;
        }
        if (trimmed.matches("-?\\d+")) {
            return Long.parseLong(trimmed);
        }
        if (trimmed.matches("-?\\d+\\.\\d+(?:[eE][+-]?\\d+)?")) {
            double number = Double.parseDouble(trimmed);
            if (!Double.isFinite(number)) {
                throw new IllegalArgumentException("Non-finite number");
            }
            return number;
        }
        return input;
    }

    @Override
    public void onPluginDisable() {
        contexts.clear();
        prompts.clear();
    }
}
