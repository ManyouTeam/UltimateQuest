# 💾 Storage and saving

Storage keeps player progress, resolved rewards, claim states, and scroll records. A database is optional: with `database.enabled: false`, records use `data/` inside the plugin folder.

## Default settings

These sections belong in `plugins/UltimateQuest/config.yml`. Edit existing nodes rather than pasting duplicate root keys.

```yaml
auto-save:
  enabled: true
  hide-message: false
  period-tick: 6000
  message: '{lang:auto-save.message}'
database:
  enabled: false
  jdbc-url: jdbc:sqlite:{plugin_folder}/quests.db
  properties:
    user: ''
    password: ''
```

`period-tick: 6000` sets the autosave interval, about five minutes at 20 TPS. The initial scheduled-save delay is 180 ticks. Player quit, critical claims, and shutdown also save data.

`hide-message` hides scheduled-save messages only. Disabling autosave does not disable quit, claim, or shutdown saves. Reloading reschedules the autosave timer.

## Database configuration

The JDBC backend supports SQLite, MySQL/MariaDB, PostgreSQL, and H2. Supply an appropriate `jdbc-url` and credentials in `database.properties.user` and `.password`. `{plugin_folder}` expands to the plugin data folder. The driver loader may download the corresponding driver.

Setting `enabled: true` with the example SQLite URL selects `quests.db` in the plugin folder. Changing the backend requires a full server restart; `/uq reload` is insufficient.

Back up the original backend first. Changing the switch or URL does not migrate player data. Empty progress in the new backend does not mean the previous records were deleted or transferred.

## Backup scope

Keep configurations, player records, and scroll records together. Physical scrolls link unique tokens to stored records; backing up inventories alone is insufficient to restore them fully.

Tasks save their resolved objectives and rewards on creation or acceptance, so definition edits do not rewrite older tasks. Stages and scrolls have their own saved content; see [rewards](rewards/README.md) and [scrolls](scrolls.md).

## Loading and interrupted claims

Queries can return `LOADING` until player data is ready. Wait for loading before operating. For claims left in `CLAIMING` or `REVIEW`, check what was actually paid before using management commands. Deleting records is not a safe retry method.
