package com.qisumei.csgo.events.match;

import com.qisumei.csgo.game.Match;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

/**
 * 换边事件 - 在比赛换边时触发
 * 用于解耦换边逻辑与经济系统，允许其他系统监听换边并做出响应
 */
public class TeamSwapEvent {
    private final Match match;
    private final Map<UUID, ServerPlayer> affectedPlayers;
    private final int currentRound;
    
    /**
     * 创建换边事件
     * @param match 比赛实例
     * @param affectedPlayers 受影响的玩家映射（UUID -> ServerPlayer）
     * @param currentRound 当前回合数
     */
    public TeamSwapEvent(Match match, Map<UUID, ServerPlayer> affectedPlayers, int currentRound) {
        this.match = match;
        this.affectedPlayers = Collections.unmodifiableMap(affectedPlayers);
        this.currentRound = currentRound;
    }
    
    /**
     * 获取比赛实例
     */
    public Match getMatch() {
        return match;
    }
    
    /**
     * 获取受影响的玩家
     */
    public Map<UUID, ServerPlayer> getAffectedPlayers() {
        return affectedPlayers;
    }
    
    /**
     * 获取当前回合数
     */
    public int getCurrentRound() {
        return currentRound;
    }
}
