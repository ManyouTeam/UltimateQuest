# 🧭 Menu navigation actions

These actions run on menu clicks. Each example is an action fragment for `items.<symbol>.actions` or `contents.<symbol>.actions`; actions are not task reward types. See [action basics](README.md) for placement.

## `previous_page`

Opens the previous list page, stopping at page one. Use it on a static pagination button.

```yaml
type: previous_page
```

## `next_page`

Opens the next list page, stopping at the last page.

```yaml
type: next_page
```

## `refresh`

Rebuilds the current page to show current progress and claim eligibility. It does not reload configuration; use /uq reload for that.

```yaml
type: refresh
```

## `close`

Closes the inventory menu and needs no additional parameters.

```yaml
type: close
```

## `open_menu`

Opens another loaded menu from menus/. `menu` is its filename without .yml and defaults to main.

| Field | Default | Description |
| --- | --- | --- |
| `menu` | main | Target menu ID/path. |

```yaml
type: open_menu
menu: main
```

## `open_categories`

Opens the current menu’s category selector; enable category-filter.enabled first.

```yaml
type: open_categories
```

## `cycle_view`

Cycles state filters in the current menu’s view-filter.values order, such as all, active, and claimable.

```yaml
type: cycle_view
```

## `cycle_challenge_group`

Cycles ALL, DAILY, WEEKLY, and SEASON in the battle pass challenge list.

```yaml
type: cycle_challenge_group
```

## `cycle_period_group`

Cycles available period profiles and all, for example daily, weekly, and all. Profile groups differ from task categories.

```yaml
type: cycle_period_group
```

## `set_period_group`

Selects the period profile named by group; all removes the profile restriction. The ID must be available in the current menu.

| Field | Default | Description |
| --- | --- | --- |
| `group` | all | Available period profile ID or all. |

```yaml
type: set_period_group
group: daily
```

## `cycle_scroll_group`

Cycles scroll-profile groups. Requires scroll-filter.enabled and filters profiles, not rarity labels.

```yaml
type: cycle_scroll_group
```

## `set_scroll_group`

Selects the scroll profile named by group, or all. Requires scroll-filter.enabled.

| Field | Default | Description |
| --- | --- | --- |
| `group` | all | Scroll profile ID or all. |

```yaml
type: set_scroll_group
group: novice
```

## Configuration validation

Run `/uq reload`, reopen the menu, and click the button. Task actions need a clicked task or selected pass/milestone; naming a static button after a task does not establish that association. Navigation retains the current menu-permission bypass, while task and reward checks still apply.
