# 🎯 目标与特殊实体

本页列出相关触发器及其事件数据。`PLAYER` 始终指获得任务进度的玩家；`SOURCE` 和 `TARGET` 的身份随事件变化，不能一律当作玩家和怪物。

## 配置位置

下面是单目标片段，替换[普通任务教程](../tasks.md)里的 `source`。多目标任务中则放在对应的 `objectives.<ID>.source`，模板中直接写 `trigger` 和 `amount`。

```yaml
source:
  trigger: target
  amount: 10
```

示例表示累计 10 次符合条件的事件。部分类型触发很频繁，请结合下文选择条件和[防刷](../anti-abuse.md)。默认使用 `count: events`；各类型可选的数量计数方式见下表和[计数方式](../counting.md)。

## `target`

实体将玩家设为目标时计数，并记录关联关系。SOURCE 是相应实体，玩家身份应从 PLAYER 读取。

| 可检查的数据 | 此事件提供的内容 |
| --- | --- |
| 支持的 `count` | `events` |
| 底层事件 | `EntityTargetEvent` |
| 平台要求 | Paper 和 Spigot |
| `PLAYER`：任务玩家 | 此事件关联的玩家 |
| `SOURCE`：来源实体 | 选定目标的实体 |
| `TARGET`：目标实体 | 任务玩家 |
| LOCATION | 玩家位置 |
| TRIGGER_ITEM / slot | 未提供 |
| 附加数据 | `target_reason`, `target_count` |

## `untag`

已跟踪实体不再以原先的玩家为目标时计数。它依赖先前跟踪记录，不是任意实体失去目标都会触发。

| 可检查的数据 | 此事件提供的内容 |
| --- | --- |
| 支持的 `count` | `events` |
| 底层事件 | `EntityTargetEvent` |
| 平台要求 | Paper 和 Spigot |
| `PLAYER`：任务玩家 | 此事件关联的玩家 |
| `SOURCE`：来源实体 | 失去跟踪目标的实体 |
| `TARGET`：目标实体 | 任务玩家 |
| LOCATION | 玩家位置 |
| TRIGGER_ITEM / slot | 未提供 |
| 附加数据 | `target_reason` |

## `target_tick`

相关目标进行期间，每 tick 在被观察玩家视线 32 格范围内寻找实体。找到才计数，持续看着同一实体可能连续增加进度。

| 可检查的数据 | 此事件提供的内容 |
| --- | --- |
| 支持的 `count` | `events` |
| 底层事件 | `TriggerRuntime.PlayerTick` |
| 平台要求 | Paper 和 Spigot |
| `PLAYER`：任务玩家 | 此事件关联的玩家 |
| `SOURCE`：来源实体 | 任务玩家 |
| `TARGET`：目标实体 | 视线实体 |
| LOCATION | 目标/实体位置 |
| TRIGGER_ITEM / slot | 未提供 |
| BLOCK | 视线选中实体所在位置的方块 |

## `enderman_attack_player`

统计末影人攻击玩家的判定事件。需要对应 Paper 事件，不能用它替代通用的受伤触发器。

| 可检查的数据 | 此事件提供的内容 |
| --- | --- |
| 支持的 `count` | `events` |
| 底层事件 | `EndermanAttackPlayerEvent` |
| 平台要求 | 需 Paper 模式及对应事件类 |
| `PLAYER`：任务玩家 | 此事件关联的玩家 |
| `SOURCE`：来源实体 | 末影人 |
| `TARGET`：目标实体 | 任务玩家 |
| LOCATION | 玩家位置 |
| TRIGGER_ITEM / slot | 未提供 |

## `phantom_pre_spawn`

统计未被中止、且能关联生成玩家的幻翼预生成事件。预生成事件不保证最终一定有幻翼留在世界中。

| 可检查的数据 | 此事件提供的内容 |
| --- | --- |
| 支持的 `count` | `events` |
| 底层事件 | `PhantomPreSpawnEvent` |
| 平台要求 | 需 Paper 模式及对应事件类 |
| `PLAYER`：任务玩家 | 此事件关联的玩家 |
| `SOURCE`：来源实体 | 任务玩家 |
| `TARGET`：目标实体 | 未提供 |
| LOCATION | 拟生成位置 |
| TRIGGER_ITEM / slot | 未提供 |
| BLOCK | 预定生成位置所在方块 |

## `warden_anger_change`

统计以玩家为目标的监守者愤怒值变化。进度按事件次数增加，不按愤怒值差额增加。

| 可检查的数据 | 此事件提供的内容 |
| --- | --- |
| 支持的 `count` | `events` |
| 底层事件 | `WardenAngerChangeEvent` |
| 平台要求 | 需 Paper 模式及对应事件类 |
| `PLAYER`：任务玩家 | 此事件关联的玩家 |
| `SOURCE`：来源实体 | 任务玩家 |
| `TARGET`：目标实体 | 监守者 |
| LOCATION | 目标/实体位置 |
| TRIGGER_ITEM / slot | 未提供 |

先确认当前平台支持该触发器，再用新任务测试。下表列出的附加数据供理解事件或开发扩展，不代表都有同名可用条件。

[触发器概览](../triggers.md) · [事件条件](../conditions/README.md)
