# 🧾 Player conditions

Use these conditions to decide which events contribute to tasks. Each `conditions` example is a fragment for an objective’s `source` or a template root. Only player checks that need no event data belong at the task root for acceptance. See [condition basics](README.md).

## `player_conditions`

Checks a player’s permission, world, biome, or placeholder. Wrap general player conditions inside `type: player_conditions`. `target: PLAYER` selects the quest owner; the default SOURCE is not necessarily a player.

| Field | Default | Description |
| --- | --- | --- |
| `target` | SOURCE | Player selector; PLAYER explicitly selects the quest owner. |
| `conditions` | — | Named general player-condition entries. |

```yaml
conditions:
  allowed:
    type: player_conditions
    target: PLAYER
    conditions:
      permission:
        type: permission
        permission: ultimatequest.quest.miner
```

### Available nested types

| type | Fields | Behavior |
| --- | --- | --- |
| `permission` | permission | The selected player must have the node. |
| `world` | world | Exact player world name. |
| `biome` | biome | Player biome name. |
| `placeholder` | placeholder, rule, value | Compare parsed text or numbers. |
| `any` | conditions | Any nested general condition passes. |
| `not` | conditions | Invert the combined nested result. |

These types belong inside the wrapper, not directly as event conditions. Numeric placeholder comparisons use `>=`, `>`, `=`, `<`, `<=`. Text comparisons use `==`, `!=`, `*=` (left contains right), `=*` (right contains left), `!*=`, and `!=*`. Supply placeholder, rule, and value, then test both passing and failing players.
