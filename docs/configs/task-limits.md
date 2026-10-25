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
    total: 28
    categories: {}
  battlepass-task:
    total: -1
    categories: {}
  categories:
    combat: -1
    gathering: -1
    uncategorized: -1
  permission-limits: {}
```

Normal tasks allow 20 total, including at most 6 combat and 10 gathering tasks. Category IDs must exist; these are bundled IDs. See [categories](categories.md) before using a custom ID.

Omitted limits or `-1` mean unlimited. `0` prevents additions. Lowering a cap does not delete tasks players already have.

## Type and category limits

`normal.total` caps that type's total. `normal.categories.combat` caps combat tasks of that type. Root `task-limits.categories.combat` caps combat tasks across all types.

A task in both combat and gathering uses both category caps but counts once toward the normal total. A scroll counts as one scroll slot regardless of objective count.

## Increase slots by permission

Replace the existing `task-limits.permission-limits: {}` with the following section, alongside `normal`, `scroll`, and the other task types:

```yaml
permission-limits:
  vip:
    permission: ultimatequest.limits.vip
    normal:
      total: 30
      categories:
        combat: 10
    scroll:
      total: 12
  svip:
    permission: ultimatequest.limits.svip
    normal:
      total: 50
    scroll:
      total: 16
    period:
      total: 40
```

`vip` and `svip` are custom group IDs. Each group requires a nonblank `permission` node. Grant that node through your permissions plugin; it does not need a separate entry in the plugin permission declaration.

Each cap takes the highest value among the default and all matching groups. Values do not add together. `-1` means unlimited and wins over every finite value. In this example, the normal total is 20 by default, 30 for VIP, and 50 for SVIP. Players with both nodes get 50 normal slots, a normal combat cap of 10, and 16 scroll slots.

Each group supports `total` and `categories` under `normal`, `scroll`, `period`, and `battlepass-task`, plus root `categories` within the group for caps across all task types. Omitted entries retain the default cap rather than becoming unlimited. Permission values can only increase slots. A default of `-1`, or an omitted default, remains unlimited; set a finite default first to offer different permission tiers.

Type totals, category caps within a type, and category caps across all types must all be satisfied. Permissions are read on every capacity check. Losing a permission does not delete existing tasks; periods wait for their next assignment check and pass challenges for their next refill check. OP and wildcard matching follow the server's `hasPermission` result.

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
