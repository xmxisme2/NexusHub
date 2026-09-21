-- NexusHub 本地测试数据；仅用于本地开发，不得用于生产。
SET NAMES utf8mb4;
CREATE DATABASE IF NOT EXISTS nexushub CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE nexushub;

-- 测试管理员使用 BCrypt 密码哈希；生产环境必须通过初始化命令生成随机密码。
INSERT INTO sys_user (id, create_time, update_time, create_by, update_by, deleted, username, display_name, password_hash, role, status, row_version, credential_version)
VALUES (1, CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3), NULL, NULL, 0, 'admin', '系统管理员', '$2b$12$BqJWGdQ.RGXRTxfhZATjN..28uYkYi7a6Ts4C07MKRwu8Y5oYxpwK', 'ADMIN', 'ENABLED', 0, 0)
ON DUPLICATE KEY UPDATE display_name=VALUES(display_name), password_hash=VALUES(password_hash), role='ADMIN', status='ENABLED', deleted=0;

-- 游客账号：无密码登录，仅用于临时题库浏览和对局归属，不授予任何写权限。
INSERT INTO sys_user (id, create_time, update_time, create_by, update_by, deleted, username, display_name, password_hash, role, status, row_version, credential_version)
VALUES (2, CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3), NULL, NULL, 0, 'guest', '游客', '', 'GUEST', 'ENABLED', 0, 0)
ON DUPLICATE KEY UPDATE display_name=VALUES(display_name), role='GUEST', status='ENABLED', deleted=0;

INSERT INTO hub_system (id, create_time, update_time, create_by, update_by, deleted, code, name, description, icon_key, entry_type, route, external_url, visible_roles, sort_order, enabled, row_version)
VALUES (1001, CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3), 1, 1, 0, 'landlord-puzzle', '斗地主残局实验室', '双人明牌残局、严格求解与复盘。', 'cards', 'INTERNAL', '/landlord/puzzles', NULL, JSON_ARRAY('GUEST','USER','ADMIN'), 10, 1, 0)
ON DUPLICATE KEY UPDATE name=VALUES(name), description=VALUES(description), route=VALUES(route), enabled=1, deleted=0;

-- L001：USER 先手，各一张牌；USER 出 3 后立即获胜。用于启动最小闭环。
INSERT INTO puzzle (id, create_time, update_time, create_by, update_by, deleted, owner_id, status, latest_version_id, published_version_id, row_version)
VALUES (2001, CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3), 1, 1, 0, 1, 'DRAFT', NULL, NULL, 0)
ON DUPLICATE KEY UPDATE owner_id=1, status='DRAFT', latest_version_id=NULL, published_version_id=NULL, deleted=0;

INSERT INTO puzzle_version (id, create_time, update_time, create_by, update_by, deleted, puzzle_id, version_no, title, description, tags, difficulty, allowed_first_seats, ruleset_version, schema_version, initial_state, state_hash)
VALUES (2101, CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3), 1, 1, 0, 2001, 1, 'L001 · 单张先手', '最小双人残局基准：USER 先手出单张即可结束。', JSON_ARRAY('基准','单张','USER先手'), 'EASY', JSON_ARRAY(0), 'CLASSIC_V1', 1,
JSON_OBJECT('schemaVersion',1,'rulesetVersion','CLASSIC_V1','hands',JSON_ARRAY(JSON_ARRAY(1,0,0,0,0,0,0,0,0,0,0,0,0,0,0),JSON_ARRAY(0,1,0,0,0,0,0,0,0,0,0,0,0,0,0)),'firstSeat',0,'currentSeat',0,'isFirstMove',true,'targetMove',NULL,'lastPlaySeat',NULL,'consecutivePasses',0),
'085b218509ef73ec04137dddc09e337905e35eff40a94743a73b17e275ade15d')
ON DUPLICATE KEY UPDATE title=VALUES(title), description=VALUES(description), tags=VALUES(tags), difficulty=VALUES(difficulty), allowed_first_seats=VALUES(allowed_first_seats), initial_state=VALUES(initial_state), state_hash=VALUES(state_hash), deleted=0;

UPDATE puzzle SET latest_version_id=2101, published_version_id=2101, status='PUBLISHED', row_version=0 WHERE id=2001;

-- 追加可复核的小残局样例。

