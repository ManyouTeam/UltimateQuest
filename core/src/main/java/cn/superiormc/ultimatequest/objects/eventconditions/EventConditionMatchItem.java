package cn.superiormc.ultimatequest.objects.eventconditions;

import cn.superiormc.ultimatequest.api.trigger.EntitySelector;
import cn.superiormc.ultimatequest.api.trigger.ItemSelector;
import cn.superiormc.ultimatequest.managers.MatchItemManager;
import cn.superiormc.ultimatequest.objects.ObjectSingleCondition;
import cn.superiormc.ultimatequest.objects.QuestContext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;

public final class EventConditionMatchItem extends AbstractEventCondition {

    public EventConditionMatchItem() {
        super("match_item");
    }

    @Override
    protected boolean onMatch(ObjectSingleCondition condition) {
        ConfigurationSection section = condition.getSection();
        QuestContext context = condition.getContext();
        if (context == null) {
            return false;
        }
        ConfigurationSection selection = section.getConfigurationSection("item");
        String rawSelector = selection == null ? section.getString("item", "CONTEXT") : selection.getString("selector", "CONTEXT");
        String rawHolder = selection == null ? section.getString("item-holder", "TARGET") : selection.getString("holder", "TARGET");
        List<ItemStack> items = resolveItems(context, ItemSelector.parse(rawSelector, ItemSelector.CONTEXT), EntitySelector.parse(rawHolder, EntitySelector.TARGET));
        Player player = context.source() instanceof Player source ? source : context.player();
        ConfigurationSection rules = section.getConfigurationSection("match");
        ConfigurationSection actualRules = rules == null ? section : rules;
        return items.stream().anyMatch(item -> MatchItemManager.matchItemManager.getMatch(actualRules, player, item, context));
    }

    public static List<ItemStack> resolveItems(QuestContext context, ItemSelector selector, EntitySelector holderSelector) {
        if (selector == ItemSelector.COUNTED_ITEM) {
            return valid(context.triggerData() == null ? null : context.triggerData().extra(
                    cn.superiormc.ultimatequest.api.trigger.BuiltinContextKeys.COUNTED_ITEM).orElse(null));
        }
        if (selector == ItemSelector.CONTEXT) {
            return valid(context.item());
        }
        if (selector == ItemSelector.TRIGGER_ITEM) {
            return valid(context.triggerItem());
        }
        Entity holder = context.entity(holderSelector);
        if (!(holder instanceof LivingEntity living)) {
            return List.of();
        }
        EntityEquipment equipment = living.getEquipment();
        if (equipment == null) {
            return List.of();
        }
        return switch(selector) {
            case MAIN_HAND -> {
                    yield valid(equipment.getItemInMainHand());
                }
            case OFF_HAND -> {
                    yield valid(equipment.getItemInOffHand());
                }
            case HELMET -> {
                    yield valid(equipment.getHelmet());
                }
            case CHESTPLATE -> {
                    yield valid(equipment.getChestplate());
                }
            case LEGGINGS -> {
                    yield valid(equipment.getLeggings());
                }
            case BOOTS -> {
                    yield valid(equipment.getBoots());
                }
            case ARMOR -> {
                    yield valid(Arrays.asList(equipment.getArmorContents()));
                }
            case ALL_EQUIPMENT -> {
                    List<ItemStack> result = new ArrayList<>();
                    result.addAll(valid(equipment.getItemInMainHand()));
                    result.addAll(valid(equipment.getItemInOffHand()));
                    result.addAll(valid(Arrays.asList(equipment.getArmorContents())));
                    yield List.copyOf(result);
                }
            default -> {
                    yield List.of();
                }
        };
    }

    private static List<ItemStack> valid(ItemStack item) {
        return item == null || item.getType().isAir() ? List.of() : List.of(item);
    }

    private static List<ItemStack> valid(List<ItemStack> items) {
        return items.stream().filter(item -> item != null && !item.getType().isAir()).toList();
    }
}
