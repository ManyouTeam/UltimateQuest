# 🎒 Item conditions

Use these conditions to decide which events contribute to tasks. Each `conditions` example is a fragment for an objective’s `source` or a template root. Only player checks that need no event data belong at the task root for acceptance. See [condition basics](README.md).

## `match_item`

Matches an event item or equipment. The example checks the quest owner’s main-hand tool. `CONTEXT` means the event item, perhaps consumed food or a trade result, not necessarily the main hand. Air and missing items do not match.

| Field | Default | Description |
| --- | --- | --- |
| `item` | CONTEXT | Scalar selector, or section with selector/holder. |
| `item-holder` | TARGET | Holder for scalar equipment selection. |
| `match` | current section | MatchItemFormat rules. |

```yaml
conditions:
  tool:
    type: match_item
    item:
      selector: MAIN_HAND
      holder: PLAYER
    match:
      material:
      - IRON_PICKAXE
      - DIAMOND_PICKAXE
```

Item selectors: `COUNTED_ITEM`, `CONTEXT`, `TRIGGER_ITEM`, `MAIN_HAND`, `OFF_HAND`, `HELMET`, `CHESTPLATE`, `LEGGINGS`, `BOOTS`, `ARMOR`, `ALL_EQUIPMENT`. Any matching item in an equipment group passes. See [item matching](../../shared-formats/match-item-format/README.md).

`COUNTED_ITEM` selects the output stack currently being counted. With `count: items`, conditions run separately for each catch, drop, or trade output and sum only matching quantities. It does not match outside that evaluation. Tool and equipment selectors retain their meaning. See [counting modes](../counting.md).

## `catch_match_item`

Filters items caught through fishing. It requires an item entity as the catch, excluding empty casts and hooked creatures. Combine with `trigger: fish` and `count: items` to count caught stack quantities.

| Field | Default | Description |
| --- | --- | --- |
| `match` | current section | MatchItemFormat for the caught stack. |

```yaml
conditions:
  fish:
    type: catch_match_item
    match:
      material:
      - COD
      - SALMON
      - TROPICAL_FISH
      - PUFFERFISH
```
