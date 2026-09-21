# Figma 设计入口

文件：[NexusHub 产品界面设计](https://www.figma.com/design/dFP4sUJTGWKyqXrSioVBZq)

文件 Key：`dFP4sUJTGWKyqXrSioVBZq`。该文件为本轮新建的可编辑 Design 文件，当前页面：Cover、Foundations、Login、Portal、Puzzle Library、Puzzle Editor、Game Table、Analysis、Admin、States & Components；Figma 默认 Page 1 保留为空。

已生成并截图验证的节点：

| 页面 | 节点 | 设计状态 |
|---|---|---|
| Portal | `1:27` | 桌面门户，两张系统卡片，OpsDesk 外链 |
| Puzzle Library | `2:2` | 题库、筛选、已证明/尚未证明标签 |
| Game Table | `3:2` | USER/BOT 双人明牌牌桌、PASS、当前牌墩和分析侧栏 |
| Analysis | `3:70` | 已证明结论、策略完整度、证明树 |
| Puzzle Editor | `4:2` | 两家手牌编辑、先后手、牌量校验和四带二固定不可出说明 |
| Admin | `4:121` | OpsDesk 地址来源边界、账号状态 |
| Cover | `11:2` | 深色科技感品牌封面、系统地图、产品原则 |
| Foundations | `11:38` | 色彩、字体、间距、交互原则 |
| Login | `11:91` | 完整版登录页、错误/会话过期文案 |
| States & Components | `11:118` | 任务状态、证明状态、空态、错误态、发布确认 |

截图验证已通过 Figma `get_screenshot` 调用，返回短期截图 URL；没有把短期截图地址当作永久设计入口。设计令牌已在文件中创建：NexusHub Color、NexusHub Spacing。

当前已覆盖桌面端核心页面和主要业务状态；移动端不在本轮范围。组件页目前是实现参考样板，尚未为每个组件建立独立 Component Set 和全量交互原型。
