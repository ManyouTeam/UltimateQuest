# ✅ Event conditions

Conditions narrow the events that count, for example stone broken in survival mode instead of every block in every mode. Sibling objective conditions must all pass by default.

## Condition paths and example

This is a normal task objective fragment:

```yaml
source:
  trigger: block_break
  amount: 16
  conditions:
    survival:
      type: game_mode
      target: PLAYER
      modes: [SURVIVAL]
    stone:
      type: block_type
      types: [STONE]
```

`survival` and `stone` are entry names you choose; `type` selects behavior. The player must be in survival mode and the broken block must be stone. Both checks must pass.

## Acceptance checks and event checks

| Location | Checked when | Available data |
| --- | --- | --- |
| Normal task root `conditions` | The player attempts acceptance | Player data, without a broken block, fishing catch, or similar event data. |
| Objective `source.conditions` / template root `conditions` | Each trigger event | Player, entity, block, or item data supplied by that trigger. |

For the quest owner, explicitly set `target: PLAYER` on a condition that supports entity selection. `TARGET` can be a mob, projectile, player, or absent. `SOURCE` is not always a player either. Check the [trigger data](../triggers.md).

## Common fields

| Field | Default / requirement | Purpose |
| --- | --- | --- |
| `type` | Required | Registered condition ID. |
| `not` | `false` | Inverts this condition's result. |
| `target` | Depends on type | Read only by conditions supporting entity selection. |
| `meet-message` | Empty | Optional success message. |
| `not-meet-message` | Empty | Optional failure message. |

Unknown direct types reject definition loading. General permission, biome, and placeholder conditions belong inside `player_conditions`, not directly alongside event conditions.

Missing required data usually fails before `not` is applied. Inverting a missing-target check may therefore pass; inversion does not establish existence. Events passing objective conditions must also pass anti-abuse before progress is added.


There are 86 registered event-condition keys, including aliases. [Additional event conditions](extended.md) document movement states, event quantities, environment filters, and scoped runtime conditions.

## Condition type index

