# CLASSIC_V1 双人残局规则与状态契约

## 1. 参与者、牌量与胜负

本版本只有两个独立参与者：`USER` 与 `BOT`，不设置地主或农民。双方手牌完全明牌，谁先出完手牌谁获胜。`firstSeat` 决定本残局的第一位行动者，也可在创建对局时按题目允许的配置选择用户先手或机器人先手；对局创建后不可切换。

先手初始手牌最多 20 张且不能为空，后手初始手牌最多 17 张且不能为空。两家合计不超过一副 54 张牌；普通点数最多 4 张，小王和大王各最多 1 张。残局可以只保存剩余牌，不要求补齐或还原完整发牌历史。

## 2. 点数与牌型

点数固定编号：`0=3,1=4,2=5,3=6,4=7,5=8,6=9,7=10,8=J,9=Q,10=K,11=A,12=2,13=小王,14=大王`。花色不参与规则，手牌是长度 15 的计数数组。

标准牌型保留：单张、对子、三张、三带一、三带二、顺子、连对、飞机不带、飞机带单、飞机带对、炸弹、王炸。四带二（四带两张、四带两对）不属于 CLASSIC_V1，系统固定不生成，也不存在牌型禁用配置。

牌型约束：顺子/连对/飞机主体不含 2 和王；普通牌型要求类型、主体长度和总张数匹配后比较主体最高点数；炸弹可压普通牌型，炸弹之间比点数；王炸最大。飞机翼牌沿用确定的标准约束：翼牌不含主体，飞机带对的对子点数互异，禁止将四张同点数拆成两对翼牌；双王不能同时作为飞机带单翼牌。具体牌型解释由规则引擎规范化，不接受前端自行解释。

## 3. 行动与 PASS

`Move` 包含牌型、实际取走的 15 位计数、主体最高点数和连续主体长度。`PASS` 的 cards 全 0。先手第一次行动必须为有效出牌，不能 PASS；从第一手有效出牌开始，双方在接牌时都可以自由选择 PASS。

自由领出时不生成 PASS。接牌时必须出相同类型/长度且主体更大的牌，或出炸弹/王炸，也可以 PASS。某玩家 PASS 后，另一玩家仍然可以继续接牌；当连续 PASS 使当前牌墩回到最后有效出牌者时，清空 targetMove，由最后有效出牌者重新领出。由于只有两人，连续 PASS 的上限是 1 次后立即换回领出者，不建立三人计数。

玩家出完手牌立即终局，忽略尚未处理的机器人任务。终局后的出牌、PASS、提示和恢复都拒绝。

## 4. GameState JSON

```json
{
  "schemaVersion": 1,
  "rulesetVersion": "CLASSIC_V1",
  "hands": [[0,0,0,0,0,0,0,0,0,0,0,0,1,0,0], [1,0,0,0,0,0,0,0,0,0,0,0,0,0,0]],
  "firstSeat": 0,
  "currentSeat": 0,
  "isFirstMove": true,
  "targetMove": null,
  "lastPlaySeat": null,
  "consecutivePasses": 0
}
```

`hands[0]` 与 `hands[1]` 分别代表 USER/BOT 的规范化顺序，不随先后手改变。对局详情额外返回 `winnerSide=USER|BOT`，该字段不进入求解 Key。`targetMove=null` 表示自由领出；非空表示接牌。`isFirstMove` 只在首个有效出牌前为 true。

## 5. 初始局面校验

- 两手牌都非空；firstSeat 对应的手牌不超过 20 张，另一手不超过 17 张。
- 每个计数非负；3～2 每个点数≤4，小王/大王≤1；双方合计≤54。
- CLASSIC_V1 固定排除四带二，其他牌型按标准规则可用；至少存在一种可领出的牌型。
- 自由领出：targetMove=null、lastPlaySeat=null、consecutivePasses=0、isFirstMove=true、currentSeat=firstSeat。
- 接牌：targetMove 是有效非 PASS，lastPlaySeat 是上一位有效出牌者，consecutivePasses 只能 0/1，isFirstMove=false，currentSeat 是另一玩家。
- 接牌局面只验证局部一致性，未提供完整历史时不声称历史可达。

## 6. 状态转移

有效出牌：从当前玩家手牌减 cards；若为空立即得到该玩家胜利；否则 targetMove=本手、lastPlaySeat=当前玩家、consecutivePasses=0、isFirstMove=false、currentSeat 切换到另一玩家。

PASS：仅当 `isFirstMove=false` 且 targetMove 非空时允许。设置 consecutivePasses=1，currentSeat 切换；下一位是最后有效出牌者时清空 targetMove/lastPlaySeat、consecutivePasses=0，并让最后有效出牌者重新领出。PASS 不减少手牌。

`applyMove` 返回新的不可变状态。每个行动由 API 以 expectedStateVersion 和幂等 Key 提交，后端再次执行全部校验。

## 7. 严格必胜与路线

这是标准双人零和完全信息博弈。当前行动者在自己的回合最大化自身胜利，另一方选择其最有利于自己（也就是阻止对手）的回应。终局值以 USER/BOT 为视角记录，未知不当作平局。

“必胜”必须是多分支证明：获胜方节点至少存在一条已验证获胜边；对手节点必须覆盖其全部合法应对，不能只展示一条主变化。证明 DAG 可以保存所有分支，但策略接口最多返回 5 条稳定排序的已验证获胜路线供用户阅读；这 5 条路线是展示上限，不是证明覆盖上限。没有完成全分支覆盖时返回 UNKNOWN。

搜索超时、节点/内存预算耗尽或取消返回 UNKNOWN；winnerSide 为空。只有独立 verifier 验证完整 DAG 后才将 strategyStatus 设为 COMPLETE。启发式建议可以展示，但必须标记为 HEURISTIC，不得称为必胜或最优。

规范化求解 Key 包括两手牌、firstSeat、currentSeat、isFirstMove、targetMove、lastPlaySeat、consecutivePasses、规则/求解器版本。缓存命中后仍核对完整规范化状态。
