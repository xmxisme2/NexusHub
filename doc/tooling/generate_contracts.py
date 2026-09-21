"""生成开发契约和表字段字典；仅生成文档，不连接数据库、不创建应用。"""
import json
from pathlib import Path

DOC = Path(__file__).resolve().parents[1]
S = {}


def ref(name):
    return {"$ref": f"#/components/schemas/{name}"}


def string(maximum=255, **kw):
    return {"type": "string", "maxLength": maximum, **kw}


def integer(lo=0, hi=2147483647, **kw):
    return {"type": "integer", "minimum": lo, "maximum": hi, **kw}


def enum(*values):
    return {"type": "string", "enum": list(values)}


def array(item, maximum=100, minimum=0):
    return {"type": "array", "items": item, "minItems": minimum, "maxItems": maximum}


def nullable(schema):
    return {"anyOf": [schema, {"type": "null"}]}


def obj(name, props, required=None, **kw):
    S[name] = {"type": "object", "additionalProperties": False,
               "properties": props, "required": list(props) if required is None else required, **kw}
    return ref(name)


ID = string(19, pattern=r"^[1-9][0-9]{0,18}$", description="正整数 BIGINT 的十进制字符串，服务端还校验 <= 9223372036854775807")
TIME = {"type": "string", "format": "date-time", "description": "UTC ISO-8601，输出以 Z 结束"}
BOOL = {"type": "boolean"}
SEAT = integer(0, 1)
SIDE = enum("USER", "BOT")
ROLE = enum("GUEST", "USER", "ADMIN")
DIFF = enum("EASY", "MEDIUM", "HARD")
PROOF = enum("PROVEN", "UNKNOWN")
STRATEGY = enum("NONE", "PARTIAL", "COMPLETE")
CARDS = {"type": "array", "minItems": 15, "maxItems": 15,
         "prefixItems": [integer(0, 4) for _ in range(13)] + [integer(0, 1), integer(0, 1)],
         "items": False, "description": "点数 3..A,2,小王,大王 的张数"}
MOVE_TYPES = ["SINGLE", "PAIR", "TRIPLE", "TRIPLE_SINGLE", "TRIPLE_PAIR", "STRAIGHT", "PAIR_STRAIGHT", "AIRPLANE", "AIRPLANE_SINGLE", "AIRPLANE_PAIR", "BOMB", "ROCKET", "PASS"]
obj("Move", {"type": enum(*MOVE_TYPES), "cards": CARDS, "mainRank": nullable(integer(0, 14)), "sequenceLength": integer(0, 12)}, description="牌型与数量交叉规则由规则引擎执行，详见 domain/rules.md")
obj("GameState", {"schemaVersion": {"const": 1, "type": "integer"}, "rulesetVersion": enum("CLASSIC_V1"), "hands": array(CARDS, 2, 2), "firstSeat": SEAT, "currentSeat": SEAT, "isFirstMove": BOOL, "targetMove": nullable(ref("Move")), "lastPlaySeat": nullable(SEAT), "consecutivePasses": integer(0, 1)}, description="hands[0]=USER，hands[1]=BOT；CLASSIC_V1 固定不支持四带二。")
obj("Empty", {})
obj("IdRequest", {"id": ID})
obj("VersionRequest", {"id": ID, "expectedRowVersion": integer()})
obj("FieldError", {"field": string(128), "reason": string(300)})
obj("ErrorData", {"fieldErrors": array(ref("FieldError")), "currentVersion": nullable(integer()), "retryAfterSeconds": nullable(integer())}, required=[])
obj("ErrorResponse", {"code": integer(40000, 59999), "message": string(500), "data": nullable(ref("ErrorData")), "requestId": string(64)})


def page_request(name, extra=None):
    props = {"pageNum": integer(1, 100000, default=1), "pageSize": integer(1, 100, default=20)}
    props.update(extra or {})
    return obj(name, props, required=[])


def page(name, item):
    return obj(name, {"items": array(ref(item)), "total": integer(0, 9007199254740991), "pageNum": integer(1), "pageSize": integer(1, 100)})


obj("Csrf", {"token": string(512), "headerName": enum("X-CSRF-TOKEN")})
obj("LoginRequest", {"username": string(64, minLength=3), "password": string(64, minLength=1, format="password")})
obj("User", {"id": ID, "username": string(64), "displayName": string(64), "role": ROLE, "status": enum("ENABLED", "DISABLED"), "rowVersion": integer(), "permissions": array(string(64))})
obj("CreateUserRequest", {"username": string(64, minLength=3, pattern=r"^[a-z0-9][a-z0-9_.-]{2,63}$"), "displayName": string(64, minLength=1), "password": string(64, minLength=12, format="password"), "role": ROLE})
obj("UserStatusRequest", {"id": ID, "expectedRowVersion": integer(), "status": enum("ENABLED", "DISABLED")})
obj("ResetPasswordRequest", {"id": ID, "expectedRowVersion": integer(), "newPassword": string(64, minLength=12, format="password")})
page_request("UserSearchRequest", {"keyword": string(64), "status": enum("ENABLED", "DISABLED")})
page("UserPage", "User")

SYSTEM_PROPS = {"code": string(64, pattern=r"^[a-z][a-z0-9-]{1,63}$"), "name": string(64, minLength=1), "description": string(300), "iconKey": enum("cards", "work-order", "grid"), "entryType": enum("INTERNAL", "EXTERNAL"), "route": nullable(enum("/landlord/puzzles")), "externalUrl": nullable(string(2048, format="uri")), "visibleRoles": {**array(ROLE, 2, 1), "uniqueItems": True}, "sortOrder": integer(0, 10000), "enabled": BOOL}
obj("SystemCard", {"id": ID, **SYSTEM_PROPS, "rowVersion": integer(), "availability": enum("READY", "UNCONFIGURED", "DISABLED")})
obj("SystemSaveRequest", {"id": ID, "expectedRowVersion": integer(), **SYSTEM_PROPS}, required=list(SYSTEM_PROPS), description="新增省略 id/expectedRowVersion；更新两者必填。entryType 决定 route/externalUrl 互斥。code 创建后不可变。")
obj("SystemSearchRequest", {"scope": enum("VISIBLE", "MANAGE")}, required=[])
obj("SystemList", {"items": array(ref("SystemCard"), 100)})

