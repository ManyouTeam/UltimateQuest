# ⏰ 周期任务

周期任务适合每日、每周或每月委托。玩家无需手动接取，系统会自动分配当期任务，在 `/uq period` 中查看和领奖。每个周期重新生成一批任务。

## 配置示例：每日采石

新建 `plugins/UltimateQuest/periods/tutorial_daily.yml`：

```yaml
enabled: true
name: '每日采石'
reset-mode: CRON
reset-time: '0 0 0 * * ?'
claim-grace-seconds: 3600
source:
  name: '挖掘石头'
  trigger: block_break
  amount: 32
  conditions:
    stone:
      type: block_type
      types: [STONE]
task:
  auto-claim: false
  rewards:
    bread:
      type: item
      item:
        material: BREAD
        amount: 8
```

执行 `/uq reload` 后，用 `/uq period` 找到“每日采石”。当期挖 32 块天然石头，完成后领取 8 个面包。此例没有目标池依赖，每天生成一项委托。

## 周期配置与任务配置

根节点的 `source` 决定分配什么目标。它可以直接写触发器，也可以用 `quest` 引用[目标模板](objectives.md)，或用 `pool` 从[目标池](pools.md)抽取。

`task` 决定每项生成任务的分类、描述、奖励及自动领奖等设置。因此奖励写在 `task.rewards`，分类写在 `task.categories`，不是周期文件根节点。

要每期随机分配多项委托，先按目标池教程建立候选，再将 `source` 换为 `pool` 与 `count`。抽到的每个目标都会成为独立委托，各自使用 `task.rewards`。例如抽 3 项且配置 8 个面包，完成全部 3 项共可得 24 个面包。

## 按分类抽取

内置每日、每周、每月目标池各有 100 个候选目标。每日和每周仍随机抽取 3 个；每月从现有 8 个分类各抽 1 个：

```yaml
source:
  pool: monthly
  categories:
    adventure: 1
    building: 1
    combat: 1
    craftsmanship: 1
    farming: 1
    fishing: 1
    gathering: 1
    nether: 1
```

`source.categories` 与 `source.count` 二选一。分类对应 `quests/` 目标模板的 `categories`，不是生成任务的 `task.categories`。配额必须为正整数，合计不能超过 45。同一目标即使属于多个分类，一批任务也只会抽中一次；候选不足以满足全部配额时，配置重载会失败。

每项委托仍有独立奖励。默认周期任务名额调整为 28，可容纳每日 3 项、每周 3 项、每月 8 项，以及处于领奖宽限期的已完成任务。更新插件不会覆盖现有配置；已有服务器需同步更新三个 `pools/` 文件、补充新增 `quests/` 文件，并更新 `periods/monthly.yml` 和 `task-limits.period.total`。

## 刷新时间

刷新采用服务端 Java 进程的系统时区。请先确认服务器时区，不要默认它等于玩家所在地的时区。

| `reset-mode` | `reset-time` 示例 | 含义 |
| --- | --- | --- |
| `CRON` | `'0 0 0 * * ?'` | 每天零点，含秒的 Quartz Cron。 |
| `CRON` | `'0 0 0 ? * MON'` | 每周一零点。 |
| `CRON` | `'0 0 0 1 * ?'` | 每月 1 日零点。 |
| `TIMED` | `'00:00:00;;12:00:00'` | 每天在列出的本地时间刷新，时间用 `;;` 分开。 |
| `TIMER` | `'1:00:00:00'` | 每隔一天，格式为 `[天:]时:分:秒`，间隔必须大于零。 |
| `CUSTOM` | `'2026-12-01 00:00:00'` | 固定结束时间，开始时间由 `reset-anchor` 指定。 |

`reset-anchor` 默认是 `'1970-01-01 00:00:00'`。战令自动轮换也使用这套刷新格式。

可选的 `starts-at`、`ends-at` 控制整个活动的可用时间，使用带时区的 ISO-8601 时间，例如 `'2026-11-01T00:00:00Z'`。它们与表中的本地日期格式不同。`paused: true` 可暂停活动。

## 周期结束与领奖宽限

周期结束后，旧任务停止增加进度。`claim-grace-seconds: 3600` 允许已完成任务额外领奖一小时；没完成的任务不能利用这一小时继续做。

分配一批新委托前会检查[任务名额](task-limits.md)。名额不足时不会只发一部分，而是保留已有任务，等条件满足再重试；重试保留已经确定的抽取结果。

## 配置生效与验证

重载不会替换已经生成的任务要求或奖励。修改配置后应检查下一批新生成的委托。测试刷新时可使用单独的测试周期配置和较短间隔，确认后再恢复正式时间。
