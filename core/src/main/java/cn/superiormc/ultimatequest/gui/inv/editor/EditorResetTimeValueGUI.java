package cn.superiormc.ultimatequest.gui.inv.editor;

import cn.superiormc.ultimatequest.UltimateQuest;
import cn.superiormc.ultimatequest.editor.EditorLang;
import cn.superiormc.ultimatequest.editor.EditorTarget;
import cn.superiormc.ultimatequest.editor.EditorUtil;
import cn.superiormc.ultimatequest.gui.InvGUI;
import cn.superiormc.ultimatequest.gui.Prompt;
import cn.superiormc.ultimatequest.managers.MenuStatusManager;
import cn.superiormc.ultimatequest.utils.ResetTimeUtil;
import cn.superiormc.ultimatequest.utils.CommonUtil;
import java.time.LocalDateTime;
import java.time.Instant;
import java.util.Arrays;
import java.util.Locale;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;

import java.util.ArrayList;
import java.util.List;

public class EditorResetTimeValueGUI extends InvGUI {

    private final EditorTarget target;

    private final String path;

    private final String backPath;

    public EditorResetTimeValueGUI(Player owner, EditorTarget target, String path, String backPath) {
        super(owner);
        this.target = target;
        this.path = path;
        this.backPath = backPath;
    }

    @Override
    public void constructGUI() {
        title = EditorLang.text(player, "editor.reset-time.title", "Reset Time: {path}",
                "path", EditorLang.displayPath(player, path));
        if (inv == null) {
            inv = UltimateQuest.methodUtil.createNewInv(player, 54, title, this);
        }
        inv.clear();

        String mode = getMode();
        inv.setItem(46, EditorUtil.createItem(Material.BOOK,
                EditorLang.text(player, "editor.reset-time.mode.name", "&eCurrent Mode"),
                modeLore(mode)));

        if (isListMode(mode)) {
            constructListMode();
        } else if (isManualStringMode(mode)) {
            constructManualStringMode();
        } else {
            constructNeverMode();
        }

        inv.setItem(47, EditorUtil.createItem(Material.COMPASS,
                EditorLang.text(player, "editor.reset-time.preview.name", "&eSchedule Preview"),
                preview()));
        inv.setItem(45, EditorUtil.createItem(Material.ARROW,
                EditorLang.text(player, "editor.common.back.name", "&eBack"),
                List.of(EditorLang.text(player, "editor.common.back.desc", "&7Return to the previous screen"))));
    }

    @Override
    public boolean clickEventHandle(Inventory inventory, ClickType type, int slot) {
        if (!MenuStatusManager.menuStatusManager.allowed(player)) {
            return true;
        }
        String mode = getMode();
        if (slot == 46) {
            List<String> modes = List.of("CRON", "TIMED", "TIMER", "CUSTOM");
            int index = modes.indexOf(mode);
            String next = modes.get((index + (type.isShiftClick() ? modes.size() - 1 : 1)) % modes.size());
            MenuStatusManager manager = MenuStatusManager.menuStatusManager;
            manager.setValue(player, target, getModePath(), next);
            String time = switch (next) {
                case "CRON" -> "0 0 4 * * ?";
                case "TIMED" -> "04:00:00";
                case "TIMER" -> "24:00:00";
                default -> CommonUtil.timeToString(LocalDateTime.now().plusDays(1L));
            };
            manager.setValue(player, target, path, time);
            new EditorResetTimeValueGUI(player, target, path, backPath).openGUI();
            return true;
        }
        if (slot == 45) {
            MenuStatusManager.menuStatusManager.openTarget(player, target, backPath, 0);
            return true;
        }
        if (isListMode(mode)) {
            return handleListMode(slot, type);
        }
        if (isManualStringMode(mode)) {
            return handleManualStringMode(slot);
        }
        if (slot == 49) {
            MenuStatusManager.menuStatusManager.removeValue(player, target, path);
            MenuStatusManager.menuStatusManager.openTarget(player, target, backPath, 0);
        }
        return true;
    }

