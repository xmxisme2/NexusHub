# 表字段字典

由 `doc/tooling/generate_contracts.py` 与 schema.sql 同源生成。字段类型、可空与默认值以下表为准，跨字段规则见 database-design.md。

所有表使用 InnoDB、utf8mb4_0900_ai_ci；技术Key覆盖为ascii_bin。

## sys_user

本系统账号；固定单角色

| 字段 | SQL 类型与约束 | 说明 |
|---|---|---|
| id | `BIGINT NOT NULL` | 应用生成的正整数ID |
| create_time | `DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)` | UTC创建时间 |
| update_time | `DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)` | UTC更新时间，应用显式维护 |
| create_by | `BIGINT NULL` | 创建者，系统任务可空 |
| update_by | `BIGINT NULL` | 最后更新者 |
| deleted | `TINYINT NOT NULL DEFAULT 0` | 逻辑删除0正常1删除 |
| username | `VARCHAR(64) NOT NULL` | 规范化用户名，删除后不复用 |
| display_name | `VARCHAR(64) NOT NULL` | 显示名 |
| password_hash | `VARCHAR(255) NOT NULL` | BCrypt散列，不对外返回 |
| role | `VARCHAR(16) NOT NULL` | GUEST、USER或ADMIN |
| status | `VARCHAR(16) NOT NULL DEFAULT 'ENABLED'` | 账号状态 |
| row_version | `INT NOT NULL DEFAULT 0` | 并发版本 |
| credential_version | `INT NOT NULL DEFAULT 0` | 重置/停用时递增以撤销会话 |

索引与约束：

- `UNIQUE KEY uk_user_username (username)`
- `CHECK (role IN ('GUEST','USER','ADMIN'))`
- `CHECK (status IN ('ENABLED','DISABLED'))`

## hub_system

可扩展系统卡片

| 字段 | SQL 类型与约束 | 说明 |
|---|---|---|
| id | `BIGINT NOT NULL` | 应用生成的正整数ID |
| create_time | `DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)` | UTC创建时间 |
| update_time | `DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)` | UTC更新时间，应用显式维护 |
| create_by | `BIGINT NULL` | 创建者，系统任务可空 |
| update_by | `BIGINT NULL` | 最后更新者 |
| deleted | `TINYINT NOT NULL DEFAULT 0` | 逻辑删除0正常1删除 |
| code | `VARCHAR(64) NOT NULL` | 不可变唯一编码 |
| name | `VARCHAR(64) NOT NULL` | 名称 |
| description | `VARCHAR(300) NOT NULL DEFAULT ''` | 简介 |
| icon_key | `VARCHAR(32) NOT NULL` | 内置图标Key |
| entry_type | `VARCHAR(16) NOT NULL` | INTERNAL或EXTERNAL |
| route | `VARCHAR(255) NULL` | 内部登记路由 |
| external_url | `VARCHAR(2048) NULL` | 批准的外部地址；空代表待配置 |
| visible_roles | `JSON NOT NULL` | 可见角色数组 |
| sort_order | `INT NOT NULL DEFAULT 0` | 升序排序 |
| enabled | `TINYINT NOT NULL DEFAULT 1` | 是否启用 |
| row_version | `INT NOT NULL DEFAULT 0` | 并发版本 |

索引与约束：

- `UNIQUE KEY uk_system_code (code)`
- `KEY idx_system_order (enabled, deleted, sort_order, id)`
- `CHECK (enabled IN (0,1))`
- `CHECK ((entry_type='INTERNAL' AND route IS NOT NULL AND external_url IS NULL) OR (entry_type='EXTERNAL' AND route IS NULL))`

## puzzle

题目生命周期与版本指针

| 字段 | SQL 类型与约束 | 说明 |
|---|---|---|
| id | `BIGINT NOT NULL` | 应用生成的正整数ID |
| create_time | `DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)` | UTC创建时间 |
| update_time | `DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)` | UTC更新时间，应用显式维护 |
| create_by | `BIGINT NULL` | 创建者，系统任务可空 |
| update_by | `BIGINT NULL` | 最后更新者 |
| deleted | `TINYINT NOT NULL DEFAULT 0` | 逻辑删除0正常1删除 |
| owner_id | `BIGINT NOT NULL` | 所有者 |
| status | `VARCHAR(16) NOT NULL DEFAULT 'DRAFT'` | DRAFT/PUBLISHED/ARCHIVED |
| latest_version_id | `BIGINT NULL` | 最新草稿；仅创建事务中短暂允许空 |
| published_version_id | `BIGINT NULL` | 公开版本，归档保留，恢复清空 |
| row_version | `INT NOT NULL DEFAULT 0` | 每次保存或状态动作递增 |

