package com.qisumei.csgo.economy;

import java.util.HashMap;
import java.util.Map;

/**
 * 武器和装备价格表（参考CSGO真实价格）
 */
public class WeaponPrices {
    private static final Map<String, Integer> PRICES = new HashMap<>();

    static {
        // 手枪类 (200-700)
        PRICES.put("pointblank:glock", 200);
        PRICES.put("pointblank:usp", 200);
        PRICES.put("pointblank:p250", 300);
        PRICES.put("pointblank:fiveseven", 500);
        PRICES.put("pointblank:cz75", 500);
        PRICES.put("pointblank:tec9", 500);
        PRICES.put("pointblank:deagle", 700);
        PRICES.put("pointblank:revolver", 600);

        // 冲锋枪类 (1050-1700)
        PRICES.put("pointblank:mac10", 1050);
        PRICES.put("pointblank:mp9", 1250);
        PRICES.put("pointblank:mp7", 1500);
        PRICES.put("pointblank:ump45", 1200);
        PRICES.put("pointblank:p90", 2350);
        PRICES.put("pointblank:bizon", 1400);

        // 霰弹枪类 (1100-2000)
        PRICES.put("pointblank:nova", 1050);
        PRICES.put("pointblank:xm1014", 2000);
        PRICES.put("pointblank:mag7", 1300);
        PRICES.put("pointblank:sawedoff", 1100);

        // 步枪类 (2250-3100)
        PRICES.put("pointblank:galil", 1800);
        PRICES.put("pointblank:famas", 2050);
        PRICES.put("pointblank:ak47", 2700);
        PRICES.put("pointblank:m4a4", 3100);
        PRICES.put("pointblank:m4a1s", 2900);
        PRICES.put("pointblank:sg553", 3000);
        PRICES.put("pointblank:aug", 3300);

        // 狙击枪类 (1700-4750)
        PRICES.put("pointblank:ssg08", 1700);
        PRICES.put("pointblank:awp", 4750);
        PRICES.put("pointblank:scar20", 5000);
        PRICES.put("pointblank:g3sg1", 5000);

        // 机枪类 (2000-5200)
        PRICES.put("pointblank:m249", 5200);
        PRICES.put("pointblank:negev", 1700);

        // 投掷物 (200-600)
        PRICES.put("pointblank:flashbang", 200);
        PRICES.put("pointblank:he_grenade", 300);
        PRICES.put("pointblank:smoke_grenade", 300);
        PRICES.put("pointblank:molotov", 400);
        PRICES.put("pointblank:incendiary", 600);
        PRICES.put("pointblank:decoy", 50);

        // 护甲 (350-1000)
        PRICES.put("minecraft:leather_chestplate", 350);  // 无头护甲
        PRICES.put("minecraft:iron_chestplate", 1000);    // 全甲+头盔
        PRICES.put("minecraft:diamond_helmet", 0);        // 头盔单独（包含在全甲里）

        // 工具包
        PRICES.put("pointblank:defuse_kit", 400);

        // 近战武器（免费或特殊价格）
        PRICES.put("minecraft:wooden_sword", 0);
        PRICES.put("minecraft:stone_sword", 0);
        PRICES.put("minecraft:iron_sword", 0);

        // === Added mappings for actual pointblank IDs used by ShopGUI (scaled to two-digit economy) ===
        PRICES.put("pointblank:glock17", 2);
        PRICES.put("pointblank:m9", 3);
        PRICES.put("pointblank:deserteagle", 7);

        PRICES.put("pointblank:mp7", 15);
        PRICES.put("pointblank:ump45", 12);
        PRICES.put("pointblank:p90", 23); // ensure scaled value
        PRICES.put("pointblank:mp5", 14);
        PRICES.put("pointblank:vector", 22);

        PRICES.put("pointblank:ak47", 27);
        PRICES.put("pointblank:m4a1", 31);
        PRICES.put("pointblank:aug", 33);
        PRICES.put("pointblank:a4_sg553", 30);

        PRICES.put("pointblank:l96a1", 47);
        PRICES.put("pointblank:grenade", 3);

        // Override armor prices to match scaled economy
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
