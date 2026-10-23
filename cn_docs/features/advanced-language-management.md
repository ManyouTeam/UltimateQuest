# 🌏 语言与翻译

可以直接在任务里填写中文或英文。需要同一任务向不同玩家显示不同语言时，再把文本放进语言文件，用 `{lang:...}` 引用。

## 设置默认语言

编辑 `plugins/UltimateQuest/config.yml` 中已有的 `config-files`：

```yaml
config-files:
  language: zh_CN
  per-player-language: true
  force-parse-mini-message: false
```

语言文件在 `plugins/UltimateQuest/languages/`，内置 `zh_CN.yml` 和 `en_US.yml`。`language` 填文件名，不带 `.yml`。

`per-player-language: true` 优先选择玩家客户端地区对应的已加载语言。没有匹配文件时回退到默认语言。因此，设置默认中文后部分玩家仍看到英文，可能是他们的客户端选择了英文。想所有玩家都用默认语言，可关闭该选项。

## 自定义文本翻译

在 `zh_CN.yml` 已有的 `override-lang` 节点下合入：

```yaml
override-lang:
  tutorial:
    mining-name: '采石入门'
    mining-description: '挖掘 16 块石头。'
```

在 `en_US.yml` 的同一位置合入：

```yaml
override-lang:
  tutorial:
    mining-name: 'Stone mining'
    mining-description: 'Break 16 stone blocks.'
```

注意是合并内容，不要重复添加第二个 `override-lang`。然后在任务中使用以下显示字段：

```yaml
name: '{lang:tutorial.mining-name}'
description:
  - '&7{lang:tutorial.mining-description}'
```

执行 `/uq reload`，分别用中文和英文客户端检查任务名称及描述。配置键、任务 ID、材质名和命令参数不需要翻译。

## 内置文本和新语言

内置任务、目标、菜单及奖励文本集中在 `override-lang.defaults`。例如 `{lang:defaults.tasks.miner.name}` 对应 `override-lang.defaults.tasks.miner.name`。

新增语言时复制已有文件，以目标地区代码重命名，再翻译值。保留必需配置键及消息参数，如 `{remaining}`。不同语言的同一消息应保留相同参数。

`force-parse-mini-message` 控制强制 MiniMessage 解析，格式示例见[颜色代码](color-code.md)。菜单的 `{name}`、`{progress}` 和 PlaceholderAPI 的 `%ultimatequest_...%` 是另外两类变量，不是语言键。
