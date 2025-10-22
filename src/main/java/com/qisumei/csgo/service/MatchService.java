package com.qisumei.csgo.service;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import com.qisumei.csgo.game.Match;
import net.minecraft.world.entity.player.Player;

import java.util.Collection;

public interface MatchService {
    boolean createMatch(String name, int maxPlayers, MinecraftServer server);
    Match getMatch(String name);
    Collection<Match> getAllMatches();
    Match getPlayerMatch(Player player);
    Match getMatchFromC4Pos(BlockPos pos);
    void removeMatch(String name);
    void tick();
}

