# 🔍 Match Entity Format

`MatchEntityFormat` is the shared format used whenever UltimateQuest tests a living entity.

Use it inside the `match` section of a `match_entity` [event condition](../../configs/conditions/entity-state.md), for example to count only kills of a particular mob.

```yaml
match-entity:
  monster: true
  entity-types:
    - ZOMBIE
    - SKELETON
  entity-health: 20
```

## Evaluation rules

A MatchEntityFormat section does not use a `type` field. Every recognized key enables one rule, and rules at the same level use logical **AND**. List entries inside one rule normally use **OR**.

## Missing and invalid input

* A missing MatchEntityFormat section matches everything.
* A missing entity does not match.
* The matcher accepts `LivingEntity`; use an event condition suited to the trigger for other kinds of targets.
* Unknown keys are ignored.
* `none: true` prevents a match; remove the key to enable matching again.

## Logical composition

Use `any` for alternatives:

```yaml
match-entity:
  any:
    '1':
      entity-types:
        - SKELETON
      ranged: true
    '2':
      mythicmobs:
        - SkeletonKing
```

Use `not` to reject nested matches:

```yaml
match-entity:
  monster: true
  not:
    entity-types:
      - CREEPER
```

## Equipment matching

`equip` contains a MatchItemFormat section for one or more equipment slots:

```yaml
match-entity:
  equip:
    main-hand:
      material-tag:
        - minecraft:swords
    helmet:
      material:
        - CARVED_PUMPKIN
```

## Built-in rules

| Rule key               | Purpose                                                                                     |
| ---------------------- | ------------------------------------------------------------------------------------------- |
| `entity-types`         | Matches Bukkit entity types.                                                                |
| `none`                 | Prevents a match.                                                                   |
| `entity-contains-name` | Matches text in the entity's custom/display name.                                           |
| `entity-health`        | Requires current health to be at least the configured value.                        |
| `entity-tag`           | Matches Minecraft entity-type tags.                                                       |
| `entity-pdc`           | Matches configured PersistentDataContainer values.                                          |
| `ranged`               | Checks for a bow, crossbow, or trident in the main hand. |
| `monster`              | Requires or rejects Bukkit `Monster`.                                                       |
| `animal`               | Requires or rejects Bukkit animal classification.                                           |
| `equip`                | Matches any configured equipment slot using nested MatchItemFormat sections.                                    |
| `mythicmobs`           | Matches MythicMobs internal IDs when the hook is loaded.                                    |
| `levelled-mobs`        | Matches LevelledMobs data when that plugin is loaded.                                       |
| `any`                  | Passes when one nested group passes.                                                        |
| `not`                  | Rejects a nested match.                                                                     |

## Optional rules

| Rule                        | Requirement                                                               |
| --------------------------- | ------------------------------------------------------------------------- |
| `mythicmobs`                | MythicMobs must be loaded before UltimateQuest registers match rules. |
| `levelled-mobs`             | LevelledMobs must be loaded.                                              |

An unavailable optional rule is not registered; its key is therefore ignored. Confirm hook registration in startup logs.

## Complete example

```yaml
match-entity:
  monster: true
  entity-health: 20
  any:
    '1':
      ranged: true
      equip:
        main-hand:
          material:
            - BOW
            - CROSSBOW
    '2':
      mythicmobs:
        - SkeletonKing
  not:
    entity-types:
      - CREEPER
```

## Related pages

* [MatchItemFormat](../match-item-format/)
* [Event Conditions](../../configs/conditions/README.md)
* [Objectives](../../configs/objectives.md)

## Using this format in a task

The `match-entity` sections below show the standalone format. In a task objective, put those rules inside the condition's `match` section. For example:

```yaml
source:
  trigger: kill
  amount: 10
  conditions:
    mob:
      type: match_entity
      target: TARGET
      match:
        entity-types:
          - ZOMBIE
```
