# 如何添加新武器到商店

本指南说明如何使用新的武器系统轻松地添加新武器到游戏商店中。

## 快速开始

添加新武器只需要3步:

### 1. 打开 WeaponRegistry.java

文件位置: `src/main/java/com/qisumei/csgo/weapon/WeaponRegistry.java`

### 2. 找到对应的武器类型注册方法

根据武器类型选择相应的方法：
- 手枪 → `registerPistols()`
- 冲锋枪 → `registerSmgs()`
- 步枪 → `registerRifles()`
- 狙击枪 → `registerSnipers()`
- 投掷物 → `registerGrenades()`

### 3. 添加武器定义

在相应方法中添加武器注册代码：

```java
register(new WeaponDefinition.Builder(
    "tacz:weapon_id",       // TaCZ 武器的物品ID
    "武器显示名称",           // 在商店中显示的名称
    WeaponType.RIFLE        // 武器类型
)
    .price(30)              // 价格
    .killReward(3)          // 击杀奖励
    .ammoType(AmmoType.AMMO_556)  // 弹药类型
    .addAttachment(WeaponAttachment.ACOG_SCOPE)  // 默认附件（可选）
    .bothTeams()            // 两队都可用（或使用 .ctOnly() 或 .tOnly()）
    .build());
```

## 完整示例

### 示例1: 添加手枪

```java
private static void registerPistols() {
    // ... 现有武器 ...
    
    // 添加 P250
    register(new WeaponDefinition.Builder("tacz:p250", "P250", WeaponType.PISTOL)
        .price(3)
        .killReward(3)
        .ammoType(AmmoType.AMMO_9MM)
        .bothTeams()
        .build());
}
```

### 示例2: 添加CT专属步枪

```java
private static void registerRifles() {
    // ... 现有武器 ...
    
    // 添加 FAMAS（仅CT可用）
    register(new WeaponDefinition.Builder("tacz:famas", "FAMAS", WeaponType.RIFLE)
        .price(22)
        .killReward(3)
        .ammoType(AmmoType.AMMO_556)
        .addAttachment(WeaponAttachment.ACOG_SCOPE)
        .ctOnly()  // 仅CT队可用
        .build());
}
```

### 示例3: 添加T专属步枪

```java
private static void registerRifles() {
    // ... 现有武器 ...
    
    // 添加 Galil AR（仅T可用）
    register(new WeaponDefinition.Builder("tacz:galil", "Galil AR", WeaponType.RIFLE)
        .price(20)
        .killReward(3)
        .ammoType(AmmoType.AMMO_556)
        .addAttachment(WeaponAttachment.ACOG_SCOPE)
        .tOnly()  // 仅T队可用
        .build());
}
```

### 示例4: 添加狙击枪

```java
private static void registerSnipers() {
    // ... 现有武器 ...
    
    // 添加 SSG 08
    register(new WeaponDefinition.Builder("tacz:ssg08", "SSG 08", WeaponType.SNIPER)
        .price(17)
        .killReward(1)
        .ammoType(AmmoType.AMMO_762)
        .addAttachment(WeaponAttachment.SCOPE_8X)
        .bothTeams()
        .build());
}
```

## 武器属性说明

### 必需属性

| 属性 | 说明 | 示例 |
|------|------|------|
| 武器ID | TaCZ 模组中的物品ID | `"tacz:ak47"` |
| 显示名称 | 在商店中显示的名称 | `"AK-47"` |
| 武器类型 | 武器的分类 | `WeaponType.RIFLE` |

### 可选属性

| 属性 | 说明 | 默认值 | 示例 |
|------|------|--------|------|
| price | 购买价格 | 根据武器类型 | `.price(27)` |
| killReward | 击杀奖励 | 根据武器类型 | `.killReward(3)` |
| ammoType | 弹药类型 | `AmmoType.NONE` | `.ammoType(AmmoType.AMMO_762)` |
| defaultAmmoAmount | 赠送弹药数量 | 64 | `.defaultAmmoAmount(90)` |
| attachment | 默认附件 | 无 | `.addAttachment(WeaponAttachment.ACOG_SCOPE)` |
| team | 队伍可用性 | 两队都可用 | `.ctOnly()` / `.tOnly()` / `.bothTeams()` |

## 可用的弹药类型

