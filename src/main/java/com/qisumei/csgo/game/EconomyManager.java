package com.qisumei.csgo.game;

import com.qisumei.csgo.config.ServerConfig;
import com.qisumei.csgo.economy.VirtualMoneyManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import com.qisumei.csgo.util.ItemNBTHelper;

import java.util.Objects;

/**
 * 经济系统管理类 - 使用虚拟货币系统。
 * 不再使用钻石，改用内存中的虚拟货币。
 * 注意：这个类主要用于向后兼容，新代码应该使用 EconomyService 接口。
 */
public final class EconomyManager {
    
    private EconomyManager() {
        // 私有构造函数防止实例化
    }
    /**
     * 向指定玩家发放一定数量的游戏货币（虚拟货币）。
     * @param player 玩家对象（不能为 null）
     * @param amount 货币数量（必须 > 0）
     * @throws NullPointerException 如果 player 为 null
     */
    public static void giveMoney(ServerPlayer player, int amount) {
        Objects.requireNonNull(player, "Player cannot be null");
        if (amount <= 0) return;

        VirtualMoneyManager moneyManager = VirtualMoneyManager.getInstance();
        moneyManager.addMoney(player, amount);

        // 显示货币变化提示
        int currentMoney = moneyManager.getMoney(player);
        player.sendSystemMessage(
            Component.literal("§a+$" + amount + " §7(余额: §e$" + currentMoney + "§7)"),
            true
        );
    }
    /**
     * 扣除玩家货币。
     * @param player 玩家对象（不能为 null）
     * @param amount 要扣除的货币数量（必须 > 0）
     * @return 如果扣除成功返回 true，余额不足返回 false
     * @throws NullPointerException 如果 player 为 null
     */
    public static boolean takeMoney(ServerPlayer player, int amount) {
        Objects.requireNonNull(player, "Player cannot be null");
        if (amount <= 0) return true;

        VirtualMoneyManager moneyManager = VirtualMoneyManager.getInstance();
        boolean success = moneyManager.takeMoney(player, amount);

        if (success) {
            int currentMoney = moneyManager.getMoney(player);
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
     * 获取玩家当前货币。
     * @param player 玩家对象（不能为 null）
     * @return 当前货币数量
     * @throws NullPointerException 如果 player 为 null
     */
    public static int getMoney(ServerPlayer player) {
        Objects.requireNonNull(player, "Player cannot be null");
        return VirtualMoneyManager.getInstance().getMoney(player);
    }

    /**
     * 设置玩家货币（用于比赛初始化）。
     * @param player 玩家对象（不能为 null）
     * @param amount 货币数量
     * @throws NullPointerException 如果 player 为 null
     */
    public static void setMoney(ServerPlayer player, int amount) {
        Objects.requireNonNull(player, "Player cannot be null");
        VirtualMoneyManager.getInstance().setMoney(player, amount);
        player.sendSystemMessage(
            Component.literal("§a货币已设置为 §e$" + amount),
            false
        );
    }

    /**
     * 清除玩家所有货币。
     * @param player 玩家对象（不能为 null）
     * @throws NullPointerException 如果 player 为 null
     */
    public static void clearMoney(ServerPlayer player) {
        Objects.requireNonNull(player, "Player cannot be null");
        VirtualMoneyManager.getInstance().clearMoney(player);
    }

    /**
     * 根据使用的武器类型获取击杀奖励金额。
     * 使用 Java 21 的 stream 和模式匹配来简化武器类型判断。
     * 
     * @param weapon 武器物品栈
     * @return 击杀奖励金额
     */
    public static int getRewardForKill(ItemStack weapon) {
        if (weapon == null || weapon.isEmpty()) {
            return ServerConfig.killRewardPistol;
        }

        // 使用方法引用简化代码
        if (ServerConfig.weaponsKnife.stream().anyMatch(id -> ItemNBTHelper.idMatches(weapon, id))) {
            return ServerConfig.killRewardKnife;
        }
        if (ServerConfig.weaponsPistol.stream().anyMatch(id -> ItemNBTHelper.idMatches(weapon, id))) {
            return ServerConfig.killRewardPistol;
        }
        if (ServerConfig.weaponsSmg.stream().anyMatch(id -> ItemNBTHelper.idMatches(weapon, id))) {
            return ServerConfig.killRewardSmg;
        }
        if (ServerConfig.weaponsHeavy.stream().anyMatch(id -> ItemNBTHelper.idMatches(weapon, id))) {
            return ServerConfig.killRewardHeavy;
        }
        if (ServerConfig.weaponsRifle.stream().anyMatch(id -> ItemNBTHelper.idMatches(weapon, id))) {
            return ServerConfig.killRewardRifle;
        }
        if (ServerConfig.weaponsAwp.stream().anyMatch(id -> ItemNBTHelper.idMatches(weapon, id))) {
            return ServerConfig.killRewardAwp;
        }
        if (ServerConfig.weaponsGrenade.stream().anyMatch(id -> ItemNBTHelper.idMatches(weapon, id))) {
            return ServerConfig.killRewardGrenade;
        }

        // 默认返回手枪击杀奖励
        return ServerConfig.killRewardPistol;
    }
}
