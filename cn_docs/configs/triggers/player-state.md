# ❤️ 玩家状态

本页列出相关触发器及其事件数据。`PLAYER` 始终指获得任务进度的玩家；`SOURCE` 和 `TARGET` 的身份随事件变化，不能一律当作玩家和怪物。

## 配置位置

下面是单目标片段，替换[普通任务教程](../tasks.md)里的 `source`。多目标任务中则放在对应的 `objectives.<ID>.source`，模板中直接写 `trigger` 和 `amount`。

```yaml
source:
  trigger: air_change
  amount: 10
```

示例表示累计 10 次符合条件的事件。部分类型触发很频繁，请结合下文选择条件和[防刷](../anti-abuse.md)。默认使用 `count: events`；各类型可选的数量计数方式见下表和[计数方式](../counting.md)。

## `air_change`

统计玩家剩余空气值变化的事件次数，不按变化的空气值累加。

| 可检查的数据 | 此事件提供的内容 |
| --- | --- |
| 支持的 `count` | `events` |
| 底层事件 | `EntityAirChangeEvent` |
| 平台要求 | Paper 和 Spigot |
| `PLAYER`：任务玩家 | 此事件关联的玩家 |
| `SOURCE`：来源实体 | 任务玩家 |
| `TARGET`：目标实体 | 未提供 |
| LOCATION | 玩家位置 |
| TRIGGER_ITEM / slot | 未提供 |
| 附加数据 | `original_air`, `previous_air` |

## `food_level_change`

统计玩家饥饿值变化，不保证每次都是进食。需要进食次数时优先使用 `consume`。

| 可检查的数据 | 此事件提供的内容 |
| --- | --- |
| 支持的 `count` | `events` |
| 底层事件 | `FoodLevelChangeEvent` |
| 平台要求 | Paper 和 Spigot |
| `PLAYER`：任务玩家 | 此事件关联的玩家 |
| `SOURCE`：来源实体 | 任务玩家 |
| `TARGET`：目标实体 | 未提供 |
| LOCATION | 玩家位置 |
| TRIGGER_ITEM / slot | 未提供 |
| 附加数据 | `original_food_level` |

## `exp_change`

`events` 每次经验变化事件加 1；`experience` 累计事件中的正数经验点数，负数和零不增加数量进度。它统计 Minecraft 经验，不是经验等级或战令积分。

| 可检查的数据 | 此事件提供的内容 |
| --- | --- |
| 支持的 `count` | `events`、`experience` |
| 底层事件 | `PlayerExpChangeEvent` |
| 平台要求 | Paper 和 Spigot |
| `PLAYER`：任务玩家 | 此事件关联的玩家 |
| `SOURCE`：来源实体 | 任务玩家 |
| `TARGET`：目标实体 | 未提供 |
| LOCATION | 玩家位置 |
| TRIGGER_ITEM / slot | 未提供 |
| 附加数据 | `original_experience` |

## `potion_effect`

统计玩家药水效果变化。时长取自发生变化的效果，进度仍按事件次数增加。

| 可检查的数据 | 此事件提供的内容 |
| --- | --- |
| 支持的 `count` | `events` |
| 底层事件 | `EntityPotionEffectEvent` |
| 平台要求 | Paper 和 Spigot |
| `PLAYER`：任务玩家 | 此事件关联的玩家 |
| `SOURCE`：来源实体 | 任务玩家 |
| `TARGET`：目标实体 | 任务玩家 |
| LOCATION | 玩家位置 |
| TRIGGER_ITEM / slot | 未提供 |
| 附加数据 | `original_duration` |

## `effect_tick`

统计服务端提供的效果 tick 事件，具体效果数据在原事件中。不要把它当作每秒触发一次。

| 可检查的数据 | 此事件提供的内容 |
| --- | --- |
| 支持的 `count` | `events` |
| 底层事件 | `EntityEffectTickEvent` |
| 平台要求 | 需 Paper 模式及对应事件类 |
| `PLAYER`：任务玩家 | 此事件关联的玩家 |
| `SOURCE`：来源实体 | 任务玩家 |
| `TARGET`：目标实体 | 未提供 |
| LOCATION | 玩家位置 |
| TRIGGER_ITEM / slot | 未提供 |

## `exhaustion`

`events` 每次消耗度事件加 1；`exhaustion` 累计事件中的正数消耗度数值。它不是饥饿值或饱和度。

| 可检查的数据 | 此事件提供的内容 |
| --- | --- |
| 支持的 `count` | `events`、`exhaustion` |
| 底层事件 | `EntityExhaustionEvent` |
| 平台要求 | 各模式按可选事件类检测 |
| `PLAYER`：任务玩家 | 此事件关联的玩家 |
| `SOURCE`：来源实体 | 任务玩家 |
| `TARGET`：目标实体 | 未提供 |
| LOCATION | 玩家位置 |
| TRIGGER_ITEM / slot | 未提供 |

## `pickup_experience`

`events` 每次拾取事件加 1；`experience` 累计该经验球提供的经验点数。它与 `exp_change` 可能对应同一次经验获取，配置两个来源会分别累计。

| 可检查的数据 | 此事件提供的内容 |
| --- | --- |
| 支持的 `count` | `events`、`experience` |
| 底层事件 | `PlayerPickupExperienceEvent` |
| 平台要求 | 需 Paper 模式及对应事件类 |
| `PLAYER`：任务玩家 | 此事件关联的玩家 |
| `SOURCE`：来源实体 | 任务玩家 |
| `TARGET`：目标实体 | 未提供 |
| LOCATION | 经验球位置 |
| TRIGGER_ITEM / slot | 未提供 |
| 附加数据 | `original_experience` |

先确认当前平台支持该触发器，再用新任务测试。下表列出的附加数据供理解事件或开发扩展，不代表都有同名可用条件。

[触发器概览](../triggers.md) · [事件条件](../conditions/README.md)
