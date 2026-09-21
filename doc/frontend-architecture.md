# 前端架构

`PuzzleProofBadge` 复用于题库和详情，仅在 proofStatus=PROVEN 且 winnerSide 为 USER/BOT 时显示必胜方；生命周期标签独立展示。

Vue 3 + TypeScript + Vite + Vue Router + Pinia + Element Plus + Axios。模块目录：`src/api/modules`（接口）、`router`（meta/守卫）、`stores`（登录、门户、题库、对局、任务）、`views`（hub/puzzles/game/analysis/admin）、`components`（cards、cards、poker、status、strategy）。

Axios 统一注入 Cookie/CSRF、requestId、错误映射和过期处理；页面不拼接 URL。题库/任务列表必须有 loading、empty、error retry、分页；表单同时做字段校验和服务端 fieldErrors 回显。路由 meta 只负责导航展示，服务端权限和所有权是最终依据。

对局 Store 只保存服务端权威状态与 stateVersion；牌桌以点击真实牌面形成用户意图，每次点击只切换一张实体牌，同点数的多张牌必须逐张点击，前端再按点数计数识别牌型。前端先做牌型/压牌校验，提交前再与服务端合法行动集合比对，后端仍是最终裁决。出牌后按响应替换快照。机器人/分析使用轮询，离开页面停止；可用动作由后端返回。`PROVEN`、`UNKNOWN`、`PARTIAL` 使用颜色和文字双重表达，UNKNOWN 禁止显示胜方。

Figma 设计入口与实现规则见 [figma-links.md](figma-links.md) 和 [design-spec.md](design-spec.md)。
