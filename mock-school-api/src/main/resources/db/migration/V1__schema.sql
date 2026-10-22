-- V1: 模拟学校系统的数据模型
-- 三张表：教室、课时（周课表）、feed 元信息（版本与生成时间）。
-- 设计目标是「像真接口那样持有数据」，不是「读一个 JSON 文件」。

CREATE TABLE rooms (
  id        BIGSERIAL PRIMARY KEY,
  code      VARCHAR(32) NOT NULL UNIQUE,
  building  VARCHAR(64) NOT NULL,
  floor     VARCHAR(16) NOT NULL,
  capacity  INT         NOT NULL,
  sockets   BOOLEAN,                       -- null = 未知（设施以现场为准）
  seat_type VARCHAR(60),
  CONSTRAINT capacity_positive CHECK (capacity > 0)
);

CREATE TABLE class_slots (
  id          BIGSERIAL PRIMARY KEY,
  room_id     BIGINT      NOT NULL REFERENCES rooms(id) ON DELETE CASCADE,
  day_of_week INT         NOT NULL,        -- 周一 = 1 … 周日 = 7
  start_time  TIME        NOT NULL,
  end_time    TIME        NOT NULL,
  course_code VARCHAR(32) NOT NULL,
  CONSTRAINT day_range CHECK (day_of_week BETWEEN 1 AND 7),
  CONSTRAINT slot_order CHECK (end_time > start_time)
);

CREATE INDEX idx_slot_room_day ON class_slots (room_id, day_of_week);

-- 单行表：接口的元信息。revision 在数据被改动时 +1，消费端可据此判断课表有没有变。
CREATE TABLE feed_meta (
  id           INT          PRIMARY KEY,
  provider     VARCHAR(120) NOT NULL,
  term         VARCHAR(40)  NOT NULL,
  generated_at TIMESTAMP    NOT NULL,
  revision     INT          NOT NULL DEFAULT 1,
  CONSTRAINT single_row CHECK (id = 1)
);

INSERT INTO feed_meta (id, provider, term, generated_at, revision)
VALUES (1, 'HKU timetable (simulated)', '2026/27 Semester 1', now(), 1);
