# 🌟 Battle pass point rewards

Use `battlepass-experience` to connect task, period, scroll, or stage rewards to a pass. For example, claiming a normal task can award 25 base pass points.

## Point reward configuration

Create `tutorial_pass` using the [battle pass tutorial](../battlepasses.md), then add this fragment at the appropriate reward path:

```yaml
rewards:
  pass_points:
    type: battlepass-experience
    battlepass: tutorial_pass
    amount: 25
```

`battlepass` must reference an existing enabled pass ID. `amount` must be a positive integer. This is the base point award; normal battle pass multipliers affect the actual increase.

Run `/uq reload` and test on a new task. Check `/uq battlepass stats tutorial_pass`, complete and claim the task, then compare totals. The target pass must also be available when the reward is issued.

This type does not directly add milestone progress. Milestones accumulate through their own `sources`. See [rewards](README.md) for other types.