META = {"title": string(120, minLength=1), "description": string(2000), "tags": {**array(string(32, minLength=1), 10), "uniqueItems": True}, "difficulty": DIFF}
obj("PuzzleVersion", {"id": ID, "puzzleId": ID, "versionNo": integer(1), **META, "allowedFirstSeats": {**array(SEAT, 2, 1), "uniqueItems": True}, "state": ref("GameState"), "stateHash": string(64, pattern="^[a-f0-9]{64}$"), "createTime": TIME})
obj("Puzzle", {"id": ID, "ownerId": ID, "status": enum("DRAFT", "PUBLISHED", "ARCHIVED"), "rowVersion": integer(), "latestVersionId": ID, "publishedVersionId": nullable(ID), "version": ref("PuzzleVersion"), "proofStatus": PROOF, "winnerSide": nullable(SIDE), "allowedActions": array(enum("EDIT", "COPY", "PUBLISH", "ARCHIVE", "RESTORE", "PLAY", "ANALYZE"))})
page_request("PuzzleSearchRequest", {"scope": enum("PUBLISHED", "MINE", "MANAGE"), "keyword": string(120), "tag": string(32), "difficulty": DIFF, "status": enum("DRAFT", "PUBLISHED", "ARCHIVED")})
page("PuzzlePage", "Puzzle")
obj("PuzzleDetailRequest", {"id": ID, "versionId": ID}, required=["id"])
obj("PuzzleProofStrategyRequest", {"versionId": ID})
obj("PuzzleValidateRequest", {"state": ref("GameState")})
obj("ValidationResult", {"valid": BOOL, "errors": array(ref("FieldError")), "canonicalState": nullable(ref("GameState")), "stateHash": nullable(string(64))})
obj("PuzzleDuplicateCheckRequest", {"state": ref("GameState"), "excludePuzzleId": nullable(ID)}, required=["state"], description="按完整初始局面结构化匹配，包含逻辑删除记录；编辑时可排除自身题目。")
obj("PuzzleDuplicate", {"id": ID, "versionId": ID, "title": string(120), "status": enum("DRAFT", "PUBLISHED", "ARCHIVED"), "deleted": BOOL, "stateHash": string(64)})
obj("PuzzleDuplicateCheck", {"duplicate": BOOL, "existing": nullable(ref("PuzzleDuplicate"))})
obj("PuzzleSaveRequest", {"id": ID, "expectedRowVersion": integer(), **META, "allowedFirstSeats": {**array(SEAT, 2, 1), "uniqueItems": True}, "state": ref("GameState")}, required=[*META, "allowedFirstSeats", "state"], description="新增省略 id/expectedRowVersion；更新两者必填。每次成功保存新增不可变版本。")
obj("PuzzlePublishRequest", {"id": ID, "expectedRowVersion": integer(), "versionId": ID})
obj("PuzzleCopyRequest", {"versionId": ID, "title": string(120, minLength=1)})

obj("GameCreateRequest", {"puzzleVersionId": ID, "playerSeat": {"type": "const", "const": 0, "description": "当前用户固定为 USER，值为0"}, "firstSeat": SEAT, "mode": enum("TRAINING", "OPTIMAL"), "proofResultId": ID, "swapSeats": {"type": "boolean", "default": False, "description": "开局前交换双方手牌和先手；机器人获得原 USER 手牌"}}, required=["puzzleVersionId", "playerSeat", "firstSeat", "mode"], description="firstSeat=0 表示原配置 USER 先手，1 表示原配置 BOT 先手；swapSeats=true 时双方手牌和先手同时互换，原残局和证明不变。")
obj("Game", {"id": ID, "puzzleVersionId": ID, "playerSeat": {"type": "const", "const": 0}, "firstSeat": SEAT, "mode": enum("TRAINING", "OPTIMAL"), "status": enum("ACTIVE", "FINISHED", "ABANDONED"), "stateVersion": integer(), "state": ref("GameState"), "winnerSide": nullable(SIDE), "robotStatus": enum("IDLE", "QUEUED", "THINKING", "NEEDS_PROOF", "FAILED"), "hintCount": integer(), "allowedActions": array(enum("PLAY", "PASS", "HINT", "ABANDON", "RESTART", "RESUME")), "createTime": TIME})
obj("GameVersionRequest", {"gameId": ID, "expectedStateVersion": integer()})
obj("PlayRequest", {"gameId": ID, "expectedStateVersion": integer(), "move": ref("Move")}, description="本端点拒绝 PASS；PASS 使用独立接口。")
obj("GameRestartRequest", {"gameId": ID, "expectedStateVersion": integer(), "mode": enum("TRAINING", "OPTIMAL"), "proofResultId": ID}, required=["gameId", "expectedStateVersion", "mode"])
obj("GameResumeRequest", {"gameId": ID, "expectedStateVersion": integer(), "proofResultId": ID})
page_request("LegalMovesRequest", {"gameId": ID, "expectedStateVersion": integer()})
S["LegalMovesRequest"]["required"] = ["gameId", "expectedStateVersion"]
obj("LegalMove", {"move": ref("Move"), "proofStatus": PROOF, "winnerSide": nullable(SIDE)})
page("LegalMovePage", "LegalMove")
obj("GameAction", {"sequenceNo": integer(1), "actorSeat": SEAT, "actorType": enum("HUMAN", "BOT"), "move": ref("Move"), "beforeVersion": integer(), "afterVersion": integer(1), "afterState": ref("GameState"), "decisionQuality": enum("OPTIMAL", "PROVEN", "HEURISTIC", "HUMAN"), "createTime": TIME})
obj("HintResponse", {"move": ref("Move"), "proofStatus": PROOF, "winnerSide": nullable(SIDE), "hintCount": integer(), "stateVersion": integer()}, description="V1 同步提示只返回规则合法建议；proofStatus 固定 UNKNOWN，winnerSide 必须为空，不修改对局状态。")
page_request("ReplayRequest", {"gameId": ID})
S["ReplayRequest"]["required"] = ["gameId"]
obj("Replay", {"game": ref("Game"), "initialState": ref("GameState"), "actions": array(ref("GameAction")), "total": integer(), "pageNum": integer(1), "pageSize": integer(1, 100)})

