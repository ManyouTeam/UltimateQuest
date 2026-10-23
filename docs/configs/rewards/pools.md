# 🗂️ Reward pools and the reward library

Use a reward pool when several tasks share random candidates. Use the reward library when they share one complete reward entry. Neither selects task objectives.

## Reward pool example

Create this file under `plugins/UltimateQuest/`:

```yaml
# reward-pools/tutorial_supplies.yml
enabled: true
rewards:
  food:
    rate: 3
    rewards:
      bread:
        type: item
        item: {material: BREAD, amount: 8}
  gems:
    rate: 1
    rewards:
      emerald:
        type: item
        item: {material: EMERALD, amount: 2}
```

Reference it at a task's reward path:

```yaml
rewards:
  supply:
    type: random
    pool: tutorial_supplies
    count: 1
    repeat: false
```

Save both files and run `/uq reload`. Newly accepted tasks draw one reward group from the pool. Keep `count` and `repeat` at the reference so different tasks can use different draw settings with the same pool.

The pool ID is its filename and accepts lowercase letters, digits, underscores, and hyphens. Only direct-child `.yml` files in `reward-pools/` are scanned. Referenced pools must exist and be enabled.

## Reward library references

Add a library entry to `config.yml`, then reference it in a task:

```yaml
# config.yml
reward-library:
  starter_bread:
    type: item
    item: {material: BREAD, amount: 8}
```

```yaml
# Task reward fragment
rewards:
  food:
    reference: starter_bread
```

`reference` replaces the entire referencing entry. Adding a sibling `amount` does not override the library reward; other fields at the reference are replaced. Create another library reward or write the full entry for a different amount.

## Common mistakes

- Do not combine external `pool` and inline `random`.
- Pool candidates need `rate` and nested `rewards`. The objective-weight mapping from `pools/` is a different format.
- Missing, cyclic, or excessively deep references reject loading. Reloading validates enabled pools and their consumers.

Edit the pool in `reward-pools/*.yml`, then apply changes with `/uq reload`. Edits do not redraw existing task rewards or unlocked stage rewards; see [random rewards](random.md).
