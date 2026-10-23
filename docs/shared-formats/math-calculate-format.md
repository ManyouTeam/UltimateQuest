# ➗ Math Calculate Format

When `config.yml -> math.enabled` is true, supported numeric fields can use expressions instead of fixed numbers. For example, an item-change rule can calculate how much durability to repair:

```yaml
change-item:
  repair-damage: 'min({max-damage}, 50)'
```

This repairs up to 50 durability, limited by the item's maximum durability.

## Configuration

```yaml
math:
  enabled: true
  enable-function: true
```

With `enabled: false`, use plain numbers. `enable-function` controls whether the functions below are available.

## Arithmetic and functions

Use expressions such as `'10 + 5'`, `'20 * 0.5'`, and `'min(100, 25 * 3)'`. Quote expressions in YAML.

| Function | Example | Result |
| --- | --- | --- |
| `sum` | `sum(1, 2, 3)` | `6` |
| `max` | `max(5, 10)` | `10` |
| `min` | `min(5, 10)` | `5` |
| `avg` | `avg(2, 4)` | `3` |
| `floor` | `floor(2.8)` | `2` |
| `ceil` | `ceil(2.1)` | `3` |
| `round` | `round(2.6)` | `3` |
| `random` | `random(1, 5)` | A random value within the given bounds. |

The shared expression format is also used by UltimateShop. Its [format documentation](https://ultimateshop.superiormc.cn/format) gives more background; the fields supported by UltimateQuest are described below.

## Where expressions can be used

Expressions are supported by item-change numeric rules and supported numeric menu-action parameters, such as particle offsets. Each rule may provide its own placeholders. For example, durability rules provide `{damage}` and `{max-damage}`, while `modify-enchants` provides `{current-level}` and `{max-level}`.

Task objective amounts, milestone thresholds, objective-pool weights, and reward counts require the integer or range format shown on their configuration pages. Do not replace these with formulas. ItemFormat's `amount` accepts a fixed integer or an inclusive range such as `2~5`.

## Placeholders

Use only placeholders supplied by the field you are editing. A placeholder that is not available there cannot be used as a number. In particular, `{level}` in a shared item-change rule does not represent the player's battle-pass level.

For example, this item change increases Sharpness by one, up to its registered maximum level:

```yaml
change-item:
  modify-enchants:
    minecraft:sharpness:
      operation: SET
      value: 'min({current-level} + 1, {max-level})'
```

See [Change Item Format](change-item-rules.md) for the complete rule example.
