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
| 平台要求 | Paper 和 Spigot |
| `PLAYER`：任务玩家 | 此事件关联的玩家 |
| `SOURCE`：来源实体 | 任务玩家 |
| `TARGET`：目标实体 | 未提供 |
| LOCATION | 玩家位置 |
| TRIGGER_ITEM / slot | 未提供 |

## `food_level_change`

统计玩家饥饿值变化，不保证每次都是进食。需要进食次数时优先使用 `consume`。

| 可检查的数据 | 此事件提供的内容 |
| --- | --- |
| 支持的 `count` | `events` |
| 平台要求 | Paper 和 Spigot |
| `PLAYER`：任务玩家 | 此事件关联的玩家 |
| `SOURCE`：来源实体 | 任务玩家 |
| `TARGET`：目标实体 | 未提供 |
| LOCATION | 玩家位置 |
| TRIGGER_ITEM / slot | 未提供 |

## `exp_change`

`events` 每次经验变化事件加 1；`experience` 累计事件中的正数经验点数，负数和零不增加数量进度。它统计 Minecraft 经验，不是经验等级或战令积分。

| 可检查的数据 | 此事件提供的内容 |
| --- | --- |
| 支持的 `count` | `events`、`experience` |
| 平台要求 | Paper 和 Spigot |
| `PLAYER`：任务玩家 | 此事件关联的玩家 |
| `SOURCE`：来源实体 | 任务玩家 |
| `TARGET`：目标实体 | 未提供 |
| LOCATION | 玩家位置 |
| TRIGGER_ITEM / slot | 未提供 |

## `advancement_done`

玩家完成一项 Minecraft 进度（成就）时加 1，不按单个判据计数，也不会补算接取任务前已完成的进度。统计服务器实际触发的事件，包括配方解锁及数据包自定义进度；撤销后再次完成会再次计数。

| 可检查的数据 | 此事件提供的内容 |
| --- | --- |
| 支持的 `count` | `events` |
| 平台要求 | Paper 和 Spigot |
| `PLAYER`：任务玩家 | 完成进度的玩家 |
| `SOURCE`：来源实体 | 任务玩家 |
| `TARGET`：目标实体 | 未提供 |
| LOCATION | 玩家位置 |
| TRIGGER_ITEM / slot | 未提供 |

使用 [`advancement` 事件条件](../conditions/extended.md#advancement)直接筛选本次完成的进度。下面只统计 `minecraft:story/mine_diamond`：

```yaml
source:
  trigger: advancement_done
  amount: 1
  conditions:
    advancement:
      type: advancement
      advancement: 'minecraft:story/mine_diamond'
```

不写条件会统计全部进度。使用 `advancements` 列表可匹配多个进度中的任意一个；`not: true` 可以排除指定的进度。

## `potion_effect`

统计玩家药水效果变化。时长取自发生变化的效果，进度仍按事件次数增加。

| 可检查的数据 | 此事件提供的内容 |
| --- | --- |
| 支持的 `count` | `events` |
| 平台要求 | Paper 和 Spigot |
| `PLAYER`：任务玩家 | 此事件关联的玩家 |
| `SOURCE`：来源实体 | 任务玩家 |
| `TARGET`：目标实体 | 任务玩家 |
| LOCATION | 玩家位置 |
| TRIGGER_ITEM / slot | 未提供 |

## `effect_tick`

统计服务端提供的效果 tick 事件，具体效果数据在原事件中。不要把它当作每秒触发一次。

| 可检查的数据 | 此事件提供的内容 |
| --- | --- |
| 支持的 `count` | `events` |
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
| 平台要求 | 需 Paper 模式及对应事件类 |
| `PLAYER`：任务玩家 | 此事件关联的玩家 |
| `SOURCE`：来源实体 | 任务玩家 |
| `TARGET`：目标实体 | 未提供 |
| LOCATION | 经验球位置 |
| TRIGGER_ITEM / slot | 未提供 |

[触发器概览](../triggers.md) · [事件条件](../conditions/README.md)
