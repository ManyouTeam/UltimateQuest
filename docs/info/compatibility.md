# 🔗 Compatibility

UltimateQuest loads optional integrations when the corresponding plugin is installed. The built-in survival tasks and vanilla item rewards work without additional plugins.

## Custom item providers

You can use items from the following plugins in supported [ItemFormat™](../shared-formats/itemformat-tm.md) sections and [item matching](../shared-formats/match-item-format/README.md) rules:

* ItemsAdder
* Oraxen
* MMOItems
* EcoItems and EcoArmor
* eco
* NeigeItems
* ExecutableItems
* Nexo
* CraftEngine
* MythicMobs items

For example, use a custom item as a task icon or reward, or require a particular item for an objective. Use the provider's item ID rather than its display name. Keep `hook-item-method: DEFAULT` in `config.yml` unless your setup requires another mode.

## Custom blocks

The `block_type` condition supports custom block hooks for ItemsAdder, Oraxen, MMOItems, Nexo, and CraftEngine. This lets you create tasks for your server's custom blocks as well as vanilla materials. See [Environment and Block Conditions](../configs/conditions/environment.md).

## MythicMobs

With MythicMobs installed, you can match a mob's internal MythicMobs ID and use MythicMobs items. For example, a kill objective can be restricted to one configured boss. See [Equipment and Integration Rules](../shared-formats/match-entity-format/equipment-integrations.md).

## PlaceholderAPI

PlaceholderAPI can be used in supported text and player conditions. UltimateQuest also provides its own placeholders for task progress, battle passes, and milestones. See [PlaceholderAPI](../features/placeholders.md) for the available placeholders.

PlaceholderAPI is optional and is not needed for normal operation.

## NBTAPI

NBTAPI enables the NBT matching and item-change rules. Install it before starting the server if your configuration uses these rules. For custom items, prefer the provider's public item ID when one is available.

## Economy plugins

To give money, points, or another plugin's currency as a reward, use that plugin's command through a `console_command` reward. For example, if your economy plugin supports `eco give`:

```yaml
rewards:
  money:
    type: console_command
    command: 'eco give {player} 100'
    display:
      name: '&6100 coins'
```

Change the command to match the economy plugin installed on your server. See [Item, Command, and Message Rewards](../configs/rewards/items-commands.md).

## Protection plugins

Tasks observe player actions; they do not grant permission to build or break blocks in protected areas. If a protection plugin cancels the action, it does not count toward normal task progress.

## Paper, Spigot, and Folia

Some triggers are available only on Paper. Check the platform requirements listed on the [trigger pages](../configs/triggers.md) before using those objectives on Spigot. Folia support is experimental; see [Requirements](requirements.md).
