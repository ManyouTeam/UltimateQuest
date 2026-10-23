# 💾 存储与保存

存储保存玩家进度、任务奖励结果、领奖状态和卷轴记录。默认不需要数据库：`database.enabled: false` 时使用插件文件夹中的 `data/`。

## 默认设置

下面两段位于 `plugins/UltimateQuest/config.yml`。修改已有节点，不要重复粘贴根节点。

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

`period-tick: 6000` 表示定时保存间隔。在 20 TPS 时约为五分钟，首次定时保存延迟为 180 tick。玩家退出、关键领奖和停服也会保存。

`hide-message` 只隐藏定时保存提示。关闭定时保存不会关闭退出、领奖和停服保存。重载会重新安排定时保存任务。

## 数据库配置

JDBC 后端支持 SQLite、MySQL/MariaDB、PostgreSQL 和 H2。启用数据库时填写适合所选后端的 `jdbc-url`，凭据放在 `database.properties.user` 和 `.password`。`{plugin_folder}` 会替换为插件数据文件夹，驱动加载器可能下载对应驱动。

示例中的 SQLite 地址配合 `enabled: true` 可选择插件文件夹里的 `quests.db`。更换数据库后端需要完整重启服务器，不能只执行 `/uq reload`。

切换前备份原后端。修改开关或地址不会自动迁移原来的玩家数据；看到空进度不代表原数据已经被删除或搬迁。

## 数据备份范围

同时保留配置、玩家数据和卷轴数据。卷轴物品通过唯一令牌关联存储记录，只备份玩家背包并不足以完整恢复卷轴。

任务在生成或接取时保存目标与确定的奖励，因此修改定义不会直接改写旧任务。阶段与卷轴也有各自已保存的内容，详见[奖励](rewards/README.md)和[卷轴](scrolls.md)。

## 加载或领奖异常

玩家数据尚未就绪时，部分查询返回 `LOADING`，请等加载完成再操作。如果领奖停在 `CLAIMING` 或 `REVIEW`，先核对实际发放结果，再按管理命令处理，不要通过删记录来重试。
