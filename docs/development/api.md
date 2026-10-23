# 💻 Developer API

This page is for plugins that read or modify UltimateQuest progression. Examples use the current repository's packages and interfaces. Match development dependencies to the plugin version running on the server.

## Dependencies and initialization

Declare the dependency in the calling plugin's `plugin.yml`:

```yaml
depend: [UltimateQuest]
```

The current parent revision is `0.1.0-SNAPSHOT`. Build and install matching modules into your development repository, then reference core. No directly usable public Maven distribution URL is declared here.

```xml
<dependency>
    <groupId>cn.superiormc.ultimatequest</groupId>
    <artifactId>core</artifactId>
    <version>0.1.0-SNAPSHOT</version>
    <scope>provided</scope>
</dependency>
```

Access managers only after UltimateQuest is enabled. Player/world operations must use the appropriate server or region scheduler; an asynchronous caller does not make direct player-state access safe.

## Read and modify progression

This is a method-body fragment in the calling plugin. `player` is an existing online `Player`, and `tutorial_pass` is an existing pass:

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

`progress` reads a loaded record and returns `Optional<ProgressionView>`. It returns empty without a matching record and does not load offline data. Supported progression types are `battlepass` and `milestone`.

`ProgressionView` exposes type, definitionId, season, total, level, and a copied rewards-state map. `addExperience` adds base points to a pass. `claimAll` attempts eligible stage claims through normal validation and persistence.

## Subscribe to progression events

```java
AutoCloseable subscription = service.subscribe(event -> {
    String action = event.action();
    String stage = event.stage();
});
```

Store the returned handle in a plugin field. Call `close()` on disable and handle its declared exception to avoid stale listeners. `ProgressionEvent` contains action, playerId, type, definitionId, season, total, level, and stage.

## Trigger lookup and extensions

`TriggerManager.triggerManager` offers `get(String)`, `values()`, `register(AbstractTrigger<?>)`, `subscribe(Consumer<QuestContext>)`, and `dispatch`. IDs without a namespace use `ultimatequest`.

Extend `api.trigger.AbstractTrigger<E>` with a unique NamespacedKey, configuration key, and payload class, then implement `handle(E, TriggerRuntime)`. `getEventClasses()` provides exact routing classes; subclasses are not automatically matched.

Registration adds a trigger to the manager but does not install a new Bukkit event listener. Explicitly dispatch at the event entry point. Ordinary class-based routing ignores cancelled events; targeted dispatch requires suitable caller checks. Emitted task contexts must be associated with a player.

Trigger `subscribe` currently has no unsubscribe handle, unlike progression subscriptions. Avoid repeated long-lived registrations or retaining disabled-plugin state.

## Event data

`QuestContext` exposes trigger, triggerData, player, and source(), target(), block(), brokenBlockState(), triggerItem(), location(), and event(). Availability depends on the trigger; check null or Optional before use.

Use `BuiltinContextKeys` for typed extra data. `QuestContext.level()` is fixed at 1 and is not the player's pass level. See the [trigger reference](../configs/triggers.md) for data availability.

## Condition and matching extensions

| Extension | Registration | Implementation |
| --- | --- | --- |
| Event condition | `EventConditionsManager.eventConditions.register(AbstractEventCondition)` | Implement protected `onMatch(ObjectSingleCondition)`; common matches handles inversion and feedback. |
| Item matcher | `MatchItemManager.matchItemManager.registerNewRule(AbstractMatchItemRule)` | Participates when its configuration is present. |
| Entity matcher | `MatchEntityManager.matchEntityManager.registerNewRule(AbstractMatchEntityRule)` | Participates when its configuration is present. |
| Item changes | `ChangesManager.changesManager.register(AbstractChangesRule)` | Follow the existing change-rule implementation. |

Use event-condition `onUnload` and `onEntityUnload` to clean up state. Matcher rules participate only when `configNotContains` is false. Use the current `cn.superiormc.ultimatequest` packages and existing TextUtil / ErrorManager entry points. Check the matching version's source for interface details.
