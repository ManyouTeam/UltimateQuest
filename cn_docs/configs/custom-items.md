# 🧵 保存和复用物品

一件奖励物品需要在多个任务中使用时，可以先保存到 `plugins/UltimateQuest/items/`，以后用物品 ID 引用。文件名去掉 `.yml` 就是 ID。

## 保存主手物品

将物品拿在主手，执行 `/uq saveitem tutorial_pickaxe`。默认使用 Bukkit 序列化保存，适合保留已有物品。

若想得到方便编辑的 ItemFormat 字段，执行 `/uq saveitem tutorial_pickaxe itemformat`。两种写法都会创建 `items/tutorial_pickaxe.yml`。

## 手动创建物品

也可以新建以下文件：

```yaml
# items/tutorial_pickaxe.yml
enabled: true
base-item:
  material: IRON_PICKAXE
  name: '&b勘探镐'
  lore:
    - '首次探险的奖励。'
```

`enabled` 默认 `true`；`base-item` 必填，使用 [ItemFormat](../shared-formats/itemformat-tm.md)。需要在创建时修改物品时，在 `base-item` 中加入 `change-item`，语法见[物品修改格式](../shared-formats/change-item-rules.md)。

执行 `/uq reload` 后用 `/uq givesaveitem tutorial_pickaxe` 发给自己，或 `/uq givesaveitem tutorial_pickaxe Steve 2` 发给在线玩家两份。控制台必须指定玩家。

数量参数是保存物品堆叠的倍数：保存 8 个再给 2 份，会得到 16 个。

## 奖励中的物品引用

将下面片段写到任务的奖励位置：

```yaml
rewards:
  pickaxe:
    type: item
    item:
      material: tutorial_pickaxe
      amount: 1
```

这里 `material` 使用保存的物品 ID，而不是原版材质名。先确认物品能单独发放，再测试新接任务的奖励。

## ItemFormat 导出

`/uq generateitemformat` 将主手物品导出到 `plugins/UltimateQuest/generated-item-format.yml`。它不会在 `items/` 注册物品，每次导出覆盖上次结果。把需要的字段复制到奖励 `item` 或保存物品的 `base-item` 后再使用。

三个物品命令分别需要 `ultimatequest.saveitem`、`ultimatequest.givesaveitem`、`ultimatequest.generateitemformat`。详见[命令与权限](../info/commands-permissions.md)。
