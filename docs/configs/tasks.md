# 📄 Normal tasks

Use a normal task when players should accept a goal, complete it, and claim a reward. A task can have one objective or several. It ends after a successful claim. For automatically refreshing content, use [periodic tasks](periods.md).

## Example: stone-mining task

Create `plugins/UltimateQuest/tasks/tutorial_mining.yml` with this complete configuration. It does not depend on other tasks, templates, categories, or language keys.

```yaml
enabled: true
name: '&aStone mining'
description:
  - 'Break 16 natural stone blocks to earn 8 bread.'
icon:
  material: STONE_PICKAXE
source:
  name: 'Break stone'
  trigger: block_break
  amount: 16
  conditions:
    stone:
      type: block_type
      types: [STONE]
rewards:
  bread:
    type: item
    item:
      material: BREAD
      amount: 8
```

The filename gives the task its ID: `tutorial_mining`, without `.yml` in commands. The `name` field is the display name and can be changed freely.

1. Run `/uq reload` and check for configuration errors.
2. Run `/uq accept tutorial_mining`, or accept it through `/uq`.
3. Break 16 natural stone blocks. Default anti-abuse rules exclude player-placed blocks recorded by the plugin.
4. Check `/uq info tutorial_mining`, then run `/uq claim tutorial_mining` once it is complete.

Mining before acceptance is not counted. This example only matches `STONE`; deepslate and other blocks do not contribute.

## Configuration fields

| Field | Required / default | Purpose |
| --- | --- | --- |
| `enabled` | `true` | Set to `false` to disable the definition. |
| `name`, `description`, `icon` | Display settings | Name, description list, and menu icon. |
| `source` | Use this or `objectives` | Shorthand for one objective, including its trigger, amount, and conditions. |
| `rewards` | Optional | Rewards issued on claim; see [rewards](rewards/README.md). |
| `auto-claim` | `false` | Automatically attempt a claim when complete. |
| `allow-abandon` | `true` | Normal tasks only. Set to `false` to prevent abandonment through commands and menus. |
| `hide-in-menu` | `false` | Normal tasks only. Set to `true` to hide every state from menus. Commands can still accept, inspect, and claim the task. |
| `auto-hide` | `false` | Normal tasks only. Hide unaccepted tasks while acceptance requirements are unmet; show them when eligible. Accepted tasks remain visible. |
| `categories` | Empty list | Category IDs; see [categories](categories.md). |
| `prerequisites` | Empty list | Normal tasks whose rewards must be claimed first. |
| `accept-mode` | `player` | Use `admin` for administrator assignment only. |
| `listed` | `true` | `false` hides tasks that have not been assigned. |
| `conditions` | Optional | Root conditions check acceptance using available player data. |

`source.conditions` checks each matching event. Root `conditions` checks whether the player can accept the task. Use `target: PLAYER` when checking the accepting player's state.

`hide-in-menu` takes priority over `auto-hide`. Automatic hiding uses the menu's existing acceptance check, including root conditions, prerequisites, activity windows, and acceptance mode, without checking task slots. Objective conditions do not hide accepted tasks. `listed: false` only hides unaccepted tasks; use `hide-in-menu: true` to hide every state.

For example, add these root fields to prevent abandonment and automatically hide a normal task until its acceptance requirements are met:

```yaml
allow-abandon: false
hide-in-menu: false
auto-hide: true
```

## Multiple objectives

Replace `source` in the full example with the following `objectives` section. Keep its name, icon, rewards, and other fields. Players must now break 16 stone blocks and kill 5 zombies.

```yaml
objectives:
  mining:
    source:
      trigger: block_break
      amount: 16
      conditions:
        stone:
          type: block_type
          types: [STONE]
  hunting:
    source:
      trigger: kill
      amount: 5
      conditions:
        zombie:
          type: match_entity
          target: TARGET
          match:
            entity-types: [ZOMBIE]
```

`mining` and `hunting` identify objectives inside this task; they are not filenames. To reuse the same requirements elsewhere, use [objective templates](objectives.md).

## Prerequisites, assignment, and activity windows

Adding `prerequisites: [tutorial_mining]` requires the player to claim that task's reward first. Completion alone does not unlock the next task. Referenced tasks must exist, and prerequisites cannot form a cycle.

Administrators can assign tasks with `/uq accept <task-id> <online-player>`, including hidden and admin-only tasks. Adding `-b` skips prerequisites, acceptance conditions, and activity checks. Slot limits and duplicate checks still apply.

For an event, set root `starts-at` and `ends-at` to ISO-8601 instants with a timezone, such as `'2026-11-01T00:00:00Z'`. `claim-grace-seconds` defaults to `0` and extends claiming only, not the time for earning progress. `paused: true` pauses activity and claiming.

## Configuration changes and existing tasks

Acceptance saves the current objectives and resolved rewards. Reloading does not rewrite them. Test with a player who has not accepted the task, or abandon and accept it again where abandonment is allowed.

Completed tasks still use [task slots](task-limits.md) until claimed. `/uq abandon <task-id>` discards the progress of an eligible normal task.

`allow-abandon`, `hide-in-menu`, and `auto-hide` use the current configuration after `/uq reload`; players do not need to accept the task again. The `quests.accepted` and `quests.abandoned` language messages support `{quest}` for the corresponding task's display name.
