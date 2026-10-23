# 🧍 Entity and state conditions

Use these conditions to decide which events contribute to tasks. Each `conditions` example is a fragment for an objective’s `source` or a template root. Only player checks that need no event data belong at the task root for acceptance. See [condition basics](README.md).

## `sneaking`

Requires the selected player to be sneaking. Use `target: PLAYER` for the quest owner; `value: false` requires the opposite state. This condition needs a player.

| Field | Default | Description |
| --- | --- | --- |
| `target` | TARGET | Entity selector. Use PLAYER to select the quest owner. |
| `value` | true | Expected boolean state. |

```yaml
conditions:
  sneaking:
    type: sneaking
    target: PLAYER
    value: true
```

A missing or incompatible target fails before common `not` inversion. Inversion is not an existence check.

## `not_sneaking`

Requires the selected player not to be sneaking. This is the dedicated non-sneaking condition and still needs a player selector.

| Field | Default | Description |
| --- | --- | --- |
| `target` | TARGET | Entity selector. Use PLAYER to select the quest owner. |

```yaml
conditions:
  not_sneaking:
    type: not_sneaking
    target: PLAYER
```

A missing or incompatible target fails before common `not` inversion. Inversion is not an existence check.

## `on_ground`

Requires the selected living entity to be on the ground; `value: false` requires it not to be. It tests entity state, not a particular block material.

| Field | Default | Description |
| --- | --- | --- |
| `target` | TARGET | Entity selector. Use PLAYER to select the quest owner. |
| `value` | true | Expected boolean state. |

```yaml
conditions:
  on_ground:
    type: on_ground
    target: PLAYER
    value: true
```

A missing or incompatible target fails before common `not` inversion. Inversion is not an existence check.

## `in_air`

Requires the selected entity to be off the ground. It does not require flight permission or imply elytra flight.

| Field | Default | Description |
| --- | --- | --- |
| `target` | TARGET | Entity selector. Use PLAYER to select the quest owner. |
| `value` | true | Expected boolean state. |

```yaml
conditions:
  in_air:
    type: in_air
    target: PLAYER
    value: true
```

A missing or incompatible target fails before common `not` inversion. Inversion is not an existence check.

## `in_water`

Requires the selected entity to be in water. Set `target: PLAYER` for the quest owner instead of relying on the default TARGET.

| Field | Default | Description |
| --- | --- | --- |
| `target` | TARGET | Entity selector. Use PLAYER to select the quest owner. |
| `value` | true | Expected boolean state. |

```yaml
conditions:
  in_water:
    type: in_water
    target: PLAYER
    value: true
```

A missing or incompatible target fails before common `not` inversion. Inversion is not an existence check.

## `in_lava`

Checks whether the block at the selected entity’s feet is `LAVA`. This is a location check, not a burning-state check.

| Field | Default | Description |
| --- | --- | --- |
| `target` | TARGET | Entity selector. Use PLAYER to select the quest owner. |
| `value` | true | Expected boolean state. |

```yaml
conditions:
  in_lava:
    type: in_lava
    target: PLAYER
    value: true
```

A missing or incompatible target fails before common `not` inversion. Inversion is not an existence check.

## `game_mode`

Allows specified game modes, such as survival and adventure. The default target may be a mob or absent, so select PLAYER explicitly for the quest owner.

| Field | Default | Description |
| --- | --- | --- |
| `target` | TARGET | Entity selector. Use PLAYER to select the quest owner. |
| `modes / mode` | — | List or scalar game-mode names. |

```yaml
conditions:
  survival:
    type: game_mode
    target: PLAYER
    modes:
    - SURVIVAL
    - ADVENTURE
```

## `has_potion`

Checks effects currently active on the selected living entity. `require-min-effects: 1` requires at least one listed effect, not all of them. ALL, BENEFICIAL, HARMFUL, and NEUTRAL select categories; unknown selectors do not match. It does not directly inspect a pending effect in an effect-change event.

| Field | Default | Description |
| --- | --- | --- |
| `target` | TARGET | Entity selector. Use PLAYER to select the quest owner. |
| `potions` | [] | Exact effect keys or ALL/BENEFICIAL/HARMFUL/NEUTRAL. |
| `require-min-effects` | 1 | Minimum matching type count, clamped to at least 1. |

```yaml
conditions:
  boosted:
    type: has_potion
    target: PLAYER
    potions:
    - minecraft:speed
    - minecraft:haste
    require-min-effects: 1
```

## `match_entity`

Filters a selected living entity, for example zombie kills. See [entity matching](../../shared-formats/match-entity-format/README.md) for rules. Nonliving targets fail. Use the `entity-health` matcher for health limits; there is no direct `health_percent` event condition.

| Field | Default | Description |
| --- | --- | --- |
| `target` | TARGET | Entity selector. Use PLAYER to select the quest owner. |
| `match` | current section | MatchEntityFormat rules. |

```yaml
conditions:
  zombie:
    type: match_entity
    target: TARGET
    match:
      entity-types:
      - ZOMBIE
```
