# puzzle API

所有 Puzzle 响应新增 `proofStatus`（PROVEN/UNKNOWN）与 `winnerSide`（USER/BOT/null）。按响应 `version.id`、规则版本、当前求解器版本、状态哈希和完整局面匹配已完成的证明任务；只读取未删除的 PROVEN 结果。不受后续 UNKNOWN 任务覆盖，无匹配证明时返回 UNKNOWN/null。`POST /api/puzzles/proof-strategy` 允许游客读取已发布版本的严格 PROVEN 结果及最多 5 条已验证代表路线，前端可将这些路线可视化为多分支策略；UNKNOWN 或未完成任务不返回必胜策略。

题库列表支持 `PUBLISHED`、`MINE`、`MANAGE` scope；草稿和历史版本执行对象级权限。`POST /api/puzzles/validate` 返回 `valid=false` 时仍是成功响应，表示领域校验错误；请求结构错误使用 400。

保存每次产生不可变 `puzzle_version`。保存前调用 `POST /api/puzzles/duplicate-check`，按完整 `initial_state` 做结构化匹配，检查范围包含 `deleted=1` 的历史残局；编辑现有题目时通过 `excludePuzzleId` 排除自身。保存接口仍在服务端再次检查重复，避免并发绕过。

`POST /api/puzzles/archive`（兼容别名 `/delete`）仅做逻辑删除：更新 `puzzle.deleted=1`、状态为 `ARCHIVED`，不删除 `puzzle_version`、对局快照或证明任务。管理员可通过 `POST /api/puzzles/restore` 恢复为草稿；恢复会清空公开指针，不会重新生成历史版本。

重复检查命中已删除残局时，响应 `existing` 返回原题目标题、题目 ID、版本 ID、状态和 `stateHash`。前端提示“已有相同的已删除残局”，并指向原题目；调用方应复用该版本已有的路径证明，不创建新题目或重复入队。
