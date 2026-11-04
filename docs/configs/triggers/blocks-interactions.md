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
| Availability | Paper and Spigot |
| PLAYER | Player associated with this event |
| SOURCE | Quest player |
| TARGET | Absent |
| LOCATION | Event block center |
| TRIGGER_ITEM / slot | Main hand / HAND |
| BLOCK | The broken block |

## `block_damage`

Counts the start of damaging a block. The block does not have to break, so use a break trigger when the task requires completed mining.

| Available data | Value supplied by this event |
| --- | --- |
| Supported `count` | `events` |
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
| Availability | Paper and Spigot |
| PLAYER | Player associated with this event |
| SOURCE | Quest player |
| TARGET | Absent |
| LOCATION | Event block center |
| TRIGGER_ITEM / slot | Current main hand / HAND |
| BLOCK | Live reference to the broken block; its original state is in broken_block_state |

## `block_place`

`events` adds one per placement event; `blocks` counts the blocks involved, so a door can add two. In `blocks` mode, conditions are evaluated separately for each block and only matching blocks count.

| Available data | Value supplied by this event |
| --- | --- |
| Supported `count` | `events`, `blocks` |
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
| Availability | Paper mode and event class required |
| PLAYER | Player associated with this event |
| SOURCE | Quest player |
| TARGET | Absent |
| LOCATION | Event block center |
| TRIGGER_ITEM / slot | Main hand / HAND |
| BLOCK | The block whose break progress changed |

## `interact`

Counts player interaction events. Air, block, and item interactions supply different data; block or item conditions need the corresponding data to exist.

| Available data | Value supplied by this event |
| --- | --- |
| Supported `count` | `events` |
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
| Availability | Paper and Spigot |
| PLAYER | Player associated with this event |
| SOURCE | Quest player |
| TARGET | Quest player |
| LOCATION | Receiving block location |
| TRIGGER_ITEM / slot | Absent |

## `brew`

Supported `count`: `events`, `items`. Runs on brewing completion, using the resulting potions and excluding unchanged bottles. `events` counts each batch once; `items` counts matching output quantities. Use `count: items` and `COUNTED_ITEM` matching to select specific potions. `TRIGGER_ITEM` is the first changed output; its slot is absent.

## `cook`

Supported `count`: `events`, `items`. Runs on furnace smelting completion, including furnaces, smokers, and blast furnaces. `events` counts each completion once; `items` counts matching output quantities. Use `COUNTED_ITEM` matching for food such as `COOKED_CHICKEN`; without filtering, other smelting outputs also count. `TRIGGER_ITEM` is the output, with no slot. Collecting existing output does not trigger it again. Campfires are excluded.

### Player attribution for brewing and cooking

Completion events have no player. After a click, shift-click, or drag, the plugin checks input slots and attributes the workstation to the player who actually added input. Opening the interface, adding fuel, or collecting output does not transfer attribution. Brewing inputs include bottle and ingredient slots; furnace input is its smelting slot.

The player's UUID is stored in the workstation PDC and survives restarts, without a long-lived player-object cache. Empty input slots clear attribution, as does automated inventory input such as a hopper. Events without a recorded online operator and cancelled events do not count. Shared workstations credit the last player who actually added input; individual item ownership is not tracked. `PLAYER` / `SOURCE` is the operator; `BLOCK` / `LOCATION` is the workstation. Available on Paper and Spigot.


## `craft_item`

Supported `count`: `events`, `items`. Runs when crafted output is successfully taken. `events` counts each crafting click once; `items` counts outputs, including shift crafting constrained by inventory capacity. Creative cloning does not count.

## `fertilize_block`

Supported `count`: `events`, `blocks`. Runs when player-applied bone meal successfully changes blocks. `events` counts uses; `blocks` counts changed blocks. `BLOCK` is the fertilized target; the original interaction supplies the item and hand. Non-player events do not count.

## `forge_item`

Supported `count`: `events`, `items`. Runs when output is successfully taken from a smithing table, including upgrades and trims. Previewing output does not count.

## `grindstone_item`

Supported `count`: `events`, `items`. Runs when output is successfully taken from a grindstone, including repairs and disenchanting. Inserting items or viewing the preview does not count.

## `milk_mob`

Supported `count`: `events`, `items`. Runs on a successful milk-bucket fill and supplies the interacted cow, mooshroom, or goat as `TARGET`. Failed, cancelled, and baby-animal interactions do not count.

## `honey_extract`

Supported `count`: `events`, `items`. Runs when a glass bottle harvests a full bee nest or beehive. A next-tick check confirms reduced honey and the bottle-use result. The trigger item is the glass bottle; output is a honey bottle.

## `honey_comb_extract`

Supported `count`: `events`, `items`. Runs when shears harvest honeycomb from a bee nest or beehive. Outputs come from the Paper shear-block event. Requires Paper with that event available.

## `anvil_repair`

Supported `count`: `events`, `items`. Runs when successfully collected anvil output has less durability damage. Renaming alone does not trigger it.

## `anvil_enchant`

Supported `count`: `events`, `items`. Runs when successfully collected anvil output adds an enchantment or increases its level, including enchanted-book merging. Unchanged enchantments do not count.

## `anvil_rename`

Supported `count`: `events`, `items`. Runs when successfully collected anvil output changes the custom name, including adding, editing, or clearing a name. Repairing or enchanting without a name change does not count.

Use `COUNTED_ITEM` to filter quantity-counted outputs. Smithing, grindstone, and anvil trigger items are outputs. One anvil action can independently trigger repair, enchant, and rename according to its actual changes. Inventory-output triggers check consumed inputs on the next tick; cancelled or unsuccessful collection does not count. Honey-bottle triggering happens after completion and cannot cancel the original harvest. All entries above except honeycomb support Paper and Spigot.

[Trigger overview](../triggers.md) · [Event conditions](../conditions/README.md)
