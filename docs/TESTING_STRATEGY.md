# MCGO 测试策略文档

## 概述

本文档详细说明MCGO项目的测试策略、实施方法和最佳实践。

## 测试金字塔

MCGO项目采用分层测试策略：

```
        /\
       /  \  集成测试（未来）
      /    \  - 在Minecraft环境中运行
     /------\  - 测试完整业务逻辑
    /        \
   /          \ 单元测试（当前）
  /            \ - 快速、独立
 /______________\ - 聚焦纯Java逻辑
```

### 测试层次

#### 1. 单元测试（Unit Tests）- 当前重点
**目标**: 快速验证单个组件的正确性

**特点**:
- ✅ 执行快速（毫秒级）
- ✅ 完全隔离，无外部依赖
- ✅ 易于调试和维护
- ⚠️ 无法测试依赖Minecraft的逻辑

**覆盖范围**:
- 纯Java逻辑（不依赖Minecraft）
- 参数验证和边界检查
- 接口实现和契约验证
- 数据结构操作

**已实现** (57个测试):
- WeaponRegistry (14) - 武器注册和查询
- WeaponDefinition (14) - 武器定义和Builder
- MatchPlayerService (10) - 参数验证
- VirtualMoneyManager (3) - 基础操作

#### 2. 集成测试（Integration Tests）- 未来计划
**目标**: 验证组件间协作的正确性

**特点**:
- 需要Minecraft游戏环境
- 测试真实的对象交互
- 覆盖完整的业务流程
- 执行较慢，但更接近实际使用

**待实现场景**:
- 比赛完整流程（创建→开始→进行→结束）
- 商店购买流程（打开→选择→购买→装备）
- C4系统（安放→倒计时→拆除/爆炸）
- 经济系统（分配→消费→奖励）
- 玩家服务（清空→发放→捕获）

#### 3. 端到端测试（E2E Tests）- 长期规划
**目标**: 模拟真实玩家使用场景

**特点**:
- 完整的游戏服务器环境
- 多玩家协作场景
- 验证游戏体验
- 执行最慢，但最真实

## Minecraft依赖问题

### 问题描述

Minecraft模组开发面临特殊的测试挑战：

1. **Minecraft核心类不可用**: ServerPlayer, ItemStack, Level等类在标准JUnit环境中无法实例化
2. **NeoForge类加载失败**: ServerConfig等依赖ModConfigSpec的类在加载时就会失败
3. **静态初始化问题**: 某些类的静态初始化块会触发Minecraft代码

### 解决方案

#### 方案A: 手动Mock（当前采用）
```java
// 为我们自己的接口创建简单Mock
ServerCommandExecutor mockExecutor = new ServerCommandExecutor() {
    @Override
    public void executeGlobal(String command) {
        // Mock实现
    }
    
    @Override
    public void executeForPlayer(ServerPlayer player, String command) {
        // Mock实现
    }
};
```

**优点**:
- 简单、轻量
- 完全控制Mock行为
- 无需额外依赖

**缺点**:
- 无法Mock Minecraft类
- 代码略显冗长

#### 方案B: 集成测试框架（未来考虑）
使用专门的Minecraft测试框架，如：
- NeoForge GameTest Framework
- Architectury Test Mod

**优点**:
- 可以测试真实Minecraft逻辑
- 官方支持

**缺点**:
- 执行慢
- 环境配置复杂
- 调试困难

## 测试编写指南

### 1. 选择合适的测试类型

**规则**: 能用单元测试就用单元测试

```java
// ✅ 好 - 纯Java逻辑，适合单元测试
@Test
void testWeaponPriceCalculation() {
    WeaponDefinition weapon = new WeaponDefinition.Builder(...)
        .price(30)
        .build();
    assertEquals(30, weapon.getPrice());
}

// ❌ 坏 - 依赖ServerPlayer，需要集成测试
@Test
void testGivePlayerWeapon() {
    ServerPlayer player = ...; // 无法在单元测试中创建！
    service.giveWeapon(player, weapon);
}
```

### 2. AAA模式

遵循Arrange-Act-Assert模式：

