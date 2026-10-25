# 🗂️ 任务分类

分类帮助玩家在菜单中找任务，例如“挖矿”“战斗”“探索”。一项任务可以同时属于多个分类，分类不支持父子层级。

## 分类配置示例

在 `plugins/UltimateQuest/` 下创建以下文件：

```yaml
# categories/tutorial.yml
enabled: true
name: '新手任务'
description:
  - '从这里熟悉服务器。'
icon:
  material: COMPASS
order: 10
```

再在[普通任务示例](tasks.md)的根节点添加 `categories: [tutorial]`，重载后打开菜单的分类按钮。`tutorial` 对应文件名；`name` 是玩家看到的名称。`order` 越小越靠前，相同时按 ID 排序。

## 分类配置位置

| 玩法 | 分类位置 |
| --- | --- |
| 普通任务、卷轴、战令目录、里程碑 | 根节点 `categories`。 |
| 周期生成任务 | `task.categories`。 |
| 战令挑战 | `tasks.<挑战ID>.categories`。 |

模板上的分类不会自动继承到任务。没有分类或空列表会归入 `uncategorized`。`all` 与 `uncategorized` 是保留 ID，不能创建同名分类。

未知分类 ID、错误类型会使定义重载失败，并保留原定义。同一分类重复填写会去重。分类设置 `enabled: false` 后不显示入口，相关任务仍能在“全部”中出现。

## 分类的进度提示颜色

分类文件顶层可以设置进度 BossBar 的默认颜色：

```yaml
progress-feedback:
  bossbar:
    color: RED
```

颜色优先级为：任务自己的 `progress-feedback.bossbar.color` → 任务分类的颜色 → `config.yml` 的 `task-progress.bossbar.color`。多分类任务按照 `categories` 顺序，使用第一个配置了颜色的分类。分类中的这个字段只决定颜色，其余提示设置继续使用任务覆写或全局默认值。

内置分类已配置对应颜色，普通任务、周期任务、卷轴、战令、里程碑和挑战均按自己的分类继承。支持 `PINK`、`BLUE`、`RED`、`GREEN`、`YELLOW`、`PURPLE`、`WHITE` 七种颜色，超过七个分类时需要复用颜色。修改后执行 `/uq reload` 生效。

## 分类菜单

菜单需要启用 `category-filter.enabled`，按钮使用 `open_categories` 动作。公共外观在 `config.yml` 的 `category-menu` 中，各菜单可用自己的 `category-filter` 覆盖标题、布局、物品和按钮。

分类选择页使用独立界面，内置布局与其他任务菜单统一为 6 行（54 格）。布局中的 `Q` 对应动态分类列表 `contents.Q`；默认的 `P`、`N`、`B`、`C` 分别配置了翻页、返回和关闭动作。固定按钮的符号可以修改，行为由 `actions` 决定。

固定按钮与普通菜单一样使用 `items.<符号>.item`、`actions` 和 `click-actions`。翻页按钮通过 `hide-on-first-page`、`hide-on-last-page` 隐藏，隐藏时显示 `hidden-item`，省略它则留空。只有一页时两个默认翻页按钮都会隐藏。

以下片段可合入 `config.yml` 的 `category-menu`，也可以合入对应菜单的 `category-filter` 覆盖公共配置：

```yaml
contents:
  Q:
    type: categories
    item:
      name: '{lang:defaults.ui.category-name}'
      lore:
        - '{lang:category.count}'
    state-items:
      SELECTED:
        glow: true
    actions:
      select:
        type: select_category
items:
  P:
    hide-on-first-page: true
    item:
      material: ARROW
      name: '{lang:menu-previous}'
      lore:
        - '{lang:defaults.ui.page-number}'
    hidden-item:
      material: BLACK_STAINED_GLASS_PANE
      name: '{lang:defaults.ui.blank}'
    actions:
      previous:
        type: previous_page
  B:
    item:
      material: ARROW
      name: '{lang:menu-back}'
    actions:
      back:
        type: back
```

`select_category` 在分类卡片上默认选择被点击的分类，也可在固定按钮上指定 `category: all` 等有效分类 ID。`back` 返回原任务列表，保留分类、状态、周期/卷轴筛选和任务页码。更换分类则从任务第一页开始。选中卡片的 `state-items.SELECTED` 继承 `item`，只需填写要覆盖的字段。

卡片和卡片动作可使用 `{category-id}`、`{category-name}`、`{category-description}`、`{category-count}`、`{category-active}`、`{category-claimable}`、`{category-selected}`；`{page}`、`{pages}` 使用分类页的页码。任务卡上的 `{categories}` 显示所属分类。

旧的扁平 `items.<符号>.material/name/lore` 和顶层 `item`、`selected-item` 会在读取时兼容转换，不会改写原文件。新配置请使用上述结构；菜单覆写的 `actions` 会整体替换继承的同节点动作，空的 `actions: {}` 可禁用动作。

如需先显示分类，再点击分类查看任务，在对应菜单文件（例如 `menus/main.yml`）中设置：

```yaml
category-filter:
  enabled: true
  open-on-start: true
  hide-empty: true
```

`open-on-start` 默认值为 `false`。开启后，每次打开该菜单先显示分类选择页，选择分类后进入对应任务列表；任务列表中的分类按钮仍可重新打开分类选择页。此选项仅在该菜单的分类筛选可用时生效，战令和里程碑的等级详情页不受影响。修改后执行 `/uq reload`。

`category-filter.hide-empty: true` 隐藏当前筛选范围内数量为零的分类；默认配置的 main 和 scroll 菜单已开启。代码默认值为 `false`，“全部”入口保留。分类筛选可以与状态及周期/卷轴配置筛选组合，切换分类时返回第一页。

## 分类与任务名额

多分类任务分别计入各分类限制，但类型总量只计一次。普通任务和战令使用当前分类定义，已经生成的周期任务、卷轴保留生成时的分类。详见[名额设置](task-limits.md)。

管理员可修改 `categories/*.yml` 中的分类配置，以及 `periods/*.yml` 中的 `task.categories`，再使用 `/uq reload` 应用更改。完整界面示例见[菜单配置](menus.md)。
