# 🛠️ Configuration files

All files are under `plugins/UltimateQuest/`. Choose the folder for the feature you want to edit. Most definitions use one file per ID, while global settings live separately.

## Files and purposes

| Path | Purpose |
| --- | --- |
| `config.yml` | Language, math, auto-save, task limits, database, editor, and category-menu defaults. |
| `languages/*.yml` | Messages and `override-lang` text. Bundled locales: `en_US`, `zh_CN`. |
| `quests/*.yml` | [Reusable objective templates](../configs/objectives.md), shared by tasks and other quest types. |
| `tasks/*.yml` | [Normal tasks](../configs/tasks.md). |
| `categories/*.yml` | [Category names, icons, and order](../configs/categories.md). |
| `pools/*.yml` | [Weighted objective pools](../configs/pools.md). |
| `reward-pools/*.yml` | [Reusable weighted reward choices](../configs/rewards/README.md). |
| `periods/*.yml` | [Periodic assignments](../configs/periods.md). |
| `scrolls/*.yml` | [Physical scroll profiles](../configs/scrolls.md). |
| `battlepasses/*.yml` | [Battle passes and challenges](../configs/battlepasses.md). |
| `milestones/*.yml` | [Cumulative milestones](../configs/milestones.md). |
| `menus/*.yml` | [Menu definitions](../configs/menus.md); `settings.yml` contains shared settings. |
| `items/` | [Saved items](../configs/custom-items.md). |
| `data/` | YAML storage when SQL is disabled; see [storage](../configs/storage.md). |

`quests/` holds reusable objectives; `tasks/` holds complete normal tasks. For example, `quests/tutorial_stone.yml` defines stone-mining requirements, while `tasks/tutorial_mining.yml` also defines presentation, acceptance, and rewards. Do not put a full task in the template folder.

## IDs and loading rules

The filename without `.yml` is the definition ID. Use lowercase letters, digits, underscores, or hyphens. References and commands use IDs rather than visible names. Definition folders read direct-child `.yml` files, not nested subfolders.

`enabled` defaults to `true`. Disable bundled definitions with `enabled: false`; deleting a file may cause it to be restored at startup.

## Global config.yml settings

| Section | Purpose | Guide |
| --- | --- | --- |
| `config-files` | Default/client language and MiniMessage | [Languages](../features/advanced-language-management.md) |
| `math` | Expressions and functions in supported fields | [Math format](../shared-formats/math-calculate-format.md) |
| `hook-item-method` | Third-party item retrieval | Keep default `DEFAULT` unless another method is needed. |
| `auto-save` | Scheduled saving and messages | [Storage](../configs/storage.md) |
| `database` | YAML / JDBC backend | Requires restart; does not automatically migrate records. |
| `task-limits` | Simultaneous type and category limits | [Task limits](../configs/task-limits.md) |
| `editor` | In-game editor switch | [Editor](../configs/editor.md) |
| `objective-display.formats` | Fallback objective text | [Objectives](../configs/objectives.md) |
| `task-display` | Prerequisite and administrative-task hints | Display text. |
| `category-menu` | Shared category selector appearance | [Categories](../configs/categories.md) |
| `debug` | Additional diagnostic information | Normally disabled. |

Layouts live in `menus/`, with shared display settings in `menus/settings.yml`; not every setting belongs in config.yml.

## Configuration change workflow

1. Back up the affected file.
2. Edit fields with spaces for YAML indentation. Merge additions into existing sections rather than duplicating root keys.
3. Run `/uq reload` and resolve reported errors.
4. Verify with newly accepted tasks, issued scrolls, or generated assignments.

Reloading does not rewrite saved goals and rewards in existing tasks. For JAR replacement, database changes, or integration changes requiring restart, see [installation](install.md) and the relevant feature page.
