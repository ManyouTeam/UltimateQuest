package cn.superiormc.ultimatequest.managers;

import cn.superiormc.ultimatequest.api.registry.KeyedRegistry;
import cn.superiormc.ultimatequest.objects.ObjectSingleCondition;
import cn.superiormc.ultimatequest.objects.QuestContext;
import cn.superiormc.ultimatequest.objects.eventconditions.AbstractEventCondition;
import cn.superiormc.ultimatequest.objects.eventconditions.EventConditionAny;
import cn.superiormc.ultimatequest.objects.eventconditions.EventConditionBlockType;
import cn.superiormc.ultimatequest.objects.eventconditions.EventConditionCatchMatchItem;
import cn.superiormc.ultimatequest.objects.eventconditions.EventConditionGameMode;
import cn.superiormc.ultimatequest.objects.eventconditions.EventConditionHasPotion;
import cn.superiormc.ultimatequest.objects.eventconditions.EventConditionInAir;
import cn.superiormc.ultimatequest.objects.eventconditions.EventConditionInLava;
import cn.superiormc.ultimatequest.objects.eventconditions.EventConditionInWater;
import cn.superiormc.ultimatequest.objects.eventconditions.EventConditionMatchEntity;
import cn.superiormc.ultimatequest.objects.eventconditions.EventConditionMatchItem;
import cn.superiormc.ultimatequest.objects.eventconditions.EventConditionNight;
import cn.superiormc.ultimatequest.objects.eventconditions.EventConditionNot;
import cn.superiormc.ultimatequest.objects.eventconditions.EventConditionNotSneaking;
import cn.superiormc.ultimatequest.objects.eventconditions.EventConditionOnGround;
import cn.superiormc.ultimatequest.objects.eventconditions.EventConditionPlayerConditions;
import cn.superiormc.ultimatequest.objects.eventconditions.EventConditionSneaking;
import cn.superiormc.ultimatequest.objects.eventconditions.EventConditionStorm;
import cn.superiormc.ultimatequest.objects.eventconditions.EventConditionWorld;
import cn.superiormc.ultimatequest.objects.eventconditions.EventConditionAgeable;
import cn.superiormc.ultimatequest.objects.eventconditions.EventConditionAirChange;
import cn.superiormc.ultimatequest.objects.eventconditions.EventConditionBestTool;
import cn.superiormc.ultimatequest.objects.eventconditions.EventConditionBiomeChanged;
import cn.superiormc.ultimatequest.objects.eventconditions.EventConditionBlockBreakTime;
import cn.superiormc.ultimatequest.objects.eventconditions.EventConditionBlocking;
import cn.superiormc.ultimatequest.objects.eventconditions.EventConditionBlockTypeOffset;
import cn.superiormc.ultimatequest.objects.eventconditions.EventConditionBoundingBoxDistance;
import cn.superiormc.ultimatequest.objects.eventconditions.EventConditionClearWeather;
import cn.superiormc.ultimatequest.objects.eventconditions.EventConditionClimbing;
import cn.superiormc.ultimatequest.objects.eventconditions.EventConditionCombustDuration;
import cn.superiormc.ultimatequest.objects.eventconditions.EventConditionCombustOrigin;
import cn.superiormc.ultimatequest.objects.eventconditions.EventConditionCooldown;
import cn.superiormc.ultimatequest.objects.eventconditions.EventConditionCooldownMaterial;
import cn.superiormc.ultimatequest.objects.eventconditions.EventConditionDamageCause;
import cn.superiormc.ultimatequest.objects.eventconditions.EventConditionDamageOrigin;
import cn.superiormc.ultimatequest.objects.eventconditions.EventConditionDamageValue;
import cn.superiormc.ultimatequest.objects.eventconditions.EventConditionDistance;
import cn.superiormc.ultimatequest.objects.eventconditions.EventConditionEnvironment;
import cn.superiormc.ultimatequest.objects.eventconditions.EventConditionEventState;
import cn.superiormc.ultimatequest.objects.eventconditions.EventConditionExhaustionReason;
import cn.superiormc.ultimatequest.objects.eventconditions.EventConditionExplosionRadius;
import cn.superiormc.ultimatequest.objects.eventconditions.EventConditionExplosionYield;
import cn.superiormc.ultimatequest.objects.eventconditions.EventConditionFallDistance;
import cn.superiormc.ultimatequest.objects.eventconditions.EventConditionFalling;
import cn.superiormc.ultimatequest.objects.eventconditions.EventConditionFatalDamage;
import cn.superiormc.ultimatequest.objects.eventconditions.EventConditionFirstAttackAgainstEntity;
import cn.superiormc.ultimatequest.objects.eventconditions.EventConditionFirstAttackFromMonster;
import cn.superiormc.ultimatequest.objects.eventconditions.EventConditionFlying;
import cn.superiormc.ultimatequest.objects.eventconditions.EventConditionFoodChange;
import cn.superiormc.ultimatequest.objects.eventconditions.EventConditionFoodLevel;
import cn.superiormc.ultimatequest.objects.eventconditions.EventConditionGliding;
import cn.superiormc.ultimatequest.objects.eventconditions.EventConditionHeadshot;
import cn.superiormc.ultimatequest.objects.eventconditions.EventConditionHealth;
import cn.superiormc.ultimatequest.objects.eventconditions.EventConditionHealthPercent;
import cn.superiormc.ultimatequest.objects.eventconditions.EventConditionHeight;
import cn.superiormc.ultimatequest.objects.eventconditions.EventConditionInputType;
import cn.superiormc.ultimatequest.objects.eventconditions.EventConditionInRain;
import cn.superiormc.ultimatequest.objects.eventconditions.EventConditionInStructure;
import cn.superiormc.ultimatequest.objects.eventconditions.EventConditionInSunlight;
import cn.superiormc.ultimatequest.objects.eventconditions.EventConditionInteractionAction;
import cn.superiormc.ultimatequest.objects.eventconditions.EventConditionItemDamage;
import cn.superiormc.ultimatequest.objects.eventconditions.EventConditionKnockbackCause;
import cn.superiormc.ultimatequest.objects.eventconditions.EventConditionLightLevel;
import cn.superiormc.ultimatequest.objects.eventconditions.EventConditionLiquidSurfaceDistance;
import cn.superiormc.ultimatequest.objects.eventconditions.EventConditionLiquidTransition;
import cn.superiormc.ultimatequest.objects.eventconditions.EventConditionMinAttackCooldown;
import cn.superiormc.ultimatequest.objects.eventconditions.EventConditionPerfectGuard;
import cn.superiormc.ultimatequest.objects.eventconditions.EventConditionPotionEffectAction;
import cn.superiormc.ultimatequest.objects.eventconditions.EventConditionPotionEffectCause;
import cn.superiormc.ultimatequest.objects.eventconditions.EventConditionPotionEffectType;
import cn.superiormc.ultimatequest.objects.eventconditions.EventConditionRandom;
import cn.superiormc.ultimatequest.objects.eventconditions.EventConditionRegainAmount;
import cn.superiormc.ultimatequest.objects.eventconditions.EventConditionRiding;
import cn.superiormc.ultimatequest.objects.eventconditions.EventConditionSmashAttackLands;
import cn.superiormc.ultimatequest.objects.eventconditions.EventConditionSpawnReason;
import cn.superiormc.ultimatequest.objects.eventconditions.EventConditionSprinting;
import cn.superiormc.ultimatequest.objects.eventconditions.EventConditionStationary;
import cn.superiormc.ultimatequest.objects.eventconditions.EventConditionSwimming;
import cn.superiormc.ultimatequest.objects.eventconditions.EventConditionTargetCategory;
import cn.superiormc.ultimatequest.objects.eventconditions.EventConditionTargetCount;
import cn.superiormc.ultimatequest.objects.eventconditions.EventConditionTargetReason;
import cn.superiormc.ultimatequest.objects.eventconditions.EventConditionThunder;
import cn.superiormc.ultimatequest.objects.eventconditions.EventConditionVillagerTrade;
import java.util.UUID;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public final class EventConditionsManager extends AbstractManager {

    public static EventConditionsManager eventConditions;

    private final KeyedRegistry<String, AbstractEventCondition> conditions = KeyedRegistry.stringTypes();

    private final ConditionManager<ObjectSingleCondition, QuestContext> playerConditions = new ConditionManager<>();

    public EventConditionsManager() {
        eventConditions = this;
        registerBuiltIns();
    }

    private void registerBuiltIns() {
        register(new EventConditionAny());
        register(new EventConditionBlockType());
        register(new EventConditionCatchMatchItem());
        register(new EventConditionGameMode());
        register(new EventConditionHasPotion());
        register(new EventConditionInAir());
        register(new EventConditionInLava());
        register(new EventConditionInWater());
        register(new EventConditionMatchEntity());
        register(new EventConditionMatchItem());
        register(new EventConditionNight());
        register(new EventConditionNot());
        register(new EventConditionNotSneaking());
        register(new EventConditionOnGround());
        register(new EventConditionPlayerConditions());
        register(new EventConditionSneaking());
        register(new EventConditionStorm());
        register(new EventConditionWorld());
        register(new EventConditionItemDamage());
        register(new EventConditionMinAttackCooldown());
        register(new EventConditionBlocking());
        register(new EventConditionPerfectGuard());
        register(new EventConditionFalling());
        register(new EventConditionFallDistance());
        register(new EventConditionSprinting());
        register(new EventConditionSwimming());
        register(new EventConditionFlying());
        register(new EventConditionClimbing());
        register(new EventConditionRiding());
        register(new EventConditionPotionEffectType());
        register(new EventConditionPotionEffectType("effect_type"));
        register(new EventConditionPotionEffectCause());
        register(new EventConditionPotionEffectAction());
        register(new EventConditionInSunlight());
        register(new EventConditionThunder());
        register(new EventConditionClearWeather());
        register(new EventConditionInRain());
        register(new EventConditionInteractionAction());
        register(new EventConditionInputType());
        register(new EventConditionEventState());
        register(new EventConditionSmashAttackLands());
        register(new EventConditionBlockTypeOffset());
        register(new EventConditionInStructure());
        register(new EventConditionBestTool());
        register(new EventConditionBestTool("preferred_tool"));
        register(new EventConditionBiomeChanged());
        register(new EventConditionLiquidTransition());
        register(new EventConditionBlockBreakTime());
        register(new EventConditionBlockBreakTime("break_time"));
        register(new EventConditionAgeable());
        register(new EventConditionHeight());
        register(new EventConditionDamageCause());
        register(new EventConditionHealthPercent());
        register(new EventConditionRandom());
        register(new EventConditionFirstAttackFromMonster());
        register(new EventConditionFirstAttackFromMonster("first_monster_attack"));
        register(new EventConditionTargetCategory());
        register(new EventConditionFoodChange());
        register(new EventConditionAirChange());
        register(new EventConditionLiquidSurfaceDistance());
        register(new EventConditionHeadshot());
        register(new EventConditionFatalDamage());
        register(new EventConditionDistance());
        register(new EventConditionBoundingBoxDistance());
        register(new EventConditionEnvironment());
        register(new EventConditionGliding());
        register(new EventConditionLightLevel());
        register(new EventConditionStationary());
        register(new EventConditionHealth());
        register(new EventConditionFoodLevel());
        register(new EventConditionDamageValue());
        register(new EventConditionFirstAttackAgainstEntity());
        register(new EventConditionCombustDuration());
        register(new EventConditionRegainAmount());
        register(new EventConditionExplosionYield());
        register(new EventConditionExplosionRadius());
        register(new EventConditionDamageOrigin());
        register(new EventConditionCombustOrigin());
        register(new EventConditionTargetReason());
        register(new EventConditionTargetCount());
        register(new EventConditionSpawnReason());
        register(new EventConditionKnockbackCause());
        register(new EventConditionKnockbackCause("knockback_reason"));
        register(new EventConditionExhaustionReason());
        register(new EventConditionVillagerTrade());
        register(new EventConditionCooldown());
        register(new EventConditionCooldownMaterial());
    }

    public void register(AbstractEventCondition condition) {
        conditions.register(condition.getType(), condition);
    }

    public void onUnload() {
        conditions.values().forEach(AbstractEventCondition::onUnload);
    }

    @Override
    public void onInit() {
        conditions.values().forEach(AbstractEventCondition::onInit);
    }

    @Override
    public void onPluginReload() {
        onUnload();
        onInit();
    }

    @Override
    public void onPluginDisable() {
        onUnload();
        conditions.clear();
        eventConditions = null;
    }

    public void onEntityUnload(UUID entityId) {
        if (entityId != null) {
            conditions.values().forEach(condition -> condition.onEntityUnload(entityId));
        }
    }

    public ConditionManager<ObjectSingleCondition, QuestContext> getPlayerConditions() {
        return playerConditions;
    }

    public void validate(ConfigurationSection section) {
        if (section == null) {
            return;
        }
        for (String key : section.getKeys(false)) {
            ConfigurationSection single = section.getConfigurationSection(key);
            String configuredType = single == null ? null : single.getString("type");
            AbstractEventCondition condition = configuredType == null ? null : conditions.get(configuredType);
            if (condition == null) {
                throw new IllegalArgumentException("Invalid event condition type '"
                        + (configuredType == null ? "<missing>" : configuredType) + "' at "
                        + section.getCurrentPath() + "." + key + ".type");
            }
            String type = condition.getType();
            if (type.equals("any") || type.equals("not")) {
                validate(single.getConfigurationSection("conditions"));
            }
        }
    }

    public boolean matches(ConfigurationSection section, QuestContext context) {
        return matches(section, context, null);
    }

    public boolean matches(ConfigurationSection section, QuestContext context, Player player) {
        QuestContext playerContext = context == null ? null : context.withPlayer(resolvePlayer(context, player));
        if (section == null) {
            return true;
        }
        for (String key : section.getKeys(false)) {
            ConfigurationSection single = section.getConfigurationSection(key);
            if (single == null || !matchesSingle(single, playerContext)) {
                return false;
            }
        }
        return true;
    }

    public boolean matchesAny(ConfigurationSection section, QuestContext context, Player player) {
        QuestContext playerContext = context == null ? null : context.withPlayer(resolvePlayer(context, player));
        if (section == null) {
            return true;
        }
        for (String key : section.getKeys(false)) {
            ConfigurationSection single = section.getConfigurationSection(key);
            if (single != null && matchesSingle(single, playerContext)) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesSingle(ConfigurationSection section, QuestContext context) {
        String type = section.getString("type");
        AbstractEventCondition condition = type == null ? null : conditions.get(type);
        if (condition == null) {
            ErrorManager.errorManager.sendErrorMessage("§cError: Invalid event condition type '"
                    + (type == null ? "<missing>" : type) + "' at " + section.getCurrentPath() + ".type");
            return false;
        }
        return condition.matches(new ObjectSingleCondition(section, context));
    }

    private Player resolvePlayer(QuestContext context, Player explicitPlayer) {
        if (explicitPlayer != null) {
            return explicitPlayer;
        }
        if (context != null && context.player() != null) {
            return context.player();
        }
        Entity source = context == null ? null : context.source();
        return source instanceof Player sourcePlayer ? sourcePlayer : null;
    }
}
