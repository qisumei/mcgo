
# 🎯 MCTP的CSGO - Minecraft版反恐精英

一个基于NeoForge的Minecraft模组，在MC中重现经典的反恐精英(Counter-Strike)游戏体验。

## ✨ 特色功能

### 🏆 完整的比赛系统
- **5v5竞技模式** - 经典的CT vs T对抗
- **回合制游戏** - 购买阶段 → 战斗阶段 → 回合结算
- **半场换边** - 6回合后自动交换队伍
- **实时计分板** - 显示击杀数、死亡数和队伍比分
- **优化观战系统** - 死亡后自动切换到队友视角，流畅的视角切换，隐藏战术信息防止泄露

### 💰 虚拟货币经济系统
- **虚拟货币** - 不再使用钻石，采用内存中的虚拟货币系统
- **回合收入** - 胜利/失败/连败都有不同的金钱奖励
- **击杀奖励** - 根据使用的武器获得不同金额的击杀奖励
- **起始资金** - 手枪局每人获得800起始资金

### 🛒 GUI商店系统
- **P键打开商店** - 购买阶段按P键即可打开商店界面
- **箱子GUI界面** - 清晰的6x9网格展示所有可购买物品
- **分队伍商店** - CT和T队有不同的武器选择
- **实时余额显示** - 界面中显示当前可用资金（移除独立余额命令）
- **无村民NPC** - 告别拥挤的村民商店，享受清爽的购买体验

### 🔫 CSGO式武器定价
| 类别 | 武器示例 | 价格范围 |
|------|----------|----------|
| **手枪** | Glock, USP, Desert Eagle | $200 - $700 |
| **冲锋枪** | MP9, UMP-45, P90 | $1050 - $2350 |
| **步枪** | AK-47, M4A4, AUG | $1800 - $3300 |
| **狙击枪** | SSG 08, AWP | $1700 - $4750 |
| **投掷物** | 闪光弹, 手雷, 烟雾弹 | $50 - $600 |
| **护甲** | 护甲, 护甲+头盔 | $350 - $1000 |

### 💣 C4炸弹系统
- **T队专属C4** - 随机分配给T队玩家
- **包点安放** - 只能在指定的A点或B点安放
- **倒计时爆炸** - 40秒倒计时，紧张刺激
- **拆弹系统** - CT队可以购买拆弹器进行拆弹
- **爆炸伤害** - 自定义爆炸伤害系统，范围内玩家受到伤害
- **C4掉落提示** - 显示到C4的距离，不显示坐标以防战术泄露

## 🎮 游戏操作

### 管理员命令
```bash
# 比赛管理
/cs create <比赛名> <最大人数> [预设名]  # 创建比赛
/cs join <比赛名>                      # 加入比赛
/cs began <比赛名> [yes]               # 开始比赛（yes为强制开始）
/cs list                               # 查看所有比赛
/cs kick <比赛名> <玩家名>             # 踢出玩家

# 地图设置
/cs setspawn <比赛名> <CT|T> <坐标>    # 设置出生点
/cs setshop <比赛名> <CT|T> <坐标>     # 设置商店位置
/cs setbombsite <比赛名> <A|B> <坐标1> <坐标2>  # 设置包点区域

# 比赛配置
/cs rounds <比赛名> <回合数>           # 设置总回合数（必须为偶数）
/cs roundtime <比赛名> <秒数>         # 设置每回合时间
```

### 玩家操作
```bash
# 基础操作
P键                    # 在购买阶段打开商店GUI（显示当前余额）

# 游戏内操作
右键C4                # T队在包点内安放C4
右键C4方块            # CT队拆除C4（需要拆弹器更快）

# 观战操作
/cs watch <比赛名>     # 观战指定比赛（仅限非比赛玩家）
```

## 📦 安装说明

### 前置要求
- **Minecraft** 1.21+
- **NeoForge** 21.0.x+
- **TaCZ** 模组（提供武器系统）

### 安装步骤
1. 下载最新版本的 `qiscsgo-1.0.0.jar`
2. 将jar文件放入 `mods` 文件夹
3. 确保已安装TaCZ模组（武器依赖）
4. 启动游戏

## ⚙️ 配置文件

配置文件位置：`config/qiscsgo-server.toml`

### 主要配置项
```toml
# 经济系统
pistolRoundStartingMoney = 800     # 手枪局起始资金
winReward = 3250                   # 胜利奖励
lossReward = 1400                  # 失败基础奖励
lossStreakBonus = 500              # 连败奖励递增
maxLossStreakBonus = 3400          # 最大连败奖励

# 时间设置
buyPhaseSeconds = 20               # 购买阶段时长
roundEndSeconds = 5                # 回合结束展示时间

# 击杀奖励（按武器类型）
killRewardKnife = 1500            # 近战击杀奖励
killRewardPistol = 300            # 手枪击杀奖励
killRewardRifle = 300             # 步枪击杀奖励
killRewardAwp = 100               # AWP击杀奖励
```

