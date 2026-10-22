-- V15: 评价可以带照片
-- 设计：**文件存本地磁盘、数据库只存指针**。原因：
--   1) 图片放进 bytea 会把每行撑到几 MB，列表查询会被一起拖慢；
--   2) 备份/迁移数据库时不必搬大文件；
--   3) 原型阶段没有对象存储，磁盘目录是最短路径。
-- 约束：一条评价最多 3 张（应用层校验 + 这里的触发器式检查留给报告讨论，不在 DB 里做）。

CREATE TABLE review_photos (
  id           BIGSERIAL PRIMARY KEY,
  review_id    BIGINT       NOT NULL REFERENCES room_reviews(id) ON DELETE CASCADE,
  file_name    VARCHAR(80)  NOT NULL,   -- 服务器生成的名字（UUID + 扩展名），不是用户原始文件名
  content_type VARCHAR(40)  NOT NULL,   -- 嗅探出来的真实类型，不是浏览器声明的
  byte_size    INT          NOT NULL,
  created_at   TIMESTAMP    NOT NULL DEFAULT now(),
  CONSTRAINT photo_size_positive CHECK (byte_size > 0),
  CONSTRAINT photo_name_unique UNIQUE (file_name)
);

CREATE INDEX idx_photo_review ON review_photos (review_id, id);

INSERT INTO audit_log (action, entity, entity_id, actor, detail)
VALUES ('SCHEMA_REVIEW_PHOTOS', 'review_photos', NULL, 'system',
        'Review photos added: files on disk, pointers in the database, at most 3 per review');
