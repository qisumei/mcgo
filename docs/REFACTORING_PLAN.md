# 项目重构计划

> **注意**: 本文档已整合到 [架构文档](ARCHITECTURE.md)。这里仅保留重构的路线图和优先级规划。

## 当前状态

✅ **已完成的改进**:
- 删除已废弃的 MatchPlayerHelper.java
- 整理项目文档结构
- 创建统一的架构文档
- 建立单元测试框架 (JUnit 5 + Mockito)
- 为核心类添加47个单元测试
- 创建测试文档和最佳实践指南

## 待完成的重构任务

### P0 - 高优先级（建议下个版本）

暂无紧急任务。当前架构已基本稳定。

### P1 - 中优先级（功能稳定后）

#### 1. 简化服务层抽象
**问题**: Service 层（EconomyService, MatchService）只是对现有管理器的简单包装，增加了复杂度。

**方案选项**:
- **选项A**: 保留 ServiceRegistry，移除具体的 Service 接口
- **选项B**: 完全移除 ServiceRegistry，回归静态工具类
- **选项C**: 保持现状，在新功能中逐步迁移到服务模式

**推荐**: 选项C（保持现状），除非有明确需求变更。

**预计工作量**: 2-3天（如果选择选项A或B）

#### 2. 拆分 Match.java
**问题**: Match.java 有 1,283 行代码，职责过多。

**拆分建议**:
```
Match.java (核心协调) ~400行
├── MatchRoundManager.java (回合管理) ~300行
├── MatchPlayerManager.java (玩家管理) ~200行
├── MatchEconomyCoordinator.java (经济协调) ~200行
└── MatchStateManager.java (状态管理) ~183行
```

**风险**: 高 - 需要充分的回归测试
**预计工作量**: 3-5天

#### 3. 优化 ShopGUI.java 和 CommandHandlers.java
**问题**: 
- ShopGUI.java: 484行，复杂度高
- CommandHandlers.java: 498行，命令处理逻辑混杂

**方案**:
- ShopGUI: 拆分为多个小组件
- CommandHandlers: 每个命令一个处理器类

**预计工作量**: 2-3天

### P2 - 低优先级（长期改进）

#### 4. 添加单元测试
**目标**: 核心类的测试覆盖率 > 80%

**优先测试**:
- ✅ VirtualMoneyManager (完成 - 19个测试, ~95%覆盖率)
- ✅ WeaponRegistry (完成 - 14个测试, ~90%覆盖率)
- ✅ WeaponDefinition (完成 - 14个测试, ~85%覆盖率)
- ✅ EconomyManager (部分完成 - 基础逻辑测试)
- ✅ MatchPlayerService (完成 - 2个测试, ~10%覆盖率，极度受Minecraft依赖限制)
- ⏳ RoundEconomyService (待完成)
- ⏳ TeamSwapService (待完成)

**已完成工作** (2025-10-25):
- 建立JUnit 5 + Mockito测试框架
- 创建49个单元测试（包括2个新的MatchPlayerService测试）
- 编写测试文档 (src/test/java/README.md)
- 配置Gradle测试任务
- 为MatchPlayerService添加构造函数验证和接口实现测试

**预计工作量**: 持续进行

**测试最佳实践**:
- 遵循AAA模式（Arrange-Act-Assert）
- 使用中文DisplayName提高可读性
- 测试正常情况、边界值和异常情况
- 每个测试独立运行，使用@BeforeEach清理状态
- 避免依赖Minecraft的测试，聚焦纯Java逻辑
- 需要Minecraft环境的测试应在集成测试中进行

**测试文档**:
- [测试指南](../TESTING.md) - 如何运行和编写测试
- [测试README](../src/test/java/README.md) - 详细的测试说明和示例

#### 5. 性能优化
- 减少 MatchManager.tick() 的开销
- 优化事件处理
- 考虑引入缓存机制

**预计工作量**: 视具体优化内容而定

## 实施原则

### 1. 渐进式重构
- **不要一次性大规模重构**
- 每次重构一个模块
- 确保每次重构后系统仍然可用

### 2. 测试先行
- 重构前编写测试
- 重构后确保测试通过
- 必要时添加新测试

### 3. 文档同步
- 代码变更后立即更新文档
- 保持架构文档的准确性
- 更新相关的使用指南

### 4. 向后兼容
- 尽量保持 API 兼容性
- 废弃 API 时提供过渡期
- 提供迁移指南

## 决策记录

### DR-001: 保留 ServiceRegistry
**日期**: 2025-10-25
**决策**: 保留 ServiceRegistry 框架
**理由**: 
- 已被广泛使用（66处引用）
- 为未来扩展保留灵活性
- 移除成本高于收益

### DR-002: 删除 MatchPlayerHelper
**日期**: 2025-10-25
**决策**: 将逻辑合并到 MatchPlayerService，删除 Helper 类
**理由**:
- 已标记为 @Deprecated
- 只有一个实现类使用
- 减少技术债务

### DR-003: 使用手动Mock而非Mockito
**日期**: 2025-10-25
**决策**: 测试中使用手动Mock实现，不依赖Mockito框架
**理由**:
- Minecraft类在标准测试环境中不可用
- 即使Mock也无法加载依赖NeoForge的类
- 手动Mock更轻量、更灵活、更易于理解
- 测试聚焦于可验证的逻辑（参数验证、接口实现）

### DR-004: 测试策略分层
**日期**: 2025-10-25
**决策**: 分层测试策略 - 单元测试（不依赖Minecraft）+ 集成测试（Minecraft环境）
**理由**:
- 单元测试快速反馈，覆盖纯Java逻辑
- 集成测试覆盖完整业务逻辑
- 明确区分可测试和需要集成测试的部分
- 避免在无法Mock的场景下浪费时间

## 参考文档

- [架构文档](ARCHITECTURE.md) - 完整的系统架构说明
- [测试指南](../TESTING.md) - 如何运行和编写测试
- [测试详细文档](../src/test/java/README.md) - 测试框架和最佳实践
- [README.md](../README.md) - 项目概述
- [CHANGELOG.md](CHANGELOG.md) - 更新历史
- [武器系统文档](WEAPON_SYSTEM_REFACTOR.md) - 武器系统重构详解
- [如何添加武器](HOW_TO_ADD_WEAPONS.md) - 添加新武器的快速指南

## 更新历史

| 日期 | 变更 | 负责人 |
|------|------|--------|
| 2025-10-25 | 添加MatchPlayerService测试，更新测试统计，新增决策记录 | - |
| 2025-10-25 | 简化重构计划，整合到架构文档 | - |
| 2024-10 | 初始版本 | - |

---

**注意**: 重构是持续的过程，本计划会根据项目需求和反馈不断调整。
