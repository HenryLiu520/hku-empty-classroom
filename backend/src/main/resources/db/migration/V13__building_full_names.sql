-- 大楼名字写全称，不加校区前缀（CPD = Central Podium，本身就在百周年校园，
-- 但界面上只写楼名就够，见 README 的建筑字段说明）。
UPDATE rooms SET building = 'Central Podium'       WHERE building = 'CPD';
UPDATE rooms SET building = 'Knowles Building'     WHERE building = 'KB';
UPDATE rooms SET building = 'K. K. Leung Building' WHERE building = 'KK';
UPDATE rooms SET building = 'Main Building'        WHERE building = 'MB';
