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
    total: 12
    categories: {}
  battlepass-task:
    total: -1
    categories: {}
  categories:
    combat: -1
    gathering: -1
    uncategorized: -1
```

这表示普通任务总共最多 20 个，其中战斗类最多 6 个、采集类最多 10 个。分类 ID 必须已经定义；这里使用插件内置分类。自建分类请先按[分类教程](categories.md)创建。

`-1` 或省略表示不限，`0` 表示不能新增。把上限降低不会删除玩家已经拥有的任务。

## 类型与分类限制

`normal.total` 等限制某一类型的总数，`normal.categories.combat` 限制该类型里的战斗任务。根节点下的 `task-limits.categories.combat` 则限制所有类型合计的战斗任务。

如果一项任务同时属于战斗和采集，它会分别占用这两个分类名额，但在普通任务总量中只算一个。卷轴无论包含几个目标，都只算一张卷轴名额。

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
