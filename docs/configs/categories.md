# 🗂️ Categories

Categories help players browse tasks, for example mining, combat, and exploration. A task can belong to several categories. Categories have no parent/child hierarchy.

## Category configuration example

Create this file under `plugins/UltimateQuest/`:

```yaml
# categories/tutorial.yml
enabled: true
name: 'Starter quests'
description:
  - 'Learn the server basics.'
icon:
  material: COMPASS
order: 10
```

Add `categories: [tutorial]` at the root of the [normal task example](tasks.md), reload, and open the menu's category button. `tutorial` is the filename ID; `name` is the visible label. Lower `order` values appear first, with IDs breaking ties.

## Category configuration paths

| System | Location |
| --- | --- |
| Normal tasks, scrolls, pass catalogs, milestones | Root `categories`. |
| Generated periodic assignments | `task.categories`. |
| Battle pass challenges | `tasks.<challenge-id>.categories`. |

Template categories are not automatically inherited. Missing or empty lists use `uncategorized`. `all` and `uncategorized` are reserved IDs and cannot be defined as categories.

Unknown IDs or incorrect types reject the definition reload and preserve the previous definitions. Repeated category IDs are deduplicated. Disabling a category hides its selector entry, while its tasks can still appear under “All”.

## Category progress feedback colors

Set a default progress BossBar color at the category file root:

```yaml
progress-feedback:
  bossbar:
    color: RED
```

Color precedence is the task's own `progress-feedback.bossbar.color`, then its category color, then global `task-progress.bossbar.color` in `config.yml`. For multiple categories, the first category with a configured color wins, following `categories` order. Categories supply only the color; other feedback fields still use task overrides or global defaults.

Bundled categories now specify colors. Normal tasks, periodic assignments, scrolls, battle passes, milestones, and challenges inherit colors from their own categories. Supported colors are `PINK`, `BLUE`, `RED`, `GREEN`, `YELLOW`, `PURPLE`, and `WHITE`; categories beyond seven must reuse colors. Apply changes with `/uq reload`.

## Category menu

Enable `category-filter.enabled` in the menu and give a button the `open_categories` action. Shared appearance comes from `config.yml` → `category-menu`. Each menu can override the title, layout, item templates, and buttons through its own `category-filter`.

The category selector has its own interface. Its bundled layout uses six rows (54 slots), as do the other bundled task menus. `Q` holds the dynamic category list configured by `contents.Q`. The default `P`, `N`, `B`, and `C` buttons have page, back, and close actions. Fixed-button symbols can be changed; `actions` determine their behavior.

Fixed buttons use `items.<symbol>.item`, `actions`, and `click-actions`, just like ordinary menus. Page buttons use `hide-on-first-page` and `hide-on-last-page`. Hidden buttons display `hidden-item`, or leave an empty slot if it is omitted. Both default page buttons are hidden when there is only one page.

Merge this fragment into `config.yml` → `category-menu`, or into a menu's `category-filter` to override the shared settings:

```yaml
contents:
  Q:
    type: categories
    item:
      name: '{lang:defaults.ui.category-name}'
      lore:
        - '{lang:category.count}'
    state-items:
      SELECTED:
        glow: true
    actions:
      select:
        type: select_category
items:
  P:
    hide-on-first-page: true
    item:
      material: ARROW
      name: '{lang:menu-previous}'
      lore:
        - '{lang:defaults.ui.page-number}'
    hidden-item:
      material: BLACK_STAINED_GLASS_PANE
      name: '{lang:defaults.ui.blank}'
    actions:
      previous:
        type: previous_page
  B:
    item:
      material: ARROW
      name: '{lang:menu-back}'
    actions:
      back:
        type: back
```

On category cards, `select_category` selects the clicked category by default. A fixed button can specify a valid ID with `category: all`, for example. `back` returns to the original task list, retaining its category, state, period/scroll filters, and task page. Selecting a different category starts at task page one. `state-items.SELECTED` inherits `item`, so only changed fields are needed.

Cards and their actions support `{category-id}`, `{category-name}`, `{category-description}`, `{category-count}`, `{category-active}`, `{category-claimable}`, and `{category-selected}`. `{page}` and `{pages}` refer to the selector's pages. Task cards can show `{categories}`.

Legacy flat `items.<symbol>.material/name/lore` and root `item`/`selected-item` settings are converted when read without rewriting the file. Use the structure above for new configurations. A menu's `actions` override replaces the inherited actions at that node; `actions: {}` disables them.

To show categories first and display tasks after a category is selected, set the following in the menu file, for example `menus/main.yml`:

```yaml
category-filter:
  enabled: true
  open-on-start: true
  hide-empty: true
```

`open-on-start` defaults to `false`. When enabled, each opening of this menu starts with the category selector. Selecting a category opens its task list, and the category button in that list can reopen the selector. This option applies only when category filtering is available for the menu; battle pass and milestone level detail pages are unaffected. Apply changes with `/uq reload`.

`category-filter.hide-empty: true` hides categories with zero entries in the current filter scope. Bundled main and scroll menus enable it; the code default is `false`. “All” remains available. Categories combine with state and period/scroll-profile filters. Switching categories returns to page one.

## Categories and slots

A task in several categories counts toward each category cap but only once toward its type total. Normal tasks and passes use current category definitions; generated periodic tasks and scrolls keep their generated category data. See [task limits](task-limits.md).

Edit categories in `categories/*.yml` and periodic-task categories at `task.categories` in `periods/*.yml`, then apply changes with `/uq reload`. See [menu configuration](menus.md) for a complete interface example.
