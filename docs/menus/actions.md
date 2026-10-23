# 🖱️ Menu click actions

Icons control appearance; actions control behavior. Put fixed-button actions under `items.<symbol>` and dynamic task-entry actions under `contents.<symbol>`.

## Click type configuration example

Merge this fragment into a menu and put `Q` in its layout:

```yaml
contents:
  Q:
    type: quests
    click-actions:
      LEFT:
        type: quest
      RIGHT:
        type: abandon_quest
        confirm: true
        confirm-seconds: 5
```

Left-click accepts, inspects, or claims according to task state. Right-click abandons eligible normal tasks only, requiring a second click within five seconds by default. Abandonment discards progress.

Each `click-actions` entry replaces generic `actions` for that click. Named child actions execute in order; one action can use `type` directly. An empty matching click node prevents fallback to generic actions.

## Action types and purposes

| Goal | Actions |
| --- | --- |
| Page, refresh, close | `previous_page`, `next_page`, `refresh`, `close`. |
| Open a menu or category selector | `open_menu`, `open_categories`. |
| Change state or profile filters | `cycle_view`, `cycle_period_group`, `set_period_group`, `cycle_scroll_group`, `set_scroll_group`, `cycle_challenge_group`. |
| Operate on the clicked task | `quest`, `quest_info`, `abandon_quest`. |
| Operate on selected progression | `claim_all`, `open_challenges` (passes). |
| Text and visual feedback | `message`, `action_bar`, `announcement`, `title`, `sound`, `particle`, `effect`. |
| Commands or world operations | `console_command`, `player_command`, `op_command`, `command`, `teleport`, `entity_spawn`. |
| Random or delayed execution | `chance`, `any`, `delay`. |

See the [full action reference](../configs/actions/README.md) for fields and examples. Task actions need an entry; an arbitrary static button cannot simply name a task to operate on it. `claim_all` claims selected progression stages, not all normal task rewards.

Menu-permission bypass does not skip task or reward checks. Reload, reopen the interface, and test left, right, and any other configured click types separately.
