-- 允许严格最优机器人行动写入审计事件；历史 HUMAN/HEURISTIC/PROVEN 值保持不变。
ALTER TABLE game_action
    DROP CHECK game_action_chk_4,
    ADD CONSTRAINT game_action_chk_4
        CHECK (decision_quality IN ('OPTIMAL', 'PROVEN', 'HEURISTIC', 'HUMAN'));
