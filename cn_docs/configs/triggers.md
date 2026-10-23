# ⚡ 任务触发器

触发器决定“什么时候尝试增加进度”。例如，`block_break` 是破坏方块，`kill` 是击杀，`consume` 是吃喝消耗物品。条件再决定“这一次算不算”。

## 目标配置示例

下面是目标片段，放在[普通任务](tasks.md)根节点；不是独立文件的完整内容。

```yaml
source:
  name: '挖掘石头'
  trigger: block_break
  amount: 16
  conditions:
    stone:
      type: block_type
      types: [STONE]
```

`trigger: block_break` 选择挖方块事件，`amount: 16` 要求 16 次，`conditions` 把范围缩小为石头。只设置触发器而不设置材质条件，会统计该事件支持的其他方块。

触发器 ID 使用参考页中的小写写法，也可以带命名空间，例如 `ultimatequest:block_break`。

## 进度计数规则

默认 `count: events`，每次符合条件的事件增加 1。部分触发器还支持物品数、方块数、伤害量、经验点数或移动距离等单位。各参考条目均列出支持的 `count`；完整规则和示例见[计数方式](counting.md)。未列出的组合会在加载时报告错误。

被取消的事件不会通过普通事件路由计数。普通任务必须先接取；卷轴必须成功激活并携带。排查不计数时，还要检查条件、活动时间、[防刷](anti-abuse.md)和[任务名额](task-limits.md)。

## 事件实体与可用数据

`PLAYER` 是获得进度的任务玩家。`SOURCE` 和 `TARGET` 根据事件不同而变化：击杀时 TARGET 通常是死亡生物，受伤时 TARGET 则是受伤玩家。有的事件没有目标、物品或方块，不能用缺失的数据筛选。

每个参考条目会列出可用数据和平台要求。依赖 Paper 或特定事件类的触发器，在不支持的平台上不能使用；不要只根据 ID 名称猜测支持情况。

## 触发器索引
* [Tick 与生命周期](triggers/lifecycle.md)：`tick`、`spawn`、`respawn`。
* [战斗与伤害](triggers/combat.md)：`attack`、`melee_attack`、`damage`、`damage_by_entity`、`kill`、`death`、`regain`、`shield_block`、`shield_disable`、`attempt_smash_attack`、`knockback`、`combust`、`creeper_explode`。
* [弹射物与远程](triggers/projectiles.md)：`projectile_launch`、`projectile_hit`、`projectile_tick`、`shoot`、`shoot_bow`、`load_crossbow`。
* [方块与交互](triggers/blocks-interactions.md)：`block_break`、`block_damage`、`block_drop_item`、`block_place`、`block_break_progress_update`、`interact`、`inside_block`、`vibration_receive`。
* [物品与装备](triggers/items-equipment.md)：`consume`、`enchant_item`、`item_damage`、`item_held`、`swap_hand`、`item_group_cooldown`、`name_entity`。
* [移动与输入](triggers/movement-input.md)：`move`、`input`、`toggle_sneak`、`toggle_flight`、`riptide`、`jump`、`elytra_boost`、`lunge`。
* [玩家状态](triggers/player-state.md)：`air_change`、`food_level_change`、`exp_change`、`potion_effect`、`effect_tick`、`exhaustion`、`pickup_experience`。
* [钓鱼与交易](triggers/fishing-trading.md)：`fish`、`piglin_barter`、`trade`、`purchase`。
* [目标与特殊实体](triggers/targeting-special.md)：`target`、`untag`、`target_tick`、`enderman_attack_player`、`phantom_pre_spawn`、`warden_anger_change`。

筛选方法见[事件条件](conditions/README.md)，复用方法见[目标模板](objectives.md)。
