# 📚 内置生存服内容

插件首次启动会提供一套可以体验和参考修改的生存服内容。建议先完成一项普通任务，再查看它使用的目标和奖励文件。

## 内容清单

| 内容 | 数量 | 文件夹 |
| --- | ---: | --- |
| 分类 | 8 | `categories/` |
| 可复用目标 | 37 | `quests/` |
| 普通任务 | 26 | `tasks/` |
| 目标池 | 6 | `pools/` |
| 周期配置 | 3 | `periods/` |
| 卷轴配置 | 4 | `scrolls/` |
| 里程碑 | 6 | `milestones/` |
| 战令 | 1 | `battlepasses/` |
| 奖励池 | 7 | `reward-pools/` |

## 普通任务路线

打开 `/uq` 接取没有前置要求的任务，例如 `miner`。完成后先领奖，才能接取以它为前置的后续任务。

| ID | 名称 | 前置任务 |
| --- | --- | --- |
| `angler` | 初次垂钓 |  |
| `archery_training` | 稳住准星 | hunter |
| `builder` | 筑起小家 |  |
| `coal_supply` | 点亮前路 | miner |
| `deep_miner` | 深入地底 | iron_supply |
| `diamond_expedition` | 深岩之光 | ore_prospector |
| `enchant_apprentice` | 初识魔法 | iron_supply |
| `enderman_research` | 暗夜凝视 | night_watch |
| `farmer` | 第一份收成 |  |
| `fishing_trip` | 湖畔时光 | angler |
| `forest_adventure` | 林间历练 | miner |
| `grain_barn` | 粮仓渐满 | farmer |
| `hunter` | 初夜巡逻 |  |
| `iron_supply` | 迈入铁器时代 | miner |
| `master_builder` | 家园扩建 | builder |
| `miner` | 初识矿脉 |  |
| `nether_expedition` | 踏入下界 | deep_miner |
| `night_watch` | 聚落守卫 | hunter |
| `ore_prospector` | 寻觅富矿 | iron_supply |
| `sprinter` | 加快脚步（疾跑 256 格） |  |
| `survivalist` | 生存能手 | forest_adventure |
| `sustainable_farm` | 沃野丰收 | grain_barn |
| `swimmer` | 畅游水中（游泳 128 格） |  |
| `traveler` | 踏出家门 |  |
| `veteran_expedition` | 久经历练 | survivalist |
| `woodworker` | 安家木材 |  |

## 周期任务与卷轴

每日、每周和每月周期各抽取 3 个目标，在服务端本地零点、周一零点和每月 1 日刷新。对应领奖宽限为 1 小时、1 天和 3 天。玩家用 `/uq period` 查看，无需手动接取。

卷轴通过任务、战令等奖励或管理员命令发放，需要携带才累计。四个卷轴奖励池各含 60 个加权选项，首次激活时不重复抽取 3 项。周期也引用各自的奖励池。修改奖励经济前，应检查池里实际发放的数量，已有奖励结果不会因改池重抽。

## 战令与里程碑

内置 `example` 战令按自然月轮换，含每日、每周、赛季各 6 项挑战，共 18 项，30 级免费与高级奖励。最终累计门槛为 7350 分，旧赛季有 7 天领奖宽限。

默认挑战不需要高级资格。高级奖励要求 `ultimatequest.battlepass.example.premium`，或管理员持久化授予的资格。6 条里程碑无需接取，按对应行为长期累计。

## 默认筛选与修改

默认目标筛选生存/冒险模式。矿石、木材目标排除记录到的玩家放置方块，相关战斗目标排除带刷怪笼标记的生物。作物要求成熟，并允许玩家种植；移动目标按跨方块事件和冷却统计，不是精确距离。

显示文本在两种语言文件的 `override-lang.defaults` 下。翻译时保留 ID、材质、命令和 Cron。已有配置文件不会因升级全部替换，缺失内置文件会补充；停用内容用 `enabled: false`。更新步骤仍按[安装说明](install.md)操作。