    private void constructListMode() {
        List<String> values = getListValues();
        for (int i = 0; i < Math.min(45, values.size()); i++) {
            String value = values.get(i);
            inv.setItem(i, EditorUtil.createItem(Material.CLOCK,
                    "&e#" + (i + 1),
                    List.of(
                            EditorLang.text(player, "editor.summary.value", "&7Value: &f{value}", "value", value),
                            EditorLang.text(player, "editor.action.edit", "&aLeft click to edit"),
                            EditorLang.text(player, "editor.action.delete", "&cRight click to delete")
                    )));
        }
        inv.setItem(49, EditorUtil.createItem(Material.ANVIL,
                EditorLang.text(player, "editor.reset-time.add.name", "&eAdd Time"),
                List.of(EditorLang.text(player, "editor.reset-time.add.desc", "&7Add a new reset time entry"))));
        inv.setItem(50, EditorUtil.createItem(Material.BARRIER,
                EditorLang.text(player, "editor.common.clear.name", "&cClear"),
                List.of(EditorLang.text(player, "editor.common.clear.desc", "&7Remove this value"))));
    }

    private boolean handleListMode(int slot, ClickType type) {
        List<String> values = getListValues();
        if (slot >= 0 && slot < Math.min(45, values.size())) {
            if (type.isRightClick()) {
                values.remove(slot);
                saveListValues(values);
                new EditorResetTimeValueGUI(player, target, path, backPath).openGUI();
                return true;
            }
            promptEdit(slot, values.get(slot));
            return true;
        }
        if (slot == 49) {
            promptAdd();
            return true;
        }
        if (slot == 50) {
            MenuStatusManager.menuStatusManager.removeValue(player, target, path);
            MenuStatusManager.menuStatusManager.openTarget(player, target, backPath, 0);
            return true;
        }
        return true;
    }

    private void constructManualStringMode() {
        String current = target.getConfig().getString(path);
        inv.setItem(13, EditorUtil.createItem(Material.NAME_TAG,
                EditorLang.text(player, "editor.reset-time.manual.name", "&eEdit Value"),
                List.of(
                        EditorLang.text(player, "editor.reset-time.current", "&7Current: &f{value}",
                                "value", current == null || current.isEmpty()
                                        ? EditorLang.text(player, "editor.sub-button.not-set", "<not set>")
                                        : EditorUtil.trim(current, 42)),
                        EditorLang.text(player, "editor.action.edit", "&aLeft click to edit")
                )));
        inv.setItem(15, EditorUtil.createItem(Material.BARRIER,
                EditorLang.text(player, "editor.common.clear.name", "&cClear"),
                List.of(EditorLang.text(player, "editor.common.clear.desc", "&7Remove this value"))));
    }

    private boolean handleManualStringMode(int slot) {
        if (slot == 13) {
            MenuStatusManager.menuStatusManager.startPrompt(player, new Prompt(
                    EditorLang.text(player, "editor.prompt.string", "Input the new string for &f{path}", "path", path),
                    (p, input) -> {
                        saveValue(input);
                        new EditorResetTimeValueGUI(p, target, path, backPath).openGUI();
                    },
                    p -> new EditorResetTimeValueGUI(p, target, path, backPath).openGUI()));
            return true;
        }
        if (slot == 15) {
            MenuStatusManager.menuStatusManager.removeValue(player, target, path);
            MenuStatusManager.menuStatusManager.openTarget(player, target, backPath, 0);
            return true;
        }
        return true;
    }

    private void constructNeverMode() {
        String current = target.getConfig().getString(path);
        inv.setItem(13, EditorUtil.createItem(Material.PAPER,
                EditorLang.text(player, "editor.reset-time.never.name", "&eNo Reset Time Needed"),
                List.of(
                        EditorLang.text(player, "editor.reset-time.never.desc", "&7This reset mode does not use a time value."),
                        EditorLang.text(player, "editor.reset-time.current", "&7Current: &f{value}",
                                "value", current == null || current.isEmpty()
                                        ? EditorLang.text(player, "editor.sub-button.not-set", "<not set>")
                                        : current)
                )));
        inv.setItem(49, EditorUtil.createItem(Material.BARRIER,
                EditorLang.text(player, "editor.common.clear.name", "&cClear"),
                List.of(EditorLang.text(player, "editor.common.clear.desc", "&7Remove this value"))));
    }

    private List<String> getListValues() {
        String raw = target.getConfig().getString(path, "");
        if (raw == null || raw.isEmpty()) {
            return new ArrayList<>();
        }
        return new ArrayList<>(Arrays.asList(raw.split(";;", -1)));
    }

