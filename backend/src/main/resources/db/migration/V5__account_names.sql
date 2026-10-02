-- V5: 演示账号名与角色口径统一 —— student1 -> user1（口令同步改为 user123）
-- V1/V2 里的 'student1' 保留原样，改名这件事由本迁移记录，历史可追溯。

UPDATE app_user
   SET username      = 'user1',
       salt          = 'u9e2c7f1',
       password_hash = 'f2cfcd6cb48497fdd0c785961dc961b07cdd30a6326603b42ea8ecc5492594b3'
 WHERE username = 'student1';

INSERT INTO audit_log (action, entity, entity_id, actor, detail)
VALUES ('ACCOUNT_NAMES', 'app_user', NULL, 'system',
        'Student demo account renamed to match the role names: student1 -> user1, password user123');
