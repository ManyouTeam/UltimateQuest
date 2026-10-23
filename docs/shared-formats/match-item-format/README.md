# 🔍 Match Item Format

`MatchItemFormat` selects the items that count toward a task, such as a caught fish, a consumed item, or a tool used by the player.

Use it inside the `match` section of a `match_item` or `catch_match_item` [event condition](../../configs/conditions/items.md), or inside an entity equipment filter.

```yaml
match-item:
  material-tag:
    - minecraft:swords
  has-enchants:
    - minecraft:sharpness
```

## Evaluation rules

A MatchItemFormat section does not use a `type` field. Every recognized key enables one rule, and rules at the same level use logical **AND**. Within list-based rules, entries normally use **OR**.

```yaml
match-item:
  material:
    - DIAMOND_SWORD
    - NETHERITE_SWORD
  has-name: true
  contains-name:
    - Legendary
```

The item must be either listed material, must have a custom name, and that name must contain `Legendary`.

## Missing and invalid input

* A missing MatchItemFormat section matches everything.
* A missing item or an item without usable `ItemMeta` does not match.
* Unrecognized keys are ignored because no matching rule is registered for them.
* `none: true` forces the section to fail.

## Logical composition

Use `any` for alternatives:

```yaml
match-item:
  any:
    '1':
      material-tag:
        - minecraft:swords
    '2':
      items:
        - mythic_sword
```

Use `not` to reject nested matches:

```yaml
match-item:
  material-tag:
    - minecraft:damageable
  not:
    contains-lore:
      - Disabled
```

## Built-in rules

| Rule key                   | Purpose                                                              |
| -------------------------- | -------------------------------------------------------------------- |
| `none`                     | Prevents a match.                                            |
| `items`                    | Matches configured provider/saved item IDs.                          |
| `material`                 | Matches Bukkit or namespaced materials.                              |
| `material-tag`             | Matches one of the configured Minecraft/item tags.                   |
| `rarity`                   | Matches item rarity on supported 1.20.5+ servers.                    |
| `has-name`                 | Requires or rejects a custom name.                                   |
| `contains-name`            | Matches text contained in the display name.                          |
| `has-lore`                 | Requires or rejects lore.                                            |
| `contains-lore`            | Matches text contained in lore.                                      |
| `has-enchants`             | Requires listed direct enchantments.                                 |
| `has-stored-enchants`      | Requires listed stored enchantments, such as enchanted-book entries. |
| `contains-enchants`        | Checks enchantment levels/ranges.                                    |
| `contains-enchants-amount` | Checks how many matching enchantments are present.                   |
| `enchantable`              | Checks whether the item is enchantable under the configured rule.    |
| `item-format`              | Matches serialized ItemFormat fields.                                |
| `any`                      | Passes when one nested group passes.                                 |
| `not`                      | Rejects a nested match.                                              |
| `contains-nbt`             | Legacy NBT containment check; requires NBTAPI.                       |
| `nbt-string`               | Legacy string NBT check; requires NBTAPI.                            |
| `nbt-byte`                 | Legacy byte NBT check; requires NBTAPI.                              |
| `nbt-int`                  | Legacy integer NBT check; requires NBTAPI.                           |
| `nbt-double`               | Legacy double NBT check; requires NBTAPI.                            |

## Optional rule availability

| Rules                                                             | Requirement                                                                   |
| ----------------------------------------------------------------- | ----------------------------------------------------------------------------- |
| `contains-nbt`, `nbt-string`, `nbt-byte`, `nbt-int`, `nbt-double` | NBTAPI must be loaded when UltimateQuest enables.                         |
| `rarity`                                                          | Server version must support modern item rarity.                               |
| External IDs in `items`                                           | Corresponding item-provider plugin must be installed and its hook registered. |

When an optional rule is not registered, its unknown key is ignored rather than treated as a failed match. Check startup logs before relying on optional matching.

## Complete example

```yaml
match-item:
  material-tag:
    - minecraft:swords
  has-name: true
  any:
    '1':
      contains-enchants:
        minecraft:sharpness: 3
    '2':
      items:
        - superior_sword
  not:
    contains-lore:
      - Disabled
```

## Related pages

* [MatchEntityFormat](../match-entity-format/)
* [Event Conditions](../../configs/conditions/README.md)
* [Objectives](../../configs/objectives.md)
* [ItemFormat™](../itemformat-tm.md)

## Using this format in a task

The `match-item` sections below show the standalone format. In a task objective, put those rules inside the condition's `match` section. For example:

```yaml
source:
  trigger: consume
  amount: 16
  conditions:
    food:
      type: match_item
      item: TRIGGER_ITEM
      match:
        material:
          - BREAD
```
