# MCGO 架构文档

## 概述

MCGO 项目采用模块化架构，遵循端口-适配器（Ports & Adapters）模式和领域驱动设计（DDD）原则。本文档描述了系统的整体架构、模块划分和设计决策。

## 架构原则

### 1. 稳定 API 优先
- 所有对外能力收敛到 `mcgo-api` 模块
- 以接口和DTO为主，确保可替换性和可扩展性
- 保持二进制兼容性

### 2. 平台隔离
- NeoForge/Minecraft 耦合仅存在于 `mcgo-platform-neoforge`
- 核心逻辑完全平台无关
- 客户端和服务端逻辑清晰分离

### 3. 领域分层
- 采用端口-适配器（Ports & Adapters）架构
- 明确的领域边界和职责划分
- 依赖方向：外层依赖内层，内层不依赖外层

### 4. 可测试性
- 核心模块零外部依赖
- 纯 Java 单元测试
- 平台集成通过集成测试验证

### 5. 文档与自动化
- 完整的文档体系
- 代码规范和约定
- CI/CD 保障演进质量

## 模块结构

### 依赖关系图

```
┌─────────────────────────────────────────────────────────────┐
│                      mcgo-platform-neoforge                  │
│          (Platform Binding - NeoForge Integration)           │
└────────────────────┬────────────────────────────────────────┘
                     │ depends on
        ┌────────────┴────────────┐
        │                         │
        ▼                         ▼
┌──────────────┐          ┌──────────────┐
│  mcgo-client │          │  mcgo-server │
│   (Client)   │          │   (Server)   │
└──────┬───────┘          └──────┬───────┘
       │ depends on              │
       │    ┌────────────────────┘
       │    │         ┌───────────────────────┐
       │    │         │  mcgo-integration-    │
       │    │         │      qiscsgo          │
       │    │         └───────────┬───────────┘
       │    │                     │
       ▼    ▼                     ▼
   ┌────────────┐  ┌──────────┐  ┌──────────┐
   │  mcgo-     │  │  mcgo-   │  │  mcgo-   │
   │  network   │  │ economy  │  │   data   │
   └─────┬──────┘  └────┬─────┘  └────┬─────┘
         │              │              │
         │ depends on   │              │
         │     ┌────────┴──────┬───────┘
         │     │               │
         ▼     ▼               ▼
      ┌─────────────┐    ┌──────────┐
      │  mcgo-api   │◄───│ mcgo-core│
      │    (API)    │    │  (Core)  │
      └─────────────┘    └──────────┘
              ▲                ▲
              │                │
              └────────┬───────┘
                       │
                ┌──────┴──────┐
                │  mcgo-bom   │
                │   (BOM)     │
                └─────────────┘
```

### 模块职责

#### mcgo-bom (Bill of Materials)
- **职责**: 集中版本管理
- **内容**: 依赖版本约束
- **依赖**: 无

#### mcgo-build-logic
- **职责**: 自定义 Gradle 约定插件
- **内容**: 代码风格、编译选项、测试约定
- **依赖**: Gradle API

#### mcgo-api
- **职责**: 公共 API 定义
- **内容**: 接口、SPI、DTO、常量、错误码
- **依赖**: 无（纯 Java）
- **包结构**:
  - `events/` - 事件定义
  - `commands/` - 命令接口
  - `dto/` - 数据传输对象
  - `spi/` - 服务提供接口
  - `errors/` - 错误码

#### mcgo-core
- **职责**: 纯 Java 领域实现
- **内容**: 比赛、地图、物品、规则、匹配、冷却、计分
- **依赖**: mcgo-api
- **包结构**:
  - `domain/` - 领域实体
  - `services/` - 领域服务
  - `policies/` - 业务策略
  - `usecase/` - 应用用例

#### mcgo-economy
- **职责**: 经济子域
- **内容**: 钱包、货币、定价、结算、奖励策略
- **依赖**: mcgo-api, mcgo-core
- **参考**: `docs/ECONOMY_BALANCE_ANALYSIS.md`

#### mcgo-network
- **职责**: 网络消息抽象
- **内容**: Message、Codec、Handler接口，消息DTO
- **依赖**: mcgo-api
- **包结构**:
  - `message/` - 消息定义
  - `codec/` - 编解码器
  - `handler/` - 处理器接口
  - `versioning/` - 版本协商

#### mcgo-data
- **职责**: 数据持久化适配器
- **内容**: Repository实现（NBT/JSON/SQLite）
- **依赖**: mcgo-api, mcgo-core
- **包结构**:
  - `repository/` - 仓储实现
  - `nbt/` - NBT适配器
  - `json/` - JSON适配器
  - `sql/` - SQL适配器
  - `uow/` - 工作单元

#### mcgo-platform-neoforge
- **职责**: 唯一平台绑定层
- **内容**: 注册、事件订阅、Tick、生命周期、数据包、命令桥接
- **依赖**: 所有其他模块
- **输出**: 最终mod JAR
- **包结构**:
  - `registry/` - 物品/方块注册
  - `bridge/` - 平台桥接
  - `listeners/` - 事件监听器
  - `bootstrap/` - 启动逻辑

#### mcgo-client
- **职责**: 客户端专属功能
- **内容**: 渲染、HUD、按键绑定、客户端配置
- **依赖**: mcgo-api, mcgo-network
- **包结构**:
  - `ui/` - 用户界面
  - `hud/` - HUD显示
  - `input/` - 输入处理
  - `assets/` - 资源管理

