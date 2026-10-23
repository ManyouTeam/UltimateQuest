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

## 分类菜单

菜单需要启用 `category-filter.enabled`，按钮使用 `open_categories` 动作。公共外观在 `config.yml` 的 `category-menu` 中，各菜单可用自己的 `category-filter` 覆盖标题、布局、物品和按钮。

分类选择页布局使用 `Q` 放分类，`P`、`N` 翻页，`B` 返回，`C` 关闭。卡片可使用 `{category-id}`、`{category-name}`、`{category-description}`、`{category-count}`、`{category-active}`、`{category-claimable}`、`{category-selected}`。任务卡上的 `{categories}` 显示所属分类。

`category-filter.hide-empty: true` 隐藏当前筛选范围内数量为零的分类；默认配置的 main 和 scroll 菜单已开启。代码默认值为 `false`，“全部”入口保留。分类筛选可以与状态及周期/卷轴配置筛选组合，切换分类时返回第一页。

## 分类与任务名额

多分类任务分别计入各分类限制，但类型总量只计一次。普通任务和战令使用当前分类定义，已经生成的周期任务、卷轴保留生成时的分类。详见[名额设置](task-limits.md)。

管理员也可用 `/uq editor category` 编辑分类；周期编辑器中的分类修改的是 `task.categories`。完整界面示例见[菜单配置](menus.md)。
