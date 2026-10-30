# ⚔️ Combat and damage

Choose an ID for the activity, then check which data it supplies. `PLAYER` is the player gaining task progress. The identities of `SOURCE` and `TARGET` vary by event; they are not always the player and a mob.

## Configuration paths

Replace `source` in the [normal task tutorial](../tasks.md) with this single-objective fragment. For several objectives, put it under `objectives.<id>.source`. Templates use `trigger` and `amount` directly.

```yaml
source:
  trigger: attack
  amount: 10
```

This requires ten matching events. Some types fire frequently; combine the explanations below with conditions and [anti-abuse](../anti-abuse.md). The default is `count: events`; each entry lists its supported quantity modes. See [counting modes](../counting.md).

## `attack`

`events` adds one per attack damage event; `damage` uses final event damage after armor and other damage modifiers. This is an event value, not measured health loss, and is not capped by the target’s remaining health.

| Available data | Value supplied by this event |
| --- | --- |
| Supported `count` | `events`, `damage` |
| Event / payload | `EntityDamageByEntityEvent` |
| Availability | Paper and Spigot |
| PLAYER | Player associated with this event |
| SOURCE | Quest player |
| TARGET | Damaged/attempted target |
| LOCATION | Target/entity location |
| TRIGGER_ITEM / slot | Melee main hand, trident item, or tracked firing weapon/slot |
| Extra context | `original_damage`, `damage_cause` |

## `melee_attack`

`events` adds one per melee damage event; `damage` sums final event damage after damage modifiers.

| Available data | Value supplied by this event |
| --- | --- |
| Supported `count` | `events`, `damage` |
| Event / payload | `EntityDamageByEntityEvent` |
| Availability | Paper and Spigot |
| PLAYER | Player associated with this event |
| SOURCE | Quest player |
| TARGET | Damaged/attempted target |
| LOCATION | Target/entity location |
| TRIGGER_ITEM / slot | Main hand / HAND |
| Extra context | `original_damage`, `damage_cause` |

## `damage`

`events` adds one per incoming damage event; `damage` sums final event damage after damage modifiers.

| Available data | Value supplied by this event |
| --- | --- |
| Supported `count` | `events`, `damage` |
| Event / payload | `EntityDamageEvent` |
| Availability | Paper and Spigot |
| PLAYER | Player associated with this event |
| SOURCE | Resolved attacker; may be absent for environmental damage |
| TARGET | Quest player |
| LOCATION | Player location |
| TRIGGER_ITEM / slot | Absent |
| Extra context | `original_damage`, `damage_cause`, `damage_by_entity`, `damage_by_block` |

## `damage_by_entity`

`events` adds one per entity-caused incoming damage event; `damage` sums final event damage after damage modifiers.

| Available data | Value supplied by this event |
| --- | --- |
| Supported `count` | `events`, `damage` |
| Event / payload | `EntityDamageByEntityEvent` |
| Availability | Paper and Spigot |
| PLAYER | Player associated with this event |
| SOURCE | Resolved attacker or direct damager |
| TARGET | Quest player |
| LOCATION | Player location |
| TRIGGER_ITEM / slot | Absent |
| Extra context | `original_damage`, `damage_cause`, `damage_by_entity`, `damage_by_block` |

## `kill`

`events` adds one per attributed kill; `items` sums the death event drop stacks, rather than inventory pickups. Use `match_item` + `item: COUNTED_ITEM` to select specific drops.

| Available data | Value supplied by this event |
| --- | --- |
| Supported `count` | `events`, `items` |
| Event / payload | `EntityDeathEvent` |
| Availability | Paper and Spigot |
| PLAYER | Player associated with this event |
| SOURCE | Quest player |
| TARGET | Dead entity |
| LOCATION | Target/entity location |
| TRIGGER_ITEM / slot | Absent |
| Extra context | `original_experience` |

## `death`

Counts the task player’s own death. Unlike `kill`, TARGET is the dead player and SOURCE is the killer when available.

| Available data | Value supplied by this event |
| --- | --- |
| Supported `count` | `events` |
| Event / payload | `EntityDeathEvent` |
| Availability | Paper and Spigot |
| PLAYER | Player associated with this event |
| SOURCE | Player killer if available |
| TARGET | Quest player |
| LOCATION | Player location |
| TRIGGER_ITEM / slot | Absent |
| Extra context | `original_experience` |

