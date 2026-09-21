# identity API

详见 [OpenAPI](openapi.json) 的 `identity` tag。匿名接口：`GET /api/auth/csrf`、`POST /api/auth/login`、`POST /api/auth/guest`；登录后接口：`/me`、`/logout`。游客仅获得题库读取和对局权限；管理员账号管理接口要求 `user:manage`。

登录错误统一返回凭证无效，不泄漏用户名是否存在。停用、重置密码会递增 `credential_version`，旧会话失效。登录/退出及管理写操作需要幂等 Key。