obj("Budget", {"timeLimitMs": integer(100, 60000, default=30000), "nodeLimit": integer(1000, 5000000, default=1000000), "memoryLimitMb": integer(16, 128, default=64)})
obj("PuzzleSource", {"type": enum("PUZZLE_VERSION"), "puzzleVersionId": ID})
obj("GameSource", {"type": enum("GAME"), "gameId": ID, "expectedStateVersion": integer()})
obj("CustomSource", {"type": enum("CUSTOM"), "state": ref("GameState")})
obj("SolveCreateRequest", {"source": {"oneOf": [ref("PuzzleSource"), ref("GameSource"), ref("CustomSource")], "discriminator": {"propertyName": "type", "mapping": {"PUZZLE_VERSION": "#/components/schemas/PuzzleSource", "GAME": "#/components/schemas/GameSource", "CUSTOM": "#/components/schemas/CustomSource"}}}, "budget": ref("Budget")}, required=["source"])
obj("SolveStats", {"elapsedMs": integer(0, 9007199254740991), "queueTimeMs": integer(0, 9007199254740991), "visitedNodes": integer(0, 9007199254740991), "cacheHits": integer(0, 9007199254740991)})
obj("WinningLine", {"lineNo": integer(1, 5), "moves": array(ref("Move"), 256), "verified": {"const": True, "type": "boolean"}})
obj("SolveResult", {"id": ID, "proofStatus": PROOF, "winnerSide": nullable(SIDE), "proofValue": nullable({"type": "integer", "enum": [-1, 1]}), "strategyStatus": STRATEGY, "rootNodeId": nullable(ID), "recommendedMove": nullable(ref("Move")), "winningLines": array(ref("WinningLine"), 5), "solverVersion": string(64), "rulesetVersion": enum("CLASSIC_V1"), "stats": ref("SolveStats")}, description="UNKNOWN 必须 winnerSide/proofValue=null；COMPLETE 必须 PROVEN；winningLines 最多展示5条已验证路线，不能替代完整多分支证明。")
obj("SolveTask", {"id": ID, "taskStatus": enum("QUEUED", "RUNNING", "COMPLETED", "CANCELLED", "FAILED"), "purpose": enum("ANALYSIS", "HINT"), "effectiveBudget": ref("Budget"), "stateHash": string(64), "result": nullable(ref("SolveResult")), "terminationReason": nullable(enum("PROVED", "TIME_LIMIT", "NODE_LIMIT", "MEMORY_LIMIT", "CANCELLED", "ERROR")), "stats": ref("SolveStats"), "createTime": TIME, "startTime": nullable(TIME), "finishTime": nullable(TIME)})
page_request("TaskSearchRequest", {"taskStatus": enum("QUEUED", "RUNNING", "COMPLETED", "CANCELLED", "FAILED")})
page("TaskPage", "SolveTask")
obj("StrategyNode", {"id": ID, "state": ref("GameState"), "proofStatus": PROOF, "winnerSide": nullable(SIDE), "coverage": enum("TERMINAL", "ONE_WINNING", "ALL_REPLIES", "PARTIAL"), "terminal": BOOL})
obj("StrategyEdge", {"move": ref("Move"), "child": ref("StrategyNode"), "recommended": BOOL})
page_request("StrategyChildrenRequest", {"resultId": ID, "nodeId": ID})
S["StrategyChildrenRequest"]["required"] = ["resultId", "nodeId"]
obj("StrategyChildren", {"node": ref("StrategyNode"), "edges": array(ref("StrategyEdge")), "total": integer(), "pageNum": integer(1), "pageSize": integer(1, 100)})

paths = {}
endpoint_rows = []


def endpoint(path, name, group, summary, request, response, permission, mutate=False, method="post", auth=True, description=""):
    response_name = response + "Response"
    if response_name not in S:
        obj(response_name, {"code": {"const": 200, "type": "integer"}, "message": string(500), "data": ref(response), "requestId": string(64)})
    success_description = "成功（同步返回）" if response == "HintResponse" else "成功（异步创建仅表示已入队）"
    op = {"operationId": name, "tags": [group], "summary": summary,
          "description": f"权限：{permission}。{description}",
          "security": ([{"SessionCookie": [], "CsrfHeader": []}] if method == "post" else [{"SessionCookie": []}]) if auth else ([{"CsrfHeader": []}] if method == "post" else []),
          "responses": {"200": {"description": success_description, "content": {"application/json": {"schema": ref(response_name)}}},
                        **{str(code): {"description": label, "content": {"application/json": {"schema": ref("ErrorResponse")}}} for code, label in [(400,"参数错误"),(401,"未认证"),(403,"权限或CSRF错误"),(404,"不存在或不可访问"),(409,"版本、状态或幂等冲突"),(422,"规则校验失败"),(429,"限流或队列已满"),(500,"服务异常")]}}}
    if request:
        op["requestBody"] = {"required": True, "content": {"application/json": {"schema": ref(request)}}}
    if mutate:
        op["parameters"] = [{"name": "Idempotency-Key", "in": "header", "required": True, "schema": string(64, minLength=16, pattern=r"^[A-Za-z0-9_-]+$"), "description": "同一用户、同一路径、相同业务动作重试复用，建议UUID"}]
    paths[path] = {method: op}
    endpoint_rows.append((group, method.upper(), path, request or "—", response, permission, mutate))


