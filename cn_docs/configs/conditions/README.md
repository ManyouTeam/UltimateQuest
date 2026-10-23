# ✅ 事件条件

条件用来缩小统计范围，例如只统计生存模式玩家挖的石头，而不是所有模式下的所有方块。目标中的同级条件默认必须全部满足。

## 条件配置位置与示例

下面是普通任务的目标片段：

```yaml
source:
  trigger: block_break
  amount: 16
  conditions:
    survival:
      type: game_mode
      target: PLAYER
      modes: [SURVIVAL]
    stone:
      type: block_type
      types: [STONE]
```

`survival` 和 `stone` 是自定义条目名，真正的类型由 `type` 指定。示例要求玩家处于生存模式，并且被挖的是石头，两项同时满足才增加进度。

## 接取条件与目标条件

| 位置 | 何时检查 | 可用数据 |
| --- | --- | --- |
| 普通任务根节点 `conditions` | 玩家尝试接取时 | 玩家数据；没有破坏方块、钓获物等事件数据。 |
| 目标 `source.conditions` / 模板根节点 `conditions` | 每次触发器事件发生时 | 由该触发器提供的玩家、实体、方块或物品。 |

检查任务玩家时，使用支持该选项的条件并显式写 `target: PLAYER`。`TARGET` 可能是怪物、弹射物、玩家或根本不存在。`SOURCE` 也不保证是玩家，见各[触发器](../triggers.md)的数据说明。

## 公共字段

| 字段 | 默认 / 要求 | 作用 |
| --- | --- | --- |
| `type` | 必填 | 已注册的条件 ID。 |
| `not` | `false` | 反转该条件结果。 |
| `target` | 随类型变化 | 仅支持实体选择的条件读取它。 |
| `meet-message` | 空 | 满足时发送的可选消息。 |
| `not-meet-message` | 空 | 不满足时发送的可选消息。 |

未知直接类型会导致定义加载失败。权限、生物群系、占位符等通用玩家条件需要放进 `player_conditions`，不能直接写成同级事件条件。

缺少所需数据时通常先返回不满足，再执行 `not`。因此“反转一个缺失目标的判断”可能变成满足，不能用反转证明目标存在。目标条件执行后还要通过防刷检查，才增加进度。


当前注册 87 个事件条件 ID（含别名）。[扩展事件条件](extended.md)列出了移动状态、事件数值、环境筛选和带运行状态的条件。

## 条件类型索引

