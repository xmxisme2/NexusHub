# NexusHub backend

要求 Java 21。项目携带 Maven Wrapper，Windows 使用 `mvnw.cmd`，Linux/macOS 使用 `./mvnw`。默认连接本机 Docker MySQL 的 `nexushub` 库；健康检查：`GET /api/health`。

规则领域代码保持纯 Java。题库已接入 MyBatis Mapper，使用 `puzzle` / `puzzle_version` 持久化版本；本地种子包含 L001～L004。
