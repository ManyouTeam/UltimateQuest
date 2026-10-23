# ✨ Welcome to **UltimateQuest**

> **UltimateQuest** is a configurable quest, battle pass, and milestone framework for modern Minecraft servers!

Build survival quest lines, rotating assignments, physical quest scrolls, and long-term player progression through YAML configuration.

[English Documentation](docs/README.md) · [Chinese Documentation](cn_docs/README.md) · [Discord](https://discord.gg/RZajEybhBw)

---

## 📖 Quests and Reusable Objectives

**UltimateQuest** separates reusable objectives from the tasks players accept, so the same gameplay rules can support several progression systems.

- Define objective templates with triggers, completion amounts, conditions, and anti-abuse rules.
- Combine one or more objectives into a normal task.
- Configure names, descriptions, icons, categories, prerequisites, and rewards.
- Offer player-accepted tasks or assign hidden, admin-only tasks through commands.
- Set activity windows, claim grace periods, and optional automatic reward claims.

Create tasks for mining, farming, hunting, fishing, building, exploration, and other supported events.

---

## ⚙️ Triggers, Conditions, and Anti-Abuse

Build quest objectives from the plugin's shared event components:

- **Triggers** decide which events contribute progress, including combat, projectiles, blocks, movement, fishing, trading, and player state changes.
- **Conditions** filter players, entities, items, blocks, worlds, and other available event context.
- **Matching formats** reuse item and entity rules across objectives and equipment checks.
- **Anti-abuse rules** filter recorded player-placed blocks and spawner-tagged mobs, and limit repeated progress through cooldowns and usage caps.

The source includes **62 built-in trigger IDs**. Optional event availability depends on the server platform and version. Objectives count matching events or supported quantities such as items, damage, experience, and movement distance. Use `move` with `count: distance` and the `sprinting` or `swimming` event condition for activity-specific distance goals. There are 87 built-in event-condition keys, including aliases.

---

## ⏰ Periodic Assignments

Give players fresh objectives on a recurring schedule.

- Draw objectives from weighted pools without replacement.
- Configure daily, weekly, monthly, or custom reset schedules.
- Use cron expressions, daily reset times, fixed intervals, or a fixed time window.
- Configure rewards for each generated assignment.
- Keep completed assignments claimable during a configured grace period.

Periodic assignments share the same objective, condition, reward, category, and task-limit systems as normal tasks.

---

## 📜 Physical Quest Scrolls

Turn quest objectives into items players can carry and complete.

- Combine fixed objectives with weighted objective-pool selections.
- Configure the scroll item, menu icon, rarity, difficulty, and rewards.
- Track progress while the scroll is carried in the inventory or offhand.
- Set an optional duration that starts when the scroll is first activated.
- Bind each scroll to a stored token and consume it when rewards are claimed.

Issue a scroll with `/uq scroll give <player> <profile> [rarity] [difficulty]`. Issuing scrolls requires `ultimatequest.admin`.

---

## 🌱 Battle Passes and Milestones

Connect everyday activities with seasonal and permanent progression.

- Define battle-pass seasons, automatically assigned challenges, and cumulative point thresholds.
- Generate level requirements from a configurable curve or set explicit thresholds.
- Offer separate free and premium reward tracks.
- Configure daily, weekly, and season challenges, unlock weeks, and catch-up behavior.
- Apply player, global, or permission-based experience boosts.
- Create milestones that accumulate progress without player acceptance.
- Grant rewards at milestone stages and battle-pass levels.

Battle passes support calendar rotation, saved season history, claim grace, and administrative progression management.

---

## 🎁 Rewards and Reward Pools

Use one reward format across normal tasks, periodic assignments, scrolls, and progression stages.

- Grant items, execute console commands, send messages, or award battle-pass experience.
- Select weighted reward groups with configurable draw counts and repeat behavior.
- Reuse external reward pools and reward-library entries.
- Customize reward preview names and descriptions.
- Preserve resolved objectives and reward outcomes in instance and stage snapshots.

Random rewards are drawn on normal-task acceptance, periodic instance creation, or first scroll activation. Stage rewards are drawn when unlocked. Reloading or reconnecting does not redraw saved outcomes.

---

## 🧭 Configurable Menus, Editor, and Language

Give players a place to browse tasks and manage their progression.

- Normal-task, periodic-assignment, scroll, battle-pass, and milestone menus.
- Categories, status views, periodic-profile filters, and scroll-profile filters.
- Configurable layouts, item templates, state displays, and click actions.
- In-game editing with drafts, validation, and rollback when application fails.
- English and Chinese default text, per-player language selection, MiniMessage, and legacy colors.
- PlaceholderAPI placeholders for task states and progression values.

Players can open the main menu with `/uq`. Administrators can edit definitions with `/uq editor`.

---

## 📦 Default Survival Content and Storage

Start with a bundled survival progression pack:

- **8 categories**, **37 objective templates**, and **26 normal tasks**.
- **6 objective pools** and daily, weekly, and monthly assignments.
- **4 scroll profiles** and **6 permanent milestone tracks**.
- A monthly battle pass with **18 challenges** and **30 levels** of free and premium rewards.
- Vanilla-item rewards and localized player-facing text.

Store player and scroll records in YAML or use SQLite, MySQL/MariaDB, PostgreSQL, or H2 through JDBC. Configure automatic saving and task limits by type and category.

See the [default survival content guide](docs/info/builtin-tasks.md) for the task route and deployment details.

---

## 🔗 Plugin Integrations

Connect UltimateQuest with other parts of your server when the corresponding plugins are installed.

- **Custom items and blocks:** MMOItems, ItemsAdder, Oraxen, Nexo, CraftEngine, ExecutableItems, and other supported providers.
- **Entity matching:** MythicMobs and optional integration-specific match rules.
- **Text and item data:** PlaceholderAPI and optional NBTAPI matching/change rules.
- **Additional hooks:** economy and protection integrations are available to the shared framework; individual features use the hooks they support.

The default survival pack does not require an economy plugin. External economy rewards can use console-command rewards.

The code includes Paper and Spigot implementations and Folia-aware scheduling. Event availability differs across platforms; Folia support remains experimental.

---

## 🛠️ Building

Use **Maven** and a **JDK compatible with the server APIs referenced by the modules**. The parent project sets the Java compilation target to **21**; newer server dependencies may require a newer build JDK.

1. Provide the local dependency JARs referenced by `systemPath` in `core/pom.xml`, under `core/lib/`.
2. Run the Maven build from the root `pom.xml`:

```shell
mvn clean package -DskipTests
```

The plugin JAR is generated in `plugin/target/`.

Use `/uq reload` for supported runtime configuration changes. Existing task instances and unlocked reward snapshots retain their saved contents. **Changing the database backend requires a full server restart.**

---

## ❤️ Support and License

Read the [English documentation](docs/README.md) or [Chinese documentation](cn_docs/README.md) for configuration guidance, or join our [Discord server](https://discord.gg/RZajEybhBw) for support. Purchasers in mainland China can also join QQ group **815351827** with proof of purchase.

Use, modification, and redistribution are governed by the existing [License Agreement](LICENSE). Please read its terms before using or sharing the plugin.
