# 🧟 实体身份与状态规则

本页面介绍 MatchEntityFormat 中用于匹配 Bukkit 实体类型、名称、生命值、标签、PersistentDataContainer 数值、远程分类、怪物和动物的规则。

## 本页面的规则

- `entity-types`
- `entity-contains-name`
- `entity-health`
- `entity-tag`
- `entity-pdc`
- `ranged`
- `monster`
- `animal`

MatchEntityFormat 接受生物实体。同一层级的规则使用逻辑 AND；一条规则内部的列表值通常使用逻辑 OR。

## `entity-types`

不区分大小写比较 Bukkit 实体类型名。

```yaml
entity-types:
- ZOMBIE
- HUSK
```

匹配器接收 LivingEntity，不接收任意弹射物/物品实体。

## `entity-contains-name`

按平台解析实体名，优先自定义名，否则使用普通实体名，包含任一配置子串时匹配。

```yaml
entity-contains-name:
- Guard
```

可用时优先稳定 ID/PDC，不依赖本地化显示名。

## `entity-health`

要求当前绝对生命 >= 配置值。

```yaml
entity-health: 10
```

生命点不是百分比，可结合 entity-types 缩小范围。

## `entity-tag`

测试实体类型注册表标签，monster 为特殊 Monster 接口入口。

```yaml
entity-tag:
- minecraft:skeletons
```

标签可用性取决于当前服务端注册表。

## `entity-pdc`

测试支持的字符串/数字 PDC，任一条目满足即成功。

```yaml
entity-pdc:
  example:rank: '>=10'
```

数字规则含端点 min~max、>=、<=、>、<、= 或数值，字符串支持 * 通配。键和类型必须与写入插件一致，非法数字语法失败。

## `ranged`

检查主手是否为弓、弩或三叉戟。

```yaml
ranged: true
```

false 要求相反状态，不代表所有远程 AI 或法术。

## `monster`

检查 Monster 接口。

```yaml
monster: true
```

false 反转接口测试。

## `animal`

检查 Animals 接口。

```yaml
animal: true
```

不检查驯服归属、成年或可繁殖。

## 组合身份规则

```yaml
match-entity:
  monster: true
  entity-types:
    - SKELETON
    - STRAY
  ranged: true
```

实体必须同时满足这三条规则。

需要允许“带标签的僵尸”或“具有指定名称的骷髅”时，请使用 `any`：

```yaml
match-entity:
  any:
    '1':
      entity-types:
        - ZOMBIE
      entity-contains-name:
        - Elite
    '2':
      entity-types:
        - SKELETON
      entity-contains-name:
        - Archer
```

## 示例：排除类似玩家的实体

```yaml
match-entity:
  monster: true
  not:
    entity-types:
      - PLAYER
      - ARMOR_STAND
```

MatchEntityFormat 面向生物实体；非生物实体类型可能在单独规则执行前就已经匹配失败。

## 常见错误

- 在存在稳定标签、PDC 或集成 ID 时仍使用名称进行匹配；
- 混淆当前生命值和最大生命值；
- 认为 `monster` 或 `animal` 分类一定与自定义语义分类完全相同；
- 认为远程实体一定正在手持弓；
- 使用 MatchEntityFormat 匹配非生物实体；
- 使用目标服务器版本中不存在的实体类型枚举；
- 认为 PDC 数值会在不同数据类型之间自动转换。

另请参阅[装备与集成规则](equipment-integrations.md)和[逻辑规则](logic.md)。
