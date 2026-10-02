-- 大楼名字写全称，不加校区前缀（CPD = Central Podium，本身在百周年校园，
-- 但界面上只写楼名就够）。列原本是 VARCHAR(16)，装不下 "K. K. Leung Building"（19 字符），
-- 所以先加宽。
ALTER TABLE rooms ALTER COLUMN building TYPE VARCHAR(64);

UPDATE rooms SET building = 'Central Podium'       WHERE building = 'CPD';
UPDATE rooms SET building = 'Knowles Building'     WHERE building = 'KB';
UPDATE rooms SET building = 'K. K. Leung Building' WHERE building = 'KK';
UPDATE rooms SET building = 'Main Building'        WHERE building = 'MB';
