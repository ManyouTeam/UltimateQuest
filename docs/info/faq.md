# ❓ Configuration troubleshooting

## Missing task progress

Check task state, trigger, conditions, anti-abuse, and activity time in that order. Normal tasks need acceptance; periods and pass challenges are automatically assigned; milestones need no acceptance; scrolls need successful activation and carrying.

Confirm platform support, then check the condition's entity selector. Use `target: PLAYER` for the quest owner and usually TARGET for a kill victim. Block/item conditions cannot match absent event data. The ordinary event route ignores cancelled events.

Mining goals exclude recorded player-placed blocks by default. A crop-material condition does not by itself check maturity. See [triggers](../configs/triggers.md), [conditions](../configs/conditions/README.md), and [anti-abuse](../configs/anti-abuse.md).

## Reloaded edits do not change existing tasks

Acceptance or generation saves the requirements and rewards. Template edits do not replace that saved content. Verify with new tasks, new scrolls, or new periodic assignments; see [saved rewards](../configs/rewards/README.md).

Random results also survive reloads and reconnects. Scroll definitions are saved on issue and reward draws occur on activation. Progression rewards are determined on unlock.

## Completed tasks still prevent acceptance

Completed but unclaimed tasks occupy slots. Claim rewards, then check type and category caps. Multi-category tasks must satisfy every applicable cap. Lowering limits does not delete old tasks. See [task limits](../configs/task-limits.md).

## Periodic batches are not assigned

Periods check capacity for a whole batch. One free slot cannot fit a batch of three assignments. Release enough slots and check again. Retries keep their determined draw.

## Scroll progress or redemption fails

Check inventory/offhand carrying, successful activation, available slots, and expiry. Scrolls require linked storage records; matching names and lore do not create valid scrolls. Removing an activated scroll does not pause its expiry. See [scrolls](../configs/scrolls.md).

## Completed content cannot be claimed

Check task/stage state, claiming time, reward track, and premium access. For `CLAIMING` / `REVIEW`, administrators must check actual payouts before resolving. Menu-permission bypass does not bypass reward checks.

## Deleted bundled files return

Missing bundled definitions are restored. Keep an unwanted file and set `enabled: false`. Do not delete player progress to update configuration.

## Some players still see a different language

With `per-player-language` enabled, the loaded language matching the client locale takes priority. The default is a fallback. See [language management](../features/advanced-language-management.md).
