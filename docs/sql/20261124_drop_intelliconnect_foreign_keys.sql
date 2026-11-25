-- Run in the IntelliConnect database. This migration can be run again safely.
-- Look up the constraint names because MySQL assigned the Rodak names at creation time.
SELECT COALESCE(CONCAT('ALTER TABLE `rodak_shadow` ', GROUP_CONCAT(
    CONCAT('DROP FOREIGN KEY `', REPLACE(CONSTRAINT_NAME, '`', '``'), '`')
    ORDER BY CONSTRAINT_NAME SEPARATOR ', ')), 'DO 0')
INTO @intelliconnect_fk_ddl
FROM information_schema.KEY_COLUMN_USAGE
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'rodak_shadow' AND COLUMN_NAME = 'device_id'
  AND REFERENCED_TABLE_SCHEMA = DATABASE()
  AND REFERENCED_TABLE_NAME = 'rodak_device' AND REFERENCED_COLUMN_NAME = 'id';
PREPARE intelliconnect_fk_stmt FROM @intelliconnect_fk_ddl;
EXECUTE intelliconnect_fk_stmt;
DEALLOCATE PREPARE intelliconnect_fk_stmt;

SELECT COALESCE(CONCAT('ALTER TABLE `rodak_command` ', GROUP_CONCAT(
    CONCAT('DROP FOREIGN KEY `', REPLACE(CONSTRAINT_NAME, '`', '``'), '`')
    ORDER BY CONSTRAINT_NAME SEPARATOR ', ')), 'DO 0')
INTO @intelliconnect_fk_ddl
FROM information_schema.KEY_COLUMN_USAGE
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'rodak_command' AND COLUMN_NAME = 'device_id'
  AND REFERENCED_TABLE_SCHEMA = DATABASE()
  AND REFERENCED_TABLE_NAME = 'rodak_device' AND REFERENCED_COLUMN_NAME = 'id';
PREPARE intelliconnect_fk_stmt FROM @intelliconnect_fk_ddl;
EXECUTE intelliconnect_fk_stmt;
DEALLOCATE PREPARE intelliconnect_fk_stmt;

SELECT COALESCE(CONCAT('ALTER TABLE `agent_workspace_file` ', GROUP_CONCAT(
    CONCAT('DROP FOREIGN KEY `', REPLACE(CONSTRAINT_NAME, '`', '``'), '`')
    ORDER BY CONSTRAINT_NAME SEPARATOR ', ')), 'DO 0')
INTO @intelliconnect_fk_ddl
FROM information_schema.KEY_COLUMN_USAGE
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'agent_workspace_file' AND COLUMN_NAME = 'workspace_id'
  AND REFERENCED_TABLE_SCHEMA = DATABASE()
  AND REFERENCED_TABLE_NAME = 'agent_workspace' AND REFERENCED_COLUMN_NAME = 'id';
PREPARE intelliconnect_fk_stmt FROM @intelliconnect_fk_ddl;
EXECUTE intelliconnect_fk_stmt;
DEALLOCATE PREPARE intelliconnect_fk_stmt;
