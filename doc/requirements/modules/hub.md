# 门户模块（OpsDesk 外链）

| 编号 | 需求 | 验收 |
|---|---|---|
| HUB-01 | 展示斗地主残局系统卡片 | 首屏进入 `/landlord/puzzles` |
| HUB-02 | 保留系统卡片扩展模型 | OpsDesk 通过批准的 HTTPS 外链打开，地址可由构建配置覆盖 |
| HUB-03 | 内部路由与外部链接模型互斥 | 斗地主使用 INTERNAL 路由，OpsDesk 使用 EXTERNAL URL；单张卡片只允许一种目标 |
| HUB-05 | 卡片入口 | 门户系统入口整张卡片可点击，不再提供独立的“进入实验室”或“打开 OpsDesk”按钮；内部入口保持站内跳转，外链保持新标签页打开 |
| HUB-04 | OpsDesk 外链 | 仅打开 `https://www.xmxisme.com/opsdesk/`，不代理业务、不探测健康、不转交 NexusHub 凭据 |

卡片字段仍保留可扩展模型：名称、简介、图标 Key、入口类型、目标、可见角色、排序、启用。当前实现由门户静态注册斗地主 INTERNAL 卡片和 OpsDesk EXTERNAL 卡片；OpsDesk 地址由 `VITE_OPS_DESK_URL` 构建变量覆盖，缺省使用已确认地址。

内部路由 V1 注册 `/landlord/puzzles`；外链在新标签页打开并设置 noopener/noreferrer。无 Token 转交、iframe、健康状态或 OpsDesk 业务调用。

数据：hub_system；API：[hub](../../api/hub.md)。
