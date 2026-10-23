# 🎲 随机奖励

`random` 从候选组中按权重抽取，发放抽中组里的全部奖励。例如，玩家可能得到面包，也可能得到绿宝石。

## 随机奖励配置示例

把下面的 `rewards` 放入[普通任务示例](../tasks.md)：

```yaml
rewards:
  supplies:
    type: random
    count: 1
    repeat: false
    random:
      food:
        rate: 3
        rewards:
          bread:
            type: item
            item: {material: BREAD, amount: 8}
      gems:
        rate: 1
        rewards:
          emerald:
            type: item
            item: {material: EMERALD, amount: 2}
```

这里只抽一组。`food` 权重 3，`gems` 权重 1，概率分别为 75% 和 25%。`rate` 是相对权重，不要求总和为 100。

`random` 下每个选项还要有一层 `rewards`。一组选中后发全部内容；如果一组放了面包和火把，两件都会发。

## 数量与重复

| 字段 | 默认值 | 要求 / 行为 |
| --- | --- | --- |
| `count` | `1` | 正整数，表示抽几组。 |
| `repeat` | `false` | `false` 不重复选组；`true` 允许同一组重复中选并多次发放。 |
| `random.<ID>.rate` | `0` | 有限非负数，`0` 不参与，所有权重之和须为有限正数。 |
| `random.<ID>.rewards` | 必填 | 非空奖励映射。 |
| `display.name`、`display.lore` | 自动生成 | 仅改变预览。 |

上例改成 `count: 2`、`repeat: false` 会两组都发。候选只有两组，不能不重复抽三组。每轮内的同名奖励条目会独立保留，不会因为 ID 相同就覆盖前一轮奖励。

单次抽取可以显示概率，多次抽取通常显示权重；第一轮的概率不等于最终被选中的概率。随机组最多嵌套 8 层，非法数量、类型、权重或引用会拒绝加载。

## 随机结果的确定时机

普通任务在接取时抽取，周期任务在生成时抽取，卷轴在首次激活时抽取，成长阶段在解锁时抽取。重载或重登不会重抽，详见[保存时机](README.md)。这与点击时执行的[随机动作](../actions/orchestration.md)不同。

多个任务共用候选时，改用[外部奖励池](pools.md)。一个随机条目不能同时填写内联 `random` 和外部 `pool`。
