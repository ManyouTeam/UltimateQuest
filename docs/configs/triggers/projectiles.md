# 🏹 Projectiles and ranged attacks

Choose an ID for the activity, then check which data it supplies. `PLAYER` is the player gaining task progress. The identities of `SOURCE` and `TARGET` vary by event; they are not always the player and a mob.

## Configuration paths

Replace `source` in the [normal task tutorial](../tasks.md) with this single-objective fragment. For several objectives, put it under `objectives.<id>.source`. Templates use `trigger` and `amount` directly.

```yaml
source:
  trigger: projectile_launch
  amount: 10
```

This requires ten matching events. Some types fire frequently; combine the explanations below with conditions and [anti-abuse](../anti-abuse.md). The default is `count: events`; each entry lists its supported quantity modes. See [counting modes](../counting.md).

## `projectile_launch`

Counts projectile launches attributed to a player and records launch information for later hit or tick handling.

| Available data | Value supplied by this event |
| --- | --- |
| Supported `count` | `events` |
| Event / payload | `ProjectileLaunchEvent` |
| Availability | Paper and Spigot |
| PLAYER | Player associated with this event |
| SOURCE | Quest player |
| TARGET | Projectile |
| LOCATION | Projectile location |
| TRIGGER_ITEM / slot | Resolved firing/held item and slot |
| Extra context | `projectile` |

## `projectile_hit`

Counts projectile hits on blocks or entities when a player is resolved. Block hits may have no entity TARGET; check available data before filtering.

| Available data | Value supplied by this event |
| --- | --- |
| Supported `count` | `events` |
| Event / payload | `ProjectileHitEvent` |
| Availability | Paper and Spigot |
| PLAYER | Player associated with this event |
| SOURCE | Resolved shooter |
| TARGET | Hit entity, absent on block-only hits |
| LOCATION | Projectile location |
| TRIGGER_ITEM / slot | Tracked firing item/slot if known |
| BLOCK | The hit block; may be absent for entity hits |
| Extra context | `projectile` |

## `projectile_tick`

Ticks tracked projectiles while the player/context remains valid and relevant objectives are active. It measures repeated flight events, not launches or hits.

| Available data | Value supplied by this event |
| --- | --- |
| Supported `count` | `events` |
| Event / payload | `TriggerRuntime.ProjectileTick` |
| Availability | Paper and Spigot |
| PLAYER | Player associated with this event |
| SOURCE | Tracked shooter |
| TARGET | Projectile |
| LOCATION | Projectile location |
| TRIGGER_ITEM / slot | Tracked firing item/slot |
| BLOCK | Block at the projectile’s current location |
| Extra context | `projectile` |

## `shoot`

Counts player bow/crossbow shooting. One shot may satisfy both `shoot` and `shoot_bow` objectives; the IDs are not mutually exclusive.

| Available data | Value supplied by this event |
| --- | --- |
| Supported `count` | `events` |
| Event / payload | `EntityShootBowEvent` |
| Availability | Paper and Spigot |
| PLAYER | Player associated with this event |
| SOURCE | Quest player |
| TARGET | Projectile |
| LOCATION | Projectile location |
| TRIGGER_ITEM / slot | Bow/crossbow / event hand |
| Extra context | `bow_force`, `bow`, `consumable`, `hand`, `projectile` |

## `shoot_bow`

Counts player bow/crossbow shooting with weapon, hand, and force data. Force is not automatically used as progress.

| Available data | Value supplied by this event |
| --- | --- |
| Supported `count` | `events` |
| Event / payload | `EntityShootBowEvent` |
| Availability | Paper and Spigot |
| PLAYER | Player associated with this event |
| SOURCE | Quest player |
| TARGET | Projectile |
| LOCATION | Projectile location |
| TRIGGER_ITEM / slot | Bow/crossbow / event hand |
| Extra context | `bow_force`, `bow`, `consumable`, `hand`, `projectile` |

## `load_crossbow`

Counts crossbow loading, which happens before shooting and does not imply a projectile was fired.

| Available data | Value supplied by this event |
| --- | --- |
| Supported `count` | `events` |
| Event / payload | `EntityLoadCrossbowEvent` |
| Availability | Paper mode and event class required |
| PLAYER | Player associated with this event |
| SOURCE | Quest player |
| TARGET | Absent |
| LOCATION | Player location |
| TRIGGER_ITEM / slot | Crossbow / event hand |

Confirm platform support and test with a new task. Extra-data names in the tables describe event data or extension APIs; they do not imply matching condition types exist.

[Trigger overview](../triggers.md) · [Event conditions](../conditions/README.md)
