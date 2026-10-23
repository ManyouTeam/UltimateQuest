# 🏹 弹射物与远程

本页列出相关触发器及其事件数据。`PLAYER` 始终指获得任务进度的玩家；`SOURCE` 和 `TARGET` 的身份随事件变化，不能一律当作玩家和怪物。

## 配置位置

下面是单目标片段，替换[普通任务教程](../tasks.md)里的 `source`。多目标任务中则放在对应的 `objectives.<ID>.source`，模板中直接写 `trigger` 和 `amount`。

```yaml
source:
  trigger: projectile_launch
  amount: 10
```

示例表示累计 10 次符合条件的事件。部分类型触发很频繁，请结合下文选择条件和[防刷](../anti-abuse.md)。默认使用 `count: events`；各类型可选的数量计数方式见下表和[计数方式](../counting.md)。

## `projectile_launch`

统计归属于玩家的弹射物发射，并记录后续命中或 tick 所需的发射信息。

| 可检查的数据 | 此事件提供的内容 |
| --- | --- |
| 支持的 `count` | `events` |
| 底层事件 | `ProjectileLaunchEvent` |
| 平台要求 | Paper 和 Spigot |
| `PLAYER`：任务玩家 | 此事件关联的玩家 |
| `SOURCE`：来源实体 | 任务玩家 |
| `TARGET`：目标实体 | 弹射物 |
| LOCATION | 弹射物位置 |
| TRIGGER_ITEM / slot | 已解析发射/手持物品和槽 |
| 附加数据 | `projectile` |

## `projectile_hit`

弹射物命中方块或实体且能够关联玩家时计数。击中方块时可能没有实体 TARGET，筛选前先核对数据。

| 可检查的数据 | 此事件提供的内容 |
| --- | --- |
| 支持的 `count` | `events` |
| 底层事件 | `ProjectileHitEvent` |
| 平台要求 | Paper 和 Spigot |
| `PLAYER`：任务玩家 | 此事件关联的玩家 |
| `SOURCE`：来源实体 | 已解析射手 |
| `TARGET`：目标实体 | 命中实体，仅方块命中时未提供 |
| LOCATION | 弹射物位置 |
| TRIGGER_ITEM / slot | 已知时的跟踪发射物品/槽 |
| BLOCK | 被命中的方块；仅命中实体时可不提供 |
| 附加数据 | `projectile` |

## `projectile_tick`

相关目标进行期间，对仍有有效玩家和上下文的已跟踪弹射物每 tick 触发。它统计飞行过程，不等于发射次数或命中次数。

| 可检查的数据 | 此事件提供的内容 |
| --- | --- |
| 支持的 `count` | `events` |
| 底层事件 | `TriggerRuntime.ProjectileTick` |
| 平台要求 | Paper 和 Spigot |
| `PLAYER`：任务玩家 | 此事件关联的玩家 |
| `SOURCE`：来源实体 | 跟踪射手 |
| `TARGET`：目标实体 | 弹射物 |
| LOCATION | 弹射物位置 |
| TRIGGER_ITEM / slot | 跟踪发射物品/槽 |
| BLOCK | 弹射物当前位置所在方块 |
| 附加数据 | `projectile` |

## `shoot`

统计玩家弓或弩射击。同一次射击可能同时满足 `shoot` 与 `shoot_bow` 目标，不要将两者当作互斥类型。

| 可检查的数据 | 此事件提供的内容 |
| --- | --- |
| 支持的 `count` | `events` |
| 底层事件 | `EntityShootBowEvent` |
| 平台要求 | Paper 和 Spigot |
| `PLAYER`：任务玩家 | 此事件关联的玩家 |
| `SOURCE`：来源实体 | 任务玩家 |
| `TARGET`：目标实体 | 弹射物 |
| LOCATION | 弹射物位置 |
| TRIGGER_ITEM / slot | 弓/弩 / 事件手 |
| 附加数据 | `bow_force`, `bow`, `consumable`, `hand`, `projectile` |

## `shoot_bow`

统计玩家弓或弩射击，并提供发射武器、使用的手和力度数据。进度不自动按力度计算。

| 可检查的数据 | 此事件提供的内容 |
| --- | --- |
| 支持的 `count` | `events` |
| 底层事件 | `EntityShootBowEvent` |
| 平台要求 | Paper 和 Spigot |
| `PLAYER`：任务玩家 | 此事件关联的玩家 |
| `SOURCE`：来源实体 | 任务玩家 |
| `TARGET`：目标实体 | 弹射物 |
| LOCATION | 弹射物位置 |
| TRIGGER_ITEM / slot | 弓/弩 / 事件手 |
| 附加数据 | `bow_force`, `bow`, `consumable`, `hand`, `projectile` |

## `load_crossbow`

统计玩家装填弩。它发生在发射之前，不代表已经射出弹射物。

| 可检查的数据 | 此事件提供的内容 |
| --- | --- |
| 支持的 `count` | `events` |
| 底层事件 | `EntityLoadCrossbowEvent` |
| 平台要求 | 需 Paper 模式及对应事件类 |
| `PLAYER`：任务玩家 | 此事件关联的玩家 |
| `SOURCE`：来源实体 | 任务玩家 |
| `TARGET`：目标实体 | 未提供 |
| LOCATION | 玩家位置 |
| TRIGGER_ITEM / slot | 弩 / 事件手 |

先确认当前平台支持该触发器，再用新任务测试。下表列出的附加数据供理解事件或开发扩展，不代表都有同名可用条件。

[触发器概览](../triggers.md) · [事件条件](../conditions/README.md)
