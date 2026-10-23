# 🔧 Change Item Format

Use `change-item` inside an ItemFormat section to change the item as it is created. Rules run in YAML order, so a later rule sees the result of an earlier one.

For example, this reward gives an Iron Pickaxe with an extra lore line and Efficiency I:

```yaml
rewards:
  pickaxe:
    type: item
    item:
      material: IRON_PICKAXE
      change-item:
        add-lore-last:
          - '&7Quest reward'
        modify-enchants:
          minecraft:efficiency: 1
```

The same format can be used in a saved item's `base-item.change-item`. See [Custom Items](../configs/custom-items.md).

The shared change rules follow the same format as our MythicChanger plugin. Refer to the [MythicChanger change-rule documentation](https://mythicchanger.superiormc.cn/change-rules/empty) for rules such as changing names and lore, replacing materials, repairing durability, and adding enchantments. NBT rules require NBTAPI; version-specific component rules require a compatible server.

This page describes `modify-enchants` in detail.

## `modify-enchants`

Changes the current levels of normal item enchantments without rebuilding or serializing the item.

### Additive shorthand

A scalar value is treated as a level delta:

```yaml
change-item:
  modify-enchants:
    minecraft:looting: -1
    minecraft:unbreaking: 2
```

For an item with Looting III and Unbreaking I, the result is Looting II and Unbreaking III.

The `minecraft:` namespace may be omitted for vanilla enchantments:

```yaml
change-item:
  modify-enchants:
    looting: -1
```

Custom enchantments use their complete namespaced key:

```yaml
change-item:
  modify-enchants:
    minecraft:sharpness: 1
```

### Operation form

Each enchantment may instead use an operation section:

```yaml
change-item:
  modify-enchants:
    minecraft:looting:
      operation: SUBTRACT
      value: 1

    minecraft:unbreaking:
      operation: MULTIPLY
      value: 2

    minecraft:sharpness:
      operation: SET
      value: 'min({current-level} + 1, 10)'
```

| Field       | Default | Description                                                             |
| ----------- | ------- | ----------------------------------------------------------------------- |
| `operation` | `ADD`   | `ADD`, `SUBTRACT`, `MULTIPLY`, or `SET`. Unknown values use `ADD`.      |
| `value`     | `0`     | Operand. Supports mathematical expressions and ChangeItem placeholders. |

### Field-specific placeholders

| Placeholder       | Meaning                                                                           |
| ----------------- | --------------------------------------------------------------------------------- |
| `{current-level}` | The enchantment level currently stored on the item. Missing enchantments use `0`. |
| `{max-level}`     | The registered maximum level of that enchantment.                                 |

### Behavior and limits

* The calculated result is rounded to the nearest integer.
* A result less than or equal to `0` removes the enchantment.
* A positive result adds or replaces the enchantment using unsafe level handling, so compatibility and vanilla maximum-level restrictions are not enforced.
* A missing enchantment starts at level `0`; a positive additive result may therefore add it.
* Unknown enchantment keys are skipped.
* Invalid or non-finite expressions are reported and skipped instead of removing the enchantment.
* This rule changes normal item enchantments. Stored enchantments on enchanted books are not affected.

### Decrease one level and remove at zero

```yaml
change-item:
  modify-enchants:
    minecraft:looting: -1
```

| Before      | After               |
| ----------- | ------------------- |
| Looting III | Looting II          |
| Looting II  | Looting I           |
| Looting I   | Enchantment removed |
| No Looting  | No change           |
