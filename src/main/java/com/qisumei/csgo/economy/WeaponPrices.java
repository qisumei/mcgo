package com.qisumei.csgo.economy;

import com.qisumei.csgo.weapon.WeaponDefinition;
import com.qisumei.csgo.weapon.WeaponRegistry;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 武器和装备价格表
 * 重构后优先使用 WeaponRegistry，保留向后兼容性
 */
public class WeaponPrices {
    // 保留旧的价格映射以实现向后兼容
    private static final Map<String, Integer> LEGACY_PRICES = new HashMap<>();

    static {
        // 手枪（缩放）
        LEGACY_PRICES.put("tacz:glock_17", 2);
        LEGACY_PRICES.put("tacz:m9", 3);
        LEGACY_PRICES.put("tacz:deagle", 7);

        // 冲锋枪（缩放）
        LEGACY_PRICES.put("tacz:mp7", 15);
        LEGACY_PRICES.put("tacz:ump45", 12);
        LEGACY_PRICES.put("tacz:p90", 23);
        LEGACY_PRICES.put("tacz:mp5", 14);
        LEGACY_PRICES.put("tacz:vector", 22);

        // 步枪（缩放）
        LEGACY_PRICES.put("tacz:ak47", 27);
        LEGACY_PRICES.put("tacz:m4a1", 31);
        LEGACY_PRICES.put("tacz:aug", 33);
        LEGACY_PRICES.put("tacz:sg552", 30);

        // 狙击
        LEGACY_PRICES.put("tacz:awp", 47);

        // 投掷物
        LEGACY_PRICES.put("tacz:frag_grenade", 3);

        // 护甲与工具（缩放）
        LEGACY_PRICES.put("minecraft:leather_chestplate", 3);
        LEGACY_PRICES.put("minecraft:iron_chestplate", 10);
    }

    /**
     * 获取物品价格
     * 优先从 WeaponRegistry 获取，回退到旧的价格映射
     * 
     * @param itemId 物品ID（如 "pointblank:ak47"）
     * @return 价格，如果未定义则返回0
     */
    public static int getPrice(String itemId) {
        // 首先尝试从武器注册表获取
        Optional<WeaponDefinition> weaponOpt = WeaponRegistry.getWeapon(itemId);
        if (weaponOpt.isPresent()) {
            return weaponOpt.get().getPrice();
        }
        
        // 回退到旧的价格映射
        return LEGACY_PRICES.getOrDefault(itemId, 0);
    }

    /**
     * 检查物品是否可以购买
     */
    public static boolean canPurchase(String itemId) {
        return WeaponRegistry.isRegistered(itemId) || LEGACY_PRICES.containsKey(itemId);
    }

    /**
     * 获取所有价格配置（用于调试）
     */
    public static Map<String, Integer> getAllPrices() {
        Map<String, Integer> allPrices = new HashMap<>(LEGACY_PRICES);
        
        // 添加注册表中的武器价格
        for (WeaponDefinition weapon : WeaponRegistry.getAllWeapons()) {
            allPrices.put(weapon.getWeaponId(), weapon.getPrice());
        }
        
        return allPrices;
    }
}
