# 🎉 UltimateQuest

UltimateQuest adds configurable quests to Minecraft servers. Ask players to mine, fight, fish, or take part in other activities, then reward them for reaching a goal. Edit the content, rewards, and menus in YAML files or use the in-game editor.

## Feature overview

| What you want | Feature | How players participate |
| --- | --- | --- |
| Accept a task and earn a reward for finishing it | [Normal tasks](configs/tasks.md) | Accept it in `/uq`. |
| Refresh assignments every day or week | [Periodic tasks](configs/periods.md) | Assigned automatically; view them in `/uq period`. |
| Give players a physical quest item | [Quest scrolls](configs/scrolls.md) | Carry the scroll, meet its goals, and redeem it in `/uq scroll`. |
| Earn points from challenges and unlock seasonal rewards | [Battle passes](configs/battlepasses.md) | Browse challenges and rewards in `/uq battlepass`. |
| Reward long-term activity | [Milestones](configs/milestones.md) | Progress accumulates without acceptance; view it in `/uq milestone`. |

These systems can run together. Breaking a stone block can advance an accepted task, a current assignment, and a mining milestone if it meets each system's conditions.

## Getting started

1. Check the [requirements](info/requirements.md) and [installation guide](info/install.md).
2. Open `/uq`, accept the bundled mining task, finish it, and return to claim the reward.
3. Read the [configuration file guide](info/configuration-files.md) to learn where content belongs.
4. Follow the [normal task tutorial](configs/tasks.md) to create your own task. Its first example uses vanilla items and needs no other plugin configuration.

The bundled content also includes periodic assignments, scrolls, a battle pass, and milestones. See [bundled content](info/builtin-tasks.md). Set `enabled: false` in a bundled definition you do not want to use.

## Configuration guides

After creating one task, learn about [objective templates](configs/objectives.md) and [rewards](configs/rewards/README.md). Use [objective pools](configs/pools.md) for random assignments. To change the interface, start with [menu types](menus/README.md), then follow [menu configuration](configs/menus.md).

Most edits are applied with `/uq reload`. Accepted tasks keep their existing goals and rewards, so test edits on a newly accepted task. Pages for settings that require a restart, such as storage, explain that requirement separately.

## Related links

### Getting support

Join our [Discord server](https://discord.gg/RZajEybhBw) for support. Please read the server rules and plugin license before requesting help.

Purchasers in mainland China can also join QQ group `815351827` with proof of purchase.
