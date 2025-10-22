package com.qisumei.csgo.game;

import com.qisumei.csgo.QisCSGO;
import com.qisumei.csgo.economy.VirtualMoneyManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 经济平衡管理器 - 处理经济平衡相关逻辑
 * 
 * 核心功能：
 * 1. 初始资金统一化：每局开局10000，每回合5000+奖励
 * 2. 死亡惩罚：死亡损失20%当前资金
 * 3. 动态平衡：连续3回合胜利后触发资金调整
 * 4. 标准化收益：固定击杀奖励、任务奖励、回合胜利奖励
 */
public final class EconomicBalanceManager {
    
    // 经济配置常量
    private static final int GAME_START_MONEY = 10000;        // 游戏开局初始资金
    private static final int ROUND_BASE_MONEY = 5000;         // 每回合基础资金
    private static final int KILL_REWARD = 300;               // 统一击杀奖励
    private static final int C4_PLANT_REWARD = 800;           // C4安放奖励
    private static final int C4_DEFUSE_REWARD = 800;          // C4拆除奖励
    private static final int ROUND_WIN_REWARD = 1000;         // 回合胜利奖励
    private static final double DEATH_PENALTY_RATE = 0.20;    // 死亡惩罚比例（20%）
    private static final int CONSECUTIVE_WIN_THRESHOLD = 3;   // 连续胜利触发动态平衡的阈值
    private static final double DYNAMIC_BALANCE_RATE = 0.10;  // 动态平衡调整比例（10%）
    
    // 跟踪每个队伍的连续胜利次数
    private final Map<String, Integer> consecutiveWins = new HashMap<>();
    
    public EconomicBalanceManager() {
        consecutiveWins.put("CT", 0);
        consecutiveWins.put("T", 0);
    }
    
    /**
     * 游戏开始时设置玩家初始资金
     */
    public void setGameStartMoney(ServerPlayer player) {
        VirtualMoneyManager.getInstance().setMoney(player, GAME_START_MONEY);
        player.sendSystemMessage(
            Component.literal("§6游戏开始！初始资金: §e$" + GAME_START_MONEY)
                .withStyle(ChatFormatting.GOLD)
        );
        QisCSGO.LOGGER.info("为玩家 {} 设置游戏初始资金: {}", player.getName().getString(), GAME_START_MONEY);
    }
    
    /**
     * 回合开始时分配基础资金（不是手枪局）
     * @param player 玩家
     * @param currentMoney 当前资金（用于保留奖励）
     */
    public void distributeRoundBaseMoney(ServerPlayer player, int currentMoney) {
        VirtualMoneyManager moneyManager = VirtualMoneyManager.getInstance();
        
        // 计算奖励部分（当前资金超过基础资金的部分）
        int rewardPortion = Math.max(0, currentMoney - ROUND_BASE_MONEY);
        
        // 重置为基础资金 + 奖励
        int newMoney = ROUND_BASE_MONEY + rewardPortion;
        moneyManager.setMoney(player, newMoney);
        
        player.sendSystemMessage(
            Component.literal("§6回合开始！基础资金: §e$" + ROUND_BASE_MONEY + 
                            (rewardPortion > 0 ? " §7(+上回合奖励 $" + rewardPortion + ")" : ""))
                .withStyle(ChatFormatting.AQUA)
        );
    }
    
    /**
     * 发放统一的击杀奖励
     */
    public void giveKillReward(ServerPlayer killer) {
        VirtualMoneyManager.getInstance().addMoney(killer, KILL_REWARD);
        killer.sendSystemMessage(
            Component.literal("§a+$" + KILL_REWARD + " §7(击杀奖励)")
                .withStyle(ChatFormatting.GREEN),
            true
        );
        QisCSGO.LOGGER.debug("玩家 {} 获得击杀奖励: {}", killer.getName().getString(), KILL_REWARD);
    }
    
    /**
     * 发放C4安放奖励
     */
    public void giveC4PlantReward(ServerPlayer player) {
        VirtualMoneyManager.getInstance().addMoney(player, C4_PLANT_REWARD);
        player.sendSystemMessage(
            Component.literal("§a+$" + C4_PLANT_REWARD + " §7(C4安放奖励)")
                .withStyle(ChatFormatting.GREEN)
        );
        QisCSGO.LOGGER.info("玩家 {} 获得C4安放奖励: {}", player.getName().getString(), C4_PLANT_REWARD);
    }
    
    /**
     * 发放C4拆除奖励
     */
    public void giveC4DefuseReward(ServerPlayer player) {
        VirtualMoneyManager.getInstance().addMoney(player, C4_DEFUSE_REWARD);
        player.sendSystemMessage(
            Component.literal("§a+$" + C4_DEFUSE_REWARD + " §7(C4拆除奖励)")
                .withStyle(ChatFormatting.GREEN)
        );
        QisCSGO.LOGGER.info("玩家 {} 获得C4拆除奖励: {}", player.getName().getString(), C4_DEFUSE_REWARD);
    }
    
