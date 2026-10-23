# 🔢 计数方式

`count` 决定一次符合条件的事件增加多少进度。默认 `events` 表示加 1；数量模式则读取该事件支持的数量。普通目标的 `amount` 仍为正整数，但进度可以包含小数。

## 支持范围

所有内置触发器均支持 `events`。以下是额外支持的方式；没有列出的触发器只支持 `events`，每个触发器参考条目也会标明可选值。

| `count` | 支持的触发器 | 累计内容 |
| --- | --- | --- |
| `items` | `fish`、`block_drop_item`、`piglin_barter`、`trade`、`purchase`、`kill` | 钓获物、掉落物或交易产物的堆叠数量之和。 |
| `blocks` | `block_place` | 本次实际涉及的放置方块数，逐块检查条件。 |
| `damage` | `attack`、`melee_attack`、`damage`、`damage_by_entity` | 事件最终伤害，计入护甲等修正；不按目标剩余生命截断。 |
| `blocked_damage` | `shield_block` | 盾牌实际格挡的伤害量。 |
| `healing` | `regain` | 正数恢复量，最多计入玩家缺失的生命值。 |
| `durability` | `item_damage` | 事件损耗量，最多计入物品剩余耐久。 |
| `experience` | `exp_change`、`pickup_experience` | 正数 Minecraft 经验点数，不是等级。 |
| `distance` | `move` | 相邻事件坐标之间的三维直线距离，单位为方块。 |
| `exhaustion` | `exhaustion` | 正数消耗度数值。 |

`death`、`block_break`、`air_change`、`food_level_change`、`warden_anger_change` 本次仍只支持 `events`。事件中即使存在经验或状态变化值，也不会自动成为可选的计数方式。例如 `block_break + items` 会报错；统计挖掘产物应使用 `block_drop_item + items`。

## 配置示例

累计造成 100 点最终伤害，将以下片段放到普通任务的 `source`：

```yaml
source:
  name: '累计造成 100 点伤害'
  trigger: attack
  count: damage
  amount: 100
```

累计挖出 64 个钻石，按掉落物数量统计，附魔时运产生的多个钻石也会计入：

```yaml
source:
  name: '挖出 64 个钻石'
  trigger: block_drop_item
  count: items
  amount: 64
  conditions:
    diamond:
      type: match_item
      item: COUNTED_ITEM
      match:
        material: [DIAMOND]
```

使用普通的 `conditions.match_item`，将 `item` 设为 `COUNTED_ITEM`，逐项检查本次产出的物品堆，只累计通过条件的数量。`match` 仍使用 [MatchItemFormat](../shared-formats/match-item-format/README.md)。没有物品条件时会累计全部产物。`CONTEXT`、`TRIGGER_ITEM` 等选择器保持原意，例如方块掉落事件中的事件物品仍是工具。`COUNTED_ITEM` 只在 `count: items` 逐项检查条件期间存在；其他场景中不匹配。

物品筛选统一放在 `conditions`，不再读取 `count-items`，也不为旧字段额外报错。UQ 的 `amount` 是完成目标所需的累计数量；ER 的 `count-every` 是可重复结算的批次大小，二者用途不同。

## 小数进度与倍率

伤害、恢复量、距离和消耗度可以包含小数。计数及保存精度为 6 位小数，按四舍五入处理。例如 5 次各 0.2 点伤害可累计为 1，重登或重启会保留已保存的小数进度。菜单和相关 PlaceholderAPI 数值显示包含小数，整数不会多出 `.0`。

里程碑每次贡献为计数结果乘以正整数 `value`。战令挑战使用普通目标规则，完成后按原配置发放战令积分；`experience` 不会直接把 Minecraft 经验变成战令积分。

## 事件范围与防刷

被取消的事件，以及数量为零或负数的事件，不增加数量进度。没有产物的钓鱼事件不按 1 个物品补算。掉落物数量来自事件提供的列表，不要求玩家已经拾入背包。

`move + distance` 计算每段移动，同一格内移动也可累计；只转头、传送和跨世界跳转不增加距离。`move + events` 保留跨越方块坐标才计一次的原有规则。疾跑和游泳距离使用 `move + distance`，再添加 `sprinting` 或 `swimming` 事件条件；具体状态限制见[移动与输入](triggers/movement-input.md)。

[防刷](anti-abuse.md)仍按事件执行。冷却期间被跳过的事件不会补回距离或其他数量；多件产物或多块放置在一次事件内只进行一次防刷判定。不要同时用 `exp_change` 和 `pickup_experience` 两个来源累计同一次经验获取，除非确实需要分别计算。

## 已有任务与存档

省略 `count` 的配置继续按事件次数累计。不支持的 `count` 会在加载时报告允许的值，修正配置后执行 `/uq reload`。

升级前已保存的普通任务、周期任务和卷轴快照中，非钓鱼触发器的 `count: items` 原来实际上按次数计数；读取旧快照时会转换为 `events` 并记录日志，避免已有进度突然改变含义。新生成任务按新配置执行。里程碑会刷新当前定义，升级时请同时检查其 `sources`。

存档保留原有整数进度字段，另外保存小数余量。现有 Java API 中返回 `long` 的进度接口继续返回整数部分；需要小数时使用 `QuestInstance.progressAmount` 或 `totalAmount`。无需手动修改内部的 `counting-version` 或余量字段。
