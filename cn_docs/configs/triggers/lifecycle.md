# ⏱️ Tick 与生命周期

本页列出相关触发器及其事件数据。`PLAYER` 始终指获得任务进度的玩家；`SOURCE` 和 `TARGET` 的身份随事件变化，不能一律当作玩家和怪物。

## 配置位置

下面是单目标片段，替换[普通任务教程](../tasks.md)里的 `source`。多目标任务中则放在对应的 `objectives.<ID>.source`，模板中直接写 `trigger` 和 `amount`。

```yaml
source:
  trigger: tick
  amount: 10
```

示例表示累计 10 次符合条件的事件。部分类型触发很频繁，请结合下文选择条件和[防刷](../anti-abuse.md)。默认使用 `count: events`；各类型可选的数量计数方式见下表和[计数方式](../counting.md)。

## `tick`

相关目标进行期间，对被观察的在线玩家每游戏 tick 检查一次。20 TPS 时约每秒 20 次，适合配合条件和冷却使用，不应直接理解为秒。

| 可检查的数据 | 此事件提供的内容 |
| --- | --- |
| 支持的 `count` | `events` |
| 平台要求 | Paper 和 Spigot |
| `PLAYER`：任务玩家 | 此事件关联的玩家 |
| `SOURCE`：来源实体 | 任务玩家 |
| `TARGET`：目标实体 | 未提供 |
| LOCATION | 玩家位置 |
| TRIGGER_ITEM / slot | 未提供 |
| BLOCK | 玩家当前位置所在方块 |

## `spawn`

统计玩家加入且数据就绪后的出现，以及重生后的出现。它不统计普通怪物生成。

| 可检查的数据 | 此事件提供的内容 |
| --- | --- |
| 支持的 `count` | `events` |
| 平台要求 | Paper 和 Spigot |
| `PLAYER`：任务玩家 | 此事件关联的玩家 |
| `SOURCE`：来源实体 | 任务玩家 |
| `TARGET`：目标实体 | 未提供 |
| LOCATION | 加入/重生位置 |
| TRIGGER_ITEM / slot | 未提供 |
| BLOCK | 加入位置或重生位置所在方块 |

## `respawn`

统计玩家重生，不统计登录。

| 可检查的数据 | 此事件提供的内容 |
| --- | --- |
| 支持的 `count` | `events` |
| 平台要求 | Paper 和 Spigot |
| `PLAYER`：任务玩家 | 此事件关联的玩家 |
| `SOURCE`：来源实体 | 任务玩家 |
| `TARGET`：目标实体 | 未提供 |
| LOCATION | 重生位置 |
| TRIGGER_ITEM / slot | 未提供 |
| BLOCK | 重生位置所在方块 |

[触发器概览](../triggers.md) · [事件条件](../conditions/README.md)

## `placeholder`

仅用于任务目标；每秒解析并比较一次，不注册 Bukkit 事件。达到条件时一次满足目标，不要求连续达标；已完成任务不会因值降低而撤回。未解析的占位符、非数值比较或解析异常均判为不满足，不会反复输出错误。不能用于战令／里程碑经验来源。

```yaml
source:
  trigger: placeholder
  placeholder: "%player_level%"
  rule: ">="
  value: 10
  amount: 1
```

需要安装 PlaceholderAPI 及对应扩展。支持数值 `>=`、`>`、`<=`、`<`、`=`，字符串 `==`、`!=`，以及包含 `*=`／反向包含 `=*` 和各自的否定 `!*=`／`!=*`。`operator` 是 `rule` 的别名。默认示例 `placeholder_level_check` 关闭，安装所需扩展后同时启用目标和任务文件。
