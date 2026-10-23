# 📦 物品、命令与消息奖励

这三种奖励分别用于发物品、调用服务器命令和发送文字。以下都是奖励片段，放入任务或阶段对应的奖励位置，见[奖励配置](README.md)。

## `item`：发物品

```yaml
rewards:
  food:
    type: item
    item:
      material: BREAD
      amount: 8
    display:
      name: '面包 ×8'
  thanks:
    type: message
    message: '&a感谢参与，{player}！'
```

第一个条目给玩家 8 个面包。`item` 使用 [ItemFormat](../../shared-formats/itemformat-tm.md)，必须生成数量为正的非空气物品。背包装不下的部分会掉落。

`item.amount` 可以是整数或受支持的 `最小值~最大值` 范围，不接受任意数学公式。保存的自定义物品可按[自定义物品教程](../custom-items.md)引用。

## `console_command`：用控制台执行命令

```yaml
rewards:
  announcement:
    type: console_command
    command: 'say {player} completed {quest}'
```

命令不能留空，也不要在开头写 `/`。可用变量有 `{player}`（玩家名）、`{uuid}`（玩家 UUID）和 `{quest}`（任务标识）。调用经济插件、权限插件等命令前，先确认目标插件的命令确实可在控制台执行。

若配置 `/uq scroll give` 奖励卷轴，还需确保对应卷轴配置存在；该命令的完整语法见[卷轴](../scrolls.md)。

## `message`：给玩家发文字

第一个示例中的 `thanks` 就是消息奖励，`message` 必须是字符串。支持的颜色和语言写法见[颜色](../../features/color-code.md)与[语言管理](../../features/advanced-language-management.md)。

消息奖励不代表可以嵌入任意动作列表。想在菜单点击时播放声音或显示标题，应配置菜单动作。

## 验证发放

使用一项新的简单任务测试，完成后分别核对物品、控制台效果和聊天消息。命令执行前可能已有其他奖励发出，出现异常时先核对实际结果，再决定是否重试。
