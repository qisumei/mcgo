package com.qisumei.csgo.game;

import com.qisumei.csgo.server.ServerCommandExecutor;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.UUID;

/**
 * 队伍换边服务 - 处理换边相关的逻辑
 * 从 Match.java 中解耦出来，提高代码可维护性
 */
public class TeamSwapService {
    
    private final ServerCommandExecutor commandExecutor;
    private final PlayerService playerService;
    
    public TeamSwapService(ServerCommandExecutor commandExecutor, PlayerService playerService) {
        this.commandExecutor = commandExecutor;
        this.playerService = playerService;
    }
    
    /**
     * 更新玩家的队伍归属和游戏内显示
     * @param player 玩家
     * @param newTeam 新队伍 ("CT" 或 "T")
     * @param newTeamName 新队伍的完整名称（用于游戏内team命令）
     */
    public void updatePlayerTeam(ServerPlayer player, String newTeam, String newTeamName) {
        // 更新游戏内队伍
        commandExecutor.executeGlobal("team leave " + player.getName().getString());
        commandExecutor.executeGlobal("team join " + newTeamName + " " + player.getName().getString());
        
        // 清理玩家背包（保留受保护物品）
        playerService.performSelectiveClear(player);
        
        // 通知玩家
        String teamDisplayName = "CT".equals(newTeam) ? "反恐精英 (CT)" : "恐怖分子 (T)";
        player.sendSystemMessage(
            Component.literal("你现在是 " + teamDisplayName + " 队的一员！").withStyle(ChatFormatting.AQUA)
        );
    }
    
    /**
     * 批量更新玩家队伍
     * @param players 玩家映射（UUID -> ServerPlayer）
     * @param statsMap 玩家统计数据映射（用于获取新队伍信息）
     * @param ctTeamName CT队伍名称
     * @param tTeamName T队伍名称
     */
    public void updatePlayersTeam(
        Map<UUID, ServerPlayer> players,
        Map<UUID, PlayerStats> statsMap,
        String ctTeamName,
        String tTeamName
    ) {
        for (Map.Entry<UUID, ServerPlayer> entry : players.entrySet()) {
            ServerPlayer player = entry.getValue();
            PlayerStats stats = statsMap.get(entry.getKey());
            
            if (player != null && stats != null) {
                String newTeam = stats.getTeam();
                String newTeamName = "CT".equals(newTeam) ? ctTeamName : tTeamName;
                updatePlayerTeam(player, newTeam, newTeamName);
            }
        }
    }
}
