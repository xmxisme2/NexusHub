# 后端架构

题库 Mapper 在原查询内按展示版本关联完成的严格证明，核对规则、当前引擎和完整快照；服务层统一输出 proofStatus/winnerSide，避免列表逐项查询任务。只返回根结论，不暴露任务所属人的私有策略。

首版是 Java 21 + Spring Boot 3.x 的模块化单体，Maven 多模块但单进程部署：`nexushub-app`、`landlord-rules`、`landlord-solver`。计算负载增长后，将 solver 与持久化任务领取迁移为独立 Worker，接口和结果契约不变。

## 分层

`Controller → Service/Domain Service → Mapper(XML)`；DTO/VO 通过 Converter 与 Entity 分离。Controller 只做传输校验、权限入口和返回；Service 编排事务、状态机、资源范围校验和审计；Mapper 只访问数据。`landlord-rules` 提供不可变 GameState、Move、合法行动、比较和 applyMove；`landlord-solver` 只依赖 rules，执行两阵营 Minimax、Alpha-Beta、记忆化、证明 DAG 和 verifier。

## 任务与并发

HTTP 线程只创建/查询任务。持久化队列由有界执行器领取，任务设置时间、节点、内存预算；循环定期检查取消和租约。任务状态用条件更新/版本控制，旧 Worker 没有匹配 leaseToken 不能提交。热点局面使用带完整状态核验的进程内置换表。

## 安全与观测

Spring Security 同源 Cookie + CSRF；角色和对象权限在 Service 强制执行。日志带 requestId、gameId、taskId、规则/求解器版本、排队/搜索统计；不记录密码、Cookie、CSRF Token。监控 API P95、队列、超时、取消、缓存命中、JVM 堆和数据库连接。
