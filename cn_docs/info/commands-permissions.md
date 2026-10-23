# ⌨️ 命令与权限

主命令是 `/ultimatequest`，别名为 `/uq` 和 `/quests`。不带参数时打开主菜单。下文的 `<参数>` 为必填，`[参数]` 为可选；输入命令时不写这些括号。

## 玩家命令

默认玩家使用权限为 `ultimatequest.use`。命令中的任务 ID 使用文件名，不带 `.yml`；动态生成内容可能需要具体实例 ID。

| 玩家命令 | 用途 |
| --- | --- |
| `/uq help` | 查看帮助。 |
| `/uq menu [menu]` | 打开菜单，默认 `main`。 |
| `/uq accept <task>` | 接取普通任务。 |
| `/uq info <task-or-instance>` | 查看目标/奖励。 |
| `/uq claim <task-or-instance>` | 领取完成任务奖励。 |
| `/uq abandon <task-or-instance>` | 放弃符合条件的普通任务。 |
| `/uq period` / `/uq scroll` | 查看周期任务/携带卷轴。 |
| `/uq battlepass [id]` / `/uq milestone [id]` | 打开成长目录/详情。 |
| `/uq battlepass claim <id> <level> [free\|premium]` | 领取等级奖励，默认 `free`。 |
| `/uq milestone claim <id> <stage>` | 领取里程碑阶段。 |
| `/uq battlepass claimall <id>` | 领取全部符合条件的阶段。 |
| `/uq battlepass history <id>` | 列出已保存赛季。 |
| `/uq battlepass challenges <id>` | 打开挑战。 |
| `/uq battlepass stats <id>` | 查看成长统计。 |

`milestone` 也支持共用处理器的 `claimall`、`history`、`challenges` 和 `stats` 子命令。保存的赛季使用 `<ID>@<赛季ID>` 指定。

## 管理命令

```text
/uq reload
/uq accept <task> <online-player> [-b]
/uq scroll give <online-player> <profile> [rarity] [difficulty]
/uq battlepass addxp <player-or-UUID> <id> <amount>
/uq battlepass setxp <player-or-UUID> <id> <total>
/uq battlepass removexp <player-or-UUID> <id> <amount>
/uq battlepass setlevel <player-or-UUID> <id> <level>
/uq battlepass premium <player-or-UUID> <id> <true|false>
/uq battlepass boost <player-or-UUID> <id> <multiplier> <seconds>
/uq battlepass resolve <player-or-UUID> <id> <track/stage> <AVAILABLE|CLAIMED>
```

派发、卷轴发放和成长管理需要 `ultimatequest.admin`。成长管理也适用于 `milestone`，可处理有记录的离线玩家。积分和等级命令修改对应记录，不代表重新分配所有挑战。

`resolve` 处理处于 `CLAIMING` / `REVIEW` 的阶段。使用前先核对实际发奖结果；改回 AVAILABLE 可能让已发出的奖励再次发放。赛季命令见[战令](../configs/battlepasses.md)。

## 控制台操作

控制台给玩家打开菜单或操作任务时，需要指定在线玩家。控制台输入不带开头的 `/`。

```text
uq menu [menu] <player> [-b]
uq accept <task> <player> [-b]
uq info <task-or-instance> <player>
uq claim <task-or-instance> <player>
uq abandon <task-or-instance> <player>
uq period <player> [-b]
uq scroll <player> [-b]
uq battlepass [id] <player> [-b]
uq battlepass challenges <id> <player> [-b]
uq battlepass claim <id> <level> [free|premium] <player>
uq battlepass claimall <id> <player>
uq milestone [id] <player> [-b]
```

菜单 `-b` 跳过菜单自身权限，并在本次菜单跳转中保留，不跳过禁用状态、任务条件或奖励资格。管理员 `accept -b` 跳过前置、接取条件和活动检查，仍检查名额和重复接取。玩家为其他玩家派发需要管理权限。

## 权限节点

| 权限 | 默认 | 用途 |
| --- | --- | --- |
| `ultimatequest.use` | 所有人 | 玩家命令与默认菜单。 |
| `ultimatequest.reload` | OP | 重载。 |
| `ultimatequest.admin` | OP | 派发、卷轴发放、成长管理。 |
| `ultimatequest.saveitem` | OP | `/uq saveitem <id> [bukkit\|itemformat]`。 |
| `ultimatequest.givesaveitem` | OP | `/uq givesaveitem <id> [player] [amount]`；控制台必须指定玩家。 |
| `ultimatequest.generateitemformat` | OP | 导出主手物品。 |
| `ultimatequest.battlepass.example.premium` | 无人 | 默认战令高级资格。 |

菜单和其他战令还可配置自己的 `permission`、`premium-permission`，不局限于表中的默认示例。

## 物品命令

`/uq saveitem <ID> [bukkit|itemformat]` 保存主手物品；`/uq givesaveitem <ID> [玩家] [数量]` 发放已保存物品，控制台必须填写玩家；`/uq generateitemformat` 导出到 `generated-item-format.yml`。详细步骤见[保存和复用物品](../configs/custom-items.md)。

## 使用示例

`/uq accept tutorial_mining`、`/uq info tutorial_mining`、`/uq claim tutorial_mining` 依次完成接取、查询和领奖；使用前先按[普通任务教程](../configs/tasks.md)创建该任务。修改配置后执行 `/uq reload`，重载权限为 `ultimatequest.reload`。
