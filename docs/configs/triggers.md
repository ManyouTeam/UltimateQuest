# ⚡ Task triggers

A trigger decides when progress should be considered. For example, `block_break` observes broken blocks, `kill` observes kills, and `consume` observes item consumption. Conditions decide whether that particular event counts.

## Objective configuration example

This is an objective fragment for a [normal task](tasks.md) root, not a complete standalone file.

```yaml
source:
  name: 'Break stone'
  trigger: block_break
  amount: 16
  conditions:
    stone:
      type: block_type
      types: [STONE]
```

`trigger: block_break` selects the block-break event, `amount: 16` requires sixteen matches, and `conditions` restricts them to stone. Without a material condition, other blocks supported by that event can count too.

Use the lowercase IDs in the reference pages. Namespaced forms such as `ultimatequest:block_break` are also accepted.

## Progress counting rules

The default `count: events` adds one per matching event. Some triggers also support item or block quantities, damage, XP points, distance, and other units. Each reference entry lists its supported `count` values. See [counting modes](counting.md) for rules and examples. Unsupported combinations are rejected during loading.

Cancelled events are ignored by the ordinary event route. Normal tasks need acceptance; scrolls need successful activation and carrying. Also check conditions, activity windows, [anti-abuse](anti-abuse.md), and [task limits](task-limits.md) when progress does not increase.

## Event entities and available data

`PLAYER` is the task player receiving progress. `SOURCE` and `TARGET` depend on the event: TARGET is usually the victim for a kill, but the injured player for received damage. Some events have no target, item, or block to match.

Each reference entry lists available data and platform requirements. Triggers needing Paper or a particular event class cannot be used on unsupported platforms. Do not infer support from the ID alone.

## Trigger index
* [Tick and lifecycle](triggers/lifecycle.md): `tick`, `spawn`, `respawn`.
* [Combat and damage](triggers/combat.md): `attack`, `melee_attack`, `damage`, `damage_by_entity`, `kill`, `death`, `regain`, `shield_block`, `shield_disable`, `attempt_smash_attack`, `knockback`, `combust`, `creeper_explode`.
* [Projectiles and ranged attacks](triggers/projectiles.md): `projectile_launch`, `projectile_hit`, `projectile_tick`, `shoot`, `shoot_bow`, `load_crossbow`.
* [Blocks and interactions](triggers/blocks-interactions.md): `block_break`, `block_damage`, `block_drop_item`, `block_place`, `block_break_progress_update`, `interact`, `inside_block`, `vibration_receive`.
* [Items and equipment](triggers/items-equipment.md): `consume`, `enchant_item`, `item_damage`, `item_held`, `swap_hand`, `item_group_cooldown`, `name_entity`.
* [Movement and input](triggers/movement-input.md): `move`, `input`, `toggle_sneak`, `toggle_flight`, `riptide`, `jump`, `elytra_boost`, `lunge`.
* [Player state](triggers/player-state.md): `air_change`, `food_level_change`, `exp_change`, `potion_effect`, `effect_tick`, `exhaustion`, `pickup_experience`.
* [Fishing and trading](triggers/fishing-trading.md): `fish`, `piglin_barter`, `trade`, `purchase`.
* [Targeting and special entities](triggers/targeting-special.md): `target`, `untag`, `target_tick`, `enderman_attack_player`, `phantom_pre_spawn`, `warden_anger_change`.

See [event conditions](conditions/README.md) for filtering and [objective templates](objectives.md) for reuse.
