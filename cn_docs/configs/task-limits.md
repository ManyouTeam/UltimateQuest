# 📊 任务名额

任务名额限制一位玩家同时进行多少任务。普通任务、卷轴、周期委托和战令挑战可以分别设置上限，也可以按分类限制。

## 配置位置

编辑 `plugins/UltimateQuest/config.yml` 中已有的 `task-limits`，不要重复创建同名根节点。下面是默认配置：

```yaml
task-limits:
  normal:
    total: 20
    categories:
      combat: 6
      gathering: 10
  scroll:
    total: 8
    categories:
      adventure: 4
      gathering: 4
  period:
    total: 28
    categories: {}
  battlepass-task:
    total: -1
    categories: {}
  categories:
    combat: -1
    gathering: -1
    uncategorized: -1
  permission-limits: {}
```

这表示普通任务总共最多 20 个，其中战斗类最多 6 个、采集类最多 10 个。分类 ID 必须已经定义；这里使用插件内置分类。自建分类请先按[分类教程](categories.md)创建。

`-1` 或省略表示不限，`0` 表示不能新增。把上限降低不会删除玩家已经拥有的任务。

## 类型与分类限制

`normal.total` 等限制某一类型的总数，`normal.categories.combat` 限制该类型里的战斗任务。根节点下的 `task-limits.categories.combat` 则限制所有类型合计的战斗任务。

如果一项任务同时属于战斗和采集，它会分别占用这两个分类名额，但在普通任务总量中只算一个。卷轴无论包含几个目标，都只算一张卷轴名额。

## 按权限增加名额

将已有的 `task-limits.permission-limits: {}` 替换为下面的配置，和 `normal`、`scroll` 等节点保持同级：

```yaml
permission-limits:
  vip:
    permission: ultimatequest.limits.vip
    normal:
      total: 30
      categories:
        combat: 10
    scroll:
      total: 12
  svip:
    permission: ultimatequest.limits.svip
    normal:
      total: 50
    scroll:
      total: 16
    period:
      total: 40
```

`vip`、`svip` 是自定义组 ID，`permission` 是必填的权限节点。通过权限插件给玩家授予对应节点后，该组配置参与名额计算，无需在插件权限表中另外声明节点。

每一项上限分别从默认值和所有命中的权限组中取最大值，不累加；`-1` 表示不限，优先于任何有限值。以上配置下，普通玩家的普通任务总上限为 20，VIP 为 30，SVIP 为 50。同时拥有 VIP 和 SVIP 时，普通任务总上限为 50，战斗分类上限为 10，卷轴总上限为 16。

每组均支持 `normal`、`scroll`、`period`、`battlepass-task` 的 `total` 和 `categories`，以及组内根节点 `categories`（跨类型分类上限）。未填写的项沿用默认限制，不会自动变为不限；权限配置只能提高名额，不能降低默认上限。默认值已为 `-1` 或省略的项始终不限；要按权限分档，请先为该项设置有限的默认值。

任务总量、类型内分类和跨类型分类限制仍须同时满足。权限在每次名额检查时读取，权限降低不会删除已有任务；周期任务等待下一次分配检查，战令挑战等待下一次补入检查。OP 和通配符权限是否命中，遵循服务器的 `hasPermission` 结果。

## 名额占用规则

| 内容或状态 | 是否占活动名额 |
| --- | --- |
| 进行中的任务 | 是。 |
| 已完成但还未领奖 | 是。完成后应及时领奖。 |
| 已领奖或失效任务 | 否。 |
| 当前没有携带的卷轴 | 不占活动名额；有效期仍可继续流逝。 |
| 战令或里程碑的等级奖励列表本身 | 否。战令挑战单独按 battlepass-task 计数。 |

## 名额不足时的分配规则

普通任务接取、卷轴激活都会检查名额。管理员 `accept -b` 也不能绕过名额限制。

周期任务按整批检查，容量不够时保留已有任务并等待重试，抽取结果不会因重试改变。战令挑战按配置顺序分配，名额释放后再补入。

修改后执行 `/uq reload`。测试时既要检查达到上限后的拒绝，也要检查领奖释放名额后能否继续接取。
