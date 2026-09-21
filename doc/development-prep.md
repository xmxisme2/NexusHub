# 开发准备与顺序

## P0 契约冻结

确认 CLASSIC_V1 牌型边界、默认管理员创建方式；四带二固定排除，不提供 disabledTypes 配置。以 OpenAPI、DDL 和规则正文为基线，先准备一个最小 L001，再逐步通过系统添加更多残局。

## P1 工程骨架

已建立 `backend` Spring Boot Maven 工程和 `frontend` Vue Vite 工程，接入统一响应、异常、会话/CSRF、MyBatis XML、MySQL 初始化脚本和健康检查。当前仓库使用单后端模块；格式化和完整迁移工具仍按后续工程需要补充。

## P2 规则与题库

先完成无剪枝 rules 和 JUnit，再做题目编辑/版本/发布 API 与页面。规则模块不能依赖 Spring；所有保存通过同一校验器。

## P3 对局

实现后端权威状态、幂等、乐观锁、机器人持久任务和桌面牌桌；完成规则回归后再开放训练。

## P4 求解与证明

穷举参考实现 → 记忆化 → Alpha-Beta/行动排序 → 预算/取消/租约 → proof DAG/verifier。先达到小残局一致性，再开启更大题库。

## P5 加固

已接入 OpenAPI 结构/规则校验、SQL 静态校验和 MySQL 空库集成校验，并纳入 CI；权限、恢复、并发和基础资源保护随业务模块继续补齐。初版不做性能指标展示和复杂性能基准。确认 OpsDesk 仅跳转或另行批准 SSO。完成后更新 project-status 和 todo。

## 环境变量（草案）

`NEXUSHUB_DB_URL`、`NEXUSHUB_DB_USERNAME`、`NEXUSHUB_DB_PASSWORD`、`NEXUSHUB_SESSION_SECRET`、`NEXUSHUB_SOLVER_*`（V1 `NEXUSHUB_SOLVER_MAX_CONCURRENCY` 最大有效值为 5）。敏感值只进部署密钥，不进仓库。本地测试配置见 `doc/config/application-test.yml.example`，本地种子见 `doc/database/seed-test.sql`。OpsDesk 相关变量暂不启用。
