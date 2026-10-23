# 🧩 Control-flow actions

These actions run on menu clicks. Each example is an action fragment for `items.<symbol>.actions` or `contents.<symbol>.actions`; actions are not task reward types. See [action basics](README.md) for placement.

## `chance`

Makes a percentage roll each time it runs, then executes all children on success. `rate: 25` means a 25% chance. Use 0–100 rather than reward-pool weights.

| Field | Default | Description |
| --- | --- | --- |
| `rate` | Required | Percentage compared to a random value in [0,100). |
| `actions` | Required | Nested action mapping. |

```yaml
type: chance
rate: 25
actions:
  feedback:
    type: message
    message: '&aLucky!'
```

## `any`

Uniformly selects up to `amount` different child actions. If amount exceeds the candidate count, all run. This does not use weighted choices.

| Field | Default | Description |
| --- | --- | --- |
| `amount` | 1 | Maximum selected action count. |
| `actions` | Required | Child action mapping. |

```yaml
type: any
amount: 1
actions:
  one:
    type: message
    message: '&aOne'
  two:
    type: message
    message: '&bTwo'
```

## `delay`

Waits `time` server ticks before running children. At 20 TPS, time: 20 is about one second. The player must still be online. The original click context is retained, but task state may have changed.

| Field | Default | Description |
| --- | --- | --- |
| `time` | Required | Server tick delay, stored as an integer. |
| `actions` | Required | Child action mapping. |

```yaml
type: delay
time: 20
actions:
  notice:
    type: message
    message: '&eOne second later.'
```

## Configuration validation

Run `/uq reload`, reopen the menu, and click the button. Task actions need a clicked task or selected pass/milestone; naming a static button after a task does not establish that association. Navigation retains the current menu-permission bypass, while task and reward checks still apply.
