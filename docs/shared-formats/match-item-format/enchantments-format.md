# ✨ Enchantment and ItemFormat Rules

This page documents MatchItemFormat rules for direct enchantments, stored enchantments, enchantment levels/counts, enchantability, and serialized ItemFormat comparison.

## Rules on this page

* `has-enchants`
* `has-stored-enchants`
* `contains-enchants`
* `contains-enchants-amount`
* `enchantable`
* `item-format`

## Direct and stored enchantments

Minecraft stores direct item enchantments separately from enchantments stored in an enchanted book. Choose the rule that matches the component being inspected.

## `has-enchants`

Any listed direct enchantment matches; * checks for any direct enchantment.

```yaml
has-enchants:
- minecraft:efficiency
```

Use has-stored-enchants for enchanted-book storage rather than direct metadata.

## `has-stored-enchants`

Matches an enchanted book containing any listed stored enchantment.

```yaml
has-stored-enchants:
- minecraft:mending
```

A normal enchanted tool does not satisfy stored-enchantment checks.

## `contains-enchants`

Reads stored levels on enchanted books and direct levels on other items.

```yaml
contains-enchants:
  minecraft:efficiency:
  - 3
  - 4
  - 5
```

A scalar uses strict >, while a list uses exact membership. To test multiple enchantments independently, use a separate item condition for each requirement.

## `contains-enchants-amount`

Counts distinct enchantments, not summed levels.

```yaml
contains-enchants-amount: 2
```

A scalar is >=; an integer list is exact membership. Books use the stored-enchantment count.

## `enchantable`

Checks whether the item can receive any enchantment in the list.

```yaml
enchantable:
- minecraft:efficiency
```

This does not prove an external enchantment effect is active, nor does UltimateQuest register custom enchantments.

## `item-format`

Compares the item against the ItemFormat fields you specify.

```yaml
item-format:
  material: DIAMOND_PICKAXE
item-format-settings:
  require-same-key: false
  ignore-key:
  - lore
```

amount is always ignored. ignore-key excludes an exact field and its descendants. require-same-key also requires every nonignored exported key to be present in the comparison. String comparisons permit case-insensitive equality. Export a real item to learn exact fields.

## Example: a high-level enchantment

```yaml
match-item:
  material-tag:
    - minecraft:pickaxes
  contains-enchants:
    minecraft:efficiency: 3
```

## Example: Protection or Fire Protection

```yaml
match-item:
  any:
    '1':
      has-enchants:
        - minecraft:protection
    '2':
      has-enchants:
        - minecraft:fire_protection
```

## Example: enchanted-book filter

```yaml
match-item:
  material:
    - ENCHANTED_BOOK
  has-stored-enchants:
    - minecraft:sharpness
```

## Example: flexible ItemFormat comparison

```yaml
match-item:
  item-format:
    material: NETHERITE_SWORD
  not:
    contains-lore:
      - Disabled
```

## Common mistakes

* using `has-enchants` for an enchanted-book stored entry;
* using display names instead of namespaced enchantment keys;
* confusing item enchantability with support for a specific enchantment;
* changing a custom enchantment key and expecting old items to match;
* over-constraining `item-format` with unstable metadata;

See [Item Conditions](../../configs/conditions/items.md), [ItemFormat™](../itemformat-tm.md), and [Logical and NBT Rules](logic-nbt.md).
