package com.qisumei.csgo.game;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.phys.AABB;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * MatchContext 提供 C4Manager 等组件所需的最小 Match API。
 * 通过接口编程可以降低模块之间的耦合。
 */
public interface MatchContext {
    MinecraftServer getServer();
    void broadcastToAllPlayersInMatch(net.minecraft.network.chat.Component message);
    void broadcastToTeam(net.minecraft.network.chat.Component message, String team);
    Map<UUID, PlayerStats> getPlayerStats();
    Set<UUID> getAlivePlayers();
    void endRound(String winningTeam, String reason);
    Match.RoundState getRoundState();
    AABB getBombsiteA();
    AABB getBombsiteB();
    boolean isPlayerInBombsite(net.minecraft.server.level.ServerPlayer player);
    AABB getMatchAreaBoundingBox();
}
