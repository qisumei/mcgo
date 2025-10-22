package com.qisumei.csgo.game;

import com.qisumei.csgo.QisCSGO;
import com.qisumei.csgo.config.ServerConfig;
import com.qisumei.csgo.util.ItemNBTHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.HashSet;
import java.util.Set;

/**
 * 装备槽位管理器 - 处理回合开始时的装备清空与保留逻辑
 * 
 * 核心规则：
 * 1. 玩家背包（非工具栏slot）全部清空
 * 2. 仅保留工具栏特定slot的物品：
 *    - slot 0 (1): 主武器
 *    - slot 1 (2): 副武器  
 *    - slot 2 (3): 近战武器
 *    - slot 4 (5): 投掷物1
 *    - slot 5 (6): 投掷物2
 * 3. slot 3 (4) 强制清空
 * 4. 长枪限制：slot 0和slot 1最多仅存在1把长枪
 */
public final class InventorySlotManager {
    
    // Minecraft inventory layout:
    // Hotbar: slots 0-8 (displayed as slots 1-9 in UI)
    // Inventory: slots 9-35
    // Armor: slots 36-39 (feet, legs, chest, head)
    // Offhand: slot 40
    
    private static final int HOTBAR_SIZE = 9;
    private static final int HOTBAR_START = 0;
    private static final int INVENTORY_START = 9;
    private static final int INVENTORY_END = 35;
    
    // Hotbar slot indices (0-indexed)
    private static final int SLOT_PRIMARY_WEAPON = 0;   // Slot 1 in UI
    private static final int SLOT_SECONDARY_WEAPON = 1; // Slot 2 in UI
    private static final int SLOT_MELEE = 2;            // Slot 3 in UI
    private static final int SLOT_FORCE_CLEAR = 3;      // Slot 4 in UI - always cleared
    private static final int SLOT_GRENADE_1 = 4;        // Slot 5 in UI
    private static final int SLOT_GRENADE_2 = 5;        // Slot 6 in UI
    
    private InventorySlotManager() {}
    
    /**
     * 每回合开始时清空并初始化玩家装备状态
     * @param player 玩家
     */
    public static void clearAndInitializeInventory(ServerPlayer player) {
        if (player == null) return;
        
        // 1. 清空背包（非工具栏）
        clearBackpack(player);
        
        // 2. 强制清空slot 4（hotbar index 3）
        clearSlot(player, SLOT_FORCE_CLEAR);
        
        // 3. 清空未保留的hotbar slot（6-8，即UI中的7-9）
        for (int i = 6; i < HOTBAR_SIZE; i++) {
            clearSlot(player, i);
        }
        
        // 4. 长枪限制检测
        enforceLongGunLimit(player);
        
        // 5. 去除重复近战武器
        removeDuplicateMelee(player);
        
        player.getInventory().setChanged();
    }
    
    /**
     * 清空玩家背包（非工具栏slot）
     */
    private static void clearBackpack(ServerPlayer player) {
        for (int i = INVENTORY_START; i <= INVENTORY_END; i++) {
            player.getInventory().setItem(i, ItemStack.EMPTY);
        }
    }
    
    /**
     * 清空指定slot
     */
    private static void clearSlot(ServerPlayer player, int slotIndex) {
        player.getInventory().setItem(slotIndex, ItemStack.EMPTY);
    }
    
    /**
     * 长枪限制检测：确保slot 0和slot 1最多仅存在1把长枪
     */
    private static void enforceLongGunLimit(ServerPlayer player) {
        ItemStack primaryWeapon = player.getInventory().getItem(SLOT_PRIMARY_WEAPON);
        ItemStack secondaryWeapon = player.getInventory().getItem(SLOT_SECONDARY_WEAPON);
        
        boolean primaryIsLongGun = isLongGun(primaryWeapon);
        boolean secondaryIsLongGun = isLongGun(secondaryWeapon);
        
        // 如果两个slot都是长枪，清空副武器slot
        if (primaryIsLongGun && secondaryIsLongGun) {
            QisCSGO.LOGGER.info("玩家 {} 持有多把长枪，清空副武器槽", player.getName().getString());
            clearSlot(player, SLOT_SECONDARY_WEAPON);
        }
    }
    
    /**
     * 判断物品是否为长枪（步枪、重型武器、AWP）
     */
    private static boolean isLongGun(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        
        // 检查是否为步枪
        for (String weaponId : ServerConfig.weaponsRifle) {
            if (ItemNBTHelper.idMatches(stack, weaponId)) {
                return true;
            }
        }
        
        // 检查是否为重型武器
        for (String weaponId : ServerConfig.weaponsHeavy) {
            if (ItemNBTHelper.idMatches(stack, weaponId)) {
                return true;
            }
        }
        
        // 检查是否为AWP
        for (String weaponId : ServerConfig.weaponsAwp) {
            if (ItemNBTHelper.idMatches(stack, weaponId)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 去除重复的近战武器，只保留第一把
     */
    private static void removeDuplicateMelee(ServerPlayer player) {
        boolean foundMelee = false;
        
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.isEmpty()) continue;
            
            // 检查是否为近战武器
            boolean isMelee = false;
            for (String weaponId : ServerConfig.weaponsKnife) {
                if (ItemNBTHelper.idMatches(stack, weaponId)) {
                    isMelee = true;
                    break;
                }
            }
            
            if (isMelee) {
                if (foundMelee) {
                    // 已经有一把近战了，清除这把
                    player.getInventory().setItem(i, ItemStack.EMPTY);
                } else {
                    foundMelee = true;
                }
            }
        }
    }
    
    /**
     * 检查并清理已使用的投掷物
     * 注意：这个方法应该在投掷物使用后调用，而不是在回合开始时
     */
    public static void markGrenadeAsUsed(ServerPlayer player, ItemStack grenade) {
        // 这个方法的实现将在投掷物系统中完成
        // 当前仅作为接口预留
    }
    
    /**
     * 获取玩家当前持有的长枪数量
     */
    public static int countLongGuns(ServerPlayer player) {
        int count = 0;
        
        ItemStack primaryWeapon = player.getInventory().getItem(SLOT_PRIMARY_WEAPON);
        ItemStack secondaryWeapon = player.getInventory().getItem(SLOT_SECONDARY_WEAPON);
        
        if (isLongGun(primaryWeapon)) count++;
        if (isLongGun(secondaryWeapon)) count++;
        
        return count;
    }
}
