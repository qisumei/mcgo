# 快速开始指南

## 构建项目

### 前置要求
- Java 21 或更高版本
- 网络连接（用于下载依赖）

### 构建命令
```bash
# 克隆项目
git clone https://github.com/qisumei/mcgo.git
cd mcgo

# 构建项目
./gradlew build

# 运行服务器
./gradlew runServer

# 运行客户端
./gradlew runClient
```

## 配置换边资金策略

编辑 `config/mcgo-server.toml` 文件：

```toml
[Economy]
# 换边时的资金清空策略
# 可选: RESET_TO_PISTOL_ROUND, CLEAR_ALL, CLEAR_TEMPORARY_ONLY, KEEP_ALL
teamSwapMoneyStrategy = "RESET_TO_PISTOL_ROUND"
```

### 策略说明

| 策略 | 行为 | 适用场景 |
|-----|------|---------|
| RESET_TO_PISTOL_ROUND | 重置为手枪局起始资金 | 竞技模式（推荐） |
| CLEAR_ALL | 清空所有资金 | 特殊比赛模式 |
| CLEAR_TEMPORARY_ONLY | 保留基础资金，清空超出部分 | 休闲模式 |
| KEEP_ALL | 保留所有资金 | 测试或特殊玩法 |

## 经济平衡调整（可选）

如果想要更紧张的经济体验，可以调整以下配置：

```toml
[Economy]
# 推荐的紧张经济配置（1/100 缩放）
pistolRoundStartingMoney = 8      # 原: 800
winReward = 32                    # 原: 3250
lossReward = 14                   # 原: 1400
lossStreakBonus = 5               # 原: 500
maxLossStreakBonus = 34           # 原: 3400

# 击杀奖励保持不变
killRewardKnife = 15
killRewardSmg = 6
killRewardPistol = 3
killRewardRifle = 3
killRewardAwp = 1
```

详细分析见：[经济平衡性分析](ECONOMY_BALANCE_ANALYSIS.md)

## 开发扩展

### 添加新的换边处理逻辑

创建新的事件监听器：

```java
package com.qisumei.csgo.events.match;

public class CustomHandler implements MatchEventListener {
    @Override
    public void onTeamSwap(TeamSwapEvent event) {
        // 你的自定义逻辑
        event.getAffectedPlayers().forEach((uuid, player) -> {
            // 处理每个玩家
        });
    }
}
```

在 Match 构造器中注册：

```java
// 在 Match.java 的构造器中
eventBus.registerListener(new CustomHandler());
```

### 添加新的回合事件

实现 `MatchEventListener` 接口：

```java
public class MyHandler implements MatchEventListener {
    @Override
    public void onRoundStart(RoundStartEvent event) {
        // 回合开始时的逻辑
    }
}
```

## 测试

### 运行单元测试
```bash
# 运行所有测试
./gradlew test

# 运行特定测试类
./gradlew test --tests WeaponRegistryTest
./gradlew test --tests MatchPlayerServiceTest

# 持续测试模式（文件改变时自动运行）
./gradlew test --continuous

# 查看测试报告
open build/reports/tests/test/index.html  # macOS
xdg-open build/reports/tests/test/index.html  # Linux
start build/reports/tests/test/index.html  # Windows
```

### 测试覆盖率

当前测试状态：

| 组件 | 测试数 | 覆盖率 | 说明 |
|-----|--------|--------|------|
| WeaponRegistry | 14 | ~90% | ✅ 完整覆盖 |
| WeaponDefinition | 14 | ~85% | ✅ 完整覆盖 |
| MatchPlayerService | 2 | ~10% | ⚠️ 构造函数验证 |
| VirtualMoneyManager | 3 | ~20% | ⚠️ 基础功能 |

**总计**: 49个单元测试

### 编写测试

遵循AAA模式和中文DisplayName：

```java
@Test
@DisplayName("应该正确注册新武器")
void testRegisterWeapon() {
    // Arrange - 准备
    WeaponDefinition weapon = new WeaponDefinition.Builder(...)
        .price(30)
        .build();
    
    // Act - 执行
    WeaponRegistry.register(weapon);
    
    // Assert - 验证
    assertTrue(WeaponRegistry.getWeapon("test:weapon").isPresent());
}
```

详细指南：
- [测试指南](../TESTING.md) - 基础测试文档
- [测试策略](TESTING_STRATEGY.md) - 详细的测试策略和最佳实践
- [测试详细文档](../src/test/java/README.md) - 测试框架使用

### 手动测试清单

#### 换边功能
- [ ] 换边时比分正确交换
- [ ] 玩家队伍正确切换
- [ ] 资金按策略正确处理
- [ ] 玩家收到正确通知

#### 经济系统
- [ ] 手枪局起始资金正确
- [ ] 胜利奖励正确发放
- [ ] 失败奖励正确计算
- [ ] 连败奖励正确累加
- [ ] 击杀奖励正确发放

## 故障排除

### 构建失败
```
错误: Could not resolve net.neoforged:neoform-runtime
```
**解决方案**: 确保网络连接正常，可以访问 maven.neoforged.net

### 配置不生效
**解决方案**: 
1. 检查配置文件路径是否正确
2. 确保配置值拼写正确（区分大小写）
3. 重启服务器使配置生效

### 事件未触发
**解决方案**:
1. 检查监听器是否正确注册
2. 查看日志确认事件是否被触发
3. 确认监听器方法签名正确

## 性能优化建议

1. **事件监听器**
   - 避免在监听器中执行耗时操作
   - 已实现：所有事件处理都包含异常捕获和详细日志
   - 优化：减少不必要的计算和循环

2. **经济计算**
   - 已优化：使用服务类集中处理
   - 避免频繁查询数据库
   - 使用缓存机制提升性能

3. **内存管理**
   - 事件对象会被自动回收
   - 使用 CopyOnWriteArrayList 优化并发
   - 及时清理不再使用的资源

4. **观战系统优化**
   - 减少视角切换频率，提升流畅度
   - 隐藏观战者信息，避免战术泄露
   - 只在必要时更新观战目标

## 更多文档

### 核心文档
- [项目架构](ARCHITECTURE.md) - 完整的系统架构说明
- [重构计划](REFACTORING_PLAN.md) - 技术债务和改进计划

### 功能文档
- [经济平衡性分析](ECONOMY_BALANCE_ANALYSIS.md) - 详细的经济系统分析
- [武器系统重构](WEAPON_SYSTEM_REFACTOR.md) - 武器系统技术细节
- [如何添加武器](HOW_TO_ADD_WEAPONS.md) - 添加新武器的快速指南

### 测试文档
- [测试指南](../TESTING.md) - 基础测试运行和编写
- [测试策略](TESTING_STRATEGY.md) - 详细的测试策略和最佳实践
- [测试详细文档](../src/test/java/README.md) - 测试框架使用指南

### 变更记录
- [更新日志](CHANGELOG.md) - 项目变更历史

## 获取帮助

- 查看源码注释
- 阅读相关文档
- 在 GitHub 上提交 Issue

---

**版本**: 1.0  
**最后更新**: 2024年
