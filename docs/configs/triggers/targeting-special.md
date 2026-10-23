# 🎯 Targeting and special entities

Choose an ID for the activity, then check which data it supplies. `PLAYER` is the player gaining task progress. The identities of `SOURCE` and `TARGET` vary by event; they are not always the player and a mob.

## Configuration paths

Replace `source` in the [normal task tutorial](../tasks.md) with this single-objective fragment. For several objectives, put it under `objectives.<id>.source`. Templates use `trigger` and `amount` directly.

```yaml
source:
  trigger: target
  amount: 10
```

This requires ten matching events. Some types fire frequently; combine the explanations below with conditions and [anti-abuse](../anti-abuse.md). The default is `count: events`; each entry lists its supported quantity modes. See [counting modes](../counting.md).

## `target`

Counts an entity acquiring a player as its target and records that association. SOURCE is the entity; use PLAYER for the quest owner.

| Available data | Value supplied by this event |
| --- | --- |
| Supported `count` | `events` |
| Event / payload | `EntityTargetEvent` |
| Availability | Paper and Spigot |
| PLAYER | Player associated with this event |
| SOURCE | Targeting entity |
| TARGET | Quest player |
| LOCATION | Player location |
| TRIGGER_ITEM / slot | Absent |
| Extra context | `target_reason`, `target_count` |

## `untag`

Counts a tracked entity dropping its previously associated player target. It depends on prior tracking, rather than every target-loss event.

| Available data | Value supplied by this event |
| --- | --- |
| Supported `count` | `events` |
| Event / payload | `EntityTargetEvent` |
| Availability | Paper and Spigot |
| PLAYER | Player associated with this event |
| SOURCE | Entity losing the tracked target |
| TARGET | Quest player |
| LOCATION | Player location |
| TRIGGER_ITEM / slot | Absent |
| Extra context | `target_reason` |

## `target_tick`

Scans for an entity within 32 blocks along the observed player’s view each tick while relevant objectives are active. No entity means no event; looking at one entity can count repeatedly.

| Available data | Value supplied by this event |
| --- | --- |
| Supported `count` | `events` |
| Event / payload | `TriggerRuntime.PlayerTick` |
| Availability | Paper and Spigot |
| PLAYER | Player associated with this event |
| SOURCE | Quest player |
| TARGET | Looked-at entity |
| LOCATION | Target/entity location |
| TRIGGER_ITEM / slot | Absent |
| BLOCK | Block at the selected entity’s location |

## `enderman_attack_player`

Counts the Enderman attack-player decision event. Requires the matching Paper event and differs from a general received-damage trigger.

| Available data | Value supplied by this event |
| --- | --- |
| Supported `count` | `events` |
| Event / payload | `EndermanAttackPlayerEvent` |
| Availability | Paper mode and event class required |
| PLAYER | Player associated with this event |
| SOURCE | Enderman |
| TARGET | Quest player |
| LOCATION | Player location |
| TRIGGER_ITEM / slot | Absent |

## `phantom_pre_spawn`

Counts non-aborted phantom pre-spawn events associated with a spawning player. A pre-spawn event does not guarantee a phantom ultimately remains in the world.

| Available data | Value supplied by this event |
| --- | --- |
| Supported `count` | `events` |
| Event / payload | `PhantomPreSpawnEvent` |
| Availability | Paper mode and event class required |
| PLAYER | Player associated with this event |
| SOURCE | Quest player |
| TARGET | Absent |
| LOCATION | Proposed spawn location |
| TRIGGER_ITEM / slot | Absent |
| BLOCK | Block at the planned spawn location |

## `warden_anger_change`

Counts Warden anger changes targeting a player, adding event counts rather than the anger difference.

| Available data | Value supplied by this event |
| --- | --- |
| Supported `count` | `events` |
| Event / payload | `WardenAngerChangeEvent` |
| Availability | Paper mode and event class required |
| PLAYER | Player associated with this event |
| SOURCE | Quest player |
| TARGET | Warden |
| LOCATION | Target/entity location |
| TRIGGER_ITEM / slot | Absent |

Confirm platform support and test with a new task. Extra-data names in the tables describe event data or extension APIs; they do not imply matching condition types exist.

[Trigger overview](../triggers.md) · [Event conditions](../conditions/README.md)
