# 🧩 控制流动作

本页说明菜单点击后可以执行的操作。每段示例都是动作片段，放在 `items.<符号>.actions` 或 `contents.<符号>.actions` 下；动作不是任务奖励类型。完整放置示例见[动作基础](README.md)。

## `chance`

每次执行时进行一次百分比判断，通过后执行全部子动作。`rate: 25` 表示本次有 25% 的机会运行；使用 0–100，不使用奖励池权重。

| 字段 | 默认值 | 说明 |
| --- | --- | --- |
| `rate` | 必填 | 与 [0,100) 随机值比较的百分比。 |
| `actions` | 必填 | 嵌套动作映射。 |

```yaml
type: chance
rate: 25
actions:
  feedback:
    type: message
    message: '&a抽中了！'
```

## `any`

从子动作中均匀、不重复地选出最多 amount 项执行。amount 超过候选数时执行全部，没有不同权重。

| 字段 | 默认值 | 说明 |
| --- | --- | --- |
| `amount` | 1 | 最多选择数量。 |
| `actions` | 必填 | 子动作映射。 |

```yaml
type: any
amount: 1
actions:
  one:
    type: message
    message: '&a第一项'
  two:
    type: message
    message: '&b第二项'
```

## `delay`

等待 time 个服务端 tick 后执行子动作。20 TPS 时 time: 20 约为一秒，执行时玩家必须仍在线。它保留原点击的信息，但任务状态可能已经变化。

| 字段 | 默认值 | 说明 |
| --- | --- | --- |
| `time` | 必填 | 整数服务端 tick 延迟。 |
| `actions` | 必填 | 子动作映射。 |

```yaml
type: delay
time: 20
actions:
  notice:
    type: message
    message: '&e延迟提示。'
```

## 配置验证

执行 `/uq reload` 后重新打开菜单并点击对应按钮。任务类动作需要被点击的任务或已选中的战令/里程碑，不能仅靠给静态按钮添加一个任务名称来建立关联。导航保留本次菜单权限绕过设置，但仍检查任务条件和奖励资格。
