# 📂 动作

动作规定玩家点击菜单后做什么，例如刷新列表、打开另一页、接取任务或播放声音。奖励规定完成任务后发什么，两者支持的类型不同。

## 多动作配置示例

将下面片段合入菜单 `items`，并在 `layout` 中放置符号 `R`。完整菜单见[菜单配置](../menus.md)。

```yaml
items:
  R:
    item:
      material: COMPASS
      name: '&e刷新'
    actions:
      update:
        type: refresh
      feedback:
        type: sound
        sound: minecraft:ui.button.click
```

点击后先刷新，再播放按钮声音。`update`、`feedback` 是条目名，多项动作按 YAML 顺序执行。只需要一项时，可以在 `actions` 下直接写 `type`。

## 静态按钮与任务条目

- `items.<符号>.actions` 是固定按钮，例如关闭、翻页。
- `contents.<符号>.actions` 用于动态条目，例如点击列表中的某项任务。
- `quest`、`quest_info`、`abandon_quest` 等需要知道被点击的是哪项任务。它们不能通过随意增加一个任务 ID 字段来操作别的任务。
- `claim_all` 和 `open_challenges` 需要当前菜单已经选中战令或里程碑等对应对象。

## 点击类型配置

把下面片段放在动态任务内容区，例如 `contents.Q` 下：

```yaml
click-actions:
  LEFT:
    type: quest
  RIGHT:
    type: abandon_quest
    confirm: true
    confirm-seconds: 5
```

`click-actions.LEFT` 替代左键的通用 `actions`，右键同理。还可使用 `SHIFT_LEFT` 等服务端点击类型。显式配置一个空的对应点击节点会阻止回退到通用动作。

动作类型名会标准化大小写和短横线，文档统一采用小写下划线。动作不普遍支持每条自带 `conditions`、`chance` 或 `cooldown`；随机执行请使用 `chance` 包装，条件写在功能实际支持的位置。

## 动作类型索引

| 类型 | 参考 |
| --- | --- |
| `message` | [message](fundamentals.md#message) |
| `action_bar` | [action_bar](fundamentals.md#action_bar) |
| `announcement` | [announcement](fundamentals.md#announcement) |
| `title` | [title](fundamentals.md#title) |
| `sound` | [sound](fundamentals.md#sound) |
| `particle` | [particle](fundamentals.md#particle) |
| `effect` | [effect](fundamentals.md#effect) |
| `console_command` | [console_command](commands-world.md#console_command) |
| `player_command` | [player_command](commands-world.md#player_command) |
| `op_command` | [op_command](commands-world.md#op_command) |
| `command` | [command](commands-world.md#command) |
| `teleport` | [teleport](commands-world.md#teleport) |
| `entity_spawn` | [entity_spawn](commands-world.md#entity_spawn) |
| `chance` | [chance](orchestration.md#chance) |
| `any` | [any](orchestration.md#any) |
| `delay` | [delay](orchestration.md#delay) |
| `previous_page` | [previous_page](navigation.md#previous_page) |
| `next_page` | [next_page](navigation.md#next_page) |
| `refresh` | [refresh](navigation.md#refresh) |
| `close` | [close](navigation.md#close) |
| `open_menu` | [open_menu](navigation.md#open_menu) |
| `open_categories` | [open_categories](navigation.md#open_categories) |
| `cycle_view` | [cycle_view](navigation.md#cycle_view) |
| `cycle_challenge_group` | [cycle_challenge_group](navigation.md#cycle_challenge_group) |
| `cycle_period_group` | [cycle_period_group](navigation.md#cycle_period_group) |
| `set_period_group` | [set_period_group](navigation.md#set_period_group) |
| `quest` | [quest](quests.md#quest) |
| `quest_info` | [quest_info](quests.md#quest_info) |
| `abandon_quest` | [abandon_quest](quests.md#abandon_quest) |
| `claim_all` | [claim_all](quests.md#claim_all) |
| `open_challenges` | [open_challenges](quests.md#open_challenges) |
| `cycle_scroll_group` | [cycle_scroll_group](navigation.md#cycle_scroll_group) |
| `set_scroll_group` | [set_scroll_group](navigation.md#set_scroll_group) |

声音和标题等不能直接写进任务 `rewards`。奖励允许的类型见[奖励配置](../rewards/README.md)。随机动作在运行时选择，随机奖励则按任务或阶段保存结果。
