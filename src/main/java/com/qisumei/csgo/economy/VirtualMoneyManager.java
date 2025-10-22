package com.qisumei.csgo.economy;

import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 虚拟货币管理器 - 管理玩家的游戏内货币（不再使用钻石）
 * 线程安全的单例实现
 */
public class VirtualMoneyManager {
    private static final VirtualMoneyManager INSTANCE = new VirtualMoneyManager();

    // 玩家货币存储 <玩家UUID, 货币数量>
    private final Map<UUID, Integer> playerMoney = new HashMap<>();

    private VirtualMoneyManager() {}

    public static VirtualMoneyManager getInstance() {
        return INSTANCE;
    }

    /**
     * 获取玩家当前货币数量
     */
    public synchronized int getMoney(ServerPlayer player) {
        return playerMoney.getOrDefault(player.getUUID(), 0);
    }

    /**
     * 获取玩家当前货币数量（通过UUID）
     */
    public synchronized int getMoney(UUID playerUUID) {
        return playerMoney.getOrDefault(playerUUID, 0);
    }

    /**
     * 设置玩家货币数量
     */
    public synchronized void setMoney(ServerPlayer player, int amount) {
        playerMoney.put(player.getUUID(), Math.max(0, amount));
    }

    /**
     * 给玩家增加货币
     */
    public synchronized void addMoney(ServerPlayer player, int amount) {
        if (amount <= 0) return;
        int current = getMoney(player);
        playerMoney.put(player.getUUID(), current + amount);
    }

    /**
     * 扣除玩家货币（如果余额不足则返回false）
     */
    public synchronized boolean takeMoney(ServerPlayer player, int amount) {
        if (amount <= 0) return true;
        int current = getMoney(player);
        if (current < amount) {
            return false; // 余额不足
        }
        playerMoney.put(player.getUUID(), current - amount);
        return true;
    }

    /**
     * 检查玩家是否有足够的货币
     */
    public synchronized boolean hasMoney(ServerPlayer player, int amount) {
        return getMoney(player) >= amount;
    }

    /**
     * 清除玩家所有货币（用于比赛结束或玩家离开）
     */
    public synchronized void clearMoney(ServerPlayer player) {
        playerMoney.remove(player.getUUID());
    }

    /**
     * 清除所有玩家货币（用于服务器重置）
     */
    public synchronized void clearAll() {
        playerMoney.clear();
    }
}

