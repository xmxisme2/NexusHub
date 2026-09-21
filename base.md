# OpsDesk AI 开发规范

## 项目简介

OpsDesk 是一个面向企业内部协作场景的智能工单平台，用于统一接收、分派、跟踪、处理、确认、关闭和沉淀问题。

计划技术栈：

- 后端：Spring Boot 3、Spring Security、MyBatis、PageHelper、MySQL、Redis、RabbitMQ
- 前端：Vue 3、TypeScript、Element Plus、Pinia、Vite、Axios、ECharts

当前状态：

- `opsdesk-backend` 和 `opsdesk-frontend` 已完成基础能力和工单主流程开发，后续按模块持续迭代。
- Figma 设计稿保持不变，入口为 `docs/figma-links.md`。
- `docs/requirements` 是最高优先级需求来源。

## 开发前必读

开始任何开发任务前，必须按顺序阅读：

1. `docs/requirements/00-需求索引.md` 和 `docs/requirements/01-项目概览与范围.md`。
2. 当前任务对应的需求文档：
   - 必须阅读对应的 `docs/requirements/modules/*.md`。
   - 涉及角色、状态机、页面、数据、非功能或工程结构时，再阅读对应的 `02` 至 `09` 专题文档。
   - 禁止无差别读取整个 `docs/requirements` 目录。
3. `docs/database-design.md` 的通用规范、表清单和当前任务涉及的数据章节；未涉及数据库时不读取其余字段明细。
4. `docs/api-contract.md` 契约索引、`docs/api/common.md`、`docs/api/cross-cutting.md` 和当前任务对应的模块契约；禁止无差别读取全部模块契约。
5. `docs/project-status.md` 当前快照。
6. 与任务相关的架构文档：
   - 后端任务阅读 `docs/backend-architecture.md`
   - 前端任务阅读 `docs/frontend-architecture.md`
   - 开发准备阅读 `docs/development-prep.md`
7. 前端页面开发前阅读 `docs/figma-links.md` 并通过 Figma MCP 读取相关设计稿。

`docs/archive` 只用于历史追溯，不属于开发前必读范围；只有当前文档明确引用且确需追溯时才读取。

禁止跳过。

## 文档优先级

优先级从高到低：

1. 当前任务涉及的 `docs/requirements` 原始需求
2. `docs/database-design.md`
3. `docs/api-contract.md`、`docs/api/common.md` 和对应模块契约
4. `docs/project-status.md`
5. Figma 设计稿
6. 其他派生文档

如果存在冲突：

1. 禁止直接开发。
2. 先说明冲突原因。
3. 更新 `docs/todo.md` 或 `docs/project-status.md` 记录冲突。
4. 等待用户确认或先完成不冲突的准备工作。

## 开发原则

### 本地开发与联调默认授权

在 `D:\OpsDesk` 本地开发、测试和联调范围内，用户默认授权 AI 执行系统全部功能操作，包括登录、填写本地图形验证码、创建或调整测试数据、执行工单状态动作、通知已读和后台管理操作，无需逐项重复确认。

该授权仅适用于 OpsDesk 本地开发环境和专用测试数据；涉及项目外部系统、真实生产数据、真实个人敏感信息、对外发送信息、付费操作、权限公开或不可逆破坏性操作时，仍必须单独确认。

遵循：

- 小步提交
- 单模块开发
- 先设计后编码
- 先接口后页面
- 先测试后提交
- 先后端契约后前端调用

禁止：

- 一次性重构整个项目
- 删除核心业务代码
- 修改无关模块
- 破坏接口兼容性
- 忽略编译错误
- 忽略测试失败
- 缺少中文注释
- 跳过项目状态更新

## 模块开发流程

开发新模块时：

1. 阅读需求。
2. 检查数据库设计。
3. 检查接口契约。
4. 检查 Figma 设计稿。
5. 说明模块边界和实现顺序。
6. 编写后端接口和测试。
7. 编写前端页面和联调代码。
8. 执行构建和测试。
9. 更新项目状态。

## 数据库规范

所有业务表必须包含：

- `id`
- `create_time`
- `update_time`
- `create_by`
- `update_by`
- `deleted`

逻辑删除：

- `0 = 正常`
- `1 = 删除`

禁止物理删除业务数据。

数据库字段使用 snake_case。

接口字段使用 camelCase。

接口层 ID 统一按字符串传输，避免前端大整数精度问题。

## 接口规范

统一返回格式：

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

要求：

1. 所有业务请求默认使用 `POST`。
2. 基础路径为 `/api`。
3. 分页、筛选、排序和动作参数默认放 Body。
4. 资源 ID 可保留在 Path。
5. 详情、查询、更新、删除类接口使用动作化路径，例如 `/detail`、`/search`、`/update`、`/delete`。
6. 文件上传使用 `multipart/form-data`。
7. 普通 JSON 接口使用 `application/json`。
8. 新增或修改接口必须同步更新 `docs/api-contract.md`。

错误码以 `docs/api-contract.md` 为准。

## 后端规范

后端按模块分层：

- Controller
- Service
- Mapper
- Entity
- DTO
- VO
- Converter

要求：

