-- V7: 房间评价（学生评价房间，不是评价人）
-- 一条记录 = 一个已登录用户对一间房的评价：星级 1-5 + 文字。
-- 约束：**每人对每间房只能有一条**（可修改/删除），避免刷屏，也让"平均分"不会被一个人拉偏。

CREATE TABLE room_reviews (
  id          BIGSERIAL PRIMARY KEY,
  room_id     BIGINT       NOT NULL REFERENCES rooms(id) ON DELETE CASCADE,
  rating      INT          NOT NULL,
  body        VARCHAR(600) NOT NULL,
  created_by  VARCHAR(64)  NOT NULL,
  created_at  TIMESTAMP    NOT NULL DEFAULT now(),
  updated_at  TIMESTAMP    NOT NULL DEFAULT now(),
  CONSTRAINT rating_range CHECK (rating BETWEEN 1 AND 5),
  CONSTRAINT one_review_per_room_per_user UNIQUE (room_id, created_by)
);
CREATE INDEX idx_review_room ON room_reviews (room_id, created_at DESC);

-- 演示用评价（原型数据，非真实学生意见）
INSERT INTO room_reviews (room_id, rating, body, created_by)
SELECT r.id, t.rating, t.body, t.who
FROM rooms r JOIN (VALUES
  ('CPD-LG.01', 5, 'Plenty of space, sockets along the wall, and it stays quiet in the afternoon.', 'user1'),
  ('CPD-LG.01', 4, 'Good for group work, but the air conditioning is loud near the front.', 'teacher1'),
  ('CPD-2.45',  3, 'Fine for two people, gets crowded when a lecture lets out next door.', 'user1'),
  ('KB-110',    4, 'Bright and easy to find. No socket near the back rows.', 'admin1')
) AS t(code, rating, body, who) ON t.code = r.code;

INSERT INTO audit_log (action, entity, entity_id, actor, detail)
VALUES ('SCHEMA_REVIEWS', 'room_reviews', NULL, 'system',
        'Room reviews added: one review per person per room, rating 1-5 plus text');
