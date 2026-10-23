# ⚔️ 战斗与伤害

本页列出相关触发器及其事件数据。`PLAYER` 始终指获得任务进度的玩家；`SOURCE` 和 `TARGET` 的身份随事件变化，不能一律当作玩家和怪物。

## 配置位置

下面是单目标片段，替换[普通任务教程](../tasks.md)里的 `source`。多目标任务中则放在对应的 `objectives.<ID>.source`，模板中直接写 `trigger` 和 `amount`。

```yaml
source:
  trigger: attack
  amount: 10
```

示例表示累计 10 次符合条件的事件。部分类型触发很频繁，请结合下文选择条件和[防刷](../anti-abuse.md)。默认使用 `count: events`；各类型可选的数量计数方式见下表和[计数方式](../counting.md)。

## `attack`

`events` 每次攻击伤害事件加 1；`damage` 累计事件的最终伤害值，已计入护甲等伤害修正。这是事件数值，不等于实际扣除的生命值，也不会按目标剩余生命截断。

| 可检查的数据 | 此事件提供的内容 |
| --- | --- |
| 支持的 `count` | `events`、`damage` |
| 底层事件 | `EntityDamageByEntityEvent` |
| 平台要求 | Paper 和 Spigot |
| `PLAYER`：任务玩家 | 此事件关联的玩家 |
| `SOURCE`：来源实体 | 任务玩家 |
| `TARGET`：目标实体 | 受伤/尝试攻击目标 |
| LOCATION | 目标/实体位置 |
| TRIGGER_ITEM / slot | 近战主手、三叉戟物品或跟踪发射武器/槽 |
| 附加数据 | `original_damage`, `damage_cause` |

## `melee_attack`

`events` 每次近战伤害事件加 1；`damage` 累计事件的最终伤害值，计入伤害修正。

| 可检查的数据 | 此事件提供的内容 |
| --- | --- |
| 支持的 `count` | `events`、`damage` |
| 底层事件 | `EntityDamageByEntityEvent` |
| 平台要求 | Paper 和 Spigot |
| `PLAYER`：任务玩家 | 此事件关联的玩家 |
| `SOURCE`：来源实体 | 任务玩家 |
| `TARGET`：目标实体 | 受伤/尝试攻击目标 |
| LOCATION | 目标/实体位置 |
| TRIGGER_ITEM / slot | 主手 / HAND |
| 附加数据 | `original_damage`, `attack_cooldown`, `damage_cause` |

## `damage`

`events` 每次受伤事件加 1；`damage` 累计玩家所受事件的最终伤害值，计入伤害修正。

| 可检查的数据 | 此事件提供的内容 |
| --- | --- |
| 支持的 `count` | `events`、`damage` |
| 底层事件 | `EntityDamageEvent` |
| 平台要求 | Paper 和 Spigot |
| `PLAYER`：任务玩家 | 此事件关联的玩家 |
| `SOURCE`：来源实体 | 已解析攻击者，环境伤害可未提供 |
| `TARGET`：目标实体 | 任务玩家 |
| LOCATION | 玩家位置 |
| TRIGGER_ITEM / slot | 未提供 |
| 附加数据 | `original_damage`, `damage_cause`, `damage_by_entity`, `damage_by_block` |

## `damage_by_entity`

`events` 每次受到实体伤害加 1；`damage` 累计该事件的最终伤害值，计入伤害修正。

| 可检查的数据 | 此事件提供的内容 |
| --- | --- |
| 支持的 `count` | `events`、`damage` |
| 底层事件 | `EntityDamageByEntityEvent` |
| 平台要求 | Paper 和 Spigot |
| `PLAYER`：任务玩家 | 此事件关联的玩家 |
| `SOURCE`：来源实体 | 已解析攻击者或直接伤害者 |
| `TARGET`：目标实体 | 任务玩家 |
| LOCATION | 玩家位置 |
| TRIGGER_ITEM / slot | 未提供 |
| 附加数据 | `original_damage`, `damage_cause`, `damage_by_entity`, `damage_by_block` |

## `kill`

`events` 每次归属于玩家的击杀加 1；`items` 累计死亡事件提供的掉落物数量，不是背包实际拾取量。可用 `match_item` + `item: COUNTED_ITEM` 只累计指定掉落物。

| 可检查的数据 | 此事件提供的内容 |
| --- | --- |
| 支持的 `count` | `events`、`items` |
| 底层事件 | `EntityDeathEvent` |
| 平台要求 | Paper 和 Spigot |
| `PLAYER`：任务玩家 | 此事件关联的玩家 |
| `SOURCE`：来源实体 | 任务玩家 |
| `TARGET`：目标实体 | 死亡实体 |
| LOCATION | 目标/实体位置 |
| TRIGGER_ITEM / slot | 未提供 |
| 附加数据 | `original_experience` |

## `death`

统计任务玩家自己的死亡。它与 `kill` 的方向相反：TARGET 是死亡玩家，SOURCE 是存在时的击杀者。

| 可检查的数据 | 此事件提供的内容 |
| --- | --- |
| 支持的 `count` | `events` |
| 底层事件 | `EntityDeathEvent` |
| 平台要求 | Paper 和 Spigot |
| `PLAYER`：任务玩家 | 此事件关联的玩家 |
| `SOURCE`：来源实体 | 存在时的玩家击杀者 |
| `TARGET`：目标实体 | 任务玩家 |
| LOCATION | 玩家位置 |
| TRIGGER_ITEM / slot | 未提供 |
| 附加数据 | `original_experience` |

