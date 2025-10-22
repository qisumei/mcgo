package com.qisumei.csgo.game;

import com.qisumei.csgo.QisCSGO;
import com.qisumei.csgo.config.ServerConfig;
import com.qisumei.csgo.util.ItemNBTHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 购买限制管理器 - 处理每回合的购买限制
 * 
 * 核心功能：
 * 1. 武器限购：所有武器（含手枪）单回合限购1把
 * 2. 护甲限购：护甲/头盔单回合限购1套
 * 3. 投掷物限购：每种投掷物单回合限购1个
 * 4. 子弹限购：单回合子弹购买上限 = 武器弹夹容量 × 4
 */
public final class PurchaseLimitManager {
    
    // 武器类型枚举
    public enum WeaponCategory {
        KNIFE,
        PISTOL,
        SMG,
        HEAVY,
        RIFLE,
        AWP,
        GRENADE
    }
    
    // 投掷物类型枚举
    public enum GrenadeType {
        FRAG_GRENADE("手雷"),
        FLASH_GRENADE("闪光弹"),
        SMOKE_GRENADE("烟雾弹"),
        MOLOTOV("燃烧弹"),
        DECOY("诱饵弹");
        
        private final String displayName;
        
        GrenadeType(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    // 玩家购买记录 <玩家UUID, 购买记录>
    private final Map<UUID, PlayerPurchaseRecord> playerPurchases = new ConcurrentHashMap<>();
    
    /**
     * 单个玩家的购买记录
     */
    private static class PlayerPurchaseRecord {
        private boolean weaponPurchased = false;
        private boolean armorPurchased = false;
        private final Map<GrenadeType, Integer> grenadePurchases = new EnumMap<>(GrenadeType.class);
        private int bulletsPurchased = 0;
        
        public boolean hasWeapon() {
            return weaponPurchased;
        }
        
        public void markWeaponPurchased() {
            weaponPurchased = true;
        }
        
        public boolean hasArmor() {
            return armorPurchased;
        }
        
        public void markArmorPurchased() {
            armorPurchased = true;
        }
        
        public int getGrenadePurchases(GrenadeType type) {
            return grenadePurchases.getOrDefault(type, 0);
        }
        
        public void incrementGrenadePurchase(GrenadeType type) {
            grenadePurchases.put(type, grenadePurchases.getOrDefault(type, 0) + 1);
        }
        
        public int getBulletsPurchased() {
            return bulletsPurchased;
        }
        
        public void addBulletsPurchased(int amount) {
            bulletsPurchased += amount;
        }
    }
    
    /**
     * 重置所有玩家的购买记录（每回合开始时调用）
     */
    public void resetAllPurchases() {
        playerPurchases.clear();
        QisCSGO.LOGGER.debug("已重置所有玩家购买记录");
    }
    
    /**
     * 获取玩家的购买记录，如果不存在则创建
     */
    private PlayerPurchaseRecord getOrCreateRecord(UUID playerUUID) {
        return playerPurchases.computeIfAbsent(playerUUID, k -> new PlayerPurchaseRecord());
    }
    
    /**
     * 检查玩家是否可以购买武器
     * @return true if allowed, false if limit reached
     */
    public boolean canPurchaseWeapon(ServerPlayer player) {
        PlayerPurchaseRecord record = getOrCreateRecord(player.getUUID());
        return !record.hasWeapon();
    }
    
    /**
     * 记录玩家购买了武器
     */
    public void recordWeaponPurchase(ServerPlayer player) {
        PlayerPurchaseRecord record = getOrCreateRecord(player.getUUID());
        record.markWeaponPurchased();
        QisCSGO.LOGGER.debug("玩家 {} 本回合已购买武器", player.getName().getString());
    }
    
    /**
     * 检查玩家是否可以购买护甲
     */
    public boolean canPurchaseArmor(ServerPlayer player) {
        PlayerPurchaseRecord record = getOrCreateRecord(player.getUUID());
        return !record.hasArmor();
    }
    
    /**
     * 记录玩家购买了护甲
     */
    public void recordArmorPurchase(ServerPlayer player) {
        PlayerPurchaseRecord record = getOrCreateRecord(player.getUUID());
        record.markArmorPurchased();
        QisCSGO.LOGGER.debug("玩家 {} 本回合已购买护甲", player.getName().getString());
    }
    
    /**
     * 检查玩家是否可以购买指定类型的投掷物
     */
    public boolean canPurchaseGrenade(ServerPlayer player, GrenadeType type) {
        PlayerPurchaseRecord record = getOrCreateRecord(player.getUUID());
        return record.getGrenadePurchases(type) < 1;
    }
    
    /**
     * 记录玩家购买了投掷物
     */
    public void recordGrenadePurchase(ServerPlayer player, GrenadeType type) {
        PlayerPurchaseRecord record = getOrCreateRecord(player.getUUID());
        record.incrementGrenadePurchase(type);
        QisCSGO.LOGGER.debug("玩家 {} 本回合购买了 {}", player.getName().getString(), type.getDisplayName());
    }
    
    /**
     * 检查玩家是否可以购买指定数量的子弹
     * @param magazineCapacity 武器弹夹容量
     * @param amount 要购买的子弹数量
     */
    public boolean canPurchaseBullets(ServerPlayer player, int magazineCapacity, int amount) {
        PlayerPurchaseRecord record = getOrCreateRecord(player.getUUID());
        int maxBullets = magazineCapacity * 4;
        return (record.getBulletsPurchased() + amount) <= maxBullets;
    }
    
    /**
     * 记录玩家购买了子弹
     */
    public void recordBulletPurchase(ServerPlayer player, int amount) {
        PlayerPurchaseRecord record = getOrCreateRecord(player.getUUID());
        record.addBulletsPurchased(amount);
        QisCSGO.LOGGER.debug("玩家 {} 本回合已购买 {} 发子弹", player.getName().getString(), record.getBulletsPurchased());
    }
    
    /**
     * 获取剩余可购买的子弹数量
     */
    public int getRemainingBullets(ServerPlayer player, int magazineCapacity) {
        PlayerPurchaseRecord record = getOrCreateRecord(player.getUUID());
        int maxBullets = magazineCapacity * 4;
        return Math.max(0, maxBullets - record.getBulletsPurchased());
    }
    
    /**
     * 判断物品是否为武器
     */
    public static boolean isWeapon(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        
        // 检查各类武器
        return isWeaponOfCategory(stack, WeaponCategory.PISTOL) ||
               isWeaponOfCategory(stack, WeaponCategory.SMG) ||
               isWeaponOfCategory(stack, WeaponCategory.HEAVY) ||
               isWeaponOfCategory(stack, WeaponCategory.RIFLE) ||
               isWeaponOfCategory(stack, WeaponCategory.AWP);
    }
    
    /**
     * 判断物品是否为指定类别的武器
     */
    public static boolean isWeaponOfCategory(ItemStack stack, WeaponCategory category) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        
        List<String> weaponList = switch (category) {
            case KNIFE -> ServerConfig.weaponsKnife;
            case PISTOL -> ServerConfig.weaponsPistol;
            case SMG -> ServerConfig.weaponsSmg;
            case HEAVY -> ServerConfig.weaponsHeavy;
            case RIFLE -> ServerConfig.weaponsRifle;
            case AWP -> ServerConfig.weaponsAwp;
            case GRENADE -> ServerConfig.weaponsGrenade;
        };
        
        return weaponList.stream().anyMatch(id -> ItemNBTHelper.idMatches(stack, id));
    }
    
