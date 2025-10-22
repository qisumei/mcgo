package com.qisumei.csgo.c4;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

/**
 * C4 行为的抽象接口，定义 Match 对 C4 的最小依赖契约。
 */
public interface C4Controller {
    void tick();
    void reset();
    void onC4Planted(BlockPos pos);
    void onC4Defused(ServerPlayer defuser);
    void onC4Exploded();
    void giveC4ToRandomT();
    boolean isC4Planted();
    BlockPos getC4Pos();
    int getC4TicksLeft();
    // 为玩家 tick 提供钩子，C4 需要在玩家 tick 时执行一些局部逻辑（例如拆弹判定）
    void handlePlayerTick(ServerPlayer player);
}
