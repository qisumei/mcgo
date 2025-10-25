package com.qisumei.csgo.game;

import com.qisumei.csgo.config.ServerConfig;
import com.qisumei.csgo.service.EconomyService;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Objects;

/**
 * 回合经济服务 - 处理回合开始时的经济分配逻辑
 * 从 Match.java 中解耦出来，提高代码可维护性
 */
public class RoundEconomyService {
    
    private final EconomyService economyService;
    
    public RoundEconomyService(EconomyService economyService) {
        this.economyService = Objects.requireNonNull(economyService, "economyService cannot be null");
    }
    
    /**
     * 为手枪局分配起始资金
     * @param player 玩家
     */
    public void distributePistolRoundMoney(ServerPlayer player) {
        economyService.setMoney(player, ServerConfig.pistolRoundStartingMoney);
        player.sendSystemMessage(
            Component.literal("§6手枪局！起始资金: §e$" + ServerConfig.pistolRoundStartingMoney)
                .withStyle(ChatFormatting.AQUA)
        );
    }
    
    /**
     * 为普通回合分配收入（基于上回合结果）
     * @param player 玩家
     * @param stats 玩家统计数据
     * @param lastRoundWinner 上回合获胜方
     */
    public void distributeRoundIncome(ServerPlayer player, PlayerStats stats, String lastRoundWinner) {
        boolean wasWinner = stats.getTeam().equals(lastRoundWinner);
        int income;
        
        if (wasWinner) {
            income = ServerConfig.winReward;
            player.sendSystemMessage(
                Component.literal("§a回合胜利！获得 §e$" + income).withStyle(ChatFormatting.GREEN)
            );
        } else {
            // 计算连败奖励
            int lossBonus = Math.min(
                stats.getConsecutiveLosses() * ServerConfig.lossStreakBonus,
                ServerConfig.maxLossStreakBonus
            );
            income = ServerConfig.lossReward + lossBonus;
            player.sendSystemMessage(
                Component.literal("§c回合失败。获得 §e$" + income + " §7(含连败奖励)")
                    .withStyle(ChatFormatting.RED)
            );
        }
        
        economyService.giveMoney(player, income);
    }
    
    /**
     * 为回合胜利分配奖励
     * @param player 玩家
     */
    public void distributeWinReward(ServerPlayer player) {
        int reward = ServerConfig.winReward;
        economyService.giveMoney(player, reward);
        player.sendSystemMessage(
            Component.literal("你们赢得了本回合！获得奖励：" + reward + " 货币")
                .withStyle(ChatFormatting.GREEN)
        );
    }
    
    /**
     * 根据击杀武器分配击杀奖励
     * @param player 玩家
     * @param weapon 击杀使用的武器（null表示空手/默认）
     */
    public void distributeKillReward(ServerPlayer player, net.minecraft.world.item.ItemStack weapon) {
        int reward = economyService.getRewardForKill(weapon);
        if (reward > 0) {
            economyService.giveMoney(player, reward);
            player.sendSystemMessage(
                Component.literal("§a+$" + reward + " §7(击杀奖励)")
                    .withStyle(ChatFormatting.GREEN),
                true
            );
        }
    }
}
