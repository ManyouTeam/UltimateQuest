# 🧟 Identity and state rules

This page documents MatchEntityFormat rules for Bukkit entity types, names, health, tags, PersistentDataContainer values, ranged classification, monsters, and animals.

## Rules on this page

- `entity-types`
- `entity-contains-name`
- `entity-health`
- `entity-tag`
- `entity-pdc`
- `ranged`
- `monster`
- `animal`

MatchEntityFormat accepts living entities. Rules at the same level use logical AND; list values inside one rule normally use OR.

## `entity-types`

Compares Bukkit entity type names case-insensitively.

```yaml
entity-types:
- ZOMBIE
- HUSK
```

The matcher accepts LivingEntity, not arbitrary projectiles or item entities.

## `entity-contains-name`

Matches any configured substring against the platform-resolved entity name, preferring the custom name and falling back to the normal entity name.

```yaml
entity-contains-name:
- Guard
```

Use stable IDs/PDC instead of localized display text when possible.

## `entity-health`

Requires current absolute health >= configured value.

```yaml
entity-health: 10
```

Health points are not percentages. Combine with entity-types to narrow the target.

## `entity-tag`

Tests entity-type registry tags; monster is a special Monster-interface entry.

```yaml
entity-tag:
- minecraft:skeletons
```

Tag availability depends on the current server registry.

## `entity-pdc`

Tests supported stored string/numeric PDC values; any matching entry succeeds.

```yaml
entity-pdc:
  example:rank: '>=10'
```

Numeric rules support inclusive min~max, >=, <=, >, <, =, or a number. String matching supports * wildcards. The stored key and type must match the writer plugin. Invalid numeric syntax fails.

## `ranged`

Checks whether the main hand is a bow, crossbow, or trident.

```yaml
ranged: true
```

False requests the opposite. This is not a classification of all ranged AI or spells.

## `monster`

Checks the Monster interface.

```yaml
monster: true
```

False inverts this interface test.

## `animal`

Checks the Animals interface.

```yaml
animal: true
```

It does not require tame ownership, adult age, or breedability.

## Combining identity rules

```yaml
match-entity:
  monster: true
  entity-types:
    - SKELETON
    - STRAY
  ranged: true
```

The entity must satisfy all three rules.

To allow either a named Zombie or a named Skeleton, use `any`:

```yaml
match-entity:
  any:
    '1':
      entity-types:
        - ZOMBIE
      entity-contains-name:
        - Elite
    '2':
      entity-types:
        - SKELETON
      entity-contains-name:
        - Archer
```

## Example: reject player-like entities

```yaml
match-entity:
  monster: true
  not:
    entity-types:
      - PLAYER
      - ARMOR_STAND
```

MatchEntityFormat is intended for living entities; non-living types may already fail before individual rules run.

## Common mistakes

- using names where a stable tag/PDC/integration ID exists;
- confusing current health and maximum health;
- expecting monster/animal categories to match a custom semantic category exactly;
- assuming ranged means the entity currently holds a bow;
- matching non-living entities with MatchEntityFormat;
- using an entity type enum that does not exist on the target version;
- assuming PDC values are automatically converted between data types.

See [Equipment and Integration Rules](equipment-integrations.md) and [Logical Rules](logic.md).
