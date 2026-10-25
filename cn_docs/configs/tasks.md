# 📄 普通任务

普通任务适合“先接取，再完成，最后领奖”的玩法。一项任务可以要求玩家做一件事，也可以同时完成几个目标。普通任务成功领奖后结束；需要自动刷新的内容请使用[周期任务](periods.md)。

## 配置示例：采石任务

在 `plugins/UltimateQuest/tasks/` 新建 `tutorial_mining.yml`，填入以下完整内容。这个例子不引用其他任务、目标模板、分类或语言键。

```yaml
enabled: true
name: '&a采石入门'
description:
  - '挖掘 16 块天然石头，领取 8 个面包。'
icon:
  material: STONE_PICKAXE
source:
  name: '挖掘石头'
  trigger: block_break
  amount: 16
  conditions:
    stone:
      type: block_type
      types: [STONE]
rewards:
  bread:
    type: item
    item:
      material: BREAD
      amount: 8
```

文件名决定任务 ID：这里是 `tutorial_mining`，命令中不写 `.yml`。`name` 是显示名称，可以自由修改。

1. 执行 `/uq reload`，确认没有配置错误。
2. 执行 `/uq accept tutorial_mining`，或在 `/uq` 中找到并接取它。
3. 挖掘 16 块天然石头。默认防刷会排除插件已记录的玩家放置方块。
4. 执行 `/uq info tutorial_mining` 查看进度，完成后执行 `/uq claim tutorial_mining` 领取面包。

接取前的挖矿不会补算。示例只筛选 `STONE`，深板岩等其他方块不会计数。

## 配置项说明

| 字段 | 是否必填 / 默认值 | 作用 |
| --- | --- | --- |
| `enabled` | 默认 `true` | 设为 `false` 停用定义。 |
| `name`、`description`、`icon` | 显示设置 | 分别是名称、描述列表和菜单图标。 |
| `source` | 与 `objectives` 二选一 | 单目标简写，包含触发器、数量和条件。 |
| `rewards` | 可省略 | 领奖时发放的奖励，见[奖励配置](rewards/README.md)。 |
| `auto-claim` | 默认 `false` | 完成后自动尝试领奖。 |
| `progress-feedback` | 默认开启 | 可设为 `false` 关闭进度提示，或配置节覆写全局样式。 |
| `allow-abandon` | 默认 `true` | 仅普通任务生效；设为 `false` 后，命令和菜单均不能放弃该任务。 |
| `hide-in-menu` | 默认 `false` | 仅普通任务生效；设为 `true` 后，所有状态均从菜单隐藏，仍可通过命令接取、查看及领奖。 |
| `auto-hide` | 默认 `false` | 仅普通任务生效；设为 `true` 后，未接取且不满足接取条件的任务自动隐藏，满足条件后显示；已接取任务继续显示。 |
| `categories` | 默认空列表 | 分类 ID，见[分类](categories.md)。 |
| `prerequisites` | 默认空列表 | 只有这些普通任务已领奖，才能接取本任务。 |
| `accept-mode` | 默认 `player` | `admin` 表示只能由管理员派发。 |
| `listed` | 默认 `true` | 设为 `false` 隐藏尚未派发的任务。 |
| `conditions` | 可省略 | 根节点条件在接取时检查，只能使用当时可用的玩家数据。 |

`source.conditions` 检查每次行为；根节点 `conditions` 检查能否接取。两者配置位置不同。检查接取玩家状态时，显式写 `target: PLAYER`。

`hide-in-menu` 优先于 `auto-hide`。`auto-hide` 使用菜单已有的接取资格判定，包括根节点条件、前置任务、活动时间和接取模式，不检查任务名额。它不因目标条件暂时不满足而隐藏已接取任务。`listed: false` 仅隐藏尚未接取的任务；需要始终隐藏时使用 `hide-in-menu: true`。

例如，在普通任务配置顶层添加以下字段，可禁止放弃，并在接取条件不满足时自动隐藏：

```yaml
allow-abandon: false
hide-in-menu: false
auto-hide: true
```

## 任务进度提示

内置里程碑、移动类普通任务和每日探索挑战默认关闭进度提示；移动目标模板也默认关闭，因此被周期任务、卷轴或混合挑战引用时，移动部分不会触发进度提示。完成、阶段升级和领奖提示仍保留。

在任务文件顶层设置 `progress-feedback: false`，可单独关闭该任务的进度提示，也可以使用配置节覆写全局设置。普通任务、战令、里程碑和卷轴都写在各自文件顶层；战令挑战写在 `tasks.<挑战ID>.progress-feedback`；周期任务写在 `task.progress-feedback`。这些设置在 `/uq reload` 后对已有对象生效。

全局样式位于 `config.yml`，默认显示 BossBar，也可改为 ActionBar 或同时显示：

```yaml
task-progress:
  enabled: true
  cooldown-ticks: 5
  display: BOSS_BAR
  actionbar: '[actionbar]{lang:quest-progress}[/actionbar]'
  bossbar:
    title: '{lang:quest-progress}'
    color: BLUE
    style: SOLID
  sound:
    name: 'minecraft:entity.experience_orb.pickup'
    volume: 0.25
    pitch: 1.35
```

