package com.qisumei.csgo.events.match;

import com.qisumei.csgo.QisCSGO;
import com.qisumei.csgo.config.ServerConfig;
import com.qisumei.csgo.economy.VirtualMoneyManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.UUID;

/**
 * 经济事件处理器 - 监听比赛事件并处理相应的经济逻辑
 * 实现了资金系统与换边逻辑的解耦
 */
public class EconomyEventHandler implements MatchEventListener {
    
    /**
     * 资金清空策略枚举
     */
    public enum MoneyClearStrategy {
        /** 清空所有资金 */
        CLEAR_ALL,
        /** 仅清空当前回合临时资金（保留基础资金）*/
        CLEAR_TEMPORARY_ONLY,
        /** 保留所有资金（不清空）*/
        KEEP_ALL,
        /** 重置为手枪局起始资金 */
        RESET_TO_PISTOL_ROUND
    }
    
    private final VirtualMoneyManager moneyManager;
    private final MoneyClearStrategy strategy;
    
    /**
     * 创建经济事件处理器
     * @param strategy 资金清空策略
     */
    public EconomyEventHandler(MoneyClearStrategy strategy) {
        this.moneyManager = VirtualMoneyManager.getInstance();
        this.strategy = strategy;
    }
    
    /**
     * 使用默认策略（重置为手枪局起始资金）创建处理器
     */
    public EconomyEventHandler() {
        this(MoneyClearStrategy.RESET_TO_PISTOL_ROUND);
    }
    
    @Override
    public void onTeamSwap(TeamSwapEvent event) {
        QisCSGO.LOGGER.info("处理换边事件的资金清空，策略: {}", strategy);
        
        Map<UUID, ServerPlayer> players = event.getAffectedPlayers();
        
        for (Map.Entry<UUID, ServerPlayer> entry : players.entrySet()) {
            ServerPlayer player = entry.getValue();
            if (player == null) continue;
            
            handlePlayerMoneyOnSwap(player);
        }
    }
    
    /**
     * 根据策略处理玩家换边时的资金
     * @param player 玩家
     */
    private void handlePlayerMoneyOnSwap(ServerPlayer player) {
        int currentMoney = moneyManager.getMoney(player);
        
        switch (strategy) {
            case CLEAR_ALL:
                moneyManager.clearMoney(player);
                player.sendSystemMessage(
                    Component.literal("§c换边：所有资金已清空").withStyle(ChatFormatting.RED),
                    false
                );
                QisCSGO.LOGGER.debug("玩家 {} 换边，清空所有资金（原有: ${}）", 
                    player.getName().getString(), currentMoney);
                break;
                
            case CLEAR_TEMPORARY_ONLY:
                // 保留基础资金（手枪局起始资金），清空额外获得的资金
                int baseAmount = ServerConfig.pistolRoundStartingMoney;
                if (currentMoney > baseAmount) {
                    moneyManager.setMoney(player, baseAmount);
                    player.sendSystemMessage(
                        Component.literal("§e换边：保留基础资金 $" + baseAmount + "，清空临时资金").withStyle(ChatFormatting.YELLOW),
                        false
                    );
                    QisCSGO.LOGGER.debug("玩家 {} 换边，保留基础资金 ${}（原有: ${}）", 
                        player.getName().getString(), baseAmount, currentMoney);
                } else {
                    player.sendSystemMessage(
                        Component.literal("§a换边：保留当前资金 $" + currentMoney).withStyle(ChatFormatting.GREEN),
                        false
                    );
                }
                break;
                
            case KEEP_ALL:
                player.sendSystemMessage(
                    Component.literal("§a换边：保留所有资金 $" + currentMoney).withStyle(ChatFormatting.GREEN),
                    false
                );
                QisCSGO.LOGGER.debug("玩家 {} 换边，保留所有资金 ${}", 
                    player.getName().getString(), currentMoney);
                break;
                
            case RESET_TO_PISTOL_ROUND:
                int pistolMoney = ServerConfig.pistolRoundStartingMoney;
                moneyManager.setMoney(player, pistolMoney);
                player.sendSystemMessage(
                    Component.literal("§6换边：资金重置为手枪局起始资金 $" + pistolMoney).withStyle(ChatFormatting.GOLD),
                    false
                );
                QisCSGO.LOGGER.debug("玩家 {} 换边，重置为手枪局资金 ${}（原有: ${}）", 
                    player.getName().getString(), pistolMoney, currentMoney);
                break;
        }
    }
    
    @Override
    public void onRoundStart(RoundStartEvent event) {
        // 可以在这里添加回合开始时的经济处理逻辑
        // 例如：手枪局特殊处理、连胜连败奖励等
    }
    
    /**
     * 获取当前使用的资金清空策略
     */
    public MoneyClearStrategy getStrategy() {
        return strategy;
    }
}
