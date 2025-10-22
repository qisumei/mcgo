package com.qisumei.csgo.economy;

import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 虚拟货币管理器 - 管理玩家的游戏内货币（不再使用钻石）
 * 线程安全的单例实现，使用 ConcurrentHashMap 提高并发性能。
 * 
 * 改进：
 * - 使用 ConcurrentHashMap 替代 HashMap + synchronized 以提高并发性能
 * - 添加防御性编程：非空检查、数值范围验证
 * - 使用 Java 21 改进的 Objects 工具类
 */
public final class VirtualMoneyManager {
    private static final VirtualMoneyManager INSTANCE = new VirtualMoneyManager();
    private static final int MAX_MONEY = 65535; // 最大货币限制，防止整数溢出

    // 玩家货币存储 <玩家UUID, 货币数量> - 使用 ConcurrentHashMap 提高并发性能
    private final Map<UUID, Integer> playerMoney = new ConcurrentHashMap<>();

    private VirtualMoneyManager() {}

    public static VirtualMoneyManager getInstance() {
        return INSTANCE;
    }

    /**
     * 获取玩家当前货币数量。
     * @param player 玩家对象（不能为 null）
     * @return 当前货币数量，如果玩家不存在则返回 0
     * @throws NullPointerException 如果 player 为 null
     */
    public int getMoney(ServerPlayer player) {
        Objects.requireNonNull(player, "Player cannot be null");
        return playerMoney.getOrDefault(player.getUUID(), 0);
    }

    /**
     * 获取玩家当前货币数量（通过UUID）。
     * @param playerUUID 玩家UUID（不能为 null）
     * @return 当前货币数量，如果玩家不存在则返回 0
     * @throws NullPointerException 如果 playerUUID 为 null
     */
    public int getMoney(UUID playerUUID) {
        Objects.requireNonNull(playerUUID, "Player UUID cannot be null");
        return playerMoney.getOrDefault(playerUUID, 0);
    }

    /**
     * 设置玩家货币数量。
     * @param player 玩家对象（不能为 null）
     * @param amount 货币数量（自动限制在 0 到 MAX_MONEY 之间）
     * @throws NullPointerException 如果 player 为 null
     */
    public void setMoney(ServerPlayer player, int amount) {
        Objects.requireNonNull(player, "Player cannot be null");
        // 确保货币在有效范围内
        int validAmount = Math.clamp(amount, 0, MAX_MONEY);
        playerMoney.put(player.getUUID(), validAmount);
    }

    /**
     * 给玩家增加货币。
     * @param player 玩家对象（不能为 null）
     * @param amount 要增加的货币数量（必须 > 0）
     * @throws NullPointerException 如果 player 为 null
     */
    public void addMoney(ServerPlayer player, int amount) {
        Objects.requireNonNull(player, "Player cannot be null");
        if (amount <= 0) return;
        
        playerMoney.compute(player.getUUID(), (uuid, current) -> {
            int newAmount = (current == null ? 0 : current) + amount;
            return Math.min(newAmount, MAX_MONEY); // 防止溢出
        });
    }

    /**
     * 扣除玩家货币。
     * @param player 玩家对象（不能为 null）
     * @param amount 要扣除的货币数量（必须 > 0）
     * @return 如果扣除成功返回 true，余额不足返回 false
     * @throws NullPointerException 如果 player 为 null
     */
    public boolean takeMoney(ServerPlayer player, int amount) {
        Objects.requireNonNull(player, "Player cannot be null");
        if (amount <= 0) return true;
        
        UUID playerId = player.getUUID();
        int current = getMoney(playerId);
        
        if (current < amount) {
            return false; // 余额不足
        }
        
        playerMoney.put(playerId, current - amount);
        return true;
    }

    /**
     * 检查玩家是否有足够的货币。
     * @param player 玩家对象（不能为 null）
     * @param amount 需要检查的货币数量
     * @return 如果玩家有足够货币返回 true
     * @throws NullPointerException 如果 player 为 null
     */
    public boolean hasMoney(ServerPlayer player, int amount) {
        Objects.requireNonNull(player, "Player cannot be null");
        return getMoney(player) >= amount;
    }

    /**
     * 清除玩家所有货币（用于比赛结束或玩家离开）。
     * @param player 玩家对象（不能为 null）
     * @throws NullPointerException 如果 player 为 null
     */
    public void clearMoney(ServerPlayer player) {
        Objects.requireNonNull(player, "Player cannot be null");
        playerMoney.remove(player.getUUID());
    }

    /**
     * 清除所有玩家货币（用于服务器重置）。
     */
    public void clearAll() {
        playerMoney.clear();
    }
}

