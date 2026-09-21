# Docker 开发运行

NexusHub 的本地运行服务统一由 `docker-compose.yml` 编排：MySQL、Spring Boot 后端和 Nginx 前端分别运行在同一 Compose 网络中。

首次启动：

```powershell
Copy-Item .env.example .env
docker compose up -d --build
```

访问地址：

- 门户：`http://localhost:5173`
- 后端健康检查：`http://localhost:8080/api/health`
- MySQL 主机端口：`3307`，容器内服务名：`nexushub-mysql:3306`

MySQL 首次创建卷时会按顺序执行 `doc/database/schema.sql` 和 `doc/database/seed-test.sql`。后续修改 DDL 必须新增迁移脚本；已有卷不会自动重复执行初始化 SQL。

常用命令：

```powershell
docker compose ps
docker compose logs -f nexushub-backend
docker compose down
```

当前 Compose 使用测试密码，仅适用于本机开发；生产部署必须通过外部密钥注入。
