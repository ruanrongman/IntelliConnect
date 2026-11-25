SELECT password, salt, is_superuser
FROM mqtt_user
WHERE username = ${username}
  AND username <> 'rodak-service'
  AND ${clientid} <> 'rodak-service'
  AND LEFT(${clientid}, 8) <> 'rodakos_'
LIMIT 1
