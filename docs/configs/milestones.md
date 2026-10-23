# 🏆 Milestones

Milestones reward long-term activity such as mining, fishing, or kills. Players do not accept them: qualifying activity automatically adds progress. Use `/uq milestone` to claim rewards for unlocked stages.

## Example: mining milestone

Create `plugins/UltimateQuest/milestones/tutorial_mining.yml`:

```yaml
enabled: true
name: 'Lifetime mining'
icon:
  material: IRON_PICKAXE
sources:
  stone:
    source:
      trigger: block_break
      conditions:
        stone:
          type: block_type
          types: [STONE]
    value: 1
stages:
  '1':
    required: 64
    rewards:
      emerald:
        type: item
        item: {material: EMERALD, amount: 2}
  '2':
    required: 256
    rewards:
      diamond:
        type: item
        item: {material: DIAMOND, amount: 1}
```

Run `/uq reload` and open `/uq milestone tutorial_mining`. Each qualifying natural stone block subsequently adds 1 progress. Players can claim 2 emeralds at 64 total progress and 1 diamond at 256.

The second threshold is 256 in total, not another 256 after stage one. Claiming an earlier reward does not spend accumulated progress.

## Progress sources and stage thresholds

`sources` decides which activities add progress. Each source can contain a trigger directly or reference an [objective template](objectives.md). `value` multiplies the counted quantity; it defaults to `1` and must be a positive integer.

`stages` sets the cumulative thresholds and rewards. Number stages consecutively from `'1'`. Every `required` must be a positive integer greater than the previous threshold. Put rewards in `stages.<number>.rewards`.

Do not use a template's ordinary `amount` as a milestone threshold. The milestone multiplies the counted quantity by `value` and uses `required` for unlocking stages.

## Multiple sources and optional settings

- Add several sources if needed. An event matching more than one source contributes through each of them.
- `count: events` contributes one `value` per match. Quantity modes multiply the measured quantity by `value`: 2.5 damage with `value: 2` contributes 5. See [counting modes](counting.md).
- `categories`, `description`, and `icon` control presentation. `auto-claim: true` automatically attempts eligible claims.
- Leave `season` unset for permanent accumulation. Set a valid season ID for a separate season's progress; old totals are not simply merged into the new season.
- `starts-at`, `ends-at`, `claim-grace-seconds`, and `paused` limit activity and claiming. See [normal tasks](tasks.md) for the time format.

The milestone itself does not occupy [active task slots](task-limits.md). Random stage rewards are determined on unlock and are not redrawn by reloads.

## Claiming and validation

Players can claim through the menu, use `/uq milestone claim tutorial_mining 1`, or claim all eligible stages with `/uq milestone claimall tutorial_mining`.

Check progress first, then both cumulative thresholds and their rewards. After changing stage settings, do not assume saved or unlocked rewards are replaced; see [saved rewards](rewards/README.md).
