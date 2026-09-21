# NexusHub frontend

要求 Node 20+。安装依赖后运行 `npm run dev`，默认端口 5173。当前提供桌面门户首页，移动端交互不在本轮范围。

OpsDesk 入口通过构建变量 `VITE_OPS_DESK_URL` 配置，默认值为 `https://www.xmxisme.com/opsdesk/`；外链在新标签页打开，不共享 NexusHub 会话。
