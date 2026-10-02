-- V9: 自助注册 —— 邮箱 + 密码。后缀必须是 HKU 的（@hku.hk 或任意 *.hku.hk，如 @connect.hku.hk）。
-- 原型阶段不发验证邮件，所以不做邮箱验证；但域名后缀在接口层强制。
-- 早期演示账号没有邮箱，允许为 NULL；注册账号的邮箱唯一（不区分大小写）。

ALTER TABLE app_user ADD COLUMN email VARCHAR(128);

CREATE UNIQUE INDEX idx_app_user_email ON app_user (lower(email)) WHERE email IS NOT NULL;

INSERT INTO audit_log (action, entity, entity_id, actor, detail)
VALUES ('SCHEMA', 'app_user', NULL, 'system',
        'email column added for self-registration; the address must end in hku.hk (no verification mail in the prototype)');