索引与约束：

- `KEY idx_puzzle_owner (owner_id, deleted, update_time, id)`
- `KEY idx_puzzle_status (status, deleted, update_time, id)`
- `FOREIGN KEY (owner_id) REFERENCES sys_user(id)`
- `CHECK (status IN ('DRAFT','PUBLISHED','ARCHIVED'))`
- `CHECK (status<>'PUBLISHED' OR published_version_id IS NOT NULL)`

## puzzle_version

不可变题目内容和局面

| 字段 | SQL 类型与约束 | 说明 |
|---|---|---|
| id | `BIGINT NOT NULL` | 应用生成的正整数ID |
| create_time | `DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)` | UTC创建时间 |
| update_time | `DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)` | UTC更新时间，应用显式维护 |
| create_by | `BIGINT NULL` | 创建者，系统任务可空 |
| update_by | `BIGINT NULL` | 最后更新者 |
| deleted | `TINYINT NOT NULL DEFAULT 0` | 逻辑删除0正常1删除 |
| puzzle_id | `BIGINT NOT NULL` | 所属题目 |
| version_no | `INT NOT NULL` | 题内递增版本号 |
| title | `VARCHAR(120) NOT NULL` | 标题 |
| description | `TEXT NOT NULL` | 说明，API最多2000字符 |
| tags | `JSON NOT NULL` | 去重标签数组，最多10个 |
| difficulty | `VARCHAR(16) NOT NULL` | 人工难度 |
| allowed_first_seats | `JSON NOT NULL` | 允许的先手数组，只能含0(USER)/1(BOT) |
| ruleset_version | `VARCHAR(32) NOT NULL` | CLASSIC_V1 |
| schema_version | `INT NOT NULL DEFAULT 1` | 局面序列化版本 |
| initial_state | `JSON NOT NULL` | 完整双人GameState |
| state_hash | `CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL` | 规范化状态SHA256；命中后还需核对原文 |

索引与约束：

- `UNIQUE KEY uk_puzzle_version (puzzle_id, version_no)`
- `UNIQUE KEY uk_version_parent (puzzle_id, id)`
- `KEY idx_version_hash (state_hash)`
- `FOREIGN KEY (puzzle_id) REFERENCES puzzle(id)`
- `CHECK (difficulty IN ('EASY','MEDIUM','HARD'))`
- `CHECK (version_no>0)`

## game_session

对局权威快照

| 字段 | SQL 类型与约束 | 说明 |
|---|---|---|
| id | `BIGINT NOT NULL` | 应用生成的正整数ID |
| create_time | `DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)` | UTC创建时间 |
| update_time | `DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)` | UTC更新时间，应用显式维护 |
| create_by | `BIGINT NULL` | 创建者，系统任务可空 |
| update_by | `BIGINT NULL` | 最后更新者 |
| deleted | `TINYINT NOT NULL DEFAULT 0` | 逻辑删除0正常1删除 |
| owner_id | `BIGINT NOT NULL` | 用户 |
| puzzle_version_id | `BIGINT NOT NULL` | 绑定版本 |
| first_seat | `TINYINT NOT NULL` | 首手座位0(USER)/1(BOT) |
| mode | `VARCHAR(16) NOT NULL` | TRAINING/OPTIMAL |
| status | `VARCHAR(16) NOT NULL DEFAULT 'ACTIVE'` | 对局状态 |
| initial_state | `JSON NOT NULL` | 创建快照 |
| current_state | `JSON NOT NULL` | 当前完整状态 |
| state_version | `INT NOT NULL DEFAULT 0` | 行动/放弃时递增 |
| winner_side | `VARCHAR(16) NULL` | 终局阵营USER/BOT |
| robot_status | `VARCHAR(16) NOT NULL DEFAULT 'IDLE'` | 机器人调度状态 |
| hint_count | `INT NOT NULL DEFAULT 0` | 使用提示次数 |
| proof_result_id | `BIGINT NULL` | 当前最优模式引用的结果，后置外键 |
| parent_game_id | `BIGINT NULL` | 重开的来源 |
| finished_time | `DATETIME(3) NULL` | 终态时间 |

