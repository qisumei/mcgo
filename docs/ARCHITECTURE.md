# MCGO 项目架构文档

## 概述

MCGO 是一个基于 NeoForge 的 Minecraft 模组，在 Minecraft 中重现经典的反恐精英(CS:GO)游戏体验。

## 技术栈

- **Minecraft**: 1.21+
- **NeoForge**: 21.0.x+
- **Java**: 21
- **Gradle**: 9.1.0
- **依赖**: PointBlank 模组（提供武器系统）

## 项目结构

```
src/main/java/com/qisumei/csgo/
├── c4/                      # C4炸弹系统
│   ├── block/              # C4方块实现
│   ├── handler/            # C4倒计时处理
│   ├── item/               # C4物品
│   ├── sound/              # 音效
│   └── task/               # 异步任务（拆弹、计时）
├── client/                  # 客户端代码
│   ├── ClientKeyInputHandler.java
│   └── KeyBindings.java
├── commands/                # 命令系统
│   ├── CSCommand.java      # 命令注册
│   └── CommandHandlers.java # 命令处理逻辑
├── config/                  # 配置系统
│   └── ServerConfig.java
├── economy/                 # 经济系统
│   ├── ShopGUI.java        # 商店界面
│   ├── VirtualMoneyManager.java # 虚拟货币管理
│   └── WeaponPrices.java   # 武器价格
├── entity/                  # 实体类型
├── events/                  # 事件处理
│   ├── match/              # 比赛相关事件
│   ├── ClientEvents.java
│   ├── GameEventsHandler.java
│   └── PlayerLifecycleEventsHandler.java
├── game/                    # 核心游戏逻辑
│   ├── preset/             # 比赛预设
│   ├── EconomyManager.java # 经济管理
│   ├── Match.java          # 比赛核心类（1283行）
│   ├── MatchAreaManager.java
│   ├── MatchContext.java
│   ├── MatchManager.java
│   ├── MatchPlayerService.java # 玩家服务
│   ├── MatchScoreboard*.java   # 计分板
│   ├── PlayerService.java
│   ├── PlayerStats.java
│   ├── RoundEconomyService.java
│   └── TeamSwapService.java
├── grenade/                 # 手雷系统
├── mixin/                   # Mixin注入
├── network/                 # 网络通信
├── server/                  # 服务器命令
├── service/                 # 服务抽象层
│   ├── ServiceRegistry.java # 服务定位器
│   ├── ServiceFallbacks.java
│   ├── EconomyService*.java
│   └── MatchService*.java
├── util/                    # 工具类
└── weapon/                  # 武器系统
    ├── WeaponDefinition.java
    ├── WeaponFactory.java
    ├── WeaponRegistry.java
    └── WeaponType.java
```

## 核心模块说明

### 1. 比赛系统 (`game/`)

#### Match.java (核心类)
- **职责**: 管理单场比赛的完整生命周期
- **规模**: 1283 行（较大，未来可能需要拆分）
- **功能**:
  - 玩家管理（加入/离开/踢出）
  - 回合管理（开始/结束/计时）
  - 队伍管理（分队/换边）
  - 经济系统集成
  - C4系统集成
  - 计分板更新

#### MatchManager.java
- **职责**: 全局比赛管理器
- **功能**:
  - 创建/删除比赛
  - 查询比赛（按名称、玩家、位置）
  - 比赛集合管理
  - 定时更新所有比赛

#### 服务类
- **PlayerService / MatchPlayerService**: 玩家相关操作（清空背包、发放装备、捕获装备）
- **RoundEconomyService**: 回合经济逻辑（奖励分配、连败补偿）
- **TeamSwapService**: 队伍交换逻辑

### 2. 经济系统 (`economy/`, `game/EconomyManager.java`)

#### VirtualMoneyManager (单例)
- **职责**: 管理玩家虚拟货币
- **特点**: 
  - 使用 ConcurrentHashMap 保证线程安全
  - 内存存储（不持久化）
  - 防止货币溢出（最大65535）

