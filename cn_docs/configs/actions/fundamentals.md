# ✨ 文本与视觉动作

本页说明菜单点击后可以执行的操作。每段示例都是动作片段，放在 `items.<符号>.actions` 或 `contents.<符号>.actions` 下；动作不是任务奖励类型。完整放置示例见[动作基础](README.md)。

## `message`

给点击按钮的玩家发送聊天消息，适合简单反馈。文本变量按当前菜单或条目解析。

| 字段 | 默认值 | 说明 |
| --- | --- | --- |
| `message` | 必填 | 发送文案。 |

```yaml
type: message
message: '&a你好，{player}。'
```

## `action_bar`

在快捷栏上方显示一条消息，适合短提示。它只改变提示位置，不会创建任务或发奖励。

| 字段 | 默认值 | 说明 |
| --- | --- | --- |
| `message` | 必填 | 快捷栏文案。 |

```yaml
type: action_bar
message: '&e有 {claimable} 项奖励可领取。'
```

## `announcement`

向在线玩家广播聊天公告。玩家变量按发起动作的玩家解析，不会为每个接收者重新选择任务。

| 字段 | 默认值 | 说明 |
| --- | --- | --- |
| `message` | 必填 | 公告文案。 |

```yaml
type: announcement
message: '&e{player} 正在查看任务。'
```

## `title`

向玩家显示屏幕主标题和副标题。三个时间字段均以 tick 为单位且都要填写，即使值为 0。20 TPS 时 20 tick 约为一秒。

| 字段 | 默认值 | 说明 |
| --- | --- | --- |
| `main-title` | 必填 | 主标题。 |
| `sub-title` | 必填 | 副标题。 |
| `fade-in` | 必填 | 淡入 tick。 |
| `stay` | 必填 | 停留 tick。 |
| `fade-out` | 必填 | 淡出 tick。 |

```yaml
type: title
main-title: '&a任务'
sub-title: '&7选择任务'
fade-in: 10
stay: 40
fade-out: 10
```

## `sound`

在玩家处播放声音。`volume` 调音量，`pitch` 调音调；声音名称须受当前服务端支持。

| 字段 | 默认值 | 说明 |
| --- | --- | --- |
| `sound` | 必填 | 声音键/名称。 |
| `volume` | 1 | 音量。 |
| `pitch` | 1 | 音调。 |

```yaml
type: sound
sound: minecraft:entity.player.levelup
volume: 1
pitch: 1
```

## `particle`

在玩家上方一格生成粒子。数量、三个偏移和 speed 都要填写；粒子名称与额外数据要求随服务端版本变化。

| 字段 | 默认值 | 说明 |
| --- | --- | --- |
| `particle` | 必填 | 服务端粒子名。 |
| `count` | 必填 | 数量。 |
| `offset-x / offset-y / offset-z` | 必填 | 三个扩散分量。 |
| `speed` | 必填 | 粒子 extra/speed 参数。 |

```yaml
type: particle
particle: HAPPY_VILLAGER
count: 8
offset-x: 0.3
offset-y: 0.3
offset-z: 0.3
speed: 0
```

## `effect`

给玩家施加药水效果。`duration: 200` 在 20 TPS 下约为 10 秒，`level: 0` 是 I 级，`1` 才是 II 级。

| 字段 | 默认值 | 说明 |
| --- | --- | --- |
| `potion` | 必填 | 已注册药水效果键。 |
| `duration` | 必填 | 持续 tick。 |
| `level` | 必填 | 从 0 开始的放大等级。 |
| `ambient / particles / icon` | true | 显示开关。 |

```yaml
type: effect
potion: minecraft:speed
duration: 200
level: 0
particles: true
```

## 配置验证

执行 `/uq reload` 后重新打开菜单并点击对应按钮。任务类动作需要被点击的任务或已选中的战令/里程碑，不能仅靠给静态按钮添加一个任务名称来建立关联。导航保留本次菜单权限绕过设置，但仍检查任务条件和奖励资格。
