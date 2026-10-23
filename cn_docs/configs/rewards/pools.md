# 🗂️ 奖励池与奖励库

多个任务需要同一组随机候选时，用奖励池；需要反复使用一条完整奖励时，用奖励库。它们都不负责抽取任务目标。

## 奖励池配置示例

在 `plugins/UltimateQuest/` 下创建这个文件：

```yaml
# reward-pools/tutorial_supplies.yml
enabled: true
rewards:
  food:
    rate: 3
    rewards:
      bread:
        type: item
        item: {material: BREAD, amount: 8}
  gems:
    rate: 1
    rewards:
      emerald:
        type: item
        item: {material: EMERALD, amount: 2}
```

在任务的奖励位置引用它：

```yaml
rewards:
  supply:
    type: random
    pool: tutorial_supplies
    count: 1
    repeat: false
```

保存两个文件并执行 `/uq reload`。新接取的任务会从这个池抽一组奖励。`count` 和 `repeat` 写在引用处，因此不同任务可以用同一个池、不同的抽取次数。

池 ID 是文件名，只允许小写字母、数字、下划线和短横线。只扫描 `reward-pools/` 直接子级 `.yml`；池必须存在并启用。

## 奖励库引用

在 `config.yml` 中添加奖励库，再在任务中引用：

```yaml
# config.yml
reward-library:
  starter_bread:
    type: item
    item: {material: BREAD, amount: 8}
```

```yaml
# 任务奖励片段
rewards:
  food:
    reference: starter_bread
```

`reference` 会替换整个引用条目。不能一边引用一边在同一级加 `amount` 来覆盖，引用处其他字段会被替换。想要不同数量，应创建另一条库奖励或直接写完整奖励。

## 常见错误

- 不要同时填写 `pool` 和内联 `random`。
- 奖励池里的候选要包含 `rate` 和嵌套 `rewards`，不能直接照搬 `pools/` 的目标权重表。
- 缺失、循环或过深引用会导致加载失败。重载也会校验启用池及使用它的配置。

可修改 `reward-pools/*.yml` 中的池配置，再使用 `/uq reload` 应用更改。修改池不会重抽已有任务或已经解锁的阶段奖励，参见[随机奖励](random.md)。
