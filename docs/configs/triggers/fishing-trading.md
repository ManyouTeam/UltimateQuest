# 🎣 Fishing and trading

Choose an ID for the activity, then check which data it supplies. `PLAYER` is the player gaining task progress. The identities of `SOURCE` and `TARGET` vary by event; they are not always the player and a mob.

## Configuration paths

Replace `source` in the [normal task tutorial](../tasks.md) with this single-objective fragment. For several objectives, put it under `objectives.<id>.source`. Templates use `trigger` and `amount` directly.

```yaml
source:
  trigger: fish
  amount: 10
```

This requires ten matching events. Some types fire frequently; combine the explanations below with conditions and [anti-abuse](../anti-abuse.md). The default is `count: events`; each entry lists its supported quantity modes. See [counting modes](../counting.md).

## `fish`

The normal event route handles successful catches only. `events` adds one per successful catch; `items` sums caught item stack sizes. Empty catches and hooked entities add no item progress. Use `match_item` + `item: COUNTED_ITEM` to select fish types.

| Available data | Value supplied by this event |
| --- | --- |
| Supported `count` | `events`, `items` |
| Event / payload | `PlayerFishEvent` |
| Availability | Paper and Spigot |
| PLAYER | Player associated with this event |
| SOURCE | Quest player |
| TARGET | Caught entity, if any |
| LOCATION | Fishing hook location |
| TRIGGER_ITEM / slot | Fishing rod / resolved main or off hand |
| Extra context | `original_experience` |

## `piglin_barter`

`events` adds one per attributed barter; `items` sums output quantities. `match_item` + `item: COUNTED_ITEM` selects outputs; the event item remains the barter input.

| Available data | Value supplied by this event |
| --- | --- |
| Supported `count` | `events`, `items` |
| Event / payload | `PiglinBarterEvent` |
| Availability | Paper and Spigot |
| PLAYER | Player associated with this event |
| SOURCE | Bartering piglin |
| TARGET | Bartering piglin |
| LOCATION | Target/entity location |
| TRIGGER_ITEM / slot | Barter input / HAND |

## `trade`

`events` adds one per trade event; `items` uses the result stack size for that event. Bulk trading accumulates the individual events emitted by the server.

| Available data | Value supplied by this event |
| --- | --- |
| Supported `count` | `events`, `items` |
| Event / payload | `PlayerTradeEvent` |
| Availability | Paper mode and event class required |
| PLAYER | Player associated with this event |
| SOURCE | Quest player |
| TARGET | Merchant entity |
| LOCATION | Merchant location |
| TRIGGER_ITEM / slot | Trade result / HAND |

## `purchase`

`events` adds one per purchase event; `items` uses the result stack size. Item counting also works with virtual merchants without an entity TARGET.

| Available data | Value supplied by this event |
| --- | --- |
| Supported `count` | `events`, `items` |
| Event / payload | `PlayerPurchaseEvent` |
| Availability | Paper mode and event class required |
| PLAYER | Player associated with this event |
| SOURCE | Quest player |
| TARGET | Merchant only when represented by an Entity |
| LOCATION | Entity merchant location if available, otherwise player |
| TRIGGER_ITEM / slot | Trade result / HAND |

Confirm platform support and test with a new task. Extra-data names in the tables describe event data or extension APIs; they do not imply matching condition types exist.

[Trigger overview](../triggers.md) · [Event conditions](../conditions/README.md)
