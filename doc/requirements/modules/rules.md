# 规则模块

| 编号 | 需求 | 验收 |
|---|---|---|
| RULE-01 | 双人牌型识别与规范化 | 按 [规则正文](../../domain/rules.md) 覆盖标准牌型；四带二固定排除 |
| RULE-02 | 生成全部合法行动 | 不因排序漏掉拆牌、炸弹或 PASS；首手不生成 PASS |
| RULE-03 | 执行唯一状态转移 | 出牌、连续不出、终局准确 |
| RULE-04 | 创建局面完整校验 | 同时检查两家牌量、同点数上限、待压牌和座位顺序 |
| RULE-05 | 无基础设施依赖 | 普通 JUnit 即可运行，不需要 Spring/MySQL |

纯 Java 对外能力：`validateInitialState`、`enumerateLegalMoves`、`applyMove`、`terminalOutcome`、`canonicalize`。首次领出由 `isFirstMove` 控制，首手不允许 PASS；之后 PASS 合法，清墩规则由最后有效出牌者重新领出。API 层只通过业务服务暴露，不提供可直接修改在线对局状态的任意执行接口。

同一实体牌组合存在多个合法解释时，枚举分别生成解释，再按 cards/type/mainRank/sequenceLength 去重。禁止 UI 猜测牌型后服务端盲信。
