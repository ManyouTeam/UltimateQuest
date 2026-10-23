# ✨ 附魔与 ItemFormat 规则

本页面介绍 MatchItemFormat 中用于匹配直接附魔、存储附魔、附魔等级与数量、可附魔性，以及序列化 ItemFormat 的规则。

## 本页面的规则

* `has-enchants`
* `has-stored-enchants`
* `contains-enchants`
* `contains-enchants-amount`
* `enchantable`
* `item-format`

## 直接附魔与存储附魔

Minecraft 会将物品上的直接附魔，与附魔书中保存的附魔分开存储。应根据实际检查的组件选择对应规则。

## `has-enchants`

任一列表直接附魔匹配，* 检查任意直接附魔。

```yaml
has-enchants:
- minecraft:efficiency
```

附魔书储存数据用 has-stored-enchants，不是直接附魔元数据。

## `has-stored-enchants`

匹配附魔书中保存的附魔，列表中任一附魔存在即可通过。

```yaml
has-stored-enchants:
- minecraft:mending
```

普通附魔工具不满足储存附魔检查。

## `contains-enchants`

附魔书读取储存等级，其他物品读取直接等级。

```yaml
contains-enchants:
  minecraft:efficiency:
  - 3
  - 4
  - 5
```

标量严格 >，列表精确包含。需要分别检查多个附魔时，请为每项要求单独配置物品条件。

## `contains-enchants-amount`

统计不同附魔种数，不是等级总和。

```yaml
contains-enchants-amount: 2
```

标量为 >=，整数列表为精确包含，附魔书用储存种数。

## `enchantable`

检查物品能否接受列表中的任一附魔。

```yaml
enchantable:
- minecraft:efficiency
```

不证明外部附魔效果已激活，UltimateQuest 也不注册自定义附魔。

## `item-format`

根据指定的 ItemFormat 字段比较物品。

```yaml
item-format:
  material: DIAMOND_PICKAXE
item-format-settings:
  require-same-key: false
  ignore-key:
  - lore
```

amount 始终忽略，ignore-key 排除精确字段及子字段。require-same-key 要求所有未忽略导出键均在比较配置中。字符串允许不区分大小写相等。应导出实际物品确定字段。

## 示例：高等级附魔

```yaml
match-item:
  material-tag:
    - minecraft:pickaxes
  contains-enchants:
    minecraft:efficiency: 3
```

## 示例：保护或火焰保护

```yaml
match-item:
  any:
    '1':
      has-enchants:
        - minecraft:protection
    '2':
      has-enchants:
        - minecraft:fire_protection
```

## 示例：附魔书筛选器

```yaml
match-item:
  material:
    - ENCHANTED_BOOK
  has-stored-enchants:
    - minecraft:sharpness
```

## 示例：灵活的 ItemFormat 比较

```yaml
match-item:
  item-format:
    material: NETHERITE_SWORD
  not:
    contains-lore:
      - Disabled
```

## 常见错误

* 使用 `has-enchants` 匹配附魔书中的存储附魔；
* 使用显示名称代替命名空间附魔键；
* 将物品可附魔性与特定附魔是否支持该物品混淆；
* 修改自定义附魔键后，仍期望旧物品能够匹配；
* 使用不稳定元数据对 `item-format` 施加过多限制；
* 认为附魔数量规则会以完全相同的方式计算直接附魔和存储附魔。

另请参阅[物品条件](../../configs/conditions/items.md)、[ItemFormat™](../itemformat-tm.md)和[逻辑与 NBT 规则](logic-nbt.md)。
