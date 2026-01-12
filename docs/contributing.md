# 贡献指南

## 欢迎贡献

感谢您对 IntelliConnect 项目的关注！我们欢迎任何形式的贡献，包括但不限于代码提交、文档完善、问题报告、功能建议等。

## 开发环境设置

### 1. 系统要求

- Java 17 或更高版本
- Maven 3.6.0 或更高版本
- MySQL 8.0
- Redis 6.0+
- Node.js (用于前端开发)
- Docker (可选，用于环境隔离)

### 2. 项目克隆

```bash
# 克隆项目
git clone https://github.com/cwliot/IntelliConnect.git
cd IntelliConnect

# 创建开发分支
git checkout -b feature/your-feature-name
```

### 3. 环境配置

```bash
# 使用 Maven 构建项目
./mvnw clean install -DskipTests

# 或者在 Windows 环境下
mvnw.cmd clean install -DskipTests
```

### 4. 数据库初始化

```bash
# 启动 MySQL 服务
# 创建数据库
CREATE DATABASE cwliot1.8 CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

# 启动 Redis 服务
redis-server &
```

## 代码规范

### 1. Java 代码规范

- 遵循 Oracle Java 编码规范
- 使用 4 个空格进行缩进（不要使用 Tab）
- 类名使用 PascalCase，方法和变量使用 camelCase
- 常量使用 UPPER_SNAKE_CASE
- 每行代码不超过 120 个字符
- 适当的注释和文档

### 2. 代码结构

```
src/
├── main/
│   ├── java/
│   │   └── top/rslly/iot/
│   │       ├── controllers/     # 控制器层
│   │       ├── services/       # 服务层
│   │       ├── models/         # 实体模型
│   │       ├── dao/            # 数据访问层
│   │       ├── param/          # 参数实体
│   │       ├── transfer/       # 数据传输对象
│   │       ├── config/         # 配置类
│   │       └── utility/        # 工具类
│   ├── resources/
│   │   ├── application.yaml    # 应用配置
│   │   └── static/            # 静态资源
│   └── proto/                 # 协议文件
└── test/                      # 测试代码
```

### 3. Git 提交规范

- 使用英文提交信息
- 提交信息格式：`<type>(<scope>): <subject>`
- Type 包括：feat, fix, docs, style, refactor, test, chore
- Subject 使用祈使句，首字母小写

**示例**:
```
feat(user): add user registration functionality
fix(auth): resolve JWT token expiration issue
docs(api): update API documentation for device management
```

## 贡献流程

### 1. Fork 项目

1. 访问 IntelliConnect 项目页面
2. 点击 "Fork" 按钮创建项目副本
3. 克隆您的 Fork 到本地

### 2. 创建功能分支

```bash
# 从主分支创建新分支
git checkout main
git pull origin main
git checkout -b feature/your-feature-name
```

### 3. 开发和测试

```bash
# 运行单元测试
./mvnw test

# 运行集成测试
./mvnw verify

# 代码格式化
./mvnw fmt:format
```

### 4. 提交更改

```bash
# 添加更改
git add .

# 提交更改
git commit -m "feat: add new feature description"

# 推送到远程分支
git push origin feature/your-feature-name
```

### 5. 创建 Pull Request

1. 访问您的 Fork 仓库
2. 点击 "New pull request"
3. 选择目标分支（通常是 main）
4. 填写 PR 描述
5. 提交 PR

## 代码审查

### 1. 审查标准

- 代码功能正确性
- 代码质量和可维护性
- 符合项目编码规范
- 充分的测试覆盖
- 文档完整性

### 2. 审查流程

1. 提交 PR 后，维护者会进行代码审查
2. 根据反馈修改代码
3. 通过审查后，PR 会被合并

## 测试要求

### 1. 单元测试

- 所有业务逻辑必须有单元测试
- 测试覆盖率应达到 80% 以上
- 使用 JUnit 5 和 Mockito

### 2. 集成测试

- 测试服务间的集成
- 测试数据库操作
- 测试 API 接口

### 3. 测试示例

```java
@SpringBootTest
class UserServiceTest {
    
    @Autowired
    private UserService userService;
    
    @MockBean
    private UserRepository userRepository;
    
    @Test
    void shouldCreateUserSuccessfully() {
        // Given
        User user = new User();
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        
        when(userRepository.save(any(User.class))).thenReturn(user);
        
        // When
        User result = userService.createUser(user);
        
        // Then
        assertThat(result.getUsername()).isEqualTo("testuser");
        verify(userRepository).save(any(User.class));
    }
}
```

## 文档贡献

### 1. API 文档

- 使用 Swagger 注解完善 API 文档
- 为新接口添加详细说明
- 提供请求/响应示例

### 2. 用户文档

- 更新相关文档
- 添加使用示例
- 完善故障排除指南

### 3. 代码注释

- 为公共方法添加 JavaDoc
- 为复杂逻辑添加注释
- 保持注释与代码同步

## 技术栈

### 1. 后端技术

- **框架**: Spring Boot 2.7
- **安全**: Spring Security + JWT
- **数据访问**: JPA + Hibernate
- **缓存**: Redis
- **消息队列**: MQTT (EMQX)
- **时序数据库**: InfluxDB
- **向量数据库**: Chroma

### 2. 前端技术

- **框架**: Vue 3
- **UI 库**: Ant Design
- **状态管理**: Pinia

### 3. 构建工具

- **构建**: Maven
- **依赖管理**: Maven Central
- **代码格式化**: google-java-format

## 常见贡献类型

### 1. 功能开发

- 实现新功能
- 优化现有功能
- 添加新 API 接口

### 2. Bug 修复

- 修复已知问题
- 优化性能问题
- 改进错误处理

### 3. 文档完善

- 更新用户文档
- 完善 API 文档
- 添加使用示例

### 4. 测试增强

- 增加测试用例
- 改进测试覆盖率
- 优化测试性能

## 社区参与

### 1. 问题报告

- 在 GitHub Issues 中报告问题
- 提供详细的复现步骤
- 包含错误日志和环境信息

### 2. 功能建议

- 在 Issues 中提出功能建议
- 说明使用场景和需求
- 讨论实现方案

### 3. 代码审查

- 参与其他贡献者的代码审查
- 提供建设性反馈
- 帮助改进代码质量

## 联系方式

- **GitHub Issues**: https://github.com/cwliot/IntelliConnect/issues
- **社区论坛**: https://wordpress.rslly.top
- **交流群**: 请参见项目 README

## 许可证

本项目采用 Apache 2.0 许可证。贡献的代码也将遵循此许可证。

## 感谢

感谢所有为 IntelliConnect 项目做出贡献的开发者！您的努力让这个项目变得更好。