CREATE TABLE IF NOT EXISTS rodak_shadow (
  device_id BIGINT NOT NULL PRIMARY KEY,
  binding_version BIGINT NOT NULL,
  shadow_version BIGINT NOT NULL,
  desired TEXT NULL,
  reported MEDIUMTEXT NULL,
  telemetry MEDIUMTEXT NULL,
  telemetry_at TIMESTAMP(3) NULL
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS rodak_command (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  command_no VARCHAR(32) NOT NULL,
  device_id BIGINT NOT NULL,
  binding_version BIGINT NOT NULL,
  created_at TIMESTAMP(3) NOT NULL,
  expires_at TIMESTAMP(3) NOT NULL,
  acknowledgement MEDIUMTEXT NULL,
  UNIQUE KEY uk_rodak_command_no (command_no),
  KEY idx_rodak_command_pending (device_id, binding_version, expires_at),
  KEY idx_rodak_command_expiry (expires_at)
) ENGINE=InnoDB;
