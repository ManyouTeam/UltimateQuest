# 🛡️ Equipment and integration rules

This page documents MatchEntityFormat rules for equipment and optional mob-plugin integrations.

## Rules on this page

- `equip`
- `mythicmobs`
- `levelled-mobs`

The integration rules are registered only when their plugins and compatible hooks are available while UltimateQuest enables.

## `equip`

Uses nested MatchItemFormat per equipment slot.

```yaml
equip:
  main-hand:
    material:
    - BOW
    - CROSSBOW
  helmet:
    material:
    - IRON_HELMET
```

Any configured matching slot passes equip. Sibling entity rules remain AND. Slot keys are main-hand, off-hand, helmet, chestplate, leggings, boots. Do not assume all configured slots are required.

## Matching an empty slot

An absent item is usually represented by air/empty `ItemStack` behavior. Use the MatchItemFormat form supported by the equipment matcher rather than assuming `material: AIR` always means an empty slot.

When an empty-slot condition matters, test it separately with the exact entity type and server version.

## Equipment example: ranged skeleton

```yaml
match-entity:
  entity-types:
    - SKELETON
    - STRAY
  equip:
    main-hand:
      material:
        - BOW
        - CROSSBOW
```

This is stricter than `ranged: true`: it checks the actual main-hand item.

## Equipment example: enchanted weapon

```yaml
match-entity:
  equip:
    main-hand:
      material-tag:
        - minecraft:swords
      has-enchants:
        - minecraft:sharpness
```

All MatchItemFormat rules can be nested under a slot, including logical groups and optional provider/NBT rules.

## `mythicmobs`

Matches internal MythicMobs IDs when the hook was registered at startup.

```yaml
mythicmobs:
- SkeletonKing
```

Use internal IDs, not display names. Unknown optional rule keys are ignored when the integration is absent.

## `levelled-mobs`

LevelledMobs level filtering is not currently available reliably. Do not use this key to restrict task rewards. If you need a level requirement, use a verified `entity-pdc` filter instead.

## Optional rule registration

If MythicMobs or LevelledMobs is absent or incompatible, the corresponding MatchEntityFormat rule may not be registered.

Unknown keys are ignored by MatchEntityFormat. This means an unavailable optional rule can make a matcher broader than intended.

{% hint style="warning" %}
Always confirm startup logs show the required integration hook before relying on `mythicmobs` or `levelled-mobs` for security, rewards, or destructive abilities.
{% endhint %}

## Safe integration fallback

When a task depends on a mob integration, combine the integration rule with another stable restriction where practical:

```yaml
match-entity:
  entity-types:
    - WITHER_SKELETON
  mythicmobs:
    - InfernalGuard
```

This does not replace the hook check, but it prevents the matcher from becoming completely unrestricted if the optional key is ignored.

Disable the affected task if its required integration plugin is missing.

## Custom-item equipment

Nested MatchItemFormat can use provider IDs:

```yaml
match-entity:
  equip:
    main-hand:
      items:
        - namespace:boss_sword
```

The corresponding item-provider hook must also be registered. MythicMobs entity matching and MythicMobs item matching are separate integrations.

## Example: target an equipped MythicMob

```yaml
match-entity:
  mythicmobs:
    - SkeletonKing
  equip:
    main-hand:
      material:
        - BOW
    helmet:
      has-name: true
```

## Example: exclude armored targets

```yaml
match-entity:
  monster: true
  not:
    equip:
      chestplate:
        material-tag:
          - minecraft:chest_armor
```

Verify that the tag exists and that empty equipment behaves as expected.

## Common mistakes

- using display names instead of MythicMobs internal IDs;
- relying on an optional key without checking hook registration;
- assuming unknown integration keys fail closed;
- using Bukkit slot names when the configuration parser expects aliases;
- expecting `ranged: true` and bow equipment matching to be identical;
- confusing an entity-provider integration with an item-provider integration.

See [MatchItemFormat](../match-item-format/README.md), [Identity and State Rules](identity-state.md), and [Logical Rules](logic.md).
