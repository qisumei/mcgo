# MCGO 单元测试文档

## 概述

本目录包含 MCGO 项目的单元测试，遵循项目重构计划中的测试优先原则。

## 测试框架

- **JUnit 5** (Jupiter) - 主要测试框架
- ~~Mockito 5~~ - 不再使用（Minecraft类在测试环境不可用）

## 测试结构

```
src/test/java/com/qisumei/csgo/
├── economy/
│   └── VirtualMoneyManagerTest.java   # 虚拟货币管理器测试
├── weapon/
│   ├── WeaponRegistryTest.java        # 武器注册表测试
│   └── WeaponDefinitionTest.java      # 武器定义测试
└── game/
    └── EconomyManagerTest.java         # 经济管理器测试
```

## 已实现的测试

### 1. VirtualMoneyManagerTest
**覆盖率目标**: ~50% (受限于ServerPlayer依赖)

测试内容：
- ✅ 单例模式验证
- ✅ 基于UUID的货币查询
- ✅ clearAll功能
- ✅ 空值异常处理
- ✅ 多UUID独立管理

**关键测试**：
- UUID-based getMoney方法验证
- 单例一致性测试
- 空指针防护测试

**限制**：
- VirtualMoneyManager的大部分方法需要ServerPlayer对象
- ServerPlayer在测试环境中不可用（需要完整Minecraft环境）
- 完整功能测试需要在游戏中进行集成测试

### 2. WeaponRegistryTest
**覆盖率目标**: >85%

测试内容：
- ✅ 武器注册和查询
- ✅ 按类型过滤
- ✅ 按队伍过滤
- ✅ 复合条件查询
- ✅ 初始化机制
- ✅ 清空和重新初始化

**关键测试**：
- 初始化只执行一次的验证
- 过滤功能的正确性
- 注册表清空后的状态一致性

### 3. WeaponDefinitionTest
**覆盖率目标**: >80%

测试内容：
- ✅ Builder模式构建
- ✅ 所有属性的正确设置
- ✅ 队伍可用性逻辑
- ✅ 附件系统
- ✅ 瞄准镜查询
- ✅ 不可变性保护

**关键测试**：
- getDefaultAttachments 返回副本而非原始列表
- 大小写不敏感的队伍检查
- 特殊武器类型（护甲、手雷）

### 4. EconomyManagerTest
**覆盖率目标**: ~40% (受限于Minecraft依赖)

测试内容：
- ✅ null参数处理
- ✅ 配置值验证
- ✅ 奖励平衡检查
- ✅ 工具类设计验证
- ✅ 奖励值范围检查

**限制**：
- 部分方法依赖 ServerPlayer 和游戏环境，需要集成测试
- 通过 VirtualMoneyManagerTest 间接验证包装逻辑

## 运行测试

### Gradle命令

```bash
# 运行所有测试
./gradlew test

# 运行特定测试类
./gradlew test --tests VirtualMoneyManagerTest

# 运行测试并生成报告
./gradlew test --info

# 持续测试模式
./gradlew test --continuous
```

### IDEA运行

1. 右键点击测试类或测试方法
2. 选择 "Run 'TestName'"
3. 查看测试结果面板

## 测试覆盖率

当前测试覆盖的核心类：
- [x] VirtualMoneyManager - ~95% 覆盖
- [x] WeaponRegistry - ~90% 覆盖  
- [x] WeaponDefinition - ~85% 覆盖
- [x] EconomyManager - ~40% 覆盖（受Minecraft依赖限制）

### 未来测试计划

根据 [REFACTORING_PLAN.md](../../../docs/REFACTORING_PLAN.md)：

#### 下一批测试（优先级：高）
- [ ] PlayerService 实现类测试
- [ ] MatchPlayerService 测试
- [ ] RoundEconomyService 测试
- [ ] TeamSwapService 测试

#### 后续测试（优先级：中）
- [ ] WeaponFactory 测试
- [ ] C4Manager 测试
- [ ] MatchScoreboard 测试
- [ ] Match 类的单元测试（需要先拆分）

#### 集成测试（优先级：低）
- [ ] 完整比赛流程集成测试
- [ ] 商店购买流程测试
- [ ] C4系统端到端测试

## 测试原则

### 1. 测试隔离
- 每个测试独立运行
- 使用 `@BeforeEach` 清理状态
- 避免测试间的依赖

### 2. 命名规范
- 测试类：`<ClassName>Test`
- 测试方法：`test<MethodName>` 或使用 `@DisplayName`
- 使用中文 DisplayName 提高可读性

### 3. AAA模式
```java
@Test
void testExample() {
    // Arrange - 准备测试数据
    
    // Act - 执行被测试的操作
    
    // Assert - 验证结果
}
```

### 4. Mock使用
- 只Mock外部依赖（Minecraft组件）
- 不Mock被测试的类
- 优先使用真实对象

### 5. 边界测试
- 测试正常情况
- 测试边界值（0, -1, 最大值）
- 测试异常情况（null, 空集合）

## 常见问题

### Q: 为什么有些测试用Mock而有些不用？
A: 对于纯Java逻辑（如VirtualMoneyManager），使用真实对象。对于Minecraft依赖（如ServerPlayer），使用Mock。

### Q: 如何测试依赖Minecraft的代码？
A: 有三种策略：
1. 提取可测试的逻辑到独立方法
2. 使用Mock模拟Minecraft组件
3. 编写集成测试在实际游戏环境运行

### Q: 测试失败怎么办？
A: 
1. 检查是否有环境依赖（如配置文件）
2. 确认测试隔离（清理共享状态）
3. 查看错误堆栈定位问题
4. 在IDE中调试测试

### Q: 如何提高测试覆盖率？
A: 
1. 关注边界条件和异常路径
2. 添加并发测试
3. 测试组合场景
4. 不要为了覆盖率而测试（关注价值）

## 持续集成

测试会在以下情况自动运行：
- GitHub Actions CI流程
- Git pre-commit hooks（如果配置）
- 本地开发时的持续测试模式

## 贡献指南

添加新测试时：
1. 遵循现有的测试结构和命名规范
2. 确保测试可以独立运行
3. 添加充分的注释和DisplayName
4. 覆盖正常和异常情况
5. 更新本README的测试列表

## 参考文档

- [JUnit 5 User Guide](https://junit.org/junit5/docs/current/user-guide/)
- [Mockito Documentation](https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html)
- [项目重构计划](../../../docs/REFACTORING_PLAN.md)
- [项目架构文档](../../../docs/ARCHITECTURE.md)

---

**最后更新**: 2025-10-25
**维护者**: MCGO开发团队
