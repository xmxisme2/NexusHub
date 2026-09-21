# NexusHub 界面规格

## 视觉基础

- 画布：暖白 `#F6F3EC`；表面 `#FFFFFF`；深蓝文字 `#0E1A2E`；品牌深蓝 `#0E294A`；青绿 `#0D9488`；成功 `#1F8F63`；警告 `#E08421`；危险 `#C44040`。
- 间距：4/8/16/24/32/48；圆角：卡片 24、输入/按钮 10、状态胶囊 99、扑克牌 8～9。
- 字体 Inter；标题 32～42 Semi Bold，正文 14～16 Regular，辅助 12～13。
- 卡片有浅灰边框，不依赖大面积阴影；按钮最小触控高度 40px。

## 页面实现映射

| 页面 | 关键区域 | API/状态 |
|---|---|---|
| Portal | Header、可点击系统卡片、OpsDesk 外链 | 内部斗地主路由 + 构建配置的 `VITE_OPS_DESK_URL` |
| Puzzle Library | Tab、搜索筛选、题目行、证明标签 | `/api/puzzles/search`，proofStatus 独立显示 |
| Puzzle Editor | 局面编辑、剩余牌、规则错误、保存版本 | validate/save，服务端 fieldErrors；四带二固定不可选 |
| Game Table | USER/BOT 两座位、牌墩、用户手牌、行动、分析侧栏 | Game + legal-moves + play/pass/hint |
| Analysis | 结论摘要、策略树、节点覆盖、统计 | SolveTask/SolveResult/strategy children |
| Admin | 系统入口、URL 来源边界、账号状态、审计 | hub/user 管理接口 |
| Login | 品牌说明、账号密码、登录失败、会话过期 | `/api/auth/csrf`、`/api/auth/login`、`/api/auth/me` |
| States & Components | 排队/运行/未知/已证明、空态、错误、发布确认 | SolveTask、SolveResult、publish/archive |

## 状态与文案规则

残局库以整张卡片链接进入详情，移除独立“查看局面”按钮；卡片保持悬停反馈并提供清晰的键盘焦点边框。该交互尚未同步 Figma。

残局详情顶部提供“← 返回残局库”按钮；最新草稿的管理员操作区提供“发布残局”，并说明发布后进入公开题库、不要求已证明。普通用户显示联系管理员发布的提示；发布有提交中、成功、失败及刷新状态入口。该详情补充交互以实现和需求为准，尚未同步到 Figma 节点。

`PROVEN` 显示“已证明 · 用户必胜/机器人必胜”；`UNKNOWN` 显示“尚未证明”，不显示 winnerSide；`PARTIAL` 显示“策略材料部分生成”；`COMPLETE` 仅在 proof=PROVEN 时显示“完整策略”。多分支证明最多展示 5 条已验证路线。初版不在页面展示性能指标，任务 `TIME_LIMIT/NODE_LIMIT/MEMORY_LIMIT` 显示资源原因，不显示失败的获胜方。

门户的斗地主和 OpsDesk 入口均以整张卡片作为可交互区域，不再在卡片底部放置独立入口按钮。顶部 Hero 信息随鼠标悬停或键盘聚焦的系统卡片切换，展示该系统自己的简介和指标。OpsDesk 卡片使用构建配置的 `VITE_OPS_DESK_URL`（默认 `https://www.xmxisme.com/opsdesk/`），通过新标签页和 `noopener noreferrer` 打开，不显示在线/健康状态，也不传递 NexusHub Cookie 或 Token。对局提示写明“基于当前预算的建议”，不能把启发式建议称作最优或必胜。

## 组件清单

`AppHeader`、`SystemCard`、`StatusBadge`、`PrimaryButton`、`FilterBar`、`PuzzleRow`、`PokerCard`、`SeatPanel`、`ActionBar`、`ProofSummary`、`StrategyNodeRow`、`TaskProgress`、`FieldErrorList`、`EmptyState`、`ErrorRetry`。组件优先通过 CSS 变量映射 Figma 令牌；扑克牌点数为文本/计数模型，花色不参与后端规则。

## 桌面交互与可访问性

1440px 为设计基准，本轮不设计移动端交互和页面。颜色外必须显示文字状态；焦点边框使用青绿 2px；禁用动作显示原因；所有按钮和卡片入口可键盘操作。对局和分析的状态变化优先显示在页面主区域，不依赖 Toast 承载关键结论。