endpoint("/api/auth/csrf", "getCsrf", "identity", "获取CSRF令牌", None, "Csrf", "匿名", method="get", auth=False)
endpoint("/api/auth/login", "login", "identity", "登录", "LoginRequest", "User", "匿名", auth=False)
endpoint("/api/auth/me", "currentUser", "identity", "当前用户", "Empty", "User", "登录")
endpoint("/api/auth/guest", "createGuest", "identity", "创建游客会话", "Empty", "User", "匿名 + CSRF", auth=False)
endpoint("/api/auth/logout", "logout", "identity", "退出", "Empty", "Empty", "登录")
endpoint("/api/admin/users/search", "searchUsers", "identity", "账号分页", "UserSearchRequest", "UserPage", "user:manage")
endpoint("/api/admin/users/create", "createUser", "identity", "创建账号", "CreateUserRequest", "User", "user:manage", True)
endpoint("/api/admin/users/status", "changeUserStatus", "identity", "启停账号", "UserStatusRequest", "User", "user:manage", True)
endpoint("/api/admin/users/reset-password", "resetPassword", "identity", "重置密码", "ResetPasswordRequest", "User", "user:manage", True)
endpoint("/api/hub/systems/search", "searchSystems", "hub", "系统入口", "SystemSearchRequest", "SystemList", "hub:read；MANAGE另需hub:manage")
endpoint("/api/hub/systems/save", "saveSystem", "hub", "新增或更新入口", "SystemSaveRequest", "SystemCard", "hub:manage", True)
endpoint("/api/puzzles/search", "searchPuzzles", "puzzle", "题库分页", "PuzzleSearchRequest", "PuzzlePage", "puzzle:read；MANAGE另需puzzle:publish")
endpoint("/api/puzzles/detail", "puzzleDetail", "puzzle", "题目与版本详情", "PuzzleDetailRequest", "Puzzle", "puzzle:read + 版本可读")
endpoint("/api/puzzles/proof-strategy", "puzzleProofStrategy", "puzzle", "读取已证明策略", "PuzzleProofStrategyRequest", "SolveTask", "puzzle:read + 已发布版本可读", description="只返回 PROVEN 结果以及最多5条已验证代表路线；UNKNOWN 不展示必胜策略。")
endpoint("/api/puzzles/validate", "validatePuzzle", "puzzle", "校验局面", "PuzzleValidateRequest", "ValidationResult", "puzzle:write", description="结构合法但规则错误时返回200 valid=false；结构错误400。")
endpoint("/api/puzzles/duplicate-check", "checkPuzzleDuplicate", "puzzle", "检查重复残局", "PuzzleDuplicateCheckRequest", "PuzzleDuplicateCheck", "puzzle:write", description="匹配范围包含已逻辑删除版本；命中时复用既有题目版本和证明任务，不重复创建。")
endpoint("/api/puzzles/save", "savePuzzle", "puzzle", "保存新版本", "PuzzleSaveRequest", "Puzzle", "puzzle:write + 所有者或ADMIN", True)
endpoint("/api/puzzles/publish", "publishPuzzle", "puzzle", "发布当前草稿", "PuzzlePublishRequest", "Puzzle", "puzzle:publish", True)
endpoint("/api/puzzles/archive", "archivePuzzle", "puzzle", "逻辑删除", "VersionRequest", "Puzzle", "puzzle:archive", True, description="只标记 deleted=1/ARCHIVED，保留版本、对局快照和证明任务。")
endpoint("/api/puzzles/restore", "restorePuzzle", "puzzle", "恢复为草稿", "VersionRequest", "Puzzle", "puzzle:archive", True)
endpoint("/api/puzzles/copy", "copyPuzzle", "puzzle", "复制为新草稿", "PuzzleCopyRequest", "Puzzle", "puzzle:write + 来源版本可读", True)
endpoint("/api/games/create", "createGame", "game", "创建对局", "GameCreateRequest", "Game", "game:play + 版本可读且未归档", True)
endpoint("/api/games/detail", "gameDetail", "game", "对局详情", "IdRequest", "Game", "game:play + 所有者")
endpoint("/api/games/actions/play", "playMove", "game", "出牌", "PlayRequest", "Game", "game:play + 所有者 + 当前人类座位", True)
endpoint("/api/games/actions/pass", "passMove", "game", "不出", "GameVersionRequest", "Game", "game:play + 所有者 + 当前人类座位", True)
endpoint("/api/games/legal-moves/search", "legalMoves", "game", "合法行动分页", "LegalMovesRequest", "LegalMovePage", "game:play + 所有者")
endpoint("/api/games/hint", "hint", "game", "同步提示分析", "GameVersionRequest", "HintResponse", "game:play + 所有者 + 当前人类座位", True, description="V1 同步返回一条规则合法建议，proofStatus 固定 UNKNOWN；后续可扩展为异步 SolveTask。")
endpoint("/api/games/abandon", "abandonGame", "game", "放弃", "GameVersionRequest", "Game", "game:play + 所有者", True)
endpoint("/api/games/restart", "restartGame", "game", "新建重开对局", "GameRestartRequest", "Game", "game:play + 所有者", True)
endpoint("/api/games/resume", "resumeGame", "game", "用已证明策略恢复机器人", "GameResumeRequest", "Game", "game:play + 所有者 + 结果可访问", True)
endpoint("/api/games/replay/detail", "gameReplay", "game", "复盘分页", "ReplayRequest", "Replay", "game:play + 所有者")
endpoint("/api/analysis/tasks/create", "createSolveTask", "analysis", "创建分析", "SolveCreateRequest", "SolveTask", "analysis:run + 来源可读", True)
endpoint("/api/analysis/tasks/search", "searchSolveTasks", "analysis", "我的分析记录", "TaskSearchRequest", "TaskPage", "analysis:run + 所有者")
endpoint("/api/analysis/tasks/detail", "solveTaskDetail", "analysis", "分析详情", "IdRequest", "SolveTask", "analysis:run + 所有者")
endpoint("/api/analysis/tasks/cancel", "cancelSolveTask", "analysis", "取消任务", "IdRequest", "SolveTask", "analysis:run + 所有者", True)
endpoint("/api/analysis/strategies/children", "strategyChildren", "analysis", "策略子分支分页", "StrategyChildrenRequest", "StrategyChildren", "analysis:run + 结果所属任务所有者")

