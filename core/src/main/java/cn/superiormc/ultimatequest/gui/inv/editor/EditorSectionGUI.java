package cn.superiormc.ultimatequest.gui.inv.editor;

import cn.superiormc.ultimatequest.UltimateQuest;
import cn.superiormc.ultimatequest.editor.EditorLang;
import cn.superiormc.ultimatequest.editor.EditorTarget;
import cn.superiormc.ultimatequest.editor.EditorTypeResolver;
import cn.superiormc.ultimatequest.editor.EditorUtil;
import cn.superiormc.ultimatequest.editor.EditorValueKind;
import cn.superiormc.ultimatequest.gui.InvGUI;
import cn.superiormc.ultimatequest.managers.MenuStatusManager;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class EditorSectionGUI extends InvGUI {

    private final EditorTarget target;

    private final String path;

    private final int page;

    private final List<String> keys = new ArrayList<>();

    public EditorSectionGUI(Player owner, EditorTarget target, String path, int page) {
        super(owner);
        this.target = target;
        this.path = path == null ? "" : path;
        this.page = Math.max(0, page);
    }

    @Override
    public void constructGUI() {
        ConfigurationSection section = MenuStatusManager.menuStatusManager.getSection(target, path);
        title = (target.dirty() ? "* " : "") + target.getId() + ": " + EditorUtil.displayPath(player, path);
        if (inv == null) {
            inv = UltimateQuest.methodUtil.createNewInv(player, 54, title, this);
        }
        inv.clear();
        keys.clear();
        if (section != null) {
            keys.addAll(section.getKeys(false));
            keys.sort(Comparator.naturalOrder());
        }

        int start = page * 45;
        for (int i = 0; i < 45; i++) {
            int index = start + i;
            if (index >= keys.size()) {
                break;
            }

            String key = keys.get(index);
            EditorValueKind kind = EditorTypeResolver.resolve(section, key);
            Object value = section.get(key);
            inv.setItem(i, createEntryItem(key, kind, value));
        }

        inv.setItem(45, EditorUtil.createItem(Material.ARROW,
                EditorLang.text(player, "editor.common.back.name", "&eBack"),
                List.of(EditorLang.text(player, "editor.common.back.desc", "&7Return to the previous screen"))));
        inv.setItem(46, EditorUtil.createItem(Material.OAK_SIGN,
                EditorLang.text(player, "editor.section.path.name", "&eCurrent Path"),
                List.of(EditorLang.text(player, "editor.section.path.desc", "&f{value}",
                        "value", EditorUtil.displayPath(player, path)))));
        inv.setItem(49, EditorUtil.createItem(Material.EMERALD,
                EditorLang.text(player, "editor.common.reload.name", "&aValidate and Apply"),
                List.of(
                        EditorLang.text(player, "editor.section.reload.line-1", "&7Changes are held in this session draft."),
                        EditorLang.text(player, "editor.section.reload.line-2", "&7Validate, back up, save and rebuild live definitions.")
                )));
        inv.setItem(48, EditorUtil.createItem(Material.REDSTONE,
                EditorLang.text(player, "editor.discard.name", "&cDiscard Draft"),
                List.of(EditorLang.text(player, "editor.discard.desc", "&7Shift-click to discard changes and reload this file"))));
        inv.setItem(50, EditorUtil.createItem(Material.ANVIL,
                EditorLang.text(player, "editor.section.add-child.name", "&eAdd Child"),
                List.of(EditorLang.text(player, "editor.section.add-child.generic", "&7Add a new child node under this section"))));
        if (categoryPath() != null) {
            setItem(47, EditorUtil.createItem(Material.CHEST,
                    EditorLang.text(player, "editor.category.edit", "&eEdit categories"), List.of()));
        }
        inv.setItem(52, EditorUtil.createItem(Material.SPECTRAL_ARROW,
                EditorLang.text(player, "editor.common.previous-page.name", "&ePrevious Page"),
                List.of(EditorLang.text(player, "editor.common.previous-page.desc", "&7Go to the previous page"))));
        inv.setItem(53, EditorUtil.createItem(Material.TIPPED_ARROW,
                EditorLang.text(player, "editor.common.next-page.name", "&eNext Page"),
                List.of(EditorLang.text(player, "editor.common.next-page.desc", "&7Go to the next page"))));
    }

    private ItemStack createEntryItem(String key, EditorValueKind kind, Object value) {
        Material material = switch (kind) {
            case ITEM_SECTION -> Material.ITEM_FRAME;
            case BOOLEAN -> Boolean.TRUE.equals(value) ? Material.LIME_DYE : Material.GRAY_DYE;
            case INTEGER, DOUBLE -> Material.SLIME_BALL;
            case STRING -> Material.NAME_TAG;
            case STRING_LIST, INTEGER_LIST -> Material.WRITABLE_BOOK;
            case SECTION -> Material.CHEST;
            default -> Material.PAPER;
        };

        List<String> lore = new ArrayList<>();
        lore.add(EditorLang.text(player, "editor.lore.path", "&7Path: &f{value}",
                "value", EditorTypeResolver.buildPath(path, key)));
        lore.add(EditorLang.text(player, "editor.lore.kind", "&7Kind: &f{value}",
                "value", EditorLang.valueKind(player, kind)));
        lore.addAll(EditorUtil.summarize(player, value));
        switch (kind) {
            case SECTION, ITEM_SECTION, STRING_LIST, INTEGER_LIST:
                lore.add(EditorLang.text(player, "editor.action.open", "&aLeft click to open"));
                if (kind == EditorValueKind.SECTION || kind == EditorValueKind.STRING_LIST || kind == EditorValueKind.INTEGER_LIST) {
                    lore.add(EditorLang.text(player, "editor.action.delete", "&cRight click to delete"));
                }
                if (kind == EditorValueKind.ITEM_SECTION) {
                    lore.add(EditorLang.text(player, "editor.item.raw-hint", "&eRight click: raw fields; shift-right: delete"));
                }
                break;
            case BOOLEAN:
                lore.add(EditorLang.text(player, "editor.action.toggle", "&aLeft click to toggle"));
                lore.add(EditorLang.text(player, "editor.action.delete", "&cRight click to delete"));
                break;
            case INTEGER, DOUBLE:
                lore.add(EditorLang.text(player, "editor.action.increase-ten", "&aLeft click to add 10"));
                lore.add(EditorLang.text(player, "editor.action.decrease-ten", "&cRight click to subtract 10"));
                lore.add(EditorLang.text(player, "editor.action.increase-one", "&aShift-left to add 1"));
                lore.add(EditorLang.text(player, "editor.action.decrease-one", "&cShift-right to subtract 1"));
                lore.add(EditorLang.text(player, "editor.action.middle-clear", "&eMiddle click to clear"));
                lore.add(EditorLang.text(player, "editor.action.input-number", "&eDrop key to enter an exact number"));
                break;
            default:
                lore.add(EditorLang.text(player, "editor.action.edit", "&aLeft click to edit"));
                lore.add(EditorLang.text(player, "editor.action.delete", "&cRight click to delete"));
                break;
        }

        return EditorUtil.createItem(material, "&e" + key, lore);
    }

    @Override
    public boolean clickEventHandle(Inventory inventory, ClickType type, int slot) {
        MenuStatusManager manager = MenuStatusManager.menuStatusManager;
        if (!manager.allowed(player)) {
            return true;
        }
        ConfigurationSection section = manager.getSection(target, path);
        if (slot >= 0 && slot < 45) {
            int index = page * 45 + slot;
            if (index >= keys.size() || section == null) {
                return true;
            }
            String key = keys.get(index);
            String childPath = EditorTypeResolver.buildPath(path, key);
            EditorValueKind kind = EditorTypeResolver.resolve(section, key);
            if (type.isRightClick() && kind != EditorValueKind.INTEGER && kind != EditorValueKind.DOUBLE) {
                if (kind == EditorValueKind.ITEM_SECTION && !type.isShiftClick()) {
                    manager.openTarget(player, target, childPath, 0);
                    return true;
                }
                manager.removeValue(player, target, childPath);
                manager.openTarget(player, target, path, page);
                return true;
            }
            switch (kind) {
                case SECTION -> {
                    manager.openTarget(player, target, childPath, 0);
                }
                case ITEM_SECTION -> {
                    new EditorItemValueGUI(player, target, childPath).openGUI();
                }
                case BOOLEAN -> {
                    manager.setValue(player, target, childPath, !section.getBoolean(key));
                    manager.openTarget(player, target, path, page);
                }
                case INTEGER, DOUBLE -> {
                    if (type == ClickType.DROP) {
                        manager.promptNumber(player, target, childPath, kind == EditorValueKind.INTEGER,
                                () -> manager.openTarget(player, target, path, page));
                        return true;
                    }
                    if (type == ClickType.MIDDLE) {
                        manager.removeValue(player, target, childPath);
                    } else {
                        int delta = resolveIntegerDelta(type);
                        if (delta != 0) {
                            if (kind == EditorValueKind.INTEGER) {
                                manager.setValue(player, target, childPath, Math.addExact(section.getLong(key), delta));
                            } else {
                                manager.setValue(player, target, childPath, section.getDouble(key) + delta);
                            }
                        }
                    }
                    manager.openTarget(player, target, path, page);
                }
                case STRING -> {
                    if (key.equals("reset-time")) {
                        new EditorResetTimeValueGUI(player, target, childPath, path).openGUI();
                    } else {
                        manager.promptString(player, target, childPath, () -> manager.openTarget(player, target, path, page));
                    }
                }
                case STRING_LIST, INTEGER_LIST -> {
                    new EditorListValueGUI(player, target, childPath, kind == EditorValueKind.INTEGER_LIST, 0).openGUI();
                }
                default -> {
                    manager.promptString(player, target, childPath, () -> manager.openTarget(player, target, path, page));
                }
            }
            return true;
        }
        if (slot == 45) {
            if (path.isEmpty()) {
                manager.openScope(player, target.getScope(), 0);
            } else {
                manager.openTarget(player, target, EditorUtil.parentPath(path), 0);
            }
        } else if (slot == 47 && categoryPath() != null) {
            new EditorListValueGUI(player, target, categoryPath(), false, 0).openGUI();
        } else if (slot == 48 && type.isShiftClick()) {
            manager.openTarget(player, EditorTarget.load(target.getScope(), target.getId()), path, page);
        } else if (slot == 49) {
            manager.apply(player, target);
            manager.openTarget(player, target, path, page);
        } else if (slot == 50) {
            manager.promptNewChild(player, target, path, () -> manager.openTarget(player, target, path, page));
        } else if (slot == 52 && page > 0) {
            manager.openTarget(player, target, path, page - 1);
        } else if (slot == 53 && (page + 1) * 45 < keys.size()) {
            manager.openTarget(player, target, path, page + 1);
        }
        return true;
    }

    private String categoryPath() {
        return switch (target.getScope()) {
            case TASK, SCROLL, MILESTONE -> path.isEmpty() ? "categories" : null;
            case PERIOD -> path.isEmpty() || path.equals("task") ? "task.categories" : null;
            case BATTLEPASS -> path.isEmpty() ? "categories"
                    : path.matches("tasks\\.[a-z0-9_-]+") ? path + ".categories" : null;
            case MENU -> path.matches("contents\\.[a-zA-Z0-9_-]+") ? path + ".categories" : null;
            default -> null;
        };
    }

    private int resolveIntegerDelta(ClickType type) {
        if (type.isShiftClick() && type.isLeftClick()) {
            return 1;
        }
        if (type.isShiftClick() && type.isRightClick()) {
            return -1;
        }
        if (type.isLeftClick()) {
            return 10;
        }
        if (type.isRightClick()) {
            return -10;
        }
        return 0;
    }
}