    /**
     * 判断物品是否为投掷物
     */
    public static boolean isGrenade(ItemStack stack) {
        return isWeaponOfCategory(stack, WeaponCategory.GRENADE);
    }
    
    /**
     * 获取投掷物类型（简单实现，可根据实际物品ID扩展）
     */
    public static GrenadeType getGrenadeType(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        
        String itemId = stack.getDescriptionId().toLowerCase();
        
        // 根据物品ID判断类型（需要根据实际模组的物品ID调整）
        if (itemId.contains("grenade") || itemId.contains("frag")) {
            return GrenadeType.FRAG_GRENADE;
        } else if (itemId.contains("flash")) {
            return GrenadeType.FLASH_GRENADE;
        } else if (itemId.contains("smoke")) {
            return GrenadeType.SMOKE_GRENADE;
        } else if (itemId.contains("molotov") || itemId.contains("incendiary")) {
            return GrenadeType.MOLOTOV;
        } else if (itemId.contains("decoy")) {
            return GrenadeType.DECOY;
        }
        
        // 默认返回手雷类型
        return GrenadeType.FRAG_GRENADE;
    }
    
    /**
     * 判断物品是否为护甲
     */
    public static boolean isArmor(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        
        // 检查是否为铁质护甲（头盔、胸甲、护腿、靴子）
        return stack.getItem() == net.minecraft.world.item.Items.IRON_HELMET ||
               stack.getItem() == net.minecraft.world.item.Items.IRON_CHESTPLATE ||
               stack.getItem() == net.minecraft.world.item.Items.IRON_LEGGINGS ||
               stack.getItem() == net.minecraft.world.item.Items.IRON_BOOTS;
    }
}
