-- V4: 角色名按最终口径定为小写 —— user（学生，只读） | admin（管理人员，可写）
-- V1/V2/V3 一律不改动，迁移历史本身就是"改了什么"的记录。

UPDATE app_user SET role = 'user'  WHERE role = 'STUDENT';
UPDATE app_user SET role = 'admin' WHERE role = 'ADMIN';

INSERT INTO audit_log (action, entity, entity_id, actor, detail)
VALUES ('ROLE_NAMES', 'app_user', NULL, 'system',
        'Roles renamed to the final spelling: user (read only) and admin (read + write)');
