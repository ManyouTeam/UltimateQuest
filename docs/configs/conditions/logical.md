# 🔀 Logical conditions

Use these conditions to decide which events contribute to tasks. Each `conditions` example is a fragment for an objective’s `source` or a template root. Only player checks that need no event data belong at the task root for acceptance. See [condition basics](README.md).

## `any`

Passes when any nested condition passes. The example allows being in water or stormy weather. Supply children: an absent `conditions` node passes, while an explicitly empty mapping has no matching child and fails.

| Field | Default | Description |
| --- | --- | --- |
| `conditions` | — | Named event-condition mapping. |

```yaml
conditions:
  choice:
    type: any
    conditions:
      water:
        type: in_water
        target: PLAYER
      rain:
        type: storm
```

## `not`

Inverts the combined nested result. `not(A AND B)` fails only when both A and B pass. To invert a single condition, its common `not: true` field is usually clearer.

| Field | Default | Description |
| --- | --- | --- |
| `conditions` | — | Named event-condition mapping. |

```yaml
conditions:
  outside:
    type: not
    conditions:
      water:
        type: in_water
        target: PLAYER
```