1. Controller 只做参数接收、权限入口、调用 Service 和返回响应。
2. Service 负责业务编排、状态机、资源范围校验和事件发布。
3. Mapper 只做数据访问，不写业务判断。
4. 查询 SQL 统一写在 MyBatis XML 中，不在 Mapper 接口使用注解查询；分页查询统一使用 PageHelper 和公共 `PageResult`，禁止业务代码手写 `total + limit/offset`。
5. 查询字段必须显式列出，禁止在业务查询中使用 `SELECT *` 或 `alias.*`。
6. 工单状态流转必须集中在状态机或领域服务中。
7. 禁止 Controller 直接修改工单状态。
8. 管理接口必须做角色校验。
9. 工单、评论、附件、知识库草稿等接口必须做资源范围校验。
10. 关键操作必须记录操作日志或审计日志。
11. 业务常量、缓存 Key、权限编码、状态/类型枚举值等常量必须写中文注释，说明含义、使用场景和是否允许外部传入，禁止出现无法理解来源的裸常量。
12. 分页请求和分页响应优先复用固定公共类，例如统一分页 DTO 和 `PageResult`；不要为每个模块重复创建语义相同的分页实体，确有差异必须在类注释中说明原因。

## 前端规范

前端使用：

- Vue 3
- TypeScript
- Element Plus
- Pinia
- Vue Router
- Axios
- Vite

要求：

1. 所有业务接口统一放在 `src/api/modules`。
2. 页面禁止直接写 Axios URL。
3. 路由守卫统一处理登录态和权限。
4. 菜单由路由 meta 和权限共同驱动。
5. 列表页必须支持分页、加载态、空状态和错误重试。
6. 表单必须有前端校验和服务端错误回显。
7. 工单状态动作必须以后端返回的可用动作为准。
8. 附件按钮必须根据 `previewable`、`previewType`、`downloadOnly` 展示。
9. 不可预览附件不展示置灰预览按钮，直接展示下载入口。

## 中文注释规范

所有新增代码必须包含中文注释。

必须添加注释：

- Controller
- Service
- Entity
- Mapper
- DTO
- VO
- Vue 页面
- API 文件
- Store
- 复杂业务逻辑

要求：

接手项目的人仅阅读代码和注释即可理解业务。

## Figma 规范

前端开发前：

1. 阅读 `docs/figma-links.md`。
2. 通过 Figma MCP 读取对应设计稿。
3. 尽量还原布局、间距、字体、配色和组件结构。

如果 Figma 与需求冲突，用户授权 AI 自主决策并继续开发：

1. 综合业务合理性、数据库与 API 契约、向后兼容性、实现成本和用户体验选择最合理方案。
2. 可以修改 Figma 设计稿，也可以修改需求文档；优先保持已发布接口和核心业务规则稳定。
3. 决策后必须同步更新受影响的需求、设计入口、接口、架构、待办或项目状态文档，记录冲突和选择理由。
4. 只有涉及生产数据、不可逆破坏性变更或明显扩大产品范围时才需要再次等待用户确认。

## AI 和知识库边界

当前阶段：

1. 知识库可延后到工单主流程稳定后实现。
2. AI 生成内容暂不接入首版。
3. AI 只保留接口、配置开关、调用日志结构和关闭态入口。
4. AI 开关关闭时前端隐藏入口或展示禁用态。
5. 后续接入 AI 时必须先做敏感信息脱敏。

## 开发完成后

必须执行：

后端：

```bash
mvn clean test
```

前端：

```bash
npm run build
```

修复编译错误和测试失败后再结束任务。

## 项目状态维护

每次完成开发后必须更新：

- `docs/project-status.md`

内容包括：

- 已完成内容
- 当前开发内容
- 已知问题
- 下一步计划

`docs/project-status.md` 只维护当前快照，直接更新现有四类内容，禁止为每个任务继续追加日期流水。阶段性历史需要保留时移入 `docs/archive`，归档文件不属于开发前必读范围。

如涉及接口、数据库、页面或架构，还必须同步更新：

- 当前任务对应的 `docs/api/*.md`；只有模块入口变化时才更新 `docs/api-contract.md`
- `docs/database-design.md`
- `docs/frontend-architecture.md`
- `docs/backend-architecture.md`
- `docs/todo.md`

维护要求：

1. 架构文档只保留当前有效结构和约定，不追加模块落地流水。
2. `docs/todo.md` 只保留未完成、边界和风险；完成项从活动文档移除，必要时归档。
3. 已完成的设计稿解析、实施计划和阶段复盘移入 `docs/archive`，不得加入常规必读清单。
4. 禁止为了记录一次构建或联调结果而重复抄写接口、架构和需求内容。

禁止只修改代码不更新项目状态。

## 输出要求

完成任务后输出：

【修改文件】

【完成内容】

【测试结果】

【影响范围】

【后续建议】

## 最高优先级规则

1. 不允许删除核心业务代码。
2. 不允许修改无关模块。
3. 不允许破坏接口兼容性。
4. 不允许忽略编译错误。
5. 不允许忽略测试失败。
6. 不允许缺少中文注释。
7. 不允许跳过项目状态更新。
8. 不允许跳过开发前必读文档。
