# 📊 Task limits

Task slots limit how much content a player can have active at once. Set separate caps for normal tasks, scrolls, periodic assignments, and pass challenges, with optional category caps.

## Configuration location

Edit the existing `task-limits` section in `plugins/UltimateQuest/config.yml`; do not add a duplicate root key. The bundled configuration is:

```yaml
task-limits:
  normal:
    total: 20
    categories:
      combat: 6
      gathering: 10
  scroll:
    total: 8
    categories:
      adventure: 4
      gathering: 4
  period:
    total: 12
    categories: {}
  battlepass-task:
    total: -1
    categories: {}
  categories:
    combat: -1
    gathering: -1
    uncategorized: -1
```

Normal tasks allow 20 total, including at most 6 combat and 10 gathering tasks. Category IDs must exist; these are bundled IDs. See [categories](categories.md) before using a custom ID.

Omitted limits or `-1` mean unlimited. `0` prevents additions. Lowering a cap does not delete tasks players already have.

## Type and category limits

`normal.total` caps that type's total. `normal.categories.combat` caps combat tasks of that type. Root `task-limits.categories.combat` caps combat tasks across all types.

A task in both combat and gathering uses both category caps but counts once toward the normal total. A scroll counts as one scroll slot regardless of objective count.

## Slot occupancy rules

| Content or state | Uses an active slot? |
| --- | --- |
| In-progress task | Yes. |
| Completed but unclaimed task | Yes; claim it to release the slot. |
| Claimed or expired task | No. |
| Scroll not currently carried | No active slot; its expiry timer may still run. |
| Pass or milestone reward list itself | No. Pass challenges use battlepass-task separately. |

## Assignment behavior at capacity

Task acceptance and scroll activation check capacity. Administrator `accept -b` does not bypass slot limits.

Periods check a whole batch. If it cannot fit, existing tasks remain and assignment retries with the same draw. Pass challenges are assigned in configuration order and filled in as capacity is released.

Reload after editing. Test rejection at the limit as well as acceptance after claiming releases a slot.