索引与约束：

- `KEY idx_game_owner (owner_id, status, deleted, update_time, id)`
- `FOREIGN KEY (owner_id) REFERENCES sys_user(id)`
- `FOREIGN KEY (puzzle_version_id) REFERENCES puzzle_version(id)`
- `FOREIGN KEY (parent_game_id) REFERENCES game_session(id)`
- `CHECK (first_seat BETWEEN 0 AND 1)`
- `CHECK (mode IN ('TRAINING','OPTIMAL'))`
- `CHECK (status IN ('ACTIVE','FINISHED','ABANDONED'))`
- `CHECK ((status='FINISHED' AND winner_side IS NOT NULL AND winner_side IN ('USER','BOT')) OR (status<>'FINISHED' AND winner_side IS NULL))`
- `CHECK (robot_status IN ('IDLE','QUEUED','THINKING','NEEDS_PROOF','FAILED'))`

## game_action

不可变出牌/不出事件

| 字段 | SQL 类型与约束 | 说明 |
|---|---|---|
| id | `BIGINT NOT NULL` | 应用生成的正整数ID |
| create_time | `DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)` | UTC创建时间 |
| update_time | `DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)` | UTC更新时间，应用显式维护 |
| create_by | `BIGINT NULL` | 创建者，系统任务可空 |
| update_by | `BIGINT NULL` | 最后更新者 |
| deleted | `TINYINT NOT NULL DEFAULT 0` | 逻辑删除0正常1删除 |
| game_id | `BIGINT NOT NULL` | 所属对局 |
| sequence_no | `INT NOT NULL` | 从1连续递增 |
| actor_seat | `TINYINT NOT NULL` | USER=0/BOT=1 |
| actor_type | `VARCHAR(16) NOT NULL` | HUMAN/BOT |
| move_json | `JSON NOT NULL` | 规范化Move |
| before_version | `INT NOT NULL` | 前版本 |
| after_version | `INT NOT NULL` | 后版本 |
| after_state | `JSON NOT NULL` | 行动后的完整快照 |
| decision_quality | `VARCHAR(16) NOT NULL` | OPTIMAL/PROVEN/HEURISTIC/HUMAN |
| request_key | `VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL` | 人类幂等Key或机器人作业Key |

索引与约束：

- `UNIQUE KEY uk_action_sequence (game_id, sequence_no)`
- `UNIQUE KEY uk_action_request (game_id, request_key)`
- `FOREIGN KEY (game_id) REFERENCES game_session(id)`
- `CHECK (actor_seat BETWEEN 0 AND 1)`
- `CHECK (after_version=before_version+1)`
- `CHECK (actor_type IN ('HUMAN','BOT'))`
- `CHECK (decision_quality IN ('OPTIMAL','PROVEN','HEURISTIC','HUMAN'))`

## game_bot_job

机器人持久调度，可重启恢复

| 字段 | SQL 类型与约束 | 说明 |
|---|---|---|
| id | `BIGINT NOT NULL` | 应用生成的正整数ID |
| create_time | `DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)` | UTC创建时间 |
| update_time | `DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)` | UTC更新时间，应用显式维护 |
| create_by | `BIGINT NULL` | 创建者，系统任务可空 |
| update_by | `BIGINT NULL` | 最后更新者 |
| deleted | `TINYINT NOT NULL DEFAULT 0` | 逻辑删除0正常1删除 |
| game_id | `BIGINT NOT NULL` | 对局 |
| state_version | `INT NOT NULL` | 依据的版本 |
| status | `VARCHAR(16) NOT NULL DEFAULT 'QUEUED'` | QUEUED/RUNNING/DONE/CANCELLED/FAILED |
| lease_token | `VARCHAR(64) NULL` | 每次领取生成的新隔离令牌 |
| lease_until | `DATETIME(3) NULL` | 租约失效时间 |
| attempt_count | `INT NOT NULL DEFAULT 0` | 领取次数 |
| available_time | `DATETIME(3) NOT NULL` | 重试可领取时间 |

