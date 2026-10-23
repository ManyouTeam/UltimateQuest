# 🎲 Random rewards

`random` draws weighted groups and issues every reward in each selected group. For example, a player might receive bread or emeralds.

## Random reward example

Use this `rewards` section in the [normal task example](../tasks.md):

```yaml
rewards:
  supplies:
    type: random
    count: 1
    repeat: false
    random:
      food:
        rate: 3
        rewards:
          bread:
            type: item
            item: {material: BREAD, amount: 8}
      gems:
        rate: 1
        rewards:
          emerald:
            type: item
            item: {material: EMERALD, amount: 2}
```

This draws one group. Food has weight 3 and gems weight 1, giving probabilities of 75% and 25%. `rate` is a relative weight; weights do not need to total 100.

Each option under `random` needs its own `rewards` mapping. Selecting a group issues everything in it. A group containing bread and torches gives both.

## Draw count and repetition

| Field | Default | Requirement / behavior |
| --- | --- | --- |
| `count` | `1` | Positive integer number of groups to draw. |
| `repeat` | `false` | Without replacement when false; true allows repeated selections and payouts. |
| `random.<id>.rate` | `0` | Finite nonnegative number; zero is excluded, and the total must be finite and positive. |
| `random.<id>.rewards` | Required | Nonempty reward mapping. |
| `display.name`, `display.lore` | Generated | Preview overrides only. |

Changing the example to `count: 2` and `repeat: false` gives both groups. Two candidates cannot support three draws without replacement. Matching nested reward IDs in separate draws remain independent instead of overwriting earlier rewards.

A single draw can preview probabilities; multiple draws generally show weights. First-draw probability is not the final inclusion probability. Random groups support at most eight nesting levels. Invalid counts, types, weights, or references reject loading.

## When the result is determined

Normal tasks draw on acceptance, periods on assignment creation, scrolls on first activation, and progression stages on unlock. Reloads and reconnects do not redraw; see [saved rewards](README.md). This differs from click-time [random actions](../actions/orchestration.md).

Use an [external reward pool](pools.md) to share candidates between tasks. A random entry cannot contain both inline `random` and external `pool`.
