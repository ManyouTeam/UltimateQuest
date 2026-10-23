# 🎲 Objective pools

An objective pool lists candidate templates and their draw weights. Periodic tasks and scrolls can draw from it to vary their goals. It selects objectives, not rewards.

## Objective pool example

Create these three files under `plugins/UltimateQuest/`. The first two are [objective templates](objectives.md); the last is the pool.

```yaml
# quests/tutorial_stone.yml
enabled: true
name: 'Stone'
trigger: block_break
amount: 32
conditions:
  stone:
    type: block_type
    types: [STONE]
```

```yaml
# quests/tutorial_zombie.yml
enabled: true
name: 'Zombies'
trigger: kill
amount: 5
conditions:
  zombie:
    type: match_entity
    target: TARGET
    match:
      entity-types: [ZOMBIE]
```

```yaml
# pools/tutorial_pool.yml
enabled: true
quests:
  tutorial_stone: 3
  tutorial_zombie: 1
```

The stone objective has weight 3 and the zombie objective weight 1. A single draw has a 75% chance of choosing stone and a 25% chance of choosing zombies. Weights do not need to total 100.

## Reference it from a period or scroll

Replace root `source` in the [periodic task example](periods.md) or [scroll example](scrolls.md) with:

```yaml
source:
  pool: tutorial_pool
  count: 1
```

Run `/uq reload` and test a newly generated assignment or newly issued scroll. `count: 1` draws one objective. Using `2` draws both different objectives because objective pools draw without replacement.

A period creates two independent assignments, each with its own rewards. A scroll combines both objectives into one item and gives one reward set after both are complete.

Periodic tasks also support `source.categories` instead of `source.count` to draw a separate quota from each objective category. See [category draws](periods.md#draw-by-category). The bundled daily, weekly, and monthly pools each contain 100 candidates; a pool's size does not change how many assignments a player receives.

## Configuration limits

| Setting | Requirement |
| --- | --- |
| File location | `pools/<pool-id>.yml`; only direct-child `.yml` files are read. |
| Keys under `quests` | Existing enabled objective template IDs, not normal task IDs. |
| Weights | Positive integers, not zero, negatives, or fractions; the total must fit a signed 64-bit integer. |
| `source.count` | Defaults to `1`; integer from `1–45`, no greater than the candidate count. |

Unknown templates, invalid weights, or too few candidates cause loading to fail. Reconnecting does not change saved draws. For random item payouts, use [reward pools](rewards/pools.md) instead.
