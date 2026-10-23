# 📦 Item, command, and message rewards

These types give items, call server commands, and send text. All examples are reward fragments. Place them at the appropriate task or stage reward path described in [rewards](README.md).

## `item`: give items

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

The first entry gives 8 bread. `item` uses [ItemFormat](../../shared-formats/itemformat-tm.md) and must create a non-air item with a positive amount. Inventory overflow is dropped.

`item.amount` accepts an integer or a supported `minimum~maximum` range, not an arbitrary mathematical formula. See [custom items](../custom-items.md) to reference saved items.

## `console_command`: execute as console

```yaml
rewards:
  announcement:
    type: console_command
    command: 'say {player} completed {quest}'
```

The command must be nonempty and have no leading `/`. Available variables are `{player}` for the player name, `{uuid}` for the UUID, and `{quest}` for the task identifier. Check that commands from economy or permission plugins support console execution before using them.

If issuing a scroll with `/uq scroll give`, its profile must exist. See [scrolls](../scrolls.md) for the full syntax.

## `message`: send player feedback

The `thanks` entry in the first example is a message reward. `message` must be a string. See [colors](../../features/color-code.md) and [language management](../../features/advanced-language-management.md) for supported formatting.

A message reward is not an arbitrary action list. Configure menu actions for click-time sounds or titles.

## Check the payout

Test with a newly accepted simple task, checking items, console effects, and chat separately. If a command fails, earlier rewards may already have been issued. Check actual results before retrying.
