-- Run against the IntelliConnect database BEFORE starting the updated backend.
-- No existing conversations or files are changed. Keep these tables when clearing chat memory.
CREATE TABLE IF NOT EXISTS agent_workspace (
    id BIGINT NOT NULL AUTO_INCREMENT,
    product_id INT NOT NULL,
    chat_id VARCHAR(255) COLLATE utf8mb4_bin NOT NULL,
    created_at BIGINT NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_workspace_product_chat (product_id, chat_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS agent_workspace_file (
    id VARCHAR(36) NOT NULL,
    workspace_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    size BIGINT NOT NULL,
    source VARCHAR(32) NOT NULL,
    created_at BIGINT NOT NULL,
    PRIMARY KEY (id),
    KEY idx_workspace_file_created (workspace_id, created_at),
    CONSTRAINT fk_workspace_file_workspace FOREIGN KEY (workspace_id)
        REFERENCES agent_workspace (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