```java
@Test
@DisplayName("应该正确添加武器到注册表")
void testRegisterWeapon() {
    // Arrange - 准备测试数据
    WeaponDefinition weapon = new WeaponDefinition.Builder(...)
        .build();
    
    // Act - 执行被测试的操作
    WeaponRegistry.register(weapon);
    
    // Assert - 验证结果
    assertTrue(WeaponRegistry.getWeapon("test:weapon").isPresent());
}
```

### 3. 测试命名规范

使用描述性的中文DisplayName：

```java
@Test
@DisplayName("构造函数应该拒绝null参数")
void testConstructorRejectsNull() {
    assertThrows(NullPointerException.class, 
        () -> new MatchPlayerService(null));
}
```

### 4. 测试隔离

每个测试独立运行，互不影响：

```java
@BeforeEach
void setUp() {
    // 在每个测试前重置状态
    WeaponRegistry.clearAll();
    WeaponRegistry.initialize();
}

@Test
void testA() {
    // 测试A的逻辑
}

@Test
void testB() {
    // 测试B独立于测试A
}
```

### 5. 边界测试

测试正常情况、边界值和异常情况：

```java
@Test
@DisplayName("应该处理各种价格值")
void testPriceHandling() {
    // 正常情况
    assertEquals(30, new WeaponDefinition.Builder(...).price(30).build().getPrice());
    
    // 边界值
    assertEquals(0, new WeaponDefinition.Builder(...).price(0).build().getPrice());
    assertEquals(Integer.MAX_VALUE, new WeaponDefinition.Builder(...).price(Integer.MAX_VALUE).build().getPrice());
    
    // 异常情况
    assertThrows(IllegalArgumentException.class,
        () -> new WeaponDefinition.Builder(...).price(-1).build());
}
```

## 测试覆盖率目标

### 当前状态 (2025-10-25)

| 组件类型 | 目标覆盖率 | 当前覆盖率 | 状态 |
|---------|-----------|-----------|------|
| 纯Java类（武器系统） | >80% | ~85% | ✅ 达标 |
| 服务类（参数验证） | >30% | ~30% | ✅ 达标 |
| 依赖Minecraft的类 | - | ~20% | ⚠️ 受限 |

### 优先级

**P0 - 必须测试**:
- 核心业务逻辑类
- 数据结构和算法
- 公共API接口

**P1 - 应该测试**:
- 工具类和辅助函数
- 参数验证逻辑
- 错误处理路径

**P2 - 可以测试**:
- UI相关代码
- 配置加载
- 日志记录

## 持续改进

### 短期目标 (1-2周)
- [ ] 为RoundEconomyService添加单元测试
- [ ] 为TeamSwapService添加单元测试
- [ ] 提高WeaponFactory测试覆盖率

### 中期目标 (1-2月)
- [ ] 建立GameTest集成测试框架
- [ ] 实现关键流程的集成测试
- [ ] 达到整体>60%测试覆盖率

### 长期目标 (3-6月)
- [ ] 完整的端到端测试套件
- [ ] 自动化回归测试
- [ ] 性能基准测试

## 常见问题

### Q: 为什么不能测试某些类？
A: 这些类依赖Minecraft核心组件，无法在标准JUnit环境中实例化。需要在实际游戏环境中进行集成测试。

### Q: 如何判断是否可以编写单元测试？
A: 检查类的依赖：
- ✅ 只依赖标准Java库和我们的接口 → 可以单元测试
- ⚠️ 依赖Minecraft类作为参数 → 只能测试参数验证
- ❌ 内部逻辑依赖Minecraft → 需要集成测试

### Q: Mock和真实对象如何选择？
A: 
- 我们的接口 → 使用手动Mock
- Minecraft类 → 无法Mock，需要集成测试
- 第三方库 → 根据情况决定

### Q: 测试失败怎么办？
A:
1. 检查测试本身是否正确
2. 查看错误消息和堆栈跟踪
3. 使用调试器单步执行
4. 检查是否有状态污染
5. 确认测试隔离性

## 相关资源

- [JUnit 5文档](https://junit.org/junit5/docs/current/user-guide/)
- [测试指南](../TESTING.md)
- [测试详细文档](../src/test/java/README.md)
- [重构计划](REFACTORING_PLAN.md)
- [架构文档](ARCHITECTURE.md)

---

**维护者**: MCGO开发团队  
**最后更新**: 2025-10-25
