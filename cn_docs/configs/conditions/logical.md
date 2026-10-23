# 🔀 逻辑条件

本页用于筛选哪些行为可以计入任务。下面的 `conditions` 示例都是片段：放入目标的 `source` 内，或目标模板根节点；只有不依赖事件数据的玩家条件才适合放在任务根节点检查接取资格。参见[条件基础](README.md)。

## `any`

嵌套条件满足任意一项即可通过。例如，下例允许“在水里”或“正在下雨”。务必填写子条件：省略 `conditions` 节点会通过，显式空映射则因没有匹配项而失败。

| 字段 | 默认值 | 说明 |
| --- | --- | --- |
| `conditions` | — | 具名事件条件映射。 |

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

将嵌套条件整体的结果反转。`not(A AND B)` 只有 A、B 都满足才失败；若只反转单个条件，通常用该条件的公共字段 `not: true` 更直观。

| 字段 | 默认值 | 说明 |
| --- | --- | --- |
| `conditions` | — | 具名事件条件映射。 |

```yaml
conditions:
  outside:
    type: not
    conditions:
      water:
        type: in_water
        target: PLAYER
```
