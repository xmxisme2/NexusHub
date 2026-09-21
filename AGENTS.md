# NexusHub 开发约定

## 项目与适用范围

本文件适用于本仓库全部目录。NexusHub 是 Vue + Java 的多系统卡片门户，内置斗地主残局配置、人机对局和可证明策略分析，外链 OpsDesk。需求和开发文档统一在 `doc/`，不要另建 `docs/`。

本规范从 `base.md` 提取通用工程约定，并按 NexusHub 调整。`base.md` 保留为来源参考，其中 OpsDesk 的工单、附件、知识库、D:\OpsDesk 本地授权和既有完成状态不适用于本项目。不要将来源文件当成本仓库已实现功能的证明。

## 开发前按需阅读

1. `doc/requirements/00-index.md`、`doc/requirements/01-scope.md`。
2. 当前模块的 `doc/requirements/modules/*.md`；涉及规则时读 `doc/domain/rules.md`，涉及页面和权限时读 `doc/requirements/02-cross-cutting.md`。
3. `doc/project-status.md`。
4. 涉及存储时读 `doc/database-design.md` 通用规范及相关表，以及 `doc/database/schema.sql`。
5. 涉及接口时读 `doc/api-contract.md`、`doc/api/common.md`、当前模块 `doc/api/*.md` 与 `doc/api/openapi.json` 对应路径/schema。
6. 前端读 `doc/frontend-architecture.md`、`doc/figma-links.md` 与 `doc/design-spec.md`；使用 Figma MCP 读取相关节点，不无差别读取整个设计文件。后端读 `doc/backend-architecture.md`。
7. 初始化工程或验收时读 `doc/development-prep.md` 与 `doc/test-plan.md`。

历史文档只在明确需要追溯时读取。`doc/01-NexusHub第一版方案与整体架构.md` 是早期方案；细化文档明确替代其中的草案字段和接口。

## 事实与文档优先级

用户当前明确指令 > 本文件 > 模块需求和规则 > 数据模型/接口契约 > 架构/状态 > Figma > 初版方案。数据库与接口是不同视角，冲突时同步修正，不能以优先级为由悄悄破坏契约。

常规设计差异按需求、兼容性和用户体验处理，并同步受影响文档。需要真实业务选择且无法合理推断时再向用户询问，同时完成不依赖该答案的工作。不要从 OpsDesk 文档推导对其他系统或真实数据的额外授权。

## 公共工程原则

- 小步实现，先需求和契约、再后端与前端联调；只改相关模块，保持已发布接口兼容。
- 代码格式统一使用多行、明确缩进和稳定换行；Java 禁止把字段、构造器、方法或控制流挤在同一行，Vue/TypeScript 的模板、类型、函数和异步流程按逻辑分段。提交前对修改范围执行 IDE/格式化工具，并保持可读 diff。
- 协议字符串、规则版本、状态和任务来源禁止散落硬编码：跨模块常量放在 `common.constants` 下并写中文业务注释；仅当前类使用的常量定义在类内并写注释。`PUZZLE_VERSION`、`CLASSIC_V1`、`TRAINING`、`OPTIMAL` 等契约值必须统一引用常量，不能在业务逻辑中重复字面量。
- 公共类统一复用：ApiResponse、PageRequest、PageResult、异常、ID、时间、审计、鉴权与资源范围校验。禁止为每个模块复制一套。
- Controller 只处理传输、参数校验和权限入口；Service 编排事务、资源权限和状态机；Mapper 只访问数据库。
- 按模块划分 Controller / Service / Mapper / Entity / DTO / VO / Converter。规则和求解是纯 Java 模块，不依赖 Spring、HTTP、数据库。
- 不忽略编译或测试失败，不删除核心业务代码，不修改无关模块。
- 中文注释说明公共类、页面、API、Store、状态枚举、权限码、缓存 Key、领域边界和复杂算法的业务含义；避免只复述语法的注释。

## 数据库与事务

