# 🔗 兼容性

安装对应插件后，UltimateQuest 会启用相关兼容功能。内置生存服任务和原版物品奖励不需要额外插件即可使用。

## 自定义物品插件

在支持的 [ItemFormat™](../shared-formats/itemformat-tm.md) 物品配置和[物品匹配](../shared-formats/match-item-format/README.md)规则中，可以使用以下插件的物品：

* ItemsAdder
* Oraxen
* MMOItems
* EcoItems 和 EcoArmor
* eco
* NeigeItems
* ExecutableItems
* Nexo
* CraftEngine
* MythicMobs 物品

例如，可以把自定义物品用作任务图标或奖励，也可以要求目标使用指定物品。请填写物品插件的物品 ID，而不是显示名称。除非你的配置有特殊要求，否则请保留 `config.yml` 中的 `hook-item-method: DEFAULT`。

## 自定义方块

`block_type` 条件支持 ItemsAdder、Oraxen、MMOItems、Nexo 和 CraftEngine 的自定义方块。除了原版方块，你也可以为服务器的自定义方块设置任务。详见[环境与方块条件](../configs/conditions/environment.md)。

## MythicMobs

安装 MythicMobs 后，可以通过内部怪物 ID 筛选生物，也可以使用 MythicMobs 物品。例如，让击杀目标只统计指定 Boss。详见[装备与集成规则](../shared-formats/match-entity-format/equipment-integrations.md)。

## PlaceholderAPI

支持的文本和玩家条件可以使用 PlaceholderAPI。UltimateQuest 也提供用于查询任务进度、战令和里程碑的占位符，完整列表见 [PlaceholderAPI](../features/placeholders.md)。

PlaceholderAPI 为可选插件，正常使用 UltimateQuest 不需要安装它。

## NBTAPI

NBTAPI 用于启用 NBT 匹配和物品修改规则。如果配置用到了这些规则，请先安装 NBTAPI，再启动服务器。匹配自定义物品时，如果物品插件提供公开的物品 ID，建议优先使用该 ID。

## 经济插件

如需发放金币、点数或其他插件的货币，请使用 `console_command` 奖励执行对应插件的命令。例如，经济插件支持 `eco give` 时：

```yaml
rewards:
  money:
    type: console_command
    command: 'eco give {player} 100'
    display:
      name: '&6100 金币'
```

请将命令改成服务器实际使用的经济插件命令。详见[物品、命令与消息奖励](../configs/rewards/items-commands.md)。

## 领地与保护插件

任务会统计玩家进行的操作，不会赋予玩家在受保护区域内建造或破坏的权限。操作被保护插件取消后，不会增加普通任务进度。

## Paper、Spigot 和 Folia

部分触发器仅适用于 Paper。在 Spigot 上使用目标前，请先查看[触发器说明](../configs/triggers.md)中的平台要求。Folia 支持仍处于实验阶段，详见[运行要求](requirements.md)。
