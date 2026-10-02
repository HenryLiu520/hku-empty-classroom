-- V6: 变更类型简化为两个方向 —— USE（添加使用） | RELEASE（释放时间）
-- 原来的 REQUISITION / CLOSURE 对可用性的影响完全一样（都让教室不可用），只是理由不同，
-- 所以合并成一个 USE，"理由"照样写在 reason 里。

UPDATE room_updates SET change_type = 'USE'     WHERE change_type IN ('REQUISITION', 'CLOSURE');
UPDATE room_updates SET change_type = 'RELEASE' WHERE change_type = 'CANCELLED_CLASS';

INSERT INTO audit_log (action, entity, entity_id, actor, detail)
VALUES ('CHANGE_TYPES', 'room_updates', NULL, 'system',
        'Only two directions now: USE (add a use) and RELEASE (free the time). REQUISITION and CLOSURE merged into USE');
