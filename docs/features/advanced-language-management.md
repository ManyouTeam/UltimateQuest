# 🌏 Language and translations

Task text can be written directly in one language. Use language files and `{lang:...}` when the same task should show different translations to different players.

## Set the default language

Edit the existing `config-files` section in `plugins/UltimateQuest/config.yml`:

```yaml
config-files:
  language: zh_CN
  per-player-language: true
  force-parse-mini-message: false
```

Files belong in `plugins/UltimateQuest/languages/`. Bundled languages are `zh_CN.yml` and `en_US.yml`; `language` takes a filename without `.yml`. The example selects Chinese as the fallback; use `en_US` for English.

With `per-player-language: true`, a loaded file matching the player's client locale takes priority. Missing locales fall back to the default. Some players can therefore still see English after the fallback is changed to Chinese. Disable this option to make everyone use the fallback language.

## Custom text translations

Merge this into the existing `override-lang` in `zh_CN.yml`:

```yaml
override-lang:
  tutorial:
    mining-name: '采石入门'
    mining-description: '挖掘 16 块石头。'
```

Merge the corresponding keys into `en_US.yml`:

```yaml
override-lang:
  tutorial:
    mining-name: 'Stone mining'
    mining-description: 'Break 16 stone blocks.'
```

Merge the contents rather than creating a second `override-lang` root key. Use these display fields in the task:

```yaml
name: '{lang:tutorial.mining-name}'
description:
  - '&7{lang:tutorial.mining-description}'
```

Reload and check the task with Chinese and English client locales. Do not translate configuration keys, task IDs, material names, or command arguments.

## Bundled text and additional languages

Bundled task, objective, menu, and reward text lives under `override-lang.defaults`. For example, `{lang:defaults.tasks.miner.name}` resolves `override-lang.defaults.tasks.miner.name`.

To add a language, copy a file, rename it to the target locale code, and translate values. Preserve required keys and message arguments such as `{remaining}`. Each translation of a message should retain the same arguments.

`force-parse-mini-message` controls forced MiniMessage parsing; see [colors](color-code.md). Menu variables such as `{name}` and `{progress}`, and PlaceholderAPI `%ultimatequest_...%`, are separate from language keys.
