# 🧍 实体与状态条件

本页用于筛选哪些行为可以计入任务。下面的 `conditions` 示例都是片段：放入目标的 `source` 内，或目标模板根节点；只有不依赖事件数据的玩家条件才适合放在任务根节点检查接取资格。参见[条件基础](README.md)。

## `sneaking`

要求选中的玩家正在潜行。检查做任务的人时用 `target: PLAYER`；`value: false` 则要求没有潜行。该条件只适用于玩家。

| 字段 | 默认值 | 说明 |
| --- | --- | --- |
| `target` | TARGET | 检查任务玩家请填写 PLAYER。 |
| `value` | true | 预期布尔状态。 |

```yaml
conditions:
  sneaking:
    type: sneaking
    target: PLAYER
    value: true
```

缺少目标或目标类型不适用时先判为不满足，再处理公共 `not` 反转；不要用反转代替“目标是否存在”的检查。

## `not_sneaking`

要求选中的玩家没有潜行。这是“不潜行”的专用写法，同样需要明确选中玩家。

| 字段 | 默认值 | 说明 |
| --- | --- | --- |
| `target` | TARGET | 检查任务玩家请填写 PLAYER。 |

```yaml
conditions:
  not_sneaking:
    type: not_sneaking
    target: PLAYER
```

缺少目标或目标类型不适用时先判为不满足，再处理公共 `not` 反转；不要用反转代替“目标是否存在”的检查。

## `on_ground`

要求选中的生物处于地面，`value: false` 要求不在地面。它检查生物状态，不检查脚下是否为某种材质。

| 字段 | 默认值 | 说明 |
| --- | --- | --- |
| `target` | TARGET | 检查任务玩家请填写 PLAYER。 |
| `value` | true | 预期布尔状态。 |

```yaml
conditions:
  on_ground:
    type: on_ground
    target: PLAYER
    value: true
```

缺少目标或目标类型不适用时先判为不满足，再处理公共 `not` 反转；不要用反转代替“目标是否存在”的检查。

## `in_air`

要求选中实体不在地面。并不要求玩家拥有飞行权限，也不代表玩家正在使用鞘翅。

| 字段 | 默认值 | 说明 |
| --- | --- | --- |
| `target` | TARGET | 检查任务玩家请填写 PLAYER。 |
| `value` | true | 预期布尔状态。 |

```yaml
conditions:
  in_air:
    type: in_air
    target: PLAYER
    value: true
```

缺少目标或目标类型不适用时先判为不满足，再处理公共 `not` 反转；不要用反转代替“目标是否存在”的检查。

## `in_water`

要求选中实体处于水中。只限制任务玩家时写 `target: PLAYER`，不要依赖默认 TARGET。

| 字段 | 默认值 | 说明 |
| --- | --- | --- |
| `target` | TARGET | 检查任务玩家请填写 PLAYER。 |
| `value` | true | 预期布尔状态。 |

```yaml
conditions:
  in_water:
    type: in_water
    target: PLAYER
    value: true
```

缺少目标或目标类型不适用时先判为不满足，再处理公共 `not` 反转；不要用反转代替“目标是否存在”的检查。

## `in_lava`

检查选中实体脚下位置的方块是否为 `LAVA`。这是位置方块检查，不等于“正在燃烧”。

| 字段 | 默认值 | 说明 |
| --- | --- | --- |
| `target` | TARGET | 检查任务玩家请填写 PLAYER。 |
| `value` | true | 预期布尔状态。 |

```yaml
conditions:
  in_lava:
    type: in_lava
    target: PLAYER
    value: true
```

缺少目标或目标类型不适用时先判为不满足，再处理公共 `not` 反转；不要用反转代替“目标是否存在”的检查。

## `game_mode`

只允许指定游戏模式，例如生存和冒险。目标默认可能是怪物或不存在，因此检查任务玩家时显式使用 PLAYER。

| 字段 | 默认值 | 说明 |
| --- | --- | --- |
| `target` | TARGET | 检查任务玩家请填写 PLAYER。 |
| `modes / mode` | — | 模式名称列表或标量。 |

```yaml
conditions:
  survival:
    type: game_mode
    target: PLAYER
    modes:
    - SURVIVAL
    - ADVENTURE
```

## `has_potion`

检查选中生物当前已经拥有的药水效果。`require-min-effects: 1` 表示列表中至少一种生效，不要求列表中的全部效果。也可用 ALL、BENEFICIAL、HARMFUL、NEUTRAL 选择类别；未知选择器不匹配。它不直接读取药水变化事件中尚未应用的效果。

| 字段 | 默认值 | 说明 |
| --- | --- | --- |
| `target` | TARGET | 检查任务玩家请填写 PLAYER。 |
| `potions` | [] | 效果键，或 ALL/BENEFICIAL/HARMFUL/NEUTRAL。 |
| `require-min-effects` | 1 | 最少匹配种类数，至少为 1。 |

```yaml
conditions:
  boosted:
    type: has_potion
    target: PLAYER
    potions:
    - minecraft:speed
    - minecraft:haste
    require-min-effects: 1
```

## `match_entity`

筛选选中的生物，例如只统计击杀僵尸。完整规则见[实体匹配格式](../../shared-formats/match-entity-format/README.md)。非生物目标不能通过。生命值规则使用 `entity-health`；没有直接名为 `health_percent` 的事件条件。

| 字段 | 默认值 | 说明 |
| --- | --- | --- |
| `target` | TARGET | 检查任务玩家请填写 PLAYER。 |
| `match` | 当前条件节点 | 实体匹配规则，默认当前节点。 |

```yaml
conditions:
  zombie:
    type: match_entity
    target: TARGET
    match:
      entity-types:
      - ZOMBIE
```