INSERT INTO puzzle (id, create_time, update_time, create_by, update_by, deleted, owner_id, status, latest_version_id, published_version_id, row_version) VALUES (2002, CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3), 1, 1, 0, 1, 'DRAFT', NULL, NULL, 0) ON DUPLICATE KEY UPDATE deleted=0;
INSERT INTO puzzle_version (id, create_time, update_time, create_by, update_by, deleted, puzzle_id, version_no, title, description, tags, difficulty, allowed_first_seats, ruleset_version, schema_version, initial_state, state_hash) VALUES (2102, CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3), 1, 1, 0, 2002, 1, 'L002 · 单张接牌', 'USER先手出3，BOT只能出更大的单张；适合验证接牌分支。', '["接牌","单张"]', 'EASY', '[0]', 'CLASSIC_V1', 1, '{"schemaVersion":1,"rulesetVersion":"CLASSIC_V1","hands":[[0,1,0,0,0,0,0,0,0,0,0,0,0,0,0],[0,0,1,0,0,0,0,0,0,0,0,0,0,0,0]],"firstSeat":0,"currentSeat":0,"isFirstMove":true,"targetMove":null,"lastPlaySeat":null,"consecutivePasses":0}', '523bf7bbe03d0c943a3ac99cc636a5f7c88ba50243eab3271ee61619eb93ea38') ON DUPLICATE KEY UPDATE title=VALUES(title), description=VALUES(description), tags=VALUES(tags), difficulty=VALUES(difficulty), allowed_first_seats=VALUES(allowed_first_seats), initial_state=VALUES(initial_state), state_hash=VALUES(state_hash), deleted=0;
UPDATE puzzle SET latest_version_id=2102, published_version_id=2102, status='PUBLISHED', row_version=0 WHERE id=2002;
INSERT INTO puzzle (id, create_time, update_time, create_by, update_by, deleted, owner_id, status, latest_version_id, published_version_id, row_version) VALUES (2003, CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3), 1, 1, 0, 1, 'DRAFT', NULL, NULL, 0) ON DUPLICATE KEY UPDATE deleted=0;
INSERT INTO puzzle_version (id, create_time, update_time, create_by, update_by, deleted, puzzle_id, version_no, title, description, tags, difficulty, allowed_first_seats, ruleset_version, schema_version, initial_state, state_hash) VALUES (2103, CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3), 1, 1, 0, 2003, 1, 'L003 · 对子抢先', 'BOT先手，对子结构用于验证机器人先手和对子比较。', '["BOT先手","对子"]', 'MEDIUM', '[1]', 'CLASSIC_V1', 1, '{"schemaVersion":1,"rulesetVersion":"CLASSIC_V1","hands":[[0,1,1,0,0,0,0,0,0,0,0,0,0,0,0],[0,0,0,1,1,0,0,0,0,0,0,0,0,0,0]],"firstSeat":1,"currentSeat":1,"isFirstMove":true,"targetMove":null,"lastPlaySeat":null,"consecutivePasses":0}', '42c5b4d577aceb50b84d3f952c13c93c8312114878d3f428470e22c8d3875e73') ON DUPLICATE KEY UPDATE title=VALUES(title), description=VALUES(description), tags=VALUES(tags), difficulty=VALUES(difficulty), allowed_first_seats=VALUES(allowed_first_seats), initial_state=VALUES(initial_state), state_hash=VALUES(state_hash), deleted=0;
UPDATE puzzle SET latest_version_id=2103, published_version_id=2103, status='PUBLISHED', row_version=0 WHERE id=2003;
INSERT INTO puzzle (id, create_time, update_time, create_by, update_by, deleted, owner_id, status, latest_version_id, published_version_id, row_version) VALUES (2004, CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3), 1, 1, 0, 1, 'DRAFT', NULL, NULL, 0) ON DUPLICATE KEY UPDATE deleted=0;
INSERT INTO puzzle_version (id, create_time, update_time, create_by, update_by, deleted, puzzle_id, version_no, title, description, tags, difficulty, allowed_first_seats, ruleset_version, schema_version, initial_state, state_hash) VALUES (2104, CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3), 1, 1, 0, 2004, 1, 'L004 · 三带一', 'USER先手，三带一残局用于验证组合牌型入口。', '["三带一","组合"]', 'MEDIUM', '[0]', 'CLASSIC_V1', 1, '{"schemaVersion":1,"rulesetVersion":"CLASSIC_V1","hands":[[0,0,0,3,0,0,0,0,0,0,0,0,0,0,0],[0,0,0,1,1,0,0,0,0,0,0,0,0,0,0]],"firstSeat":0,"currentSeat":0,"isFirstMove":true,"targetMove":null,"lastPlaySeat":null,"consecutivePasses":0}', '5ad6037d34f1b431e939cb1b1f96ad93b59b3769cd2a0aaf7eef5263a8a6c977') ON DUPLICATE KEY UPDATE title=VALUES(title), description=VALUES(description), tags=VALUES(tags), difficulty=VALUES(difficulty), allowed_first_seats=VALUES(allowed_first_seats), initial_state=VALUES(initial_state), state_hash=VALUES(state_hash), deleted=0;
UPDATE puzzle SET latest_version_id=2104, published_version_id=2104, status='PUBLISHED', row_version=0 WHERE id=2004;

-- L005：通过配置页面入库的最小样例，USER 先手单张立即获胜。
INSERT INTO puzzle (id, create_time, update_time, create_by, update_by, deleted, owner_id, status, latest_version_id, published_version_id, row_version)
VALUES (2005, CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3), 1, 1, 0, 1, 'DRAFT', NULL, NULL, 0)
ON DUPLICATE KEY UPDATE owner_id=1, status='DRAFT', latest_version_id=NULL, published_version_id=NULL, deleted=0;
INSERT INTO puzzle_version (id, create_time, update_time, create_by, update_by, deleted, puzzle_id, version_no, title, description, tags, difficulty, allowed_first_seats, ruleset_version, schema_version, initial_state, state_hash)
VALUES (2105, CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3), 1, 1, 0, 2005, 1, 'L005 · 配置示例', '通过残局配置页面入库的最小验证样例。', '["配置","单张"]', 'EASY', '[0]', 'CLASSIC_V1', 1, '{"schemaVersion":1,"rulesetVersion":"CLASSIC_V1","hands":[[1,0,0,0,0,0,0,0,0,0,0,0,0,0,0],[0,1,0,0,0,0,0,0,0,0,0,0,0,0,0]],"firstSeat":0,"currentSeat":0,"isFirstMove":true,"targetMove":null,"lastPlaySeat":null,"consecutivePasses":0}', '11276c1a976f1b926a46682de585fd0f2008ac9143a418f3435487bfac44628c')
ON DUPLICATE KEY UPDATE title=VALUES(title), description=VALUES(description), tags=VALUES(tags), difficulty=VALUES(difficulty), allowed_first_seats=VALUES(allowed_first_seats), initial_state=VALUES(initial_state), state_hash=VALUES(state_hash), deleted=0;
UPDATE puzzle SET latest_version_id=2105, published_version_id=2105, status='PUBLISHED', row_version=0 WHERE id=2005;
