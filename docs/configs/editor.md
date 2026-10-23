# 🛠️ In-game editor

Administrators can edit tasks, objectives, menus, and other definitions in game. Open `/uq editor`, choose a scope, then a file. It requires `ultimatequest.admin`; `config.yml` → `editor.enabled` is enabled by default.

## Configuration file selection

`/uq editor task miner` opens the bundled mining task, `/uq editor scroll novice` opens the novice scroll, and `/uq editor menu main` opens the main menu. The last argument is the file ID without `.yml`.

| Scope | Files |
| --- | --- |
| `quest` | Reusable objectives in `quests/`. |
| `task` | Normal tasks in `tasks/`. |
| `category` | Task categories in `categories/`. |
| `battlepass` | Battle passes in `battlepasses/`. |
| `milestone` | Milestones in `milestones/`. |
| `period` | Periodic tasks in `periods/`. |
| `scroll` | Quest scrolls in `scrolls/`. |
| `pool` | Objective pools in `pools/`. |
| `reward-pool` | Reward pools in `reward-pools/`. |
| `menu` | Menus in `menus/`. |

## Draft validation and application

1. Edit text, numbers, lists, items, or nested fields. Categories support multiple selections.
2. Review the draft and choose **Validate and Apply**.
3. Correct any validation errors and apply again.
4. Test a new task or reopened menu after success.

Edits are drafts until applied. Applying creates a backup and reloads the affected configuration. Validation failure restores the original file.

## External edits and existing tasks

Do not edit the same file externally while its editor is open. Reopen it if the file changed. Category editing in a periodic definition changes `task.categories`.

The editor does not rewrite objectives and rewards in accepted tasks. Issued scrolls and unlocked random rewards retain their respective saved content; see [rewards](rewards/README.md).
