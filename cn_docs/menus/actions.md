# 🖱️ 菜单点击动作

菜单的图标只负责显示，动作决定点击后发生什么。固定按钮的动作写在 `items.<符号>` 下，任务等动态条目的动作写在 `contents.<符号>` 下。

## 点击类型配置示例

把下面片段合入菜单配置，并在 `layout` 中放置 `Q`：

```yaml
contents:
  Q:
    type: quests
    click-actions:
      LEFT:
        type: quest
      RIGHT:
        type: abandon_quest
        confirm: true
        confirm-seconds: 5
```

左键按任务状态接取、查看或领奖，右键仅放弃符合条件的普通任务。默认五秒内再次点击确认，进度会丢失。

每种点击的 `click-actions` 会覆盖该点击的通用 `actions`。多个具名子动作按顺序执行；只用一个时直接写 `type`。空的对应点击节点可以禁止回退执行通用动作。

## 动作类型与用途

| 目的 | 动作 |
| --- | --- |
| 翻页、刷新、关闭 | `previous_page`、`next_page`、`refresh`、`close`。 |
| 打开另一个菜单或分类选择 | `open_menu`、`open_categories`。 |
| 切换状态或配置分组 | `cycle_view`、`cycle_period_group`、`set_period_group`、`cycle_scroll_group`、`set_scroll_group`、`cycle_challenge_group`。 |
| 操作被点击任务 | `quest`、`quest_info`、`abandon_quest`。 |
| 操作已选战令或里程碑 | `claim_all`、`open_challenges`（战令）。 |
| 提示和视觉效果 | `message`、`action_bar`、`announcement`、`title`、`sound`、`particle`、`effect`。 |
| 执行命令或世界操作 | `console_command`、`player_command`、`op_command`、`command`、`teleport`、`entity_spawn`。 |
| 随机或延迟执行 | `chance`、`any`、`delay`。 |

每种类型的字段和示例见[完整动作参考](../configs/actions/README.md)。任务动作需要条目信息，不能在任意静态按钮中凭空指定被操作任务。`claim_all` 只领取选中成长的阶段奖励，不是所有普通任务奖励。

菜单权限绕过不跳过任务和奖励检查。执行 `/uq reload` 后重新打开界面，分别测试左键、右键及需要的其他点击方式。