| 类型 | 参考 |
| --- | --- |
| `any` | [any](logical.md#any) |
| `not` | [not](logical.md#not) |
| `sneaking` | [sneaking](entity-state.md#sneaking) |
| `not_sneaking` | [not_sneaking](entity-state.md#not_sneaking) |
| `on_ground` | [on_ground](entity-state.md#on_ground) |
| `in_air` | [in_air](entity-state.md#in_air) |
| `in_water` | [in_water](entity-state.md#in_water) |
| `in_lava` | [in_lava](entity-state.md#in_lava) |
| `game_mode` | [game_mode](entity-state.md#game_mode) |
| `has_potion` | [has_potion](entity-state.md#has_potion) |
| `match_entity` | [match_entity](entity-state.md#match_entity) |
| `match_item` | [match_item](items.md#match_item) |
| `catch_match_item` | [catch_match_item](items.md#catch_match_item) |
| `world` | [world](environment.md#world) |
| `block_type` | [block_type](environment.md#block_type) |
| `night` | [night](environment.md#night) |
| `storm` | [storm](environment.md#storm) |
| `player_conditions` | [player_conditions](player-conditions.md#player_conditions) |
| `item_damage` | [item_damage](extended.md#item_damage) |
| `min_attack_cooldown` | [min_attack_cooldown](extended.md#min_attack_cooldown) |
| `blocking` | [blocking](extended.md#blocking) |
| `perfect_guard` | [perfect_guard](extended.md#perfect_guard) |
| `falling` | [falling](extended.md#falling) |
| `fall_distance` | [fall_distance](extended.md#fall_distance) |
| `sprinting` | [sprinting](extended.md#sprinting) |
| `swimming` | [swimming](extended.md#swimming) |
| `flying` | [flying](extended.md#flying) |
| `climbing` | [climbing](extended.md#climbing) |
| `riding` | [riding](extended.md#riding) |
| `potion_effect_type` | [potion_effect_type](extended.md#potion_effect_type) |
| `effect_type` | [effect_type](extended.md#effect_type) |
| `potion_effect_cause` | [potion_effect_cause](extended.md#potion_effect_cause) |
| `potion_effect_action` | [potion_effect_action](extended.md#potion_effect_action) |
| `in_sunlight` | [in_sunlight](extended.md#in_sunlight) |
| `thunder` | [thunder](extended.md#thunder) |
| `clear_weather` | [clear_weather](extended.md#clear_weather) |
| `in_rain` | [in_rain](extended.md#in_rain) |
| `interaction_action` | [interaction_action](extended.md#interaction_action) |
| `input_type` | [input_type](extended.md#input_type) |
| `event_state` | [event_state](extended.md#event_state) |
| `smash_attack_lands` | [smash_attack_lands](extended.md#smash_attack_lands) |
| `block_type_offset` | [block_type_offset](extended.md#block_type_offset) |
| `in_structure` | [in_structure](extended.md#in_structure) |
| `best_tool` | [best_tool](extended.md#best_tool) |
| `preferred_tool` | [preferred_tool](extended.md#preferred_tool) |
| `biome_changed` | [biome_changed](extended.md#biome_changed) |
| `liquid_transition` | [liquid_transition](extended.md#liquid_transition) |
| `block_break_time` | [block_break_time](extended.md#block_break_time) |
| `break_time` | [break_time](extended.md#break_time) |
| `ageable` | [ageable](extended.md#ageable) |
| `height` | [height](extended.md#height) |
| `damage_cause` | [damage_cause](extended.md#damage_cause) |
| `health_percent` | [health_percent](extended.md#health_percent) |
| `random` | [random](extended.md#random) |
| `first_attack_from_monster` | [first_attack_from_monster](extended.md#first_attack_from_monster) |
| `first_monster_attack` | [first_monster_attack](extended.md#first_monster_attack) |
| `target_category` | [target_category](extended.md#target_category) |
| `food_change` | [food_change](extended.md#food_change) |
| `air_change` | [air_change](extended.md#air_change) |
| `liquid_surface_distance` | [liquid_surface_distance](extended.md#liquid_surface_distance) |
| `headshot` | [headshot](extended.md#headshot) |
| `fatal_damage` | [fatal_damage](extended.md#fatal_damage) |
| `distance` | [distance](extended.md#distance) |
| `bounding_box_distance` | [bounding_box_distance](extended.md#bounding_box_distance) |
| `environment` | [environment](extended.md#environment) |
| `gliding` | [gliding](extended.md#gliding) |
| `light_level` | [light_level](extended.md#light_level) |
| `stationary` | [stationary](extended.md#stationary) |
| `health` | [health](extended.md#health) |
| `food_level` | [food_level](extended.md#food_level) |
| `damage_value` | [damage_value](extended.md#damage_value) |
| `first_attack_against_entity` | [first_attack_against_entity](extended.md#first_attack_against_entity) |
| `combust_duration` | [combust_duration](extended.md#combust_duration) |
| `regain_amount` | [regain_amount](extended.md#regain_amount) |
| `explosion_yield` | [explosion_yield](extended.md#explosion_yield) |
| `explosion_radius` | [explosion_radius](extended.md#explosion_radius) |
| `damage_origin` | [damage_origin](extended.md#damage_origin) |
| `combust_origin` | [combust_origin](extended.md#combust_origin) |
| `target_reason` | [target_reason](extended.md#target_reason) |
| `target_count` | [target_count](extended.md#target_count) |
| `spawn_reason` | [spawn_reason](extended.md#spawn_reason) |
| `knockback_cause` | [knockback_cause](extended.md#knockback_cause) |
| `knockback_reason` | [knockback_reason](extended.md#knockback_reason) |
| `exhaustion_reason` | [exhaustion_reason](extended.md#exhaustion_reason) |
| `villager_trade` | [villager_trade](extended.md#villager_trade) |
| `cooldown` | [cooldown](extended.md#cooldown) |
| `cooldown_material` | [cooldown_material](extended.md#cooldown_material) |

组合“任意满足”和“取反”见[逻辑条件](logical.md)。修改后请分别测试该通过和不该通过的行为。
