package com.qisumei.csgo.game;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 比赛管理器工具类，负责全局管理所有CSGO比赛实例的生命周期。
 * <p>
 * 这是一个静态工具类，用于创建、检索、更新和移除 {@link Match} 对象。
 * 它通过一个集中的 tick 方法来驱动所有正在进行中的比赛逻辑更新。
 * </p>
 *
 * @author Qisumei
 */
public final class MatchManager {

    /**
     * 存储所有活动比赛的映射表，键为比赛名称，值为比赛实例。
     * 使用 {@link ConcurrentHashMap} 是为了线程安全，防止在遍历和修改时出现问题。
     */
    private static final Map<String, Match> ACTIVE_MATCHES = new ConcurrentHashMap<>();

    /**
     * 私有构造函数，防止该工具类被实例化。
     */
    private MatchManager() {}

    /**
     * 每个服务器 tick 调用一次，用于更新所有正在进行中的比赛状态。
     * 这是驱动整个模组核心逻辑循环的入口点。
     */
    public static void tick() {
        // 遍历所有活动的比赛，并调用它们的 tick 方法
        for (Match match : ACTIVE_MATCHES.values()) {
            if (match.getState() == Match.MatchState.IN_PROGRESS) {
                match.tick();
            }
        }
    }

    /**
     * 根据给定的名称和参数创建一个新的比赛。
     *
     * @param name       比赛的唯一名称。
     * @param maxPlayers 比赛的最大玩家数量。
     * @param server     当前的 Minecraft 服务器实例。
     * @return 如果成功创建则返回 true；如果已存在同名比赛则返回 false。
     */
    public static boolean createMatch(String name, int maxPlayers, MinecraftServer server) {
        // 使用 computeIfAbsent 原子操作来创建比赛，避免竞态条件
        return ACTIVE_MATCHES.computeIfAbsent(name, k -> new Match(k, maxPlayers, server)) != null;
    }

    /**
     * 根据名称获取一个比赛实例。
     *
     * @param name 要查找的比赛名称。
     * @return 如果找到，则返回对应的 {@link Match} 对象；否则返回 null。
     */
    public static Match getMatch(String name) {
        return ACTIVE_MATCHES.get(name);
    }

    /**
     * 获取当前所有活动比赛的不可修改集合。
     *
     * @return 包含所有活动比赛的只读集合。
     */
    public static Collection<Match> getAllMatches() {
        return Collections.unmodifiableCollection(ACTIVE_MATCHES.values());
    }

    /**
     * 查找指定玩家当前所在的比赛。
     *
     * @param player 要查询的玩家对象。
     * @return 如果玩家在某场比赛中，则返回该 {@link Match} 对象；否则返回 null。
     */
    public static Match getPlayerMatch(Player player) {
        for (Match match : ACTIVE_MATCHES.values()) {
            if (match.getPlayerStats().containsKey(player.getUUID())) {
                return match;
            }
        }
        return null;
    }

    /**
     * 根据C4炸弹在世界中的位置，找到对应的比赛。
     *
     * @param pos C4炸弹的位置坐标。
     * @return 如果找到，则返回在该位置安放了C4的比赛；否则返回 null。
     */
    public static Match getMatchFromC4Pos(BlockPos pos) {
        for (Match match : ACTIVE_MATCHES.values()) {
            if (match.isC4Planted() && pos.equals(match.getC4Pos())) {
                return match;
            }
        }
        return null;
    }

    /**
     * 根据名称移除一个比赛。
     *
     * @param name 要移除的比赛名称。
     */
    public static void removeMatch(String name) {
        ACTIVE_MATCHES.remove(name);
    }
}
