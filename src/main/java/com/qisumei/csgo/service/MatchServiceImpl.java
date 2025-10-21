package com.qisumei.csgo.service;

import com.qisumei.csgo.game.Match;
import com.qisumei.csgo.game.MatchManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;

import java.util.Collection;

/**
 * MatchService 的默认实现，当前委托给老的静态 MatchManager，以保证兼容性。
 * 未来可以替换为更好的实现而不改动使用方。
 */
public class MatchServiceImpl implements MatchService {
    @Override
    public boolean createMatch(String name, int maxPlayers, MinecraftServer server) {
        return MatchManager.createMatch(name, maxPlayers, server);
    }

    @Override
    public Match getMatch(String name) {
        return MatchManager.getMatch(name);
    }

    @Override
    public Collection<Match> getAllMatches() {
        return MatchManager.getAllMatches();
    }

    @Override
    public Match getPlayerMatch(Player player) {
        return MatchManager.getPlayerMatch(player);
    }

    @Override
    public Match getMatchFromC4Pos(BlockPos pos) {
        return MatchManager.getMatchFromC4Pos(pos);
    }

    @Override
    public void removeMatch(String name) {
        MatchManager.removeMatch(name);
    }

    @Override
    public void tick() {
        MatchManager.tick();
    }
}

