# Additional event conditions

These conditions use the same IDs and fields as the applicable ER conditions. Put them in an objective’s `source.conditions` or a reusable objective’s `conditions`. Common `not` and result-message fields still apply. UQ selectors are `PLAYER`, `SOURCE`, and `TARGET`; event values are read from the observed event rather than an ER modifier result. `skill_level`, `state_value`, and `attribute_value` depend on ER systems and are not registered in UQ.

Numeric conditions support `value`, `compare` (`>`, `>=`, `<`, `<=`, `=`, `!=` and their ER aliases), plus inclusive `min`/`max`. `value` defaults to the current quantity and `compare` defaults to `>=`. Missing event or entity data does not satisfy the condition before common inversion. The `damage_value` condition checks the event damage before armor; `count: damage` counts final damage.

Stateful conditions are scoped by their task configuration. Entity unload, player quit, reload, and disable clear their state; state is not persisted. First-attack checks are idempotent for the same event and reset when either entity unloads. `stationary` tracks an entity’s position per condition type. `perfect_guard` listens for shield raising. `in_rain`, saved spawn-reason fallback, and Paper event-only conditions require the corresponding server API; missing APIs produce a non-match. `headshot` uses the projectile from the damage or projectile-hit event; `fatal_damage` checks the damaged entity.


## `item_damage`

Checks the current or projected durability damage of a selected damageable item.

Supports the common numeric comparison fields described above.

| Field | Default | Description |
| --- | --- | --- |
| `item` | `TRIGGER_ITEM` | Item selector string, or nested section containing `selector` and `holder`. |
| `item-holder` | `PLAYER` | Holder selector when using the flat item form. |
| `mode` | `DAMAGE` | Value derived from the selected item's durability state. See the mode table below. |
| `include-event-damage` | `false` | During `PlayerItemDamageEvent`, include the event durability damage that the current event will apply. |
| `min / max` | unbounded | Inclusive numeric range. |
| `compare / value` | unset | Alternative numeric comparison such as `>=`, `<`, or `=`. |

## `blocking`

Checks whether a selected living entity is actively blocking with a shield.

| Field | Default | Description |
| --- | --- | --- |
| `target` | `TARGET` | Living-entity selector. |
| `value` | `true` | Expected blocking state. |

## `perfect_guard`

Matches when a player blocks within a short window after raising a shield.

| Field | Default | Description |
| --- | --- | --- |
| `target` | `TARGET` | Player selector. |
| `window-millis` | `120` | Maximum elapsed milliseconds since the shield was raised. Supports placeholders and expressions. |
| `consume` | `true` | Removes the recorded raise after a successful match, allowing only one perfect guard per raise. |

## `falling`

Checks whether an airborne entity is moving downward.

| Field | Default | Description |
| --- | --- | --- |
| `target` | `TARGET` | Entity selector. |
| `maximum-y` | `0` | The entity's Y velocity must be lower than this value. |
| `value` | `true` | Expected falling state. |

## `fall_distance`

Compares an entity's accumulated Bukkit fall distance.

Supports the common numeric comparison fields described above.

| Field | Default | Description |
| --- | --- | --- |
| `target` | `TARGET` | Entity selector. |
| `value` / `compare` | unset / `>=` | Optional direct numeric comparison. |
| `min` | unlimited | Inclusive minimum fall distance. |
| `max` | unlimited | Inclusive maximum fall distance. |

## `sprinting`

Checks whether a selected player is sprinting.

| Field | Default | Description |
| --- | --- | --- |
| `target` | `TARGET` | Player selector. |
| `value` | `true` | Expected sprinting state. |

## `swimming`

Checks whether a selected player is swimming.

| Field | Default | Description |
| --- | --- | --- |
| `target` | `TARGET` | Player selector. |
| `value` | `true` | Expected swimming state. |

## `flying`

Checks whether a selected player is flying.

| Field | Default | Description |
| --- | --- | --- |
| `target` | `TARGET` | Player selector. |
| `value` | `true` | Expected flying state. |

## `climbing`

Checks climbing.

Fields: `target`, `value`.

## `riding`

Checks riding.

Fields: `target`, `value`.

## `potion_effect_type`

Matches the potion-effect type supplied by an effect event.

| Field | Default | Description |
| --- | --- | --- |
| `selector / selectors` | empty | Exact effect selectors. |
| `effect / effects` | empty | Aliases for effect selectors. |
| `type / types` | empty | Additional selector aliases. |
| `category / categories` | empty | Effect categories supported by `PotionEffectSelector`. |

## `effect_type`

