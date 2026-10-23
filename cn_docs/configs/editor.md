# 🛠️ 游戏内编辑器

编辑器让管理员在游戏里修改任务、目标、菜单等配置。打开 `/uq editor`，选择配置类型，再选择文件。需要 `ultimatequest.admin`，`config.yml` 中的 `editor.enabled` 默认开启。

## 配置文件选择

`/uq editor task miner` 打开内置挖矿任务，`/uq editor scroll novice` 打开新手卷轴，`/uq editor menu main` 打开主菜单。最后一个参数是文件 ID，不带 `.yml`。

| 范围 | 对应文件 |
| --- | --- |
| `quest` | `quests/` 中的可复用目标。 |
| `task` | `tasks/` 中的普通任务。 |
| `category` | `categories/` 中的任务分类。 |
| `battlepass` | `battlepasses/` 中的战令。 |
| `milestone` | `milestones/` 中的里程碑。 |
| `period` | `periods/` 中的周期任务。 |
| `scroll` | `scrolls/` 中的任务卷轴。 |
| `pool` | `pools/` 中的目标池。 |
| `reward-pool` | `reward-pools/` 中的奖励池。 |
| `menu` | `menus/` 中的菜单。 |

## 草稿校验与应用

1. 修改文本、数值、列表、物品或嵌套字段。分类支持多选。
2. 检查草稿，选择 **校验并应用（Validate and Apply）**。
3. 如果校验报错，按提示修正，再次应用。
4. 应用成功后，用新任务或重新打开的菜单测试结果。

应用前修改只是草稿，尚未变成正式文件。应用过程会创建备份并重载相关配置，校验失败则恢复原文件。

## 外部修改与已有任务

编辑器打开期间不要同时从外部编辑同一个文件。如果文件已变化，重新打开再改。周期配置中的分类编辑对应 `task.categories`。

编辑器不会改写玩家已接任务的目标和奖励。已经发出的卷轴和解锁的随机奖励也按各自保存规则保留，见[奖励配置](rewards/README.md)。
