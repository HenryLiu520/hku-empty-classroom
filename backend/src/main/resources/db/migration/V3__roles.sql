-- V3: 角色模型改为两类 —— STUDENT（学生，只读） | ADMIN（管理人员，可写）
-- V1/V2 里写作角色的名字叫 TEACHER，这里统一改名并把账号补全。
-- 保留 V1/V2 不动，是为了让"改了什么"在迁移历史里看得见。

UPDATE app_user SET role = 'ADMIN' WHERE role = 'TEACHER';

-- 管理人员账号（原型级：SHA-256(salt:password)）
INSERT INTO app_user (username, display_name, role, salt, password_hash) VALUES
  ('admin1', 'Estates Office', 'ADMIN', 'a1b2c3d4', '72f7afa206f8b74b0be94369f7468ef46ff92e02fc8aada899948890ed4bf8f6');

INSERT INTO audit_log (action, entity, entity_id, actor, detail)
VALUES ('ROLE_MODEL', 'app_user', NULL, 'system',
        'Two roles only: STUDENT (read) and ADMIN (read + write). teacher1 renamed to role ADMIN, admin1 added');
