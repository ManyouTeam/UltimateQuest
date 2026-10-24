# 🌟 Battle passes

A battle pass connects challenges to level rewards. Completing challenges earns points. When the total reaches a level threshold, that level's rewards unlock. A pass can offer both free rewards and rewards requiring premium access.

Challenges are assigned automatically. Players select a pass in `/uq battlepass`, complete tasks on its challenge page, and claim rewards on its level page.

## Battle pass example

Create `plugins/UltimateQuest/battlepasses/tutorial_pass.yml`:

```yaml
enabled: true
name: 'Starter pass'
season: season_1
season-name: 'Season 1'
icon:
  material: EXPERIENCE_BOTTLE
cap-experience: true
premium-by-permission: true
premium-permission: ultimatequest.battlepass.tutorial_pass.premium
tasks:
  mining:
    name: 'Mine stone'
    points: 25
    group: daily
    reset: daily
    source:
      trigger: block_break
      amount: 16
      conditions:
        stone:
          type: block_type
          types: [STONE]
levels:
  '1':
    required: 25
    free:
      bread:
        type: item
        item: {material: BREAD, amount: 8}
    premium:
      emerald:
        type: item
        item: {material: EMERALD, amount: 2}
  '2':
    required: 50
    free:
      iron:
        type: item
        item: {material: IRON_INGOT, amount: 4}
```

Run `/uq reload` and open `/uq battlepass tutorial_pass`. Break 16 natural stone blocks to complete today's challenge and automatically earn 25 points. Claim the level-one bread. The challenge resets the next day; completing it again brings the total to 50 and unlocks level two.

To check level-two presentation immediately, an administrator can run `/uq battlepass addxp Steve tutorial_pass 25` for a test player. Also verify that completing the challenge itself awards points.

## Challenge configuration

Each `tasks.<challenge-id>` uses normal `source` or `objectives` syntax and requires positive-integer `points` for the completion award.

| Field | Default / requirement | Purpose |
| --- | --- | --- |
| `reset` | `season` | `daily`, `weekly`, or `season`. |
| `group`, `group-name` | Optional | Challenge browsing group and display name; set a valid `reset` explicitly for custom groups. |
| `unlock-week` | `1` | First season week in which the challenge is available. |
| `catch-up` | `true` | Allows completion in later weeks; `false` restricts it to the unlock week. |
| `premium` | `false` | Requires this pass's premium access when true. |
| `categories` | Empty list | Challenge categories, separate from the pass catalog categories. |

Points are settled automatically on challenge completion. Players do not manually claim the points as they would a normal task reward. Challenges use `battlepass-task` [slots](task-limits.md), are assigned in configuration order, and are filled in as slots become available.

## Level thresholds and rewards

Number `levels` consecutively from `'1'`. Each `required` is a strictly increasing cumulative point threshold. Level two in the example requires 50 total points, not another 50 after level one.

`free` and `premium` each contain [reward entries](rewards/README.md). Premium access determines eligibility for `premium` rewards; displaying that row does not grant access. Level rewards themselves do not occupy active task slots.

With `premium-by-permission: true`, the permission named by `premium-permission` grants access. Administrators can also persistently grant it with `/uq battlepass premium Steve tutorial_pass true`.

`auto-claim: true` automatically attempts eligible level claims. `cap-experience: true` caps points at the final threshold.

## Generate thresholds with a curve

For many levels, add this root section:

```yaml
leveling:
  count: 30
  base: 100
  increment: 10
  multiplier: 1.0
```

The additional cost of each level is `ceil((base + increment × (level - 1)) × multiplier^(level - 1))`. Costs are accumulated into thresholds: the first three here are 100, 210, and 330.

`count` is 1–10000, `base` is positive, `increment` is nonnegative, and `multiplier` is at least 1. Results must not overflow. Explicit `levels.<level>.required` values take precedence; thresholds must remain increasing. Configure rewards under `levels` as usual.

## Rotate seasons and claim old rewards

The example uses the fixed `season: season_1`. For calendar-month rotation, add:

```yaml
season-prefix: expedition
rotation:
  enabled: true
  reset-mode: CRON
  reset-time: '0 0 0 1 * ?'
claim-grace-seconds: 604800
```

Rotation uses the [period schedule format](periods.md) and server timezone. IDs are generated from the prefix and period start. First installation enters the current calendar period rather than starting a month-long season at installation time.

After an old season stops gaining points, unlocked rewards remain claimable for the configured grace period, seven days above. List saved seasons with `/uq battlepass history tutorial_pass`, then use `<pass-id>@<season-id>` to open one. Optional `starts-at` and `ends-at` further limit activity; `paused: true` pauses activity and claiming.

## Manage seasons and boosts

These commands require administrator access:

```text
/uq battlepass season pause tutorial_pass
/uq battlepass season resume tutorial_pass
/uq battlepass season end tutorial_pass
/uq battlepass season start tutorial_pass season_2 2592000
/uq battlepass season schedule tutorial_pass season_3 2026-11-01T00:00:00Z 2026-12-01T00:00:00Z
/uq battlepass season globalboost tutorial_pass 2 3600
```

Manual `start`, `schedule`, and `end` operations disable automatic rotation for that pass. Player, global, and permission boosts use the highest applicable multiplier; they do not multiply together. Configure permission boosts under `permission-boosters.<id>.permission` and `.multiplier`.

Definition edits do not redraw already unlocked random level rewards. See [commands and permissions](../info/commands-permissions.md) for all management commands.
