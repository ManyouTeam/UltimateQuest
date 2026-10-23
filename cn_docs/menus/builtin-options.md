# ⚙️ 内置菜单选项

本页按菜单文件列出配置项。完整菜单示例见[菜单配置](../configs/menus.md)。所有路径都位于 `plugins/UltimateQuest/menus/`。

## `main.yml`

普通任务列表，默认左键接取、查看或领奖，右键请求放弃。前置任务必须领奖才解锁后续。

打开方式：`/uq`

下面列出默认内容区的符号。字母只与对应布局绑定，可以自行调整，但布局与 contents 必须一致。

| 符号 | 类型 | 视图/轨道 | 用途 |
| --- | --- | --- | --- |
| `Q` | `quests` | always / — | 普通任务定义与已接取实例 |

## `period.yml`

当前周期委托。period-filter 按周期配置 ID 分组，与任务分类不同；默认开启。未指定 groups 时优先列 daily、weekly、monthly，再列其他已启用或已分配的组。

打开方式：`/uq period`

下面列出默认内容区的符号。字母只与对应布局绑定，可以自行调整，但布局与 contents 必须一致。

| 符号 | 类型 | 视图/轨道 | 用途 |
| --- | --- | --- | --- |
| `Q` | `period` | always / — | 已分配周期实例 |

## `scroll.yml`

当前携带卷轴。scroll-filter 按配置筛选，未指定 groups 时优先 novice、mining、adventure、veteran。显示条目不代表必定能兑换，领奖仍核对令牌、归属和时限。

打开方式：`/uq scroll`

下面列出默认内容区的符号。字母只与对应布局绑定，可以自行调整，但布局与 contents 必须一致。

| 符号 | 类型 | 视图/轨道 | 用途 |
| --- | --- | --- | --- |
| `Q` | `scroll` | always / — | 携带卷轴实例 |

## `battlepass.yml`

包含战令目录、等级详情和挑战三个视图。目录使用 catalog-item，阶段使用 state-items 或 item。free 与 premium 只选择奖励区，不授予高级资格。

打开方式：`/uq battlepass [ID]`

下面列出默认内容区的符号。字母只与对应布局绑定，可以自行调整，但布局与 contents 必须一致。

| 符号 | 类型 | 视图/轨道 | 用途 |
| --- | --- | --- | --- |
| `J` | `challenge` | challenges / — | 选中战令/赛季挑战 |
| `A` | `battlepass` | catalog / — | 战令目录或阶段奖励 |
| `Q` | `battlepass` | detail / free | 战令目录或阶段奖励 |
| `L` | `battlepass` | detail / free | 战令目录或阶段奖励 |
| `R` | `battlepass` | detail / premium | 战令目录或阶段奖励 |

## `milestone.yml`

包含目录和阶段详情两个视图。阶段奖励使用 default 轨道；没有战令的高级奖励区。

打开方式：`/uq milestone [ID]`

下面列出默认内容区的符号。字母只与对应布局绑定，可以自行调整，但布局与 contents 必须一致。

| 符号 | 类型 | 视图/轨道 | 用途 |
| --- | --- | --- | --- |
| `A` | `milestone` | catalog / — | 里程碑目录或阶段奖励 |
| `L` | `milestone` | detail / default | 里程碑目录或阶段奖励 |

## 公共字段与物品外观

| 字段 | 作用 |
| --- | --- |
| `enabled`、`permission` | 是否启用与打开权限，禁用不能被权限绕过。 |
| `title`、`layout` | 普通标题及布局，1–6 行，每行 9 个符号。 |
| `catalog-title`、`catalog-layout` | 成长目录的标题及布局。 |
| `detail-title`、`detail-layout` | 已选战令或里程碑的阶段详情。 |
| `challenge-title`、`challenge-layout` | 战令挑战页，标题未设时回退到通用标题或默认值。 |
| `contents.<符号>` | 动态内容、筛选、卡片与动作。 |
| `items.<符号>` | 固定按钮、外观与动作。 |
| `category-filter` | 继承 config.yml 的 category-menu，可逐菜单覆盖。 |

卡片配置使用 [ItemFormat](../shared-formats/itemformat-tm.md)。内容区字段如 include/exclude、track、state-items 的用法见[菜单配置](../configs/menus.md)。

## 状态筛选的含义

`view-filter.values` 接受 ALL、ACTIVE、AVAILABLE、COMPLETED、CLAIMABLE、CLAIMED、LOCKED、INACTIVE，控制 cycle_view 的顺序。

| 状态筛选 | 包含的内容 |
| --- | --- |
| `ALL` | 当前内容区允许的全部条目。 |
| `ACTIVE` | 进行中。 |
| `AVAILABLE` | 尚未接取的 AVAILABLE / LOCKED 普通条目，不保证通过所有接取检查。 |
| `COMPLETED` | COMPLETED、CLAIMING、CLAIMED、REVIEW。它比“可领奖”范围大。 |
| `CLAIMABLE` | 已完成任务或状态为 AVAILABLE 的阶段。 |
| `CLAIMED` | 已领奖。 |
| `LOCKED` | 未解锁或需要高级资格。 |
| `INACTIVE` | 非活动条目。 |

没有合法覆盖时，代码默认：卷轴为 ALL/ACTIVE/CLAIMABLE；周期与挑战为 ALL/ACTIVE/CLAIMABLE/CLAIMED；其他为 ALL/ACTIVE/AVAILABLE/COMPLETED。默认文件可自行覆盖，例如 main 还包含 CLAIMABLE 和 CLAIMED。

`category-filter.hide-empty` 的代码默认值为 false，内置 main/scroll 开启。它按当前筛选范围隐藏空分类。周期和卷轴按钮的分组、外观与动作见[菜单配置](../configs/menus.md)。

## 公共显示设置

```yaml
# menus/settings.yml
anti-dupe-checker: true
lore-wrap-length: 34
progress-bar:
  length: 12
  complete: '&a|'
  remaining: '&7|'
  suffix: ''
```

这是显示设置片段。防复制检查正常使用时保留开启；lore-wrap-length 控制描述换行，progress-bar 控制进度条。settings 不能打开为菜单。

## 可用变量

| 范围 | 变量 |
| --- | --- |
| 公共 | `{player}`, `{menu}`, `{page}`, `{pages}`, `{view}`, `{count}`, `{active}`, `{claimable}`, `{claimed}`, `{available}`, `{locked}`, `{category-id}`, `{category-name}`, `{challenge-group}`, `{period-id}`, `{period-group}`, `{scroll-id}`, `{scroll-group}` |
| 任务/阶段 | `{id}`, `{name}`, `{description}`, `{objectives}`, `{state}`, `{hint}`, `{prerequisites}`, `{categories}`, `{level}`, `{total}`, `{required}`, `{progress}`, `{progress-required}`, `{progress-percent}`, `{progress-bar}`, `{track}`, `{season}`, `{rewards}` |
| 分配信息 | `{rarity}`, `{difficulty}`, `{points}`, `{group}`, `{unlock-week}`, `{source}`, `{expires}`, `{expires-in}` |
| 选中成长 | `{next-required}`, `{remaining-xp}`, `{season-time}` |

把多行变量 `{description}`、`{objectives}`、`{rewards}` 分别放在独立 Lore 行。条目变量优先于菜单汇总值，不同视图中可用信息不同。分类卡的专用变量见[分类](../configs/categories.md)，点击方式见[菜单动作](actions.md)。
