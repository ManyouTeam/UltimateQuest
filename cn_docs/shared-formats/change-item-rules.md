# 🔧 物品修改格式

在 ItemFormat 内使用 `change-item`，即可在物品生成时修改它。规则按照 YAML 中的顺序执行，后面的规则可以读取前面修改后的结果。

例如，下面的奖励会发放一把添加了 Lore 和效率 I 的铁镐：

```yaml
rewards:
  pickaxe:
    type: item
    item:
      material: IRON_PICKAXE
      change-item:
        add-lore-last:
          - '&7任务奖励'
        modify-enchants:
          minecraft:efficiency: 1
```

已保存物品的 `base-item.change-item` 也可以使用相同格式，详见[自定义物品](../configs/custom-items.md)。

公共修改规则与我们的 MythicChanger 插件使用相同格式。修改名称和 Lore、替换材质、修复耐久、添加附魔等规则，请参考 [MythicChanger 物品修改文档](https://mythicchanger.superiormc.cn/change-rules/empty)。NBT 规则需要 NBTAPI，特定版本的物品组件规则需要对应服务端支持。

本页详细说明 `modify-enchants`。

## `modify-enchants`

直接修改物品上的普通附魔等级，不需要重新构建或序列化物品。

### 加法简写

标量值会被视为附魔等级变化量：

```yaml
change-item:
  modify-enchants:
    minecraft:looting: -1
    minecraft:unbreaking: 2
```

对于带有抢夺 III 和耐久 I 的物品，结果将变为抢夺 II 和耐久 III。

原版附魔可以省略 `minecraft:` 命名空间：

```yaml
change-item:
  modify-enchants:
    looting: -1
```

自定义附魔必须使用完整的命名空间键：

```yaml
change-item:
  modify-enchants:
    minecraft:sharpness: 1
```

### 操作形式

每个附魔也可以改用操作配置部分：

```yaml
change-item:
  modify-enchants:
    minecraft:looting:
      operation: SUBTRACT
      value: 1

    minecraft:unbreaking:
      operation: MULTIPLY
      value: 2

    minecraft:sharpness:
      operation: SET
      value: 'min({current-level} + 1, 10)'
```

| 字段 | 默认值 | 说明 |
| --- | --- | --- |
| `operation` | `ADD` | 可填写 `ADD`、`SUBTRACT`、`MULTIPLY` 或 `SET`。未知值会使用 `ADD`。 |
| `value` | `0` | 操作数。支持数学表达式和 ChangeItem 占位符。 |

### 字段专用占位符

| 占位符 | 含义 |
| --- | --- |
| `{current-level}` | 当前物品中保存的附魔等级。物品没有该附魔时使用 `0`。 |
| `{max-level}` | 该附魔注册的最高等级。 |

### 行为与限制

* 计算结果会四舍五入为最接近的整数。
* 结果小于或等于 `0` 时会移除该附魔。
* 结果为正数时，会使用不安全等级处理添加或替换附魔，因此不会强制执行兼容性和原版最高等级限制。
* 缺失的附魔从等级 `0` 开始，因此正数加法结果可以为物品添加该附魔。
* 未知附魔键会被跳过。
* 无效或非有限表达式会被报告并跳过，而不会因此移除附魔。
* 此规则修改普通物品附魔，不会影响附魔书中保存的附魔。
### 降低一级并在零级时移除

```yaml
change-item:
  modify-enchants:
    minecraft:looting: -1
```

| 修改前 | 修改后 |
| --- | --- |
| 抢夺 III | 抢夺 II |
| 抢夺 II | 抢夺 I |
| 抢夺 I | 移除附魔 |
| 没有抢夺 | 不发生变化 |
