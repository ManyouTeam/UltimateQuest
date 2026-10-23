# 📜 Quest and reward actions

These actions run on menu clicks. Each example is an action fragment for `items.<symbol>.actions` or `contents.<symbol>.actions`; actions are not task reward types. See [action basics](README.md) for placement.

## `quest`

Acts on the clicked entry: accept a normal task, inspect an active task, claim a completed reward, or open progression details and claim stages. It uses the clicked entry, not a separate quest-ID field.

```yaml
type: quest
```

## `quest_info`

Shows information about the clicked task. A static button without a task entry has no task to inspect.

```yaml
type: quest_info
```

## `abandon_quest`

Abandons an eligible ACTIVE or COMPLETED normal task and discards progress. By default, a second click within five seconds confirms; confirm-seconds is at least 1. Generated assignments and progression rewards are not general abandonable tasks.

Left-click the confirmation item to cancel abandonment and restore its normal display. That click does not inspect the task or claim rewards. A later abandonment attempt requires a new right-click confirmation.

With root `allow-abandon: false` in the normal task configuration, this action and the abandon command reject the operation without a confirmation step.

| Field | Default | Description |
| --- | --- | --- |
| `confirm` | true | Require a second click. |
| `confirm-seconds` | 5 | Confirmation window, minimum one second. |

```yaml
type: abandon_quest
confirm: true
confirm-seconds: 5
```

## `claim_all`

Claims all eligible stages of the selected pass or milestone. It does not claim every normal task reward.

```yaml
type: claim_all
```

## `open_challenges`

Opens challenges for the selected pass; a pass must already be selected in the menu.

```yaml
type: open_challenges
```

## Configuration validation

Run `/uq reload`, reopen the menu, and click the button. Task actions need a clicked task or selected pass/milestone; naming a static button after a task does not establish that association. Navigation retains the current menu-permission bypass, while task and reward checks still apply.
