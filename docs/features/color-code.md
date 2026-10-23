# 🎨 Colors and text formatting

Supported task names, descriptions, menu text, and messages can use color formatting. Test the actual output location because chat, item lore, and inventory titles can use different text components.

## Common formats

These are display-field fragments for task names or equivalent text locations:

```yaml
name: '&aGreen quest'
```

`&a` is green, `&6` is gold, and `&c` is red. Quote YAML strings beginning with `&`.

For a precise color, use six hexadecimal digits:

```yaml
name: '&#60A5FAStone mining'
```

The plugin also recognizes paired gradient endpoints:

```yaml
name: '&<#60A5FA>Stone mining&<#22D3EE>'
```

Full gradient rendering depends on the parsing path and receiving component. A result in one interface does not guarantee identical rendering elsewhere.

## MiniMessage

`config.yml` → `config-files.force-parse-mini-message` controls forced MiniMessage parsing. Confirm the field's parsing path before using tags, then test a simple example:

```yaml
name: '<green>Stone mining</green>'
```

Avoid casually mixing formats in one string. When troubleshooting, start with one simple color to confirm parsing works.

## Colors in translated text

Define a key through [language management](advanced-language-management.md), then reference it:

```yaml
name: '&a{lang:tutorial.mining-name}'
```

If raw codes appear or text stays unchanged, check quoting, field placement, and the language key. Reload and reopen the menu. Accepted tasks can display saved definitions, which also matters when testing edits.
