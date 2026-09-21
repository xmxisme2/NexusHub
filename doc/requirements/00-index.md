# 需求索引

基线：V1.0-design，2026-09-17。按模块阅读，需求编号用于接口、设计和测试追踪。

| 模块 | 文档 | 核心需求编号 | 依赖 |
|---|---|---|---|
| 范围 | [项目范围](01-scope.md) | SCOPE | — |
| 横切 | [权限、状态、页面、非功能](02-cross-cutting.md) | CROSS | 全部 |
| 账号与基础 | [identity](modules/identity.md) | IAM-01～05 | — |
| 门户与 OpsDesk | [hub](modules/hub.md) | HUB-01～04 | identity |
| 残局与题库 | [puzzle](modules/puzzle.md) | PUZ-01～06 | rules、identity |
| 斗地主规则 | [rules](modules/rules.md) | RULE-01～05 | — |
| 人机对局 | [game](modules/game.md) | GAME-01～06 | puzzle、rules、solver |
| 求解分析 | [analysis](modules/analysis.md) | ANA-01～07 | rules、solver |

跨模块契约：[API 索引](../api-contract.md)、[数据模型](../database-design.md)、[验收矩阵](../test-plan.md)。
