# 🧵 Save and reuse items

Save an item under `plugins/UltimateQuest/items/` when several tasks should reuse it. Reference it by its filename without `.yml`.

## Save the main-hand item

Hold it in your main hand and run `/uq saveitem tutorial_pickaxe`. The default Bukkit serialization format preserves an existing item.

For editable ItemFormat fields, use `/uq saveitem tutorial_pickaxe itemformat`. Both forms create `items/tutorial_pickaxe.yml`.

## Define an item manually

Alternatively, create this file:

```yaml
# items/tutorial_pickaxe.yml
enabled: true
base-item:
  material: IRON_PICKAXE
  name: '&bSurvey pickaxe'
  lore:
    - 'A reward for your first expedition.'
```

`enabled` defaults to `true`. Required `base-item` uses [ItemFormat](../shared-formats/itemformat-tm.md). Add `change-item` inside it for creation-time changes; see [item changes](../shared-formats/change-item-rules.md).

Reload and run `/uq givesaveitem tutorial_pickaxe` for yourself or `/uq givesaveitem tutorial_pickaxe Steve 2` for two copies to an online player. The console must specify a player.

The quantity multiplies the saved stack: saving 8 items and giving 2 copies produces 16 items.

## Item references in rewards

Put this fragment at the task's reward path:

```yaml
rewards:
  pickaxe:
    type: item
    item:
      material: tutorial_pickaxe
      amount: 1
```

Here `material` is a saved-item ID rather than a vanilla material name. Verify direct item delivery before testing a newly accepted task's reward.

## ItemFormat export

`/uq generateitemformat` exports the main-hand item to `plugins/UltimateQuest/generated-item-format.yml`. It does not register an item in `items/` and overwrites the previous export. Copy fields into a reward's `item` or a saved item's `base-item` to use them.

The commands respectively require `ultimatequest.saveitem`, `ultimatequest.givesaveitem`, and `ultimatequest.generateitemformat`. See [commands and permissions](../info/commands-permissions.md).
