# 📚 Bundled survival content

The first startup provides survival content to try and customize. Complete one normal task, then inspect its objective and reward files to understand how they fit together.

## Content inventory

| Content | Count | Folder |
| --- | ---: | --- |
| Categories | 8 | `categories/` |
| Reusable objectives | 37 | `quests/` |
| Normal tasks | 26 | `tasks/` |
| Objective pools | 6 | `pools/` |
| Period profiles | 3 | `periods/` |
| Scroll profiles | 4 | `scrolls/` |
| Milestones | 6 | `milestones/` |
| Battle passes | 1 | `battlepasses/` |
| Reward pools | 7 | `reward-pools/` |

## Normal task progression

Open `/uq` and accept a task without prerequisites, such as `miner`. Claim its reward after completion before accepting tasks that depend on it.

| ID | Name | Prerequisites |
| --- | --- | --- |
| `angler` | First Catch |  |
| `archery_training` | Steady Aim | hunter |
| `builder` | A Place to Call Home |  |
| `coal_supply` | Light the Way | miner |
| `deep_miner` | Below the Surface | iron_supply |
| `diamond_expedition` | Diamonds in the Deep | ore_prospector |
| `enchant_apprentice` | A Touch of Magic | iron_supply |
| `enderman_research` | Eyes in the Darkness | night_watch |
| `farmer` | First Harvest |  |
| `fishing_trip` | An Afternoon by the Water | angler |
| `forest_adventure` | Forest Adventure | miner |
| `grain_barn` | Fill the Granary | farmer |
| `hunter` | First Night Patrol |  |
| `iron_supply` | Iron Age | miner |
| `master_builder` | A Growing Settlement | builder |
| `miner` | First Stone |  |
| `nether_expedition` | Into the Nether | deep_miner |
| `night_watch` | Settlement Guardian | hunter |
| `ore_prospector` | Veins of Fortune | iron_supply |
| `sprinter` | Pick Up the Pace (sprint 256 blocks) |  |
| `survivalist` | Prepared for Anything | forest_adventure |
| `sustainable_farm` | A Thriving Farm | grain_barn |
| `swimmer` | Into the Water (swim 128 blocks) |  |
| `traveler` | Beyond the Doorstep |  |
| `veteran_expedition` | Seasoned Adventurer | survivalist |
| `woodworker` | Timber for Shelter |  |

## Periodic assignments and scrolls

Daily, weekly, and monthly profiles each draw three objectives. Resets occur at server-local midnight, Monday midnight, and the first of each month. Claim grace is one hour, one day, and three days respectively. Players use `/uq period` without manual acceptance.

Scrolls come from task/pass rewards or administrator commands and must be carried for progress. Each of the four scroll reward pools has 60 weighted options; first activation draws three without replacement. Periods have separate reward pools. Check actual payout quantities before balancing the economy. Pool edits do not redraw saved rewards.

## Pass and milestones

The bundled `example` pass rotates by calendar month. It has six daily, six weekly, and six season challenges, eighteen total, plus thirty levels of free and premium rewards. The final cumulative threshold is 7350 points, with seven days of old-season claim grace.

Default challenges do not require premium access. Premium rewards require `ultimatequest.battlepass.example.premium` or a persistent administrator grant. Six milestone tracks accumulate automatically without acceptance.

## Default filters and customization

Default objectives allow survival/adventure modes. Mining/wood goals exclude recorded player-placed blocks; applicable combat goals exclude spawner-tagged mobs. Crop goals require maturity and allow player planting. Movement goals count block-coordinate changes with cooldowns, not exact distance.

Visible text lives under `override-lang.defaults` in both language files. Translate text without changing IDs, materials, commands, or Cron. Updates preserve existing files and restore missing bundled files; disable unwanted content with `enabled: false`. Continue to use the unchanged [installation guide](install.md) for updates.