| Type | Reference |
| --- | --- |
| `any` | [any](logical.md#any) |
| `not` | [not](logical.md#not) |
| `sneaking` | [sneaking](entity-state.md#sneaking) |
| `not_sneaking` | [not_sneaking](entity-state.md#not_sneaking) |
| `on_ground` | [on_ground](entity-state.md#on_ground) |
| `in_air` | [in_air](entity-state.md#in_air) |
| `in_water` | [in_water](entity-state.md#in_water) |
| `in_lava` | [in_lava](entity-state.md#in_lava) |
| `game_mode` | [game_mode](entity-state.md#game_mode) |
| `has_potion` | [has_potion](entity-state.md#has_potion) |
| `match_entity` | [match_entity](entity-state.md#match_entity) |
| `match_item` | [match_item](items.md#match_item) |
| `catch_match_item` | [catch_match_item](items.md#catch_match_item) |
| `world` | [world](environment.md#world) |
| `block_type` | [block_type](environment.md#block_type) |
| `night` | [night](environment.md#night) |
| `storm` | [storm](environment.md#storm) |
| `player_conditions` | [player_conditions](player-conditions.md#player_conditions) |
| `item_damage` | [item_damage](extended.md#item_damage) |
| `blocking` | [blocking](extended.md#blocking) |
| `perfect_guard` | [perfect_guard](extended.md#perfect_guard) |
| `falling` | [falling](extended.md#falling) |
| `fall_distance` | [fall_distance](extended.md#fall_distance) |
| `sprinting` | [sprinting](extended.md#sprinting) |
| `swimming` | [swimming](extended.md#swimming) |
| `flying` | [flying](extended.md#flying) |
| `climbing` | [climbing](extended.md#climbing) |
| `riding` | [riding](extended.md#riding) |
| `potion_effect_type` | [potion_effect_type](extended.md#potion_effect_type) |
| `effect_type` | [effect_type](extended.md#effect_type) |
| `potion_effect_cause` | [potion_effect_cause](extended.md#potion_effect_cause) |
| `potion_effect_action` | [potion_effect_action](extended.md#potion_effect_action) |
| `in_sunlight` | [in_sunlight](extended.md#in_sunlight) |
| `thunder` | [thunder](extended.md#thunder) |
| `clear_weather` | [clear_weather](extended.md#clear_weather) |
| `in_rain` | [in_rain](extended.md#in_rain) |
| `interaction_action` | [interaction_action](extended.md#interaction_action) |
| `input_type` | [input_type](extended.md#input_type) |
| `event_state` | [event_state](extended.md#event_state) |
| `smash_attack_lands` | [smash_attack_lands](extended.md#smash_attack_lands) |
| `block_type_offset` | [block_type_offset](extended.md#block_type_offset) |
| `in_structure` | [in_structure](extended.md#in_structure) |
| `best_tool` | [best_tool](extended.md#best_tool) |
| `preferred_tool` | [preferred_tool](extended.md#preferred_tool) |
| `biome_changed` | [biome_changed](extended.md#biome_changed) |
| `liquid_transition` | [liquid_transition](extended.md#liquid_transition) |
| `block_break_time` | [block_break_time](extended.md#block_break_time) |
| `break_time` | [break_time](extended.md#break_time) |
| `ageable` | [ageable](extended.md#ageable) |
| `height` | [height](extended.md#height) |
| `damage_cause` | [damage_cause](extended.md#damage_cause) |
| `health_percent` | [health_percent](extended.md#health_percent) |
| `random` | [random](extended.md#random) |
| `first_attack_from_monster` | [first_attack_from_monster](extended.md#first_attack_from_monster) |
| `first_monster_attack` | [first_monster_attack](extended.md#first_monster_attack) |
| `target_category` | [target_category](extended.md#target_category) |
| `food_change` | [food_change](extended.md#food_change) |
| `air_change` | [air_change](extended.md#air_change) |
| `liquid_surface_distance` | [liquid_surface_distance](extended.md#liquid_surface_distance) |
| `headshot` | [headshot](extended.md#headshot) |
| `fatal_damage` | [fatal_damage](extended.md#fatal_damage) |
| `distance` | [distance](extended.md#distance) |
| `bounding_box_distance` | [bounding_box_distance](extended.md#bounding_box_distance) |
| `environment` | [environment](extended.md#environment) |
| `gliding` | [gliding](extended.md#gliding) |
| `light_level` | [light_level](extended.md#light_level) |
| `stationary` | [stationary](extended.md#stationary) |
| `health` | [health](extended.md#health) |
| `food_level` | [food_level](extended.md#food_level) |
| `damage_value` | [damage_value](extended.md#damage_value) |
| `first_attack_against_entity` | [first_attack_against_entity](extended.md#first_attack_against_entity) |
| `combust_duration` | [combust_duration](extended.md#combust_duration) |
| `regain_amount` | [regain_amount](extended.md#regain_amount) |
| `explosion_yield` | [explosion_yield](extended.md#explosion_yield) |
| `explosion_radius` | [explosion_radius](extended.md#explosion_radius) |
| `damage_origin` | [damage_origin](extended.md#damage_origin) |
| `combust_origin` | [combust_origin](extended.md#combust_origin) |
| `target_reason` | [target_reason](extended.md#target_reason) |
| `target_count` | [target_count](extended.md#target_count) |
| `spawn_reason` | [spawn_reason](extended.md#spawn_reason) |
| `knockback_cause` | [knockback_cause](extended.md#knockback_cause) |
| `knockback_reason` | [knockback_reason](extended.md#knockback_reason) |
| `exhaustion_reason` | [exhaustion_reason](extended.md#exhaustion_reason) |
| `villager_trade` | [villager_trade](extended.md#villager_trade) |
| `cooldown` | [cooldown](extended.md#cooldown) |
| `cooldown_material` | [cooldown_material](extended.md#cooldown_material) |

See [logical conditions](logical.md) for alternatives and inversion. After editing, test both events that should pass and ones that should fail.
