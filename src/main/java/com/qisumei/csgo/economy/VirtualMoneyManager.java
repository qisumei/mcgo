package com.qisumei.csgo.economy;

import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 虚拟货币管理器 - 管理玩家的游戏内货币（不再使用钻石）
 * 使用ConcurrentHashMap实现线程安全的单例模式
 * 
 * <p>改进点：
 * <ul>
 *   <li>使用ConcurrentHashMap替代HashMap+synchronized以提高并发性能</li>
 *   <li>使用compute/computeIfPresent等原子操作避免竞态条件</li>
 *   <li>添加空值检查和参数验证</li>
 *   <li>改进JavaDoc文档</li>
 * </ul>
 */
public final class VirtualMoneyManager {
    private static final VirtualMoneyManager INSTANCE = new VirtualMoneyManager();

    /**
     * 玩家货币存储 - 使用ConcurrentHashMap保证线程安全
     * Key: 玩家UUID
     * Value: 货币数量（始终非负）
     */
    private final Map<UUID, Integer> playerMoney = new ConcurrentHashMap<>();

    private VirtualMoneyManager() {}

    /**
     * 获取单例实例
     * 
     * @return VirtualMoneyManager实例
     */
    public static VirtualMoneyManager getInstance() {
        return INSTANCE;
    }

    /**
     * 获取玩家当前货币数量
     * 
     * @param player 玩家对象，不能为null
     * @return 货币数量，如果玩家不存在则返回0
     * @throws IllegalArgumentException 如果player为null
     */
    public int getMoney(ServerPlayer player) {
        if (player == null) {
            throw new IllegalArgumentException("Player cannot be null");
        }
        return playerMoney.getOrDefault(player.getUUID(), 0);
    }

    /**
     * 获取玩家当前货币数量（通过UUID）
     * 
     * @param playerUUID 玩家UUID，不能为null
     * @return 货币数量，如果玩家不存在则返回0
     * @throws IllegalArgumentException 如果playerUUID为null
     */
    public int getMoney(UUID playerUUID) {
        if (playerUUID == null) {
            throw new IllegalArgumentException("Player UUID cannot be null");
        }
        return playerMoney.getOrDefault(playerUUID, 0);
    }

    /**
     * 设置玩家货币数量（自动确保非负）
     * 
     * @param player 玩家对象，不能为null
     * @param amount 货币数量，负数会被转换为0
     * @throws IllegalArgumentException 如果player为null
     */
    public void setMoney(ServerPlayer player, int amount) {
        if (player == null) {
            throw new IllegalArgumentException("Player cannot be null");
        }
        playerMoney.put(player.getUUID(), Math.max(0, amount));
    }

    /**
     * 给玩家增加货币（原子操作）
     * 
     * @param player 玩家对象，不能为null
     * @param amount 要增加的货币数量，必须为正数
     * @throws IllegalArgumentException 如果player为null或amount为非正数
     */
    public void addMoney(ServerPlayer player, int amount) {
        if (player == null) {
            throw new IllegalArgumentException("Player cannot be null");
        }
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive: " + amount);
        }
        playerMoney.compute(player.getUUID(), (uuid, current) -> 
            (current == null ? 0 : current) + amount
        );
    }

    /**
     * 扣除玩家货币（原子操作，如果余额不足则返回false）
     * 
     * @param player 玩家对象，不能为null
     * @param amount 要扣除的货币数量，必须为正数
     * @return 如果扣除成功返回true，余额不足则返回false
     * @throws IllegalArgumentException 如果player为null或amount为非正数
     */
    public boolean takeMoney(ServerPlayer player, int amount) {
        if (player == null) {
            throw new IllegalArgumentException("Player cannot be null");
        }
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive: " + amount);
        }
        
        var result = new boolean[1];
        playerMoney.compute(player.getUUID(), (uuid, current) -> {
            int currentMoney = current == null ? 0 : current;
            if (currentMoney < amount) {
                result[0] = false;
                return current; // 不修改，余额不足
            }
            result[0] = true;
            return currentMoney - amount;
        });
        return result[0];
    }

    /**
     * 检查玩家是否有足够的货币
     * 
     * @param player 玩家对象，不能为null
     * @param amount 需要检查的货币数量
     * @return 如果玩家货币>=amount返回true，否则返回false
     * @throws IllegalArgumentException 如果player为null
     */
    public boolean hasMoney(ServerPlayer player, int amount) {
        if (player == null) {
            throw new IllegalArgumentException("Player cannot be null");
        }
        return getMoney(player) >= amount;
    }

    /**
     * 清除玩家所有货币（用于比赛结束或玩家离开）
     * 
     * @param player 玩家对象，不能为null
     * @throws IllegalArgumentException 如果player为null
     */
    public void clearMoney(ServerPlayer player) {
        if (player == null) {
            throw new IllegalArgumentException("Player cannot be null");
        }
        playerMoney.remove(player.getUUID());
    }

    /**
     * 清除所有玩家货币（用于服务器重置）
     * 注意：此方法会清空所有玩家的货币数据，应谨慎使用
     */
    public void clearAll() {
        playerMoney.clear();
    }
}

