# 🎒 物品与装备

本页列出相关触发器及其事件数据。`PLAYER` 始终指获得任务进度的玩家；`SOURCE` 和 `TARGET` 的身份随事件变化，不能一律当作玩家和怪物。

## 配置位置

下面是单目标片段，替换[普通任务教程](../tasks.md)里的 `source`。多目标任务中则放在对应的 `objectives.<ID>.source`，模板中直接写 `trigger` 和 `amount`。

```yaml
source:
  trigger: consume
  amount: 10
```

示例表示累计 10 次符合条件的事件。部分类型触发很频繁，请结合下文选择条件和[防刷](../anti-abuse.md)。默认使用 `count: events`；各类型可选的数量计数方式见下表和[计数方式](../counting.md)。

## `consume`

统计玩家消耗物品，例如吃食物或喝药水。事件物品是被消耗的物品，不需要猜测它来自哪只手。

| 可检查的数据 | 此事件提供的内容 |
| --- | --- |
| 支持的 `count` | `events` |
| 底层事件 | `PlayerItemConsumeEvent` |
| 平台要求 | Paper 和 Spigot |
| `PLAYER`：任务玩家 | 此事件关联的玩家 |
| `SOURCE`：来源实体 | 任务玩家 |
| `TARGET`：目标实体 | 未提供 |
| LOCATION | 玩家位置 |
| TRIGGER_ITEM / slot | 被消耗物品堆 / 事件手 |

## `enchant_item`

统计附魔台附魔，不统计铁砧合并。附加数据中的 `original_experience` 是显示等级费用，`enchantment_level_cost` 和 `original_lapis` 是选项索引加 1（通常为 1–3）；这些数值不会自动变成进度增量。

| 可检查的数据 | 此事件提供的内容 |
| --- | --- |
| 支持的 `count` | `events` |
| 底层事件 | `EnchantItemEvent` |
| 平台要求 | Paper 和 Spigot |
| `PLAYER`：任务玩家 | 此事件关联的玩家 |
| `SOURCE`：来源实体 | 任务玩家 |
| `TARGET`：目标实体 | 未提供 |
| LOCATION | 玩家位置 |
| TRIGGER_ITEM / slot | 附魔物品 / HAND |
| 附加数据 | `original_experience`, `enchantment_level_cost`, `original_lapis` |

## `item_damage`

`events` 每次物品损耗事件加 1；`durability` 累计事件损耗量，最多计入物品剩余耐久。不计不可损坏物品，也不把手中整堆物品数量当作损耗量。

| 可检查的数据 | 此事件提供的内容 |
| --- | --- |
| 支持的 `count` | `events`、`durability` |
| 底层事件 | `PlayerItemDamageEvent` |
| 平台要求 | Paper 和 Spigot |
| `PLAYER`：任务玩家 | 此事件关联的玩家 |
| `SOURCE`：来源实体 | 任务玩家 |
| `TARGET`：目标实体 | 未提供 |
| LOCATION | 玩家位置 |
| TRIGGER_ITEM / slot | 受损物品 / 解析槽 |
| 附加数据 | `original_item_damage` |

## `item_held`

统计玩家切换选中的快捷栏槽位。仅改变槽位选择，不要求使用物品。

| 可检查的数据 | 此事件提供的内容 |
| --- | --- |
| 支持的 `count` | `events` |
| 底层事件 | `PlayerItemHeldEvent` |
| 平台要求 | Paper 和 Spigot |
| `PLAYER`：任务玩家 | 此事件关联的玩家 |
| `SOURCE`：来源实体 | 任务玩家 |
| `TARGET`：目标实体 | 未提供 |
| LOCATION | 玩家位置 |
| TRIGGER_ITEM / slot | 新选中快捷栏物品 / HAND |

## `swap_hand`

统计玩家交换主副手。事件物品使用交换后的新主手物品，筛选时要按这个方向理解。

| 可检查的数据 | 此事件提供的内容 |
| --- | --- |
| 支持的 `count` | `events` |
| 底层事件 | `PlayerSwapHandItemsEvent` |
| 平台要求 | Paper 和 Spigot |
| `PLAYER`：任务玩家 | 此事件关联的玩家 |
| `SOURCE`：来源实体 | 任务玩家 |
| `TARGET`：目标实体 | 未提供 |
| LOCATION | 玩家位置 |
| TRIGGER_ITEM / slot | 事件新主手物品 / HAND |

## `item_group_cooldown`

统计物品或物品组的冷却变化。只有事件能解析出具体物品时才提供该物品，物品条件可能因此不满足。

| 可检查的数据 | 此事件提供的内容 |
| --- | --- |
| 支持的 `count` | `events` |
| 底层事件 | `PlayerItemGroupCooldownEvent` |
| 平台要求 | 需 Paper 模式及对应事件类 |
| `PLAYER`：任务玩家 | 此事件关联的玩家 |
| `SOURCE`：来源实体 | 任务玩家 |
| `TARGET`：目标实体 | 未提供 |
| LOCATION | 玩家位置 |
| TRIGGER_ITEM / slot | 事件提供材质时才匹配手持物品 |

## `name_entity`

统计玩家给实体命名的事件。可用 TARGET 条件筛选被命名实体。

| 可检查的数据 | 此事件提供的内容 |
| --- | --- |
| 支持的 `count` | `events` |
| 底层事件 | `PlayerNameEntityEvent` |
| 平台要求 | 需 Paper 模式及对应事件类 |
| `PLAYER`：任务玩家 | 此事件关联的玩家 |
| `SOURCE`：来源实体 | 任务玩家 |
| `TARGET`：目标实体 | 被命名实体 |
| LOCATION | 目标/实体位置 |
| TRIGGER_ITEM / slot | 找到时的手持命名牌 |

先确认当前平台支持该触发器，再用新任务测试。下表列出的附加数据供理解事件或开发扩展，不代表都有同名可用条件。

[触发器概览](../triggers.md) · [事件条件](../conditions/README.md)