`display` 支持 `BOSS_BAR`、`ACTION_BAR`、`BOTH`。`enabled: false` 关闭全部进度提示；音效名称留空可静音。冷却按玩家共享，单位为 tick（20 tick 为 1 秒），设为 `0` 关闭冷却。冷却期间仍正常计数；若一次行为推进多项任务，共享冷却只允许其中一项显示反馈。

单个任务可只覆写需要的字段，其余字段继承 `task-progress`。例如：

```yaml
progress-feedback:
  enabled: true
  cooldown-ticks: 10
  display: ACTION_BAR
  actionbar: '[actionbar]&e{quest} &7进度：&a{progress}/{required}[/actionbar]'
  sound:
    name: ''
messages:
  completed: '&a{quest} 已完成！'
  claimed: '&a已领取 {quest} 的奖励。'
```

全局 `task-progress.enabled: false` 始终关闭所有进度提示。对象自己的 `progress-feedback.enabled: false` 只关闭该对象。`messages` 独立于进度提示开关，空字符串可关闭对应消息。

语言文件的 `override-lang.quest-progress` 控制默认进度文案。进度、完成、领奖、升级等相关提示，以及现有 `level-up.message` 均支持以下名称变量：

| 变量 | 含义 |
| --- | --- |
| `{quest}`、`{task}`、`{name}` | 当前对象名称；战令和里程碑提示中为战令或里程碑名称。 |
| `{battlepass}`、`{battlepass-id}` | 当前或所属战令名称、ID；挑战提示可同时使用 `{quest}` 显示挑战名称。 |
| `{milestone}` | 当前里程碑名称。 |
| `{scroll}` | 当前卷轴名称。 |
| `{period}` | 当前周期名称。 |
| `{id}`、`{type}`、`{source}`、`{player}` | 当前定义 ID、类型、来源、玩家名称。 |
| `{level}`、`{total}`、`{season}` | 当前等级、累计进度、赛季 ID。 |

不适用的名称变量为空。进度提示额外支持 `{progress}` 或 `{current}`（当前进度）、`{required}`（所需进度）、`{gained}`（本次实际增加量）、`{percent}`（整数百分比）。多目标任务汇总所有目标的进度和要求数量；战令和里程碑显示当前等级区间的进度，到最高级后显示累计值。小数会保留，超额贡献只显示实际计入的部分。`{gained}` 不累计冷却期间跳过的提示。

`messages` 使用语言键的末段名称覆写，例如 `accepted`、`abandoned`、`completed`、`task-completed`、`claimed`、`level-up`、`xp-added`；卷轴还支持 `given`、`activated`。周期任务的 `messages` 写在 `task.messages`，战令挑战写在对应挑战节点下。

完成提示 `quests.completed` 和战令挑战完成提示 `battlepass.task-completed` 均支持 `{quest}`。例如可将 `quests.completed` 改为：

```yaml
quests:
  completed: '&#98FB98[UltimateQuest] &a任务 {quest} &a已完成，打开 /uq 领取奖励。'
```

## 多目标任务

将完整示例中的 `source` 替换为下面的 `objectives`。保留名称、图标和奖励等其余字段。此时需要同时挖 16 块石头、击杀 5 只僵尸才能领奖。

```yaml
objectives:
  mining:
    source:
      trigger: block_break
      amount: 16
      conditions:
        stone:
          type: block_type
          types: [STONE]
  hunting:
    source:
      trigger: kill
      amount: 5
      conditions:
        zombie:
          type: match_entity
          target: TARGET
          match:
            entity-types: [ZOMBIE]
```

`mining` 和 `hunting` 是这项任务内部的目标 ID，不是其他文件名。重复使用同一要求时，可以改成引用[目标模板](objectives.md)。

## 前置任务、派发与活动时间

写入 `prerequisites: [tutorial_mining]` 会要求玩家先给前一个任务领奖。仅完成但没领奖仍不能解锁。被引用的任务必须存在，前置关系不能循环。

管理员可以用 `/uq accept <任务ID> <在线玩家>` 派发任务，适用于 `accept-mode: admin` 或隐藏任务。加 `-b` 可跳过前置、接取条件和活动时间检查，仍会检查名额和重复接取。

限时活动可在根节点设置 `starts-at`、`ends-at`，使用带时区的 ISO-8601 时间，例如 `'2026-11-01T00:00:00Z'`。`claim-grace-seconds` 默认为 `0`，只延长活动结束后的领奖时间，不延长做任务的时间。`paused: true` 暂停活动和领奖。

## 配置修改与已有任务

任务在接取时保存当时的目标和确定的奖励。重载不会改写已接任务。请用尚未接过该任务的测试玩家，或在允许放弃的情况下放弃后重新接取。

完成但未领奖的任务仍占[任务名额](task-limits.md)。允许放弃的普通任务可以使用 `/uq abandon <任务ID>` 放弃，已有进度会丢失。

`allow-abandon`、`hide-in-menu` 和 `auto-hide` 在 `/uq reload` 后读取当前配置，无需重新接取任务。接取和放弃成功提示分别使用语言键 `quests.accepted`、`quests.abandoned`，可用 `{quest}` 显示对应任务名称。
