# 🏆 里程碑

里程碑记录长期积累，例如累计挖矿、钓鱼或击杀。玩家无需接取，满足条件的行为会自动增加进度。达到各阶段门槛后，打开 `/uq milestone` 领取阶段奖励。

## 配置示例：挖矿里程碑

新建 `plugins/UltimateQuest/milestones/tutorial_mining.yml`：

```yaml
enabled: true
name: '采石成就'
icon:
  material: IRON_PICKAXE
sources:
  stone:
    source:
      trigger: block_break
      conditions:
        stone:
          type: block_type
          types: [STONE]
    value: 1
stages:
  '1':
    required: 64
    rewards:
      emerald:
        type: item
        item: {material: EMERALD, amount: 2}
  '2':
    required: 256
    rewards:
      diamond:
        type: item
        item: {material: DIAMOND, amount: 1}
```

执行 `/uq reload`，打开 `/uq milestone tutorial_mining`。之后每挖一块符合条件的天然石头增加 1 点进度，累计到 64 时可领 2 个绿宝石，到 256 时可领 1 个钻石。

第二阶段要求的是累计 256，不是在第一阶段后再挖 256。领取第一阶段奖励不会扣除累计进度。

## 进度来源与阶段门槛

`sources` 决定什么行为增加进度。每个来源可以直接写触发器，也可以引用[目标模板](objectives.md)。`value` 是计数结果的倍率，默认 `1`，必须是正整数。每次贡献为所选 `count` 得出的数量乘以 `value`。

`stages` 决定多少累计进度解锁奖励。阶段编号必须从 `'1'` 连续递增，`required` 必须是严格递增的正整数。阶段奖励放在 `stages.<编号>.rewards`。

不要用目标模板里的 `amount` 设置里程碑阶段要求。里程碑每次贡献为计数结果乘以 `value`，阶段门槛读取的是 `required`。

## 多来源及可选配置

- 可以增加多个来源。同一次行为如果同时符合多个来源，会分别贡献进度。
- 来源使用 `count: events` 时每次贡献一个 `value`；使用数量模式时为数量乘以 `value`。例如 2.5 点伤害乘以 `value: 2`，贡献 5 点。支持情况见[计数方式](counting.md)。
- `categories`、`description`、`icon` 控制展示；`auto-claim: true` 自动尝试领取符合条件的奖励。
- 不填写 `season` 时长期累计。需要独立的一组赛季进度时，可填写合法的赛季 ID，旧进度不会直接合并到新赛季。
- 可用 `starts-at`、`ends-at`、`claim-grace-seconds` 和 `paused` 限制活动与领奖时间。时间写法见[普通任务](tasks.md)。

里程碑本身不占普通任务等[活动任务名额](task-limits.md)。随机阶段奖励在阶段解锁时确定，重载不会重新抽取。

## 领奖操作与配置验证

玩家可在菜单中领奖，也可使用 `/uq milestone claim tutorial_mining 1`，或 `/uq milestone claimall tutorial_mining` 领取全部符合条件的阶段。

先测试进度是否增加，再测试两个累计门槛和奖励。修改阶段要求后，不要假定已经保存或解锁的奖励会被替换，参见[奖励保存规则](rewards/README.md)。