#### EconomyManager
- **职责**: 经济系统静态工具类
- **功能**:
  - 发放/扣除货币
  - 计算击杀奖励
  - 价格查询

#### ShopGUI
- **职责**: 商店界面
- **规模**: 484 行
- **功能**:
  - GUI界面管理
  - 武器分类展示
  - 购买逻辑
  - 余额显示

### 3. C4炸弹系统 (`c4/`)

#### C4Manager
- **职责**: C4全局管理
- **功能**:
  - 分配C4给T队
  - 安放检测
  - 倒计时管理
  - 拆弹逻辑
  - 爆炸效果

#### C4相关类
- **C4Item**: C4物品行为（右键安放）
- **C4Block**: C4方块状态
- **C4CountdownHandler**: 倒计时显示
- **C4TickTask / C4DefuseTask**: 异步任务

### 4. 武器系统 (`weapon/`)

详见 [武器系统重构说明](WEAPON_SYSTEM_REFACTOR.md) 和 [如何添加新武器](HOW_TO_ADD_WEAPONS.md)。

#### 核心组件
- **WeaponRegistry**: 武器注册表（单例）
- **WeaponDefinition**: 武器定义（Builder模式）
- **WeaponFactory**: 武器和弹药创建工厂
- **WeaponType / AmmoType**: 类型枚举

### 5. 命令系统 (`commands/`)

#### CSCommand
- **职责**: 命令注册入口
- **支持的命令**: create, join, began, list, kick, setspawn, setshop, setbombsite, rounds, roundtime, watch

#### CommandHandlers
- **职责**: 具体命令处理逻辑
- **规模**: 498 行
- **特点**: 参数验证、权限检查、错误处理

### 6. 事件系统 (`events/`)

#### 事件总线 (`events/match/`)
- **MatchEventBus**: 比赛事件总线
- **MatchEventListener**: 事件监听器接口
- **具体事件**: RoundStartEvent, TeamSwapEvent
- **事件处理器**: EconomyEventHandler

#### 全局事件处理
- **GameEventsHandler**: 游戏事件（击杀、死亡等）
- **PlayerLifecycleEventsHandler**: 玩家生命周期
- **ClientEvents**: 客户端事件

### 7. 服务定位器模式 (`service/`)

#### ServiceRegistry
- **模式**: 服务定位器（Service Locator）
- **用途**: 解耦模块依赖，支持运行时替换实现
- **线程安全**: 使用 ConcurrentHashMap

#### 注册的服务
- **EconomyService**: 经济服务接口
- **MatchService**: 比赛服务接口
- **ServerCommandExecutor**: 命令执行器接口

**注意**: 当前实现主要是对现有静态管理器的包装，实际使用中可以考虑简化。

### 8. 配置系统 (`config/`)

#### ServerConfig
- **职责**: 服务器配置管理
- **规模**: 291 行
- **功能**:
  - TOML配置文件读取
  - 配置值存储
  - 默认值定义

**配置项分类**:
- 经济配置（起始金额、奖励等）
- 时间配置（购买阶段、回合时长）
- 游戏规则（受保护物品、初始装备）
- 击杀奖励（按武器类型）

## 设计模式使用

### 1. 单例模式 (Singleton)
- `VirtualMoneyManager`
- `WeaponRegistry`
- `MatchManager`（事实上的单例）

### 2. 工厂模式 (Factory)
- `WeaponFactory`: 创建武器和弹药

### 3. 建造者模式 (Builder)
- `WeaponDefinition.Builder`: 构建武器定义

### 4. 服务定位器 (Service Locator)
- `ServiceRegistry`: 管理服务实例

### 5. 观察者模式 (Observer)
- `MatchEventBus` + `MatchEventListener`: 事件系统

### 6. 策略模式 (Strategy)
- `PlayerService`: 可替换的玩家操作实现

## 依赖关系

