package com.qisumei.csgo.economy;

import java.util.HashMap;
import java.util.Map;

/**
 * 武器和装备价格表（与 ShopGUI 使用的物品ID保持一致；采用缩放后的两位数经济）
 */
public class WeaponPrices {
    private static final Map<String, Integer> PRICES = new HashMap<>();

    static {
        // 手枪（缩放）
        PRICES.put("pointblank:glock17", 2);
        PRICES.put("pointblank:m9", 3);
        PRICES.put("pointblank:deserteagle", 7);

        // 冲锋枪（缩放）
        PRICES.put("pointblank:mp7", 15);
        PRICES.put("pointblank:ump45", 12);
        PRICES.put("pointblank:p90", 23);
        PRICES.put("pointblank:mp5", 14);
        PRICES.put("pointblank:vector", 22);

        // 步枪（缩放）
        PRICES.put("pointblank:ak47", 27);
        PRICES.put("pointblank:m4a1", 31);
        PRICES.put("pointblank:aug", 33);
        PRICES.put("pointblank:a4_sg553", 30);

        // 狙击
        PRICES.put("pointblank:l96a1", 47);

        // 投掷物
        PRICES.put("pointblank:grenade", 3);

        // 护甲与工具（缩放）
        PRICES.put("minecraft:leather_chestplate", 3);
        PRICES.put("minecraft:iron_chestplate", 10);
    }

    /**
     * 获取物品价格
     * @param itemId 物品ID（如 "pointblank:ak47"）
     * @return 价格，如果未定义则返回0
     */
    public static int getPrice(String itemId) {
        return PRICES.getOrDefault(itemId, 0);
    }

    /**
     * 检查物品是否可以购买
     */
    public static boolean canPurchase(String itemId) {
        return PRICES.containsKey(itemId);
    }

    /**
     * 获取所有价格配置（用于调试）
     */
    public static Map<String, Integer> getAllPrices() {
        return new HashMap<>(PRICES);
    }
}
