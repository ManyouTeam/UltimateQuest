# 🎣 钓鱼与交易

本页列出相关触发器及其事件数据。`PLAYER` 始终指获得任务进度的玩家；`SOURCE` 和 `TARGET` 的身份随事件变化，不能一律当作玩家和怪物。

## 配置位置

下面是单目标片段，替换[普通任务教程](../tasks.md)里的 `source`。多目标任务中则放在对应的 `objectives.<ID>.source`，模板中直接写 `trigger` 和 `amount`。

```yaml
source:
  trigger: fish
  amount: 10
```

示例表示累计 10 次符合条件的事件。部分类型触发很频繁，请结合下文选择条件和[防刷](../anti-abuse.md)。默认使用 `count: events`；各类型可选的数量计数方式见下表和[计数方式](../counting.md)。

## `fish`

正常事件路由只处理成功钓获状态。`events` 每次成功钓获加 1；`items` 只累计钓获物品的堆叠数量，空钓获、钩住实体等情况不增加物品进度。用 `match_item` + `item: COUNTED_ITEM` 筛选鱼的种类。

| 可检查的数据 | 此事件提供的内容 |
| --- | --- |
| 支持的 `count` | `events`、`items` |
| 底层事件 | `PlayerFishEvent` |
| 平台要求 | Paper 和 Spigot |
| `PLAYER`：任务玩家 | 此事件关联的玩家 |
| `SOURCE`：来源实体 | 任务玩家 |
| `TARGET`：目标实体 | 存在时的钓获实体 |
| LOCATION | 鱼钩位置 |
| TRIGGER_ITEM / slot | 鱼竿 / 解析后的主或副手 |
| 附加数据 | `original_experience` |

## `piglin_barter`

`events` 每次可归属于玩家的交易加 1；`items` 累计交易产出的物品数量。`match_item` + `item: COUNTED_ITEM` 筛选产物，事件物品仍是交易输入物品。

| 可检查的数据 | 此事件提供的内容 |
| --- | --- |
| 支持的 `count` | `events`、`items` |
| 底层事件 | `PiglinBarterEvent` |
| 平台要求 | Paper 和 Spigot |
| `PLAYER`：任务玩家 | 此事件关联的玩家 |
| `SOURCE`：来源实体 | 交易猪灵 |
| `TARGET`：目标实体 | 交易猪灵 |
| LOCATION | 目标/实体位置 |
| TRIGGER_ITEM / slot | 交易输入物品 / HAND |

## `trade`

`events` 每次交易事件加 1；`items` 读取该次交易结果的堆叠数量。批量交易按服务端实际触发的各次事件累计。

| 可检查的数据 | 此事件提供的内容 |
| --- | --- |
| 支持的 `count` | `events`、`items` |
| 底层事件 | `PlayerTradeEvent` |
| 平台要求 | 需 Paper 模式及对应事件类 |
| `PLAYER`：任务玩家 | 此事件关联的玩家 |
| `SOURCE`：来源实体 | 任务玩家 |
| `TARGET`：目标实体 | 商人实体 |
| LOCATION | 商人位置 |
| TRIGGER_ITEM / slot | 交易结果 / HAND |

## `purchase`

`events` 每次购买事件加 1；`items` 读取该次购买结果的堆叠数量。虚拟商人没有实体 TARGET，物品计数仍可使用。

| 可检查的数据 | 此事件提供的内容 |
| --- | --- |
| 支持的 `count` | `events`、`items` |
| 底层事件 | `PlayerPurchaseEvent` |
| 平台要求 | 需 Paper 模式及对应事件类 |
| `PLAYER`：任务玩家 | 此事件关联的玩家 |
| `SOURCE`：来源实体 | 任务玩家 |
| `TARGET`：目标实体 | 仅商人为实体时存在 |
| LOCATION | 实体商人位置（存在时），否则玩家位置 |
| TRIGGER_ITEM / slot | 交易结果 / HAND |

先确认当前平台支持该触发器，再用新任务测试。下表列出的附加数据供理解事件或开发扩展，不代表都有同名可用条件。

[触发器概览](../triggers.md) · [事件条件](../conditions/README.md)
