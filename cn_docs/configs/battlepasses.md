# 🌟 战令

战令把“完成挑战”和“升级领奖”连在一起：玩家完成挑战得到积分，累计积分达到等级门槛后，解锁该等级的奖励。一份战令可以同时有免费奖励和需要高级资格的奖励。

挑战会自动分配。玩家用 `/uq battlepass` 选择战令，在挑战页做任务，在等级页领奖。

## 战令配置示例

新建 `plugins/UltimateQuest/battlepasses/tutorial_pass.yml`：

```yaml
enabled: true
name: '入门战令'
season: season_1
season-name: '第一赛季'
icon:
  material: EXPERIENCE_BOTTLE
cap-experience: true
premium-by-permission: true
premium-permission: ultimatequest.battlepass.tutorial_pass.premium
tasks:
  mining:
    name: '采石挑战'
    points: 25
    group: daily
    reset: daily
    source:
      trigger: block_break
      amount: 16
      conditions:
        stone:
          type: block_type
          types: [STONE]
levels:
  '1':
    required: 25
    free:
      bread:
        type: item
        item: {material: BREAD, amount: 8}
    premium:
      emerald:
        type: item
        item: {material: EMERALD, amount: 2}
  '2':
    required: 50
    free:
      iron:
        type: item
        item: {material: IRON_INGOT, amount: 4}
```

执行 `/uq reload`，输入 `/uq battlepass tutorial_pass`。当日挖 16 块天然石头完成挑战，自动得到 25 分，解锁第 1 级免费面包。第二天挑战刷新，再完成一次可累计到 50 分并解锁第 2 级。

想立即核对第二级显示，可由管理员执行 `/uq battlepass addxp Steve tutorial_pass 25`，把 Steve 换成测试玩家。正式测试还应确认挑战本身确实增加积分。

## 挑战配置

`tasks.<挑战ID>` 中使用普通任务的 `source` 或 `objectives`。每个挑战必须有正整数 `points`，表示完成时奖励多少战令积分。

| 字段 | 默认 / 要求 | 作用 |
| --- | --- | --- |
| `reset` | 默认取 `group`，两者都未填则为 `season` | `daily` 每日、`weekly` 每周、`season` 每赛季刷新。 |
| `group`、`group-name` | 可省略 | 挑战列表中的分组和显示名称；自定义分组时应显式填写合法 `reset`。 |
| `unlock-week` | `1` | 从赛季第几周开始开放。 |
| `catch-up` | `true` | 开放周过去后仍可做；`false` 只允许在开放周做。 |
| `premium` | `false` | `true` 要求玩家有该战令的高级资格。 |
| `categories` | 空列表 | 挑战自身分类，与整个战令的目录分类独立。 |

挑战完成后自动结算积分，不需要像普通任务一样手动领取积分。挑战占用 `battlepass-task` [任务名额](task-limits.md)，按配置顺序分配，释放名额后再补入。

## 等级门槛与奖励

`levels` 从 `'1'` 连续编号，`required` 是累计积分且必须严格递增。上例第二级要求总计 50 分，不是第一级之后再获得 50 分。

`free` 和 `premium` 是两个奖励区，各自放[奖励配置](rewards/README.md)。高级资格决定能否领取 `premium` 奖励；菜单显示两行奖励不代表玩家自动拥有资格。等级奖励本身不占活动任务名额。

设置 `premium-by-permission: true` 后，拥有 `premium-permission` 指定权限的玩家具备高级资格。管理员也可以用 `/uq battlepass premium Steve tutorial_pass true` 持久化授予资格。

`auto-claim: true` 自动尝试领取符合条件的等级奖励。`cap-experience: true` 将累计积分封顶到最后一级门槛。

## 自动生成等级门槛

级数较多时，可在根节点添加：

```yaml
leveling:
  count: 30
  base: 100
  increment: 10
  multiplier: 1.0
```

每级新增所需积分为 `ceil((base + increment × (level - 1)) × multiplier^(level - 1))`，再累计成门槛。示例前三级门槛是 100、210、330。

`count` 为 1–10000，`base` 为正数，`increment` 非负，`multiplier` 至少为 1，结果不能溢出。显式的 `levels.<等级>.required` 优先；生成后仍须保持门槛递增。等级奖励仍配置在 `levels` 下。

## 赛季轮换和旧赛季领奖

示例的 `season: season_1` 是固定赛季。想按自然月轮换，在根节点加入：

```yaml
season-prefix: expedition
rotation:
  enabled: true
  reset-mode: CRON
  reset-time: '0 0 0 1 * ?'
claim-grace-seconds: 604800
```

轮换使用[周期刷新格式](periods.md)及服务端时区，以前缀和周期起点生成赛季 ID。首次安装进入当前日历周期，而不是从安装时刻开始算一个月。

旧赛季停止累计后，已解锁奖励可在上例的 7 天宽限内领取。用 `/uq battlepass history tutorial_pass` 查询已保存赛季，用 `<战令ID>@<赛季ID>` 打开。可选 `starts-at`、`ends-at` 另行限制活动时间，`paused: true` 暂停活动与领奖。

## 管理赛季与积分倍率

以下命令需要管理员权限：

```text
/uq battlepass season pause tutorial_pass
/uq battlepass season resume tutorial_pass
/uq battlepass season end tutorial_pass
/uq battlepass season start tutorial_pass season_2 2592000
/uq battlepass season schedule tutorial_pass season_3 2026-11-01T00:00:00Z 2026-12-01T00:00:00Z
/uq battlepass season globalboost tutorial_pass 2 3600
```

`start`、`schedule` 和 `end` 手动管理会关闭该战令的自动轮换。玩家倍率、全服倍率和权限倍率取其中最大值，不相乘。权限倍率通过 `permission-boosters.<ID>.permission` 和 `.multiplier` 配置。

修改定义不会重抽已经解锁的随机等级奖励。完整管理命令见[命令与权限](../info/commands-permissions.md)。
