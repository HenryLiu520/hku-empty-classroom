-- V2: 种子数据（演示用，非真实课表）
-- Monday 的排布刻意与 UI 稿一致：CPD-LG.01 在 14:00–16:20 空闲，CPD-1.09 被征用，MB-121 关闭。

INSERT INTO rooms (code, building, floor, capacity, room_type) VALUES
  ('CPD-LG.01', 'CPD', 'LG1', 120, 'Lecture'),
  ('CPD-1.09',  'CPD', '1F',   40, 'Tutorial'),
  ('CPD-2.45',  'CPD', '2F',   60, 'Seminar'),
  ('CPD-3.12',  'CPD', '3F',   30, 'Tutorial'),
  ('CPD-4.01',  'CPD', '4F',  100, 'Lecture'),
  ('MB-121',    'MB',  '1F',   80, 'Lecture'),
  ('MB-201',    'MB',  '2F',   50, 'Seminar'),
  ('KK-101',    'KK',  '1F',   60, 'Lecture'),
  ('KK-202',    'KK',  '2F',   45, 'Tutorial'),
  ('KB-110',    'KB',  '1F',   80, 'Lecture');

-- Monday：与 UI 稿对齐的显式排布
INSERT INTO class_slots (room_id, day_of_week, start_time, end_time, course_code)
SELECT r.id, 1, t.s, t.e, t.cc FROM rooms r JOIN (VALUES
  ('CPD-LG.01', TIME '08:30', TIME '09:20', 'COMP1110'),
  ('CPD-LG.01', TIME '09:30', TIME '11:20', 'COMP1117'),
  ('CPD-LG.01', TIME '11:30', TIME '13:50', 'MATH1013'),
  ('CPD-LG.01', TIME '16:30', TIME '18:20', 'CAES1001'),
  ('CPD-2.45',  TIME '08:30', TIME '10:20', 'CCST9003'),
  ('CPD-2.45',  TIME '10:30', TIME '12:20', 'CCST9042'),
  ('CPD-2.45',  TIME '16:00', TIME '17:50', 'MATH1013'),
  ('CPD-3.12',  TIME '08:30', TIME '10:20', 'COMP1117'),
  ('CPD-3.12',  TIME '10:30', TIME '12:20', 'COMP1110'),
  ('CPD-3.12',  TIME '13:00', TIME '15:20', 'MATH1013'),
  ('CPD-1.09',  TIME '10:00', TIME '11:50', 'CAES1001'),
  ('CPD-4.01',  TIME '09:30', TIME '11:20', 'COMP1110'),
  ('CPD-4.01',  TIME '13:00', TIME '14:50', 'COMP1117'),
  ('MB-121',    TIME '08:30', TIME '10:20', 'COMP1110'),
  ('MB-201',    TIME '13:00', TIME '14:50', 'CCST9003'),
  ('KB-110',    TIME '10:00', TIME '11:50', 'MATH1013')
) AS t(code, s, e, cc) ON t.code = r.code;

-- Tuesday, Wednesday: 每个房间三节（用 id/dow 取模跳过一部分，制造空闲差异）
INSERT INTO class_slots (room_id, day_of_week, start_time, end_time, course_code)
SELECT r.id, d.dow, t.s, t.e, t.cc
FROM rooms r
CROSS JOIN (VALUES (2), (3)) AS d(dow)
CROSS JOIN (VALUES
  (TIME '09:00', TIME '10:50', 'COMP1110'),
  (TIME '11:00', TIME '12:50', 'COMP1117'),
  (TIME '14:30', TIME '16:20', 'MATH1013')
) AS t(s, e, cc)
WHERE (r.id + d.dow) % 3 <> 0;

-- Thursday, Friday：留出下午 12:30–16:20 的大段空闲，方便演示"14:00 起两小时"
INSERT INTO class_slots (room_id, day_of_week, start_time, end_time, course_code)
SELECT r.id, d.dow, t.s, t.e, t.cc
FROM rooms r
CROSS JOIN (VALUES (4), (5)) AS d(dow)
CROSS JOIN (VALUES
  (TIME '08:30', TIME '10:20', 'CCST9042'),
  (TIME '10:30', TIME '12:20', 'CAES1001'),
  (TIME '16:30', TIME '18:20', 'COMP1110')
) AS t(s, e, cc)
WHERE (r.id * d.dow) % 4 <> 0;

-- 演示用的临时变更：以"今天"为基准，保证任何一天演示都能看到被征用/关闭的效果
INSERT INTO room_updates (room_id, change_type, slot_date, start_time, end_time, reason, created_by, expires_at, active)
SELECT r.id, t.ct, CURRENT_DATE + t.dayoff, t.s, t.e, t.rs, 'teacher1',
       (CURRENT_DATE + t.dayoff + t.e)::timestamp + INTERVAL '1 day', TRUE
FROM rooms r JOIN (VALUES
  ('CPD-1.09', 'REQUISITION',     0, TIME '14:00', TIME '16:00', 'Department meeting, about 25 people'),
  ('CPD-2.45', 'CANCELLED_CLASS', 0, TIME '09:30', TIME '11:20', 'Make up class moved'),
  ('MB-121',   'CLOSURE',         0, TIME '08:00', TIME '22:00', 'AV repair'),
  ('CPD-4.01', 'REQUISITION',     1, TIME '13:00', TIME '15:00', 'Staff briefing')
) AS t(code, ct, dayoff, s, e, rs) ON t.code = r.code;

-- 账号（原型级：SHA-256(salt:password)）
INSERT INTO app_user (username, display_name, role, salt, password_hash) VALUES
  ('student1', 'Haoran Liu',     'STUDENT', 's1f3a9b2', '145bd7bcabd087bbb46c0c24d045a792cd6f42849a752cd0d8617136dc1d1aab'),
  ('teacher1', 'Dr. C. M. Liu',  'TEACHER', 't7c4d1e8', 'ecf6423639759ccaa2da0341507e17ab2fa43816dffe4d9ebfe91b6a6fdb9e25');

INSERT INTO audit_log (action, entity, entity_id, actor, detail)
VALUES ('SEED', 'SYSTEM', NULL, 'system', 'Initial rooms, timetable and demo updates loaded');
