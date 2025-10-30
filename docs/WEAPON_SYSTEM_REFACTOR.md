# 武器系统重构说明

## 概述

本次重构为 MCGO 项目创建了一个新的武器系统抽象层，将 TaCZ 模组的武器进行封装，使得添加新武器到商店变得更加简单和一致。

## 核心组件

### 1. 武器类型 (WeaponType)

定义了所有武器类别的枚举：
- `KNIFE` - 近战武器
- `PISTOL` - 手枪
- `SMG` - 冲锋枪
- `HEAVY` - 重型武器
- `RIFLE` - 步枪
- `SNIPER` - 狙击枪
- `GRENADE` - 投掷物

每种类型都有默认的击杀奖励和价格。

### 2. 弹药类型 (AmmoType)

定义了所有弹药类型及其对应的 TaCZ 物品ID：
- `AMMO_9MM` - 9mm弹药
- `AMMO_45ACP` - .45 ACP弹药
- `AMMO_50AE` - .50 AE弹药
- `AMMO_46` - 4.6mm弹药
- `AMMO_57` - 5.7mm弹药
- `AMMO_556` - 5.56mm弹药
- `AMMO_762` - 7.62mm弹药
- `AMMO_338` - .338 Lapua弹药

### 3. 武器附件 (WeaponAttachment)

定义了武器可以附加的附件：
- `SCOPE` - 瞄准镜
- `BARREL` - 枪管
- `GRIP` - 握把
- `STOCK` - 枪托
- `MAGAZINE` - 弹匣

预定义的附件：
- `ACOG_SCOPE` - ACOG瞄准镜（用于步枪）
- `SCOPE_8X` - 8倍镜（用于狙击枪）

### 4. 武器定义 (WeaponDefinition)

封装了武器的所有属性：
- 武器ID（TaCZ 物品ID）
- 显示名称
- 武器类型
- 价格
- 击杀奖励
- 弹药类型
- 默认弹药数量
- 默认附件
- 队伍可用性（CT/T）

使用 Builder 模式创建：

```java
WeaponDefinition ak47 = new WeaponDefinition.Builder("tacz:ak47", "AK-47", WeaponType.RIFLE)
    .price(27)
    .killReward(3)
    .ammoType(AmmoType.AMMO_762)
    .addAttachment(WeaponAttachment.ACOG_SCOPE)
    .bothTeams()
    .build();
```

### 5. 武器工厂 (WeaponFactory)

负责根据武器定义创建实际的 Minecraft 物品：
- 创建武器物品
- 附加默认附件
- 创建弹药物品

```java
ItemStack weapon = WeaponFactory.createWeapon(weaponDefinition);
ItemStack ammo = WeaponFactory.createAmmo(AmmoType.AMMO_762, 64);
```

### 6. 武器注册表 (WeaponRegistry)

管理所有武器定义的中央注册表：
- 注册武器定义
- 根据ID查询武器
- 根据类型查询武器
- 根据队伍查询可用武器

```java
// 初始化注册表（在模组加载时自动调用）
WeaponRegistry.initialize();

// 查询武器
Optional<WeaponDefinition> weapon = WeaponRegistry.getWeapon("tacz:ak47");

// 查询特定类型的武器
List<WeaponDefinition> rifles = WeaponRegistry.getWeaponsByType(WeaponType.RIFLE);

// 查询特定队伍可用的武器
List<WeaponDefinition> ctWeapons = WeaponRegistry.getWeaponsForTeam("CT");
```

### 7. 商店物品 (ShopItem)

封装在商店中显示的物品：
- 物品ID
- 显示名称
- 价格
- 显示用的物品栈
- 是否每回合只能购买一次

```java
ShopItem shopItem = ShopItem.fromWeaponDefinition(weaponDefinition);
```

## 如何添加新武器

### 方法1: 在 WeaponRegistry 中注册

编辑 `WeaponRegistry.java`，在相应的注册方法中添加新武器：

```java
private static void registerRifles() {
    // 现有武器...
    
    // 添加新武器
    register(new WeaponDefinition.Builder("tacz:scar", "SCAR-H", WeaponType.RIFLE)
        .price(28)
        .killReward(3)
        .ammoType(AmmoType.AMMO_762)
        .addAttachment(WeaponAttachment.ACOG_SCOPE)
        .bothTeams()
        .build());
}
```

### 方法2: 运行时动态注册

