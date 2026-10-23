# 🧱 Blocks and interactions

Choose an ID for the activity, then check which data it supplies. `PLAYER` is the player gaining task progress. The identities of `SOURCE` and `TARGET` vary by event; they are not always the player and a mob.

## Configuration paths

Replace `source` in the [normal task tutorial](../tasks.md) with this single-objective fragment. For several objectives, put it under `objectives.<id>.source`. Templates use `trigger` and `amount` directly.

```yaml
source:
  trigger: block_break
  amount: 10
```

This requires ten matching events. Some types fire frequently; combine the explanations below with conditions and [anti-abuse](../anti-abuse.md). The default is `count: events`; each entry lists its supported quantity modes. See [counting modes](../counting.md).

## `block_break`

Counts blocks actually broken by the player, suitable for mining. Recorded player-placed blocks are excluded by default. Use `block_type` to select stone, ores, or other blocks.

| Available data | Value supplied by this event |
| --- | --- |
| Supported `count` | `events` |
| Event / payload | `BlockBreakEvent` |
| Availability | Paper and Spigot |
| PLAYER | Player associated with this event |
| SOURCE | Quest player |
| TARGET | Absent |
| LOCATION | Event block center |
| TRIGGER_ITEM / slot | Main hand / HAND |
| BLOCK | The broken block |
| Extra context | `original_experience` |

## `block_damage`

Counts the start of damaging a block. The block does not have to break, so use a break trigger when the task requires completed mining.

| Available data | Value supplied by this event |
| --- | --- |
| Supported `count` | `events` |
| Event / payload | `BlockDamageEvent` |
| Availability | Paper and Spigot |
| PLAYER | Player associated with this event |
| SOURCE | Quest player |
| TARGET | Absent |
| LOCATION | Event block center |
| TRIGGER_ITEM / slot | Event item / HAND |
| BLOCK | The block being damaged |

## `block_drop_item`

`events` adds one per block-drop event; `items` sums all output stacks. `match_item` + `item: COUNTED_ITEM` filters each output stack; the event item used by `conditions.match_item` remains the tool.

| Available data | Value supplied by this event |
| --- | --- |
| Supported `count` | `events`, `items` |
| Event / payload | `BlockDropItemEvent` |
| Availability | Paper and Spigot |
| PLAYER | Player associated with this event |
| SOURCE | Quest player |
| TARGET | Absent |
| LOCATION | Event block center |
| TRIGGER_ITEM / slot | Current main hand / HAND |
| BLOCK | Live reference to the broken block; its original state is in broken_block_state |
| Extra context | `broken_block_state` |

## `block_place`

`events` adds one per placement event; `blocks` counts the blocks involved, so a door can add two. In `blocks` mode, conditions are evaluated separately for each block and only matching blocks count.

| Available data | Value supplied by this event |
| --- | --- |
| Supported `count` | `events`, `blocks` |
| Event / payload | `BlockPlaceEvent` |
| Availability | Paper and Spigot |
| PLAYER | Player associated with this event |
| SOURCE | Quest player |
| TARGET | Absent |
| LOCATION | Event block center |
| TRIGGER_ITEM / slot | Placed item / event hand |
| BLOCK | The placed block |

## `block_break_progress_update`

Counts reported mining-progress updates, which do not necessarily mean a block was broken. Requires Paper mode and the corresponding event class.

| Available data | Value supplied by this event |
| --- | --- |
| Supported `count` | `events` |
| Event / payload | `BlockBreakProgressUpdateEvent` |
| Availability | Paper mode and event class required |
| PLAYER | Player associated with this event |
| SOURCE | Quest player |
| TARGET | Absent |
| LOCATION | Event block center |
| TRIGGER_ITEM / slot | Main hand / HAND |
| BLOCK | The block whose break progress changed |
| Extra context | `block_break_progress` |

## `interact`

Counts player interaction events. Air, block, and item interactions supply different data; block or item conditions need the corresponding data to exist.

| Available data | Value supplied by this event |
| --- | --- |
| Supported `count` | `events` |
| Event / payload | `PlayerInteractEvent` |
| Availability | Paper and Spigot |
| PLAYER | Player associated with this event |
| SOURCE | Quest player |
| TARGET | Absent |
| LOCATION | Clicked block center, otherwise player location |
| TRIGGER_ITEM / slot | Event item / event hand, both optional |
| BLOCK | The clicked block; absent for air clicks |

## `inside_block`

Counts the server event for a player being inside a block. It needs the corresponding Paper event and is not a general region-entry detector.

| Available data | Value supplied by this event |
| --- | --- |
| Supported `count` | `events` |
| Event / payload | `EntityInsideBlockEvent` |
| Availability | Paper mode and event class required |
| PLAYER | Player associated with this event |
| SOURCE | Quest player |
| TARGET | Absent |
| LOCATION | Event block center |
| TRIGGER_ITEM / slot | Absent |
| BLOCK | The event block containing the player |

## `vibration_receive`

Counts received game vibrations whose source entity is a player. The context location is the receiving block, which may differ from the player location.

| Available data | Value supplied by this event |
| --- | --- |
| Supported `count` | `events` |
| Event / payload | `BlockReceiveGameEvent` |
| Availability | Paper and Spigot |
| PLAYER | Player associated with this event |
| SOURCE | Quest player |
| TARGET | Quest player |
| LOCATION | Receiving block location |
| TRIGGER_ITEM / slot | Absent |

Confirm platform support and test with a new task. Extra-data names in the tables describe event data or extension APIs; they do not imply matching condition types exist.

[Trigger overview](../triggers.md) · [Event conditions](../conditions/README.md)