    private void saveListValues(List<String> values) {
        if (values == null || values.isEmpty()) {
            MenuStatusManager.menuStatusManager.removeValue(player, target, path);
            return;
        }
        saveValue(String.join(";;", values));
    }

    private void promptAdd() {
        MenuStatusManager.menuStatusManager.startPrompt(player, new Prompt(
                EditorLang.text(player, "editor.reset-time.prompt-add", "Input the new reset time for &f{path}",
                        "path", path),
                (p, input) -> {
                    List<String> values = getListValues();
                    values.add(input.trim());
                    saveListValues(values);
                    new EditorResetTimeValueGUI(player, target, path, backPath).openGUI();
                },
                p -> new EditorResetTimeValueGUI(player, target, path, backPath).openGUI()
        ));
    }

    private void promptEdit(int index, String currentValue) {
        MenuStatusManager.menuStatusManager.startPrompt(player, new Prompt(
                EditorLang.text(player, "editor.reset-time.prompt-edit", "Input the new reset time for entry &f#{index}",
                        "index", String.valueOf(index + 1))
                        + " "
                        + EditorLang.text(player, "editor.reset-time.current", "&7Current: &f{value}",
                        "value", currentValue),
                (p, input) -> {
                    List<String> values = getListValues();
                    values.set(index, input.trim());
                    saveListValues(values);
                    new EditorResetTimeValueGUI(player, target, path, backPath).openGUI();
                },
                p -> new EditorResetTimeValueGUI(player, target, path, backPath).openGUI()
        ));
    }

    private String getMode() {
        return target.getConfig().getString(getModePath(), "CRON").toUpperCase(Locale.ROOT);
    }

    private String getModePath() {
        if (!path.endsWith("-time")) {
            return path;
        }
        return path.substring(0, path.length() - 5) + "-mode";
    }

    private boolean isListMode(String mode) {
        return mode.equals("TIMED");
    }

    private boolean isManualStringMode(String mode) {
        return mode.equals("CRON") || mode.equals("TIMER") || mode.equals("CUSTOM");
    }

    private List<String> modeLore(String mode) {
        List<String> lore = new ArrayList<>();
        lore.add(EditorLang.text(player, "editor.reset-time.mode.value", "&7Mode: &f{value}", "value", mode));
        lore.add(EditorLang.text(player, "editor.reset-time.mode.desc", "&7Click to cycle modes (shift: reverse); sets a default value."));
        switch (mode) {
            case "CRON" -> {
                lore.add("&7Quartz cron: &f0 0 4 * * ?");
            }
            case "TIMER" -> {
                lore.add("&7[days:]hours:minutes:seconds");
            }
            case "TIMED" -> {
                lore.add("&7HH:mm[:ss];;HH:mm[:ss]");
            }
            case "CUSTOM" -> {
                lore.add("&7yyyy-MM-dd HH:mm:ss");
            }
            default -> {
            }
        }
        return lore;
    }

    private List<String> preview() {
        try {
            var schedule = ResetTimeUtil.parse(MenuStatusManager.menuStatusManager.getSection(target, backPath));
            Instant now = Instant.now();
            return List.of(EditorLang.text(player, "editor.reset-time.preview.previous", "&7Previous: &f{value}",
                    "value", schedule.previous(now).toString()),
                    EditorLang.text(player, "editor.reset-time.preview.next", "&7Next: &f{value}",
                    "value", schedule.next(now).toString()));
        } catch (RuntimeException exception) {
            return List.of(EditorLang.text(player, "editor.reset-time.invalid", "&cInvalid schedule: {value}",
                    "value", String.valueOf(exception.getMessage())));
        }
    }

    private void saveValue(String value) {
        try {
            YamlConfiguration candidate = new YamlConfiguration();
            candidate.loadFromString(target.getConfig().saveToString());
            candidate.set(path, value);
            ResetTimeUtil.parse(backPath.isEmpty() ? candidate : candidate.getConfigurationSection(backPath));
            MenuStatusManager.menuStatusManager.setValue(player, target, path, value);
        } catch (Exception exception) {
            EditorLang.send(player, "editor.reset-time.invalid", "&cInvalid schedule: {value}", "value", exception.getMessage());
        }
    }

}
