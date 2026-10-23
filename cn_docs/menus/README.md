# 📖 菜单类型

菜单让玩家浏览任务、查看进度和领取奖励。你可以先调整内置菜单的标题、图标和按钮，也可以新建自己的菜单。

## 内置菜单

配置均位于 `plugins/UltimateQuest/menus/`。

| 文件 | 打开方式 | 玩家看到什么 |
| --- | --- | --- |
| `main.yml` | `/uq` | 普通任务，支持接取、查看、领奖与符合条件的放弃操作。 |
| `period.yml` | `/uq period` | 自动分配的周期委托。 |
| `scroll.yml` | `/uq scroll` | 当前携带的卷轴与兑换入口。 |
| `battlepass.yml` | `/uq battlepass [ID]` | 战令目录、等级奖励和挑战。 |
| `milestone.yml` | `/uq milestone [ID]` | 里程碑目录与阶段奖励。 |

`settings.yml` 存放所有菜单的公共设置，不能作为菜单打开。其他 `.yml` 文件名是菜单 ID，例如 `tutorial_menu.yml` 用 `/uq menu tutorial_menu` 打开。

## 配置修改步骤

1. 备份想修改的菜单文件。
2. 改标题、布局、显示物品或点击动作。
3. 执行 `/uq reload`，重新打开菜单测试。

普通任务默认左键按状态接取、查看或领奖；右键可配置放弃，并要求二次确认。按钮外观和点击行为是两回事，修改物品名称不会自动改变动作。

创建新菜单请用[完整配置示例](../configs/menus.md)。逐项查字段见[内置菜单选项](builtin-options.md)，查按钮行为见[菜单动作](actions.md)。

## 菜单权限与控制台操作

默认玩家权限为 `ultimatequest.use`。每个菜单还可设置 `permission`，`enabled: false` 的菜单不能打开。

控制台可执行 `uq menu main Steve` 为在线玩家打开菜单。加 `-b` 可跳过菜单自身权限检查，但仍检查任务条件和奖励资格，详见[命令与权限](../info/commands-permissions.md)。

菜单中的任务名称、目标和奖励来自对应任务配置；多语言文本和颜色分别见[语言管理](../features/advanced-language-management.md)与[颜色代码](../features/color-code.md)。
