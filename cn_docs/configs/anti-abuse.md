# 🛡️ 防刷

防刷用来避免反复放置再挖掉方块、重复击杀同一玩家等行为快速刷满任务。配置写在目标的 `anti-abuse` 下，不是独立的任务类型。

## 防刷配置位置

下面是目标配置片段，可放在普通任务的根节点；如果使用多个目标，放在对应目标的 `source` 中。

```yaml
source:
  trigger: block_break
  amount: 16
  anti-abuse:
    ignore-player-placed: true
    ignore-internal-block-breaks: true
    cooldown-ms: 0
    maximum-uses-per-minute: 0
```

模板可直接配置根节点 `anti-abuse`。事件先通过目标条件，再检查防刷，全部通过才增加进度。

## 配置项说明

| 字段 | 默认行为 | 用途 |
| --- | --- | --- |
| `ignore-player-placed` | 挖掘相关触发器默认 `true`，其他为 `false` | 忽略插件已记录的玩家放置方块。 |
| `ignore-internal-block-breaks` | `true` | 忽略带有内部破坏标记的事件。 |
| `ignore-spawner-mobs` | `true` | 忽略带有插件刷怪笼标记的生物。 |
| `cooldown-ms` | `0`，关闭 | 同一玩家在该目标的两次计数之间至少间隔多少毫秒。 |
| `maximum-uses-per-minute` | `0`，关闭 | 该玩家在固定一分钟窗口中最多计数多少次。 |
| `player-victim-cooldown-seconds` | `kill` 默认 `300`，其他为 `0` | 对同一个玩家受害者重复计数的间隔。 |
| `target-cooldown-seconds` | 默认按目标及上一项决定 | 显式设置同一目标的重复计数间隔，优先于上一项。 |

挖掘相关触发器指 `block_break`、`block_drop_item`、`block_damage`、`block_break_progress_update`。放置任务默认仍正常统计放置行为。

例如 `cooldown-ms: 1000` 最多允许该目标约每秒计数一次；`maximum-uses-per-minute: 30` 则限制一个固定一分钟窗口内的计数次数，不是滚动窗口。

## 方块记录与作物筛选

插件只能排除已记录的放置和已标记的生物，不能追溯安装前的世界历史。农业任务通常需要允许玩家种植的作物，并另外检查作物成熟状态；仅选择作物材质不代表已经成熟。

防刷不替代游戏模式或世界条件。只允许生存模式时，请另外添加条件。冷却和次数窗口属于运行时状态，玩家退出或插件卸载时会清理，不应当作持久化的每日限制。
