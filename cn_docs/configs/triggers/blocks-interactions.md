# 🧱 方块与交互

本页列出相关触发器及其事件数据。`PLAYER` 始终指获得任务进度的玩家；`SOURCE` 和 `TARGET` 的身份随事件变化，不能一律当作玩家和怪物。

## 配置位置

下面是单目标片段，替换[普通任务教程](../tasks.md)里的 `source`。多目标任务中则放在对应的 `objectives.<ID>.source`，模板中直接写 `trigger` 和 `amount`。

```yaml
source:
  trigger: block_break
  amount: 10
```

示例表示累计 10 次符合条件的事件。部分类型触发很频繁，请结合下文选择条件和[防刷](../anti-abuse.md)。默认使用 `count: events`；各类型可选的数量计数方式见下表和[计数方式](../counting.md)。

## `block_break`

统计玩家实际挖掉方块的次数，适合采矿任务。默认排除插件已记录的玩家放置方块；用 `block_type` 指定石头、矿石等材质。

| 可检查的数据 | 此事件提供的内容 |
| --- | --- |
| 支持的 `count` | `events` |
| 底层事件 | `BlockBreakEvent` |
| 平台要求 | Paper 和 Spigot |
| `PLAYER`：任务玩家 | 此事件关联的玩家 |
| `SOURCE`：来源实体 | 任务玩家 |
| `TARGET`：目标实体 | 未提供 |
| LOCATION | 事件方块中心 |
| TRIGGER_ITEM / slot | 主手 / HAND |
| BLOCK | 被破坏的方块 |
| 附加数据 | `original_experience` |

## `block_damage`

统计玩家开始挖掘方块的行为。方块还没有被挖掉，因此不适合代替“采集完成”的计数。

| 可检查的数据 | 此事件提供的内容 |
| --- | --- |
| 支持的 `count` | `events` |
| 底层事件 | `BlockDamageEvent` |
| 平台要求 | Paper 和 Spigot |
| `PLAYER`：任务玩家 | 此事件关联的玩家 |
| `SOURCE`：来源实体 | 任务玩家 |
| `TARGET`：目标实体 | 未提供 |
| LOCATION | 事件方块中心 |
| TRIGGER_ITEM / slot | 事件物品 / HAND |
| BLOCK | 正在被挖掘的方块 |

## `block_drop_item`

`events` 每次方块掉落事件加 1；`items` 累计该事件所有掉落物的堆叠数量。`match_item` + `item: COUNTED_ITEM` 逐堆筛选掉落物；`conditions.match_item` 的事件物品仍是工具。

| 可检查的数据 | 此事件提供的内容 |
| --- | --- |
| 支持的 `count` | `events`、`items` |
| 底层事件 | `BlockDropItemEvent` |
| 平台要求 | Paper 和 Spigot |
| `PLAYER`：任务玩家 | 此事件关联的玩家 |
| `SOURCE`：来源实体 | 任务玩家 |
| `TARGET`：目标实体 | 未提供 |
| LOCATION | 事件方块中心 |
| TRIGGER_ITEM / slot | 当前主手 / HAND |
| BLOCK | 被破坏方块的当前引用；原始状态另存于 broken_block_state |
| 附加数据 | `broken_block_state` |

## `block_place`

`events` 每次放置事件加 1；`blocks` 按事件涉及的方块数累计，例如放置一扇门可计 2 块。使用 `blocks` 时，会对每块方块分别检查条件，只累计符合条件的方块。

| 可检查的数据 | 此事件提供的内容 |
| --- | --- |
| 支持的 `count` | `events`、`blocks` |
| 底层事件 | `BlockPlaceEvent` |
| 平台要求 | Paper 和 Spigot |
| `PLAYER`：任务玩家 | 此事件关联的玩家 |
| `SOURCE`：来源实体 | 任务玩家 |
| `TARGET`：目标实体 | 未提供 |
| LOCATION | 事件方块中心 |
| TRIGGER_ITEM / slot | 放置物品 / 事件手 |
| BLOCK | 本次放置的方块 |

## `block_break_progress_update`

统计服务端报告的挖掘进度更新，未必代表方块已被挖掉。只在 Paper 模式和对应事件类可用时使用。

| 可检查的数据 | 此事件提供的内容 |
| --- | --- |
| 支持的 `count` | `events` |
| 底层事件 | `BlockBreakProgressUpdateEvent` |
| 平台要求 | 需 Paper 模式及对应事件类 |
| `PLAYER`：任务玩家 | 此事件关联的玩家 |
| `SOURCE`：来源实体 | 任务玩家 |
| `TARGET`：目标实体 | 未提供 |
| LOCATION | 事件方块中心 |
| TRIGGER_ITEM / slot | 主手 / HAND |
| BLOCK | 挖掘进度发生变化的方块 |
| 附加数据 | `block_break_progress` |

## `interact`

统计玩家交互事件。点空气、方块和物品时可用的数据不同；只有存在方块或物品时，相应条件才有数据可检查。

| 可检查的数据 | 此事件提供的内容 |
| --- | --- |
| 支持的 `count` | `events` |
| 底层事件 | `PlayerInteractEvent` |
| 平台要求 | Paper 和 Spigot |
| `PLAYER`：任务玩家 | 此事件关联的玩家 |
| `SOURCE`：来源实体 | 任务玩家 |
| `TARGET`：目标实体 | 未提供 |
| LOCATION | 点击方块中心，否则玩家位置 |
| TRIGGER_ITEM / slot | 事件物品 / 事件手，均可未提供 |
| BLOCK | 被点击的方块；点击空气时不提供 |

## `inside_block`

统计服务端报告的玩家处于方块内部的事件。需要 Paper 对应事件，不是通用的进入区域检测。

| 可检查的数据 | 此事件提供的内容 |
| --- | --- |
| 支持的 `count` | `events` |
| 底层事件 | `EntityInsideBlockEvent` |
| 平台要求 | 需 Paper 模式及对应事件类 |
| `PLAYER`：任务玩家 | 此事件关联的玩家 |
| `SOURCE`：来源实体 | 任务玩家 |
| `TARGET`：目标实体 | 未提供 |
| LOCATION | 事件方块中心 |
| TRIGGER_ITEM / slot | 未提供 |
| BLOCK | 玩家所处的事件方块 |

## `vibration_receive`

当方块接收到来源为玩家的游戏振动时计数。条件里的位置指接收方块的位置，不一定是玩家的位置。

| 可检查的数据 | 此事件提供的内容 |
| --- | --- |
| 支持的 `count` | `events` |
| 底层事件 | `BlockReceiveGameEvent` |
| 平台要求 | Paper 和 Spigot |
| `PLAYER`：任务玩家 | 此事件关联的玩家 |
| `SOURCE`：来源实体 | 任务玩家 |
| `TARGET`：目标实体 | 任务玩家 |
| LOCATION | 接收方块位置 |
| TRIGGER_ITEM / slot | 未提供 |

先确认当前平台支持该触发器，再用新任务测试。下表列出的附加数据供理解事件或开发扩展，不代表都有同名可用条件。

[触发器概览](../triggers.md) · [事件条件](../conditions/README.md)
