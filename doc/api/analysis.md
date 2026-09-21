# analysis API

分析创建接口立即返回 `SolveTask`，不在 HTTP 请求线程执行搜索。任务状态、证明状态和策略完整度分开返回；`UNKNOWN` 时 winnerSide/proofValue 必须为空，`principalVariation` 只是阅读用代表路线。

当前 V1 已接入 `MINIMAX_V1` 后台 Worker：应用启动时会为所有已发布残局版本补建 `ANALYSIS` 任务，重复启动不会重复排队；Worker 固定最多并发 5 个证明任务，超出部分保持 `QUEUED`。任务执行使用时间/节点预算，完整覆盖对手分支后写入 `PROVEN`，预算耗尽写入 `UNKNOWN`。

策略子分支按 resultId/nodeId 懒加载。只有独立 verifier 确认获胜方节点和失败方所有合法应对覆盖后，strategyStatus 才能为 COMPLETE。取消、租约和重试使用 token/CAS，旧 Worker 不能覆盖终态。

## 对局同步提示（V1）

`POST /api/games/hint` 接收 `GameVersionRequest`，必须登录、通过 CSRF 校验、携带 `Idempotency-Key`，并且只能访问自己的对局和当前用户回合。服务端按 `expectedStateVersion` 做 CAS 校验后读取完整 `GameState`，复用 `GameRules.legalMoves` 和 `BotStrategy.chooseHint` 选出一条确定性的规则合法建议。

V1 在 HTTP 请求内同步返回 `HintResponse`：

```json
{
  "move": {"type":"SINGLE","cards":[1,0,0,0,0,0,0,0,0,0,0,0,0,0,0],"mainRank":0,"sequenceLength":0},
  "proofStatus":"UNKNOWN",
  "winnerSide":null,
  "hintCount":1,
  "stateVersion":0
}
```

提示不会落子，不会新增 `game_action`，也不会修改 `current_state` 或 `state_version`；只在 `game_session.hint_count` 上执行带版本条件的原子递增。因此提示不能被解释为必胜证明，`proofStatus` 固定为 `UNKNOWN`，`winnerSide` 固定为空。未来可将同一请求模型扩展为创建 `solve_task(purpose=HINT)` 的异步任务，但必须继续绑定不可变状态快照和状态版本。
