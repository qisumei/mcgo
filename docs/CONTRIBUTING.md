# 贡献指南

感谢您对 MCGO 项目的关注！本文档将帮助您了解如何参与项目开发。

## 目录

- [开发环境设置](#开发环境设置)
- [项目结构](#项目结构)
- [开发工作流](#开发工作流)
- [代码规范](#代码规范)
- [提交规范](#提交规范)
- [Pull Request 流程](#pull-request-流程)
- [测试要求](#测试要求)
- [文档更新](#文档更新)

## 开发环境设置

### 前置要求

- **JDK 21** 或更高版本
- **Gradle 8.x+** (项目已包含 Gradle Wrapper)
- **Git**
- **IDE**: IntelliJ IDEA 或 Eclipse（推荐 IDEA）

### 克隆项目

```bash
git clone https://github.com/qisumei/mcgo.git
cd mcgo
```

### 构建项目

```bash
# 完整构建
./gradlew build

# 仅编译
./gradlew compileJava

# 运行测试
./gradlew test
```

### 运行开发环境

```bash
# 运行客户端
./gradlew :mcgo-platform-neoforge:runClient

# 运行服务器
./gradlew :mcgo-platform-neoforge:runServer
```

### IDE 设置

#### IntelliJ IDEA

1. 打开项目目录
2. IDEA 会自动检测 Gradle 项目
3. 等待 Gradle 同步完成
4. 配置 Java 21 SDK
5. 启用 Gradle JVM toolchain

#### Eclipse

1. 导入 Gradle 项目
2. 运行 `./gradlew eclipse`
3. 刷新项目

## 项目结构

详细的项目架构请参考 [ARCHITECTURE.md](ARCHITECTURE.md)。

### 模块概览

```
mcgo/
├── mcgo-api/              # 公共 API 接口
├── mcgo-core/             # 核心业务逻辑
├── mcgo-economy/          # 经济系统
├── mcgo-network/          # 网络通信
├── mcgo-data/             # 数据持久化
├── mcgo-platform-neoforge/# 平台绑定
├── mcgo-client/           # 客户端
├── mcgo-server/           # 服务端
├── mcgo-integration-qiscsgo/ # 集成适配
└── mcgo-test/             # 测试
```

### 重要目录

- `src/main/java/` - 源代码
- `src/main/resources/` - 资源文件
- `src/test/java/` - 测试代码
- `docs/` - 文档
- `run/` - 运行时配置（不提交到版本控制）

## 开发工作流

### 1. 创建功能分支

```bash
# 从主分支创建新分支
git checkout main
git pull origin main
git checkout -b feature/your-feature-name
```

### 分支命名规范

- `feature/` - 新功能
- `bugfix/` - 错误修复
- `refactor/` - 重构
- `docs/` - 文档更新
- `test/` - 测试相关

### 2. 进行开发

- 遵循 [代码规范](#代码规范)
- 编写单元测试
- 保持提交原子性

### 3. 本地测试

```bash
# 运行所有测试
./gradlew test

# 运行特定模块测试
./gradlew :mcgo-core:test

# 代码格式化
./gradlew spotlessApply

# 静态分析
./gradlew check
```

### 4. 提交更改

遵循 [提交规范](#提交规范) 提交代码。

### 5. 推送并创建 PR

```bash
git push origin feature/your-feature-name
```

然后在 GitHub 上创建 Pull Request。

## 代码规范

### Java 代码规范

#### 命名约定

- **类名**: PascalCase（例如：`MatchManager`）
- **方法名**: camelCase（例如：`startMatch()`）
- **常量**: UPPER_SNAKE_CASE（例如：`MAX_PLAYERS`）
- **变量**: camelCase（例如：`playerCount`）

#### 格式化

- **缩进**: 4 个空格（不使用 Tab）
- **行宽**: 建议不超过 120 字符
- **大括号**: K&R 风格
  ```java
  public void method() {
      // code
  }
  ```

#### 注释

- 公共 API 必须有 Javadoc
- 复杂逻辑需要行内注释
- 注释要说明"为什么"而不是"做什么"

```java
/**
 * Calculates the economy reward for a round win.
 *
 * @param team the winning team
 * @param roundNumber the current round number
 * @return the reward amount
 */
public int calculateWinReward(Team team, int roundNumber) {
    // Complex logic explanation
    return reward;
}
```

### 包结构规范

遵循模块化原则：

```
com.selfabandonment.mcgo.{module}/
├── {feature}/         # 功能包
│   ├── {Feature}.java
│   └── {Feature}Impl.java
└── ...
```

### 依赖规则

- **严格遵循模块依赖关系**
- **不要引入循环依赖**
- **核心模块禁止平台依赖**

#### 允许的依赖方向

```
mcgo-platform-neoforge → all modules
mcgo-client → mcgo-api, mcgo-network
mcgo-server → mcgo-api, mcgo-core, mcgo-data, mcgo-economy
mcgo-core → mcgo-api only
mcgo-api → no dependencies
```

## 提交规范

### 提交消息格式

```
<type>(<scope>): <subject>

<body>

<footer>
```

### Type 类型

- `feat`: 新功能
- `fix`: 错误修复
- `refactor`: 重构
- `docs`: 文档更新
- `test`: 测试相关
- `chore`: 构建/工具更改
- `style`: 代码格式（不影响功能）
- `perf`: 性能优化

### Scope 范围

使用模块名作为范围：
- `api`
- `core`
- `economy`
- `network`
- `data`
- `platform`
- `client`
- `server`

### 示例

```
feat(economy): add wallet service interface

Implement WalletService interface in mcgo-api module to
support multiple currency types and transaction history.

Refs #123
```

### 提交原则

1. **原子性**: 每个提交完成一个逻辑单元
2. **可编译**: 每个提交都应该能够编译通过
3. **可回退**: 任何提交都应该能够单独回退
4. **清晰描述**: 说明改动的目的和影响

## Pull Request 流程

### 创建 PR 前检查清单

- [ ] 代码已格式化（`./gradlew spotlessApply`）
- [ ] 所有测试通过（`./gradlew test`）
- [ ] 静态分析通过（`./gradlew check`）
- [ ] 添加必要的测试
- [ ] 更新相关文档
- [ ] 提交消息符合规范
- [ ] 没有合并冲突

### PR 描述模板

```markdown
## 变更描述
简要描述这个 PR 的目的和改动内容。

## 变更类型
- [ ] 新功能
- [ ] 错误修复
- [ ] 重构
- [ ] 文档更新
- [ ] 其他

## 测试
描述如何测试这些变更。

## 相关 Issue
Closes #issue_number

## 截图（如适用）
添加截图帮助审查者理解变更。

## 检查清单
- [ ] 代码已格式化
- [ ] 测试已添加/更新
- [ ] 文档已更新
- [ ] CI 检查通过
```

### PR 审查标准

#### 代码质量
- 遵循项目代码规范
- 没有明显的性能问题
- 错误处理适当
- 没有安全隐患

#### 测试覆盖
- 新功能有单元测试
- 关键路径有集成测试
- 测试覆盖率不下降

#### 文档完整
- 公共 API 有 Javadoc
- 复杂逻辑有注释
- 架构变更更新文档

#### 可维护性
- 代码清晰易懂
- 没有过度设计
- 遵循 SOLID 原则

### 审查流程

1. **提交 PR**: 创建 PR 并填写描述
2. **CI 检查**: 等待 CI 自动检查通过
3. **代码审查**: 至少一位维护者审查
4. **修改反馈**: 根据反馈修改代码
5. **批准合并**: 审查通过后合并

## 测试要求

### 单元测试

- **位置**: `src/test/java/`
- **框架**: JUnit 5
- **覆盖率**: 核心模块 > 80%

```java
@Test
void shouldCalculateCorrectWinReward() {
    // Given
    Team team = Team.CT;
    int roundNumber = 5;
    
    // When
    int reward = economyService.calculateWinReward(team, roundNumber);
    
    // Then
    assertEquals(3250, reward);
}
```

### 集成测试

- **位置**: `mcgo-test` 模块
- **范围**: 跨模块交互
- **场景**: 关键业务流程

### 测试命名

```java
// 模式: should{ExpectedBehavior}When{Condition}
@Test
void shouldThrowExceptionWhenPlayerNotFound() {
    // test code
}
```

### Mock 使用

```java
@Mock
private MatchRepository matchRepository;

@InjectMocks
private MatchService matchService;

@BeforeEach
void setUp() {
    MockitoAnnotations.openMocks(this);
}
```

## 文档更新

### 需要更新文档的情况

- 添加新的公共 API
- 修改现有 API 行为
- 添加新模块或功能
- 架构或设计变更
- 配置选项变更

### 文档位置

- **README.md**: 项目概述和快速开始
- **docs/ARCHITECTURE.md**: 架构设计
- **docs/QUICK_START.md**: 详细使用指南
- **docs/CHANGELOG.md**: 变更记录
- **模块 README**: 各模块具体说明

### 文档规范

- 使用 Markdown 格式
- 保持简洁清晰
- 包含代码示例
- 及时更新

## 发布流程

发布由维护者负责，一般流程：

1. 更新版本号
2. 更新 CHANGELOG
3. 创建 Git Tag
4. 构建发布包
5. 发布到 GitHub Releases

## 获取帮助

### 遇到问题？

- 查看 [QUICK_START.md](QUICK_START.md)
- 查看已有 Issues
- 在 Discussions 中讨论
- 创建新 Issue 描述问题

### 联系方式

- **Issue Tracker**: GitHub Issues
- **Discussions**: GitHub Discussions
- **Email**: 维护者邮箱（见 README）

## 行为准则

### 我们的承诺

为了营造开放友好的环境，我们承诺：

- 尊重不同的观点和经验
- 接受建设性的批评
- 关注对社区最有利的事情
- 同理心对待社区成员

### 不可接受的行为

- 使用性别化语言或图像
- 人身攻击或侮辱
- 骚扰行为
- 发布他人隐私信息
- 其他不专业或不受欢迎的行为

## 许可证

通过向本项目贡献代码，您同意您的贡献将按照项目的 MIT 许可证进行授权。

---

**感谢您的贡献！**

如有任何疑问，请随时创建 Issue 或在 Discussions 中讨论。
