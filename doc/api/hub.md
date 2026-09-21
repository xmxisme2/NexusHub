# hub API

`POST /api/hub/systems/search` 返回当前用户可见系统卡片；`scope=MANAGE` 需要 `hub:manage`。`POST /api/hub/systems/save` 使用 rowVersion 乐观锁并要求管理员。

外部入口只允许部署配置中的批准来源；`EXTERNAL` 时 route 必须为空，URL 为空代表待配置。后端不探测、不代理 OpsDesk，也不向其转交 NexusHub token。