## `regain`

`events` 每次恢复生命事件加 1；`healing` 累计正数恢复量，上限为事件发生时玩家距离最大生命值的差额。例如缺 2 点生命、事件恢复 10 点，只计 2 点。

| 可检查的数据 | 此事件提供的内容 |
| --- | --- |
| 支持的 `count` | `events`、`healing` |
| 底层事件 | `EntityRegainHealthEvent` |
| 平台要求 | Paper 和 Spigot |
| `PLAYER`：任务玩家 | 此事件关联的玩家 |
| `SOURCE`：来源实体 | 任务玩家 |
| `TARGET`：目标实体 | 未提供 |
| LOCATION | 玩家位置 |
| TRIGGER_ITEM / slot | 未提供 |
| 附加数据 | `original_amount` |

## `shield_block`

`events` 每次成功格挡加 1；`blocked_damage` 累计该事件被盾牌挡下的伤害量。

| 可检查的数据 | 此事件提供的内容 |
| --- | --- |
| 支持的 `count` | `events`、`blocked_damage` |
| 底层事件 | `EntityDamageByEntityEvent` |
| 平台要求 | Paper 和 Spigot |
| `PLAYER`：任务玩家 | 此事件关联的玩家 |
| `SOURCE`：来源实体 | 已解析攻击者或直接伤害者 |
| `TARGET`：目标实体 | 任务玩家 |
| LOCATION | 玩家位置 |
| TRIGGER_ITEM / slot | 未提供 |
| 附加数据 | `original_damage`, `damage_cause`, `damage_by_entity` |

## `shield_disable`

统计玩家的盾牌被禁用的事件，需要对应 Paper 事件。它不是“成功格挡”的替代触发器。

| 可检查的数据 | 此事件提供的内容 |
| --- | --- |
| 支持的 `count` | `events` |
| 底层事件 | `PlayerShieldDisableEvent` |
| 平台要求 | 需 Paper 模式及对应事件类 |
| `PLAYER`：任务玩家 | 此事件关联的玩家 |
| `SOURCE`：来源实体 | 事件伤害者 |
| `TARGET`：目标实体 | 任务玩家 |
| LOCATION | 玩家位置 |
| TRIGGER_ITEM / slot | 未提供 |

## `attempt_smash_attack`

统计玩家尝试重击且事件结果没有拒绝该尝试的事件。需要对应 Paper 事件，计数不等于伤害数值。

| 可检查的数据 | 此事件提供的内容 |
| --- | --- |
| 支持的 `count` | `events` |
| 底层事件 | `EntityAttemptSmashAttackEvent` |
| 平台要求 | 需 Paper 模式及对应事件类 |
| `PLAYER`：任务玩家 | 此事件关联的玩家 |
| `SOURCE`：来源实体 | 任务玩家 |
| `TARGET`：目标实体 | 受伤/尝试攻击目标 |
| LOCATION | 目标/实体位置 |
| TRIGGER_ITEM / slot | 事件武器 / HAND |

## `knockback`

统计玩家被击退的事件。事件提供推挤实体时 SOURCE 为该实体，否则为玩家；不要默认 SOURCE 总是攻击者。

| 可检查的数据 | 此事件提供的内容 |
| --- | --- |
| 支持的 `count` | `events` |
| 底层事件 | `EntityKnockbackEvent` |
| 平台要求 | 需 Paper 模式及对应事件类 |
| `PLAYER`：任务玩家 | 此事件关联的玩家 |
| `SOURCE`：来源实体 | 事件提供的推挤实体，否则为玩家 |
| `TARGET`：目标实体 | 任务玩家 |
| LOCATION | 玩家位置 |
| TRIGGER_ITEM / slot | 未提供 |

## `combust`

统计玩家开始燃烧的事件，不会按燃烧持续秒数累加。

| 可检查的数据 | 此事件提供的内容 |
| --- | --- |
| 支持的 `count` | `events` |
| 底层事件 | `EntityCombustEvent` |
| 平台要求 | Paper 和 Spigot |
| `PLAYER`：任务玩家 | 此事件关联的玩家 |
| `SOURCE`：来源实体 | 任务玩家 |
| `TARGET`：目标实体 | 未提供 |
| LOCATION | 玩家位置 |
| TRIGGER_ITEM / slot | 未提供 |
| 附加数据 | `original_duration` |

## `creeper_explode`

在爆炸准备事件能够关联玩家时计数，通常关联苦力怕的目标玩家。不是全世界所有苦力怕爆炸的累计器。

| 可检查的数据 | 此事件提供的内容 |
| --- | --- |
| 支持的 `count` | `events` |
| 底层事件 | `ExplosionPrimeEvent` |
| 平台要求 | Paper 和 Spigot |
| `PLAYER`：任务玩家 | 此事件关联的玩家 |
| `SOURCE`：来源实体 | 爆炸实体 |
| `TARGET`：目标实体 | 任务玩家 |
| LOCATION | 目标/实体位置 |
| TRIGGER_ITEM / slot | 未提供 |
| 附加数据 | `original_radius` |

先确认当前平台支持该触发器，再用新任务测试。下表列出的附加数据供理解事件或开发扩展，不代表都有同名可用条件。

[触发器概览](../triggers.md) · [事件条件](../conditions/README.md)
