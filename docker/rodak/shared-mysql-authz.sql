WITH identity AS (
  SELECT ${username} AS username, ${clientid} AS client_id,
         ${client_attrs.rodak_id} AS device_uuid,
         ${client_attrs.rodak_version} AS binding_version,
         ${client_attrs.rodak_product} AS product_id,
         ${client_attrs.rodak_exp} AS expires_at,
         ${client_attrs.rodak_bridge} AS bridge
), principal AS (
  SELECT identity.*, CASE
    WHEN username = 'rodak-service' OR client_id = 'rodak-service' THEN 'bridge'
    WHEN LEFT(client_id, 8) = 'rodakos_' THEN 'device'
    ELSE 'legacy'
  END AS kind
  FROM identity
), active_device AS (
  SELECT d.device_key
  FROM principal i
  JOIN rodak_device d ON d.device_uuid = i.device_uuid AND d.device_key = i.username
  JOIN product p ON p.id = d.product_id
  WHERE i.kind = 'device' AND d.status = 'ACTIVE'
    AND CAST(d.token_version AS CHAR) = i.binding_version
    AND CAST(d.product_id AS CHAR) = i.product_id
    AND CAST(i.expires_at AS UNSIGNED) > UNIX_TIMESTAMP()
), device_rules AS (
  SELECT 'publish' AS action, 'telemetry' AS suffix, '' AS exact
  UNION ALL SELECT 'publish', 'shadow/report', ''
  UNION ALL SELECT 'publish', 'commands/+/ack', ''
  UNION ALL SELECT 'subscribe', 'shadow/desired', 'eq '
  UNION ALL SELECT 'subscribe', 'commands/+', 'eq '
  UNION ALL SELECT 'subscribe', 'ota/notify', 'eq '
  UNION ALL SELECT 'subscribe', 'pc_status', 'eq '
), rules AS (
  SELECT 0 AS priority, 0 AS rule_id, 'allow' AS permission, r.action,
         CONVERT(CONCAT(r.exact, 'devices/', d.device_key, '/', r.suffix)
                 USING utf8mb4) COLLATE utf8mb4_bin AS topic,
         '0,1,2' AS qos, '0' AS retain
  FROM active_device d CROSS JOIN device_rules r
  UNION ALL
  SELECT 0, 0, 'allow', 'subscribe', 'eq _rodak/events/#', '1', 'all'
  FROM principal WHERE kind = 'bridge' AND bridge = 'true'
  UNION ALL
  SELECT 0, 0, 'allow', 'publish', topic, '1', '0'
  FROM principal CROSS JOIN (
    SELECT 'devices/+/shadow/desired' AS topic
    UNION ALL SELECT 'devices/+/commands/+'
  ) bridge_rules
  WHERE kind = 'bridge' AND bridge = 'true'
  UNION ALL
  SELECT 1, 0, 'deny', 'all', '#', '0,1,2', 'all' FROM principal WHERE kind <> 'legacy'
  UNION ALL
  SELECT 1, 0, 'deny', 'all', topic, '0,1,2', 'all'
  FROM principal CROSS JOIN (
    SELECT 'devices/#' AS topic
    UNION ALL SELECT '_rodak/#'
  ) reserved
  WHERE kind = 'legacy'
  UNION ALL
  SELECT 2, a.id, a.permission, a.action,
         CONVERT(a.topic USING utf8mb4) COLLATE utf8mb4_bin,
         COALESCE(CAST(a.qos AS CHAR), '0,1,2'), COALESCE(CAST(a.retain AS CHAR), 'all')
  FROM principal i JOIN mqtt_acl a ON a.username = i.username
  WHERE i.kind = 'legacy'
)
SELECT permission, action, topic, qos, retain FROM rules ORDER BY priority, rule_id
