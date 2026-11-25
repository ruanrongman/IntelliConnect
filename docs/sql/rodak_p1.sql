CREATE TABLE IF NOT EXISTS rodak_device (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  device_uuid VARCHAR(36) NOT NULL,
  device_key VARCHAR(128) NOT NULL,
  product_id INT NOT NULL,
  product_key VARCHAR(64) NOT NULL DEFAULT 'rymcu-bigsmart',
  status VARCHAR(32) NOT NULL,
  credential_digest VARCHAR(128) NOT NULL,
  token_version BIGINT NOT NULL DEFAULT 1,
  last_seen_at TIMESTAMP NULL,
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP NOT NULL,
  UNIQUE KEY uk_rodak_device_uuid (device_uuid),
  UNIQUE KEY uk_rodak_device_key (device_key),
  KEY idx_rodak_device_product (product_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS rodak_pairing_request (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  request_id VARCHAR(32) NOT NULL,
  device_key VARCHAR(128) NOT NULL,
  credential_digest VARCHAR(128) NOT NULL,
  pairing_code_digest VARCHAR(128) NOT NULL,
  request_token_digest VARCHAR(128) NOT NULL,
  product_id INT NULL,
  source_ip VARCHAR(64) NULL,
  status VARCHAR(32) NOT NULL,
  failed_attempts INT NOT NULL DEFAULT 0,
  expires_at TIMESTAMP NOT NULL,
  created_at TIMESTAMP NOT NULL,
  UNIQUE KEY uk_rodak_pairing_request_id (request_id),
  KEY idx_rodak_pairing_device (device_key),
  KEY idx_rodak_pairing_expires (expires_at),
  KEY idx_rodak_pairing_status (status)
) ENGINE=InnoDB;