#### mcgo-server
- **职责**: 服务端专属功能
- **内容**: 服务器命令、权限、数据落地、服务端Tick
- **依赖**: mcgo-api, mcgo-core, mcgo-data, mcgo-economy
- **包结构**:
  - `commands/` - 命令实现
  - `auth/` - 权限鉴权
  - `scheduler/` - 调度器

#### mcgo-integration-qiscsgo
- **职责**: 外部 jar 集成适配
- **内容**: 接口适配器
- **依赖**: mcgo-api, mcgo-core
- **包结构**:
  - `adapters/` - 适配器
  - `mappers/` - 映射器

#### mcgo-test
- **职责**: 端到端和集成测试
- **内容**: 测试用例、基准测试
- **依赖**: 所有模块（test scope）

## 设计模式与实践

### 端口-适配器模式
- **端口**: 在 mcgo-api 中定义接口
- **适配器**: 在具体模块中实现
- **示例**: Repository 接口（端口）在 mcgo-data 中有 NBT、JSON、SQL 适配器

### 事件驱动架构
- **领域事件**: 在 mcgo-api 中定义
- **发布**: 核心模块发布领域事件
- **订阅**: 平台层监听并转换为平台事件

### 依赖倒置原则
- 高层模块不依赖低层模块
- 都依赖于抽象（接口）
- 抽象不依赖于细节

### 单一职责原则
- 每个模块有明确的单一职责
- 配置、业务逻辑、持久化分离

## 扩展点设计

### 在 mcgo-api 中定义的扩展点

1. **EconomyService** - 经济服务接口
2. **MatchService** - 比赛服务接口
3. **InventoryGateway** - 物品栏网关
4. **PermissionGateway** - 权限网关
5. **MessageBus** - 消息总线
6. **Clock** - 时钟抽象
7. **ConfigService** - 配置服务

### SPI 加载
使用 Java ServiceLoader 实现可插拔策略：
- 价格策略
- 掉落策略
- 匹配策略

## 配置管理

### 配置层次
1. **默认配置**: 各模块 `src/main/resources`
2. **运行时配置**: `run/config` 目录
3. **环境配置**: 开发/集成/发布

### 配置服务
- Schema 校验
- 默认值回填
- 热加载策略

## 数据流与通信

### 事件流
```
Platform Event → Event Listener → Domain Event → Event Bus → Domain Handler
```

### 命令流
```
User Input → Command Parser → Command Handler → Domain Service → Response
```

### 网络流
```
Client → Message DTO → Codec → Channel → Server
Server → Message DTO → Codec → Channel → Client
```

## 测试策略

### 测试金字塔

```
        /\
       /  \  E2E Tests (mcgo-test)
      /    \
     /------\  Integration Tests
    /        \
   /----------\  Component Tests
  /            \
 /--------------\ Unit Tests (mcgo-core, mcgo-economy, etc.)
```

### 测试类型
1. **单元测试**: mcgo-core 纯 Java 逻辑（高覆盖率）
2. **组件测试**: mcgo-data/mcgo-network 适配器
3. **集成测试**: mcgo-platform-neoforge + :run 场景
4. **端到端测试**: 关键用例和兼容性验证

### 测试夹具
- `testFixtures` 提供构建器
- 假实现（FakeClock、InMemoryRepository）

## 构建与发布

### 构建配置
- **版本管理**: `gradle/libs.versions.toml`
- **Java Toolchain**: Java 21
- **代码质量**: Xlint、ErrorProne、SpotBugs、Spotless

### 依赖约束
- 仅允许文档定义的依赖方向
- 使用 Gradle `api`/`implementation` 控制可见性

### 产物
- 仅 `mcgo-platform-neoforge` 生成可发布 JAR
- 其余模块为库（不独立发布）

## 迁移路线

### 阶段一：骨架建立（当前）
- ✅ 创建所有子模块目录
- ✅ 配置 Gradle 构建
- ✅ 建立版本管理
- 🔄 文档完善

### 阶段二：核心逻辑迁移
- 移动平台无关逻辑到 mcgo-core/mcgo-economy
- 通过桥接类保持兼容

### 阶段三：网络层抽取
- 抽出网络 DTO 到 mcgo-network
- 平台层仅保留通道注册

### 阶段四：数据层分离
- 拆出数据适配到 mcgo-data
- 引入 Repository 和 UnitOfWork

### 阶段五：客户端/服务端分离
- 分离客户端/服务端逻辑
- 验证功能等价性

### 阶段六：测试与质量门禁
- 完善测试覆盖
- 清理废弃代码

## 质量保障

### 静态分析
- Checkstyle
- PMD
- SpotBugs
- OWASP Dependency-Check

### 代码审查
- 所有改动需要 PR
- 至少一位审查者批准
- CI 检查通过

### 持续集成
- 自动化构建
- 自动化测试
- 质量门禁

## 参考资源

### 内部文档
- [快速开始指南](QUICK_START.md)
- [经济平衡分析](ECONOMY_BALANCE_ANALYSIS.md)
- [重构计划](../REFACTORING_PLAN.md)
- [变更日志](CHANGELOG.md)

### 设计原则
- 端口-适配器架构（Hexagonal Architecture）
- 领域驱动设计（Domain-Driven Design）
- SOLID 原则
- 清晰架构（Clean Architecture）

### 外部资源
- [NeoForge 文档](https://docs.neoforged.net/)
- [Gradle 用户手册](https://docs.gradle.org/)

---

**文档版本**: 1.0  
**最后更新**: 2024-10  
**维护者**: MCGO 团队
