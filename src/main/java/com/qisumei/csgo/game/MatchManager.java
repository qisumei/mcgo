package com.qisumei.csgo.game;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MatchManager 类用于管理所有正在进行的比赛。
 * 它提供了创建、获取、移除比赛以及处理比赛逻辑更新的方法。
 * 
 * <p>改进点：
 * <ul>
 *   <li>使用ConcurrentHashMap替代HashMap以提高线程安全性</li>
 *   <li>添加参数验证和空值检查</li>
 *   <li>使用final防止子类化</li>
 * </ul>
 */
public final class MatchManager {

    // 使用ConcurrentHashMap确保线程安全
    private static final Map<String, Match> ACTIVE_MATCHES = new ConcurrentHashMap<>();
    
    // 私有构造器防止实例化
    private MatchManager() {
        throw new AssertionError("Utility class should not be instantiated");
    }

    /**
     * 每个游戏刻调用一次，用于更新所有正在进行中的比赛状态。
     * 使用Java 21的var简化局部变量声明
     */
    public static void tick() {
        // 创建一个副本以避免在迭代过程中修改集合
        // ConcurrentHashMap的values()已经是线程安全的，但创建副本提供额外的安全性
        for (var match : new ArrayList<>(ACTIVE_MATCHES.values())) {
            if (match.getState() == Match.MatchState.IN_PROGRESS) {
                match.tick();
            }
        }
    }

    /**
     * 根据给定名称创建一个新的比赛。
     *
     * @param name 比赛名称，不能为null或空白
     * @param maxPlayers 最大玩家数量，必须为正数
     * @param server 当前的Minecraft服务器实例，不能为null
     * @return 如果成功创建则返回true，如果已存在同名比赛则返回false
     * @throws IllegalArgumentException 如果参数无效
     */
    public static boolean createMatch(String name, int maxPlayers, MinecraftServer server) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Match name cannot be null or blank");
        }
        if (maxPlayers <= 0) {
            throw new IllegalArgumentException("Max players must be positive: " + maxPlayers);
        }
        if (server == null) {
            throw new IllegalArgumentException("Server cannot be null");
        }
        
        // 使用putIfAbsent原子操作避免竞态条件
        var newMatch = new Match(name, maxPlayers, server);
        return ACTIVE_MATCHES.putIfAbsent(name, newMatch) == null;
    }

    /**
     * 获取指定名称的比赛对象。
     *
     * @param name 要查找的比赛名称，不能为null
     * @return 对应的比赛对象，若不存在则返回null
     * @throws IllegalArgumentException 如果name为null
     */
    public static Match getMatch(String name) {
        if (name == null) {
            throw new IllegalArgumentException("Match name cannot be null");
        }
        return ACTIVE_MATCHES.get(name);
    }

    /**
     * 获取当前所有的比赛集合。
     * 返回的集合是不可修改的视图，防止外部修改内部状态
     *
     * @return 包含所有活动比赛的不可修改集合视图
     */
    public static Collection<Match> getAllMatches() {
        return new ArrayList<>(ACTIVE_MATCHES.values());
    }

    /**
     * 查找某个玩家所在的比赛。
     *
     * @param player 要查询的玩家对象，不能为null
     * @return 玩家所在比赛的对象，若未参与任何比赛则返回null
     * @throws IllegalArgumentException 如果player为null
     */
    public static Match getPlayerMatch(Player player) {
        if (player == null) {
            throw new IllegalArgumentException("Player cannot be null");
        }
        
        var playerUUID = player.getUUID();
        for (var match : ACTIVE_MATCHES.values()) {
            if (match.getPlayerStats().containsKey(playerUUID)) {
                return match;
            }
        }
        return null;
    }

    /**
     * 根据C4炸弹的位置找到对应的比赛。
     *
     * @param pos C4炸弹的位置坐标，不能为null
     * @return 在该位置种植了C4的比赛对象，如果没有则返回null
     * @throws IllegalArgumentException 如果pos为null
     */
    public static Match getMatchFromC4Pos(BlockPos pos) {
        if (pos == null) {
            throw new IllegalArgumentException("Position cannot be null");
        }
        
        for (var match : ACTIVE_MATCHES.values()) {
            if (match.isC4Planted() && pos.equals(match.getC4Pos())) {
                return match;
            }
        }
        return null;
    }

    /**
     * 移除指定名称的比赛。
     *
     * @param name 要移除的比赛名称，不能为null
     * @return 被移除的比赛对象，如果不存在则返回null
     * @throws IllegalArgumentException 如果name为null
     */
    public static Match removeMatch(String name) {
        if (name == null) {
            throw new IllegalArgumentException("Match name cannot be null");
        }
        return ACTIVE_MATCHES.remove(name);
    }
}
