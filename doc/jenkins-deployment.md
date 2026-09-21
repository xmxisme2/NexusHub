# Jenkins 部署

生产 NexusHub 使用远程 Jenkins 任务 `nexushub-deploy` 构建和重启。任务采用 Jenkins 内置 Pipeline 配置，通过受限 SSH 入口调用服务器上的 `nexushub-ci-deploy`；仓库根目录的 `Jenkinsfile` 保留为同等流程的版本化参考。这样 Jenkins 不需要先通过 Git 插件拉取仓库，避免 GitHub TLS 短暂中断阻塞发布。

部署脚本按提交固定拉取源码，在 Docker 构建容器中执行 Java 21 Maven 测试/打包和 Node 20 前端类型检查/构建，然后更新 `/opt/nexushub/source` 并执行 Docker Compose。后端健康接口和前端端口都通过后才算发布成功；失败会恢复上一版 JAR 和前端产物。发布使用独占锁，不会操作 OpsDesk 的 systemd 服务。

Jenkins 使用服务器现有的受限 `jenkins-deploy` 账号和密钥，仓库不保存任何服务器、数据库或 Jenkins 凭据。生产 Compose 的 `.env` 继续只保存在服务器上。
