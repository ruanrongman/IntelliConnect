# 安全与认证

## 概述

IntelliConnect 平台采用多层次安全架构，确保用户数据、设备通信和系统资源的安全性。平台集成了 Spring Security 框架、JWT 认证机制和 EMQX 设备认证系统。

## 认证机制

### JWT Token 认证

平台使用 JWT (JSON Web Token) 进行用户身份验证和会话管理：

- **Token 生成**: 用户登录成功后，服务器生成包含用户信息的 JWT Token
- **Token 有效期**: 默认 24 小时，可配置
- **Token 刷新**: 支持 Token 刷新机制
- **Token 验证**: 每次 API 请求都需要在请求头中携带有效的 Token

**请求头格式:**
```
Authorization: Bearer {jwt_token}
```

### 用户角色与权限

平台采用基于角色的访问控制 (RBAC) 模型：

- **ADMIN**: 系统管理员，拥有所有权限
- **USER**: 普通用户，可管理自己的设备和数据
- **GUEST**: 访客用户，有限的只读权限

### 密码安全

- **密码加密**: 使用 BCrypt 算法对用户密码进行哈希加密
- **密码策略**: 要求密码长度至少 8 位，包含字母和数字
- **密码重置**: 支持通过邮箱进行密码重置

## 设备安全

### EMQX 设备认证

平台使用 EMQX 作为 MQTT 消息中间件，并配置了设备认证机制：

- **MySQL 认证**: 设备连接时通过 MySQL 数据库进行身份验证
- **客户端 ID 验证**: 验证设备的唯一标识符
- **用户名/密码认证**: 每个设备都有独立的用户名和密码

### 设备授权

- **主题权限控制**: 控制设备对 MQTT 主题的发布/订阅权限
- **QoS 级别限制**: 限制设备的 MQTT 服务质量级别
- **连接频率限制**: 防止设备频繁连接导致的 DoS 攻击

### 设备证书

- **TLS/SSL 加密**: 支持设备与服务器之间的端到端加密通信
- **客户端证书**: 可选的双向 SSL 认证机制

## API 安全

### 请求验证

- **参数校验**: 所有 API 请求参数都会进行严格校验
- **SQL 注入防护**: 使用参数化查询防止 SQL 注入攻击
- **XSS 防护**: 对用户输入进行过滤和转义

### 速率限制

- **API 调用频率限制**: 防止恶意用户频繁调用 API
- **IP 黑名单**: 支持将恶意 IP 地址加入黑名单

### CORS 配置

- **跨域资源共享**: 限制允许访问的域名
- **请求头验证**: 验证跨域请求的来源和类型

## 数据安全

### 数据加密

- **传输加密**: 所有数据传输使用 HTTPS/TLS 加密
- **存储加密**: 敏感数据在数据库中进行加密存储
- **密钥管理**: 使用安全的密钥管理系统

### 数据备份

- **定期备份**: 自动备份重要数据
- **备份加密**: 备份数据进行加密存储
- **备份验证**: 定期验证备份数据的完整性

## 网络安全

### 防火墙配置

- **端口限制**: 仅开放必要的服务端口
- **IP 白名单**: 限制特定 IP 地址的访问权限
- **DDoS 防护**: 配置 DDoS 攻击防护机制

### 网络隔离

- **DMZ 区域**: 将对外服务部署在 DMZ 区域
- **内网隔离**: 核心数据库与外部网络隔离
- **VPN 访问**: 管理员通过 VPN 访问内部系统

## 微信集成安全

### OAuth2 认证

- **AppID/AppSecret**: 安全存储微信应用凭证
- **回调地址验证**: 验证微信服务器回调地址
- **消息加密**: 微信消息传输加密

### 消息安全

- **消息签名验证**: 验证微信服务器发送的消息签名
- **Token 验证**: 验证微信接口调用的 Token
- **数据加密**: 用户敏感信息加密存储

## MCP 协议安全

### 服务认证

- **MCP 服务器验证**: 验证外部 MCP 服务的合法性
- **服务权限控制**: 控制 MCP 服务可访问的资源范围
- **通信加密**: MCP 服务通信使用加密协议

## 安全配置

### application.yaml 安全配置示例

```yaml
# 安全配置
security:
  jwt:
    secret: ${JWT_SECRET:default_secret_key_for_development}  # JWT 密钥
    expiration: 86400000  # Token 有效期 (毫秒)
    refresh-expiration: 604800000  # 刷新 Token 有效期 (毫秒)
  
  # 密码策略
  password:
    min-length: 8
    require-uppercase: true
    require-lowercase: true
    require-numbers: true
    require-special-chars: false

# EMQX 安全配置
emqx:
  auth:
    mysql:
      server: ${EMQX_MYSQL_HOST:localhost}
      port: ${EMQX_MYSQL_PORT:3306}
      database: ${EMQX_MYSQL_DB:emqx}
      username: ${EMQX_MYSQL_USER:emqx}
      password: ${EMQX_MYSQL_PASSWORD:emqx123}
  
  # SSL 配置
  ssl:
    enabled: true
    keystore: /path/to/keystore.jks
    keystore-password: keystore_password
    truststore: /path/to/truststore.jks
    truststore-password: truststore_password

# 微信安全配置
wx:
  security:
    token: ${WX_TOKEN:default_token}
    encoding-aes-key: ${WX_ENCODING_AES_KEY:}
    validate-signature: true
    validate-https: true
```

## 安全最佳实践

### 部署安全

1. **环境变量**: 敏感配置信息使用环境变量而非硬编码
2. **密钥管理**: 使用专门的密钥管理系统 (如 HashiCorp Vault)
3. **容器安全**: 使用非 root 用户运行容器
4. **镜像安全**: 定期更新基础镜像，扫描安全漏洞

### 运行时安全

1. **日志审计**: 记录所有安全相关事件
2. **异常监控**: 监控异常登录和操作行为
3. **定期更新**: 及时更新系统和依赖库
4. **安全扫描**: 定期进行安全漏洞扫描

### 开发安全

1. **代码审查**: 实施安全代码审查流程
2. **依赖管理**: 定期检查和更新依赖库
3. **安全测试**: 进行渗透测试和安全评估
4. **安全培训**: 定期进行安全意识培训

## 安全事件响应

### 监控指标

- **登录失败次数**: 监控异常登录尝试
- **API 调用频率**: 监控异常 API 调用
- **设备连接异常**: 监控设备连接异常
- **数据访问异常**: 监控异常数据访问

### 应急响应

1. **事件分类**: 根据严重程度分类安全事件
2. **响应流程**: 建立标准化应急响应流程
3. **通知机制**: 建立安全事件通知机制
4. **恢复计划**: 制定系统恢复计划

## 合规性

平台遵循以下安全标准和合规要求：

- **GDPR**: 保护用户隐私和数据安全
- **网络安全法**: 符合国家网络安全法规
- **等保三级**: 满足信息系统安全等级保护要求