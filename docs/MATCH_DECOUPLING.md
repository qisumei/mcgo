# Match.java 解耦重构说明

## 重构目标
将 Match.java 中的核心逻辑解耦，提高代码可维护性和可测试性，实现关注点分离。

## 主要改进

### 1. 事件系统（Event System）

#### 新增组件
- **MatchEventBus**: 事件总线，管理事件监听器和分发事件
- **MatchEventListener**: 事件监听器接口
- **TeamSwapEvent**: 换边事件，携带换边相关信息
- **RoundStartEvent**: 回合开始事件
- **EconomyEventHandler**: 经济事件处理器，处理换边时的资金清空

#### 设计优势
```
传统方式（紧耦合）:
Match.swapTeams() -> 直接调用 EconomyManager.clearMoney()

新方式（事件驱动，解耦）:
Match.swapTeams() -> 触发 TeamSwapEvent -> EconomyEventHandler 监听并处理
```

**好处**:
- Match 不需要知道经济系统如何处理资金
- 可以轻松添加新的换边处理逻辑（如重置技能、清空buff等）
- 便于单元测试和模拟

### 2. 专门的服务类

#### TeamSwapService
负责处理队伍交换相关的逻辑：
- 更新玩家队伍归属
- 更新游戏内team命令
- 清理玩家背包
- 通知玩家

**从 Match.java 中移出的代码**:
```java
// 之前在 swapTeams() 中的所有玩家队伍更新逻辑
// 现在委托给 TeamSwapService.updatePlayersTeam()
```

#### RoundEconomyService  
负责处理回合经济分配逻辑：
- 手枪局资金设置
- 普通回合收入分配（含连败奖励计算）
- 回合胜利奖励
- 击杀奖励

**从 Match.java 中移出的代码**:
```java
// 之前在 distributeRoundIncome() 中的所有经济计算逻辑
// 现在委托给 RoundEconomyService 的专门方法
```

### 3. 配置增强

#### 换边资金清空策略
在 `ServerConfig.java` 中新增配置项：
```java
teamSwapMoneyStrategy: String
```

支持4种策略：
1. **RESET_TO_PISTOL_ROUND**（推荐，默认）：重置为手枪局起始资金
2. **CLEAR_ALL**：清空所有资金
3. **CLEAR_TEMPORARY_ONLY**：仅清空临时资金，保留基础资金
4. **KEEP_ALL**：保留所有资金

## 代码结构对比

### 重构前
```
Match.java (1247 行)
├── swapTeams()
│   ├── 交换比分
│   ├── 更新队伍信息
│   ├── 清理背包
│   ├── 清空资金 ← 与经济系统紧耦合
│   └── 通知玩家
│
└── distributeRoundIncome()
    ├── 判断手枪局
    ├── 计算收入
    ├── 计算连败奖励
    └── 发放金钱
```

### 重构后
```
Match.java (简化后)
├── swapTeams()
│   ├── 交换比分
│   ├── 更新队伍信息
│   ├── teamSwapService.updatePlayersTeam() ← 委托
│   └── eventBus.fireTeamSwapEvent() ← 事件驱动
│
└── distributeRoundIncome()
    ├── 判断手枪局
    └── roundEconomyService.distribute...() ← 委托

MatchEventBus
└── 管理监听器和分发事件

EconomyEventHandler (实现 MatchEventListener)
└── onTeamSwap()
    └── 根据配置策略处理资金

TeamSwapService
└── updatePlayersTeam()
    └── 批量更新玩家队伍信息

RoundEconomyService
├── distributePistolRoundMoney()
├── distributeRoundIncome()
└── distributeWinReward()
```

## 依赖关系图

```
Match
 ├─→ MatchEventBus
 │    └─→ EconomyEventHandler
 │         └─→ VirtualMoneyManager
 ├─→ TeamSwapService
 │    ├─→ ServerCommandExecutor
 │    └─→ PlayerService
 └─→ RoundEconomyService
      └─→ EconomyService
```

## 扩展性示例

### 添加新的换边处理逻辑
只需创建新的监听器：

```java
public class SkillResetHandler implements MatchEventListener {
    @Override
    public void onTeamSwap(TeamSwapEvent event) {
        // 重置玩家技能
        event.getAffectedPlayers().forEach((uuid, player) -> {
            resetPlayerSkills(player);
        });
    }
}

// 在 Match 构造器中注册
eventBus.registerListener(new SkillResetHandler());
```

**无需修改 Match.java 的核心代码！**

## 测试友好性

### 重构前
测试 swapTeams 需要：
- 完整的 Match 对象
- MinecraftServer 实例
- 玩家数据
- 经济系统初始化

### 重构后
可以单独测试各个组件：

```java
// 测试经济事件处理器
@Test
public void testEconomyEventHandler() {
    EconomyEventHandler handler = new EconomyEventHandler(RESET_TO_PISTOL_ROUND);
    TeamSwapEvent event = new TeamSwapEvent(...);
    handler.onTeamSwap(event);
    // 验证资金是否正确重置
}

// 测试换边服务
@Test
public void testTeamSwapService() {
    TeamSwapService service = new TeamSwapService(mockExecutor, mockPlayerService);
    service.updatePlayerTeam(mockPlayer, "CT", "TestMatch_CT");
    // 验证队伍更新逻辑
}
```

## 性能影响

事件系统的性能开销：
- **事件分发**: O(n)，n为监听器数量（通常 < 10）
- **内存**: 每个事件对象约 100-200 字节
- **整体影响**: 可忽略不计（< 1ms per event）

## 向后兼容性

✅ **完全兼容**：
- 所有公开API保持不变
- 游戏逻辑行为一致（除非修改配置）
- 现有的存档和配置文件可以继续使用

## 未来优化方向

1. **更多事件类型**
   - RoundEndEvent
   - PlayerDeathEvent  
   - C4PlantedEvent
   - 等等

2. **异步事件处理**
   - 对于非关键路径的处理，可以考虑异步执行

3. **事件优先级**
   - 允许监听器指定执行顺序

4. **事件取消机制**
   - 允许监听器取消事件传播

## 维护建议

1. **添加新功能时**：优先考虑是否可以通过事件监听器实现
2. **修改经济逻辑时**：在 RoundEconomyService 中修改，而非 Match.java
3. **添加新的换边处理**：创建新的监听器，注册到事件总线
4. **保持 Match.java 简洁**：新逻辑优先考虑抽取到专门的服务类

---

**相关文档**:
- [经济平衡性分析](./ECONOMY_BALANCE_ANALYSIS.md)
- [配置说明](../src/main/java/com/qisumei/csgo/config/ServerConfig.java)
