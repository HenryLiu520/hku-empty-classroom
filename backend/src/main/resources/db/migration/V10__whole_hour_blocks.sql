-- V10: 占用以「整块」为单位 —— 所有课整点开始、:50 结束。
--
-- 规则（Plan §3.1）：
--   * 一节课 = 一个或多个整点块：50 分钟 = 1 块、110 分钟 = 2 块、170 分钟 = 3 块；
--   * 因此任何占用都落在整点上，绝不会出现"占半块"；
--   * 管理员发布的变更同样以整块为单位（整点 → 整点）。
--
-- 种子里少量排课是按 08:30-10:20 / 09:30-11:20 / 14:30-16:20 这种"半点起"写的，
-- 这里全部挪到整点；长度按 50 + 60k 分钟就近取整（140 → 110，即 2 块）。

UPDATE class_slots
SET start_time = date_trunc('hour', start_time)::time,
    end_time   = (date_trunc('hour', start_time)
                  + make_interval(mins => 50 + 60 * GREATEST(0,
                        FLOOR((EXTRACT(EPOCH FROM (end_time - start_time)) / 60 - 50) / 60))::int))::time
WHERE EXTRACT(MINUTE FROM start_time) <> 0
   OR EXTRACT(MINUTE FROM end_time) <> 50;

-- 管理员的变更也必须是整块（整点 → 整点，长度是整小时）
UPDATE room_updates
SET start_time = date_trunc('hour', start_time)::time,
    end_time   = (date_trunc('hour', start_time)
                  + make_interval(hours => GREATEST(1,
                        ROUND(EXTRACT(EPOCH FROM (end_time - start_time)) / 3600)::int)))::time
WHERE EXTRACT(MINUTE FROM start_time) <> 0
   OR EXTRACT(MINUTE FROM end_time) <> 0;

-- 去掉完全重复的变更行（同房间/同类型/同日期/同时间/同提交人，演示列表更干净）
DELETE FROM room_updates a
USING room_updates b
WHERE a.id > b.id
  AND a.room_id = b.room_id
  AND a.change_type = b.change_type
  AND a.slot_date = b.slot_date
  AND a.start_time = b.start_time
  AND a.end_time = b.end_time
  AND a.created_by = b.created_by
  AND COALESCE(a.reason, '') = COALESCE(b.reason, '');

INSERT INTO audit_log (action, entity, entity_id, actor, detail)
VALUES ('SCHEMA', 'class_slots', NULL, 'system',
        'All occupancy now falls on whole hour blocks: classes start on the hour and end at :50 (50/110/170 minutes = 1/2/3 blocks); staff changes are whole blocks too');
