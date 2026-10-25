-- Verified on the configured database: history_message.content is VARCHAR(6144)
-- (24576 bytes with utf8mb4). Complete router arguments/results can exceed this.
-- Applied to the configured database on 2026-10-22; 1502 existing rows preserved.
-- Use this statement to update other deployments before enabling complete event persistence.
-- This expands content capacity and preserves existing rows; no new columns are needed.
ALTER TABLE history_message MODIFY COLUMN content LONGTEXT NOT NULL;
