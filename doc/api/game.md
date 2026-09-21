# game API

创建对局绑定题目版本快照；出牌和不出分别调用 action API，带 expectedStateVersion 与幂等 Key。后端重新生成并校验合法行动，前端不能提交机器人行动或直接设置状态。

机器人作业持久化并异步执行；陈旧 stateVersion、已终局或已放弃的结果丢弃。`TRAINING` 使用确定性启发式合法行动并标记 `HEURISTIC`；`OPTIMAL` 在每个机器人回合实时执行带预算的 Minimax，只有 `PROVEN` 才会落子，预算不足时返回 `NEEDS_PROOF`，不会退回启发式。定时证明任务用于残局的独立证明和结果展示，不是 OPTIMAL 对局的前置等待条件。

## 重开对局

`POST /api/games/restart` 在当前对局内恢复创建时保存的 `initialState`，清理该局已有行动事件（逻辑删除），将状态版本递增并重置为 `ACTIVE`。请求必须携带当前 `expectedStateVersion` 和 `Idempotency-Key`；版本不一致返回 409。若初始座位为机器人，重开后重新进入 `QUEUED`，避免旧机器人任务覆盖新局面。重开保留原对局模式，`OPTIMAL` 重开后继续逐回合实时求解。

## 同步提示

`POST /api/games/hint` 要求登录、CSRF、所有者权限和当前用户回合，并携带 `expectedStateVersion` 与 `Idempotency-Key`。V1 同步返回 `HintResponse`，建议行动由规则引擎枚举后确定性选择，`proofStatus` 固定为 `UNKNOWN`、`winnerSide` 固定为空。提示只递增 `hintCount`，不会创建出牌事件或修改局面版本。当前 V1 对 Key 做请求头格式校验和状态版本 CAS；幂等记录持久化以及相同 Key 重放原响应仍待补齐，调用方应在 409 时重新读取对局后再发起新请求。

## 弃局

`POST /api/games/abandon` 要求登录、CSRF、所有者权限、当前对局为 `ACTIVE`，请求体为 `GameVersionRequest`，并携带 `Idempotency-Key`。服务端使用 `expectedStateVersion` 做 CAS：成功后将对局标记为 `ABANDONED`，递增 `stateVersion`，清空 `winnerSide`，将 `robotStatus` 置为 `IDLE` 并记录终态时间。已有 `game_action` 事件保留，弃局本身不伪造出牌行动。重复或过期版本返回冲突，已结束或已弃局对局不能再次弃局。

## 复盘

`POST /api/games/replay/detail` 要求登录、CSRF 和对局所有者权限，返回创建时的 `initialState`、当前 `game` 快照以及按 `sequenceNo` 升序分页的 `game_action` 事件。事件中的 `afterState` 是写入行动事务时保存的权威快照，前端只负责展示，不重新推断历史；重开后旧事件已逻辑删除，因此复盘展示的是当前对局轮次。
# 交换先后手

创建对局时可选 `swapSeats=true`。服务端以残局原始配置校验先手权限后，交换两方初始手牌并将先手座位取反，再保存为本局不可变快照。这样原残局证明为 USER 必胜时，可让 BOT 获得原 USER 手牌并执行该必胜路线；不会修改残局版本、证明结果或公开题库。
