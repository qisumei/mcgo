# MCGO 测试指南

## 概述

本项目现已建立完整的单元测试框架，遵循测试驱动开发(TDD)原则。

## 快速开始

### 运行所有测试
```bash
./gradlew test
```

### 运行特定测试类
```bash
./gradlew test --tests VirtualMoneyManagerTest
./gradlew test --tests WeaponRegistryTest
```

### 查看测试报告
测试完成后，报告位于：`build/reports/tests/test/index.html`

## 测试覆盖率

### 已完成测试 ✅

| 类名 | 测试数量 | 预计覆盖率 | 状态 |
|-----|---------|-----------|------|
| VirtualMoneyManager | 19 | ~95% | ✅ 完成 |
| WeaponRegistry | 14 | ~90% | ✅ 完成 |
| WeaponDefinition | 14 | ~85% | ✅ 完成 |
| EconomyManager | 基础测试 | ~40% | ⚠️ 部分完成 |

### 待测试类 📋

- PlayerService 实现类
- MatchPlayerService
- RoundEconomyService
- TeamSwapService
- WeaponFactory
- Match (需先拆分)

## 测试框架

- **JUnit 5.10.1**: 主测试框架
- **Mockito 5.8.0**: Mock框架
- **断言**: JUnit Assertions

## 测试结构

```
src/test/java/com/qisumei/csgo/
├── economy/
│   └── VirtualMoneyManagerTest.java
├── weapon/
│   ├── WeaponRegistryTest.java
│   └── WeaponDefinitionTest.java
└── game/
    └── EconomyManagerTest.java
```

## 编写测试的最佳实践

### 1. 测试命名
```java
@Test
@DisplayName("应该能通过Builder创建基本武器")
void testBasicWeaponCreation() {
    // 测试代码
}
```

### 2. AAA模式
```java
@Test
void testAddMoney() {
    // Arrange (准备)
    manager.setMoney(mockPlayer, 500);
    
    // Act (执行)
    manager.addMoney(mockPlayer, 300);
    
    // Assert (断言)
    assertEquals(800, manager.getMoney(mockPlayer));
}
```

### 3. 测试隔离
```java
@BeforeEach
void setUp() {
    // 每个测试前重置状态
    manager.clearAll();
}
```

### 4. Mock使用
```java
@Mock
private ServerPlayer mockPlayer;

@BeforeEach
void setUp() {
    MockitoAnnotations.openMocks(this);
    when(mockPlayer.getUUID()).thenReturn(testUUID);
}
```

## 测试类型

### 单元测试
- 测试单个类的功能
- 使用Mock隔离依赖
- 快速执行

### 集成测试
- 测试多个组件交互
- 使用真实依赖
- 在实际游戏环境运行

## 常见测试场景

### 测试异常情况
```java
@Test
@DisplayName("传入null应该抛出异常")
void testNullThrowsException() {
    assertThrows(NullPointerException.class, 
        () -> manager.getMoney(null));
}
```

### 测试边界条件
```java
@Test
@DisplayName("设置负数应该被限制为0")
void testNegativeValue() {
    manager.setMoney(mockPlayer, -100);
    assertEquals(0, manager.getMoney(mockPlayer));
}
```

### 测试并发安全
```java
@Test
@DisplayName("并发操作应该线程安全")
void testConcurrency() throws InterruptedException {
    // 创建多线程并发操作
    // 验证最终结果正确
}
```

## CI/CD集成

测试会在以下时机自动运行：
- Pull Request提交时
- 代码合并前
- 定期构建

## 故障排查

### 测试失败
1. 检查错误消息和堆栈
2. 确认测试数据设置正确
3. 验证Mock配置
4. 使用调试器单步执行

### 测试不稳定
1. 检查共享状态
2. 确保测试隔离
3. 避免依赖执行顺序
4. 检查时间相关逻辑

## 资源

- [JUnit 5文档](https://junit.org/junit5/docs/current/user-guide/)
- [Mockito文档](https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html)
- [项目测试README](src/test/java/README.md)
- [重构计划](docs/REFACTORING_PLAN.md)

## 贡献

添加新测试时请：
1. 遵循现有命名和结构规范
2. 使用中文DisplayName提高可读性
3. 包含正常和异常情况测试
4. 更新本文档
5. 确保所有测试通过

---

**维护**: MCGO开发团队
**更新**: 2025-10-25
