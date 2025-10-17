package com.qisumei.csgo.game;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class MatchManager {

    private static final Map<String, Match> ACTIVE_MATCHES = new HashMap<>();

    public static void tick(MinecraftServer server) {
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

    // --- 修正 #2: createMatch 需要 server 参数来创建 Match ---
    public static boolean createMatch(String name, int maxPlayers, MinecraftServer server) {
        if (ACTIVE_MATCHES.containsKey(name)) {
            return false;
        }
        Match newMatch = new Match(name, maxPlayers, server);
        ACTIVE_MATCHES.put(name, newMatch);
        return true;
    }

    public static Match getMatch(String name) {
        return ACTIVE_MATCHES.get(name);
    }

    public static Collection<Match> getAllMatches() {
        return ACTIVE_MATCHES.values();
    }

    public static Match getPlayerMatch(Player player) {
        for (Match match : ACTIVE_MATCHES.values()) {
            if (match.getPlayerStats().containsKey(player.getUUID())) {
                return match;
            }
        }
        return null;
    }

    public static Match getMatchFromC4Pos(BlockPos pos) {
        for (Match match : ACTIVE_MATCHES.values()) {
            if (match.isC4Planted() && pos.equals(match.getC4Pos())) {
                return match;
            }
        }
        return null;
    }

    public static void removeMatch(String name) {
        ACTIVE_MATCHES.remove(name);
    }
}