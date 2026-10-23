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

## Category menu

Enable `category-filter.enabled` in the menu and give a button the `open_categories` action. Shared appearance comes from `config.yml` → `category-menu`. Each menu can override the title, layout, item templates, and buttons through its own `category-filter`.

The selector layout uses `Q` for category slots, `P` and `N` for pages, `B` to return, and `C` to close. Cards support `{category-id}`, `{category-name}`, `{category-description}`, `{category-count}`, `{category-active}`, `{category-claimable}`, and `{category-selected}`. Task cards can show `{categories}`.

`category-filter.hide-empty: true` hides categories with zero entries in the current filter scope. Bundled main and scroll menus enable it; the code default is `false`. “All” remains available. Categories combine with state and period/scroll-profile filters. Switching categories returns to page one.

## Categories and slots

A task in several categories counts toward each category cap but only once toward its type total. Normal tasks and passes use current category definitions; generated periodic tasks and scrolls keep their generated category data. See [task limits](task-limits.md).

Edit categories in `categories/*.yml` and periodic-task categories at `task.categories` in `periods/*.yml`, then apply changes with `/uq reload`. See [menu configuration](menus.md) for a complete interface example.
