# 故障排除与常见问题

## 常见问题解答 (FAQ)

### 1. 平台基础问题

**Q: IntelliConnect 平台支持哪些设备类型？**
A: 平台支持所有支持 MQTT 协议的设备，包括 ESP32、树莓派、Arduino 等物联网设备。通过物模型抽象，可以灵活适配各种设备类型。

**Q: 如何重置管理员密码？**
A: 可以通过数据库直接修改用户表中的密码字段，密码需要使用 BCrypt 加密。或者通过应用的密码重置功能（如果已配置邮箱服务）。

**Q: 平台支持哪些大模型？**
A: 平台支持 Qwen3 系列、GLM 系列、DeepSeek 等多种大模型，也支持自定义模型提供商。

**Q: 如何扩展 Agent 智能体的能力？**
A: 可以通过 MCP (Model Context Protocol) 协议添加外部工具和服务来扩展 Agent 能力。

### 2. 安装与部署问题

**Q: Docker 部署时出现端口冲突怎么办？**
A: 检查是否有其他服务占用了相同端口，可以修改 docker-compose.yaml 中的端口映射，例如将 8080 改为 8081。

**Q: 应用启动时数据库连接失败如何解决？**
A: 检查以下几点：
- 数据库服务是否正常运行
- 数据库连接参数是否正确
- 数据库用户权限是否足够
- 网络连接是否正常

**Q: EMQX 连接失败怎么处理？**
A: 确认：
- EMQX 服务是否启动
- 认证配置是否正确
- 设备用户名密码是否匹配
- 网络防火墙设置

### 3. 设备连接问题

**Q: 设备无法连接到平台怎么办？**
A: 检查以下配置：
- MQTT 服务器地址和端口
- 设备认证信息（用户名/密码）
- 网络连接状态
- EMQX 服务状态

**Q: 如何查看设备在线状态？**
A: 可以通过 API 接口 `/api/v2/device/list` 查看设备状态，或在 EMQX Dashboard 中查看连接设备列表。

### 4. Agent 智能体问题

**Q: Agent 响应很慢怎么办？**
A: 可能的原因：
- 大模型 API 响应慢
- 网络延迟高
- 知识库检索耗时
- 可以尝试更换更快的模型或优化知识库

**Q: Agent 无法控制设备怎么办？**
A: 检查：
- 设备是否在线
- 物模型配置是否正确
- 设备权限是否足够
- MQTT 通信是否正常

## 故障排除指南

### 1. 应用启动问题

#### 问题: 应用启动失败
**症状**: 应用无法正常启动，控制台输出错误信息

**排查步骤**:
1. 检查日志文件 `logs/intelliconnect.log`
2. 确认数据库服务状态
3. 验证配置文件参数
4. 检查端口占用情况

**解决方案**:
```bash
# 检查日志
tail -f logs/intelliconnect.log

# 检查端口占用
netstat -tulpn | grep :8080

# 检查数据库连接
mysql -h localhost -u root -p
```

#### 问题: 数据库连接异常
**症状**: 启动时提示数据库连接失败

**排查步骤**:
1. 确认数据库服务是否运行
2. 检查数据库连接参数
3. 验证数据库用户权限

**解决方案**:
```bash
# 检查 MySQL 服务
systemctl status mysql

# 测试数据库连接
mysql -h localhost -u intelliconnect -p

# 检查数据库配置
cat application.yaml | grep datasource
```

### 2. MQTT 连接问题

#### 问题: 设备无法连接 MQTT
**症状**: 设备提示连接失败或认证失败

**排查步骤**:
1. 检查 EMQX 服务状态
2. 验证设备认证信息
3. 确认网络连接

**解决方案**:
```bash
# 检查 EMQX 服务
docker ps | grep emqx

# 查看 EMQX 日志
docker logs intelliconnect-emqx

# 检查连接状态
curl -u admin:public http://localhost:18083/api/v5/clients
```

#### 问题: 设备数据无法上报
**症状**: 设备连接正常但数据无法上报

**排查步骤**:
1. 检查 MQTT 主题权限
2. 验证设备订阅/发布权限
3. 确认数据格式是否正确

### 3. Agent 智能体问题

#### 问题: Agent 无法响应
**症状**: 发送消息后 Agent 没有响应

