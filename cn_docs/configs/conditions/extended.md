# 扩展事件条件

以下条件沿用 ER 中适用条件的 ID 与字段，放在目标的 `source.conditions` 或可复用目标的 `conditions` 中。公共 `not` 和结果消息字段继续有效。UQ 实体选择器为 `PLAYER`、`SOURCE`、`TARGET`；事件数值来自当前观测事件，不读取 ER 修改器结果。`skill_level`、`state_value`、`attribute_value` 依赖 ER 专用系统，不在 UQ 注册。

数值条件支持 `value`、`compare`（`>`、`>=`、`<`、`<=`、`=`、`!=` 及 ER 别名），以及包含边界的 `min` / `max`。`value` 默认取当前数值，`compare` 默认为 `>=`。缺少事件或实体数据时先判为不满足，再处理公共反转。`damage_value` 检查护甲修正前的事件伤害；`count: damage` 累计最终伤害。

冷却和首次攻击条件按任务配置隔离状态；实体卸载、玩家退出、重载和停服会清理缓存，不写入存档。首次攻击条件在同一事件内重复检查结果相同，任一相关实体卸载后重置。`stationary` 按条件类型跟踪实体位置；`perfect_guard` 监听举盾行为。`in_rain`、历史生成原因回退及 Paper 专属事件条件需要对应服务端 API；缺失时判为不满足。`headshot` 从伤害或弹射物命中事件读取弹射物；`fatal_damage` 检查真正受到伤害的实体。


## `item_damage`

检查选中可损耗物品的当前耐久损耗值或预计耐久损耗值。

支持本页开头说明的通用数值比较字段。

| 字段 | 默认值 | 说明 |
| --- | --- | --- |
| `item` | `TRIGGER_ITEM` | 物品选择器字符串，或包含 `selector` 和 `holder` 的嵌套部分。 |
| `item-holder` | `PLAYER` | 使用扁平物品配置形式时的持有者选择器。 |
| `mode` | `DAMAGE` | 根据所选物品耐久状态计算出的数值。参见下方模式表。 |
| `include-event-damage` | `false` | 在 `PlayerItemDamageEvent` 中，是否计入当前事件即将应用的事件耐久损耗。 |
| `min / max` | 无限制 | 包含边界值的数值范围。 |
| `compare / value` | 未设置 | 另一种数值比较方式，例如 `>=`、`<` 或 `=`。 |

## `min_attack_cooldown`

检查玩家当前的攻击冷却强度。

| 字段 | 默认值 | 说明 |
| --- | --- | --- |
| `target` | `TARGET` | 玩家选择器。 |
| `value` | `0` | 包含边界的最小攻击冷却强度。 |

## `blocking`

检查选中的生物实体是否正在使用盾牌格挡。

| 字段 | 默认值 | 说明 |
| --- | --- | --- |
| `target` | `TARGET` | 生物实体选择器。 |
| `value` | `true` | 期望的格挡状态。 |

## `perfect_guard`

检查玩家是否在举盾后的短暂时间窗内完成格挡。

| 字段 | 默认值 | 说明 |
| --- | --- | --- |
| `target` | `TARGET` | 玩家选择器。 |
| `window-millis` | `120` | 从举盾开始计算的最大毫秒数，支持占位符和表达式。 |
| `consume` | `true` | 匹配成功后移除本次举盾记录，使每次举盾最多触发一次完美格挡。 |

## `falling`

检查滞空实体是否正在向下移动。

| 字段 | 默认值 | 说明 |
| --- | --- | --- |
| `target` | `TARGET` | 实体选择器。 |
| `maximum-y` | `0` | 实体的 Y 轴速度必须低于此值。 |
| `value` | `true` | 期望的下落状态。 |

## `fall_distance`

比较实体累计的 Bukkit 坠落距离。

支持本页开头说明的通用数值比较字段。

| 字段 | 默认值 | 说明 |
| --- | --- | --- |
| `target` | `TARGET` | 实体选择器。 |
| `value` / `compare` | 未设置 / `>=` | 可选的直接数值比较。 |
| `min` | 无限制 | 坠落距离的包含性下限。 |
| `max` | 无限制 | 坠落距离的包含性上限。 |

## `sprinting`

检查选中的玩家是否正在疾跑。

| 字段 | 默认值 | 说明 |
| --- | --- | --- |
| `target` | `TARGET` | 玩家选择器。 |
| `value` | `true` | 期望的疾跑状态。 |

## `swimming`

检查选中的玩家是否正在游泳。

