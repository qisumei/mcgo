package com.qisumei.csgo.game;

import net.minecraft.server.level.ServerPlayer;

/**
 * Match 的计分板抽象接口，定义 Match 与计分板管理器之间的交互契约，便于替换实现以降低耦合。
 */
public interface MatchScoreboard {
    void setupScoreboard();
    void updateScoreboard();
    void reapplyToPlayer(ServerPlayer player);
    void removeScoreboard();
}

