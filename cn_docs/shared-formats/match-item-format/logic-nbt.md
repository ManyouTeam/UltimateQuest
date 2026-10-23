# 🧠 逻辑与 NBT 规则

本页面介绍 MatchItemFormat 的逻辑组合方式，以及可选的旧版 NBT 匹配规则。

## 本页面的规则

- `none`
- `any`
- `not`
- `contains-nbt`
- `nbt-string`
- `nbt-byte`
- `nbt-int`
- `nbt-double`

只有 UltimateQuest 启用时 NBTAPI 已经可用，NBT 规则才会被注册。

## 默认 AND 行为

同一层级中，每一条可识别规则都必须通过：

```yaml
match-item:
  material-tag:
    - minecraft:swords
  has-name: true
  contains-name:
    - Legendary
```

该物品必须同时满足这三个规则组。

## `none`

仅 true 时强制失败。

```yaml
none: true
```

与实体 none 不同，物品规则检查布尔值。

## `any`

支持平铺 OR 或数字 AND 组。

```yaml
any:
  '1':
    material:
    - DIAMOND_SWORD
    has-name: true
  '2':
    material:
    - IRON_AXE
```

子键 1 启用分组，空 any 通过。

## `not`

反转嵌套物品匹配。

```yaml
not:
  contains-lore:
  - Disabled
```

其他同级规则仍需满足。

## 嵌套逻辑

以下表达式表示：

```text
(sword OR axe) AND NOT disabled
```

```yaml
match-item:
  any:
    '1':
      material-tag:
        - minecraft:swords
    '2':
      material-tag:
        - minecraft:axes
  not:
    contains-lore:
      - Disabled
```

尽可能减少嵌套层级。过深的逻辑更难调试，并且通常可以改用可复用的物品 ID 或标签简化。

## 可选 NBT 规则

现代 Minecraft 的物品数据越来越多地使用数据组件，而不是旧版任意 NBT。下方规则通过 NBTAPI 提供兼容支持，不应作为新配置的首选方案。

{% hint style="warning" %}
如果 UltimateQuest 注册匹配规则时没有加载 NBTAPI，这些键会成为未知键并被忽略。可选键被忽略后，匹配范围可能比预期更广。依赖 NBT 规则前，请检查启动日志。
{% endhint %}

## `contains-nbt`

检查任一传统标签路径存在。

```yaml
contains-nbt:
- quest_marker
- custom;;marker
```

父复合节点以 ;; 分隔，启动需要 NBTAPI。

## `nbt-string`

要求字符串标签精确值。

```yaml
nbt-string:
- quest_marker;;mining
```

最后两段为标签和预期值。

## `nbt-byte`

检查对应数值类型的 NBT 标签。列表项格式为 `标签;;运算符;;数值`，父级复合标签用 `;;` 分隔。

```yaml
nbt-byte:
  - custom;;bound;;==;;1
```

支持 `>=`、`>`、`==`、`<`、`<=`。存储的 NBT 类型必须与选用的规则一致。

## `nbt-int`

检查对应数值类型的 NBT 标签。列表项格式为 `标签;;运算符;;数值`，父级复合标签用 `;;` 分隔。

```yaml
nbt-int:
  - custom;;tier;;>=;;3
```

支持 `>=`、`>`、`==`、`<`、`<=`。存储的 NBT 类型必须与选用的规则一致。

## `nbt-double`

检查对应数值类型的 NBT 标签。列表项格式为 `标签;;运算符;;数值`，父级复合标签用 `;;` 分隔。

```yaml
nbt-double:
  - custom;;power;;>=;;1.5
```

支持 `>=`、`>`、`==`、`<`、`<=`。存储的 NBT 类型必须与选用的规则一致。

## NBT、PDC 与提供器 ID

建议按照以下优先级选择物品标识方式：

1. 已注册的自定义物品提供器 ID；
2. 命名空间附魔键；
3. 稳定的材质、标签和组件元数据；
4. 支持时使用由插件自己管理的 PersistentDataContainer 或组件规则；
5. 只有确实必要时才匹配旧版 NBT。

提供器插件可能会在不同版本之间重写内部 NBT，同时保持公开的物品 ID 不变。

## 调试逻辑

1. 将每个嵌套组单独作为完整的 `match-item` 配置部分进行测试。
2. 确认每个规则键都已经注册。
3. 暂时使用材质或提供器 ID 检查替换可选 NBT 检查。
4. 使用可信的检查工具确认实际 NBT 类型、路径和值。
5. 只有正向规则正常工作后，才添加 `not`。
6. 记录或显示触发器实际提供的物品；延迟事件或装备事件提供的物品可能与预期不同。

## 常见错误

- 认为顶层规则使用 OR；
- 没有正确建立子组，就将多个独立规则键放在 `any` 下；
- 意外反转了比预期更大的配置组；
- NBTAPI 没有注册时仍依赖 NBT 规则；
- 使用 `nbt-double` 比较整数标签，或反过来使用整数规则比较双精度值；
- 匹配提供器私有 NBT，而不是公开的物品 ID；
- 认为未知键会使匹配失败。

另请参阅[基础与元数据规则](basic-metadata.md)和[附魔与 ItemFormat 规则](enchantments-format.md)。
