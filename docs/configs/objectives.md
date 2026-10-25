# 🎯 Objectives and templates

An objective describes an activity and how much of it is required. `trigger` selects the activity, `amount` sets the goal, and `conditions` decides which events count. For example, breaking 16 stone blocks needs a block-break trigger, an amount of 16, and a stone filter.

You can put these requirements directly in a task's `source`; see [your first task](tasks.md). Create a template when periodic assignments, scrolls, or other tasks should reuse them.

## Objective template configuration

Create `plugins/UltimateQuest/quests/tutorial_stone.yml`:

```yaml
enabled: true
name: 'Stone'
trigger: block_break
amount: 16
conditions:
  stone:
    type: block_type
    types: [STONE]
```

`quests/` holds objective templates. `tasks/` holds complete tasks players can accept. A template does not appear as a normal task or issue a task reward by itself.

Subfolders under `quests/` are loaded recursively, including `.yml` and `.yaml` files. Bundled templates are organized into `common/`, `daily/<category>/`, `weekly/<category>/`, and `monthly/<category>/`. The template ID is always its filename without the extension: `quests/monthly/farming/monthly_crops.yml` is still referenced as `monthly_crops`. IDs must be unique across all subfolders. Existing templates may also remain directly under `quests/`. Following ER's default-definition extraction policy, templates are installed on first startup; later startups preserve the administrator's layout without regenerating defaults in new locations.

Replace a normal task's `source` with:

```yaml
source:
  quest: tutorial_stone
  amount: 64
```

`quest: tutorial_stone` refers to the template filename. This task keeps the template's trigger and conditions but requires 64 blocks. Rewards still belong in the normal task's `rewards`. Save the template before running `/uq reload`.

## Fields and overrides

| Field | Default / requirement | Meaning |
| --- | --- | --- |
| `trigger` | Must resolve to a registered trigger | For example, `block_break` or `ultimatequest:block_break`. |
| `amount` | Positive integer for a normal objective | Progress required for completion. |
| `progress-feedback` | `true` | Set to `false` to suppress progress feedback when this objective advances; counting and completion messages are unaffected. |
| `count` | `events` | Counting unit; defaults to one per matching event. Supported values depend on the trigger; see [counting modes](counting.md). |
| `conditions` | Optional | Events that fail the conditions do not count. |
| `anti-abuse` | Default protections | See [anti-abuse](anti-abuse.md). |
| `name` | Generated text | Give complex objectives a clear display name. |
| `display` | Generated format | Used when no explicit `name` is set. |

Resolution copies the `source.quest` template, applies fields in `source`, then applies fields alongside `source`. Later values at the same path take precedence. Adding a condition does not automatically remove other template conditions; check the resulting combination.

The template must exist and be enabled. `source.quest` cannot reference a normal task in `tasks/`. Template categories are not inherited as task categories.

## Progress counting

Bundled movement templates default to `progress-feedback: false`. Normal tasks, periodic assignments, scrolls, and challenges inherit this switch when referencing these templates. Other objectives in mixed tasks can still display feedback. Override a template with `source.progress-feedback: true`; the whole task's feedback switch must also be enabled. Objective settings are saved in the acceptance or generation snapshot, so existing tasks need to be accepted or generated again to use new objective settings.

`count: events` counts occurrences. With a supported quantity mode, `amount` uses that unit instead. For example, `attack` with `count: damage` and `amount: 100` requires 100 points of final event damage.

`count: items` supports specified catches, block drops, kill drops, and trade outputs. A `match_item` condition with `item: COUNTED_ITEM` filters each output stack. See [counting modes](counting.md) for the full support table, fractional progress, and examples. Unsupported combinations are rejected during loading.

## Use in other task types

- [Periodic tasks](periods.md) and [scrolls](scrolls.md) can reference one template or draw several from an [objective pool](pools.md). `pool` and `count` are not general normal-task source fields.
- [Battle pass challenges](battlepasses.md) use normal objective amounts.
- [Milestones](milestones.md) use `value` for each event's contribution. Their thresholds belong in `stages`, rather than the ordinary objective `amount`.

The trigger determines which data conditions can inspect. Acceptance has no broken block or fishing catch, so do not put event-dependent conditions at the task root. See [event conditions](conditions/README.md).
