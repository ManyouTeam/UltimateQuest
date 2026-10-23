# 🛠️ 配置菜单

菜单由布局、固定按钮和动态列表组成。例如，边框和关闭按钮是固定内容，玩家可接取的任务列表是动态内容。先创建一个简单菜单，再调整默认的复杂界面会更容易。

## 任务菜单配置示例

新建 `plugins/UltimateQuest/menus/tutorial_menu.yml`，填写以下完整配置：

```yaml
enabled: true
permission: ultimatequest.use
title: '&8任务 {page}/{pages}'
layout:
  - FFFFFFFFF
  - FQQQQQQQF
  - PFFFKRFCN
category-filter:
  enabled: true
contents:
  Q:
    type: quests
    filterable: true
    sort: actionable
    use-quest-icon: true
    item:
      name: '&a{name}'
      lore:
        - '{objectives}'
        - '{rewards}'
        - '{hint}'
    click-actions:
      LEFT:
        type: quest
      RIGHT:
        type: abandon_quest
        confirm: true
        confirm-seconds: 5
items:
  F:
    item: {material: BLACK_STAINED_GLASS_PANE, name: ' '}
  P:
    item: {material: ARROW, name: '上一页'}
    actions: {type: previous_page}
  N:
    item: {material: ARROW, name: '下一页'}
    actions: {type: next_page}
  K:
    category-button: true
    item: {material: CHEST, name: '分类'}
    actions: {type: open_categories}
  R:
    item: {material: COMPASS, name: '刷新'}
    actions: {type: refresh}
  C:
    item: {material: BARRIER, name: '关闭'}
    actions: {type: close}
```

执行 `/uq reload`，再用 `/uq menu tutorial_menu` 打开。`Q` 区域会列出普通任务，左键根据任务状态接取、查看或领奖，右键尝试放弃允许放弃的普通任务。

## 布局符号

每一行代表背包的一行，必须有 9 个符号，总共 1–6 行。同一个符号可以重复：示例的 7 个 `Q` 是列表的 7 个位置，不是同一任务复制 7 次。

`contents.Q` 决定 Q 区域列出什么，`items.F` 决定边框物品，其他字母对应按钮。此布局不使用单个条目的 `slot` / `slots` 设置。不要让同一符号同时代表固定按钮和内容区。

## 内容区常用选项

| 字段 | 用途 |
| --- | --- |
| `type` | `quests` 普通任务、`period` 周期、`scroll` 卷轴、`challenge` 挑战、`battlepass` 战令、`milestone` 里程碑。 |
| `include`、`exclude` | 限制显示的定义 ID，默认不指定白名单。 |
| `categories` | 固定分类范围，与玩家选择的分类取交集。 |
| `filterable` | 让内容参与当前状态筛选。 |
| `sort` | `actionable` 优先可操作内容；`natural` 使用自然顺序。 |
| `use-quest-icon` | 默认 `true`，使用任务图标；希望保留它时别在 `item` 中再填 `material`。 |
| `item`、`state-items` | 通用卡片与不同状态卡片的物品配置。 |
| `empty-item`、`empty-slot` | 没有内容时的提示物品和位置。 |

`description`、`objectives`、`rewards` 可能展开成多行，示例将每个变量单独放一行 Lore。菜单变量和 PlaceholderAPI 的 `%...%` 不同。

## 分类、状态和配置分组

分类选择使用 `category-filter` 和 `open_categories` 动作，详见[分类](categories.md)。`view-filter.values` 指定状态筛选的合法值和轮换顺序，由 `cycle_view` 切换。

周期菜单可启用 `period-filter.enabled`，通过 `cycle_period_group` 或 `set_period_group` 按周期配置筛选。卷轴菜单对应 `scroll-filter`、`cycle_scroll_group` 和 `set_scroll_group`。`groups` 可限制允许的配置 ID，`all` 表示全部。

分组按钮可分别用 `period-items.<ID>`、`scroll-items.<ID>` 覆盖外观，卷轴按钮也支持 `scroll-button`。`{period-id}` / `{period-group}`、`{scroll-id}` / `{scroll-group}` 显示当前配置标识与名称。分类、状态、配置分组可以一起使用。

## 战令与里程碑视图

这两类菜单区分目录与详情，分别配置 `catalog-title` / `catalog-layout`、`detail-title` / `detail-layout`。战令还支持 `challenge-title` / `challenge-layout`。

内容区用 `display: catalog`、`detail` 或 `challenges` 指定出现的视图。战令详情的 `track: free` / `premium` 选择奖励区；里程碑使用 `default`。目录卡使用 `catalog-item`，其他条目优先用 `state-items.<状态>`，没有则回退到 `item`。

固定按钮还支持 `hide-on-first-page`、`hide-on-last-page`、`hidden-item` 和 `view-items.<VIEW>`。逐文件选项、状态与变量见[内置菜单参考](../menus/builtin-options.md)，按钮类型见[菜单动作](../menus/actions.md)。