## 🎯 游戏流程

### 比赛流程
1. **创建比赛** - 管理员使用 `/cs create` 创建比赛
2. **玩家加入** - 玩家使用 `/cs join` 加入比赛
3. **开始比赛** - 管理员使用 `/cs began` 开始比赛
4. **购买阶段** - 玩家按P键打开商店购买装备（20秒）
5. **战斗阶段** - 玩家进行对战（默认120秒）
6. **回合结算** - 根据胜负条件结算比分和奖励
7. **重复循环** - 继续下一回合直到比赛结束

### 胜利条件
- **T队获胜**：
  - 击杀所有CT队员
  - C4爆炸成功
- **CT队获胜**：
  - 击杀所有T队员
  - 成功拆除C4
  - 时间耗尽（T队未完成目标）

## 🛠️ 开发信息

### 技术栈
- **NeoForge** 21.0.x - 模组框架
- **Java 21** - 开发语言
- **Gradle** 8.x - 构建工具

### 主要模块
- **比赛系统** (`game/`) - 核心比赛逻辑
- **经济系统** (`economy/`) - 虚拟货币和商店
- **C4系统** (`c4/`) - 炸弹机制
- **网络通信** (`network/`) - 客户端-服务器通信
- **客户端** (`client/`) - 按���绑定和输入处理

### 构建项目
```bash
# 克隆项目
git clone <repository-url>
cd mcgo

# 构建
./gradlew build

# 运行开发环境
./gradlew runClient  # 客户端
./gradlew runServer  # 服务器
```

## 🤝 贡献

欢迎提交Issue和Pull Request！

### 开发计划
- [x] 优化观战系统 - 流畅视角切换、隐藏战术信息
- [x] 移除余额命令 - 商店界面集成余额显示
- [x] C4掉落优化 - 移除坐标显示、保留距离提示
- [x] 事件处理优化 - 增强错误处理和日志记录
- [x] **测试基础设施** - JUnit 5测试框架，49个单元测试覆盖核心类
  - WeaponRegistry/WeaponDefinition: ~85-90%覆盖率
  - MatchPlayerService: 构造函数验证和接口实现测试
  - VirtualMoneyManager: 基础功能测试
- [x] **武器系统迁移** - 从 PointBlank 迁移到 TaCZ 模组
- [ ] 完善集成测试 - 在Minecraft环境中测试完整业务逻辑
- [ ] 实现GUI商店的购买逻辑
- [ ] 添加武器图标显示
- [ ] 添加更多地图预设
- [ ] 数据持久化支持

## 🧪 测试

### 运行测试
```bash
# 运行所有单元测试
./gradlew test

# 运行特定测试类
./gradlew test --tests WeaponRegistryTest
./gradlew test --tests MatchPlayerServiceTest

# 查看测试报告
open build/reports/tests/test/index.html
```

### 测试覆盖率

| 组件 | 测试数 | 覆盖率 | 状态 |
|-----|--------|--------|------|
| WeaponRegistry | 14 | ~90% | ✅ |
| WeaponDefinition | 14 | ~85% | ✅ |
| MatchPlayerService | 2 | ~10% | ⚠️ |
| VirtualMoneyManager | 3 | ~20% | ⚠️ |

**总计**: 49个单元测试 (减少至可编译的测试)

**注意**: 由于Minecraft依赖限制，部分测试仅覆盖构造函数验证和接口实现。完整的业务逻辑测试需要在实际游戏环境中进行。

详见：[测试指南](TESTING.md) | [测试文档](src/test/java/README.md)

## 📚 开发文档
- [项目架构](docs/ARCHITECTURE.md) - 完整的系统架构说明
- [如何添加新武器](docs/HOW_TO_ADD_WEAPONS.md) - 快速指南
- [武器系统详解](docs/WEAPON_SYSTEM_REFACTOR.md) - 技术细节
- [快速开始](docs/QUICK_START.md) - 开发环境配置
- [重构计划](docs/REFACTORING_PLAN.md) - 未来改进方向
- [测试指南](TESTING.md) - 单元测试编写和运行指南

## 📄 许可证

本项目采用 MIT 许可证。详见 [LICENSE](LICENSE) 文件。

## 🔗 相关链接

- [NeoForge官网](https://neoforged.net/)
- [TaCZ模组](https://github.com/bitzlay/TACZ)
- [问题反馈](../../issues)

---

**享受在Minecraft中的反恐精英体验！** 🎮

