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
| Availability | Paper and Spigot |
| PLAYER | Player associated with this event |
| SOURCE | Targeting entity |
| TARGET | Quest player |
| LOCATION | Player location |
| TRIGGER_ITEM / slot | Absent |

## `untag`

Counts a tracked entity dropping its previously associated player target. It depends on prior tracking, rather than every target-loss event.

| Available data | Value supplied by this event |
| --- | --- |
| Supported `count` | `events` |
| Availability | Paper and Spigot |
| PLAYER | Player associated with this event |
| SOURCE | Entity losing the tracked target |
| TARGET | Quest player |
| LOCATION | Player location |
| TRIGGER_ITEM / slot | Absent |

## `target_tick`

Scans for an entity within 32 blocks along the observed player’s view each tick while relevant objectives are active. No entity means no event; looking at one entity can count repeatedly.

| Available data | Value supplied by this event |
| --- | --- |
| Supported `count` | `events` |
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
| Availability | Paper mode and event class required |
| PLAYER | Player associated with this event |
| SOURCE | Quest player |
| TARGET | Warden |
| LOCATION | Target/entity location |
| TRIGGER_ITEM / slot | Absent |


## `shear`

Supported `count`: `events`. Runs once per matching entity-shearing event. Filter `TARGET` to distinguish sheep (`SHEEP`), mooshrooms (`MOOSHROOM`), and other supported entities. `PLAYER` / `SOURCE` is the player, `TARGET` is the sheared entity, and `LOCATION` is its location. The trigger item and slot are the actual shears and hand. Leash cutting uses the separate `cut_leash` trigger.

## `cut_leash`

Supported `count`: `events`. Runs when a player cuts a leash with shears, once per unleashed entity. `PLAYER` / `SOURCE` is the player, `TARGET` is the unleashed entity, and `LOCATION` is its location. The trigger item and slot are the actual shears and hand. Empty-hand unleashing does not trigger it. Requires a server version that supports leash cutting and emits the corresponding player unleash event. Both triggers are available on Paper and Spigot; cancelled events do not count.

## `breed`

Supported `count`: `events`. Runs once per matching breeding event attributed to a player. `PLAYER` / `SOURCE` is the recorded breeder; `TARGET` is the newborn entity; `LOCATION` is the newborn's location. Use target entity matching to restrict the animal type.

The trigger item is the recorded breeding item, if present. The event does not supply the original feeding slot, so the slot is absent. Events without a player breeder and cancelled events do not count. Available on Paper and Spigot; animal and breeding coverage depends on the server emitting `EntityBreedEvent`.

## `tame`

Supported `count`: `events`. Runs once when a player successfully tames an entity. `PLAYER` / `SOURCE` is the owner, `TARGET` is the tamed entity, and `LOCATION` is its location. Use target entity matching to restrict species. Failed attempts, non-player owners, and cancelled events do not count. The event does not provide the exact taming item or hand, so trigger-item data is absent. Available on Paper and Spigot.

[Trigger overview](../triggers.md) · [Event conditions](../conditions/README.md)
