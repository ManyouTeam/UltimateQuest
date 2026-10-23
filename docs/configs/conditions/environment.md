# 🌍 Environment and block conditions

Use these conditions to decide which events contribute to tasks. Each `conditions` example is a fragment for an objective’s `source` or a template root. Only player checks that need no event data belong at the task root for acceptance. See [condition basics](README.md).

## `world`

Checks the world of the event location. An event at a target or block can differ from the player’s current world. Use [player conditions](player-conditions.md) to always check the player’s world. World-name comparison normalizes case.

| Field | Default | Description |
| --- | --- | --- |
| `worlds / world` | — | World-name list or scalar; case-normalized. |

```yaml
conditions:
  overworld:
    type: world
    worlds:
    - world
```

## `block_type`

Matches an event block using vanilla or supported custom-block hooks. It prefers the saved pre-break state, otherwise the current context block. Missing block data fails. Matching wheat material alone does not check maturity.

| Field | Default | Description |
| --- | --- | --- |
| `types / type-name` | — | Block identifiers, list or scalar. |

```yaml
conditions:
  stone:
    type: block_type
    types:
    - STONE
```

## `night`

Requires night in the SOURCE living entity’s world: game time 12300–23850 inclusive. It does not support changing `target`; a missing SOURCE is not replaced with the player.

| Field | Default | Description |
| --- | --- | --- |
| `value` | true | Expected boolean state. |

```yaml
conditions:
  dark:
    type: night
    value: true
```

## `storm`

Checks for a storm in the event location’s world. `value: false` requires no storm. Missing location or world fails.

| Field | Default | Description |
| --- | --- | --- |
| `value` | true | Expected boolean state. |

```yaml
conditions:
  rain:
    type: storm
    value: true
```
