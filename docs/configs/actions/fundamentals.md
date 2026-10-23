# ✨ Text and visual actions

These actions run on menu clicks. Each example is an action fragment for `items.<symbol>.actions` or `contents.<symbol>.actions`; actions are not task reward types. See [action basics](README.md) for placement.

## `message`

Sends chat feedback to the clicking player. Text variables are resolved using the current menu or entry.

| Field | Default | Description |
| --- | --- | --- |
| `message` | Required | Text to send. |

```yaml
type: message
message: '&aHello {player}.'
```

## `action_bar`

Shows a short message above the hotbar. It changes presentation only; it does not create tasks or issue rewards.

| Field | Default | Description |
| --- | --- | --- |
| `message` | Required | Action-bar text. |

```yaml
type: action_bar
message: '&e{claimable} rewards ready.'
```

## `announcement`

Broadcasts a chat announcement to online players. Player variables use the initiating player and context, not a separate task for each recipient.

| Field | Default | Description |
| --- | --- | --- |
| `message` | Required | Announcement text. |

```yaml
type: announcement
message: '&e{player} is viewing quests.'
```

## `title`

Shows a screen title and subtitle. Supply all three timing fields in ticks, even when zero. At 20 TPS, 20 ticks is about one second.

| Field | Default | Description |
| --- | --- | --- |
| `main-title` | Required | Main text. |
| `sub-title` | Required | Subtitle text. |
| `fade-in` | Required | Fade-in ticks. |
| `stay` | Required | Display ticks. |
| `fade-out` | Required | Fade-out ticks. |

```yaml
type: title
main-title: '&aQuests'
sub-title: '&7Choose a task'
fade-in: 10
stay: 40
fade-out: 10
```

## `sound`

Plays a sound at the player. `volume` controls volume and `pitch` controls pitch; the server must support the sound name.

| Field | Default | Description |
| --- | --- | --- |
| `sound` | Required | Sound key/name. |
| `volume` | 1 | Volume. |
| `pitch` | 1 | Pitch. |

```yaml
type: sound
sound: minecraft:entity.player.levelup
volume: 1
pitch: 1
```

## `particle`

Spawns particles one block above the player. Supply count, all three offsets, and speed. Particle names and additional-data requirements depend on the server version.

| Field | Default | Description |
| --- | --- | --- |
| `particle` | Required | Server particle name. |
| `count` | Required | Particle count. |
| `offset-x / offset-y / offset-z` | Required | Three spread components. |
| `speed` | Required | Particle extra/speed parameter. |

```yaml
type: particle
particle: HAPPY_VILLAGER
count: 8
offset-x: 0.3
offset-y: 0.3
offset-z: 0.3
speed: 0
```

## `effect`

Applies a potion effect. At 20 TPS, `duration: 200` is about ten seconds. `level: 0` means level I; `1` means level II.

| Field | Default | Description |
| --- | --- | --- |
| `potion` | Required | Registered potion-effect key. |
| `duration` | Required | Duration in ticks. |
| `level` | Required | Zero-based amplifier. |
| `ambient / particles / icon` | true | Display flags. |

```yaml
type: effect
potion: minecraft:speed
duration: 200
level: 0
particles: true
```

## Configuration validation

Run `/uq reload`, reopen the menu, and click the button. Task actions need a clicked task or selected pass/milestone; naming a static button after a task does not establish that association. Navigation retains the current menu-permission bypass, while task and reward checks still apply.
