# 📜 Quest scrolls

A scroll is a physical quest item. Carry it, finish its objectives, and redeem it for rewards. Scrolls are issued by command and gain progress only while carried in the inventory or offhand.

## Configuration and issue example

Scroll files support root `progress-feedback: false` to disable progress feedback, or a `progress-feedback` section to override global settings. Reloading applies these settings to existing scrolls. See [progress feedback](tasks.md#progress-feedback).

Create `plugins/UltimateQuest/scrolls/tutorial_scroll.yml`:

```yaml
enabled: true
name: 'Mining scroll'
description:
  - 'Carry this scroll and break 16 stone blocks.'
item:
  material: MAP
  name: '&eMining scroll'
icon:
  material: MAP
source:
  name: 'Break stone'
  trigger: block_break
  amount: 16
  conditions:
    stone:
      type: block_type
      types: [STONE]
duration-seconds: 86400
rarity: common
difficulty: easy
rewards:
  emerald:
    type: item
    item:
      material: EMERALD
      amount: 2
```

Run `/uq reload`, then have an administrator with `ultimatequest.admin` run `/uq scroll give Steve tutorial_scroll`. Replace Steve with an online test player. The console can issue scrolls too.

Carry the scroll in the inventory or offhand, break 16 natural stone blocks, then open `/uq scroll` to inspect and claim it. A successful claim consumes the corresponding scroll.

## Configuration fields

| Field | Requirement / default | Purpose |
| --- | --- | --- |
| `item` | Required | Physical item given to the player, using ItemFormat. |
| `icon` | Display setting | Scroll icon in the menu. |
| `source` | Provide this, fixed `tasks`, or both | Inline objective, template reference, or objective pool. |
| `tasks` | Optional | Fixed objectives, each using the ordinary objective/source format. |
| `duration-seconds` | `0` | Seconds after first activation; `0` means no expiry, and negative values are invalid. |
| `rarity`, `difficulty` | `common`, `normal` | Labels; they do not automatically scale requirements or rewards. |
| `rewards` | Optional | One reward set for completing all objectives. |

You can combine root `source` with fixed `tasks`. All become objectives of one scroll and must be completed. Root rewards are not issued separately for every objective.

## Random objectives and labels

Reference an [objective pool](pools.md) in `source` to create random scrolls. Drawn objectives are combined with the fixed ones. Avoid reusing a fixed objective ID as a pool candidate ID, because the generated objective can overwrite it.

For random rarity labels, add a positive-integer weight mapping under `rarities`; use `difficulties` for difficulty. When a mapping exists, optional labels in the issue command must be keys in that mapping. Without a mapping, the scalar `rarity` and `difficulty` values are used.

The full command is `/uq scroll give <online-player> <profile-id> [rarity] [difficulty]`. Labels identify the scroll. Configure different goals and rewards explicitly if you need different gameplay.

## Carrying, expiry, and saved content

- First successful activation checks [task slots](task-limits.md). One scroll occupies one slot regardless of its objective count.
- Removing it from the inventory and offhand stops progress. An activated expiry timer keeps running; storing it in a chest does not pause the timer.
- Issuing saves the objective and reward definitions. Random rewards are drawn on first activation and survive reconnects and reloads.
- A scroll is linked to a unique stored record. Copying its name and lore does not create a valid scroll. Back up [player and scroll records](storage.md) together.

After editing a profile, issue a new scroll to test it. Previously issued scrolls keep the definition saved when they were issued.
