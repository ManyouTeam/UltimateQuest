# 🎒 物品条件

本页用于筛选哪些行为可以计入任务。下面的 `conditions` 示例都是片段：放入目标的 `source` 内，或目标模板根节点；只有不依赖事件数据的玩家条件才适合放在任务根节点检查接取资格。参见[条件基础](README.md)。

## `match_item`

筛选事件物品或装备。示例检查任务玩家的主手工具；`CONTEXT` 则是事件提供的物品，可能是吃掉的食物或交易结果，不一定是主手。空气和缺失物品不匹配。

| 字段 | 默认值 | 说明 |
| --- | --- | --- |
| `item` | CONTEXT | 标量选择器，或含 selector/holder 的节点。 |
| `item-holder` | TARGET | 标量装备选择器的持有者。 |
| `match` | 当前条件节点 | 物品匹配规则，默认当前节点。 |

```yaml
conditions:
  tool:
    type: match_item
    item:
      selector: MAIN_HAND
      holder: PLAYER
    match:
      material:
      - IRON_PICKAXE
      - DIAMOND_PICKAXE
```

物品选择器：`COUNTED_ITEM`、`CONTEXT`、`TRIGGER_ITEM`、`MAIN_HAND`、`OFF_HAND`、`HELMET`、`CHESTPLATE`、`LEGGINGS`、`BOOTS`、`ARMOR`、`ALL_EQUIPMENT`。装备组中任意一件匹配即可。匹配语法见[物品匹配](../../shared-formats/match-item-format/README.md)。

`COUNTED_ITEM` 表示当前正在累计的产物堆。配合 `count: items` 时，条件会对每个钓获物、掉落物或交易产物分别执行，只累计通过条件的数量。此选择器在其他场景中不匹配，工具和装备选择器的含义不变。示例见[计数方式](../counting.md)。

## `catch_match_item`

专门筛选钓鱼获得的物品。它要求钓获对象是物品实体，因此能排除空抛竿、钩住生物等行为。配合 `trigger: fish`、`count: items` 可以按钓获堆叠数量计数。

| 字段 | 默认值 | 说明 |
| --- | --- | --- |
| `match` | 当前条件节点 | 钓获物品堆的匹配规则。 |

```yaml
conditions:
  fish:
    type: catch_match_item
    match:
      material:
      - COD
      - SALMON
      - TROPICAL_FISH
      - PUFFERFISH
```
