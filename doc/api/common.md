# 公共 API 契约

## 传输

- 基础路径 `/api`；业务接口默认 `POST`，唯一例外是匿名获取 CSRF 的 `GET /api/auth/csrf`。
- `Content-Type: application/json`；暂不提供上传接口。
- 成功响应：`{code:200,message:"success",data:{},requestId:"..."}`。
- 错误响应保持 HTTP 状态语义，并使用 `{code,message,data,requestId}`；`data.fieldErrors` 用于表单回显。
- 时间为 UTC ISO-8601（`Z`）；ID 以十进制字符串传输。

## 登录、CSRF 与幂等

本系统使用同源 HttpOnly Cookie 会话 `NEXUSHUB_SESSION`。前端先调用 CSRF 接口，再把返回 token 放入 `X-CSRF-TOKEN`；登录、退出及所有业务 POST 都必须带该 Header。会话轮换、退出和密码重置后需重新获取 CSRF。

写接口表中标记幂等为“是”时，必须带 `Idempotency-Key`，长度 16～64，只能使用字母、数字、下划线和短横线。相同用户、路径和 Key 只接受相同 payload hash；相同 Key 不同 payload 返回 409。记录默认保留 24 小时，过期 Key 不重用。

## 分页与排序

请求复用 `pageNum`（从 1 开始）和 `pageSize`（1～100，默认 20）；筛选和排序放 Body。响应使用 `items,total,pageNum,pageSize`。服务端只接受白名单排序，默认按业务文档规定的稳定排序；不得把字段值直接拼接 SQL。

## 错误码

`40000` 参数格式错误；`40100` 未登录/会话失效；`40300` 权限或 CSRF 错误；`40400` 资源不存在或无权访问；`40900` 并发版本、状态或幂等冲突；`42200` 规则校验失败；`42900` 限流或队列满；`50000` 未知服务错误。具体场景在模块 API 文档补充。

## 资源权限

所有对象详情都再次校验角色与所有权。私有对象对非所有者统一返回 404。任务结果、策略节点、对局复盘沿用源对象权限；管理员可管理题目和入口，但不默认读取普通用户私有对局和分析。

## OpenAPI

机器可读契约位于 [openapi.json](openapi.json)，使用 OpenAPI 3.1。生成源是 `doc/tooling/generate_contracts.py`，修改接口时先改生成源，再运行脚本并同步模块文档。