```java
WeaponDefinition customWeapon = new WeaponDefinition.Builder(
    "custom:weapon", 
    "Custom Weapon", 
    WeaponType.RIFLE
)
    .price(25)
    .killReward(4)
    .ammoType(AmmoType.AMMO_556)
    .ctOnly()  // 仅CT队可用
    .build();

WeaponRegistry.register(customWeapon);
```

## 商店系统集成

重构后的 `ShopGUI` 自动使用 `WeaponRegistry` 来填充商店物品：

1. **自动分类**: 武器按类型自动分配到不同行
2. **队伍过滤**: 自动显示对应队伍可用的武器
3. **自动价格**: 从武器定义中读取价格
4. **自动弹药**: 购买武器时自动赠送对应口径的弹药
5. **自动附件**: 武器自动附加在定义中指定的附件

## 向后兼容性

- `WeaponPrices` 类保留了旧的价格映射，确保向后兼容
- 优先使用 `WeaponRegistry`，如果找不到则回退到旧的映射
- `ShopGUI` 保留了旧的物品创建逻辑作为回退

## 优势

### 1. 集中管理
所有武器定义集中在 `WeaponRegistry` 中，易于维护和修改。

### 2. 类型安全
使用强类型的枚举和类，减少字符串硬编码错误。

### 3. 易于扩展
添加新武器只需在注册表中添加一行代码。

### 4. 状态管理
武器定义包含了所有必要的状态信息（弹药、附件等）。

### 5. 队伍支持
内置队伍可用性检查，支持武器仅对特定队伍可用。

### 6. 自动化
商店系统自动处理武器显示、弹药分配和附件附加。

## 未来改进

1. **配置文件支持**: 允许从配置文件加载武器定义
2. **数据驱动**: 支持通过JSON文件定义武器
3. **热重载**: 支持运行时重载武器定义
4. **更多附件**: 支持更多类型的武器附件
5. **武器皮肤**: 支持武器外观自定义
6. **统计追踪**: 追踪每种武器的使用统计

## 示例：完整的武器添加流程

```java
// 1. 定义新的弹药类型（如果需要）
// 在 AmmoType.java 中添加：
AMMO_9X39("tacz:ammo/9x39mm", "9x39mm")

// 2. 定义新的附件（如果需要）
// 在 WeaponAttachment.java 中添加：
public static final WeaponAttachment RED_DOT = new WeaponAttachment(
    "tacz:attachment/red_dot", "红点瞄具", AttachmentType.SCOPE
);

// 3. 在 WeaponRegistry 中注册武器
private static void registerRifles() {
    // ... 其他武器
    
    register(new WeaponDefinition.Builder("tacz:as_val", "AS Val", WeaponType.RIFLE)
        .price(29)
        .killReward(3)
        .ammoType(AmmoType.AMMO_9X39)
        .defaultAmmoAmount(60)
        .addAttachment(WeaponAttachment.RED_DOT)
        .tOnly()  // 仅T队可用
        .build());
}

// 4. 武器会自动出现在商店中，无需修改 ShopGUI
```

## 测试

添加新武器后，应该测试：

1. ✅ 武器在商店中正确显示
2. ✅ 价格正确显示
3. ✅ 购买后武器正确添加到背包
4. ✅ 弹药自动赠送
5. ✅ 附件正确附加到武器
6. ✅ 队伍限制正确工作
7. ✅ 击杀奖励正确计算

## 迁移指南

如果你有自定义代码使用旧的系统：

### 旧代码：
```java
// 直接使用物品ID
addShopItem(slot++, "tacz:ak47", "AK-47", 27);
```

### 新代码：
```java
// 使用武器定义
WeaponDefinition weapon = WeaponRegistry.getWeapon("tacz:ak47").orElseThrow();
addShopItemFromWeapon(slot++, weapon);
```

### 获取价格：
```java
// 旧方式（仍然支持）
int price = WeaponPrices.getPrice("tacz:ak47");

// 新方式（推荐）
int price = WeaponRegistry.getWeapon("tacz:ak47")
    .map(WeaponDefinition::getPrice)
    .orElse(0);
```

## 总结

新的武器系统提供了一个清晰、类型安全、易于扩展的架构，将 TaCZ 武器系统很好地封装起来，同时保持了向后兼容性。添加新武器现在只需要在注册表中添加几行代码，系统会自动处理其余的事情。
