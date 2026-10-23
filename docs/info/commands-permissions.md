# ⌨️ Commands and permissions

The main command is `/ultimatequest`, with aliases `/uq` and `/quests`. With no arguments it opens the main menu. `<argument>` is required and `[argument]` is optional; omit the brackets when entering a command.

## Player commands

The default player permission is `ultimatequest.use`. Definition IDs are filenames without `.yml`; generated content may require a specific instance ID.

| Player command | Purpose |
| --- | --- |
| `/uq help` | Show help. |
| `/uq menu [menu]` | Open a menu; default `main`. |
| `/uq accept <task>` | Accept a normal task. |
| `/uq info <task-or-instance>` | Show objectives/rewards. |
| `/uq claim <task-or-instance>` | Claim a completed task. |
| `/uq abandon <task-or-instance>` | Abandon an eligible normal task. |
| `/uq period` / `/uq scroll` | View periodic tasks / carried scrolls. |
| `/uq battlepass [id]` / `/uq milestone [id]` | Open progression catalog/details. |
| `/uq battlepass claim <id> <level> [free\|premium]` | Claim a level; default track `free`. |
| `/uq milestone claim <id> <stage>` | Claim a milestone stage. |
| `/uq battlepass claimall <id>` | Claim eligible stage rewards. |
| `/uq battlepass history <id>` | List saved seasons. |
| `/uq battlepass challenges <id>` | Open challenges. |
| `/uq battlepass stats <id>` | Show progression stats. |

The shared handler also supports `claimall`, `history`, `challenges`, and `stats` under `milestone`. Select a saved season with `<id>@<season-id>`.

## Administration commands

```text
/uq reload
/uq accept <task> <online-player> [-b]
/uq scroll give <online-player> <profile> [rarity] [difficulty]
/uq battlepass addxp <player-or-UUID> <id> <amount>
/uq battlepass setxp <player-or-UUID> <id> <total>
/uq battlepass removexp <player-or-UUID> <id> <amount>
/uq battlepass setlevel <player-or-UUID> <id> <level>
/uq battlepass premium <player-or-UUID> <id> <true|false>
/uq battlepass boost <player-or-UUID> <id> <multiplier> <seconds>
/uq battlepass resolve <player-or-UUID> <id> <track/stage> <AVAILABLE|CLAIMED>
```

Assignment, scroll issue, and progression administration require `ultimatequest.admin`. Progression administration also applies to `milestone` and supports offline players with existing records. Point/level edits change the corresponding record rather than redistributing every challenge.

`resolve` handles stages in `CLAIMING` / `REVIEW`. Check actual payouts first; returning a stage to AVAILABLE can issue already paid rewards again. See [battle passes](../configs/battlepasses.md) for season commands.

## Console operations

Specify an online player when opening menus or operating on tasks from the console. Console commands have no leading `/`.

```text
uq menu [menu] <player> [-b]
uq accept <task> <player> [-b]
uq info <task-or-instance> <player>
uq claim <task-or-instance> <player>
uq abandon <task-or-instance> <player>
uq period <player> [-b]
uq scroll <player> [-b]
uq battlepass [id] <player> [-b]
uq battlepass challenges <id> <player> [-b]
uq battlepass claim <id> <level> [free|premium] <player>
uq battlepass claimall <id> <player>
uq milestone [id] <player> [-b]
```

Menu `-b` skips the menu's own permission and is retained during that navigation session. It does not skip disabled state, task conditions, or reward eligibility. Administrator `accept -b` skips prerequisites, acceptance conditions, and activity checks, while slot and duplicate checks remain. Players assigning to other players need administrator permission.

## Permission nodes

| Permission | Default | Purpose |
| --- | --- | --- |
| `ultimatequest.use` | Everyone | Player commands and bundled menus. |
| `ultimatequest.reload` | OP | Reload. |
| `ultimatequest.admin` | OP | Dispatch, scroll issuance, progression management. |
| `ultimatequest.saveitem` | OP | `/uq saveitem <id> [bukkit\|itemformat]`. |
| `ultimatequest.givesaveitem` | OP | `/uq givesaveitem <id> [player] [amount]`; console requires player. |
| `ultimatequest.generateitemformat` | OP | Export main-hand item. |
| `ultimatequest.battlepass.example.premium` | Nobody | Bundled pass premium access. |

Other menus and passes can define their own `permission` and `premium-permission`; they are not restricted to the bundled example.

## Item commands

`/uq saveitem <id> [bukkit|itemformat]` saves the main-hand item. `/uq givesaveitem <id> [player] [amount]` gives saved items and needs a player from console. `/uq generateitemformat` exports to `generated-item-format.yml`. See [saved items](../configs/custom-items.md).

## Usage example

`/uq accept tutorial_mining`, `/uq info tutorial_mining`, and `/uq claim tutorial_mining` accept, inspect, and claim the task from the [normal task tutorial](../configs/tasks.md). Create it before testing. Reload edits with `/uq reload`, which requires `ultimatequest.reload`.
