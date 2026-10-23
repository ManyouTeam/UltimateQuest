# 📜 任务与领奖动作

本页说明菜单点击后可以执行的操作。每段示例都是动作片段，放在 `items.<符号>.actions` 或 `contents.<符号>.actions` 下；动作不是任务奖励类型。完整放置示例见[动作基础](README.md)。

## `quest`

按被点击条目的状态操作：接取普通任务、查看进行中的任务、领取完成奖励，或打开战令/里程碑详情及领取阶段。它使用点击条目，不读取另一个 quest ID 参数。

```yaml
type: quest
```

## `quest_info`

显示被点击任务的信息。放在普通静态按钮上却没有任务条目时，没有可查看的对象。

```yaml
type: quest_info
```

## `abandon_quest`

放弃允许放弃、状态为 ACTIVE 或 COMPLETED 的普通任务，已有进度会丢失。默认要求五秒内再次点击确认；confirm-seconds 最少为 1。生成委托及成长奖励不是通用可放弃任务。

| 字段 | 默认值 | 说明 |
| --- | --- | --- |
| `confirm` | true | 要求二次点击。 |
| `confirm-seconds` | 5 | 确认窗口，至少 1 秒。 |

```yaml
type: abandon_quest
confirm: true
confirm-seconds: 5
```

## `claim_all`

领取当前选中战令或里程碑中所有符合资格的阶段奖励。它不代表领取玩家全部普通任务奖励。

```yaml
type: claim_all
```

## `open_challenges`

打开当前选中战令的挑战页，需要菜单已经选中一份战令。

```yaml
type: open_challenges
```

## 配置验证

执行 `/uq reload` 后重新打开菜单并点击对应按钮。任务类动作需要被点击的任务或已选中的战令/里程碑，不能仅靠给静态按钮添加一个任务名称来建立关联。导航保留本次菜单权限绕过设置，但仍检查任务条件和奖励资格。
