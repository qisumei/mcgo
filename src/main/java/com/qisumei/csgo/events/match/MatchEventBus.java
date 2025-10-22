package com.qisumei.csgo.events.match;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 比赛事件总线 - 管理事件监听器并分发事件
 * 使用观察者模式实现解耦
 */
public class MatchEventBus {
    // 使用 CopyOnWriteArrayList 保证线程安全，适合读多写少的场景
    private final List<MatchEventListener> listeners = new CopyOnWriteArrayList<>();
    
    /**
     * 注册事件监听器
     * @param listener 监听器实例
     */
    public void registerListener(MatchEventListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }
    
    /**
     * 注销事件监听器
     * @param listener 监听器实例
     */
    public void unregisterListener(MatchEventListener listener) {
        listeners.remove(listener);
    }
    
    /**
     * 触发换边事件
     * @param event 换边事件
     */
    public void fireTeamSwapEvent(TeamSwapEvent event) {
        for (MatchEventListener listener : listeners) {
            try {
                listener.onTeamSwap(event);
            } catch (Exception e) {
                // 记录异常但不中断其他监听器
                com.qisumei.csgo.QisCSGO.LOGGER.error("换边事件处理异常", e);
            }
        }
    }
    
    /**
     * 触发回合开始事件
     * @param event 回合开始事件
     */
    public void fireRoundStartEvent(RoundStartEvent event) {
        for (MatchEventListener listener : listeners) {
            try {
                listener.onRoundStart(event);
            } catch (Exception e) {
                com.qisumei.csgo.QisCSGO.LOGGER.error("回合开始事件处理异常", e);
            }
        }
    }
    
    /**
     * 获取当前注册的监听器数量
     */
    public int getListenerCount() {
        return listeners.size();
    }
}
