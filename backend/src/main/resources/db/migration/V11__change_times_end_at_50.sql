-- V11: 管理员发布的变更也按课表口径 —— 起点整点、终点 :50。
--
-- 一节课是整点开始、:50 结束（50 / 110 / 170 分钟 = 1 / 2 / 3 个整块），
-- 管理员的变更同样以整块为单位，所以终点也写 :50：
--   14:00 → 15:50 是两块（110 分钟），而不是 14:00 → 16:00。
-- 这里把已有变更的终点统一往前 10 分钟。

UPDATE room_updates
SET end_time = end_time - interval '10 minutes'
WHERE EXTRACT(MINUTE FROM end_time) = 0;

INSERT INTO audit_log (action, entity, entity_id, actor, detail)
VALUES ('SCHEMA', 'room_updates', NULL, 'system',
        'Staff changes now follow the timetable convention too: start on the hour, end at :50 (whole hour blocks)');
