# 📜 任务卷轴

卷轴是一件可以携带的任务物品。玩家带着它完成目标，再用卷轴兑换奖励。与普通任务不同，卷轴通过发放命令获得，携带在背包或副手中才会增加进度。

## 配置与发放示例

新建 `plugins/UltimateQuest/scrolls/tutorial_scroll.yml`：

```yaml
enabled: true
name: '采石卷轴'
description:
  - '携带卷轴，挖掘 16 块石头。'
item:
  material: MAP
  name: '&e采石卷轴'
icon:
  material: MAP
source:
  name: '挖掘石头'
  trigger: block_break
  amount: 16
  conditions:
    stone:
      type: block_type
      types: [STONE]
duration-seconds: 86400
rarity: common
difficulty: easy
rewards:
  emerald:
    type: item
    item:
      material: EMERALD
      amount: 2
```

执行 `/uq reload`，再由拥有 `ultimatequest.admin` 的管理员执行 `/uq scroll give Steve tutorial_scroll`。请将 Steve 换成在线测试玩家；控制台也可执行。

让玩家把卷轴放在背包或副手里，挖 16 块天然石头，再打开 `/uq scroll` 查看并领奖。领奖会消耗对应卷轴。

## 配置项说明

| 字段 | 要求 / 默认值 | 作用 |
| --- | --- | --- |
| `item` | 必填 | 实际发给玩家的物品，使用 ItemFormat。 |
| `icon` | 显示设置 | 卷轴在菜单中的图标。 |
| `source` | 与固定 `tasks` 至少提供一种 | 可写内联目标、模板引用或目标池。 |
| `tasks` | 可省略 | 固定目标列表，每项使用 `source` / 普通目标格式。 |
| `duration-seconds` | 默认 `0` | 首次激活后的有效秒数，`0` 表示不限时，不能是负数。 |
| `rarity`、`difficulty` | 默认 `common`、`normal` | 稀有度与难度标签，不自动提高要求或奖励。 |
| `rewards` | 可省略 | 所有目标完成后的一份卷轴奖励。 |

根节点 `source` 与固定 `tasks` 可以一起使用。它们合并成同一张卷轴的目标，必须全部完成；不会为每个目标各发一次根节点奖励。

## 随机目标与标签

要制作随机卷轴，在 `source` 中引用[目标池](pools.md)。抽取结果与固定目标合并。不要让固定目标 ID 与目标池候选 ID 重复，否则可能覆盖同名目标。

需要随机稀有度时，可以配置 `rarities` 正整数权重映射；随机难度使用 `difficulties`。配置了映射后，发放命令中的可选标签必须是映射中的键。未配置映射时使用 `rarity`、`difficulty` 的固定标签。

完整命令为 `/uq scroll give <在线玩家> <配置ID> [稀有度] [难度]`。标签只标识卷轴；想调整玩法和奖励，需要自己编写对应配置。

## 进度统计、有效期与保存

- 首次携带并成功激活时检查[任务名额](task-limits.md)，一张卷轴占一个名额，与目标数量无关。
- 移出背包和副手后暂停增加进度。已开始的有效期继续流逝，放进箱子不会冻结倒计时。
- 发放时保存目标与奖励定义；随机奖励在首次激活时抽取。重新登录和重载不会让它重新抽奖。
- 卷轴关联存储中的唯一记录。仅复制物品名称和描述不能制作有效卷轴。备份时应一起保留[玩家与卷轴数据](storage.md)。

修改卷轴配置后，应发放一张新卷轴测试。已经发出的卷轴继续使用发放时保存的定义。