### 核心依赖图
```
Match (核心)
  ├─> MatchManager (创建和管理)
  ├─> C4Manager (C4系统)
  ├─> EconomyManager (经济系统)
  ├─> PlayerService (玩家操作)
  ├─> ServerCommandExecutor (命令执行)
  └─> MatchEventBus (事件发布)

ShopGUI
  ├─> WeaponRegistry (获取武器)
  ├─> WeaponFactory (创建武器)
  └─> VirtualMoneyManager (查询余额)

CommandHandlers
  ├─> MatchManager (比赛操作)
  └─> ServerConfig (读取配置)
```

## 数据流

### 比赛流程
```
创建比赛 -> 玩家加入 -> 开始比赛 -> 购买阶段 -> 战斗阶段 -> 回合结算 -> 下一回合
                                   ↓
                              打开商店GUI -> 选择武器 -> 购买 -> 发放装备
```

### 经济流程
```
回合开始
  ├─> 手枪局: 发放 800 起始资金
  └─> 其他回合: 
      ├─> 胜利: +3250
      ├─> 失败: +1400 + 连败补偿
      └─> 击杀: +300~1500 (按武器)
```

### C4流程
```
T队开始 -> 随机分配C4 -> 进入包点 -> 安放C4 -> 倒计时40秒
                                         ├─> CT拆弹成功 -> CT胜利
                                         └─> 倒计时结束 -> 爆炸 -> T胜利
```

## 线程安全

### 线程安全的类
- `VirtualMoneyManager`: ConcurrentHashMap
- `ServiceRegistry`: ConcurrentHashMap
- `MatchManager`: synchronized 方法

### 需要注意的并发问题
- Minecraft 服务器主线程处理大部分游戏逻辑
- 异步任务（C4TickTask, C4DefuseTask）需要确保线程安全
- GUI 操作在客户端线程执行

## 性能考虑

### 潜在的性能瓶颈
1. **Match.java (1283行)**: 单个类过大，可能影响可维护性
2. **每Tick更新**: MatchManager.tick() 遍历所有比赛
3. **事件处理**: 大量玩家时的事件处理开销

### 优化建议
1. 考虑拆分 Match 类
2. 使用事件驱动减少轮询
3. 缓存频繁查询的数据

## 测试策略

### 当前状态
- **单元测试**: 无
- **集成测试**: 手动测试

### 建议
1. 为核心类添加单元测试（Match, EconomyManager, WeaponRegistry）
2. 使用 Mock 对象测试服务交互
3. 添加回归测试防止功能退化

## 技术债务

### 已识别的问题
1. ~~MatchPlayerHelper 已废弃~~ ✅ 已清理
2. Service 层过度设计（接口只是简单委托）
3. Match.java 过大（1283行）
4. 缺少单元测试
5. 部分类职责不够单一

### 改进建议
详见 [重构计划](REFACTORING_PLAN.md)

## 扩展指南

### 添加新武器
参见 [如何添加新武器](HOW_TO_ADD_WEAPONS.md)

### 添加新命令
1. 在 `CSCommand.java` 注册命令
2. 在 `CommandHandlers.java` 实现处理逻辑
3. 更新文档

### 添加新事件
1. 在 `events/match/` 创建事件类
2. 在需要的地方发布事件 (`MatchEventBus.post()`)
3. 实现 `MatchEventListener` 处理事件

### 修改游戏规则
编辑 `config/qiscsgo-server.toml` 配置文件

## 参考文档

- [README.md](../README.md) - 项目介绍和使用说明
- [QUICK_START.md](QUICK_START.md) - 快速开始指南
- [WEAPON_SYSTEM_REFACTOR.md](WEAPON_SYSTEM_REFACTOR.md) - 武器系统详解
- [HOW_TO_ADD_WEAPONS.md](HOW_TO_ADD_WEAPONS.md) - 添加武器教程
- [ECONOMY_BALANCE_ANALYSIS.md](ECONOMY_BALANCE_ANALYSIS.md) - 经济平衡分析
- [CHANGELOG.md](CHANGELOG.md) - 更新日志

## 版本历史

| 版本 | 日期 | 说明 |
|------|------|------|
| 1.0 | 2025-10 | 初始版本 |

---

**维护者**: qisumei,SelfAbandonment
**最后更新**: 2025-10-25
