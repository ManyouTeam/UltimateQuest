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
| Event / payload | `EntityAirChangeEvent` |
| Availability | Paper and Spigot |
| PLAYER | Player associated with this event |
| SOURCE | Quest player |
| TARGET | Absent |
| LOCATION | Player location |
| TRIGGER_ITEM / slot | Absent |
| Extra context | `original_air`, `previous_air` |

## `food_level_change`

Counts food-level changes, which do not always represent eating. Prefer `consume` for food-consumption goals.

| Available data | Value supplied by this event |
| --- | --- |
| Supported `count` | `events` |
| Event / payload | `FoodLevelChangeEvent` |
| Availability | Paper and Spigot |
| PLAYER | Player associated with this event |
| SOURCE | Quest player |
| TARGET | Absent |
| LOCATION | Player location |
| TRIGGER_ITEM / slot | Absent |
| Extra context | `original_food_level` |

## `exp_change`

`events` adds one per experience change event; `experience` sums positive event experience points. Zero and negative amounts add no quantity progress. These are Minecraft XP points, not levels or battle pass points.

| Available data | Value supplied by this event |
| --- | --- |
| Supported `count` | `events`, `experience` |
| Event / payload | `PlayerExpChangeEvent` |
| Availability | Paper and Spigot |
| PLAYER | Player associated with this event |
| SOURCE | Quest player |
| TARGET | Absent |
| LOCATION | Player location |
| TRIGGER_ITEM / slot | Absent |
| Extra context | `original_experience` |

## `potion_effect`

Counts potion-effect changes on the player. Duration comes from the changed effect, but progress still counts events.

| Available data | Value supplied by this event |
| --- | --- |
| Supported `count` | `events` |
| Event / payload | `EntityPotionEffectEvent` |
| Availability | Paper and Spigot |
| PLAYER | Player associated with this event |
| SOURCE | Quest player |
| TARGET | Quest player |
| LOCATION | Player location |
| TRIGGER_ITEM / slot | Absent |
| Extra context | `original_duration` |

## `effect_tick`

Counts supported effect-tick events; effect details remain in the underlying event. It is not a once-per-second timer.

| Available data | Value supplied by this event |
| --- | --- |
| Supported `count` | `events` |
| Event / payload | `EntityEffectTickEvent` |
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
| Event / payload | `EntityExhaustionEvent` |
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
| Event / payload | `PlayerPickupExperienceEvent` |
| Availability | Paper mode and event class required |
| PLAYER | Player associated with this event |
| SOURCE | Quest player |
| TARGET | Absent |
| LOCATION | Experience-orb location |
| TRIGGER_ITEM / slot | Absent |
| Extra context | `original_experience` |

Confirm platform support and test with a new task. Extra-data names in the tables describe event data or extension APIs; they do not imply matching condition types exist.

[Trigger overview](../triggers.md) · [Event conditions](../conditions/README.md)
