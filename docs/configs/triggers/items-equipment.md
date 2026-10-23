# 🎒 Items and equipment

Choose an ID for the activity, then check which data it supplies. `PLAYER` is the player gaining task progress. The identities of `SOURCE` and `TARGET` vary by event; they are not always the player and a mob.

## Configuration paths

Replace `source` in the [normal task tutorial](../tasks.md) with this single-objective fragment. For several objectives, put it under `objectives.<id>.source`. Templates use `trigger` and `amount` directly.

```yaml
source:
  trigger: consume
  amount: 10
```

This requires ten matching events. Some types fire frequently; combine the explanations below with conditions and [anti-abuse](../anti-abuse.md). The default is `count: events`; each entry lists its supported quantity modes. See [counting modes](../counting.md).

## `consume`

Counts item consumption such as eating or drinking. The context item is the consumed stack, so it can be matched without guessing the hand.

| Available data | Value supplied by this event |
| --- | --- |
| Supported `count` | `events` |
| Event / payload | `PlayerItemConsumeEvent` |
| Availability | Paper and Spigot |
| PLAYER | Player associated with this event |
| SOURCE | Quest player |
| TARGET | Absent |
| LOCATION | Player location |
| TRIGGER_ITEM / slot | Consumed stack / event hand |

## `enchant_item`

Counts enchanting-table enchants, not anvil combinations. Extra `original_experience` is the displayed level cost; `enchantment_level_cost` and `original_lapis` are the selected button index plus one, usually 1–3. These do not become progress increments.

| Available data | Value supplied by this event |
| --- | --- |
| Supported `count` | `events` |
| Event / payload | `EnchantItemEvent` |
| Availability | Paper and Spigot |
| PLAYER | Player associated with this event |
| SOURCE | Quest player |
| TARGET | Absent |
| LOCATION | Player location |
| TRIGGER_ITEM / slot | Enchanted stack / HAND |
| Extra context | `original_experience`, `enchantment_level_cost`, `original_lapis` |

## `item_damage`

`events` adds one per durability event; `durability` sums event damage capped by remaining durability. Unbreakable items add no durability progress; held stack sizes are not multiplied into wear.

| Available data | Value supplied by this event |
| --- | --- |
| Supported `count` | `events`, `durability` |
| Event / payload | `PlayerItemDamageEvent` |
| Availability | Paper and Spigot |
| PLAYER | Player associated with this event |
| SOURCE | Quest player |
| TARGET | Absent |
| LOCATION | Player location |
| TRIGGER_ITEM / slot | Damaged item / resolved slot |
| Extra context | `original_item_damage` |

## `item_held`

Counts changes to the selected hotbar slot; actually using the item is not required.

| Available data | Value supplied by this event |
| --- | --- |
| Supported `count` | `events` |
| Event / payload | `PlayerItemHeldEvent` |
| Availability | Paper and Spigot |
| PLAYER | Player associated with this event |
| SOURCE | Quest player |
| TARGET | Absent |
| LOCATION | Player location |
| TRIGGER_ITEM / slot | New selected hotbar item / HAND |

## `swap_hand`

Counts main/offhand swaps. The context item is the new main-hand item, so match it accordingly.

| Available data | Value supplied by this event |
| --- | --- |
| Supported `count` | `events` |
| Event / payload | `PlayerSwapHandItemsEvent` |
| Availability | Paper and Spigot |
| PLAYER | Player associated with this event |
| SOURCE | Quest player |
| TARGET | Absent |
| LOCATION | Player location |
| TRIGGER_ITEM / slot | Event new main-hand item / HAND |

## `item_group_cooldown`

Counts item or item-group cooldown changes. A concrete context item is supplied only when resolvable, so an item condition may have nothing to match.

| Available data | Value supplied by this event |
| --- | --- |
| Supported `count` | `events` |
| Event / payload | `PlayerItemGroupCooldownEvent` |
| Availability | Paper mode and event class required |
| PLAYER | Player associated with this event |
| SOURCE | Quest player |
| TARGET | Absent |
| LOCATION | Player location |
| TRIGGER_ITEM / slot | Matching held item only when the material is exposed |

## `name_entity`

Counts players naming entities. TARGET conditions can filter the named entity.

| Available data | Value supplied by this event |
| --- | --- |
| Supported `count` | `events` |
| Event / payload | `PlayerNameEntityEvent` |
| Availability | Paper mode and event class required |
| PLAYER | Player associated with this event |
| SOURCE | Quest player |
| TARGET | Named entity |
| LOCATION | Target/entity location |
| TRIGGER_ITEM / slot | Held NAME_TAG if found |

Confirm platform support and test with a new task. Extra-data names in the tables describe event data or extension APIs; they do not imply matching condition types exist.

[Trigger overview](../triggers.md) · [Event conditions](../conditions/README.md)
