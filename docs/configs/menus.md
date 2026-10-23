# 🛠️ Menu configuration

A menu combines a layout, fixed buttons, and dynamic lists. Borders and close buttons are fixed; a player's available tasks are dynamic. Start with a small menu before changing the more complex bundled interfaces.

## Task menu configuration example

Create `plugins/UltimateQuest/menus/tutorial_menu.yml` with this complete configuration:

```yaml
enabled: true
permission: ultimatequest.use
title: '&8Quests {page}/{pages}'
layout:
  - FFFFFFFFF
  - FQQQQQQQF
  - PFFFKRFCN
category-filter:
  enabled: true
contents:
  Q:
    type: quests
    filterable: true
    sort: actionable
    use-quest-icon: true
    item:
      name: '&a{name}'
      lore:
        - '{objectives}'
        - '{rewards}'
        - '{hint}'
    click-actions:
      LEFT:
        type: quest
      RIGHT:
        type: abandon_quest
        confirm: true
        confirm-seconds: 5
items:
  F:
    item: {material: BLACK_STAINED_GLASS_PANE, name: ' '}
  P:
    item: {material: ARROW, name: 'Previous'}
    actions: {type: previous_page}
  N:
    item: {material: ARROW, name: 'Next'}
    actions: {type: next_page}
  K:
    category-button: true
    item: {material: CHEST, name: 'Categories'}
    actions: {type: open_categories}
  R:
    item: {material: COMPASS, name: 'Refresh'}
    actions: {type: refresh}
  C:
    item: {material: BARRIER, name: 'Close'}
    actions: {type: close}
```

Run `/uq reload`, then `/uq menu tutorial_menu`. Q slots list normal tasks. Left-click accepts, inspects, or claims according to task state. Right-click attempts to abandon an eligible normal task.

## Layout symbols

Each string represents an inventory row of exactly nine symbols. Use one to six rows. Repeated symbols supply multiple positions: the seven Q slots are seven list positions, not seven copies of one task.

`contents.Q` defines the list, `items.F` defines border items, and other letters define buttons. This layout does not use per-entry `slot` or `slots` fields. Avoid assigning the same symbol to both a fixed button and a content section.

## Common content settings

| Field | Purpose |
| --- | --- |
| `type` | `quests`, `period`, `scroll`, `challenge`, `battlepass`, or `milestone`. |
| `include`, `exclude` | Filter definition IDs; no include whitelist is imposed by default. |
| `categories` | Fixed category scope, intersected with the player's selected category. |
| `filterable` | Participates in the current state filter. |
| `sort` | `actionable` prioritizes usable entries; `natural` keeps natural ordering. |
| `use-quest-icon` | Defaults to `true`; omit `item.material` to preserve the task icon. |
| `item`, `state-items` | Common and state-specific card templates. |
| `empty-item`, `empty-slot` | Empty-list hint and its position. |

`description`, `objectives`, and `rewards` can expand to multiple lines. Give each its own lore line as above. Menu variables differ from PlaceholderAPI `%...%` placeholders.

## Category, state, and profile filters

Category selection uses `category-filter` and `open_categories`; see [categories](categories.md). `view-filter.values` defines valid state filters and their cycle order, selected with `cycle_view`.

Period menus can enable `period-filter.enabled` and use `cycle_period_group` or `set_period_group`. Scroll menus use `scroll-filter`, `cycle_scroll_group`, and `set_scroll_group`. `groups` limits available profile IDs, while `all` selects all profiles.

Override group-button appearance with `period-items.<id>` or `scroll-items.<id>`; scroll buttons also support `scroll-button`. `{period-id}` / `{period-group}` and `{scroll-id}` / `{scroll-group}` show the selected profile's ID and name. Category, state, and profile filters can be combined.

## Multiple progression views

Passes and milestones have catalog and detail views, configured with `catalog-title` / `catalog-layout` and `detail-title` / `detail-layout`. Passes also support `challenge-title` / `challenge-layout`.

A content section's `display` selects `catalog`, `detail`, or `challenges`. Pass detail sections use `track: free` or `premium`; milestones use `default`. Catalog cards use `catalog-item`; other entries prefer `state-items.<state>` and fall back to `item`.

Fixed buttons also support `hide-on-first-page`, `hide-on-last-page`, `hidden-item`, and `view-items.<VIEW>`. See [built-in menu options](../menus/builtin-options.md) for files, states, and variables, and [menu actions](../menus/actions.md) for button behavior.
