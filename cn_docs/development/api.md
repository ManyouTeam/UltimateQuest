# 💻 开发 API

本页面向需要通过自己的插件读取或修改 UltimateQuest 成长数据的开发者。示例使用当前仓库的包名和接口，开发依赖应与服务器运行的插件版本一致。

## 依赖与初始化

在调用方插件的 `plugin.yml` 中声明依赖：

```yaml
depend: [UltimateQuest]
```

父项目当前版本为 `0.1.0-SNAPSHOT`。将匹配源码构建的模块安装到自己的开发仓库，再引用 core；项目没有在此声明一个可直接使用的公开 Maven 发布地址。

```xml
<dependency>
    <groupId>cn.superiormc.ultimatequest</groupId>
    <artifactId>core</artifactId>
    <version>0.1.0-SNAPSHOT</version>
    <scope>provided</scope>
</dependency>
```

只有 UltimateQuest 启用后才能访问其管理器。玩家或世界操作必须使用对应服务端及区域调度器，不能因调用来自异步任务就直接访问玩家状态。

## 成长数据查询与修改

以下是调用方插件方法中的代码片段，`player` 为已有在线 `Player`，`tutorial_pass` 为已经创建的战令：

```java
import cn.superiormc.ultimatequest.api.progression.ProgressionService;
import cn.superiormc.ultimatequest.managers.QuestManager;

ProgressionService service = QuestManager.questManager.progression();
service.progress(player.getUniqueId(), "battlepass", "tutorial_pass")
        .ifPresent(view -> {
            long total = view.total();
            int level = view.level();
        });
service.addExperience(player, "tutorial_pass", 25L);
service.claimAll(player, "battlepass", "tutorial_pass");
```

`progress` 读取当前已加载记录，返回 `Optional<ProgressionView>`。没有对应记录时为空，不会主动加载离线玩家数据。`type` 支持 `battlepass` 和 `milestone`。

`ProgressionView` 提供 type、definitionId、season、total、level 和复制的 rewards 状态映射。`addExperience` 向指定战令增加基础积分；`claimAll` 尝试领取符合条件的阶段，经过正常校验和持久化流程。

## 成长事件订阅

```java
AutoCloseable subscription = service.subscribe(event -> {
    String action = event.action();
    String stage = event.stage();
});
```

将返回的订阅句柄保存到调用方插件字段，在插件停用时调用 `close()` 并处理其声明的异常，避免留下失效监听器。`ProgressionEvent` 包含 action、playerId、type、definitionId、season、total、level、stage。

## 触发器查询与扩展

`TriggerManager.triggerManager` 提供 `get(String)`、`values()`、`register(AbstractTrigger<?>)`、`subscribe(Consumer<QuestContext>)` 和 `dispatch`。未写命名空间的 ID 默认使用 `ultimatequest`。

自定义触发器继承 `api.trigger.AbstractTrigger<E>`，提供唯一 NamespacedKey、配置键和载荷类，实现 `handle(E, TriggerRuntime)`。`getEventClasses()` 返回精确路由类，不自动匹配子类。

注册触发器只会加入管理器，不会自动安装新的 Bukkit 事件监听器。调用方还需在事件入口显式 dispatch。按事件类的普通派发忽略取消事件，定向派发需由调用方负责适当检查。发出的任务事件必须关联玩家。

触发器 `subscribe` 当前不返回退订句柄，与成长事件订阅不同。避免反复注册长期监听器或保留已停用插件的数据。

## 事件数据

`QuestContext` 提供 trigger、triggerData、player，以及 source()、target()、block()、brokenBlockState()、triggerItem()、location()、event()。具体字段是否存在取决于触发器，读取前检查 null 或 Optional。

附加数据可通过 `BuiltinContextKeys` 类型化读取。`QuestContext.level()` 固定为 1，不是玩家战令等级。各触发器的数据范围见[触发器参考](../configs/triggers.md)。

## 条件与匹配扩展

| 扩展点 | 注册入口 | 实现要点 |
| --- | --- | --- |
| 事件条件 | `EventConditionsManager.eventConditions.register(AbstractEventCondition)` | 实现 protected `onMatch(ObjectSingleCondition)`；公共 matches 处理反转与提示。 |
| 物品匹配 | `MatchItemManager.matchItemManager.registerNewRule(AbstractMatchItemRule)` | 对应配置存在时参与匹配。 |
| 实体匹配 | `MatchEntityManager.matchEntityManager.registerNewRule(AbstractMatchEntityRule)` | 对应配置存在时参与匹配。 |
| 物品修改 | `ChangesManager.changesManager.register(AbstractChangesRule)` | 按现有物品修改规则实现。 |

事件条件的 `onUnload`、`onEntityUnload` 用于清理状态。匹配规则仅在 `configNotContains` 为 false 时参与。开发时使用当前 `cn.superiormc.ultimatequest` 包及既有 TextUtil / ErrorManager 入口，接口细节以匹配版本源码为准。
