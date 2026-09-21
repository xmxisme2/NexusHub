# NexusHub 开发前产物入口

本目录是 V1 开发基线。当前交付为需求、契约、数据与设计，尚未实现运行中的应用。

## 推荐阅读顺序

1. [项目开发约定](../AGENTS.md)
2. [需求索引](requirements/00-index.md) → 当前模块需求
3. [斗地主规则与局面](domain/rules.md)
4. [数据库设计](database-design.md) 与 [建表 DDL](database/schema.sql)
5. [接口索引](api-contract.md)、[公共契约](api/common.md)、[OpenAPI 3.1](api/openapi.json)
6. [后端架构](backend-architecture.md)、[前端架构](frontend-architecture.md)
7. [Figma 入口](figma-links.md)、[界面规格](design-spec.md)
8. [开发准备和里程碑](development-prep.md)、[验收计划](test-plan.md)
9. [当前状态](project-status.md)、[待办](todo.md)

[初版整体方案](01-NexusHub第一版方案与整体架构.md) 保留为背景说明；本轮细化文档替代其接口和数据草案。字段类型以 OpenAPI / DDL 为准，规则和权限以模块需求为准，发生冲突必须同步修正。

## 本轮决策

- 当前优先交付斗地主残局模块；OpsDesk 已作为独立外链接入门户，SSO、代理、健康探测和管理员动态入口配置仍延后。
- 首版门户及业务需登录；无自助注册，管理员创建普通账号。
- 普通用户维护自己的草稿、创建自己的对局/分析；管理员可以发布和归档题目、管理系统入口和账号。
- 题目保存产生不可变版本；已发布版本继续可见，新的草稿不影响已有对局。
- 任务结束状态、胜负证明、策略材料完整度相互独立。
- 首版最优挑战仅允许完整策略可用的题目；限时训练可使用未证明最优的合法行动。
- 重开创建新对局，不回写旧对局；首版无悔棋。
- OpsDesk 使用 `https://www.xmxisme.com/opsdesk/` 外链，新标签页打开且不共享 NexusHub 会话；由于外部站点不提供已确认的集成协议，本轮不做 SSO 或业务代理。
- 原有三人明牌/地主农民模型仅保留在历史方案中；当前需求为用户与机器人两名参与者，规则已冻结。
- 当前规则已冻结：四带二固定不可出，其他标准牌型按正常斗地主规则开放；不提供 `disabledTypes` 配置。
- 本地测试账号由本地种子配置提供；数据库配置与 L001 种子见 `doc/config/application-test.yml.example`、`doc/database/seed-test.sql`。
- OpenAPI 是可导入的开发契约，SQL 是待在空测试库执行验证的建表草案；不代表已经部署。