**排查步骤**:
1. 检查大模型 API 配置
2. 验证 API 密钥是否正确
3. 确认网络连接是否正常

**解决方案**:
```bash
# 检查 Agent 配置
cat application.yaml | grep -A 10 ai

# 测试大模型 API
curl -X POST https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation \
  -H "Authorization: Bearer YOUR_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{"model": "qwen-max", "input": {"prompt": "Hello"}}'
```

#### 问题: 知识库检索失败
**症状**: 知识库功能无法正常使用

**排查步骤**:
1. 检查向量数据库连接
2. 验证嵌入模型配置
3. 确认文档上传状态

### 4. 微信集成问题

#### 问题: 微信消息无法接收
**症状**: 微信服务器无法正常回调

**排查步骤**:
1. 检查微信配置参数
2. 验证服务器外网访问
3. 确认回调 URL 配置

**解决方案**:
```bash
# 检查微信配置
cat application.yaml | grep -A 10 wx

# 验证服务器访问
curl -v https://your-domain.com/api/v2/wechat/callback
```

## 性能优化

### 1. 数据库性能问题

**问题**: 查询速度慢
**解决方案**:
- 为常用查询字段添加索引
- 优化数据库连接池配置
- 定期清理历史数据

**问题**: 数据库连接池耗尽
**解决方案**:
```yaml
# 优化连接池配置
spring:
  datasource:
    hikari:
      maximum-pool-size: 50
      minimum-idle: 10
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
```

### 2. 内存问题

**问题**: 内存使用过高
**解决方案**:
- 调整 JVM 堆内存大小
- 优化缓存配置
- 定期清理临时文件

**JVM 配置示例**:
```bash
JAVA_OPTS="-Xms2g -Xmx4g -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
```

## 监控与诊断

### 1. 健康检查

**应用健康状态**:
```bash
curl http://localhost:8080/actuator/health
```

**详细健康信息**:
```bash
curl http://localhost:8080/actuator/health/db
curl http://localhost:8080/actuator/health/mqtt
curl http://localhost:8080/actuator/health/redis
```

### 2. 性能指标

**JVM 内存使用**:
```bash
curl http://localhost:8080/actuator/metrics/jvm.memory.used
```

**HTTP 请求统计**:
```bash
curl http://localhost:8080/actuator/metrics/http.server.requests
```

### 3. 日志分析

**查看错误日志**:
```bash
grep -i error logs/intelliconnect.log
grep -i exception logs/intelliconnect.log
```

**查看特定时间段日志**:
```bash
sed -n '/2024-01-04 10:00:00/,/2024-01-04 11:00:00/p' logs/intelliconnect.log
```

## 网络问题排查

### 1. 连接超时

**问题**: 网络连接超时
**解决方案**:
- 检查防火墙设置
- 验证网络带宽
- 调整超时配置

**超时配置示例**:
```yaml
spring:
  redis:
    timeout: 5000ms
  datasource:
    hikari:
      connection-timeout: 30000
      validation-timeout: 5000
```

### 2. DNS 解析问题

**问题**: DNS 解析失败
**解决方案**:
- 检查 DNS 配置
- 使用 IP 地址替代域名
- 配置 DNS 缓存

## 安全问题

### 1. 认证失败

**问题**: API 认证失败
**解决方案**:
- 检查 JWT Token 是否过期
- 验证 Token 格式是否正确
- 确认密钥配置

### 2. 权限不足

**问题**: 操作权限不足
**解决方案**:
- 检查用户角色配置
- 验证权限分配
- 确认 API 访问控制

## 数据恢复

### 1. 数据库恢复

**从备份恢复**:
```bash
mysql -u root -p cwliot1.8 < backup.sql
```

### 2. 配置恢复

**从备份恢复配置**:
```bash
cp backup/config/application.yaml config/application.yaml
systemctl restart intelliconnect
```

## 联系支持

如果以上方法无法解决问题，请提供以下信息联系技术支持：

1. 详细的错误信息和日志
2. 系统环境信息（操作系统、版本等）
3. 应用版本信息
4. 问题发生的具体步骤
5. 已尝试的解决方案

**支持渠道**:
- GitHub Issues: https://github.com/cwliot/IntelliConnect/issues
- 社区论坛: https://wordpress.rslly.top
- 交流群: 参见 README 文档