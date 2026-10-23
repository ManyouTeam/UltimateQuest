# 🛠️ 配置文件

所有配置均位于 `plugins/UltimateQuest/`。先按功能选择文件夹，再编辑对应文件。除全局设置外，大部分定义使用“一个文件对应一个 ID”的方式组织。

## 文件与用途

| 路径 | 用途 |
| --- | --- |
| `config.yml` | 语言、数学计算、自动保存、任务名额、数据库与分类菜单默认值。 |
| `languages/*.yml` | 消息与 `override-lang` 文案；内置 `en_US`、`zh_CN`。 |
| `quests/**/*.yml` | [可复用目标模板](../configs/objectives.md)，递归读取子目录，供普通任务、周期任务等引用。 |
| `tasks/*.yml` | [普通任务](../configs/tasks.md)。 |
| `categories/*.yml` | [分类名称、图标和排序](../configs/categories.md)。 |
| `pools/*.yml` | [加权目标池](../configs/pools.md)。 |
| `reward-pools/*.yml` | [可复用加权奖励选项](../configs/rewards/README.md)。 |
| `periods/*.yml` | [周期任务](../configs/periods.md)。 |
| `scrolls/*.yml` | [实体卷轴配置](../configs/scrolls.md)。 |
| `battlepasses/*.yml` | [战令与挑战](../configs/battlepasses.md)。 |
| `milestones/*.yml` | [累计里程碑](../configs/milestones.md)。 |
| `menus/*.yml` | [菜单定义](../configs/menus.md)；`settings.yml` 保存公共设置。 |
| `items/` | [已保存物品](../configs/custom-items.md)。 |
| `data/` | 关闭 SQL 时使用的 YAML 数据，见[存储](../configs/storage.md)。 |

`quests/` 保存可复用目标，`tasks/` 保存完整普通任务。例如 `quests/tutorial_stone.yml` 只定义挖石头的要求，`tasks/tutorial_mining.yml` 还包含任务显示、接取设置和奖励。不要将完整任务放进目标模板文件夹。

## ID 与加载规则

定义 ID 是去掉 `.yml` 的文件名，使用小写字母、数字、下划线或短横线。配置中的引用和命令均使用 ID，而不是玩家看到的名称。只读取定义文件夹直接子级的 `.yml`，不会递归扫描子文件夹。

`enabled` 默认 `true`。停用内置定义请设置 `enabled: false`；直接删除文件后，插件可能在下次启动时重新补充它。

## 全局配置 config.yml

| 配置节 | 用途 | 说明 |
| --- | --- | --- |
| `config-files` | 默认语言、按玩家语言选择、MiniMessage | [语言管理](../features/advanced-language-management.md) |
| `math` | 支持字段的表达式与函数开关 | [数学格式](../shared-formats/math-calculate-format.md) |
| `hook-item-method` | 第三方物品获取方式 | 默认 `DEFAULT`，无特殊需要时保留。 |
| `auto-save` | 定时保存与提示 | [存储与保存](../configs/storage.md) |
| `database` | YAML / JDBC 后端选择 | 修改后需要完整重启，不会自动迁移数据。 |
| `task-limits` | 各类型与分类的同时进行数量 | [任务名额](../configs/task-limits.md) |
| `objective-display.formats` | 未自定义目标名称时的显示格式 | [目标](../configs/objectives.md) |
| `task-display` | 前置要求和管理任务提示 | 显示文本。 |
| `category-menu` | 分类选择页公共外观 | [分类](../configs/categories.md) |
| `debug` | 排查所需的额外信息 | 平时保持关闭。 |

菜单布局在 `menus/`，公共显示设置在 `menus/settings.yml`，不是全部写入 config.yml。

## 配置修改流程

1. 备份准备修改的文件。
2. 修改对应字段，YAML 缩进使用空格；向已有节添加内容时合并节点，不要重复根键。
3. 执行 `/uq reload`，根据控制台提示修正错误。
4. 使用新接取的任务、新发放的卷轴或新生成的委托验证结果。

重载不会改写已接任务中保存的要求和奖励。更换插件 JAR、数据库设置或兼容插件等需要重启的操作，参考[安装说明](install.md)及相应配置页。
