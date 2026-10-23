# 🎁 Rewards

Rewards define what a player receives after completing content. Normal tasks, periodic assignments, scrolls, pass levels, and milestone stages share the same reward format, at different configuration paths.

## Fixed reward example

Replace `rewards` in the [normal task example](../tasks.md) with this fragment:

```yaml
rewards:
  food:
    type: item
    item:
      material: BREAD
      amount: 8
    display:
      name: 'Bread x8'
  thanks:
    type: message
    message: '&aThanks, {player}!'
```

`food` and `thanks` are entry IDs you choose. Both run on claim; they are not alternatives. `type` selects the reward behavior. `display` changes preview text, not the quantity issued.

## Reward configuration paths

| System | Configuration path |
| --- | --- |
| Normal task or scroll | Root `rewards`. |
| Periodic assignment | `task.rewards`, issued separately for each generated assignment. |
| Battle pass | `levels.<level>.free` or `.premium`. |
| Milestone | `stages.<stage>.rewards`. |

| `type` | Purpose | Guide |
| --- | --- | --- |
| `item` | Give a vanilla or supported custom item. | [Items, commands, messages](items-commands.md) |
| `console_command` | Run a console command. | [Items, commands, messages](items-commands.md) |
| `message` | Send the claiming player a message. | [Items, commands, messages](items-commands.md) |
| `battlepass-experience` | Add points to a specified pass. | [Progression rewards](progression.md) |
| `random` | Draw one or more weighted reward groups. | [Random rewards](random.md) |

Only these types are accepted as rewards. Sounds and titles are menu [actions](../actions/README.md); `type: sound` is not a valid reward entry.

## Reward resolution and persistence

Selected reward keys are saved so that reopening a menu does not draw them again. Player records do not store copies of item definitions, commands, or display text. Repeated and nested draws retain their full selection paths.

| System | When random results are determined |
| --- | --- |
| Normal tasks | On acceptance. |
| Periodic assignments | When the period's assignment is created. |
| Scrolls | Definitions are saved on issue; rewards are drawn on first activation. |
| Pass levels and milestone stages | On stage unlock. |

Reconnecting and reloading do not redraw these results. Saved keys resolve against the loaded reward configuration, so changing an entry's item, amount, command, or display text takes effect when that configuration is loaded. Keep selected entry IDs stable: deleting one prevents reward delivery instead of drawing a replacement or skipping it. Old full reward snapshot records are not supported. Reuse definitions through [reward pools or the reward library](pools.md), which differ from objective `pools/`.

## Interrupted claims

Claiming saves an in-progress state before issuing rewards, then saves the final result. An interrupted write or payout can leave `CLAIMING` or `REVIEW`. Administrators should check issued items and executed commands before resolving the record.

External commands and issued items cannot be rolled back with a database operation. Making a stage claimable again can pay twice; it is not a general repair action.
