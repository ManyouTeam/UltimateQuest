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
| 平台要求 | Paper 和 Spigot |
| `PLAYER`：任务玩家 | 此事件关联的玩家 |
| `SOURCE`：来源实体 | 任务玩家 |
| `TARGET`：目标实体 | 未提供 |
| LOCATION | 事件方块中心 |
| TRIGGER_ITEM / slot | 主手 / HAND |
| BLOCK | 被破坏的方块 |

## `block_damage`

统计玩家开始挖掘方块的行为。方块还没有被挖掉，因此不适合代替“采集完成”的计数。

| 可检查的数据 | 此事件提供的内容 |
| --- | --- |
| 支持的 `count` | `events` |
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
| 平台要求 | Paper 和 Spigot |
| `PLAYER`：任务玩家 | 此事件关联的玩家 |
| `SOURCE`：来源实体 | 任务玩家 |
| `TARGET`：目标实体 | 未提供 |
| LOCATION | 事件方块中心 |
| TRIGGER_ITEM / slot | 当前主手 / HAND |
| BLOCK | 被破坏方块的当前引用；原始状态另存于 broken_block_state |

## `block_place`

`events` 每次放置事件加 1；`blocks` 按事件涉及的方块数累计，例如放置一扇门可计 2 块。使用 `blocks` 时，会对每块方块分别检查条件，只累计符合条件的方块。

| 可检查的数据 | 此事件提供的内容 |
| --- | --- |
| 支持的 `count` | `events`、`blocks` |
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
| 平台要求 | 需 Paper 模式及对应事件类 |
| `PLAYER`：任务玩家 | 此事件关联的玩家 |
| `SOURCE`：来源实体 | 任务玩家 |
| `TARGET`：目标实体 | 未提供 |
| LOCATION | 事件方块中心 |
| TRIGGER_ITEM / slot | 主手 / HAND |
| BLOCK | 挖掘进度发生变化的方块 |

## `interact`

统计玩家交互事件。点空气、方块和物品时可用的数据不同；只有存在方块或物品时，相应条件才有数据可检查。

| 可检查的数据 | 此事件提供的内容 |
| --- | --- |
| 支持的 `count` | `events` |
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
| 平台要求 | Paper 和 Spigot |
| `PLAYER`：任务玩家 | 此事件关联的玩家 |
| `SOURCE`：来源实体 | 任务玩家 |
| `TARGET`：目标实体 | 任务玩家 |
| LOCATION | 接收方块位置 |
| TRIGGER_ITEM / slot | 未提供 |

## `brew`

支持 `count: events` 和 `count: items`。在酿造完成时触发，使用酿造后的药水结果；未改变的瓶子不计入结果。`events` 每批计 1，`items` 按符合条件的药水数量计数。使用 `count: items` 和 `COUNTED_ITEM` 物品匹配，可筛选指定药水；`TRIGGER_ITEM` 为第一份已改变的产物，槽位留空。

## `cook`

支持 `count: events` 和 `count: items`。在熔炉烧制完成时触发，包含普通熔炉、烟熏炉和高炉，使用烧制后的产物。`events` 每次完成计 1，`items` 按符合条件的产物数量计数。可用 `COUNTED_ITEM` 物品匹配限制为 `COOKED_CHICKEN` 等熟食；不配置筛选时也会统计其他烧制产物。`TRIGGER_ITEM` 为烧制产物，槽位留空。取出已有成品不会再次触发；营火不包含在此事件中。

### 酿造和烹饪的玩家归属

这两个完成事件本身没有玩家。插件会在点击、Shift 点击或拖拽后检查原料槽，实际放入原料的玩家成为设备当前操作者。仅打开界面、放燃料和取出成品不会接管归属。酿造原料槽为瓶子及材料槽；熔炉为输入槽。

玩家 UUID 保存在设备 PDC 中，跨重启保留；不保留长期玩家对象缓存。原料槽全部清空时清除记录，漏斗等库存自动输入也会清除记录。未记录操作者、操作者离线或事件被取消时不计数。多人合用设备时，完成事件归属最后实际放入原料的玩家，而非逐份追踪物品所有权。`PLAYER` / `SOURCE` 为操作者，`BLOCK` / `LOCATION` 为设备，支持 Paper 和 Spigot。


## `craft_item`

支持 `count`: `events`, `items`. 合成物品并成功取出成品时触发。`events` 每次合成点击计 1，`items` 按产物数量计数，支持 Shift 批量合成并考虑背包容量；创造模式克隆不计入。

## `fertilize_block`

支持 `count`: `events`, `blocks`. 玩家用骨粉成功施肥且产生方块变化时触发。`events` 每次使用计 1；`blocks` 按实际变化方块计数。`BLOCK` 为施肥目标，触发物品和手来自原始交互。发射器等无玩家事件不计数。

## `forge_item`

支持 `count`: `events`, `items`. 在锻造台成功取出成品时触发，包括装备升级和纹饰。仅查看预览不会触发。

## `grindstone_item`

支持 `count`: `events`, `items`. 在砂轮成功取出处理后的成品时触发，包括修补和祛除附魔。仅放入或查看物品不触发。

## `milk_mob`

支持 `count`: `events`, `items`. 成功获得奶桶时触发，并关联本次交互的牛、蘑菇牛或山羊为 `TARGET`。空桶点击幼年动物、失败或取消的操作不计数。

## `honey_extract`

支持 `count`: `events`, `items`. 使用玻璃瓶从满蜜蜂巢或蜂箱采集蜂蜜时触发，在下一 tick 确认蜜量下降和玻璃瓶使用结果。触发物品为玻璃瓶，产物为蜂蜜瓶。

## `honey_comb_extract`

支持 `count`: `events`, `items`. 使用剪刀从蜂巢或蜂箱剪取蜜脾时触发，产物取自 Paper 剪切方块事件的掉落物。仅支持具备该事件的 Paper。

## `anvil_repair`

支持 `count`: `events`, `items`. 在铁砧成功取出耐久损伤减少的物品时触发。仅改名不会触发此项。

## `anvil_enchant`

支持 `count`: `events`, `items`. 在铁砧成功取出新增附魔或附魔等级提高的物品时触发，包括附魔书合并；保留原有且不提升的附魔不计入。

## `anvil_rename`

支持 `count`: `events`, `items`. 在铁砧成功取出自定义名称发生变化的物品时触发，包含添加、修改和清除名称；仅修补或附魔不会误计改名。

物品数量模式使用 `COUNTED_ITEM` 筛选产物；锻造、砂轮及铁砧的触发物品为成品。一次铁砧操作可分别触发修补、附魔和改名，取决于实际发生的变化。库存成品事件会在下一 tick 确认输入物品被消耗，取消或未取出成品不会计数。蜂蜜触发发生在操作完成后，不能用来取消原始采集。除蜜脾触发器外，以上均支持 Paper 和 Spigot。

[触发器概览](../triggers.md) · [事件条件](../conditions/README.md)
