# 🔌 Commands and world actions

These actions run on menu clicks. Each example is an action fragment for `items.<symbol>.actions` or `contents.<symbol>.actions`; actions are not task reward types. See [action basics](README.md) for placement.

## `console_command`

Runs a console command without a leading `/`. Include `{player}` when the command needs a target player.

| Field | Default | Description |
| --- | --- | --- |
| `command` | Required | Command without leading slash. |

```yaml
type: console_command
command: uq info miner {player}
```

## `player_command`

Runs the command as the clicking player, using that player’s permissions. The target plugin still checks its command permissions.

| Field | Default | Description |
| --- | --- | --- |
| `command` | Required | Command without leading slash. |

```yaml
type: player_command
command: uq info miner
```

## `op_command`

Runs the player command through the temporary-OP path. Use it for fixed administrator-authored actions that require that execution mode.

| Field | Default | Description |
| --- | --- | --- |
| `command` | Required | Command without leading slash. |

```yaml
type: op_command
command: uq info miner
```

## `command`

This compatibility menu form runs as console by default; `as-console: false` runs as the player. Explicit console_command or player_command makes execution identity clearer in new configurations.

| Field | Default | Description |
| --- | --- | --- |
| `command` | Required | Command without leading slash. |
| `as-console` | true | False executes as the player. |

```yaml
type: command
command: uq info miner {player}
as-console: true
```

## `teleport`

Teleports the clicking player to coordinates in a loaded world. Supply world, x, y, and z. Optional yaw and pitch set facing; omitted facing uses the current player direction.

| Field | Default | Description |
| --- | --- | --- |
| `world` | Required | Loaded world name. |
| `x / y / z` | Required | Target coordinates. |
| `yaw / pitch` | Current player rotation | Target orientation. |

```yaml
type: teleport
world: world
x: 0.5
y: 80
z: 0.5
```

## `entity_spawn`

Spawns a vanilla entity at the player, or at a fully specified custom location. `entity` is a Bukkit entity type, not a MythicMobs ID.

| Field | Default | Description |
| --- | --- | --- |
| `entity` | Required | Bukkit entity type. |
| `world / x / y / z` | Player location | Supply all location fields when using a custom location. |

```yaml
type: entity_spawn
entity: VILLAGER
```

## Configuration validation

Run `/uq reload`, reopen the menu, and click the button. Task actions need a clicked task or selected pass/milestone; naming a static button after a task does not establish that association. Navigation retains the current menu-permission bypass, while task and reward checks still apply.