索引与约束：

- `UNIQUE KEY uk_bot_version (game_id, state_version)`
- `KEY idx_bot_claim (status, available_time, lease_until)`
- `FOREIGN KEY (game_id) REFERENCES game_session(id)`
- `CHECK (status IN ('QUEUED','RUNNING','DONE','CANCELLED','FAILED'))`

## solve_task

持久分析任务及计算快照

| 字段 | SQL 类型与约束 | 说明 |
|---|---|---|
| id | `BIGINT NOT NULL` | 应用生成的正整数ID |
| create_time | `DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)` | UTC创建时间 |
| update_time | `DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)` | UTC更新时间，应用显式维护 |
| create_by | `BIGINT NULL` | 创建者，系统任务可空 |
| update_by | `BIGINT NULL` | 最后更新者 |
| deleted | `TINYINT NOT NULL DEFAULT 0` | 逻辑删除0正常1删除 |
| owner_id | `BIGINT NOT NULL` | 提交者 |
| purpose | `VARCHAR(16) NOT NULL` | ANALYSIS/HINT |
| source_type | `VARCHAR(24) NOT NULL` | PUZZLE_VERSION/GAME/CUSTOM |
| puzzle_version_id | `BIGINT NULL` | 题目来源 |
| game_id | `BIGINT NULL` | 对局来源 |
| game_state_version | `INT NULL` | 对局来源版本 |
| state_snapshot | `JSON NOT NULL` | 不可变求解输入 |
| state_hash | `CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL` | 规范化摘要 |
| ruleset_version | `VARCHAR(32) NOT NULL` | 规则版本 |
| solver_version | `VARCHAR(64) NOT NULL` | 运行算法版本，领取时固定 |
| task_status | `VARCHAR(16) NOT NULL DEFAULT 'QUEUED'` | 任务状态 |
| time_limit_ms | `INT NOT NULL` | 实际预算 |
| node_limit | `BIGINT NOT NULL` | 节点上限 |
| memory_limit_mb | `INT NOT NULL` | 搜索缓存预算 |
| termination_reason | `VARCHAR(24) NULL` | 结束原因 |
| lease_token | `VARCHAR(64) NULL` | 领取隔离令牌 |
| lease_until | `DATETIME(3) NULL` | 租约截止 |
| attempt_count | `INT NOT NULL DEFAULT 0` | 领取次数 |
| available_time | `DATETIME(3) NOT NULL` | 排队/重试时间 |
| start_time | `DATETIME(3) NULL` | 首次开始时间 |
| finish_time | `DATETIME(3) NULL` | 结束时间 |
| stats_json | `JSON NOT NULL` | SolveStats进度快照 |
| error_code | `VARCHAR(64) NULL` | 脱敏错误码 |

索引与约束：

- `KEY idx_task_owner (owner_id, deleted, create_time, id)`
- `KEY idx_task_claim (task_status, available_time, lease_until)`
- `KEY idx_task_hash (state_hash, solver_version)`
- `FOREIGN KEY (owner_id) REFERENCES sys_user(id)`
- `FOREIGN KEY (puzzle_version_id) REFERENCES puzzle_version(id)`
- `FOREIGN KEY (game_id) REFERENCES game_session(id)`
- `CHECK (task_status IN ('QUEUED','RUNNING','COMPLETED','CANCELLED','FAILED'))`
- `CHECK (purpose IN ('ANALYSIS','HINT'))`
- `CHECK (time_limit_ms BETWEEN 100 AND 60000)`
- `CHECK (node_limit BETWEEN 1000 AND 5000000)`
- `CHECK (memory_limit_mb BETWEEN 16 AND 128)`
- `CHECK ((source_type='CUSTOM' AND puzzle_version_id IS NULL AND game_id IS NULL AND game_state_version IS NULL) OR (source_type='PUZZLE_VERSION' AND puzzle_version_id IS NOT NULL AND game_id IS NULL AND game_state_version IS NULL) OR (source_type='GAME' AND puzzle_version_id IS NULL AND game_id IS NOT NULL AND game_state_version IS NOT NULL))`

