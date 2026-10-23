# 📂 Actions

Actions define what a menu click does: refresh a list, open another menu, accept a task, or play a sound. Rewards define task payouts and support a different set of types.

## Multiple-action example

Merge this fragment into menu `items` and place `R` in the `layout`. See [menu configuration](../menus.md) for a complete menu.

```yaml
items:
  R:
    item:
      material: COMPASS
      name: '&eRefresh'
    actions:
      update:
        type: refresh
      feedback:
        type: sound
        sound: minecraft:ui.button.click
```

The click refreshes first and then plays a sound. `update` and `feedback` are entry names. Multiple actions run in YAML order. For one action, write `type` directly beneath `actions`.

## Static buttons and task entries

- `items.<symbol>.actions` belongs to fixed buttons such as close or pagination.
- `contents.<symbol>.actions` belongs to dynamic entries such as a particular task in a list.
- `quest`, `quest_info`, and `abandon_quest` need the clicked task. Adding an arbitrary task-ID field does not redirect them to another task.
- `claim_all` and `open_challenges` need the corresponding progression object selected in the current menu.

## Click type configuration

Put this fragment inside a dynamic task section such as `contents.Q`:

```yaml
click-actions:
  LEFT:
    type: quest
  RIGHT:
    type: abandon_quest
    confirm: true
    confirm-seconds: 5
```

`click-actions.LEFT` replaces generic `actions` for left clicks; right clicks work the same way. Other server click types such as `SHIFT_LEFT` are accepted. An explicitly empty matching click node prevents fallback to generic actions.

Type names normalize case and hyphens; the documentation consistently uses lowercase underscores. Actions do not universally support their own `conditions`, `chance`, or `cooldown` fields. Use the `chance` wrapper for random execution and conditions only at supported configuration positions.

## Action type index

| Type | Reference |
| --- | --- |
| `message` | [message](fundamentals.md#message) |
| `action_bar` | [action_bar](fundamentals.md#action_bar) |
| `announcement` | [announcement](fundamentals.md#announcement) |
| `title` | [title](fundamentals.md#title) |
| `sound` | [sound](fundamentals.md#sound) |
| `particle` | [particle](fundamentals.md#particle) |
| `effect` | [effect](fundamentals.md#effect) |
| `console_command` | [console_command](commands-world.md#console_command) |
| `player_command` | [player_command](commands-world.md#player_command) |
| `op_command` | [op_command](commands-world.md#op_command) |
| `command` | [command](commands-world.md#command) |
| `teleport` | [teleport](commands-world.md#teleport) |
| `entity_spawn` | [entity_spawn](commands-world.md#entity_spawn) |
| `chance` | [chance](orchestration.md#chance) |
| `any` | [any](orchestration.md#any) |
| `delay` | [delay](orchestration.md#delay) |
| `previous_page` | [previous_page](navigation.md#previous_page) |
| `next_page` | [next_page](navigation.md#next_page) |
| `refresh` | [refresh](navigation.md#refresh) |
| `close` | [close](navigation.md#close) |
| `open_menu` | [open_menu](navigation.md#open_menu) |
| `open_categories` | [open_categories](navigation.md#open_categories) |
| `cycle_view` | [cycle_view](navigation.md#cycle_view) |
| `cycle_challenge_group` | [cycle_challenge_group](navigation.md#cycle_challenge_group) |
| `cycle_period_group` | [cycle_period_group](navigation.md#cycle_period_group) |
| `set_period_group` | [set_period_group](navigation.md#set_period_group) |
| `quest` | [quest](quests.md#quest) |
| `quest_info` | [quest_info](quests.md#quest_info) |
| `abandon_quest` | [abandon_quest](quests.md#abandon_quest) |
| `claim_all` | [claim_all](quests.md#claim_all) |
| `open_challenges` | [open_challenges](quests.md#open_challenges) |
| `cycle_scroll_group` | [cycle_scroll_group](navigation.md#cycle_scroll_group) |
| `set_scroll_group` | [set_scroll_group](navigation.md#set_scroll_group) |

Sounds and titles cannot be placed directly in task `rewards`; see [supported rewards](../rewards/README.md). Random actions select on execution, whereas random rewards save their outcomes with the task or unlocked stage.
