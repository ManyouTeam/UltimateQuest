# 🎲 目标池

目标池是一份“候选目标 + 抽取权重”的清单。周期任务和卷轴可以从中抽选目标，让不同玩家或不同周期拿到不同组合。它抽取的是目标，不是奖励。

## 目标池配置示例

在 `plugins/UltimateQuest/` 下创建下面三个文件。前两个是[目标模板](objectives.md)，最后一个才是池文件。

```yaml
# quests/tutorial_stone.yml
enabled: true
name: '挖掘石头'
trigger: block_break
amount: 32
conditions:
  stone:
    type: block_type
    types: [STONE]
```

```yaml
# quests/tutorial_zombie.yml
enabled: true
name: '击杀僵尸'
trigger: kill
amount: 5
conditions:
  zombie:
    type: match_entity
    target: TARGET
    match:
      entity-types: [ZOMBIE]
```

```yaml
# pools/tutorial_pool.yml
enabled: true
quests:
  tutorial_stone: 3
  tutorial_zombie: 1
```

这里石头目标权重是 3，僵尸目标权重是 1。只抽一个时，分别有 75% 和 25% 的机会。权重不需要凑成 100。

## 目标池引用

将[周期任务示例](periods.md)或[卷轴示例](scrolls.md)中的根节点 `source` 替换为：

```yaml
source:
  pool: tutorial_pool
  count: 1
```

执行 `/uq reload` 后测试新生成的周期任务或新发放的卷轴。`count: 1` 抽一个目标；改成 `2` 会抽到两个不同目标，因为目标池不重复抽取。

周期任务会生成两项独立委托，各有一份奖励。卷轴则把两个目标放进同一张卷轴，全部完成后领取一份奖励。

## 配置限制

| 项目 | 要求 |
| --- | --- |
| 文件位置 | `pools/<池ID>.yml`，只读取直接子级 `.yml`。 |
| `quests` 的键 | 已存在且启用的目标模板 ID，不是普通任务 ID。 |
| 权重 | 正整数，不接受 `0`、负数或小数；总和不能超过有符号 64 位整数范围。 |
| `source.count` | 默认 `1`，必须为 `1–45` 的整数，且不超过候选数量。 |

未知模板、非法权重或候选不足会导致加载失败。已保存的抽取结果不会因重登而改变。发物品时用到的随机奖励是另一套[奖励池](rewards/pools.md)。
