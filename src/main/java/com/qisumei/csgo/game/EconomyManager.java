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
 */
public class EconomyManager {

    /**
     * 向指定玩家发放一定数量的游戏货币（虚拟货币）
     */
    public static void giveMoney(ServerPlayer player, int amount) {
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
     * 扣除玩家货币
     */
    public static boolean takeMoney(ServerPlayer player, int amount) {
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
     * 获取玩家当前货币
     */
    public static int getMoney(ServerPlayer player) {
        return VirtualMoneyManager.getInstance().getMoney(player);
    }

    /**
     * 设置玩家货币（用于比赛初始化）
     */
    public static void setMoney(ServerPlayer player, int amount) {
        VirtualMoneyManager.getInstance().setMoney(player, amount);
        player.sendSystemMessage(
            Component.literal("§a货币已设置为 §e$" + amount),
            false
        );
    }

    /**
     * 清除玩家所有货币
     */
    public static void clearMoney(ServerPlayer player) {
        VirtualMoneyManager.getInstance().clearMoney(player);
    }

    /**
     * 根据使用的武器类型获取击杀奖励金额
     */
    public static int getRewardForKill(ItemStack weapon) {
        if (weapon.isEmpty()) return ServerConfig.killRewardPistol;

        if (ServerConfig.weaponsKnife.stream().anyMatch(s -> ItemNBTHelper.idMatches(weapon, s))) return ServerConfig.killRewardKnife;
        if (ServerConfig.weaponsPistol.stream().anyMatch(s -> ItemNBTHelper.idMatches(weapon, s))) return ServerConfig.killRewardPistol;
        if (ServerConfig.weaponsSmg.stream().anyMatch(s -> ItemNBTHelper.idMatches(weapon, s))) return ServerConfig.killRewardSmg;
        if (ServerConfig.weaponsHeavy.stream().anyMatch(s -> ItemNBTHelper.idMatches(weapon, s))) return ServerConfig.killRewardHeavy;
        if (ServerConfig.weaponsRifle.stream().anyMatch(s -> ItemNBTHelper.idMatches(weapon, s))) return ServerConfig.killRewardRifle;
        if (ServerConfig.weaponsAwp.stream().anyMatch(s -> ItemNBTHelper.idMatches(weapon, s))) return ServerConfig.killRewardAwp;
        if (ServerConfig.weaponsGrenade.stream().anyMatch(s -> ItemNBTHelper.idMatches(weapon, s))) return ServerConfig.killRewardGrenade;

        return ServerConfig.killRewardPistol;
    }
}
