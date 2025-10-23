# 项目重构计划

## 概述

本文档规划了 MCGO 项目的架构重构方案，旨在提高代码质量、降低模块耦合度、明确职责边界。

## 识别的问题

### 1. 模块交叉依赖问题

#### 问题描述
- **Match ↔ C4Manager 双向依赖**
  - Match 类直接调用 C4Manager 的方法
  - C4Manager 通过 MatchContext 回调 Match 的方法
  - 耦合度高，难以独立测试和维护

- **C4Item → MatchManager 依赖**
  - C4Item 直接依赖 MatchManager 来查询比赛状态
  - 违反了依赖倒置原则

#### 影响
- 模块难以独立开发和测试
- 修改一个模块可能影响其他模块
- 代码复用性差

### 2. 职责边界不清晰

#### ServerConfig 问题
- **混合职责**
  - 同时承担配置文件读取
  - 充当静态配置数据存储器
- **违反单一职责原则**

#### 影响
- 配置管理逻辑与数据存储混在一起
- 难以替换配置源（如从数据库读取）
- 测试时难以模拟配置

### 3. 网络模块结构不清晰

#### 问题描述
- **network/** 包仅有 OpenShopPacket
- 未区分客户端→服务器、服务器→客户端的数据包
- 缺少网络协议的整体规划

#### 影响
- 后续添加网络功能时结构混乱
- 难以维护和理解数据流向

## 重构方案

### 阶段 1: Match 与 C4Manager 解耦

#### 目标
消除 Match 和 C4Manager 之间的双向依赖，使用事件驱动架构。

#### 实施步骤

**1.1 引入 C4 事件系统**
```
创建事件类：
- C4PlantedEvent
- C4DefusedEvent
- C4ExplodedEvent
- C4DroppedEvent
```

**1.2 修改 C4Manager**
```java
// 当前：直接调用 Match 方法
context.endRound("T", "炸弹已爆炸");

// 重构后：发布事件
eventBus.post(new C4ExplodedEvent(matchId, position));
```

**1.3 修改 Match**
```java
// 订阅 C4 事件
@SubscribeEvent
public void onC4Exploded(C4ExplodedEvent event) {
    if (event.getMatchId().equals(this.id)) {
        endRound("T", "炸弹已爆炸");
    }
}
```

**1.4 重构 C4Item**
```java
// 当前：直接查询 MatchManager
Match match = MatchManager.getMatch(matchName);

// 重构后：通过服务接口
MatchQuery matchQuery = ServiceRegistry.get(MatchQuery.class);
Optional<MatchInfo> match = matchQuery.findMatchByPlayer(player);
```

#### 预期成果
- Match 和 C4Manager 通过事件通信，无直接依赖
- C4Item 通过接口访问比赛信息
- 各模块可独立测试

#### 风险评估
- **中等风险**：需要大量修改现有代码
- **测试要求**：需要完整的回归测试

---

### 阶段 2: ServerConfig 职责分离

#### 目标
将配置读取与配置存储分离，遵循单一职责原则。

#### 实施步骤

**2.1 创建 ConfigReader**
```java
/**
 * 负责从文件读取配置
 */
public class ConfigReader {
    public ServerConfigData load(Path configPath) {
        // 读取 TOML 文件
        // 返回配置数据对象
    }
}
```

**2.2 创建 ConfigHolder**
```java
/**
 * 持有配置数据，提供访问接口
 */
public class ConfigHolder {
    private final ServerConfigData data;
    
    public int getBuyPhaseSeconds() {
        return data.buyPhaseSeconds;
    }
    
    // ... 其他访问方法
}
```

**2.3 重构 ServerConfig**
```java
/**
 * 配置管理器，协调读取和存储
 */
public class ServerConfig {
    private static ConfigHolder holder;
    
    public static void load(Path path) {
        ConfigReader reader = new ConfigReader();
        ServerConfigData data = reader.load(path);
        holder = new ConfigHolder(data);
    }
    
    public static ConfigHolder get() {
        return holder;
    }
}
```

**2.4 更新使用方**
```java
// 当前
int buyTime = ServerConfig.buyPhaseSeconds;

// 重构后
int buyTime = ServerConfig.get().getBuyPhaseSeconds();
```

#### 预期成果
- 配置读取逻辑与存储分离
- 易于添加新的配置源（数据库、远程配置等）
- 便于单元测试

#### 风险评估
- **低风险**：影响范围可控，主要是调用方式变化
- **测试要求**：配置读取的单元测试

---

### 阶段 3: 网络模块结构化

#### 目标
建立清晰的网络协议层次结构，区分数据包类型和方向。

#### 实施步骤

**3.1 创建包结构**
```
network/
├── packet/
│   ├── clientbound/     # 服务器→客户端
│   │   ├── ShopDataPacket.java
│   │   ├── MatchStatePacket.java
│   │   └── ScoreUpdatePacket.java
│   └── serverbound/     # 客户端→服务器
│       ├── OpenShopPacket.java
│       ├── PurchaseItemPacket.java
│       └── CloseShopPacket.java
├── handler/
│   ├── ClientPacketHandler.java
│   └── ServerPacketHandler.java
└── NetworkRegistry.java
```

**3.2 定义基础数据包接口**
```java
public interface ClientboundPacket {
    void handle(ClientPacketHandler handler);
}

