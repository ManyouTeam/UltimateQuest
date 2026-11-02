# ❤️ Player state

Choose an ID for the activity, then check which data it supplies. `PLAYER` is the player gaining task progress. The identities of `SOURCE` and `TARGET` vary by event; they are not always the player and a mob.

## Configuration paths

Replace `source` in the [normal task tutorial](../tasks.md) with this single-objective fragment. For several objectives, put it under `objectives.<id>.source`. Templates use `trigger` and `amount` directly.

```yaml
source:
  trigger: air_change
  amount: 10
```

This requires ten matching events. Some types fire frequently; combine the explanations below with conditions and [anti-abuse](../anti-abuse.md). The default is `count: events`; each entry lists its supported quantity modes. See [counting modes](../counting.md).

## `air_change`

Counts changes in remaining air, not the number of air units changed.

| Available data | Value supplied by this event |
| --- | --- |
| Supported `count` | `events` |
| Availability | Paper and Spigot |
| PLAYER | Player associated with this event |
| SOURCE | Quest player |
| TARGET | Absent |
| LOCATION | Player location |
| TRIGGER_ITEM / slot | Absent |

## `food_level_change`

Counts food-level changes, which do not always represent eating. Prefer `consume` for food-consumption goals.

| Available data | Value supplied by this event |
| --- | --- |
| Supported `count` | `events` |
| Availability | Paper and Spigot |
| PLAYER | Player associated with this event |
| SOURCE | Quest player |
| TARGET | Absent |
| LOCATION | Player location |
| TRIGGER_ITEM / slot | Absent |

## `exp_change`

`events` adds one per experience change event; `experience` sums positive event experience points. Zero and negative amounts add no quantity progress. These are Minecraft XP points, not levels or battle pass points.

| Available data | Value supplied by this event |
| --- | --- |
| Supported `count` | `events`, `experience` |
| Availability | Paper and Spigot |
| PLAYER | Player associated with this event |
| SOURCE | Quest player |
| TARGET | Absent |
| LOCATION | Player location |
| TRIGGER_ITEM / slot | Absent |

## `advancement_done`

Adds one when a player completes a Minecraft advancement, rather than each individual criterion. Advancements completed before accepting the task are not counted retroactively. All events reported by the server count, including recipe unlocks and custom datapack advancements. Revoking and completing an advancement again counts again.

| Available data | Value supplied by this event |
| --- | --- |
| Supported `count` | `events` |
| Availability | Paper and Spigot |
| PLAYER | Player completing the advancement |
| SOURCE | Quest player |
| TARGET | Absent |
| LOCATION | Player location |
| TRIGGER_ITEM / slot | Absent |

Use the [`advancement` event condition](../conditions/extended.md#advancement) to directly match the advancement completed by this event. This example only counts `minecraft:story/mine_diamond`:

```yaml
source:
  trigger: advancement_done
  amount: 1
  conditions:
    advancement:
      type: advancement
      advancement: 'minecraft:story/mine_diamond'
```

Without conditions, all advancements count. Use an `advancements` list to match any of several IDs, or `not: true` to exclude the specified advancements.

## `potion_effect`

Counts potion-effect changes on the player. Duration comes from the changed effect, but progress still counts events.

| Available data | Value supplied by this event |
| --- | --- |
| Supported `count` | `events` |
| Availability | Paper and Spigot |
| PLAYER | Player associated with this event |
| SOURCE | Quest player |
| TARGET | Quest player |
| LOCATION | Player location |
| TRIGGER_ITEM / slot | Absent |

## `effect_tick`

Counts supported effect-tick events; effect details remain in the underlying event. It is not a once-per-second timer.

| Available data | Value supplied by this event |
| --- | --- |
| Supported `count` | `events` |
| Availability | Paper mode and event class required |
| PLAYER | Player associated with this event |
| SOURCE | Quest player |
| TARGET | Absent |
| LOCATION | Player location |
| TRIGGER_ITEM / slot | Absent |

## `exhaustion`

`events` adds one per exhaustion event; `exhaustion` sums positive event exhaustion amounts. This is neither hunger nor saturation.

| Available data | Value supplied by this event |
| --- | --- |
| Supported `count` | `events`, `exhaustion` |
| Availability | Optional class detection on any mode |
| PLAYER | Player associated with this event |
| SOURCE | Quest player |
| TARGET | Absent |
| LOCATION | Player location |
| TRIGGER_ITEM / slot | Absent |

## `pickup_experience`

`events` adds one per pickup event; `experience` uses the orb’s XP points. This and `exp_change` may describe the same XP gain; configuring both sources counts both.

| Available data | Value supplied by this event |
| --- | --- |
| Supported `count` | `events`, `experience` |
| Availability | Paper mode and event class required |
| PLAYER | Player associated with this event |
| SOURCE | Quest player |
| TARGET | Absent |
| LOCATION | Experience-orb location |
| TRIGGER_ITEM / slot | Absent |

[Trigger overview](../triggers.md) · [Event conditions](../conditions/README.md)
