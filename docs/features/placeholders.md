# 🧩 PlaceholderAPI

Install PlaceholderAPI to display task and progression data in compatible scoreboard, hologram, or other plugins. UltimateQuest registers its own persistent expansion at startup. Menu-local `{name}` variables are a separate system.

## Placeholder usage example

Start the server with PlaceholderAPI installed, join with a test player, and open a pass. If you created `tutorial_pass`, test `%ultimatequest_battlepass_total_tutorial_pass%` using PlaceholderAPI's parse command.

A player with 25 accumulated points should receive `25`. Replace the final ID with your pass ID; the bundled pass uses `example`.

## Pass and milestone fields

The format is `%ultimatequest_battlepass_<field>_<id>%`. Replace `battlepass` with `milestone` for milestones.

| Field | Meaning when a corresponding record is loaded |
| --- | --- |
| `total` | Accumulated points or progress. |
| `level` | Reached level or stage. |
| `season` | Current definition's season ID; empty for an unknown definition. |
| `remaining` | Amount needed to reach the next cumulative threshold. |
| `progress` | Progress beyond the previous reached threshold, not the total. |
| `percent` | Integer percentage within the current level interval. |
| `premium` | Online player's premium eligibility, true / false. |
| `claimable` | Available stage count; premium stages also check the online player's eligibility. |
| `timeleft` | Seconds until the end; -1 for no end, 0 after ending. |

## Query a normal task state

Use `%ultimatequest_state_<task-or-instance-id>%`, for example `%ultimatequest_state_tutorial_mining%`.

It can return stored states such as `ACTIVE`, `COMPLETED`, or `CLAIMED`. With no matching instance it returns `AVAILABLE`, which does not prove that the definition exists or the player can accept it.

## Missing and loading data

Data not ready returns `LOADING`. Missing progression records return `0` for most value queries; this does not automatically load offline records. Premium eligibility needs an online player. Unknown formats return no value, which the calling plugin may display differently.

For internal `{name}` and `{progress}` variables, see [menu options](../menus/builtin-options.md). For `{lang:...}`, see [language management](advanced-language-management.md).
