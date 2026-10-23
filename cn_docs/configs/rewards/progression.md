# 🌟 战令积分奖励

使用 `battlepass-experience` 可以把普通任务、周期任务、卷轴或阶段奖励与战令连接起来。例如，完成一个普通任务后额外得到 25 点战令积分。

## 积分奖励配置

先按[战令教程](../battlepasses.md)创建 `tutorial_pass`，再将下面片段放到需要的奖励位置：

```yaml
rewards:
  pass_points:
    type: battlepass-experience
    battlepass: tutorial_pass
    amount: 25
```

`battlepass` 必须指向已经存在且启用的战令 ID。`amount` 必须是正整数。这里填写的是发放基础积分，实际增加量会经过正常的战令倍率规则。

执行 `/uq reload`，用新任务测试。先查看 `/uq battlepass stats tutorial_pass`，完成任务并领奖，再检查累计积分变化。目标战令还需在发放时处于可用状态。

`battlepass-experience` 不用于直接增加里程碑进度；里程碑通过自己的 `sources` 累计。其他奖励类型见[奖励配置](README.md)。
