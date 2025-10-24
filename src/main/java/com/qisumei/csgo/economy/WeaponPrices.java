package com.qisumei.csgo.economy;

import java.util.Map;

/**
 * 武器和装备价格表（参考CSGO真实价格）
 * 
 * <p><b>Bridge/Adapter Class:</b> This class now delegates to the new
 * mcgo-economy module to maintain backward compatibility during Phase 2 migration.
 * 
 * @deprecated Use {@link com.selfabandonment.mcgo.economy.pricing.WeaponPrices} instead.
 *             This bridge class will be removed in Phase 6.
 */
@Deprecated
public class WeaponPrices {
    
    /**
     * 获取物品价格
     * @param itemId 物品ID（如 "pointblank:ak47"）
     * @return 价格，如果未定义则返回0
     */
    public static int getPrice(String itemId) {
        return com.selfabandonment.mcgo.economy.pricing.WeaponPrices.getPrice(itemId);
    }

    /**
     * 检查物品是否可以购买
     */
    public static boolean canPurchase(String itemId) {
        return com.selfabandonment.mcgo.economy.pricing.WeaponPrices.canPurchase(itemId);
    }

    /**
     * 获取所有价格配置（用于调试）
     */
    public static Map<String, Integer> getAllPrices() {
        return com.selfabandonment.mcgo.economy.pricing.WeaponPrices.getAllPrices();
    }
}
