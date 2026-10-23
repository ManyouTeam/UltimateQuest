# 🕹️ Movement and input

Choose an ID for the activity, then check which data it supplies. `PLAYER` is the player gaining task progress. The identities of `SOURCE` and `TARGET` vary by event; they are not always the player and a mob.

## Configuration paths

Replace `source` in the [normal task tutorial](../tasks.md) with this single-objective fragment. For several objectives, put it under `objectives.<id>.source`. Templates use `trigger` and `amount` directly.

```yaml
source:
  trigger: move
  amount: 10
```

This requires ten matching events. Some types fire frequently; combine the explanations below with conditions and [anti-abuse](../anti-abuse.md). The default is `count: events`; each entry lists its supported quantity modes. See [counting modes](../counting.md).

## `move`

`events` keeps the existing behavior: changes in block coordinates or world add one; movement within a block and rotation do not. `distance` sums three-dimensional straight-line segments between event positions, in blocks. It includes movement within a block and excludes rotation, teleports, and cross-world jumps.

| Available data | Value supplied by this event |
| --- | --- |
| Supported `count` | `events`, `distance` |
| Event / payload | `PlayerMoveEvent` |
| Availability | Paper and Spigot |
| PLAYER | Player associated with this event |
| SOURCE | Quest player |
| TARGET | Absent |
| LOCATION | Destination; from/to also retained |
| TRIGGER_ITEM / slot | Absent |
| BLOCK | Block at the movement destination |

## Sprinting and swimming

Use `move` with `count: distance` and event conditions. `sprinting` and `swimming` are conditions, not separate triggers. Add `riding`, `flying`, or `gliding` with `value: false` to exclude those states. Swimming means the server swimming state, rather than standing in water. Set `anti-abuse.cooldown-ms: 0` to retain every movement segment.

```yaml
source:
  trigger: move
  count: distance
  amount: 256
  conditions:
    sprinting:
      type: sprinting
      target: PLAYER
      value: true
    not_swimming:
      type: swimming
      target: PLAYER
      value: false
  anti-abuse:
    cooldown-ms: 0
```

For swimming, replace these conditions with `type: swimming`, `target: PLAYER`, and `value: true`; set `amount` to the required block distance. The bundled `sprinter` and `swimmer` templates include the other movement exclusions.

## `input`

Counts reported player input updates where the event is supported. Input changes do not prove movement or measure distance.

| Available data | Value supplied by this event |
| --- | --- |
| Supported `count` | `events` |
| Event / payload | `PlayerInputEvent` |
| Availability | Paper and Spigot |
| PLAYER | Player associated with this event |
| SOURCE | Quest player |
| TARGET | Absent |
| LOCATION | Player location |
| TRIGGER_ITEM / slot | Absent |

## `toggle_sneak`

Counts sneak-state toggles. Starting and stopping can both trigger; add a player-state condition when only one state should qualify.

| Available data | Value supplied by this event |
| --- | --- |
| Supported `count` | `events` |
| Event / payload | `PlayerToggleSneakEvent` |
| Availability | Paper and Spigot |
| PLAYER | Player associated with this event |
| SOURCE | Quest player |
| TARGET | Absent |
| LOCATION | Player location |
| TRIGGER_ITEM / slot | Absent |

## `toggle_flight`

Counts flight-state toggles. The event carries the enabled/disabled state; an occurrence does not necessarily mean taking off.

| Available data | Value supplied by this event |
| --- | --- |
| Supported `count` | `events` |
| Event / payload | `PlayerToggleFlightEvent` |
| Availability | Paper and Spigot |
| PLAYER | Player associated with this event |
| SOURCE | Quest player |
| TARGET | Absent |
| LOCATION | Player location |
| TRIGGER_ITEM / slot | Absent |

## `riptide`

Counts the player activating Riptide with an item; the context item is the item used.

| Available data | Value supplied by this event |
| --- | --- |
| Supported `count` | `events` |
| Event / payload | `PlayerRiptideEvent` |
| Availability | Paper and Spigot |
| PLAYER | Player associated with this event |
| SOURCE | Quest player |
| TARGET | Absent |
| LOCATION | Player location |
| TRIGGER_ITEM / slot | Event item / HAND |

## `jump`

Counts player jumps and retains the from/to locations. Requires the matching Paper event; jump height is not added to progress.

| Available data | Value supplied by this event |
| --- | --- |
| Supported `count` | `events` |
| Event / payload | `PlayerJumpEvent` |
| Availability | Paper mode and event class required |
| PLAYER | Player associated with this event |
| SOURCE | Quest player |
| TARGET | Absent |
| LOCATION | Destination; from/to also retained |
| TRIGGER_ITEM / slot | Absent |
| BLOCK | Block at the event destination |

## `elytra_boost`

Counts player elytra boosts for flight-activity goals.

| Available data | Value supplied by this event |
| --- | --- |
| Supported `count` | `events` |
| Event / payload | `PlayerElytraBoostEvent` |
| Availability | Paper mode and event class required |
| PLAYER | Player associated with this event |
| SOURCE | Quest player |
| TARGET | Quest player |
| LOCATION | Player location |
| TRIGGER_ITEM / slot | Boost item / event hand |

## `lunge`

Counts reported player lunges, retaining the active item when resolvable. Requires support for the corresponding event.

| Available data | Value supplied by this event |
| --- | --- |
| Supported `count` | `events` |
| Event / payload | `EntityLungeEvent` |
| Availability | Paper mode and event class required |
| PLAYER | Player associated with this event |
| SOURCE | Quest player |
| TARGET | Absent |
| LOCATION | Player location |
| TRIGGER_ITEM / slot | Active item when available, otherwise implementation fallback |

Confirm platform support and test with a new task. Extra-data names in the tables describe event data or extension APIs; they do not imply matching condition types exist.

[Trigger overview](../triggers.md) · [Event conditions](../conditions/README.md)