```java
AmmoType.AMMO_9MM       // 9mm - 用于 Glock, M9, MP5
AmmoType.AMMO_45ACP     // .45 ACP - 用于 UMP-45, Vector
AmmoType.AMMO_50AE      // .50 AE - 用于沙漠之鹰
AmmoType.AMMO_46        // 4.6mm - 用于 MP7
AmmoType.AMMO_57        // 5.7mm - 用于 P90
AmmoType.AMMO_556       // 5.56mm - 用于 M4A1, AUG, SG 552
AmmoType.AMMO_762       // 7.62mm - 用于 AK-47
AmmoType.AMMO_338       // .338 Lapua - 用于 AWP
AmmoType.NONE           // 无弹药 - 用于近战和投掷物
```

## 可用的附件

```java
WeaponAttachment.ACOG_SCOPE   // ACOG瞄准镜 - 用于步枪
WeaponAttachment.SCOPE_8X     // 8倍镜 - 用于狙击枪
```

## 武器类型及其默认值

| 类型 | 显示名称 | 默认击杀奖励 | 默认价格 |
|------|----------|--------------|----------|
| KNIFE | 近战 | 1500 | 300 |
| PISTOL | 手枪 | 300 | 300 |
| SMG | 冲锋枪 | 600 | 600 |
| HEAVY | 重型武器 | 300 | 300 |
| RIFLE | 步枪 | 300 | 300 |
| SNIPER | 狙击枪 | 100 | 100 |
| GRENADE | 投掷物 | 300 | 300 |

## 自动功能

添加到注册表的武器会自动：

1. ✅ 在商店中正确分类显示
2. ✅ 根据队伍限制显示/隐藏
3. ✅ 购买时自动创建带附件的武器
4. ✅ 自动赠送对应口径的弹药
5. ✅ 在商店中显示正确的价格和图标
6. ✅ 计算正确的击杀奖励

## 测试清单

添加新武器后，请测试以下内容：

- [ ] 武器在商店中正确显示
- [ ] 价格正确
- [ ] 可以成功购买
- [ ] 购买后武器添加到背包
- [ ] 弹药自动赠送（如果有）
- [ ] 附件正确附加（如果有）
- [ ] 队伍限制正确（如果有）
- [ ] 击杀获得正确奖励
- [ ] 武器显示正确的名称和图标

## 常见问题

### Q: 如何找到 TaCZ 武器的物品ID？

A: 在游戏中使用 F3+H 显示高级工具提示，将鼠标悬停在物品上即可看到其ID。或者查看 TaCZ 模组的源代码和配置文件。

### Q: 我的武器没有在商店中显示？

A: 检查：
1. 武器ID是否正确
2. WeaponRegistry.initialize() 是否被调用
3. 队伍限制是否正确设置
4. 是否重启了游戏

### Q: 如何让武器只对一个队伍可用？

A: 使用 `.ctOnly()` 或 `.tOnly()`：
```java
.ctOnly()  // 仅CT队
.tOnly()   // 仅T队
.bothTeams()  // 两队都可用（默认）
```

### Q: 如何添加多个附件？

A: 多次调用 `.addAttachment()`：
```java
.addAttachment(WeaponAttachment.ACOG_SCOPE)
.addAttachment(someOtherAttachment)
```

### Q: 如何修改现有武器的属性？

A: 找到对应的 register() 调用，修改相应的属性值。

### Q: 如何临时禁用某个武器？

A: 注释掉对应的 register() 调用：
```java
// register(new WeaponDefinition.Builder(...) // 临时禁用
```

## 高级用法

### 动态添加武器（运行时）

```java
// 在代码中任何地方调用
WeaponDefinition customWeapon = new WeaponDefinition.Builder(
    "modid:weapon",
    "自定义武器",
    WeaponType.RIFLE
)
    .price(25)
    .bothTeams()
    .build();

WeaponRegistry.register(customWeapon);
```

### 查询注册的武器

```java
// 获取所有步枪
List<WeaponDefinition> rifles = WeaponRegistry.getWeaponsByType(WeaponType.RIFLE);

// 获取CT可用的所有武器
List<WeaponDefinition> ctWeapons = WeaponRegistry.getWeaponsForTeam("CT");

// 检查武器是否已注册
boolean exists = WeaponRegistry.isRegistered("pointblank:ak47");
```

## 贡献

如果你添加了新武器并希望分享，欢迎提交 Pull Request！

## 相关文档

- [武器系统重构说明](WEAPON_SYSTEM_REFACTOR.md) - 技术细节
- [README.md](../README.md) - 项目总览
