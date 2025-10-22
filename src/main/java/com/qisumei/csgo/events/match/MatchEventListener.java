package com.qisumei.csgo.events.match;

/**
 * 比赛事件监听器接口
 * 用于监听比赛中的各种事件
 */
public interface MatchEventListener {
    /**
     * 换边时调用
     * @param event 换边事件
     */
    default void onTeamSwap(TeamSwapEvent event) {}
    
    /**
     * 回合开始时调用
     * @param event 回合开始事件
     */
    default void onRoundStart(RoundStartEvent event) {}
}
