# 🧩 PlaceholderAPI

安装 PlaceholderAPI 后，可以在支持它的计分板、悬浮字等插件中显示任务和成长信息。UltimateQuest 启动时注册自己的持久扩展，不需要把菜单中的 `{name}` 直接搬到外部插件。

## 占位符使用示例

在安装 PlaceholderAPI 后完整启动服务器，让测试玩家上线并打开战令。若已按教程创建 `tutorial_pass`，可用 PlaceholderAPI 的解析命令测试 `%ultimatequest_battlepass_total_tutorial_pass%`。

例如该玩家累计 25 分，这个占位符应返回 `25`。把末尾的 ID 换成自己的战令 ID；内置战令使用 `example`。

## 战令与里程碑字段

统一格式是 `%ultimatequest_battlepass_<字段>_<ID>%`。将 `battlepass` 换成 `milestone` 可查里程碑。

| 字段 | 有对应已加载记录时的含义 |
| --- | --- |
| `total` | 累计积分或进度。 |
| `level` | 已达到的等级或阶段。 |
| `season` | 当前定义的赛季 ID；无对应定义时为空。 |
| `remaining` | 到下一累计门槛还差多少。 |
| `progress` | 超过上一个已达门槛的进度，不是累计总数。 |
| `percent` | 当前等级区间的整数百分比。 |
| `premium` | 在线玩家的高级资格，true / false。 |
| `claimable` | 状态可领取的阶段数，高级阶段还检查玩家在线时的资格。 |
| `timeleft` | 距结束剩余秒数，无结束时间为 -1，已结束为 0。 |

## 查询普通任务状态

格式为 `%ultimatequest_state_<任务或实例ID>%`，例如 `%ultimatequest_state_tutorial_mining%`。

可能返回 `ACTIVE`、`COMPLETED`、`CLAIMED` 等保存状态。没有对应实例时返回 `AVAILABLE`，这不证明定义存在，也不保证玩家满足接取条件。

## 空值与加载状态

数据未就绪时返回 `LOADING`。成长记录不存在时，多数数值类字段返回 `0`，不能把它当作对离线数据的自动加载。高级资格检查需要在线玩家。未知占位符格式没有返回值，具体显示方式取决于调用插件。

任务和菜单内部使用的 `{name}`、`{progress}` 等变量另见[菜单参考](../menus/builtin-options.md)；`{lang:...}` 另见[语言管理](advanced-language-management.md)。