## solve_result

每任务一个根结论

| 字段 | SQL 类型与约束 | 说明 |
|---|---|---|
| id | `BIGINT NOT NULL` | 应用生成的正整数ID |
| create_time | `DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)` | UTC创建时间 |
| update_time | `DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)` | UTC更新时间，应用显式维护 |
| create_by | `BIGINT NULL` | 创建者，系统任务可空 |
| update_by | `BIGINT NULL` | 最后更新者 |
| deleted | `TINYINT NOT NULL DEFAULT 0` | 逻辑删除0正常1删除 |
| task_id | `BIGINT NOT NULL` | 所属任务唯一 |
| proof_status | `VARCHAR(16) NOT NULL` | PROVEN/UNKNOWN |
| winner_side | `VARCHAR(16) NULL` | 获胜阵营USER/BOT |
| proof_value | `TINYINT NULL` | USER视角+1或-1 |
| strategy_status | `VARCHAR(16) NOT NULL` | NONE/PARTIAL/COMPLETE |
| root_node_id | `BIGINT NULL` | 证明根节点，后置复合外键 |
| winning_lines_json | `JSON NOT NULL` | 最多5条已验证展示路线 |
| stats_json | `JSON NOT NULL` | 最终统计 |
| verified_time | `DATETIME(3) NULL` | 完整证明校验时间 |

索引与约束：

- `UNIQUE KEY uk_result_task (task_id)`
- `FOREIGN KEY (task_id) REFERENCES solve_task(id)`
- `CHECK ((proof_status='UNKNOWN' AND winner_side IS NULL AND proof_value IS NULL) OR (proof_status='PROVEN' AND winner_side IS NOT NULL AND proof_value IS NOT NULL AND ((winner_side='USER' AND proof_value=1) OR (winner_side='BOT' AND proof_value=-1))))`
- `CHECK (strategy_status IN ('NONE','PARTIAL','COMPLETE'))`
- `CHECK (strategy_status<>'COMPLETE' OR (proof_status='PROVEN' AND root_node_id IS NOT NULL AND verified_time IS NOT NULL))`

## strategy_node

结果内去重证明局面

| 字段 | SQL 类型与约束 | 说明 |
|---|---|---|
| id | `BIGINT NOT NULL` | 应用生成的正整数ID |
| create_time | `DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)` | UTC创建时间 |
| update_time | `DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)` | UTC更新时间，应用显式维护 |
| create_by | `BIGINT NULL` | 创建者，系统任务可空 |
| update_by | `BIGINT NULL` | 最后更新者 |
| deleted | `TINYINT NOT NULL DEFAULT 0` | 逻辑删除0正常1删除 |
| result_id | `BIGINT NOT NULL` | 所属结果 |
| state_hash | `CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL` | 规范化摘要 |
| state_json | `JSON NOT NULL` | 局面 |
| proof_status | `VARCHAR(16) NOT NULL` | 节点证明状态 |
| winner_side | `VARCHAR(16) NULL` | 节点获胜方USER/BOT |
| coverage | `VARCHAR(16) NOT NULL` | TERMINAL/ONE_WINNING/ALL_REPLIES/PARTIAL |
| terminal | `TINYINT NOT NULL DEFAULT 0` | 是否终局 |

索引与约束：

- `UNIQUE KEY uk_node_hash (result_id, state_hash)`
- `UNIQUE KEY uk_node_result (result_id, id)`
- `FOREIGN KEY (result_id) REFERENCES solve_result(id)`
- `CHECK (terminal IN (0,1))`
- `CHECK ((proof_status='UNKNOWN' AND winner_side IS NULL) OR (proof_status='PROVEN' AND winner_side IS NOT NULL AND winner_side IN ('USER','BOT')))`
- `CHECK (coverage IN ('TERMINAL','ONE_WINNING','ALL_REPLIES','PARTIAL'))`

## strategy_edge

证明行动边及覆盖

