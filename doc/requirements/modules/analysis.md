# 求解任务与策略模块

| 编号 | 需求 | 验收 |
|---|---|---|
| ANA-01 | 从题目版本/对局版本/自定义局面创建任务 | 权限通过后复制不可变快照，不在任务中读可变现状 |
| ANA-02 | 持久排队、领取、心跳、有限重试 | 服务重启不无声丢任务，旧 Worker 不能覆盖新结果 |
| ANA-03 | 双人 Minimax + 安全剪枝 | 小状态与穷举一致，双方回合按自身胜负目标取值 |
| ANA-04 | 严格区分已证明和未知 | UNKNOWN 的 winnerSide、proofValue 均为空 |
| ANA-05 | 策略 DAG 与必胜路线 | 完整策略需覆盖对方所有应对；残局开始对局前可读取最多 5 条已验证路线并以多分支方式展示，单路线不是证明 |
| ANA-06 | 取消与预算 | 终态 CAS 提交，取消后旧线程不能改成 COMPLETED |
| ANA-07 | 结果/候选行动/分支分页 | 任务所有权检查后才可访问节点与结果 |

搜索以 USER 视角收益 +1/-1 表示。USER 节点 MAX，BOT 节点 MIN；时间/节点/内存停止返回 UNKNOWN，但已建立的严格证明可保留。任务达到预算可正常 COMPLETED，terminationReason 标明预算原因。

节点值、候选行动值和根值分别有证明状态。Alpha-Beta 界值不能作为精确值写入证明 DAG；完整材料由独立 verifier 校验后设置 COMPLETE。PARTIAL 表示部分说明材料可读，不表示根结论必然未知。证明 DAG 可以包含超过 5 条路线，但对外策略接口最多返回 5 条稳定排序的已验证路线，不能因此降低“覆盖所有应对”的证明要求。

任务创建允许 timeLimitMs 100～60000、nodeLimit 1000～5000000、memoryLimitMb 16～128，服务端下发 effectiveBudget 并按 profile 进一步限制。排队等待不计入计算预算，另记录 queueTimeMs。

任务结果保留所用规则与引擎版本，缓存 Key 包含完整规范化状态和目标函数。首版目标仅阵营获胜，不承诺最快赢/最长抵抗；推荐行动排序稳定以便复现。

取消 RUNNING 任务即时持久化 CANCELLED，撤销租约；线程协作停止。第一次终态 CAS 获胜：若已经完成，再次取消返回现有终态，不能抹除结果。Worker 只在 taskStatus=RUNNING 且 leaseToken 匹配时提交。

数据：solve_task、solve_result、strategy_node、strategy_edge；API：[analysis](../../api/analysis.md)。

V1 初始部署会自动扫描已发布 `puzzle_version` 并创建证明任务；证明 Worker 的并发上限为 5，任务状态和结果持久化到 MySQL。当前证明结果写入根结论与最多 5 条路线，策略 DAG 持久化仍是后续增强项，因此 `strategyStatus` 可能为 `PARTIAL`，不能将其误解为未完成根证明。