- MySQL；字段 snake_case，Java/JSON 字段 camelCase；接口 ID 一律字符串。
- 业务表包含 `id/create_time/update_time/create_by/update_by/deleted`；`deleted=0/1`，业务删除使用逻辑删除。
- MyBatis 查询写 XML，不用注解 SQL；列名显式列出，禁止 `SELECT *`。分页统一 PageHelper 与 PageResult。
- 唯一约束、并发版本、请求幂等和资源权限必须在服务端执行；不能只靠前端禁用按钮。
- 对局快照、行动事件与版本更新在同一事务；深度搜索不放在数据库事务中。
- 残局版本不可原地修改，已发布对局/任务绑定不可变快照。变更 DDL 时新增迁移，不修改已应用迁移历史。

## API 与安全

- 业务接口默认 POST，前缀 `/api`，动作化路径，JSON Body；普通响应 `{code,message,data,requestId}`。
- 分页、错误码、时间、枚举、鉴权、CSRF、幂等遵循 `doc/api/common.md`。HTTP 状态不能全部伪装成 200。
- 管理接口做角色校验；草稿、对局、任务、策略同时校验对象级权限。前端菜单隐藏不能代替服务端授权。
- Cookie 会话默认同源；不在 URL 传令牌，不直接互认 OpsDesk 令牌，不共享其数据库。
- 关键业务动作记录审计；密码、会话 Cookie、CSRF Token 不写日志。

## 前端

- Vue 3 / TypeScript / Vite / Vue Router / Pinia / Element Plus / Axios。
- API 统一位于 `src/api/modules`，页面不拼接 Axios URL；路由 meta、统一守卫与服务端权限共同控制入口。
- 列表具有加载、空、失败重试和分页；表单包含前端校验与服务端字段错误回显。
- 对局状态及可用动作以后端为准；服务端不接受客户端宣布胜负、机器人落子或修改手牌。
- Figma 是页面实现参考；需求、接口与稿件发生变化时同步文档和设计入口。未真实读取稿件时不可声称已还原。

## 斗地主专用不变量

- 当前规则已确认：两名独立参与者 USER/BOT、完全明牌、用户/机器人先后手可配置、先出完手牌获胜；取消地主/农民角色。先手第一次必须出牌，之后双方都可 PASS；先手最多 20 张、后手最多 17 张、均不能为空、同点数最多 4 张。标准牌型固定不含四带二，其他牌型按正常斗地主规则可用。旧三人规则仅作为历史文档，不得作为当前实现依据。
- 规则确认后，合法行动枚举和状态转移必须共用一个纯 Java 引擎；前端仅做交互提示。
- 规则判断、合法行动枚举和状态转移共用一个引擎；前端仅做交互提示。
- `PROVEN` 才能显示必胜；预算截断为 `UNKNOWN`，获胜方为空；必胜必须覆盖对手多分支，最多向前端展示 5 条已验证路线；一条示例路线不是完整策略证明。
- 缓存区分精确值和界值，包含完整规则状态与版本；哈希命中要核验规范化状态。
- 搜索必须有时间、节点、内存和队列上限；过期版本的机器人结果不能落子。

## 验证与文档维护

- 后端代码变更执行 `mvn clean test`；前端代码变更执行 `npm run build`，并运行与改动相关的测试。仓库未初始化时明确标记不适用，不虚构通过结果。
- 文档任务检查本地链接、OpenAPI 路径/schema、SQL/模型一致性与关键状态契约；纯文档改动无需运行不存在的工程构建。
- 同步更新涉及的模块需求、API、OpenAPI、DDL、架构与测试计划；只有契约索引变化时更新 API 索引。
- `doc/project-status.md` 仅维护已完成、当前工作、已知问题、下一步；`doc/todo.md` 仅保留未完成事项。历史材料可移至 `doc/archive/`。
- 完成时说明交付文件、完成内容、验证结果和实质限制。设计文件必须给出真实 Figma 链接，不能把本地示意图当作已完成的 Figma 交付。