public interface ServerboundPacket {
    void handle(ServerPacketHandler handler);
}
```

**3.3 迁移现有数据包**
```java
// 移动 OpenShopPacket 到 serverbound/
// 实现 ServerboundPacket 接口
```

**3.4 创建网络注册表**
```java
public class NetworkRegistry {
    public static void registerPackets() {
        // 注册所有数据包
        registerClientbound(ShopDataPacket.class);
        registerServerbound(OpenShopPacket.class);
        // ...
    }
}
```

#### 预期成果
- 清晰的网络包结构
- 数据流向一目了然
- 易于添加新的网络功能

#### 风险评估
- **低风险**：主要是文件移动和接口实现
- **测试要求**：网络包序列化/反序列化测试

---

## 实施优先级

### P0 - 高优先级（立即执行）
无。当前 PR 应专注于功能改进。

### P1 - 中优先级（功能稳定后）
1. **阶段 2: ServerConfig 职责分离**
   - 风险最低
   - 影响范围可控
   - 可快速实施并验证

### P2 - 低优先级（有余力时）
1. **阶段 3: 网络模块结构化**
   - 当前网络功能简单
   - 不是紧迫问题
   - 可随新功能开发逐步完善

2. **阶段 1: Match 与 C4Manager 解耦**
   - 改动最大
   - 需要充分测试
   - 建议在其他重构完成后进行

---

## 实施指南

### 每个阶段的工作流程

1. **创建独立分支**
   ```bash
   git checkout -b refactor/phase-N-description
   ```

2. **创建专门的 PR**
   - 一个阶段一个 PR
   - 便于代码审查
   - 易于回滚

3. **编写测试**
   - 重构前：为现有功能编写测试
   - 重构后：确保测试通过
   - 添加新的单元测试

4. **分步提交**
   - 每个小改动一个提交
   - 提交信息清晰描述变更
   - 保持每个提交可编译

5. **充分测试**
   - 单元测试
   - 集成测试
   - 手动功能测试

6. **代码审查**
   - 邀请团队成员审查
   - 讨论设计决策
   - 合并前解决所有评论

---

## 成功标准

### 阶段 1 成功标准
- [ ] Match 不直接调用 C4Manager 方法
- [ ] C4Manager 不直接调用 Match 方法
- [ ] C4Item 通过接口访问比赛信息
- [ ] 所有现有功能正常工作
- [ ] 单元测试覆盖率 > 80%

### 阶段 2 成功标准
- [ ] ConfigReader 可独立测试
- [ ] ConfigHolder 不包含 I/O 逻辑
- [ ] 可以轻松添加新的配置源
- [ ] 所有配置访问正常工作
- [ ] 配置加载性能不下降

### 阶段 3 成功标准
- [ ] 数据包按方向明确分类
- [ ] 新增数据包有清晰的添加位置
- [ ] 网络包处理逻辑集中管理
- [ ] 所有网络功能正常工作
- [ ] 网络通信日志清晰可读

---

## 注意事项

### 1. 不要混合重构与功能开发
- 重构 PR 只做重构
- 功能 PR 只做功能
- 避免同时进行，降低风险

### 2. 保持向后兼容
- API 变更要有过渡期
- 标记废弃 API 但保留
- 提供迁移指南

### 3. 文档同步更新
- 更新架构文档
- 更新 API 文档
- 更新开发指南

### 4. 性能考虑
- 重构不应降低性能
- 关键路径需要性能测试
- 必要时进行性能对比

---

## 时间估算

| 阶段 | 预计工作量 | 建议时间窗口 |
|------|-----------|------------|
| 阶段 1 | 3-5 天 | 1-2 周（包含测试） |
| 阶段 2 | 2-3 天 | 1 周 |
| 阶段 3 | 2-3 天 | 1 周 |
| **总计** | **7-11 天** | **3-4 周** |

---

## 参考资源

### 设计模式
- 事件驱动架构
- 依赖倒置原则
- 单一职责原则
- 接口隔离原则

### 相关文档
- `docs/QUICK_START.md` - 开发指南
- `README.md` - 项目概述

---

## 版本历史

| 版本 | 日期 | 变更说明 |
|------|------|---------|
| 1.0 | 2024-10 | 初始版本 |

---

## 反馈与更新

如有疑问或建议，请在相关 Issue 或 PR 中讨论。

本计划将根据实施过程中的反馈持续更新。
