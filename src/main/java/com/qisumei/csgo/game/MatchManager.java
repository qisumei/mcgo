package com.qisumei.csgo.game;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * MatchManager 类用于管理所有正在进行的比赛。
 * 它提供了创建、获取、移除比赛以及处理比赛逻辑更新的方法。
 */
public class MatchManager {

    private static final Map<String, Match> ACTIVE_MATCHES = new HashMap<>();

    /**
     * 每个游戏刻调用一次，用于更新所有正在进行中的比赛状态。
     *
     * @param server 当前的Minecraft服务器实例
     */
    public static void tick(MinecraftServer server) {
        // 创建一个副本以避免在迭代过程中修改集合
        for (Match match : new ArrayList<>(ACTIVE_MATCHES.values())) {
            if (match.getState() == Match.MatchState.IN_PROGRESS) {
                // --- 修正 #1: match.tick() 不再需要 server 参数 ---
                match.tick();
            }
            if (match.getState() == Match.MatchState.FINISHED) {
                // TODO: Maybe add a delay before removing finished matches
            }
        }
    }

    /**
     * 根据给定名称创建一个新的比赛。
     *
     * @param name 比赛名称
     * @param maxPlayers 最大玩家数量
     * @param server 当前的Minecraft服务器实例
     * @return 如果成功创建则返回true，如果已存在同名比赛则返回false
     */
    // --- 修正 #2: createMatch 需要 server 参数来创建 Match ---
    public static boolean createMatch(String name, int maxPlayers, MinecraftServer server) {
        if (ACTIVE_MATCHES.containsKey(name)) {
            return false;
        }
        Match newMatch = new Match(name, maxPlayers, server);
        ACTIVE_MATCHES.put(name, newMatch);
        return true;
    }

    /**
     * 获取指定名称的比赛对象。
     *
     * @param name 要查找的比赛名称
     * @return 对应的比赛对象，若不存在则返回null
     */
    public static Match getMatch(String name) {
        return ACTIVE_MATCHES.get(name);
    }

    /**
     * 获取当前所有的比赛集合。
     *
     * @return 包含所有活动比赛的集合视图
     */
    public static Collection<Match> getAllMatches() {
        return ACTIVE_MATCHES.values();
    }

    /**
     * 查找某个玩家所在的比赛。
     *
     * @param player 要查询的玩家对象
     * @return 玩家所在比赛的对象，若未参与任何比赛则返回null
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
     * 根据C4炸弹的位置找到对应的比赛。
     *
     * @param pos C4炸弹的位置坐标
     * @return 在该位置种植了C4的比赛对象，如果没有则返回null
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
     * 移除指定名称的比赛。
     *
     * @param name 要移除的比赛名称
     */
    public static void removeMatch(String name) {
        ACTIVE_MATCHES.remove(name);
    }
}
