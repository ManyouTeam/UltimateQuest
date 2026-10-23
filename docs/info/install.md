# ⚙️ Install

## Install

* Download UltimateQuest from the official release page. The file name should be **UltimateQuest-X.X.X.jar**, where X is the version number.
* Put the `.jar` file into your server's `plugins` folder.
* Stop your server and then restart it. <mark style="color:red;">Do not load the plugin while the server is running.</mark>
* On the first start, the plugin creates its configuration files in `plugins/UltimateQuest/`.
* Default configs help you understand the configuration framework. Try them first, then modify them to meet your server's needs.

{% hint style="warning" %}
Do not use Bukkit `/reload`, PlugMan, or another plugin hot-loader. Use `/uq reload` for configuration changes and a full server restart when replacing the plugin JAR.
{% endhint %}

## Getting started

1. Use `/uq` to open the task menu.
2. Accept a task, such as the built-in mining task.
3. Complete its objectives. Only actions performed after acceptance count toward a normal task.
4. Return to the menu to claim the reward.

The other menus are available through `/uq period`, `/uq scroll`, `/uq battlepass`, and `/uq milestone`. See [Built-in Tasks](builtin-tasks.md) for the included content and [Commands & Permissions](commands-permissions.md) for administrative commands.

To change the default language, edit `config-files.language` in `config.yml`. Use `en_US` for English or `zh_CN` for Simplified Chinese. See [Advanced Language Management](../features/advanced-language-management.md) for player-specific languages and custom translations.

## Update

* Back up the `UltimateQuest` folder and your database, if used, before updating.
* Stop the server, remove the old plugin JAR, and install the new version. Keep only one UltimateQuest JAR in `plugins/`.
* You should always keep the plugin updated to the latest version.
* Existing task and menu files are preserved. Compare your customized files with the new defaults and manually merge changes as needed. Missing built-in files are created on startup; set `enabled: false` in a built-in task file if you want to disable it.
* Missing built-in language keys are added automatically; existing translations are preserved.
* Before every update, read the changelog carefully. If you skip multiple versions, review all changelogs in between. Major breaking changes will be noted there.

Keep player and scroll data when updating. You do not need to delete progress to update the plugin.

## Reloading configuration

After editing tasks, rewards, menus, or language files, use:

```text
/uq reload
```

If a configuration error is reported, correct the indicated file and reload again. Tasks already accepted or assigned keep their existing objectives and rewards, so test a changed task by accepting a new one.

A full restart is required after replacing the plugin JAR, installing or removing an integration plugin, or changing the database settings.
