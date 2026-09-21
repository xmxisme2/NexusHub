# 数据库设计

目标 MySQL 8.4、InnoDB、UTC、utf8mb4。详细字段字典见 [field-dictionary.md](database/field-dictionary.md)，建表草案见 [schema.sql](database/schema.sql)。这两份文件是设计产物，必须在专用空库执行验证后才能作为迁移基线。已应用库的增量变更放在 [database/migrations](database/migrations/)；当前 `V2__allow_optimal_decision_quality.sql` 放开严格最优机器人行动事件的审计值。

## 通用规范

- 所有业务表包含 `id/create_time/update_time/create_by/update_by/deleted`；deleted 仅取 0/1，业务删除逻辑删除。
- 表字段 snake_case，API JSON camelCase，ID 用字符串。
- 查询使用 MyBatis XML，字段显式列出，禁止 SELECT *；分页使用 PageHelper + PageResult。
- 版本、状态、请求幂等和外键约束由服务端与数据库共同保护；JSON 领域内容必须经过 Java 规则引擎校验。

## 关系与生命周期

`sys_user → puzzle → puzzle_version`；puzzle 的 latest/published 指针必须指向同一 puzzle 的版本。`puzzle_version → game_session → game_action`，对局与行动保存不可变快照。`solve_task` 复制题目/对局输入，`solve_result → strategy_node → strategy_edge` 保存同一结果内的证明 DAG。`audit_log` 与 `idempotency_record` 记录关键写操作和重试语义。

深度搜索不持有数据库事务；任务领取用状态条件更新、租约和 leaseToken。对局行动、事件与 stateVersion 在同一短事务内提交。双向复合外键在全部表建立后添加，详见 SQL 尾部。

## 一致性检查

DDL 中的 CHECK 只做基础约束；牌型、牌数、状态可达性、证明覆盖由服务层和纯 Java 规则/verifier 校验。`solve_result` 中 UNKNOWN 不得填 winner_side/proof_value；COMPLETE 必须有 PROVEN 根节点和 verified_time。