## `regain`

`events` adds one per regain event; `healing` uses positive healing capped by the player’s missing health at the event. If 2 health is missing and the event restores 10, it contributes 2.

| Available data | Value supplied by this event |
| --- | --- |
| Supported `count` | `events`, `healing` |
| Event / payload | `EntityRegainHealthEvent` |
| Availability | Paper and Spigot |
| PLAYER | Player associated with this event |
| SOURCE | Quest player |
| TARGET | Absent |
| LOCATION | Player location |
| TRIGGER_ITEM / slot | Absent |
| Extra context | `original_amount` |

## `shield_block`

`events` adds one per successful block; `blocked_damage` sums the amount blocked by the shield in that event.

| Available data | Value supplied by this event |
| --- | --- |
| Supported `count` | `events`, `blocked_damage` |
| Event / payload | `EntityDamageByEntityEvent` |
| Availability | Paper and Spigot |
| PLAYER | Player associated with this event |
| SOURCE | Resolved attacker or direct damager |
| TARGET | Quest player |
| LOCATION | Player location |
| TRIGGER_ITEM / slot | Absent |
| Extra context | `original_damage`, `damage_cause`, `damage_by_entity` |

## `shield_disable`

Counts the player’s shield being disabled and requires the matching Paper event. It is different from a successful shield block.

| Available data | Value supplied by this event |
| --- | --- |
| Supported `count` | `events` |
| Event / payload | `PlayerShieldDisableEvent` |
| Availability | Paper mode and event class required |
| PLAYER | Player associated with this event |
| SOURCE | Event damager |
| TARGET | Quest player |
| LOCATION | Player location |
| TRIGGER_ITEM / slot | Absent |

## `attempt_smash_attack`

Counts player smash-attack attempts when the event result is not DENY. Requires the matching Paper event and does not measure damage.

| Available data | Value supplied by this event |
| --- | --- |
| Supported `count` | `events` |
| Event / payload | `EntityAttemptSmashAttackEvent` |
| Availability | Paper mode and event class required |
| PLAYER | Player associated with this event |
| SOURCE | Quest player |
| TARGET | Damaged/attempted target |
| LOCATION | Target/entity location |
| TRIGGER_ITEM / slot | Event weapon / HAND |

## `knockback`

Counts knockback affecting the player. SOURCE is the pushing entity when supplied, otherwise the player; do not assume it always identifies an attacker.

| Available data | Value supplied by this event |
| --- | --- |
| Supported `count` | `events` |
| Event / payload | `EntityKnockbackEvent` |
| Availability | Paper mode and event class required |
| PLAYER | Player associated with this event |
| SOURCE | Pushing entity if exposed, otherwise the player |
| TARGET | Quest player |
| LOCATION | Player location |
| TRIGGER_ITEM / slot | Absent |

## `combust`

Counts the player starting to burn, rather than the number of seconds spent burning.

| Available data | Value supplied by this event |
| --- | --- |
| Supported `count` | `events` |
| Event / payload | `EntityCombustEvent` |
| Availability | Paper and Spigot |
| PLAYER | Player associated with this event |
| SOURCE | Quest player |
| TARGET | Absent |
| LOCATION | Player location |
| TRIGGER_ITEM / slot | Absent |
| Extra context | `original_duration` |

## `creeper_explode`

Counts explosion preparation when a player can be associated, typically the creeper’s target. It does not total every creeper explosion in the world.

| Available data | Value supplied by this event |
| --- | --- |
| Supported `count` | `events` |
| Event / payload | `ExplosionPrimeEvent` |
| Availability | Paper and Spigot |
| PLAYER | Player associated with this event |
| SOURCE | Exploding entity |
| TARGET | Quest player |
| LOCATION | Target/entity location |
| TRIGGER_ITEM / slot | Absent |
| Extra context | `original_radius` |

Confirm platform support and test with a new task. Extra-data names in the tables describe event data or extension APIs; they do not imply matching condition types exist.

[Trigger overview](../triggers.md) · [Event conditions](../conditions/README.md)