spec = {"openapi": "3.1.0", "info": {"title": "NexusHub V1 API", "version": "1.0.0-design", "description": "开发前契约，尚无运行服务。全部业务POST，只有CSRF引导为GET。跨字段领域规则见模块文档。"}, "servers": [{"url": "/", "description": "同源部署"}], "tags": [{"name": n} for n in ["identity", "hub", "puzzle", "game", "analysis"]], "paths": paths, "components": {"securitySchemes": {"SessionCookie": {"type": "apiKey", "in": "cookie", "name": "NEXUSHUB_SESSION"}, "CsrfHeader": {"type": "apiKey", "in": "header", "name": "X-CSRF-TOKEN"}}, "schemas": S}}
(DOC / "api").mkdir(exist_ok=True)
(DOC / "api/openapi.json").write_text(json.dumps(spec, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
rows = ["# 接口索引", "", "基线 V1.0-design。由 `tooling/generate_contracts.py` 生成；修改契约请先更新生成源，再同步模块说明。", "", "[公共规范](api/common.md) · [机器契约](api/openapi.json)", "", "所有类型定义在 OpenAPI components.schemas；权限说明是强制服务端约束。表中幂等为“是”的接口必须携带 Idempotency-Key。", "", "| 模块 | 方法/路径 | 请求类型 | data 类型 | 权限 | 幂等 |", "|---|---|---|---|---|---|"]
for group, method, path, req, res, permission, idem in endpoint_rows:
    rows.append(f"| [{group}](api/{group}.md) | `{method} {path}` | {req} | {res} | {permission} | {'是' if idem else '否'} |")
rows += ["", "接口默认排序：题目与任务 update/create_time DESC,id DESC；对局事件 sequence_no ASC；入口 sort_order ASC,id ASC；不接受任意 SQL 排序字段。", "", "非目标：首版不提供 OpsDesk 代理接口、SSO、真人对战和直接改写在线手牌接口。"]
(DOC / "api-contract.md").write_text("\n".join(rows) + "\n", encoding="utf-8")

# 同一元数据同时生成 SQL 与字段字典，避免手工复制漂移。
tables = []


def table(name, description, columns, constraints):
    tables.append((name, description, columns, constraints))


AUDIT = [("id", "BIGINT NOT NULL", "应用生成的正整数ID"), ("create_time", "DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)", "UTC创建时间"), ("update_time", "DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)", "UTC更新时间，应用显式维护"), ("create_by", "BIGINT NULL", "创建者，系统任务可空"), ("update_by", "BIGINT NULL", "最后更新者"), ("deleted", "TINYINT NOT NULL DEFAULT 0", "逻辑删除0正常1删除")]
table("sys_user", "本系统账号；固定单角色", [
    ("username", "VARCHAR(64) NOT NULL", "规范化用户名，删除后不复用"), ("display_name", "VARCHAR(64) NOT NULL", "显示名"), ("password_hash", "VARCHAR(255) NOT NULL", "BCrypt散列，不对外返回"), ("role", "VARCHAR(16) NOT NULL", "GUEST、USER或ADMIN"), ("status", "VARCHAR(16) NOT NULL DEFAULT 'ENABLED'", "账号状态"), ("row_version", "INT NOT NULL DEFAULT 0", "并发版本"), ("credential_version", "INT NOT NULL DEFAULT 0", "重置/停用时递增以撤销会话")], ["UNIQUE KEY uk_user_username (username)", "CHECK (role IN ('GUEST','USER','ADMIN'))", "CHECK (status IN ('ENABLED','DISABLED'))"])
table("hub_system", "可扩展系统卡片", [
    ("code", "VARCHAR(64) NOT NULL", "不可变唯一编码"), ("name", "VARCHAR(64) NOT NULL", "名称"), ("description", "VARCHAR(300) NOT NULL DEFAULT ''", "简介"), ("icon_key", "VARCHAR(32) NOT NULL", "内置图标Key"), ("entry_type", "VARCHAR(16) NOT NULL", "INTERNAL或EXTERNAL"), ("route", "VARCHAR(255) NULL", "内部登记路由"), ("external_url", "VARCHAR(2048) NULL", "批准的外部地址；空代表待配置"), ("visible_roles", "JSON NOT NULL", "可见角色数组"), ("sort_order", "INT NOT NULL DEFAULT 0", "升序排序"), ("enabled", "TINYINT NOT NULL DEFAULT 1", "是否启用"), ("row_version", "INT NOT NULL DEFAULT 0", "并发版本")], ["UNIQUE KEY uk_system_code (code)", "KEY idx_system_order (enabled, deleted, sort_order, id)", "CHECK (enabled IN (0,1))", "CHECK ((entry_type='INTERNAL' AND route IS NOT NULL AND external_url IS NULL) OR (entry_type='EXTERNAL' AND route IS NULL))"])
table("puzzle", "题目生命周期与版本指针", [
    ("owner_id", "BIGINT NOT NULL", "所有者"), ("status", "VARCHAR(16) NOT NULL DEFAULT 'DRAFT'", "DRAFT/PUBLISHED/ARCHIVED"), ("latest_version_id", "BIGINT NULL", "最新草稿；仅创建事务中短暂允许空"), ("published_version_id", "BIGINT NULL", "公开版本，归档保留，恢复清空"), ("row_version", "INT NOT NULL DEFAULT 0", "每次保存或状态动作递增")], ["KEY idx_puzzle_owner (owner_id, deleted, update_time, id)", "KEY idx_puzzle_status (status, deleted, update_time, id)", "FOREIGN KEY (owner_id) REFERENCES sys_user(id)", "CHECK (status IN ('DRAFT','PUBLISHED','ARCHIVED'))", "CHECK (status<>'PUBLISHED' OR published_version_id IS NOT NULL)"])
table("puzzle_version", "不可变题目内容和局面", [
    ("puzzle_id", "BIGINT NOT NULL", "所属题目"), ("version_no", "INT NOT NULL", "题内递增版本号"), ("title", "VARCHAR(120) NOT NULL", "标题"), ("description", "TEXT NOT NULL", "说明，API最多2000字符"), ("tags", "JSON NOT NULL", "去重标签数组，最多10个"), ("difficulty", "VARCHAR(16) NOT NULL", "人工难度"), ("allowed_first_seats", "JSON NOT NULL", "允许的先手数组，只能含0(USER)/1(BOT)"), ("ruleset_version", "VARCHAR(32) NOT NULL", "CLASSIC_V1"), ("schema_version", "INT NOT NULL DEFAULT 1", "局面序列化版本"), ("initial_state", "JSON NOT NULL", "完整双人GameState"), ("state_hash", "CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL", "规范化状态SHA256；命中后还需核对原文")], ["UNIQUE KEY uk_puzzle_version (puzzle_id, version_no)", "UNIQUE KEY uk_version_parent (puzzle_id, id)", "KEY idx_version_hash (state_hash)", "FOREIGN KEY (puzzle_id) REFERENCES puzzle(id)", "CHECK (difficulty IN ('EASY','MEDIUM','HARD'))", "CHECK (version_no>0)"])
table("game_session", "对局权威快照", [
    ("owner_id", "BIGINT NOT NULL", "用户"), ("puzzle_version_id", "BIGINT NOT NULL", "绑定版本"), ("first_seat", "TINYINT NOT NULL", "首手座位0(USER)/1(BOT)"), ("mode", "VARCHAR(16) NOT NULL", "TRAINING/OPTIMAL"), ("status", "VARCHAR(16) NOT NULL DEFAULT 'ACTIVE'", "对局状态"), ("initial_state", "JSON NOT NULL", "创建快照"), ("current_state", "JSON NOT NULL", "当前完整状态"), ("state_version", "INT NOT NULL DEFAULT 0", "行动/放弃时递增"), ("winner_side", "VARCHAR(16) NULL", "终局阵营USER/BOT"), ("robot_status", "VARCHAR(16) NOT NULL DEFAULT 'IDLE'", "机器人调度状态"), ("hint_count", "INT NOT NULL DEFAULT 0", "使用提示次数"), ("proof_result_id", "BIGINT NULL", "当前最优模式引用的结果，后置外键"), ("parent_game_id", "BIGINT NULL", "重开的来源"), ("finished_time", "DATETIME(3) NULL", "终态时间")], ["KEY idx_game_owner (owner_id, status, deleted, update_time, id)", "FOREIGN KEY (owner_id) REFERENCES sys_user(id)", "FOREIGN KEY (puzzle_version_id) REFERENCES puzzle_version(id)", "FOREIGN KEY (parent_game_id) REFERENCES game_session(id)", "CHECK (first_seat BETWEEN 0 AND 1)", "CHECK (mode IN ('TRAINING','OPTIMAL'))", "CHECK (status IN ('ACTIVE','FINISHED','ABANDONED'))", "CHECK ((status='FINISHED' AND winner_side IS NOT NULL AND winner_side IN ('USER','BOT')) OR (status<>'FINISHED' AND winner_side IS NULL))", "CHECK (robot_status IN ('IDLE','QUEUED','THINKING','NEEDS_PROOF','FAILED'))"])
table("game_action", "不可变出牌/不出事件", [
    ("game_id", "BIGINT NOT NULL", "所属对局"), ("sequence_no", "INT NOT NULL", "从1连续递增"), ("actor_seat", "TINYINT NOT NULL", "USER=0/BOT=1"), ("actor_type", "VARCHAR(16) NOT NULL", "HUMAN/BOT"), ("move_json", "JSON NOT NULL", "规范化Move"), ("before_version", "INT NOT NULL", "前版本"), ("after_version", "INT NOT NULL", "后版本"), ("after_state", "JSON NOT NULL", "行动后的完整快照"), ("decision_quality", "VARCHAR(16) NOT NULL", "OPTIMAL/PROVEN/HEURISTIC/HUMAN"), ("request_key", "VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL", "人类幂等Key或机器人作业Key")], ["UNIQUE KEY uk_action_sequence (game_id, sequence_no)", "UNIQUE KEY uk_action_request (game_id, request_key)", "FOREIGN KEY (game_id) REFERENCES game_session(id)", "CHECK (actor_seat BETWEEN 0 AND 1)", "CHECK (after_version=before_version+1)", "CHECK (actor_type IN ('HUMAN','BOT'))", "CHECK (decision_quality IN ('OPTIMAL','PROVEN','HEURISTIC','HUMAN'))"])
table("game_bot_job", "机器人持久调度，可重启恢复", [
    ("game_id", "BIGINT NOT NULL", "对局"), ("state_version", "INT NOT NULL", "依据的版本"), ("status", "VARCHAR(16) NOT NULL DEFAULT 'QUEUED'", "QUEUED/RUNNING/DONE/CANCELLED/FAILED"), ("lease_token", "VARCHAR(64) NULL", "每次领取生成的新隔离令牌"), ("lease_until", "DATETIME(3) NULL", "租约失效时间"), ("attempt_count", "INT NOT NULL DEFAULT 0", "领取次数"), ("available_time", "DATETIME(3) NOT NULL", "重试可领取时间")], ["UNIQUE KEY uk_bot_version (game_id, state_version)", "KEY idx_bot_claim (status, available_time, lease_until)", "FOREIGN KEY (game_id) REFERENCES game_session(id)", "CHECK (status IN ('QUEUED','RUNNING','DONE','CANCELLED','FAILED'))"])
table("solve_task", "持久分析任务及计算快照", [
    ("owner_id", "BIGINT NOT NULL", "提交者"), ("purpose", "VARCHAR(16) NOT NULL", "ANALYSIS/HINT"), ("source_type", "VARCHAR(24) NOT NULL", "PUZZLE_VERSION/GAME/CUSTOM"), ("puzzle_version_id", "BIGINT NULL", "题目来源"), ("game_id", "BIGINT NULL", "对局来源"), ("game_state_version", "INT NULL", "对局来源版本"), ("state_snapshot", "JSON NOT NULL", "不可变求解输入"), ("state_hash", "CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL", "规范化摘要"), ("ruleset_version", "VARCHAR(32) NOT NULL", "规则版本"), ("solver_version", "VARCHAR(64) NOT NULL", "运行算法版本，领取时固定"), ("task_status", "VARCHAR(16) NOT NULL DEFAULT 'QUEUED'", "任务状态"), ("time_limit_ms", "INT NOT NULL", "实际预算"), ("node_limit", "BIGINT NOT NULL", "节点上限"), ("memory_limit_mb", "INT NOT NULL", "搜索缓存预算"), ("termination_reason", "VARCHAR(24) NULL", "结束原因"), ("lease_token", "VARCHAR(64) NULL", "领取隔离令牌"), ("lease_until", "DATETIME(3) NULL", "租约截止"), ("attempt_count", "INT NOT NULL DEFAULT 0", "领取次数"), ("available_time", "DATETIME(3) NOT NULL", "排队/重试时间"), ("start_time", "DATETIME(3) NULL", "首次开始时间"), ("finish_time", "DATETIME(3) NULL", "结束时间"), ("stats_json", "JSON NOT NULL", "SolveStats进度快照"), ("error_code", "VARCHAR(64) NULL", "脱敏错误码")], ["KEY idx_task_owner (owner_id, deleted, create_time, id)", "KEY idx_task_claim (task_status, available_time, lease_until)", "KEY idx_task_hash (state_hash, solver_version)", "FOREIGN KEY (owner_id) REFERENCES sys_user(id)", "FOREIGN KEY (puzzle_version_id) REFERENCES puzzle_version(id)", "FOREIGN KEY (game_id) REFERENCES game_session(id)", "CHECK (task_status IN ('QUEUED','RUNNING','COMPLETED','CANCELLED','FAILED'))", "CHECK (purpose IN ('ANALYSIS','HINT'))", "CHECK (time_limit_ms BETWEEN 100 AND 60000)", "CHECK (node_limit BETWEEN 1000 AND 5000000)", "CHECK (memory_limit_mb BETWEEN 16 AND 128)", "CHECK ((source_type='CUSTOM' AND puzzle_version_id IS NULL AND game_id IS NULL AND game_state_version IS NULL) OR (source_type='PUZZLE_VERSION' AND puzzle_version_id IS NOT NULL AND game_id IS NULL AND game_state_version IS NULL) OR (source_type='GAME' AND puzzle_version_id IS NULL AND game_id IS NOT NULL AND game_state_version IS NOT NULL))"])
table("solve_result", "每任务一个根结论", [
    ("task_id", "BIGINT NOT NULL", "所属任务唯一"), ("proof_status", "VARCHAR(16) NOT NULL", "PROVEN/UNKNOWN"), ("winner_side", "VARCHAR(16) NULL", "获胜阵营USER/BOT"), ("proof_value", "TINYINT NULL", "USER视角+1或-1"), ("strategy_status", "VARCHAR(16) NOT NULL", "NONE/PARTIAL/COMPLETE"), ("root_node_id", "BIGINT NULL", "证明根节点，后置复合外键"), ("winning_lines_json", "JSON NOT NULL", "最多5条已验证展示路线"), ("stats_json", "JSON NOT NULL", "最终统计"), ("verified_time", "DATETIME(3) NULL", "完整证明校验时间")], ["UNIQUE KEY uk_result_task (task_id)", "FOREIGN KEY (task_id) REFERENCES solve_task(id)", "CHECK ((proof_status='UNKNOWN' AND winner_side IS NULL AND proof_value IS NULL) OR (proof_status='PROVEN' AND winner_side IS NOT NULL AND proof_value IS NOT NULL AND ((winner_side='USER' AND proof_value=1) OR (winner_side='BOT' AND proof_value=-1))))", "CHECK (strategy_status IN ('NONE','PARTIAL','COMPLETE'))", "CHECK (strategy_status<>'COMPLETE' OR (proof_status='PROVEN' AND root_node_id IS NOT NULL AND verified_time IS NOT NULL))"])
table("strategy_node", "结果内去重证明局面", [
    ("result_id", "BIGINT NOT NULL", "所属结果"), ("state_hash", "CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL", "规范化摘要"), ("state_json", "JSON NOT NULL", "局面"), ("proof_status", "VARCHAR(16) NOT NULL", "节点证明状态"), ("winner_side", "VARCHAR(16) NULL", "节点获胜方USER/BOT"), ("coverage", "VARCHAR(16) NOT NULL", "TERMINAL/ONE_WINNING/ALL_REPLIES/PARTIAL"), ("terminal", "TINYINT NOT NULL DEFAULT 0", "是否终局")], ["UNIQUE KEY uk_node_hash (result_id, state_hash)", "UNIQUE KEY uk_node_result (result_id, id)", "FOREIGN KEY (result_id) REFERENCES solve_result(id)", "CHECK (terminal IN (0,1))", "CHECK ((proof_status='UNKNOWN' AND winner_side IS NULL) OR (proof_status='PROVEN' AND winner_side IS NOT NULL AND winner_side IN ('USER','BOT')))", "CHECK (coverage IN ('TERMINAL','ONE_WINNING','ALL_REPLIES','PARTIAL'))"])
table("strategy_edge", "证明行动边及覆盖", [
    ("result_id", "BIGINT NOT NULL", "所属结果"), ("from_node_id", "BIGINT NOT NULL", "起点"), ("to_node_id", "BIGINT NOT NULL", "终点"), ("move_json", "JSON NOT NULL", "合法行动"), ("move_key", "VARCHAR(160) CHARACTER SET ascii COLLATE ascii_bin NOT NULL", "规范化行动字符串"), ("recommended", "TINYINT NOT NULL DEFAULT 0", "是否建议"), ("sort_order", "INT NOT NULL", "稳定分页序号")], ["UNIQUE KEY uk_edge_move (result_id, from_node_id, move_key)", "KEY idx_edge_children (result_id, from_node_id, sort_order, id)", "FOREIGN KEY (result_id, from_node_id) REFERENCES strategy_node(result_id, id)", "FOREIGN KEY (result_id, to_node_id) REFERENCES strategy_node(result_id, id)", "CHECK (recommended IN (0,1))"])
table("audit_log", "关键动作审计，不存秘密", [
    ("actor_id", "BIGINT NULL", "操作者"), ("action", "VARCHAR(64) NOT NULL", "业务动作码"), ("resource_type", "VARCHAR(32) NOT NULL", "资源类型"), ("resource_id", "BIGINT NULL", "资源ID"), ("request_id", "VARCHAR(64) NOT NULL", "请求关联"), ("outcome", "VARCHAR(16) NOT NULL", "SUCCESS/FAILURE"), ("details_json", "JSON NOT NULL", "脱敏差异或错误码")], ["KEY idx_audit_resource (resource_type, resource_id, create_time)", "KEY idx_audit_actor (actor_id, create_time)", "CHECK (outcome IN ('SUCCESS','FAILURE'))"])
table("idempotency_record", "写请求幂等；凭证类不缓存完整请求", [
    ("owner_id", "BIGINT NOT NULL", "调用者"), ("operation", "VARCHAR(128) CHARACTER SET ascii COLLATE ascii_bin NOT NULL", "API路径或内部操作码"), ("request_key", "VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL", "幂等键"), ("payload_hash", "CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL", "规范化请求的服务端HMAC摘要，不保存密码"), ("status", "VARCHAR(16) NOT NULL", "PROCESSING/COMPLETED"), ("response_json", "JSON NULL", "成功响应或资源引用，不含敏感字段"), ("expires_time", "DATETIME(3) NOT NULL", "默认24小时；到期返回KEY_EXPIRED，不重用键")], ["UNIQUE KEY uk_idem_scope (owner_id, operation, request_key)", "KEY idx_idem_expiry (expires_time)", "FOREIGN KEY (owner_id) REFERENCES sys_user(id)", "CHECK (status IN ('PROCESSING','COMPLETED'))"])

sql = ["-- NexusHub V1 建表草案；目标 MySQL 8.4，UTF8MB4，UTC。", "-- 仅在专用空库执行；不含DROP、不含默认管理员密码。", "SET NAMES utf8mb4;", "SET time_zone = '+00:00';", ""]
dictionary = ["# 表字段字典", "", "由 `doc/tooling/generate_contracts.py` 与 schema.sql 同源生成。字段类型、可空与默认值以下表为准，跨字段规则见 database-design.md。", "", "所有表使用 InnoDB、utf8mb4_0900_ai_ci；技术Key覆盖为ascii_bin。", ""]
for name, desc, columns, constraints in tables:
    definitions = []
    dictionary += [f"## {name}", "", desc, "", "| 字段 | SQL 类型与约束 | 说明 |", "|---|---|---|"]
    for col, dtype, comment in AUDIT + columns:
        definitions.append(f"  {col} {dtype} COMMENT '{comment.replace(chr(39), chr(39)*2)}'")
        dictionary.append(f"| {col} | `{dtype}` | {comment} |")
    definitions += ["  PRIMARY KEY (id)", f"  CONSTRAINT ck_{name}_deleted CHECK (deleted IN (0,1))"] + ["  " + c for c in constraints]
    sql += [f"CREATE TABLE {name} (", ",\n".join(definitions), f") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='{desc}';", ""]
    dictionary += ["", "索引与约束：", ""] + [f"- `{c}`" for c in constraints] + [""]
sql += ["-- 双向指针在两端建表后添加；复合外键保证指针属于同一题目/结果。", "ALTER TABLE puzzle ADD CONSTRAINT fk_puzzle_latest FOREIGN KEY (id, latest_version_id) REFERENCES puzzle_version(puzzle_id, id), ADD CONSTRAINT fk_puzzle_published FOREIGN KEY (id, published_version_id) REFERENCES puzzle_version(puzzle_id, id);", "ALTER TABLE solve_result ADD CONSTRAINT fk_result_root FOREIGN KEY (id, root_node_id) REFERENCES strategy_node(result_id, id);", "ALTER TABLE game_session ADD CONSTRAINT fk_game_proof FOREIGN KEY (proof_result_id) REFERENCES solve_result(id);", ""]
(DOC / "database").mkdir(exist_ok=True)
(DOC / "database/schema.sql").write_text("\n".join(sql), encoding="utf-8")
(DOC / "database/field-dictionary.md").write_text("\n".join(dictionary), encoding="utf-8")
print(f"Generated {len(paths)} paths, {len(S)} schemas, {len(tables)} tables")
