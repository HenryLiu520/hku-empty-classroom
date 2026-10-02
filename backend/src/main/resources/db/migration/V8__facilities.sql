-- V8: 房间设施（第一批三条）—— 座位总数 / 有没有插座 / 座位类型
-- 座位总数沿用 rooms.capacity；新增 sockets 与 seat_type。
-- facilities_verified_at 为空 = **占位样例数据，未经核实**；管理员在界面上编辑保存时会盖上核实时间。
-- 这样"先填点内容让功能能演示"和"不把编造的数据冒充事实"两件事可以同时成立。

ALTER TABLE rooms ADD COLUMN sockets               BOOLEAN;
ALTER TABLE rooms ADD COLUMN seat_type             VARCHAR(60);
ALTER TABLE rooms ADD COLUMN facilities_verified_at TIMESTAMP;

-- 占位样例值（按房间类型给一个合理的形状，不是核实过的事实）
UPDATE rooms SET seat_type = 'Fixed rows',       sockets = TRUE  WHERE room_type = 'Lecture'  AND code IN ('CPD-LG.01','KB-110');
UPDATE rooms SET seat_type = 'Fixed rows',       sockets = FALSE WHERE room_type = 'Lecture'  AND code IN ('CPD-4.01','MB-121');
UPDATE rooms SET seat_type = 'Fixed rows',       sockets = TRUE  WHERE room_type = 'Lecture'  AND code = 'KK-101';
UPDATE rooms SET seat_type = 'Long shared table', sockets = TRUE  WHERE room_type = 'Seminar';
UPDATE rooms SET seat_type = 'Individual desks', sockets = TRUE  WHERE room_type = 'Tutorial' AND code IN ('CPD-3.12','KK-202');
UPDATE rooms SET seat_type = 'Individual desks', sockets = FALSE WHERE room_type = 'Tutorial' AND code = 'CPD-1.09';

INSERT INTO audit_log (action, entity, entity_id, actor, detail)
VALUES ('FACILITIES_FIELDS', 'rooms', NULL, 'system',
        'Three facility fields added: total seats (capacity), sockets (yes/no), seat type. Values are sample placeholders until verified');
