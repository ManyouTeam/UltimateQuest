# 📝 ItemFormat

ItemFormat builds reward items, scroll items, icons, saved base-items, and menu templates. Example:

```yaml
item:
  material: DIAMOND_PICKAXE
  amount: 1
  name: '&bSurvey pickaxe'
  lore: ['&7A reward from UltimateQuest']
  enchants:
    minecraft:efficiency: 3
  change-item:
    repair-damage: 50
```

## ⚙️ Fields

Use material for a vanilla material or saved item ID. Without material, hook-plugin/hook-item can select a provider item; item can contain a serialized item. amount is an integer or inclusive min~max range, default 1; it is not a general math-expression field. name (alias display), lore, enchants, stored-enchants, flags, custom-model-data, and supported component fields modify metadata. Component support depends on the server implementation.

## 🔧 Item changes and export

`change-item` applies the [item-change rules](change-item-rules.md) after building the item. Use `/uq generateitemformat` to inspect the current exporter or `/uq saveitem <id> itemformat` for reusable definitions. Matching uses a separate [MatchItemFormat](match-item-format/README.md); material is scalar for building but a list for matching.
