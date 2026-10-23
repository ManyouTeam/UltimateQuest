# 🧠 Logical and NBT rules

This page documents MatchItemFormat composition and optional legacy NBT matching.

## Rules on this page

- `none`
- `any`
- `not`
- `contains-nbt`
- `nbt-string`
- `nbt-byte`
- `nbt-int`
- `nbt-double`

The NBT rules are registered only when NBTAPI is available while UltimateQuest enables.

## Default AND behavior

Every recognized rule at the same level must pass:

```yaml
match-item:
  material-tag:
    - minecraft:swords
  has-name: true
  contains-name:
    - Legendary
```

The item must satisfy all three rule groups.

## `none`

Forces failure only when true.

```yaml
none: true
```

Unlike entity none, the item rule checks its boolean value.

## `any`

Supports flat OR or numbered AND groups.

```yaml
any:
  '1':
    material:
    - DIAMOND_SWORD
    has-name: true
  '2':
    material:
    - IRON_AXE
```

Child key 1 enables group mode. Empty any passes.

## `not`

Negates nested item matching.

```yaml
not:
  contains-lore:
  - Disabled
```

All other sibling rules must still pass.

## Nested logic

The following expression means:

```text
(sword OR axe) AND NOT disabled
```

```yaml
match-item:
  any:
    '1':
      material-tag:
        - minecraft:swords
    '2':
      material-tag:
        - minecraft:axes
  not:
    contains-lore:
      - Disabled
```

Keep nesting shallow when possible. Deep logic is harder to debug and can usually be replaced with reusable item IDs/tags.

## Optional NBT rules

Modern Minecraft item data increasingly uses data components rather than legacy arbitrary NBT. The rules below exist for compatibility through NBTAPI and should not be the first choice for new configuration.

{% hint style="warning" %}
If NBTAPI is not loaded when UltimateQuest registers match rules, these keys are unknown and therefore ignored. An ignored optional key can make a match broader than intended. Verify startup logs before relying on NBT rules.
{% endhint %}

## `contains-nbt`

Checks one of the listed legacy tag paths exists.

```yaml
contains-nbt:
- quest_marker
- custom;;marker
```

Parent compounds are separated by ;;. Requires NBTAPI at startup.

## `nbt-string`

Requires an exact string tag value.

```yaml
nbt-string:
- quest_marker;;mining
```

The final two segments are tag and expected value.

## `nbt-byte`

Checks a typed numeric NBT tag. Entries use `tag;;operator;;value`; parent compounds can be added before the tag with `;;`.

```yaml
nbt-byte:
  - custom;;bound;;==;;1
```

Supported operators: `>=`, `>`, `==`, `<`, `<=`. The stored NBT type must match the selected rule.

## `nbt-int`

Checks a typed numeric NBT tag. Entries use `tag;;operator;;value`; parent compounds can be added before the tag with `;;`.

```yaml
nbt-int:
  - custom;;tier;;>=;;3
```

Supported operators: `>=`, `>`, `==`, `<`, `<=`. The stored NBT type must match the selected rule.

## `nbt-double`

Checks a typed numeric NBT tag. Entries use `tag;;operator;;value`; parent compounds can be added before the tag with `;;`.

```yaml
nbt-double:
  - custom;;power;;>=;;1.5
```

Supported operators: `>=`, `>`, `==`, `<`, `<=`. The stored NBT type must match the selected rule.

## NBT versus PDC and provider IDs

Prefer these identifiers in order:

1. registered custom-item provider ID;
2. namespaced enchantment key;
3. stable material/tag and component metadata;
4. plugin-owned PersistentDataContainer/component rule where supported;
5. legacy NBT matching only when necessary.

Provider plugins may rewrite their internal NBT between versions while keeping the public item ID stable.

## Debugging logic

1. Test each nested group as the entire `match-item` section.
2. Confirm every rule key is registered.
3. Replace optional NBT checks with a material/provider-ID check temporarily.
4. Verify the actual NBT type, path, and value with a trusted inspection tool.
5. Add `not` only after the positive rule works.
6. Log or display the exact item used by the trigger; delayed/equipment events may provide a different item than expected.

## Common mistakes

- expecting top-level rules to use OR;
- placing several individual rule keys under `any` without grouping them correctly;
- accidentally negating a broader group than intended;
- relying on an NBT rule when NBTAPI was not registered;
- comparing an integer tag with `nbt-double` or vice versa;
- matching provider-private NBT instead of the public item ID;
- assuming unknown keys make the match fail.

See [Basic and Metadata Rules](basic-metadata.md) and [Enchantment and ItemFormat Rules](enchantments-format.md).
