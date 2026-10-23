# 🎉 UltimateQuest

UltimateQuest 是 Minecraft 服务端的任务插件。你可以让玩家挖矿、击杀怪物、钓鱼或参与其他活动，达到要求后领取奖励。任务内容、奖励和菜单都可以通过 YAML 配置文件修改，再使用 `/uq reload` 应用更改。

## 功能概览

| 你想实现什么 | 使用什么功能 | 玩家怎样参与 |
| --- | --- | --- |
| 接取一项任务，完成后领取奖励 | [普通任务](configs/tasks.md) | 在 `/uq` 中接取。 |
| 每天或每周刷新一批委托 | [周期任务](configs/periods.md) | 系统自动分配，在 `/uq period` 查看。 |
| 发给玩家一张可以完成的任务物品 | [任务卷轴](configs/scrolls.md) | 携带卷轴完成目标，在 `/uq scroll` 兑换。 |
| 完成挑战获得积分，升级领取赛季奖励 | [战令](configs/battlepasses.md) | 在 `/uq battlepass` 查看挑战与奖励。 |
| 长期累计挖矿、战斗等进度 | [里程碑](configs/milestones.md) | 无需接取，在 `/uq milestone` 查看进度。 |

这些玩法可以一起使用。例如，挖石头的一次操作可以分别推进已接任务、当前委托和挖矿里程碑，前提是各自的条件都满足。

## 快速开始

1. 按[运行要求](info/requirements.md)和[安装说明](info/install.md)安装插件。
2. 输入 `/uq`，接取内置的挖矿任务，完成后回到菜单领奖。
3. 阅读[配置文件说明](info/configuration-files.md)，认识各文件夹。
4. 按[普通任务教程](configs/tasks.md)创建自己的第一个任务。示例使用原版物品，不要求先配置其他插件。

默认还附带周期委托、卷轴、战令和里程碑，详情见[内置内容](info/builtin-tasks.md)。不需要的内置定义可以在对应文件中设置 `enabled: false`。

## 配置指南

先学会一个普通任务，再了解[目标模板](configs/objectives.md)和[奖励](configs/rewards/README.md)。想做随机委托时，再学习[目标池](configs/pools.md)。想调整界面时，先看[菜单类型](menus/README.md)，再看[菜单配置](configs/menus.md)。

修改后一般执行 `/uq reload`。已经接取的任务会保留原来的要求和奖励，测试修改时请使用新接取的任务。数据库设置等需要重启的项目会在对应页面单独说明。

## 相关链接

### 获取支持

请通过 [Discord 服务器](https://discord.gg/RZajEybhBw) 获取支持。请求帮助前，请先阅读服务器规则和插件许可协议。

中国大陆已购买插件的用户可以凭购买证明加入 QQ 群 `815351827`。