    /**
     * 发放回合胜利奖励
     */
    public void giveRoundWinReward(ServerPlayer player) {
        VirtualMoneyManager.getInstance().addMoney(player, ROUND_WIN_REWARD);
        player.sendSystemMessage(
            Component.literal("§a+$" + ROUND_WIN_REWARD + " §7(回合胜利奖励)")
                .withStyle(ChatFormatting.GREEN)
        );
    }
    
    /**
     * 应用死亡惩罚（扣除20%当前资金）
     */
    public void applyDeathPenalty(ServerPlayer player) {
        VirtualMoneyManager moneyManager = VirtualMoneyManager.getInstance();
        int currentMoney = moneyManager.getMoney(player);
        int penalty = (int) (currentMoney * DEATH_PENALTY_RATE);
        
        if (penalty > 0) {
            moneyManager.takeMoney(player, penalty);
            player.sendSystemMessage(
                Component.literal("§c-$" + penalty + " §7(死亡惩罚 20%)")
                    .withStyle(ChatFormatting.RED),
                true
            );
            QisCSGO.LOGGER.debug("玩家 {} 死亡惩罚: -{}", player.getName().getString(), penalty);
        }
    }
    
    /**
     * 记录回合结果并检查是否需要动态平衡
     * @param winningTeam 获胜队伍
     * @return 是否触发了动态平衡
     */
    public boolean recordRoundResult(String winningTeam) {
        // 更新连胜计数
        int wins = consecutiveWins.getOrDefault(winningTeam, 0) + 1;
        consecutiveWins.put(winningTeam, wins);
        
        // 重置失败方的连胜
        String losingTeam = winningTeam.equals("CT") ? "T" : "CT";
        consecutiveWins.put(losingTeam, 0);
        
        // 检查是否触发动态平衡
        if (wins >= CONSECUTIVE_WIN_THRESHOLD) {
            QisCSGO.LOGGER.info("{} 队连续胜利 {} 回合，触发动态平衡机制", winningTeam, wins);
            return true;
        }
        
        return false;
    }
    
    /**
     * 应用动态平衡：胜利方-10%，失败方+10%
     * @param winningTeam 连续获胜的队伍
     * @param players 所有玩家及其队伍信息
     */
    public void applyDynamicBalance(String winningTeam, Map<UUID, PlayerStats> players, 
                                    net.minecraft.server.MinecraftServer server) {
        VirtualMoneyManager moneyManager = VirtualMoneyManager.getInstance();
        String losingTeam = winningTeam.equals("CT") ? "T" : "CT";
        
        for (Map.Entry<UUID, PlayerStats> entry : players.entrySet()) {
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player == null) continue;
            
            PlayerStats stats = entry.getValue();
            int currentMoney = moneyManager.getMoney(player);
            
            if (stats.getTeam().equals(winningTeam)) {
                // 胜利方减少10%
                int reduction = (int) (currentMoney * DYNAMIC_BALANCE_RATE);
                if (reduction > 0) {
                    moneyManager.takeMoney(player, reduction);
                    player.sendSystemMessage(
                        Component.literal("§6动态平衡：-$" + reduction + " §7(连胜优势)")
                            .withStyle(ChatFormatting.YELLOW)
                    );
                }
            } else if (stats.getTeam().equals(losingTeam)) {
                // 失败方增加10%
                int bonus = (int) (currentMoney * DYNAMIC_BALANCE_RATE);
                moneyManager.addMoney(player, bonus);
                player.sendSystemMessage(
                    Component.literal("§a动态平衡：+$" + bonus + " §7(劣势补偿)")
                        .withStyle(ChatFormatting.GREEN)
                );
            }
        }
        
        QisCSGO.LOGGER.info("应用动态平衡：{} 队 -10%, {} 队 +10%", winningTeam, losingTeam);
    }
    
    /**
     * 重置连胜计数（用于换边等场景）
     */
    public void resetConsecutiveWins() {
        consecutiveWins.clear();
        consecutiveWins.put("CT", 0);
        consecutiveWins.put("T", 0);
        QisCSGO.LOGGER.debug("重置连胜计数");
    }
    
    /**
     * 获取队伍的连续胜利次数
     */
    public int getConsecutiveWins(String team) {
        return consecutiveWins.getOrDefault(team, 0);
    }
    
    // Getters for configuration constants
    public static int getGameStartMoney() {
        return GAME_START_MONEY;
    }
    
    public static int getRoundBaseMoney() {
        return ROUND_BASE_MONEY;
    }
    
    public static int getKillReward() {
        return KILL_REWARD;
    }
    
    public static int getC4PlantReward() {
        return C4_PLANT_REWARD;
    }
    
    public static int getC4DefuseReward() {
        return C4_DEFUSE_REWARD;
    }
    
    public static int getRoundWinReward() {
        return ROUND_WIN_REWARD;
    }
}
