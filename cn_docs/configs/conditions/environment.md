# 🌍 环境与方块条件

本页用于筛选哪些行为可以计入任务。下面的 `conditions` 示例都是片段：放入目标的 `source` 内，或目标模板根节点；只有不依赖事件数据的玩家条件才适合放在任务根节点检查接取资格。参见[条件基础](README.md)。

## `world`

检查事件位置所在的世界。事件发生在目标或方块处时，这可能不是玩家当前世界；要始终检查玩家世界，使用[玩家条件](player-conditions.md)。世界名比较会标准化大小写。

| 字段 | 默认值 | 说明 |
| --- | --- | --- |
| `worlds / world` | — | 世界名称列表或标量，比较时标准化大小写。 |

```yaml
conditions:
  overworld:
    type: world
    worlds:
    - world
```

## `block_type`

筛选事件中的方块，支持原版及已接入的自定义方块。存在破坏前状态时优先读取它，否则读取当前事件方块。没有方块数据会失败；只匹配小麦材质不会额外检查成熟度。

| 字段 | 默认值 | 说明 |
| --- | --- | --- |
| `types / type-name` | — | 方块标识列表或标量。 |

```yaml
conditions:
  stone:
    type: block_type
    types:
    - STONE
```

## `night`

要求 SOURCE 生物所在世界处于夜间，范围为游戏时间 12300–23850 tick，包含边界。它不支持 `target` 切换；SOURCE 缺失时不能按玩家时间替代。

| 字段 | 默认值 | 说明 |
| --- | --- | --- |
| `value` | true | 预期布尔状态。 |

```yaml
conditions:
  dark:
    type: night
    value: true
```

## `storm`

检查事件位置所在世界是否有风暴。`value: false` 要求没有风暴；缺少位置或世界时失败。

| 字段 | 默认值 | 说明 |
| --- | --- | --- |
| `value` | true | 预期布尔状态。 |

```yaml
conditions:
  rain:
    type: storm
    value: true
```