| 字段 | 默认值 | 说明 |
| --- | --- | --- |
| `target` | `TARGET` | 玩家选择器。 |
| `value` | `true` | 期望的游泳状态。 |

## `flying`

检查选中的玩家是否正在飞行。

| 字段 | 默认值 | 说明 |
| --- | --- | --- |
| `target` | `TARGET` | 玩家选择器。 |
| `value` | `true` | 期望的飞行状态。 |

## `climbing`

检查 climbing。

字段：`target`, `value`.

## `riding`

检查 riding。

字段：`target`, `value`.

## `potion_effect_type`

匹配效果事件提供的药水效果类型。

| 字段 | 默认值 | 说明 |
| --- | --- | --- |
| `selector / selectors` | 空 | 精确效果选择器。 |
| `effect / effects` | 空 | 效果选择器的别名字段。 |
| `type / types` | 空 | 其他选择器别名字段。 |
| `category / categories` | 空 | `PotionEffectSelector` 支持的效果分类。 |

## `effect_type`

别名，规则同 [`potion_effect_type`](#potion_effect_type)。

## `potion_effect_cause`

匹配 Bukkit 药水效果变更事件的原因。

| 字段 | 默认值 | 说明 |
| --- | --- | --- |
| `cause / causes` | 空 | 接受的 `EntityPotionEffectEvent.Cause` 名称，例如 `FOOD`、`POTION_DRINK` 或 `ATTACK`。 |

## `potion_effect_action`

匹配 Bukkit 药水效果变更事件的操作。

| 字段 | 默认值 | 说明 |
| --- | --- | --- |
| `action / actions` | 空 | 接受的操作，例如 `ADDED`、`CHANGED`、`REMOVED` 或 `CLEARED`。 |

## `in_sunlight`

按照简化的白天时间和天空光照暴露规则进行检查。

| 字段 | 默认值 | 说明 |
| --- | --- | --- |
| `target` | `TARGET` | 实体选择器。 |
| `value` | `true` | 期望的状态。 |

## `thunder`

检查上下文所在世界是否正在打雷。

| 字段 | 默认值 | 说明 |
| --- | --- | --- |
| `value` | `true` | 期望的雷暴状态。 |

## `clear_weather`

检查上下文所在世界是否没有暴风雨。

| 字段 | 默认值 | 说明 |
| --- | --- | --- |
| `value` | `true` | 期望的晴朗天气状态。 |

## `in_rain`

检查 Bukkit 的 `Entity#isInRain` 状态。

| 字段 | 默认值 | 说明 |
| --- | --- | --- |
| `target` | `TARGET` | 实体选择器。 |
| `value` | `true` | 期望的状态。 |

## `interaction_action`

匹配 `PlayerInteractEvent.Action`。

| 字段 | 默认值 | 说明 |
| --- | --- | --- |
| `action` | 空 | 单个操作枚举名称。 |
| `actions` | 空 | 接受的操作列表。 |

## `input_type`

匹配 Paper 玩家输入中当前激活、刚刚按下或刚刚松开的控制按键。

| 字段 | 默认值 | 说明 |
| --- | --- | --- |
| `input` | 空 | 单个输入：`FORWARD`、`BACKWARD`、`LEFT`、`RIGHT`、`JUMP`、`SNEAK` 或 `SPRINT`。 |
| `inputs` | 空 | 接受的输入列表。 |
| `state` | `ACTIVE` | 可填写 `ACTIVE`、`PRESSED` 或 `RELEASED`。 |

## `event_state`

匹配 `PlayerFishEvent.State`。

| 字段 | 默认值 | 说明 |
| --- | --- | --- |
| `state` | 空 | 单个钓鱼状态。 |
| `states` | 空 | 接受的钓鱼状态列表。 |

## `smash_attack_lands`

检查 Paper 重锤猛击尝试是否会成功命中。

| 字段 | 默认值 | 说明 |
| --- | --- | --- |
| `value` | `true` | 期望的猛击成功状态。 |

## `block_type_offset`

匹配相对于上下文方块存在一定偏移的方块。

| 字段 | 默认值 | 说明 |
| --- | --- | --- |
| `type-name` | 空 | 单个方块规范。 |
| `types` | 空 | 接受的方块规范。 |
| `offset.x / offset.y / offset.z` | `0` | 相对方块坐标。 |

## `in_structure`

检查某个位置是否位于配置的任意已生成结构边界框内。

| 字段 | 默认值 | 说明 |
| --- | --- | --- |
| `target` | `TARGET` | 实体选择器。 |
| `structure` | 空 | 单个结构命名空间键。 |
| `structures` | 空 | 接受的结构键列表。 |

## `best_tool`

检查上下文方块和玩家手持物品对应的 Bukkit 首选工具结果。

| 字段 | 默认值 | 说明 |
| --- | --- | --- |
| `target` | `SOURCE` | 玩家选择器。 |
| `hand` | `MAIN_HAND` | 可填写 `MAIN_HAND` 或 `OFF_HAND`。 |
| `value` | `true` | 期望的首选工具结果。 |

## `preferred_tool`

别名，规则同 [`best_tool`](#best_tool)。

## `biome_changed`

检查玩家移动事件是否跨越了生物群系边界。

_没有此类型专用的字段。_

## `liquid_transition`

检查玩家移动时是否刚进入或离开指定液体。

| 字段 | 默认值 | 说明 |
| --- | --- | --- |
| `liquid` | `ANY` | 接受 `ANY`、`WATER` 或 `LAVA`。 |
| `transition` | `ENTER` | 接受 `ENTER`、`EXIT`；`LEAVE` 是 `EXIT` 的别名。 |

## `block_break_time`

计算玩家破坏上下文方块所需的 Tick 数或时间。

支持本页开头说明的通用数值比较字段。

| 字段 | 默认值 | 说明 |
| --- | --- | --- |
| `target` | `SOURCE` | 玩家选择器。 |
| `unit` | `TICKS` | 可填写 `TICKS`、`SECONDS` 或 `MILLISECONDS`。 |
| `min / max` | 无限制 | 包含边界值的时间范围。 |
| `compare / value` | 未设置 | 另一种数值比较方式。 |

## `break_time`

别名，规则同 [`block_break_time`](#block_break_time)。

## `ageable`

检查上下文方块的可生长数据是否已经完全成熟。

| 字段 | 默认值 | 说明 |
| --- | --- | --- |
| `value` | `true` | 期望的完全成熟状态。 |

## `height`

检查解析出的触发位置所在方块的 Y 坐标。

| 字段 | 默认值 | 说明 |
| --- | --- | --- |
| `min` | 最小整数 | 包含边界的最小 Y 值。 |
| `max` | 最大整数 | 包含边界的最大 Y 值。 |

## `damage_cause`

匹配已记录的 Bukkit `EntityDamageEvent.DamageCause`。

| 字段 | 默认值 | 说明 |
| --- | --- | --- |
| `value` | 空 | 单个伤害原因枚举名称。 |
| `values` | 空 | 接受的伤害原因名称列表。 |

## `health_percent`

检查当前生命值占最大生命值的百分比。

| 字段 | 默认值 | 说明 |
| --- | --- | --- |
| `target` | `SOURCE` | 实体选择器。 |
| `min` | `0` | 包含边界的最小百分比。 |
| `max` | `100` | 包含边界的最大百分比。 |
| `min-max-health` | `0` | 最大生命值本身允许的包含边界最小值。 |
| `max-max-health` | 无限制 | 最大生命值本身允许的包含边界最大值。 |

## `random`

进行一次新的概率判定。

| 字段 | 默认值 | 说明 |
| --- | --- | --- |
| `value` | `1` | 取值范围为 `0` 到 `1` 的概率；超出范围的值会被限制在该范围内。 |

## `first_attack_from_monster`

对于当前条件配置路径，只匹配防守者第一次受到某个特定怪物的已追踪攻击。

_没有此类型专用的字段。_

## `first_monster_attack`

别名，规则同 [`first_attack_from_monster`](#first_attack_from_monster)。

## `target_category`

匹配 `TARGET` 的固定分类。

| 字段 | 默认值 | 说明 |
| --- | --- | --- |
| `category` | `ANIMAL` | 支持 `MONSTER`、`ANIMAL` 或 `UNDEAD`。 |

## `food_change`

检查即将应用的饥饿值变化量。

支持本页开头说明的通用数值比较字段。

| 字段 | 默认值 | 说明 |
| --- | --- | --- |
| `min / max` | 无限制 | 包含边界值的变化量范围；正数表示增加饥饿值，负数表示减少饥饿值。 |
| `compare / value` | 未设置 | 另一种数值比较方式。 |

## `air_change`

检查即将应用的剩余空气值变化量。

支持本页开头说明的通用数值比较字段。

| 字段 | 默认值 | 说明 |
| --- | --- | --- |
| `min / max` | 无限制 | 包含边界值的变化量范围；正数表示增加空气值，负数表示减少空气值。 |
| `compare / value` | 未设置 | 另一种数值比较方式。 |

## `liquid_surface_distance`

检查选中实体眼睛到正上方液面的垂直距离。

支持本页开头说明的通用数值比较字段。

| 字段 | 默认值 | 说明 |
| --- | --- | --- |
| `target` | `PLAYER` | 实体选择器。生物使用眼睛位置，其他实体使用自身位置。 |
| `liquid` | `ANY` | 接受 `ANY`、`WATER` 或 `LAVA`。 |
| `min / max` | 无限制 | 到液面的垂直距离范围，单位为格。 |
| `compare / value` | 未设置 | 另一种数值比较方式。 |
| `scan-limit` | 世界高度上限 | 向上扫描的最大格数，可使用数学表达式与变量。 |

## `headshot`

检查伤害或弹射物命中事件中的弹射物与生物目标眼部的高度差。

| 字段 | 默认值 | 说明 |
| --- | --- | --- |
| `tolerance` | `0.45` | 投射物位置与目标眼睛位置之间允许的最大 Y 轴绝对差值。 |

## `fatal_damage`

检查事件最终伤害是否达到受伤实体的剩余生命值。

_没有此类型专用的字段。_

## `distance`

检查两个上下文实体之间的欧几里得距离。

支持本页开头说明的通用数值比较字段。

| 字段 | 默认值 | 说明 |
| --- | --- | --- |
| `source` | `SOURCE` | 第一个实体选择器。 |
| `target` | `TARGET` | 第二个实体选择器。 |
| `min / max` | 无限制 | 包含边界值的距离范围。 |
| `compare / value` | 未设置 | 另一种数值比较方式。 |

## `bounding_box_distance`

检查一个实体的位置到另一个实体碰撞箱的最短距离。

支持本页开头说明的通用数值比较字段。

| 字段 | 默认值 | 说明 |
| --- | --- | --- |
| `source` | `SOURCE` | 作为测量起点的实体。 |
| `target` | `TARGET` | 被测量碰撞箱的实体。 |
| `source-eye` | `false` | 使用来源生物的眼睛位置，而不是脚部位置。 |
| `min / max` | 无限制 | 包含边界值的最短距离范围。 |
| `compare / value` | 未设置 | 另一种数值比较方式。 |

## `environment`

匹配世界维度，并可选择同时匹配生物群系。

| 字段 | 默认值 | 说明 |
| --- | --- | --- |
| `target` | `SOURCE` | 实体选择器。 |
| `dimension` | 空 | Bukkit 世界环境，例如 `NORMAL`、`NETHER` 或 `THE_END`。 |
| `biomes` | 空 | 接受的生物群系枚举名称或命名空间键。 |

## `gliding`

检查生物实体的滑翔状态。

| 字段 | 默认值 | 说明 |
| --- | --- | --- |
| `target` | `TARGET` | 实体选择器。 |
| `value` | `true` | 期望的状态。 |

## `light_level`

检查方块的总光照等级。

| 字段 | 默认值 | 说明 |
| --- | --- | --- |
| `min` | `0` | 包含边界的最小光照等级。 |
| `max` | `15` | 包含边界的最大光照等级。 |

## `stationary`

检查选中的实体是否在指定时间内始终保持在很小的移动容差范围内。

| 字段 | 默认值 | 说明 |
| --- | --- | --- |
| `target` | `TARGET` | 实体选择器。 |
| `seconds` | `3` | 必须保持静止的时间，单位为秒。 |

## `health`

使用共用数值比较语法检查当前生命值或最大生命值。

支持本页开头说明的通用数值比较字段。

| 字段 | 默认值 | 说明 |
| --- | --- | --- |
| `target` | `PLAYER` | 实体选择器。 |
| `mode` | `CURRENT` | 可填写 `CURRENT`、`MAX` 或 `MAXIMUM`。 |
| `min / max` | 无限制 | 包含边界值的数值范围。 |
| `compare / value` | 未设置 | 另一种比较方式，例如 `>=`、`<` 或 `=`。 |

## `food_level`

检查玩家当前的饥饿值。

支持本页开头说明的通用数值比较字段。

| 字段 | 默认值 | 说明 |
| --- | --- | --- |
| `target` | `PLAYER` | 实体选择器。 |
| `min / max` | 无限制 | 包含边界值的范围。 |
| `compare / value` | 未设置 | 另一种数值比较方式。 |

## `damage_value`

检查当前事件的原始伤害，不包含护甲等最终伤害修正。

支持本页开头说明的通用数值比较字段。

| 字段 | 默认值 | 说明 |
| --- | --- | --- |
| `min / max` | 无限制 | 包含边界值的数值范围。 |
| `compare / value` | 未设置 | 另一种数值比较方式。 |

## `first_attack_against_entity`

对于当前条件配置路径，只匹配符合要求的来源玩家与目标实体组合的第一次攻击。

_没有此类型专用的字段。_

## `combust_duration`

检查当前燃烧持续时间结果。

支持本页开头说明的通用数值比较字段。

| 字段 | 默认值 | 说明 |
| --- | --- | --- |
| `min / max` | 无限制 | 包含边界值的数值范围。 |
| `compare / value` | 未设置 | 另一种数值比较方式。 |

## `regain_amount`

检查当前生命恢复结果。

支持本页开头说明的通用数值比较字段。

| 字段 | 默认值 | 说明 |
| --- | --- | --- |
| `min / max` | 无限制 | 包含边界值的数值范围。 |
| `compare / value` | 未设置 | 另一种数值比较方式。 |

## `explosion_yield`

检查当前爆炸掉落倍率结果。

支持本页开头说明的通用数值比较字段。

| 字段 | 默认值 | 说明 |
| --- | --- | --- |
| `min / max` | 无限制 | 包含边界值的数值范围。 |
| `compare / value` | 未设置 | 另一种数值比较方式。 |

## `explosion_radius`

检查当前爆炸准备阶段的爆炸半径。

支持本页开头说明的通用数值比较字段。

| 字段 | 默认值 | 说明 |
| --- | --- | --- |
| `min / max` | 无限制 | 包含边界值的数值范围。 |
| `compare / value` | 未设置 | 另一种数值比较方式。 |

## `damage_origin`

按照已记录的伤害来源类型对伤害进行分类。

| 字段 | 默认值 | 说明 |
| --- | --- | --- |
| `value` | `ANY` | 可填写 `ANY`、`ENTITY`、`BLOCK` 或 `OTHER`。 |

## `combust_origin`

匹配燃烧是由实体、方块还是其他来源引起。

| 字段 | 默认值 | 说明 |
| --- | --- | --- |
| `origin` | 空 | 单个值：`ENTITY`、`BLOCK` 或 `OTHER`。 |
| `origins` | 空 | 接受的来源类型列表。 |

## `target_reason`

匹配已记录的 Bukkit 实体选择目标原因。

| 字段 | 默认值 | 说明 |
| --- | --- | --- |
| `value` | 空 | 单个原因枚举名称。 |
| `values` | 空 | 接受的原因列表。 |

## `target_count`

检查 target_count。

支持本页开头说明的通用数值比较字段。

## `spawn_reason`

匹配实体的 Bukkit 生成原因。

| 字段 | 默认值 | 说明 |
| --- | --- | --- |
| `target` | `TARGET` | 实体选择器。 |
| `reason` | 空 | 单个 `CreatureSpawnEvent.SpawnReason`。 |
| `reasons` | 空 | 接受的生成原因列表。 |

## `knockback_cause`

匹配 Paper 的 `EntityKnockbackEvent.Cause`。

| 字段 | 默认值 | 说明 |
| --- | --- | --- |
| `cause / causes` | 空 | 接受的原因名称。 |
| `reason / reasons` | 空 | 合并到同一接受集合中的别名字段。 |

## `knockback_reason`

别名，规则同 [`knockback_cause`](#knockback_cause)。

## `exhaustion_reason`

匹配 Bukkit 的 `EntityExhaustionEvent.ExhaustionReason`。

| 字段 | 默认值 | 说明 |
| --- | --- | --- |
| `reason` | 空 | 单个消耗度原因。 |
| `reasons` | 空 | 接受的原因列表。 |

## `villager_trade`

区分真正的村民交易和其他 Paper 购买事件。

| 字段 | 默认值 | 说明 |
| --- | --- | --- |
| `value` | `true` | `true` 要求事件为 `PlayerTradeEvent`；`false` 要求事件为其他购买事件子类型。 |

## `cooldown`

尝试获取仅属于当前条件的运行时冷却，并且只在成功获取冷却时匹配。

| 字段 | 默认值 | 说明 |
| --- | --- | --- |
| `seconds` | `0` | 冷却时间，单位为秒；填写非正数时始终匹配。 |
| `owner` | `SOURCE` | 拥有该冷却的实体选择器。 |
| `per-target` | `false` | 是否为每个 `TARGET` 分别记录冷却。 |

## `cooldown_material`

匹配 Paper `PlayerItemCooldownEvent` 中涉及的物品材质。

| 字段 | 默认值 | 说明 |
| --- | --- | --- |
| `material` | 空 | 单个 Bukkit 材质名称或命名空间键。 |
| `materials` | 空 | 接受的材质列表。 |
