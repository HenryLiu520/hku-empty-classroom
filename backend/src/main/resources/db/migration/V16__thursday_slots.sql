-- V16: the demo timetable had no Thursday.
-- Monday / Tuesday / Wednesday / Friday were seeded; Thursday was empty, so opening the
-- client on a Thursday showed every room free, which looks like a bug even though the
-- availability logic is correct. This adds a Thursday that mirrors Wednesday's shape
-- (same rooms, same blocks, no overlaps) with different course codes.

INSERT INTO class_slots (room_id, day_of_week, start_time, end_time, course_code)
SELECT room_id, 4, start_time, end_time, course_code
FROM class_slots
WHERE day_of_week = 3
ORDER BY room_id, start_time;

-- Make Thursday visibly its own day rather than a copy of Wednesday.
UPDATE class_slots SET course_code = 'MATH1013' WHERE day_of_week = 4 AND course_code = 'COMP1110';
UPDATE class_slots SET course_code = 'CAES1001' WHERE day_of_week = 4 AND course_code = 'COMP1117';
UPDATE class_slots SET course_code = 'CCST9042' WHERE day_of_week = 4 AND course_code = 'CCST9003';
