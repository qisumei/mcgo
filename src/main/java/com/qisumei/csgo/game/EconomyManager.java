package com.qisumei.csgo.game;

import com.qisumei.csgo.config.ServerConfig;
import com.qisumei.csgo.economy.VirtualMoneyManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import com.qisumei.csgo.util.ItemNBTHelper;

/**
 * 经济系统管理类 - 使用虚拟货币系统
 * 不再使用钻石，改用内存中的虚拟货币
 * 
 * <p>改进点：
 * <ul>
 *   <li>使用Java 21的var简化局部变量声明</li>
 *   <li>改进了空值检查和参数验证</li>
 *   <li>优化了武器类型检测逻辑</li>
 * </ul>
 */
public final class EconomyManager {
    
    // 私有构造器防止实例化
    private EconomyManager() {
        throw new AssertionError("Utility class should not be instantiated");
    }

    /**
     * 向指定玩家发放一定数量的游戏货币（虚拟货币）
     * 
     * @param player 玩家对象，不能为null
     * @param amount 货币数量，必须为正数
     * @throws IllegalArgumentException 如果player为null
     */
    public static void giveMoney(ServerPlayer player, int amount) {
        if (player == null) {
            throw new IllegalArgumentException("Player cannot be null");
        }
        if (amount <= 0) return;

        var moneyManager = VirtualMoneyManager.getInstance();
        moneyManager.addMoney(player, amount);

        // 显示货币变化提示
        var currentMoney = moneyManager.getMoney(player);
        player.sendSystemMessage(
            Component.literal("§a+$" + amount + " §7(余额: §e$" + currentMoney + "§7)"),
            true
        );
    }

    /**
     * 扣除玩家货币
     * 
     * @param player 玩家对象，不能为null
     * @param amount 要扣除的货币数量
     * @return 如果扣除成功返回true，余额不足返回false
     * @throws IllegalArgumentException 如果player为null
     */
    public static boolean takeMoney(ServerPlayer player, int amount) {
        if (player == null) {
            throw new IllegalArgumentException("Player cannot be null");
        }
        if (amount <= 0) return true;

        var moneyManager = VirtualMoneyManager.getInstance();
        var success = moneyManager.takeMoney(player, amount);

        if (success) {
            var currentMoney = moneyManager.getMoney(player);
            player.sendSystemMessage(
                Component.literal("§c-$" + amount + " §7(余额: §e$" + currentMoney + "§7)"),
                true
            );
        } else {
            player.sendSystemMessage(
                Component.literal("§c余额不足！需要 $" + amount).withStyle(ChatFormatting.RED),
                false
            );
        }

        return success;
    }

    /**
     * 获取玩家当前货币
     * 
     * @param player 玩家对象，不能为null
     * @return 当前货币数量
     * @throws IllegalArgumentException 如果player为null
     */
    public static int getMoney(ServerPlayer player) {
        if (player == null) {
            throw new IllegalArgumentException("Player cannot be null");
        }
        return VirtualMoneyManager.getInstance().getMoney(player);
    }

    /**
     * 设置玩家货币（用于比赛初始化）
     * 
     * @param player 玩家对象，不能为null
     * @param amount 货币数量，负数会被转换为0
     * @throws IllegalArgumentException 如果player为null
     */
    public static void setMoney(ServerPlayer player, int amount) {
        if (player == null) {
            throw new IllegalArgumentException("Player cannot be null");
        }
        VirtualMoneyManager.getInstance().setMoney(player, amount);
        player.sendSystemMessage(
            Component.literal("§a货币已设置为 §e$" + amount),
            false
        );
    }

    /**
     * 清除玩家所有货币
     * 
     * @param player 玩家对象，不能为null
     * @throws IllegalArgumentException 如果player为null
     */
    public static void clearMoney(ServerPlayer player) {
        if (player == null) {
            throw new IllegalArgumentException("Player cannot be null");
        }
        VirtualMoneyManager.getInstance().clearMoney(player);
    }

    /**
     * 根据使用的武器类型获取击杀奖励金额
     * 使用Java 21的增强模式匹配优化武器类型检测
     * 
     * @param weapon 武器物品栈，可以为null或empty
     * @return 击杀奖励金额
     */
    public static int getRewardForKill(ItemStack weapon) {
        if (weapon == null || weapon.isEmpty()) {
            return ServerConfig.killRewardPistol;
        }

        // 使用早期返回模式优化可读性
        if (matchesWeaponCategory(weapon, ServerConfig.weaponsKnife)) {
            return ServerConfig.killRewardKnife;
        }
        if (matchesWeaponCategory(weapon, ServerConfig.weaponsPistol)) {
            return ServerConfig.killRewardPistol;
        }
        if (matchesWeaponCategory(weapon, ServerConfig.weaponsSmg)) {
            return ServerConfig.killRewardSmg;
        }
        if (matchesWeaponCategory(weapon, ServerConfig.weaponsHeavy)) {
            return ServerConfig.killRewardHeavy;
        }
        if (matchesWeaponCategory(weapon, ServerConfig.weaponsRifle)) {
            return ServerConfig.killRewardRifle;
        }
        if (matchesWeaponCategory(weapon, ServerConfig.weaponsAwp)) {
            return ServerConfig.killRewardAwp;
        }
        if (matchesWeaponCategory(weapon, ServerConfig.weaponsGrenade)) {
            return ServerConfig.killRewardGrenade;
        }

        return ServerConfig.killRewardPistol; // 默认奖励
    }
    
    /**
     * 辅助方法：检查武器是否匹配指定类别
     * 
     * @param weapon 武器物品栈
     * @param category 武器类别列表
     * @return 如果匹配返回true
     */
    private static boolean matchesWeaponCategory(ItemStack weapon, java.util.List<String> category) {
        return category != null && category.stream()
            .anyMatch(id -> ItemNBTHelper.idMatches(weapon, id));
    }
}
