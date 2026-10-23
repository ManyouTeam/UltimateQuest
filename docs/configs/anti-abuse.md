# 🛡️ Anti-abuse

Anti-abuse rules help prevent progress farming through repeated place-and-break activity or repeated kills of the same player. Configure them under an objective's `anti-abuse`; they are not a separate task type.

## Anti-abuse configuration paths

This objective fragment can be placed at a normal task's root. For multiple objectives, put it in the corresponding objective's `source`.

```yaml
source:
  trigger: block_break
  amount: 16
  anti-abuse:
    ignore-player-placed: true
    ignore-internal-block-breaks: true
    cooldown-ms: 0
    maximum-uses-per-minute: 0
```

Templates can use root `anti-abuse` directly. An event must pass the objective conditions and then anti-abuse checks before it adds progress.

## Available settings

| Field | Default behavior | Purpose |
| --- | --- | --- |
| `ignore-player-placed` | `true` for mining-related triggers; otherwise `false` | Excludes recorded player-placed blocks. |
| `ignore-internal-block-breaks` | `true` | Excludes events marked as internal block breaks. |
| `ignore-spawner-mobs` | `true` | Excludes mobs with the plugin's spawner tracking tag. |
| `cooldown-ms` | `0`, disabled | Minimum milliseconds between counts for this player and objective. |
| `maximum-uses-per-minute` | `0`, disabled | Maximum counts for this player within a fixed one-minute window. |
| `player-victim-cooldown-seconds` | `300` for `kill`; otherwise `0` | Interval before the same player victim can count again. |
| `target-cooldown-seconds` | Depends on target and the previous setting | Explicit repeat-target interval, overriding the previous setting. |

Mining-related triggers are `block_break`, `block_drop_item`, `block_damage`, and `block_break_progress_update`. Placement tasks still count placed blocks by default.

For example, `cooldown-ms: 1000` permits about one count per second for that objective. `maximum-uses-per-minute: 30` limits a fixed one-minute window, not a sliding window.

## Placement tracking and crop filters

The plugin only recognizes recorded placements and tagged mobs; it cannot reconstruct world history from before installation. Farming objectives generally need to allow player-grown crops and separately check maturity. Matching the crop material alone does not require maturity.

Anti-abuse does not replace game-mode or world conditions. Add those conditions separately. Cooldowns and usage windows are runtime state, cleared on player quit or plugin unload; they are not persistent daily caps.
