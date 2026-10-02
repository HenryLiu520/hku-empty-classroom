-- V1: schema for the Empty Classroom Plan (prototype)
-- 依据 Plan §3.6：rooms / class_slots（基线课表）/ room_updates（教师临时变更）/ audit_log

CREATE TABLE rooms (
  id          BIGSERIAL PRIMARY KEY,
  code        VARCHAR(32)  NOT NULL UNIQUE,   -- 沿用学校房号，如 CPD-LG.01
  building    VARCHAR(16)  NOT NULL,
  floor       VARCHAR(16)  NOT NULL,
  capacity    INT          NOT NULL,
  room_type   VARCHAR(24)  NOT NULL
);

-- 基线课表：按星期几循环（大学课表本身就是周期性的）
CREATE TABLE class_slots (
  id          BIGSERIAL PRIMARY KEY,
  room_id     BIGINT      NOT NULL REFERENCES rooms(id) ON DELETE CASCADE,
  day_of_week INT         NOT NULL,           -- 1=Mon ... 7=Sun
  start_time  TIME        NOT NULL,
  end_time    TIME        NOT NULL,
  course_code VARCHAR(32) NOT NULL,
  CONSTRAINT slot_order CHECK (end_time > start_time)
);
CREATE INDEX idx_slot_room_day ON class_slots (room_id, day_of_week);

-- 教师临时变更：REQUISITION 征用 / CLOSURE 关闭 / CANCELLED_CLASS 某节取消（释放时间）
CREATE TABLE room_updates (
  id          BIGSERIAL PRIMARY KEY,
  room_id     BIGINT       NOT NULL REFERENCES rooms(id) ON DELETE CASCADE,
  change_type VARCHAR(24)  NOT NULL,
  slot_date   DATE         NOT NULL,
  start_time  TIME         NOT NULL,
  end_time    TIME         NOT NULL,
  reason      VARCHAR(200),
  created_by  VARCHAR(64)  NOT NULL,
  created_at  TIMESTAMP    NOT NULL DEFAULT now(),
  expires_at  TIMESTAMP,
  active      BOOLEAN      NOT NULL DEFAULT TRUE,
  CONSTRAINT update_order CHECK (end_time > start_time)
);
CREATE INDEX idx_update_room_date ON room_updates (room_id, slot_date);

CREATE TABLE audit_log (
  id          BIGSERIAL PRIMARY KEY,
  action      VARCHAR(32)  NOT NULL,
  entity      VARCHAR(32)  NOT NULL,
  entity_id   BIGINT,
  actor       VARCHAR(64)  NOT NULL,
  detail      VARCHAR(400),
  created_at  TIMESTAMP    NOT NULL DEFAULT now()
);

-- 原型级账号表（口令用 SHA-256(salt:password)，非生产级）
CREATE TABLE app_user (
  id            BIGSERIAL PRIMARY KEY,
  username      VARCHAR(64) NOT NULL UNIQUE,
  display_name  VARCHAR(64) NOT NULL,
  role          VARCHAR(16) NOT NULL,          -- STUDENT | TEACHER
  salt          VARCHAR(32) NOT NULL,
  password_hash VARCHAR(64) NOT NULL
);