Alias of [`potion_effect_type`](#potion_effect_type)。

## `potion_effect_cause`

Matches the cause of a Bukkit potion-effect change event.

| Field | Default | Description |
| --- | --- | --- |
| `cause / causes` | empty | Accepted `EntityPotionEffectEvent.Cause` name(s), such as `FOOD`, `POTION_DRINK`, or `ATTACK`. |

## `potion_effect_action`

Matches the action of a Bukkit potion-effect change event.

| Field | Default | Description |
| --- | --- | --- |
| `action / actions` | empty | Accepted actions, such as `ADDED`, `CHANGED`, `REMOVED`, or `CLEARED`. |

## `in_sunlight`

Checks a simplified daylight and sky-light exposure rule.

| Field | Default | Description |
| --- | --- | --- |
| `target` | `TARGET` | Entity selector. |
| `value` | `true` | Expected state. |

## `thunder`

Checks whether the context world is thundering.

| Field | Default | Description |
| --- | --- | --- |
| `value` | `true` | Expected thundering state. |

## `clear_weather`

Checks whether the context world has no storm.

| Field | Default | Description |
| --- | --- | --- |
| `value` | `true` | Expected clear-weather state. |

## `in_rain`

Checks Bukkit's `Entity#isInRain` state.

| Field | Default | Description |
| --- | --- | --- |
| `target` | `TARGET` | Entity selector. |
| `value` | `true` | Expected state. |

## `interaction_action`

Matches a `PlayerInteractEvent.Action`.

| Field | Default | Description |
| --- | --- | --- |
| `action` | empty | Single action enum name. |
| `actions` | empty | Accepted action list. |

## `input_type`

Matches active, newly pressed, or released Paper player-input controls.

| Field | Default | Description |
| --- | --- | --- |
| `input` | empty | Single input: `FORWARD`, `BACKWARD`, `LEFT`, `RIGHT`, `JUMP`, `SNEAK`, or `SPRINT`. |
| `inputs` | empty | Accepted input list. |
| `state` | `ACTIVE` | `ACTIVE`, `PRESSED`, or `RELEASED`. |

## `event_state`

Matches a `PlayerFishEvent.State`.

| Field | Default | Description |
| --- | --- | --- |
| `state` | empty | Single fishing state. |
| `states` | empty | Accepted fishing-state list. |

## `smash_attack_lands`

Checks whether a Paper mace smash attempt will land successfully.

| Field | Default | Description |
| --- | --- | --- |
| `value` | `true` | Expected successful-smash state. |

## `block_type_offset`

Matches a block at an offset from the context block.

| Field | Default | Description |
| --- | --- | --- |
| `type-name` | empty | Single block specification. |
| `types` | empty | Accepted block specifications. |
| `offset.x / offset.y / offset.z` | `0` | Relative block coordinates. |

## `in_structure`

Checks whether a location lies inside one of the configured generated-structure bounding boxes.

| Field | Default | Description |
| --- | --- | --- |
| `target` | `TARGET` | Entity selector. |
| `structure` | empty | Single namespaced structure key. |
| `structures` | empty | List of accepted structure keys. |

## `best_tool`

Checks Bukkit's preferred-tool result for the context block and a player's held item.

| Field | Default | Description |
| --- | --- | --- |
| `target` | `SOURCE` | Player selector. |
| `hand` | `MAIN_HAND` | `MAIN_HAND` or `OFF_HAND`. |
| `value` | `true` | Expected preferred-tool result. |

## `preferred_tool`

Alias of [`best_tool`](#best_tool)。

## `biome_changed`

Checks whether a player movement event crossed a biome boundary.

_No type-specific fields._

## `liquid_transition`

Checks whether a player has just entered or left a configured liquid while moving.

| Field | Default | Description |
| --- | --- | --- |
| `liquid` | `ANY` | Accepts `ANY`, `WATER`, or `LAVA`. |
| `transition` | `ENTER` | Accepts `ENTER` or `EXIT`; `LEAVE` aliases `EXIT`. |

## `block_break_time`

Calculates the number of ticks/time required for a player to break the context block.

Supports the common numeric comparison fields described above.

| Field | Default | Description |
| --- | --- | --- |
| `target` | `SOURCE` | Player selector. |
| `unit` | `TICKS` | `TICKS`, `SECONDS`, or `MILLISECONDS`. |
| `min / max` | unbounded | Inclusive time range. |
| `compare / value` | unset | Alternative numeric comparison. |

## `break_time`

Alias of [`block_break_time`](#block_break_time)。

## `ageable`

Checks whether the context block's ageable data is fully grown.

| Field | Default | Description |
| --- | --- | --- |
| `value` | `true` | Expected fully-grown state. |

## `height`

Checks the block Y coordinate of the resolved trigger location.

| Field | Default | Description |
| --- | --- | --- |
| `min` | minimum integer | Inclusive minimum Y. |
| `max` | maximum integer | Inclusive maximum Y. |

## `damage_cause`

Matches the captured Bukkit `EntityDamageEvent.DamageCause`.

| Field | Default | Description |
| --- | --- | --- |
| `value` | empty | Single damage-cause enum name. |
| `values` | empty | Accepted cause names. |

## `health_percent`

Checks current health as a percentage of maximum health.

| Field | Default | Description |
| --- | --- | --- |
| `target` | `SOURCE` | Entity selector. |
| `min` | `0` | Inclusive minimum percent. |
| `max` | `100` | Inclusive maximum percent. |
| `min-max-health` | `0` | Inclusive minimum maximum-health value. |
| `max-max-health` | unbounded | Inclusive maximum maximum-health value. |

## `random`

Performs a fresh probability roll.

| Field | Default | Description |
| --- | --- | --- |
| `value` | `1` | Probability from `0` to `1`; values are clamped to that range. |

## `first_attack_from_monster`

Matches the first tracked attack received from a specific monster for this condition path.

_No type-specific fields._

## `first_monster_attack`

Alias of [`first_attack_from_monster`](#first_attack_from_monster)。

## `target_category`

Matches the fixed category of `TARGET`.

| Field | Default | Description |
| --- | --- | --- |
| `category` | `ANIMAL` | Supported values: `MONSTER`, `ANIMAL`, or `UNDEAD`. |

## `food_change`

Checks the pending food-level delta.

Supports the common numeric comparison fields described above.

| Field | Default | Description |
| --- | --- | --- |
| `min / max` | unbounded | Inclusive delta range; positive increases food and negative decreases it. |
| `compare / value` | unset | Alternative numeric comparison. |

## `air_change`

Checks the pending remaining-air delta.

Supports the common numeric comparison fields described above.

| Field | Default | Description |
| --- | --- | --- |
| `min / max` | unbounded | Inclusive delta range; positive increases air and negative decreases it. |
| `compare / value` | unset | Alternative numeric comparison. |

## `liquid_surface_distance`

Checks the vertical distance from the selected entity's eyes to the liquid surface directly above.

Supports the common numeric comparison fields described above.

| Field | Default | Description |
| --- | --- | --- |
| `target` | `PLAYER` | Entity selector. Living entities use eye position; other entities use their own location. |
| `liquid` | `ANY` | Accepts `ANY`, `WATER`, or `LAVA`. |
| `min / max` | unbounded | Vertical distance range to the surface, in blocks. |
| `compare / value` | unset | Alternative numeric comparison. |
| `scan-limit` | world height limit | Maximum upward scan distance; supports math expressions and variables. |

## `headshot`

Checks the projectile from the damage or projectile-hit event against the living target’s eye height.

| Field | Default | Description |
| --- | --- | --- |
| `tolerance` | `0.45` | Maximum absolute Y difference between projectile location and target eye location. |

## `fatal_damage`

Checks whether final event damage reaches the damaged entity’s remaining health.

_No type-specific fields._

## `distance`

Checks Euclidean distance between two context entities.

Supports the common numeric comparison fields described above.

| Field | Default | Description |
| --- | --- | --- |
| `source` | `SOURCE` | First entity selector. |
| `target` | `TARGET` | Second entity selector. |
| `min / max` | unbounded | Inclusive distance range. |
| `compare / value` | unset | Alternative numeric comparison. |

## `bounding_box_distance`

Checks the shortest distance from one entity's position to another entity's bounding box.

Supports the common numeric comparison fields described above.

| Field | Default | Description |
| --- | --- | --- |
| `source` | `SOURCE` | Entity whose position is used as the measurement point. |
| `target` | `TARGET` | Entity whose bounding box is measured. |
| `source-eye` | `false` | Use the source living entity's eye position instead of its feet position. |
| `min / max` | unbounded | Inclusive shortest-distance range. |
| `compare / value` | unset | Alternative numeric comparison. |

## `environment`

Matches a world dimension and optionally a biome.

| Field | Default | Description |
| --- | --- | --- |
| `target` | `SOURCE` | Entity selector. |
| `dimension` | empty | Bukkit world environment such as `NORMAL`, `NETHER`, or `THE_END`. |
| `biomes` | empty | Accepted biome enum names or namespaced keys. |

## `gliding`

Checks a living entity's gliding state.

| Field | Default | Description |
| --- | --- | --- |
| `target` | `TARGET` | Entity selector. |
| `value` | `true` | Expected state. |

## `light_level`

Checks total block light.

| Field | Default | Description |
| --- | --- | --- |
| `min` | `0` | Inclusive minimum light level. |
| `max` | `15` | Inclusive maximum light level. |

## `stationary`

Checks whether a selected entity has remained within a small movement tolerance for a duration.

| Field | Default | Description |
| --- | --- | --- |
| `target` | `TARGET` | Entity selector. |
| `seconds` | `3` | Required stationary duration in seconds. |

## `health`

Checks current or maximum health with the shared numeric-comparison syntax.

Supports the common numeric comparison fields described above.

| Field | Default | Description |
| --- | --- | --- |
| `target` | `PLAYER` | Entity selector. |
| `mode` | `CURRENT` | `CURRENT`, `MAX`, or `MAXIMUM`. |
| `min / max` | unbounded | Inclusive numeric range. |
| `compare / value` | unset | Alternative comparison such as `>=`, `<`, or `=`. |

## `food_level`

Checks a player's current food level.

Supports the common numeric comparison fields described above.

| Field | Default | Description |
| --- | --- | --- |
| `target` | `PLAYER` | Entity selector. |
| `min / max` | unbounded | Inclusive range. |
| `compare / value` | unset | Alternative numeric comparison. |

## `damage_value`

Checks raw damage from the current event, before armor and other final-damage modifiers.

Supports the common numeric comparison fields described above.

| Field | Default | Description |
| --- | --- | --- |
| `min / max` | unbounded | Inclusive numeric range. |
| `compare / value` | unset | Alternative numeric comparison. |

## `first_attack_against_entity`

Matches only the first qualifying source-player/target pair for this condition path.

_No type-specific fields._

## `combust_duration`

Checks the current combustion-duration result.

Supports the common numeric comparison fields described above.

| Field | Default | Description |
| --- | --- | --- |
| `min / max` | unbounded | Inclusive numeric range. |
| `compare / value` | unset | Alternative numeric comparison. |

## `regain_amount`

Checks the current health-regain result.

Supports the common numeric comparison fields described above.

| Field | Default | Description |
| --- | --- | --- |
| `min / max` | unbounded | Inclusive numeric range. |
| `compare / value` | unset | Alternative numeric comparison. |

## `explosion_yield`

Checks the current explosion-yield result.

Supports the common numeric comparison fields described above.

| Field | Default | Description |
| --- | --- | --- |
| `min / max` | unbounded | Inclusive numeric range. |
| `compare / value` | unset | Alternative numeric comparison. |

## `explosion_radius`

Checks the current primed-explosion radius.

Supports the common numeric comparison fields described above.

| Field | Default | Description |
| --- | --- | --- |
| `min / max` | unbounded | Inclusive numeric range. |
| `compare / value` | unset | Alternative numeric comparison. |

## `damage_origin`

Classifies damage by its captured origin.

| Field | Default | Description |
| --- | --- | --- |
| `value` | `ANY` | `ANY`, `ENTITY`, `BLOCK`, or `OTHER`. |

## `combust_origin`

Matches whether combustion was caused by an entity, block, or other source.

| Field | Default | Description |
| --- | --- | --- |
| `origin` | empty | Single value: `ENTITY`, `BLOCK`, or `OTHER`. |
| `origins` | empty | Accepted origin list. |

## `target_reason`

Matches the captured Bukkit entity-target reason.

| Field | Default | Description |
| --- | --- | --- |
| `value` | empty | Single reason enum name. |
| `values` | empty | Accepted reason list. |

## `target_count`

Checks target count.

Supports the common numeric comparison fields described above.

## `spawn_reason`

Matches an entity's Bukkit spawn reason.

| Field | Default | Description |
| --- | --- | --- |
| `target` | `TARGET` | Entity selector. |
| `reason` | empty | Single `CreatureSpawnEvent.SpawnReason`. |
| `reasons` | empty | Accepted reason list. |

## `knockback_cause`

Matches Paper `EntityKnockbackEvent.Cause`.

| Field | Default | Description |
| --- | --- | --- |
| `cause / causes` | empty | Accepted cause name(s). |
| `reason / reasons` | empty | Aliases merged into the same accepted set. |

## `knockback_reason`

Alias of [`knockback_cause`](#knockback_cause)。

## `exhaustion_reason`

Matches Bukkit `EntityExhaustionEvent.ExhaustionReason`.

| Field | Default | Description |
| --- | --- | --- |
| `reason` | empty | Single reason. |
| `reasons` | empty | Accepted reason list. |

## `villager_trade`

Distinguishes an actual villager trade from another Paper purchase event.

| Field | Default | Description |
| --- | --- | --- |
| `value` | `true` | `true` requires `PlayerTradeEvent`; `false` requires another purchase-event subtype. |

## `cooldown`

Acquires a condition-scoped runtime cooldown and matches only when it can be acquired.

| Field | Default | Description |
| --- | --- | --- |
| `seconds` | `0` | Cooldown length in seconds; non-positive values always match. |
| `owner` | `SOURCE` | Entity selector that owns the cooldown. |
| `per-target` | `false` | Keep an independent cooldown for each `TARGET`. |

## `cooldown_material`

Matches the material involved in Paper `PlayerItemCooldownEvent`.

| Field | Default | Description |
| --- | --- | --- |
| `material` | empty | Single Bukkit material name or namespaced key. |
| `materials` | empty | Accepted material list. |
