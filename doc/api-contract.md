# 接口索引

基线 V1.0-design。由 `tooling/generate_contracts.py` 生成；修改契约请先更新生成源，再同步模块说明。

[公共规范](api/common.md) · [机器契约](api/openapi.json)

所有类型定义在 OpenAPI components.schemas；权限说明是强制服务端约束。表中幂等为“是”的接口必须携带 Idempotency-Key。

| 模块 | 方法/路径 | 请求类型 | data 类型 | 权限 | 幂等 |
|---|---|---|---|---|---|
| [identity](api/identity.md) | `GET /api/auth/csrf` | — | Csrf | 匿名 | 否 |
| [identity](api/identity.md) | `POST /api/auth/login` | LoginRequest | User | 匿名 | 否 |
| [identity](api/identity.md) | `POST /api/auth/me` | Empty | User | 登录 | 否 |
| [identity](api/identity.md) | `POST /api/auth/guest` | Empty | User | 匿名 + CSRF | 否 |
| [identity](api/identity.md) | `POST /api/auth/logout` | Empty | Empty | 登录 | 否 |
| [identity](api/identity.md) | `POST /api/admin/users/search` | UserSearchRequest | UserPage | user:manage | 否 |
| [identity](api/identity.md) | `POST /api/admin/users/create` | CreateUserRequest | User | user:manage | 是 |
| [identity](api/identity.md) | `POST /api/admin/users/status` | UserStatusRequest | User | user:manage | 是 |
| [identity](api/identity.md) | `POST /api/admin/users/reset-password` | ResetPasswordRequest | User | user:manage | 是 |
| [hub](api/hub.md) | `POST /api/hub/systems/search` | SystemSearchRequest | SystemList | hub:read；MANAGE另需hub:manage | 否 |
| [hub](api/hub.md) | `POST /api/hub/systems/save` | SystemSaveRequest | SystemCard | hub:manage | 是 |
| [puzzle](api/puzzle.md) | `POST /api/puzzles/search` | PuzzleSearchRequest | PuzzlePage | puzzle:read；MANAGE另需puzzle:publish | 否 |
| [puzzle](api/puzzle.md) | `POST /api/puzzles/detail` | PuzzleDetailRequest | Puzzle | puzzle:read + 版本可读 | 否 |
| [puzzle](api/puzzle.md) | `POST /api/puzzles/proof-strategy` | PuzzleProofStrategyRequest | SolveTask | puzzle:read + 已发布版本可读 | 否 |
| [puzzle](api/puzzle.md) | `POST /api/puzzles/validate` | PuzzleValidateRequest | ValidationResult | puzzle:write | 否 |
| [puzzle](api/puzzle.md) | `POST /api/puzzles/duplicate-check` | PuzzleDuplicateCheckRequest | PuzzleDuplicateCheck | puzzle:write | 否 |
| [puzzle](api/puzzle.md) | `POST /api/puzzles/save` | PuzzleSaveRequest | Puzzle | puzzle:write + 所有者或ADMIN | 是 |
| [puzzle](api/puzzle.md) | `POST /api/puzzles/publish` | PuzzlePublishRequest | Puzzle | puzzle:publish | 是 |
| [puzzle](api/puzzle.md) | `POST /api/puzzles/archive` | VersionRequest | Puzzle | puzzle:archive | 是 |
| [puzzle](api/puzzle.md) | `POST /api/puzzles/restore` | VersionRequest | Puzzle | puzzle:archive | 是 |
| [puzzle](api/puzzle.md) | `POST /api/puzzles/copy` | PuzzleCopyRequest | Puzzle | puzzle:write + 来源版本可读 | 是 |
| [game](api/game.md) | `POST /api/games/create` | GameCreateRequest | Game | game:play + 版本可读且未归档 | 是 |
| [game](api/game.md) | `POST /api/games/detail` | IdRequest | Game | game:play + 所有者 | 否 |
| [game](api/game.md) | `POST /api/games/actions/play` | PlayRequest | Game | game:play + 所有者 + 当前人类座位 | 是 |
| [game](api/game.md) | `POST /api/games/actions/pass` | GameVersionRequest | Game | game:play + 所有者 + 当前人类座位 | 是 |
| [game](api/game.md) | `POST /api/games/legal-moves/search` | LegalMovesRequest | LegalMovePage | game:play + 所有者 | 否 |
| [game](api/game.md) | `POST /api/games/hint` | GameVersionRequest | HintResponse | game:play + 所有者 + 当前人类座位 | 是 |
| [game](api/game.md) | `POST /api/games/abandon` | GameVersionRequest | Game | game:play + 所有者 | 是 |
| [game](api/game.md) | `POST /api/games/restart` | GameRestartRequest | Game | game:play + 所有者 | 是 |
| [game](api/game.md) | `POST /api/games/resume` | GameResumeRequest | Game | game:play + 所有者 + 结果可访问 | 是 |
| [game](api/game.md) | `POST /api/games/replay/detail` | ReplayRequest | Replay | game:play + 所有者 | 否 |
| [analysis](api/analysis.md) | `POST /api/analysis/tasks/create` | SolveCreateRequest | SolveTask | analysis:run + 来源可读 | 是 |
| [analysis](api/analysis.md) | `POST /api/analysis/tasks/search` | TaskSearchRequest | TaskPage | analysis:run + 所有者 | 否 |
| [analysis](api/analysis.md) | `POST /api/analysis/tasks/detail` | IdRequest | SolveTask | analysis:run + 所有者 | 否 |
| [analysis](api/analysis.md) | `POST /api/analysis/tasks/cancel` | IdRequest | SolveTask | analysis:run + 所有者 | 是 |
| [analysis](api/analysis.md) | `POST /api/analysis/strategies/children` | StrategyChildrenRequest | StrategyChildren | analysis:run + 结果所属任务所有者 | 否 |

接口默认排序：题目与任务 update/create_time DESC,id DESC；对局事件 sequence_no ASC；入口 sort_order ASC,id ASC；不接受任意 SQL 排序字段。

非目标：首版不提供 OpsDesk 代理接口、SSO、真人对战和直接改写在线手牌接口。
