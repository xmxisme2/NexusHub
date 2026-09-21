# 横切需求

## 角色与对象范围

| 行为 | 未登录 | USER | ADMIN |
|---|---|---|---|
| 获取 CSRF、登录 | 允许 | 允许 | 允许 |
| 查看门户、已发布题库 | 允许（游客会话） | 允许 | 允许 |
| 创建/修改草稿 | 拒绝 | 仅自己的题目 | 可管理全部 |
| 查看未发布版本 | 拒绝 | 仅自己的题目 | 可管理全部 |
| 发布、归档、恢复草稿 | 拒绝 | 拒绝 | 允许 |
| 创建对局、分析 | 可创建对局；分析需登录 | 基于可读局面 | 基于可读局面 |
| 读取对局、任务、策略 | 拒绝 | 仅自己 | 仍仅自己；不默认开放他人私有记录 |
| 配置卡片和账号 | 拒绝 | 拒绝 | 允许 |

固定角色 `GUEST/USER/ADMIN`；游客由系统创建临时会话，仅拥有 `hub:read`、`puzzle:read`、`game:play`，没有新建、修改、发布、归档、分析和账号权限。ADMIN 包含普通功能。权限码由后端角色映射返回，首版不开放动态 RBAC 配置。私有对象不存在或不属于调用者统一 404，避免枚举。

## 权限码

`hub:read`、`hub:manage`、`puzzle:read`、`puzzle:write`、`puzzle:publish`、`puzzle:archive`、`game:play`、`analysis:run`、`user:manage`。普通用户拥有 read/write/play/run；管理角色额外拥有 manage/publish/archive。权限码不替代所有权校验。

## 状态机

- 用户：`ENABLED ↔ DISABLED`；停用立即失效现有会话，禁止停用自身或最后一个启用管理员。
- 题目：`DRAFT → PUBLISHED → ARCHIVED`；DRAFT 也可归档；恢复只到 DRAFT，清空公开指针，需重新发布。编辑 PUBLISHED 保留其公开版本，形成待发布的新草稿。
- 对局：`ACTIVE → FINISHED / ABANDONED`，终态不可继续行动；重开另建 ACTIVE。
- 任务：`QUEUED → RUNNING → COMPLETED / FAILED`；QUEUED/RUNNING 可转 CANCELLED；租约失效可 RUNNING → QUEUED，重试上限耗尽 FAILED。终态不可覆盖。
- 证明：`UNKNOWN / PROVEN`；策略：`NONE / PARTIAL / COMPLETE`。获胜阵营只在 PROVEN 存在；COMPLETE 必须 PROVEN 且证明 DAG 验证通过。

## 页面与路由

| 路由 | 页面 | 关键状态 |
|---|---|---|
| `/login` | 登录 | 提交中、凭证错误、会话过期 |
| `/` | 门户 | 加载、无入口、斗地主入口、OpsDesk 外链 |
| `/landlord/puzzles` | 残局库 | 发布题库/我的草稿、筛选、空结果 |
| `/landlord/puzzles/new` | 新建残局 | 未保存、校验失败 |
| `/landlord/puzzles/:id/edit` | 编辑残局 | 版本冲突、已发布有新草稿 |
| `/landlord/games/:id` | 对局台 | 玩家行动、机器人计算、结束 |
| `/landlord/games/:id/replay` | 复盘 | 初始局面、逐手、最终局面 |
| `/landlord/analysis/:id` | 分析详情 | 排队、运行、已证明、未知、失败、取消 |
| `/admin/systems` | 入口管理 | 地址白名单、禁用；当前 OpsDesk 地址由构建配置提供 |
| `/admin/users` | 账号管理 | 启用、停用 |

## 非功能约束（设计目标，待实测）

- API 单请求 JSON 上限 64 KiB；服务端限制牌数、数组和字符串长度。
- 列表 pageSize 默认 20、最大 100；排序字段白名单，不能直传 SQL。
- 默认每用户最多 2 个未结束分析任务，全局排队上限 100；单 JVM 初始求解并发 2，依据 CPU/内存调整。
- 机器人和提示预算起点 2 秒；后台分析默认 30 秒、最大 60 秒；节点默认 100 万、最大 500 万；置换表默认 64 MiB/任务、最大 128 MiB。包含 JVM 对象开销的实际内存需测量，超预算停止新增缓存或结束搜索。
- 任务租约 30 秒、5 秒心跳、最多重试 2 次（共 3 次执行）。持久化与证明材料也受节点/空间预算约束。
- 普通读取 API 在小规模部署 P95 目标 500ms，不含排队/搜索；统计时记录硬件和负载，不将其作为已达到指标。
- 1440px 桌面完整布局，768px 折叠，390px 单列；牌桌横向牌区可滚动，禁止整页横向溢出。
- 可操作元素支持键盘焦点，颜色外另有状态文字；按钮有禁用原因。
- 账号密码/令牌不记录；审计至少含操作者、动作、资源、时间、结果与 requestId。
