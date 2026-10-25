# 🎯 目标与目标模板

目标定义需要完成的行为与数量。`trigger` 选择行为，`amount` 设置完成数量，`conditions` 限制哪些行为算数。例如，“挖 16 块石头”由破坏方块触发器、数量 16 和石头条件组成。

普通任务可以直接在 `source` 中写这些要求，见[第一个任务](tasks.md)。当每日任务、卷轴等也要使用相同要求时，再把它保存为模板。

## 目标模板配置

新建 `plugins/UltimateQuest/quests/tutorial_stone.yml`：

```yaml
enabled: true
name: '挖掘石头'
trigger: block_break
amount: 16
conditions:
  stone:
    type: block_type
    types: [STONE]
```

`quests/` 存目标模板；`tasks/` 存玩家可以接取的完整任务。模板不会单独出现在普通任务列表里，也不负责发放任务奖励。

`quests/` 支持递归读取子文件夹中的 `.yml` 和 `.yaml`。内置模板按 `common/`、`daily/<分类>/`、`weekly/<分类>/`、`monthly/<分类>/` 整理。目标 ID 始终是去掉扩展名的文件名，例如 `quests/monthly/farming/monthly_crops.yml` 仍通过 `monthly_crops` 引用；不同子目录不能使用相同 ID。原来直接放在 `quests/` 下的模板仍可读取。默认文件参照 ER 的策略，仅首次启动时释放，后续保留管理员的目录布局，不再在新位置重新生成默认目标。

在普通任务中，将原来的 `source` 替换成：

```yaml
source:
  quest: tutorial_stone
  amount: 64
```

`quest: tutorial_stone` 对应模板文件名。这个任务保留模板的触发器和条件，将完成数量改为 64。奖励仍写在普通任务的 `rewards` 中。先保存模板，再执行 `/uq reload`。

## 字段与覆盖规则

| 字段 | 默认 / 要求 | 说明 |
| --- | --- | --- |
| `trigger` | 必须能解析到已注册触发器 | 如 `block_break`，也可写 `ultimatequest:block_break`。 |
| `amount` | 普通目标要求正整数 | 达到这个数量才算完成。 |
| `progress-feedback` | 默认 `true` | 设为 `false` 后，该目标推进时不触发进度提示；计数与完成提示仍正常。 |
| `count` | `events` | 计数单位，默认每次符合条件的事件加 1。可选值取决于触发器，见[计数方式](counting.md)。 |
| `conditions` | 可省略 | 不符合条件的事件不增加进度。 |
| `anti-abuse` | 使用默认防刷设置 | 见[防刷](anti-abuse.md)。 |
| `name` | 自动生成显示文本 | 建议给复杂目标填写容易理解的名称。 |
| `display` | 自动显示格式 | 未填写 `name` 时使用的格式。 |

解析顺序是：复制 `source.quest` 模板 → 应用 `source` 中的字段 → 应用与 `source` 同级的字段。后面的同名配置值优先。修改条件时请核对最终的条件组合，已有模板中的其他条件不会因为你添加一个新条件就自动消失。

模板必须存在并启用。`source.quest` 不能引用 `tasks/` 中的普通任务。模板的分类也不会自动变成使用它的任务分类。

## 进度计数方式

内置移动目标模板默认使用 `progress-feedback: false`。普通任务、周期任务、卷轴和挑战引用这些模板时会继承该开关；混合任务的其他目标仍可显示进度。在引用处设置 `source.progress-feedback: true` 可覆写模板，但整个任务的提示开关仍需开启。目标配置随接取或生成时的快照保存，已有任务需要重新接取或生成后才会使用新的目标设置。

`count: events` 按事件次数计数；改为受支持的数量模式后，`amount` 就表示对应单位的完成要求。例如 `attack` 配合 `count: damage`、`amount: 100` 表示累计 100 点最终伤害。

`count: items` 适用于钓获物、方块掉落物、击杀掉落物和交易产物等明确支持的事件。使用 `match_item` 条件和 `item: COUNTED_ITEM` 按物品逐项筛选。完整支持表、小数进度和示例见[计数方式](counting.md)。不支持的组合会在加载时报告错误。

## 其他任务类型中的使用方式

- [周期任务](periods.md)和[卷轴](scrolls.md)可以用单个模板，也可以通过[目标池](pools.md)抽取多个模板。`pool` 和 `count` 不是普通任务的通用写法。
- [战令挑战](battlepasses.md)使用普通目标的数量规则。
- [里程碑](milestones.md)使用 `value` 表示每次行为贡献多少累计进度，阶段门槛写在 `stages`，不使用普通目标的 `amount` 作门槛。

条件可用的数据由触发器决定。接取时没有破坏方块、钓获物等事件数据；不要把依赖这些数据的条件放到任务根节点。参见[事件条件](conditions/README.md)。
