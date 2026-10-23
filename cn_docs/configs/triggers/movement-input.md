# 🕹️ 移动与输入

本页列出相关触发器及其事件数据。`PLAYER` 始终指获得任务进度的玩家；`SOURCE` 和 `TARGET` 的身份随事件变化，不能一律当作玩家和怪物。

## 配置位置

下面是单目标片段，替换[普通任务教程](../tasks.md)里的 `source`。多目标任务中则放在对应的 `objectives.<ID>.source`，模板中直接写 `trigger` 和 `amount`。

```yaml
source:
  trigger: move
  amount: 10
```

示例表示累计 10 次符合条件的事件。部分类型触发很频繁，请结合下文选择条件和[防刷](../anti-abuse.md)。默认使用 `count: events`；各类型可选的数量计数方式见下表和[计数方式](../counting.md)。

## `move`

`events` 保留原来的规则：跨越方块坐标或切换世界才加 1，同一格内移动和转头不计次数。`distance` 累计相邻移动事件的三维直线距离，单位为方块；同一格内移动也计算距离，转头、传送和跨世界跳转不增加距离。

| 可检查的数据 | 此事件提供的内容 |
| --- | --- |
| 支持的 `count` | `events`、`distance` |
| 底层事件 | `PlayerMoveEvent` |
| 平台要求 | Paper 和 Spigot |
| `PLAYER`：任务玩家 | 此事件关联的玩家 |
| `SOURCE`：来源实体 | 任务玩家 |
| `TARGET`：目标实体 | 未提供 |
| LOCATION | 终点，另保留 from/to |
| TRIGGER_ITEM / slot | 未提供 |
| BLOCK | 移动终点所在方块 |

## 疾跑与游泳

使用 `move`、`count: distance` 和事件条件组合。`sprinting`、`swimming` 是条件，不是独立触发器。可额外配置 `riding`、`flying`、`gliding` 并设为 `value: false`，排除这些状态。游泳指服务端的游泳状态，不包括只在水中站立。使用 `anti-abuse.cooldown-ms: 0` 可避免移动片段因冷却丢失。

```yaml
source:
  trigger: move
  count: distance
  amount: 256
  conditions:
    sprinting:
      type: sprinting
      target: PLAYER
      value: true
    not_swimming:
      type: swimming
      target: PLAYER
      value: false
  anti-abuse:
    cooldown-ms: 0
```

游泳任务将上述状态条件替换为 `type: swimming`、`target: PLAYER`、`value: true`，并将 `amount` 设为所需格数。内置 `sprinter`、`swimmer` 模板还包含其他移动状态排除条件。

## `input`

统计服务端报告的玩家输入更新，需要对应事件支持。输入变化不代表已经移动，也不代表移动距离。

| 可检查的数据 | 此事件提供的内容 |
| --- | --- |
| 支持的 `count` | `events` |
| 底层事件 | `PlayerInputEvent` |
| 平台要求 | Paper 和 Spigot |
| `PLAYER`：任务玩家 | 此事件关联的玩家 |
| `SOURCE`：来源实体 | 任务玩家 |
| `TARGET`：目标实体 | 未提供 |
| LOCATION | 玩家位置 |
| TRIGGER_ITEM / slot | 未提供 |

## `toggle_sneak`

统计潜行状态切换，开始和结束潜行都可能触发。只要其中一种状态时，添加玩家状态条件筛选。

| 可检查的数据 | 此事件提供的内容 |
| --- | --- |
| 支持的 `count` | `events` |
| 底层事件 | `PlayerToggleSneakEvent` |
| 平台要求 | Paper 和 Spigot |
| `PLAYER`：任务玩家 | 此事件关联的玩家 |
| `SOURCE`：来源实体 | 任务玩家 |
| `TARGET`：目标实体 | 未提供 |
| LOCATION | 玩家位置 |
| TRIGGER_ITEM / slot | 未提供 |

## `toggle_flight`

统计飞行状态切换。开启或关闭的具体状态在事件中，不要把一次事件直接理解为一次起飞。

| 可检查的数据 | 此事件提供的内容 |
| --- | --- |
| 支持的 `count` | `events` |
| 底层事件 | `PlayerToggleFlightEvent` |
| 平台要求 | Paper 和 Spigot |
| `PLAYER`：任务玩家 | 此事件关联的玩家 |
| `SOURCE`：来源实体 | 任务玩家 |
| `TARGET`：目标实体 | 未提供 |
| LOCATION | 玩家位置 |
| TRIGGER_ITEM / slot | 未提供 |

## `riptide`

统计玩家使用物品发动激流，事件物品是发动激流的物品。

| 可检查的数据 | 此事件提供的内容 |
| --- | --- |
| 支持的 `count` | `events` |
| 底层事件 | `PlayerRiptideEvent` |
| 平台要求 | Paper 和 Spigot |
| `PLAYER`：任务玩家 | 此事件关联的玩家 |
| `SOURCE`：来源实体 | 任务玩家 |
| `TARGET`：目标实体 | 未提供 |
| LOCATION | 玩家位置 |
| TRIGGER_ITEM / slot | 事件物品 / HAND |

## `jump`

统计玩家跳跃，保留跳跃的起止位置。需要相应 Paper 事件，不按跳跃高度累计。

| 可检查的数据 | 此事件提供的内容 |
| --- | --- |
| 支持的 `count` | `events` |
| 底层事件 | `PlayerJumpEvent` |
| 平台要求 | 需 Paper 模式及对应事件类 |
| `PLAYER`：任务玩家 | 此事件关联的玩家 |
| `SOURCE`：来源实体 | 任务玩家 |
| `TARGET`：目标实体 | 未提供 |
| LOCATION | 终点，另保留 from/to |
| TRIGGER_ITEM / slot | 未提供 |
| BLOCK | 事件终点所在方块 |

## `elytra_boost`

统计玩家的鞘翅加速事件，适合飞行活动的次数目标。

| 可检查的数据 | 此事件提供的内容 |
| --- | --- |
| 支持的 `count` | `events` |
| 底层事件 | `PlayerElytraBoostEvent` |
| 平台要求 | 需 Paper 模式及对应事件类 |
| `PLAYER`：任务玩家 | 此事件关联的玩家 |
| `SOURCE`：来源实体 | 任务玩家 |
| `TARGET`：目标实体 | 任务玩家 |
| LOCATION | 玩家位置 |
| TRIGGER_ITEM / slot | 加速物品 / 事件手 |

## `lunge`

统计服务端报告的玩家突进，能解析时保留正在使用的物品。需要对应事件支持。

| 可检查的数据 | 此事件提供的内容 |
| --- | --- |
| 支持的 `count` | `events` |
| 底层事件 | `EntityLungeEvent` |
| 平台要求 | 需 Paper 模式及对应事件类 |
| `PLAYER`：任务玩家 | 此事件关联的玩家 |
| `SOURCE`：来源实体 | 任务玩家 |
| `TARGET`：目标实体 | 未提供 |
| LOCATION | 玩家位置 |
| TRIGGER_ITEM / slot | 存在时的使用中物品，否则实现的备用物品 |

先确认当前平台支持该触发器，再用新任务测试。下表列出的附加数据供理解事件或开发扩展，不代表都有同名可用条件。

[触发器概览](../triggers.md) · [事件条件](../conditions/README.md)
