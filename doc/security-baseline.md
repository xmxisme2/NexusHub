# 安全基线

## 当前已落实

- 账号密码只保存 BCrypt 单向散列，不保存明文；当前新密码散列成本为 12。
- 登录使用 HttpOnly 会话 Cookie；会话包含凭据版本，停用账号或重置密码会立即使旧会话失效。
- 业务 POST 要求 CSRF 双提交校验；CSRF Token 只通过同源 Cookie 和请求头传递。
- 对局、题目、分析任务在服务端做对象级所有权校验，前端隐藏菜单不作为授权依据。
- 数据库访问通过环境变量注入；前端 Nginx 增加 nosniff、禁止 iframe、Referrer/Permissions Policy 和基础 CSP。
- 密码、Cookie 和 CSRF Token 不写入业务日志。

## 部署要求

- 生产环境必须使用 HTTPS，并设置 `NEXUSHUB_COOKIE_SECURE=true`。
- 不使用 Compose 和配置文件中的本地默认数据库密码；通过 Secret 管理 `NEXUSHUB_DB_PASSWORD`，并限制 MySQL 仅内网访问。
- 单实例内存会话只适用于本地测试；生产部署需要 Redis 或其他共享会话存储。
- 登录限流、失败次数锁定、幂等记录和审计日志需要在网关/共享存储中持久化，不能只依赖单 JVM 内存。
- 生产构建应锁定依赖版本并执行漏洞扫描、镜像扫描、数据库最小权限账号和定期备份恢复演练。

测试环境管理员凭据仅用于本地验证；正式环境必须在部署初始化时替换为随机强密码。