| 字段 | SQL 类型与约束 | 说明 |
|---|---|---|
| id | `BIGINT NOT NULL` | 应用生成的正整数ID |
| create_time | `DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)` | UTC创建时间 |
| update_time | `DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)` | UTC更新时间，应用显式维护 |
| create_by | `BIGINT NULL` | 创建者，系统任务可空 |
| update_by | `BIGINT NULL` | 最后更新者 |
| deleted | `TINYINT NOT NULL DEFAULT 0` | 逻辑删除0正常1删除 |
| result_id | `BIGINT NOT NULL` | 所属结果 |
| from_node_id | `BIGINT NOT NULL` | 起点 |
| to_node_id | `BIGINT NOT NULL` | 终点 |
| move_json | `JSON NOT NULL` | 合法行动 |
| move_key | `VARCHAR(160) CHARACTER SET ascii COLLATE ascii_bin NOT NULL` | 规范化行动字符串 |
| recommended | `TINYINT NOT NULL DEFAULT 0` | 是否建议 |
| sort_order | `INT NOT NULL` | 稳定分页序号 |

索引与约束：

- `UNIQUE KEY uk_edge_move (result_id, from_node_id, move_key)`
- `KEY idx_edge_children (result_id, from_node_id, sort_order, id)`
- `FOREIGN KEY (result_id, from_node_id) REFERENCES strategy_node(result_id, id)`
- `FOREIGN KEY (result_id, to_node_id) REFERENCES strategy_node(result_id, id)`
- `CHECK (recommended IN (0,1))`

## audit_log

关键动作审计，不存秘密

| 字段 | SQL 类型与约束 | 说明 |
|---|---|---|
| id | `BIGINT NOT NULL` | 应用生成的正整数ID |
| create_time | `DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)` | UTC创建时间 |
| update_time | `DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)` | UTC更新时间，应用显式维护 |
| create_by | `BIGINT NULL` | 创建者，系统任务可空 |
| update_by | `BIGINT NULL` | 最后更新者 |
| deleted | `TINYINT NOT NULL DEFAULT 0` | 逻辑删除0正常1删除 |
| actor_id | `BIGINT NULL` | 操作者 |
| action | `VARCHAR(64) NOT NULL` | 业务动作码 |
| resource_type | `VARCHAR(32) NOT NULL` | 资源类型 |
| resource_id | `BIGINT NULL` | 资源ID |
| request_id | `VARCHAR(64) NOT NULL` | 请求关联 |
| outcome | `VARCHAR(16) NOT NULL` | SUCCESS/FAILURE |
| details_json | `JSON NOT NULL` | 脱敏差异或错误码 |

索引与约束：

- `KEY idx_audit_resource (resource_type, resource_id, create_time)`
- `KEY idx_audit_actor (actor_id, create_time)`
- `CHECK (outcome IN ('SUCCESS','FAILURE'))`

## idempotency_record

写请求幂等；凭证类不缓存完整请求

| 字段 | SQL 类型与约束 | 说明 |
|---|---|---|
| id | `BIGINT NOT NULL` | 应用生成的正整数ID |
| create_time | `DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)` | UTC创建时间 |
| update_time | `DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)` | UTC更新时间，应用显式维护 |
| create_by | `BIGINT NULL` | 创建者，系统任务可空 |
| update_by | `BIGINT NULL` | 最后更新者 |
| deleted | `TINYINT NOT NULL DEFAULT 0` | 逻辑删除0正常1删除 |
| owner_id | `BIGINT NOT NULL` | 调用者 |
| operation | `VARCHAR(128) CHARACTER SET ascii COLLATE ascii_bin NOT NULL` | API路径或内部操作码 |
| request_key | `VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL` | 幂等键 |
| payload_hash | `CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL` | 规范化请求的服务端HMAC摘要，不保存密码 |
| status | `VARCHAR(16) NOT NULL` | PROCESSING/COMPLETED |
| response_json | `JSON NULL` | 成功响应或资源引用，不含敏感字段 |
| expires_time | `DATETIME(3) NOT NULL` | 默认24小时；到期返回KEY_EXPIRED，不重用键 |

索引与约束：

- `UNIQUE KEY uk_idem_scope (owner_id, operation, request_key)`
- `KEY idx_idem_expiry (expires_time)`
- `FOREIGN KEY (owner_id) REFERENCES sys_user(id)`
- `CHECK (status IN ('PROCESSING','COMPLETED'))`
