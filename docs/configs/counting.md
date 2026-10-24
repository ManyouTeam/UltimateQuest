# 🔢 Counting modes

`count` controls how much progress a matching event contributes. The default `events` adds one. Quantity modes read a supported event quantity. Normal objective `amount` remains a positive integer, while progress may include fractions.

## Supported modes

All built-in triggers support `events`. The table lists additional modes. Unlisted triggers support only `events`; each trigger reference also lists its allowed values.

| `count` | Supported triggers | Quantity |
| --- | --- | --- |
| `items` | `fish`, `block_drop_item`, `piglin_barter`, `trade`, `purchase`, `kill` | Sum of caught, dropped, or output item stack quantities. |
| `blocks` | `block_place` | Blocks involved in the placement, with conditions evaluated per block. |
| `damage` | `attack`, `melee_attack`, `damage`, `damage_by_entity` | Final event damage after modifiers such as armor; not capped by remaining health. |
| `blocked_damage` | `shield_block` | Damage actually blocked by the shield. |
| `healing` | `regain` | Positive healing capped by missing health. |
| `durability` | `item_damage` | Event wear capped by remaining item durability. |
| `experience` | `exp_change`, `pickup_experience` | Positive Minecraft XP points, not levels. |
| `distance` | `move` | Three-dimensional straight-line distance between event positions, in blocks. |
| `exhaustion` | `exhaustion` | Positive exhaustion amounts. |

`death`, `block_break`, `air_change`, `food_level_change`, and `warden_anger_change` still support only `events`. An extra event value does not automatically make it a supported counting mode. For example, `block_break + items` is rejected; use `block_drop_item + items` for mining outputs.

## Examples

Use this normal-task source to require 100 points of final attack damage:

```yaml
source:
  name: 'Deal 100 damage'
  trigger: attack
  count: damage
  amount: 100
```

Count 64 diamonds produced by mining, including multiple diamonds from Fortune:

```yaml
source:
  name: 'Mine 64 diamonds'
  trigger: block_drop_item
  count: items
  amount: 64
  conditions:
    diamond:
      type: match_item
      item: COUNTED_ITEM
      match:
        material: [DIAMOND]
```

Use a normal `conditions.match_item` rule with `item: COUNTED_ITEM`. Conditions evaluate each output stack, and only passing quantities contribute. `match` still uses [MatchItemFormat](../shared-formats/match-item-format/README.md). Without an item condition, all outputs contribute. Existing selectors such as `CONTEXT` and `TRIGGER_ITEM` retain their meaning, including the tool in block-drop events. `COUNTED_ITEM` exists only during per-item condition evaluation for `count: items`; it does not match elsewhere.

Item filters now belong to `conditions`. The old `count-items` field is ignored without a separate error. UQ `amount` is the cumulative objective requirement; ER `count-every` defines repeatable batch size. They serve different purposes.

## Fractions and multipliers

Damage, healing, distance, and exhaustion may be fractional. Counting and storage use six decimal places with half-up rounding. Five hits worth 0.2 each add up to 1, and saved fractions survive reconnects and restarts. Menus and related PlaceholderAPI values include fractions without adding `.0` to integers.

Milestones multiply the counted quantity by the positive integer `value`. Battle pass challenges follow normal objective rules and award their configured points on completion. `experience` does not directly convert Minecraft XP into battle pass points.

## Event scope and anti-abuse

Cancelled events and zero or negative quantities add no quantity progress. Empty fishing outputs do not fall back to one item. Drops use the event output list and do not require an inventory pickup.

`move + distance` includes movement within a block and excludes rotation, teleports, and cross-world jumps. `move + events` retains its existing block-coordinate rule. For sprinting or swimming distance, use `move + distance` with a `sprinting` or `swimming` event condition; movement-state restrictions are described in [movement and input](triggers/movement-input.md).

[Anti-abuse](anti-abuse.md) applies once per event. Events skipped during a cooldown do not contribute delayed distance or other quantities. Multiple outputs or blocks in one event share one anti-abuse check. `exp_change` and `pickup_experience` may describe the same XP gain; avoid using both sources unless both contributions are intended.

## Progress saves

Omitted `count` still means event counting. Unsupported modes report the allowed values during loading; correct the configuration and run `/uq reload`.

Saves retain the integer fields and store fractional remainders separately. Existing Java progress methods returning `long` continue to expose the integer part. Use `QuestInstance.progressAmount` or `totalAmount` for fractional values. Remainder fields do not need manual editing.
