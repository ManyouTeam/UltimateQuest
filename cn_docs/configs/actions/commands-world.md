# 🔌 命令与世界动作

本页说明菜单点击后可以执行的操作。每段示例都是动作片段，放在 `items.<符号>.actions` 或 `contents.<符号>.actions` 下；动作不是任务奖励类型。完整放置示例见[动作基础](README.md)。

## `console_command`

以控制台身份执行命令。命令不带开头的 `/`，需要目标玩家时在命令中填写 `{player}`。

| 字段 | 默认值 | 说明 |
| --- | --- | --- |
| `command` | 必填 | 不含开头斜线的命令。 |

```yaml
type: console_command
command: uq info miner {player}
```

## `player_command`

让点击者以自己的权限执行命令。命令本身需要的权限仍由目标插件检查。

| 字段 | 默认值 | 说明 |
| --- | --- | --- |
| `command` | 必填 | 不含开头斜线的命令。 |

```yaml
type: player_command
command: uq info miner
```

## `op_command`

通过临时 OP 路径执行点击者命令。只有明确需要该权限方式的固定管理配置才应选择此类型。

| 字段 | 默认值 | 说明 |
| --- | --- | --- |
| `command` | 必填 | 不含开头斜线的命令。 |

```yaml
type: op_command
command: uq info miner
```

## `command`

这是菜单保留的兼容写法：默认控制台执行，`as-console: false` 改为玩家执行。新配置使用 console_command 或 player_command 更容易看懂执行身份。

| 字段 | 默认值 | 说明 |
| --- | --- | --- |
| `command` | 必填 | 不含开头斜线的命令。 |
| `as-console` | true | false 时玩家执行。 |

```yaml
type: command
command: uq info miner {player}
as-console: true
```

## `teleport`

把点击玩家传送到已加载世界中的指定坐标。填写 world、x、y、z，可用 yaw、pitch 设置朝向；省略朝向时保留玩家当前方向。

| 字段 | 默认值 | 说明 |
| --- | --- | --- |
| `world` | 必填 | 已加载世界名。 |
| `x / y / z` | 必填 | 目标坐标。 |
| `yaw / pitch` | 玩家当前朝向 | 目标朝向。 |

```yaml
type: teleport
world: world
x: 0.5
y: 80
z: 0.5
```

## `entity_spawn`

在点击玩家处生成原版实体，或填写完整位置字段指定地点。entity 使用 Bukkit 实体类型，不是 MythicMobs 自定义 ID。

| 字段 | 默认值 | 说明 |
| --- | --- | --- |
| `entity` | 必填 | Bukkit 实体类型。 |
| `world / x / y / z` | 玩家位置 | 自定义位置时填写全部位置字段。 |

```yaml
type: entity_spawn
entity: VILLAGER
```

## 配置验证

执行 `/uq reload` 后重新打开菜单并点击对应按钮。任务类动作需要被点击的任务或已选中的战令/里程碑，不能仅靠给静态按钮添加一个任务名称来建立关联。导航保留本次菜单权限绕过设置，但仍检查任务条件和奖励资格。
