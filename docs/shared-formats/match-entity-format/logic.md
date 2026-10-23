# 🧠 Logical rules

This page documents MatchEntityFormat composition. Rules at the same level use logical AND; use `any` for alternatives and `not` for exclusions.

## Rules on this page

- `none`
- `any`
- `not`

## Default AND behavior

```yaml
match-entity:
  monster: true
  entity-types:
    - ZOMBIE
    - HUSK
  entity-health: 20
```

The entity must be a Bukkit `Monster`, must be either a Zombie or Husk, and must satisfy the health rule.

## `none`

Forces entity matching to fail when the key is present.

```yaml
none: true
```

To enable matching again, remove `none` rather than setting it to `false`.

## `any`

Flat alternatives use OR between recognized rules; numbered children use OR between complete AND groups.

```yaml
any:
  '1':
    entity-types:
    - ZOMBIE
    entity-health: 10
  '2':
    entity-types:
    - SKELETON
```

The presence of child key 1 selects grouped mode. Empty any passes. Arbitrary named children do not select group mode.

## `not`

Negates the combined nested match.

```yaml
not:
  entity-types:
  - PLAYER
```

The outer matcher still needs a valid living entity; not is not a missing-entity existence test.

## Combining AND, OR, and NOT

The following means:

```text
monster AND (zombie OR skeleton) AND NOT protected
```

```yaml
match-entity:
  monster: true
  any:
    '1':
      entity-types:
        - ZOMBIE
        - HUSK
        - DROWNED
    '2':
      entity-types:
        - SKELETON
        - STRAY
        - WITHER_SKELETON
  not:
    entity-contains-name:
      - Protected
```

## Multiple requirements in one alternative

```yaml
match-entity:
  any:
    '1':
      entity-types:
        - ZOMBIE
      equip:
        main-hand:
          material-tag:
            - minecraft:swords
    '2':
      entity-types:
        - SKELETON
      ranged: true
```

An alternative group can contain as many AND rules as necessary.

## Inverting a single rule

Use `not` around the smallest matcher possible:

```yaml
match-entity:
  not:
    animal: true
```

Avoid broad inversions that accidentally match entities with missing/unsupported data.

## Missing entities and living-entity restriction

A missing entity does not match. MatchEntityFormat is designed for `LivingEntity`; non-living entities should be handled by an condition or trigger that explicitly supports them.

Logical wrappers do not make a missing/non-living entity valid. For example, `not: { monster: true }` should not be treated as a safe way to match every non-monster object.

## Optional keys and fail-open behavior

Unknown MatchEntityFormat keys are ignored because no registered rule handles them. This matters for:

- `mythicmobs` without MythicMobs hook;
- `levelled-mobs` without LevelledMobs hook;
- platform-specific classification rules;
- misspelled keys.

A matcher containing only an ignored key can become effectively unrestricted.

For important filters:

1. verify startup hook registration;
2. include a stable built-in restriction where possible;
3. test a known matching and known non-matching entity;
4. disable the dependent task when the required integration is absent.

## Example: custom boss or high-health vanilla mob

```yaml
match-entity:
  any:
    '1':
      mythicmobs:
        - SkeletonKing
    '2':
      monster: true
      entity-health: 100
```

## Debugging nested matchers

1. Test each child as the entire `match-entity` section.
2. Remove `not` and verify the positive group first.
3. Confirm the selected target is a living entity.
4. Check exact entity type, tag, PDC, equipment, and integration ID.
5. Confirm optional rules were registered.
6. Add alternatives back one at a time.
7. Keep nesting shallow and use numbered keys for alternative groups.

## Common mistakes

- expecting top-level rule keys to use OR;
- placing several unrelated rule keys directly under `any` without complete child groups;
- negating a broader group than intended;
- assuming an unknown integration key fails closed;
- using `not` to match missing or non-living entities;
- forgetting that each alternative's internal rules use AND;

See [Identity and State Rules](identity-state.md) and [Equipment and Integration Rules](equipment-integrations.md).
