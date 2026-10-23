# 📝 ItemFormat

ItemFormat 构建奖励物品、卷轴、图标、保存的 base-item 和菜单模板。例如：

```yaml
item:
  material: DIAMOND_PICKAXE
  amount: 1
  name: '&bSurvey pickaxe'
  lore: ['&7A reward from UltimateQuest']
  enchants:
    minecraft:efficiency: 3
  change-item:
    repair-damage: 50
```

## ⚙️ 字段

material 可为原版材质或已保存物品 ID；未设置时 hook-plugin/hook-item 可选提供器物品，item 可放序列化物品。amount 为整数或含端点 min~max 区间，默认 1，不是通用数学表达式字段。name（别名 display）、lore、enchants、stored-enchants、flags、custom-model-data 和受支持组件字段修改元数据，组件取决于服务端实现。

## 🔧 修改与导出

change-item 在构建后执行[物品修改](change-item-rules.md)。可用 `/uq generateitemformat` 查看当前导出，或 `/uq saveitem <id> itemformat` 保存复用定义。匹配用另一套 [MatchItemFormat](match-item-format/README.md)，构建 material 为标量，匹配 material 为列表。
