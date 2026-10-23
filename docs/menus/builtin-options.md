# ⚙️ Built-in menu options

Find the relevant menu first, then check the shared fields below. This is a field reference; use [menu configuration](../configs/menus.md) to build an interface. All files are under `plugins/UltimateQuest/menus/`.

## `main.yml`

Normal task list. Default left-click accepts, inspects, or claims; right-click requests abandonment. Prerequisites must be claimed before later tasks unlock.

Open with: `/uq`

The table shows bundled content symbols. Letters are layout bindings, not reserved IDs; keep layout and contents consistent when changing them.

| Symbol | Type | Display / track | Purpose |
| --- | --- | --- | --- |
| `Q` | `quests` | always / — | Normal task definitions and accepted instances |

## `period.yml`

Current periodic assignments. period-filter groups by profile ID, independently of categories, and is enabled by default. Without groups, daily, weekly, and monthly appear first, followed by other enabled or assigned profiles.

Open with: `/uq period`

The table shows bundled content symbols. Letters are layout bindings, not reserved IDs; keep layout and contents consistent when changing them.

| Symbol | Type | Display / track | Purpose |
| --- | --- | --- | --- |
| `Q` | `period` | always / — | Assigned periodic instances |

## `scroll.yml`

Carried scrolls. scroll-filter selects profiles, prioritizing novice, mining, adventure, and veteran when groups is omitted. Displaying a scroll does not guarantee redemption; token, ownership, and expiry checks remain.

Open with: `/uq scroll`

The table shows bundled content symbols. Letters are layout bindings, not reserved IDs; keep layout and contents consistent when changing them.

| Symbol | Type | Display / track | Purpose |
| --- | --- | --- | --- |
| `Q` | `scroll` | always / — | Carried scroll instances |

## `battlepass.yml`

Has catalog, level-detail, and challenge views. Catalog cards use catalog-item; stages use state-items or item. free and premium select reward tracks and do not grant premium access.

Open with: `/uq battlepass [ID]`

The table shows bundled content symbols. Letters are layout bindings, not reserved IDs; keep layout and contents consistent when changing them.

| Symbol | Type | Display / track | Purpose |
| --- | --- | --- | --- |
| `J` | `challenge` | challenges / — | Challenges for the selected pass/season |
| `A` | `battlepass` | catalog / — | Pass catalog or its stage rewards |
| `Q` | `battlepass` | detail / free | Pass catalog or its stage rewards |
| `L` | `battlepass` | detail / free | Pass catalog or its stage rewards |
| `R` | `battlepass` | detail / premium | Pass catalog or its stage rewards |

## `milestone.yml`

Has catalog and stage-detail views. Milestone stages use the default track, without the pass premium reward track.

Open with: `/uq milestone [ID]`

The table shows bundled content symbols. Letters are layout bindings, not reserved IDs; keep layout and contents consistent when changing them.

| Symbol | Type | Display / track | Purpose |
| --- | --- | --- | --- |
| `A` | `milestone` | catalog / — | Milestone catalog or its stage rewards |
| `L` | `milestone` | detail / default | Milestone catalog or its stage rewards |

## Shared fields and item appearance

| Field | Purpose |
| --- | --- |
| `enabled`, `permission` | Enablement and opening permission; bypass cannot open a disabled menu. |
| `title`, `layout` | General title and one-to-six-row layout, nine symbols per row. |
| `catalog-title`, `catalog-layout` | Progression catalog presentation. |
| `detail-title`, `detail-layout` | Selected pass or milestone stages. |
| `challenge-title`, `challenge-layout` | Pass challenges; omitted title falls back to the general/default title. |
| `contents.<symbol>` | Dynamic lists, filters, cards, and actions. |
| `items.<symbol>` | Fixed buttons, appearance, and actions. |
| `category-filter` | Inherits config.yml category-menu, with per-menu overrides. |

Cards use [ItemFormat](../shared-formats/itemformat-tm.md). See [menu configuration](../configs/menus.md) for include/exclude, track, state-items, and other content fields.

## State filter meanings

`view-filter.values` accepts ALL, ACTIVE, AVAILABLE, COMPLETED, CLAIMABLE, CLAIMED, LOCKED, and INACTIVE, defining the cycle_view order.

| Filter | Included entries |
| --- | --- |
| `ALL` | All entries allowed by the content section. |
| `ACTIVE` | In progress. |
| `AVAILABLE` | Unaccepted AVAILABLE / LOCKED ordinary entries; does not guarantee acceptance eligibility. |
| `COMPLETED` | COMPLETED, CLAIMING, CLAIMED, REVIEW; broader than claimable. |
| `CLAIMABLE` | Completed tasks or AVAILABLE stages. |
| `CLAIMED` | Claimed rewards. |
| `LOCKED` | Locked or premium-required entries. |
| `INACTIVE` | Inactive entries. |

Without a valid override, code defaults are ALL/ACTIVE/CLAIMABLE for scrolls, ALL/ACTIVE/CLAIMABLE/CLAIMED for periods and challenges, and ALL/ACTIVE/AVAILABLE/COMPLETED elsewhere. Bundled files may override them; main also includes CLAIMABLE and CLAIMED.

category-filter.hide-empty defaults to false in code and is enabled in bundled main/scroll files. It hides empty categories within the current filter scope. See [menu configuration](../configs/menus.md) for profile buttons, appearance, and actions.

## Shared display settings

```yaml
# menus/settings.yml
anti-dupe-checker: true
lore-wrap-length: 34
progress-bar:
  length: 12
  complete: '&a|'
  remaining: '&7|'
  suffix: ''
```

This is a display-settings fragment. Keep the duplication checker enabled for normal use. lore-wrap-length controls wrapping and progress-bar controls the bar appearance. settings cannot be opened as a menu.

## Available variables

| Scope | Placeholders |
| --- | --- |
| Common | `{player}`, `{menu}`, `{page}`, `{pages}`, `{view}`, `{count}`, `{active}`, `{claimable}`, `{claimed}`, `{available}`, `{locked}`, `{category-id}`, `{category-name}`, `{challenge-group}`, `{period-id}`, `{period-group}`, `{scroll-id}`, `{scroll-group}` |
| Task/stage | `{id}`, `{name}`, `{description}`, `{objectives}`, `{state}`, `{hint}`, `{prerequisites}`, `{categories}`, `{level}`, `{total}`, `{required}`, `{progress}`, `{progress-required}`, `{progress-percent}`, `{progress-bar}`, `{track}`, `{season}`, `{rewards}` |
| Assignments | `{rarity}`, `{difficulty}`, `{points}`, `{group}`, `{unlock-week}`, `{source}`, `{expires}`, `{expires-in}` |
| Selected progression | `{next-required}`, `{remaining-xp}`, `{season-time}` |

Place multiline `{description}`, `{objectives}`, and `{rewards}` on separate lore lines. Entry variables override menu totals, and available data depends on the view. See [categories](../configs/categories.md) for category-card variables and [menu actions](actions.md) for clicks.
