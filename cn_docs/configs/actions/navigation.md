# 🧭 菜单导航动作

本页说明菜单点击后可以执行的操作。每段示例都是动作片段，放在 `items.<符号>.actions` 或 `contents.<符号>.actions` 下；动作不是任务奖励类型。完整放置示例见[动作基础](README.md)。

## `previous_page`

打开当前列表上一页，已经在第一页时不再后退。适合静态翻页按钮。

```yaml
type: previous_page
```

## `next_page`

打开当前列表下一页，已到最后一页时停止。

```yaml
type: next_page
```

## `refresh`

重新生成当前页，让玩家看到最新进度和可领奖状态。它不是重载配置，重载仍使用 /uq reload。

```yaml
type: refresh
```

## `close`

关闭当前背包菜单。此动作无需额外参数。

```yaml
type: close
```

## `open_menu`

打开 menus/ 中另一个已加载菜单。menu 写文件名，不带 .yml，默认 main。

| 字段 | 默认值 | 说明 |
| --- | --- | --- |
| `menu` | main | 目标菜单 ID/路径。 |

```yaml
type: open_menu
menu: main
```

## `open_categories`

打开当前菜单的分类选择页，需要先开启 category-filter.enabled。

```yaml
type: open_categories
```

## `cycle_view`

按当前菜单 view-filter.values 的顺序切换状态筛选，例如全部、进行中和可领奖。

```yaml
type: cycle_view
```

## `cycle_challenge_group`

在战令挑战列表中切换 ALL、DAILY、WEEKLY、SEASON 分组。

```yaml
type: cycle_challenge_group
```

## `cycle_period_group`

轮流显示当前可用的周期配置及 all，例如每日、每周和全部。周期配置分组不同于任务分类。

```yaml
type: cycle_period_group
```

## `set_period_group`

直接选中 group 指定的周期配置，all 表示不限制配置。目标 ID 必须在当前菜单可用分组中。

| 字段 | 默认值 | 说明 |
| --- | --- | --- |
| `group` | all | 可用周期配置 ID 或 all。 |

```yaml
type: set_period_group
group: daily
```

## `cycle_scroll_group`

轮流显示卷轴配置分组，需要 scroll-filter.enabled。它按卷轴配置筛选，不按稀有度筛选。

```yaml
type: cycle_scroll_group
```

## `set_scroll_group`

直接选择 group 指定的卷轴配置，或用 all 显示全部。需要 scroll-filter.enabled。

| 字段 | 默认值 | 说明 |
| --- | --- | --- |
| `group` | all | 卷轴配置 ID 或 all。 |

```yaml
type: set_scroll_group
group: novice
```

## 配置验证

执行 `/uq reload` 后重新打开菜单并点击对应按钮。任务类动作需要被点击的任务或已选中的战令/里程碑，不能仅靠给静态按钮添加一个任务名称来建立关联。导航保留本次菜单权限绕过设置，但仍检查任务条件和奖励资格。
