# ➗ 数学计算格式

当 `config.yml -> math.enabled` 为 `true` 时，支持的数值字段可以使用表达式代替固定数字。例如，在物品修改中计算修复的耐久：

```yaml
change-item:
  repair-damage: 'min({max-damage}, 50)'
```

此例最多修复 50 点耐久，且不超过物品的最大耐久值。

## 配置

```yaml
math:
  enabled: true
  enable-function: true
```

`enabled: false` 时请填写普通数字；`enable-function` 控制是否启用下列函数。

## 运算与函数

可以使用 `'10 + 5'`、`'20 * 0.5'`、`'min(100, 25 * 3)'` 等表达式。YAML 中的表达式请加引号。

| 函数 | 示例 | 结果 |
| --- | --- | --- |
| `sum` | `sum(1, 2, 3)` | `6` |
| `max` | `max(5, 10)` | `10` |
| `min` | `min(5, 10)` | `5` |
| `avg` | `avg(2, 4)` | `3` |
| `floor` | `floor(2.8)` | `2` |
| `ceil` | `ceil(2.1)` | `3` |
| `round` | `round(2.6)` | `3` |
| `random` | `random(1, 5)` | 在给定上下限之间取随机值。 |

公共表达式格式也用于 UltimateShop，更多背景可参考其[格式文档](https://ultimateshop.superiormc.cn/format)。UltimateQuest 可使用表达式的位置见下文。

## 可以使用表达式的位置

物品修改中的数值规则，以及支持的菜单动作数值参数（如粒子偏移量）可以使用表达式。每条规则可能提供自己的占位符。例如，耐久规则提供 `{damage}`、`{max-damage}`，`modify-enchants` 提供 `{current-level}`、`{max-level}`。

任务目标数量、里程碑门槛、目标池权重和奖励抽取数量，请使用各配置页要求的整数或区间格式，不要改成公式。ItemFormat 的 `amount` 支持固定整数或 `2~5` 这样的包含端点区间。

## 占位符

只使用当前字段提供的占位符。该位置没有提供的占位符，无法作为数字参与计算。尤其要注意，公共物品修改规则中的 `{level}` 不代表玩家的战令等级。

例如，下面把锋利增加一级，但不超过其注册的最高等级：

```yaml
change-item:
  modify-enchants:
    minecraft:sharpness:
      operation: SET
      value: 'min({current-level} + 1, {max-level})'
```

完整规则示例见[物品修改格式](change-item-rules.md)。
