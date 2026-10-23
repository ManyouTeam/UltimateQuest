# 📖 Menu types

Menus let players browse tasks, inspect progress, and claim rewards. Customize bundled titles, icons, and buttons, or create your own interface.

## Built-in menus

All files are under `plugins/UltimateQuest/menus/`.

| File | Command | Content |
| --- | --- | --- |
| `main.yml` | `/uq` | Normal task acceptance, inspection, claiming, and eligible abandonment. |
| `period.yml` | `/uq period` | Automatically assigned periodic tasks. |
| `scroll.yml` | `/uq scroll` | Carried scrolls and redemption. |
| `battlepass.yml` | `/uq battlepass [id]` | Pass catalogs, levels, and challenges. |
| `milestone.yml` | `/uq milestone [id]` | Milestone catalogs and stage rewards. |

`settings.yml` contains shared settings and cannot be opened as a menu. Other filenames are menu IDs: `tutorial_menu.yml` opens with `/uq menu tutorial_menu`.

## Configuration changes

1. Back up the menu file.
2. Edit its title, layout, item appearance, or click actions.
3. Run `/uq reload` and reopen it to test.

Normal tasks use state-dependent left-click behavior by default: accept, inspect, or claim. Right-click can abandon an eligible task with confirmation. A button's appearance and action are separate; renaming it does not change what it does.

Use the [complete configuration example](../configs/menus.md) for a new menu. Look up fields in [built-in options](builtin-options.md) and button behavior in [menu actions](actions.md).

## Menu permissions and console commands

The default player permission is `ultimatequest.use`. A menu can set its own `permission`. A menu with `enabled: false` cannot be opened.

The console can run `uq menu main Steve` for an online player. Adding `-b` skips the menu's own permission check, while task conditions and reward eligibility remain enforced. See [commands and permissions](../info/commands-permissions.md).

Task names, objectives, and rewards come from their task definitions. See [language management](../features/advanced-language-management.md) and [colors](../features/color-code.md) for presentation.
