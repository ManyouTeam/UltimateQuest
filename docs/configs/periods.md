# ⏰ Periodic tasks

Periodic tasks provide daily, weekly, or monthly assignments. Players receive them automatically and use `/uq period` to view progress and claim rewards. A new batch is generated for each period.

## Example: daily mining assignment

Create `plugins/UltimateQuest/periods/tutorial_daily.yml`:

```yaml
enabled: true
name: 'Daily mining'
reset-mode: CRON
reset-time: '0 0 0 * * ?'
claim-grace-seconds: 3600
source:
  name: 'Break stone'
  trigger: block_break
  amount: 32
  conditions:
    stone:
      type: block_type
      types: [STONE]
task:
  auto-claim: false
  rewards:
    bread:
      type: item
      item:
        material: BREAD
        amount: 8
```

Run `/uq reload`, then find “Daily mining” in `/uq period`. Break 32 natural stone blocks during the period to claim 8 bread. This example needs no objective pool and generates one assignment per day.

## Schedule and assignment settings

Root `source` selects the objectives. Write a trigger directly, reference an [objective template](objectives.md) with `quest`, or draw from an [objective pool](pools.md) with `pool`.

`task` supplies each generated assignment's description, categories, rewards, and automatic claiming settings. Put rewards in `task.rewards` and categories in `task.categories`, rather than at the file root.

For several random assignments, create the candidates as described in the pool tutorial, then replace `source` with `pool` and `count`. Each drawn objective becomes a separate assignment with its own `task.rewards`. Drawing three objectives with an 8-bread reward allows a total of 24 bread if all three are completed.

## Reset schedules

Schedules use the server Java process's system timezone. Confirm that timezone instead of assuming it matches the player's location.

| `reset-mode` | Example `reset-time` | Meaning |
| --- | --- | --- |
| `CRON` | `'0 0 0 * * ?'` | Midnight each day, using Quartz Cron with seconds. |
| `CRON` | `'0 0 0 ? * MON'` | Midnight each Monday. |
| `CRON` | `'0 0 0 1 * ?'` | Midnight on the first of each month. |
| `TIMED` | `'00:00:00;;12:00:00'` | Listed local times each day, separated by `;;`. |
| `TIMER` | `'1:00:00:00'` | A one-day interval in `[days:]hours:minutes:seconds` format; must be positive. |
| `CUSTOM` | `'2026-12-01 00:00:00'` | Fixed end time; `reset-anchor` supplies the start. |

`reset-anchor` defaults to `'1970-01-01 00:00:00'`. Automatic battle pass rotation uses the same schedule format.

Optional `starts-at` and `ends-at` limit the overall activity window. Use ISO-8601 instants with a timezone, such as `'2026-11-01T00:00:00Z'`; these differ from the local date format in the table. `paused: true` pauses the activity.

## Claiming after a reset

Old assignments stop gaining progress when their period ends. `claim-grace-seconds: 3600` allows one extra hour for claiming completed assignments, not for finishing incomplete ones.

The plugin checks [task slots](task-limits.md) before assigning a whole batch. If there is insufficient capacity, it keeps existing tasks and retries later instead of assigning a partial batch. Retries keep the previously determined draw.

## Edit and test

Reloading does not replace the requirements or rewards of generated assignments. Check a new batch after editing. For schedule testing, use a separate test definition with a short interval, then restore the intended schedule.
