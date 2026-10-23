# 🛡️ 装备与集成规则

本页面介绍 MatchEntityFormat 中用于匹配装备和可选怪物插件集成的规则。

## 本页面的规则

- `equip`
- `mythicmobs`
- `levelled-mobs`

只有在 UltimateQuest 启用时，对应插件和兼容挂钩均可用，集成规则才会被注册。

## `equip`

按装备槽使用嵌套 MatchItemFormat。

```yaml
equip:
  main-hand:
    material:
    - BOW
    - CROSSBOW
  helmet:
    material:
    - IRON_HELMET
```

任一已配置槽满足即通过 equip，同级实体规则仍 AND。槽位为 main-hand、off-hand、helmet、chestplate、leggings、boots，不是所有槽均需满足。

## 匹配空槽位

缺少物品通常会表现为空气或空 `ItemStack`。请使用装备匹配器实际支持的 MatchItemFormat 形式，不要直接假设 `material: AIR` 在所有情况下都表示空槽位。

空槽位条件很重要时，请使用准确的实体类型和服务器版本单独测试。

## 装备示例：远程骷髅

```yaml
match-entity:
  entity-types:
    - SKELETON
    - STRAY
  equip:
    main-hand:
      material:
        - BOW
        - CROSSBOW
```

此规则比 `ranged: true` 更严格，因为它会检查实际主手物品。

## 装备示例：带附魔武器

```yaml
match-entity:
  equip:
    main-hand:
      material-tag:
        - minecraft:swords
      has-enchants:
        - minecraft:sharpness
```

所有 MatchItemFormat 规则都可以嵌套在槽位下，包括逻辑组，以及可选的提供器或 NBT 规则。

## `mythicmobs`

启动注册钩子后匹配 MythicMobs 内部 ID。

```yaml
mythicmobs:
- SkeletonKing
```

用内部 ID 而非显示名，缺少集成时未注册规则键会忽略。

## `levelled-mobs`

目前 `levelled-mobs` 不能可靠地按等级筛选，请勿用它作为任务奖励的等级限制。需要等级要求时，可改用经过验证的 `entity-pdc` 筛选。

## 可选规则注册

MythicMobs 或 LevelledMobs 缺失或不兼容时，对应的 MatchEntityFormat 规则可能不会被注册。

MatchEntityFormat 会忽略未知键。这意味着不可用的可选规则可能导致匹配范围比预期更广。

{% hint style="warning" %}
将 `mythicmobs` 或 `levelled-mobs` 用于安全控制、奖励或破坏性任务前，务必确认启动日志显示所需的集成挂钩已经注册。
{% endhint %}

## 安全集成后备限制

某项任务必须在集成存在时才执行时，应尽可能将集成规则与另一个稳定限制组合使用：

```yaml
match-entity:
  entity-types:
    - WITHER_SKELETON
  mythicmobs:
    - InfernalGuard
```

这不能替代挂钩检查，但在可选键被忽略时，可以防止匹配器变为完全不受限制。

缺少必要的兼容插件时，请先禁用依赖该插件的任务。

## 自定义物品装备

嵌套 MatchItemFormat 可以使用提供器 ID：

```yaml
match-entity:
  equip:
    main-hand:
      items:
        - namespace:boss_sword
```

对应的物品提供器挂钩也必须完成注册。MythicMobs 实体匹配和 MythicMobs 物品匹配属于两个独立集成。

## 示例：选择带装备的 MythicMob

```yaml
match-entity:
  mythicmobs:
    - SkeletonKing
  equip:
    main-hand:
      material:
        - BOW
    helmet:
      has-name: true
```

## 示例：排除穿戴护甲的目标

```yaml
match-entity:
  monster: true
  not:
    equip:
      chestplate:
        material-tag:
          - minecraft:chest_armor
```

请确认该标签确实存在，并验证空装备槽位的行为是否符合预期。

## 常见错误

- 使用显示名称代替 MythicMobs 内部 ID；
- 未检查挂钩注册就依赖可选键；
- 认为未知集成键会以关闭方式失败；
- 配置解析器要求别名时却使用 Bukkit 槽位名称；
- 认为 `ranged: true` 与匹配弓类装备完全相同；
- 忽略装备可能在不同事件之间发生变化；
- 混淆实体提供器集成与物品提供器集成。

另请参阅 [MatchItemFormat](../match-item-format/README.md)、[实体身份与状态规则](identity-state.md)和[逻辑规则](logic.md)。
