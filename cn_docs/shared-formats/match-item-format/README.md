# 🔍 物品匹配格式

使用 `MatchItemFormat` 指定目标需要的物品，例如钓获的鱼、消耗的物品，或玩家使用的工具。

在 `match_item` 或 `catch_match_item` [事件条件](../../configs/conditions/items.md)的 `match` 中使用，也可以用于实体的装备筛选。

```yaml
match-item:
  material-tag:
    - minecraft:swords
  has-enchants:
    - minecraft:sharpness
```

## 检查规则

MatchItemFormat 配置部分不使用 `type` 字段。每个可识别的键都会启用一条规则，同一层级的规则使用逻辑 **AND（与）**。基于列表的规则内部通常使用逻辑 **OR（或）**。

```yaml
match-item:
  material:
    - DIAMOND_SWORD
    - NETHERITE_SWORD
  has-name: true
  contains-name:
    - Legendary
```

该物品必须是列表中的任意一种材质，必须具有自定义名称，并且名称中必须包含 `Legendary`。

## 缺失或无效输入

* 缺少 MatchItemFormat 配置部分时，视为匹配所有物品。
* 缺少物品，或物品没有可用的 `ItemMeta` 时，不会匹配。
* 无法识别的键会被忽略，因为没有已注册的匹配规则负责处理它们。
* `none: true` 会强制该配置部分匹配失败。

## 逻辑组合

使用 `any` 表示多个候选条件：

```yaml
match-item:
  any:
    '1':
      material-tag:
        - minecraft:swords
    '2':
      items:
        - mythic_sword
```

使用 `not` 排除嵌套匹配结果：

```yaml
match-item:
  material-tag:
    - minecraft:damageable
  not:
    contains-lore:
      - Disabled
```

## 内置规则

| 规则键 | 用途 |
| --- | --- |
| `none` | 设置为 true 时强制失败。 |
| `items` | 匹配配置的物品提供器或保存物品 ID。 |
| `material` | 匹配 Bukkit 材质或带命名空间的材质。 |
| `material-tag` | 匹配配置的 Minecraft 或物品标签之一。 |
| `rarity` | 在支持的 1.20.5+ 服务端上匹配物品稀有度。 |
| `has-name` | 要求或排除自定义名称。 |
| `contains-name` | 匹配显示名称中包含的文本。 |
| `has-lore` | 要求或排除 Lore。 |
| `contains-lore` | 匹配 Lore 中包含的文本。 |
| `has-enchants` | 列表中任一直接附魔存在即可通过。 |
| `has-stored-enchants` | 要求存在列出的存储附魔，例如附魔书条目。 |
| `contains-enchants` | 检查附魔等级或范围。 |
| `contains-enchants-amount` | 检查存在多少个符合要求的附魔。 |
| `enchantable` | 检查物品是否符合配置的可附魔规则。 |
| `item-format` | 匹配序列化的 ItemFormat 字段。 |
| `any` | 至少一个嵌套组通过时通过。 |
| `not` | 排除一个嵌套匹配结果。 |
| `contains-nbt` | 旧版 NBT 包含检查；需要 NBTAPI。 |
| `nbt-string` | 旧版字符串 NBT 检查；需要 NBTAPI。 |
| `nbt-byte` | 旧版字节 NBT 检查；需要 NBTAPI。 |
| `nbt-int` | 旧版整数 NBT 检查；需要 NBTAPI。 |
| `nbt-double` | 旧版双精度浮点数 NBT 检查；需要 NBTAPI。 |

## 可选规则的可用性

| 规则 | 要求 |
| --- | --- |
| `contains-nbt`、`nbt-string`、`nbt-byte`、`nbt-int`、`nbt-double` | UltimateQuest 启用时必须已经加载 NBTAPI。 |
| `rarity` | 服务端版本必须支持现代物品稀有度。 |
| `items` 中的外部 ID | 必须安装对应的物品提供器插件，并且其挂钩已经注册。 |

可选规则没有注册时，其键会作为未知键被忽略，而不会被视为匹配失败。依赖可选匹配前，请检查启动日志。

## 完整示例

```yaml
match-item:
  material-tag:
    - minecraft:swords
  has-name: true
  any:
    '1':
      contains-enchants:
        minecraft:sharpness: 3
    '2':
      items:
        - superior_sword
  not:
    contains-lore:
      - Disabled
```

## 相关页面

* [MatchEntityFormat](../match-entity-format/)
* [事件条件](../../configs/conditions/README.md)
* [目标与来源](../../configs/objectives.md)
* [ItemFormat™](../itemformat-tm.md)

## 在任务中使用

下文的 `match-item` 是独立格式示例。在任务目标中，请将其中的规则放入条件的 `match` 内。例如：

```yaml
source:
  trigger: consume
  amount: 16
  conditions:
    food:
      type: match_item
      item: TRIGGER_ITEM
      match:
        material:
          - BREAD
```
