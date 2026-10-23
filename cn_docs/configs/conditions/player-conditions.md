# 🧾 玩家条件

本页用于筛选哪些行为可以计入任务。下面的 `conditions` 示例都是片段：放入目标的 `source` 内，或目标模板根节点；只有不依赖事件数据的玩家条件才适合放在任务根节点检查接取资格。参见[条件基础](README.md)。

## `player_conditions`

检查某个玩家的权限、世界、生物群系或占位符。外层必须先用 `type: player_conditions` 包装，再在内部写通用玩家条件。`target: PLAYER` 表示做任务的人；默认 SOURCE 不保证是玩家。

| 字段 | 默认值 | 说明 |
| --- | --- | --- |
| `target` | SOURCE | 玩家选择器，PLAYER 显式选择任务拥有者。 |
| `conditions` | — | 具名通用玩家条件。 |

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

### 可用的嵌套类型

| type | 字段 | 行为 |
| --- | --- | --- |
| `permission` | permission | 选中玩家必须拥有权限。 |
| `world` | world | 玩家世界精确名称。 |
| `biome` | biome | 玩家所在生物群系。 |
| `placeholder` | placeholder, rule, value | 比较解析后的文本或数值。 |
| `any` | conditions | 任一嵌套通用条件通过。 |
| `not` | conditions | 反转嵌套整体结果。 |

这些类型只写在包装内部，不能直接作为事件条件。占位符数字比较用 `>=`、`>`、`=`、`<`、`<=`；文本比较用 `==`、`!=`、`*=`（左含右）、`=*`（右含左）、`!*=`、`!=*`。placeholder、rule、value 都要填写，并分别测试符合与不符合条件的玩家。
