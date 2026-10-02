-- V12: 角色分三层，权限分离。
--
--   user        只看：查询、教室页、写自己的评价
--   admin       user 的全部 + 锁定/释放教室时间（整块）、编辑设施、删任何评价
--   superadmin  以上全部 + 管理账户：增删改查、提权降权
--
-- 角色用等级判定（superadmin ⊇ admin ⊇ user），所以只需要一个超级管理员账号就能覆盖所有权限。
-- 演示账号密码与其它种子账号同一套规则（SHA-256(salt:password)），口令见 README。

INSERT INTO app_user (username, display_name, role, salt, password_hash, email)
VALUES ('super1', 'Estates Office (super)', 'superadmin', '39150150',
        '6b52df3fa4419a086947afff72fc98787a55d4db5dae9482b06cd594079f1bea', NULL);

INSERT INTO audit_log (action, entity, entity_id, actor, detail)
VALUES ('SCHEMA', 'app_user', NULL, 'system',
        'Three level roles: user (read only) < admin (room time lock/release) < superadmin (also manages accounts)');
