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
| 平台要求 | Paper 和 Spigot |
| `PLAYER`：任务玩家 | 此事件关联的玩家 |
| `SOURCE`：来源实体 | 选定目标的实体 |
| `TARGET`：目标实体 | 任务玩家 |
| LOCATION | 玩家位置 |
| TRIGGER_ITEM / slot | 未提供 |

## `untag`

已跟踪实体不再以原先的玩家为目标时计数。它依赖先前跟踪记录，不是任意实体失去目标都会触发。

| 可检查的数据 | 此事件提供的内容 |
| --- | --- |
| 支持的 `count` | `events` |
| 平台要求 | Paper 和 Spigot |
| `PLAYER`：任务玩家 | 此事件关联的玩家 |
| `SOURCE`：来源实体 | 失去跟踪目标的实体 |
| `TARGET`：目标实体 | 任务玩家 |
| LOCATION | 玩家位置 |
| TRIGGER_ITEM / slot | 未提供 |

## `target_tick`

相关目标进行期间，每 tick 在被观察玩家视线 32 格范围内寻找实体。找到才计数，持续看着同一实体可能连续增加进度。

| 可检查的数据 | 此事件提供的内容 |
| --- | --- |
| 支持的 `count` | `events` |
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
| 平台要求 | 需 Paper 模式及对应事件类 |
| `PLAYER`：任务玩家 | 此事件关联的玩家 |
| `SOURCE`：来源实体 | 任务玩家 |
| `TARGET`：目标实体 | 监守者 |
| LOCATION | 目标/实体位置 |
| TRIGGER_ITEM / slot | 未提供 |


## `shear`

支持 `count: events`。玩家剪切实体时触发，每次匹配事件计数 1。使用目标实体匹配可区分羊 `SHEEP`、蘑菇牛 `MOOSHROOM` 等。`PLAYER` / `SOURCE` 为玩家，`TARGET` 为被剪切实体，`LOCATION` 为实体位置，触发物品和槽位为实际使用的剪刀和手。剪断栓绳使用独立的 `cut_leash` 触发器。

## `cut_leash`

支持 `count: events`。玩家使用剪刀解除栓绳时触发，每个被解除栓绳的实体计数 1。`PLAYER` / `SOURCE` 为玩家，`TARGET` 为被解除栓绳的实体，`LOCATION` 为实体位置，触发物品和槽位为实际使用的剪刀和手。空手解绳不触发。需要服务器版本支持剪断栓绳并发送对应的玩家解绳事件。两个触发器均支持 Paper 和 Spigot，被取消的事件不计数。

## `breed`

支持 `count: events`。玩家促成实体繁殖时触发，每次匹配事件计数 1。`PLAYER` / `SOURCE` 为事件记录的繁殖玩家，`TARGET` 为新生实体，`LOCATION` 为新生实体的位置。通过目标实体匹配可限制动物种类。

触发物品为事件记录的繁殖物品（可能缺失）；事件没有提供原始喂食槽位，因此槽位留空。没有玩家繁殖者的事件及被取消的事件不计数。支持 Paper 和 Spigot；动物及繁殖行为的覆盖范围取决于服务器是否发送 `EntityBreedEvent`。

## `tame`

支持 `count: events`。玩家成功驯服生物时触发一次。`PLAYER` / `SOURCE` 是驯服玩家，`TARGET` 是被驯服生物，`LOCATION` 是生物位置。可使用目标实体匹配限制生物种类。未成功的驯服尝试、非玩家主人和取消的事件不计数。事件未提供准确的驯服物品和手，因此不填充触发物品及槽位。支持 Paper 和 Spigot。

[触发器概览](../triggers.md) · [事件条件](../conditions/README.md)
