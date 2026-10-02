-- 房间并没有固定的"用途/类型"（Lecture / Tutorial / Seminar 是某门课怎么用它，
-- 不是房间本身的属性），学校那边也提供不了这一列。删掉。
ALTER TABLE rooms DROP COLUMN room_type;
